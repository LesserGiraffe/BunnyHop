/*
 * Copyright 2017 K.Koike
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package net.seapanda.bunnyhop.bhprogram;

import com.jcraft.jsch.Channel;
import com.jcraft.jsch.ChannelExec;
import com.jcraft.jsch.ChannelSftp;
import com.jcraft.jsch.JSch;
import com.jcraft.jsch.JSchException;
import com.jcraft.jsch.Session;
import com.jcraft.jsch.UserInfo;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.control.ButtonType;
import net.seapanda.bunnyhop.bhprogram.common.message.BhProgramNotification;
import net.seapanda.bunnyhop.bhprogram.message.BhProgramMessageProcessor;
import net.seapanda.bunnyhop.common.BhConstants;
import net.seapanda.bunnyhop.common.BhConstants.BhRuntime;
import net.seapanda.bunnyhop.common.ExclusiveSelection;
import net.seapanda.bunnyhop.common.TextDefs;
import net.seapanda.bunnyhop.service.BhScriptRepository;
import net.seapanda.bunnyhop.service.LogManager;
import net.seapanda.bunnyhop.service.MessageService;
import net.seapanda.bunnyhop.simulator.SimulatorCmdProcessor;
import org.mozilla.javascript.Context;
import org.mozilla.javascript.Script;
import org.mozilla.javascript.Scriptable;

/**
 * リモート環境で動作する BhRuntime 操作を行うクラス.
 *
 * @author K.Koike
 */
public class RemoteBhRuntimeController implements BhRuntimeController {
  
  private final MessageService msgService;
  private final BhRuntimeHelper helper;
  private UserInfoImpl userInfo;
  /** プログラム実行中なら true. */
  private final AtomicReference<Boolean> programRunning = new AtomicReference<>(false);
  /** ファイルがコピー中の場合 true. */
  private AtomicReference<Boolean> fileCopyIsCancelled = new AtomicReference<>(true);
  /** BhRuntime を起動するコマンドを生成するスクリプト. */
  private final Script startCmdGenerator;
  /** BhRuntime を終了するコマンドを生成するスクリプト. */
  private final Script killCmdGenerator;

  /** コンストラクタ. */
  public RemoteBhRuntimeController(
      BhProgramMessageProcessor msgProcessor,    
      SimulatorCmdProcessor simCmdProcessor,
      MessageService msgService,
      BhScriptRepository repository) 
      throws IllegalStateException {
    this.msgService = msgService;
    helper = new BhRuntimeHelper(msgProcessor, simCmdProcessor, msgService);
    boolean success = repository.allExist(
        BhConstants.Path.REMOTE_EXEC_CMD_GENERATOR_JS,
        BhConstants.Path.REMOTE_KILL_CMD_GENERATOR_JS);
    if (!success) {
      String msg = "Cannot find remote cmd scripts";
      LogManager.logger().error(msg);
      throw new IllegalStateException(msg);
    }
    startCmdGenerator = repository.getScript(BhConstants.Path.REMOTE_EXEC_CMD_GENERATOR_JS);
    killCmdGenerator = repository.getScript(BhConstants.Path.REMOTE_KILL_CMD_GENERATOR_JS);
  }

  @Override
  public synchronized boolean start(Path filePath, String ipAddr, String uname, String password) {
    boolean doTerminate = true;
    if (userInfo != null && !userInfo.isSameAccessPoint(ipAddr, uname)) {
      switch (askIfStopProgram()) {
        case NO:
          doTerminate = false;  // 実行環境が現在のものと違ってかつ, プログラムを止めないが選択された場合
          break;
        case CANCEL:
          return false;
        default:
          break;
      }
    }
    return execute(filePath, ipAddr, uname, password, doTerminate);
  }

  @Override
  public boolean start(Path filePath) {
    throw new UnsupportedOperationException("Cannot start a local BhRuntime.");
  }

  /**
   * BhProgram を実行する.
   *
   * @param filePath このパスのファイルをリモート環境にコピーして実行する
   * @param ipAddr BhProgram を実行するマシンのIPアドレス
   * @param uname BhProgram を実行するマシンにログインする際のユーザ名
   * @param password BhProgram を実行するマシンにログインする際のパスワード
   * @param terminate 現在実行中のプログラムを停止する場合 true. 切断だけする場合 false.
   * @return BhProgram の実行処理でエラーが発生しなかった場合 true
   */
  private boolean execute(
      Path filePath,
      String ipAddr,
      String uname,
      String password,
      boolean terminate) {
    terminateOrDisconnect(terminate);

    msgService.info(TextDefs.BhRuntime.Remote.preparingToRun.get());
    try {
      var destPath = Paths.get(
          BhConstants.Path.REMOTE_BUNNYHOP_DIR,
          BhConstants.Path.REMOTE_APP_DIR,
          BhConstants.Path.REMOTE_COMPILED_DIR,
          BhConstants.Path.APP_FILE_NAME_JS);
      boolean success = copyFile(ipAddr, uname, password, filePath.toString(), destPath.toString());
      if (!success) {
        throw new Exception();
      }
      userInfo = new UserInfoImpl(ipAddr, uname, password, msgService);
      ChannelExec channel = startRuntimeProcess(userInfo);  // リモート実行環境起動
      if (channel == null) {
        throw new Exception();
      }
      // BhRuntime の実行時パスからの相対パスで実行するスクリプトのパスを指定する.
      String fileRelPath = Paths.get(
          BhConstants.Path.REMOTE_COMPILED_DIR, BhConstants.Path.APP_FILE_NAME_JS).toString();
      success = helper.runBhProgram(fileRelPath, ipAddr, channel.getInputStream());  // BhProgram 実行
      // チャンネルは開いたままだが切断する
      channel.disconnect();
      channel.getSession().disconnect();
      if (!success) {
        throw new Exception();
      }
    } catch (Exception e) {
      msgService.error(TextDefs.BhRuntime.Remote.failedToRun.get());
      LogManager.logger().error(
          "Failed to run %s. (remote)".formatted(filePath.getFileName()));
      terminate(BhConstants.BhRuntime.REMOTE_RUNTIME_TERMINATION_TIMEOUT_SHORT);
      return false;
    }
    msgService.info(TextDefs.BhRuntime.Remote.startToRun.get());
    programRunning.set(true);
    return true;
  }

  /**
   * BhProgram が動作している場合, 終了か切断を行う.
   *
   * @param terminate 終了する場合 true. 切断する場合 false.
   */
  private void terminateOrDisconnect(boolean terminate) {
    if (programRunning.get()) {
      if (terminate) {
        terminate(BhConstants.BhRuntime.REMOTE_RUNTIME_TERMINATION_TIMEOUT);
      } else {
        helper.haltTransceiver();
        programRunning.set(false);
      }
    }
  }

  @Override
  public synchronized boolean terminate() {
    fileCopyIsCancelled.set(true);
    if (!programRunning.get()) {
      msgService.error(TextDefs.BhRuntime.Remote.hasAlreadyEnded.get());
      return false;
    }
    return terminate(BhConstants.BhRuntime.REMOTE_RUNTIME_TERMINATION_TIMEOUT);
  }

  /**
   * <pre>
   * リモートマシンで実行中の BhProgram 実行環境を強制終了する.
   * BhProgram 実行環境を終了済みの場合に呼んでも問題ない.
   * </pre>
   *
   * @param timeout タイムアウト
   * @return 強制終了に成功した場合true
   */
  private boolean terminate(int timeout) {
    msgService.info(TextDefs.BhRuntime.Remote.preparingToEnd.get());
    try {
      boolean success = helper.haltTransceiver();
      if (!success) {
        throw new Exception();
      }
      String killCmd = genKillCmd();
      if (killCmd == null) {
        throw new Exception();
      }
      ChannelExec channel = execCmd(killCmd, new UserInfoImpl(userInfo, msgService));
      if (channel == null) {
        throw new Exception();
      }
      Optional<Integer> status = waitForChannelClosed(channel, timeout);
      int statusCode = status.orElseThrow(() -> new Exception());
      if (statusCode != 0) {
        LogManager.logger().error("terminate status err  (%s)".formatted(statusCode));
        throw new Exception("");
      }
    } catch (Exception e) {
      msgService.error(TextDefs.BhRuntime.Remote.failedToEnd.get());
      return false;
    }
    msgService.info(TextDefs.BhRuntime.Remote.hasTeminated.get());
    programRunning.set(false);
    return true;
  }

  @Override
  public synchronized boolean connect() {
    return helper.connect();
  }

  @Override
  public synchronized boolean disconnect() {
    return helper.disconnect();
  }

  @Override
  public synchronized BhRuntimeStatus send(BhProgramNotification notif) {
    return helper.send(notif);
  }

  /**
   * BhProgramの実行環境プロセスをスタートする.
   *
   * @param userInfo リモートのユーザー情報
   * @return スタートしたプロセスのオブジェクト. スタートに失敗した場合null.
   */
  private ChannelExec startRuntimeProcess(UserInfoImpl userInfo) {
    ChannelExec channel = null;
    try {
      String startCmd = genStartCmd(userInfo.getHost());
      if (startCmd == null) {
        throw new Exception();
      }
      channel = execCmd(startCmd, userInfo);
      if (channel == null) {
        throw new Exception();
      }
    } catch (Exception e) {
      LogManager.logger().error(
          "Failed to start %s.\n%s".formatted(BhConstants.BhRuntime.BH_PROGRAM_RUNTIME_JAR, e));
    }
    return channel;
  }

  /**
   * BhProgram 実行環境開始のコマンドを生成する.
   *
   * @return BhProgram 実行環境開始のコマンド. コマンドの生成に失敗した場合 null.
   */
  private String genStartCmd(String host) {
    Context cx = Context.enter();
    Scriptable scope = cx.initStandardObjects();
    scope.put(BhConstants.JsIdName.IP_ADDR, scope, Context.javaToJS(host, scope));
    try {
      return (String) startCmdGenerator.exec(cx, scope);
    } catch (Exception e) {
      LogManager.logger().error(String.format(
          "Failed to generate BhProgram start command  (%s).\n%s",
          BhConstants.Path.REMOTE_EXEC_CMD_GENERATOR_JS, e));
    } finally {
      Context.exit();
    }
    return null;
  }

  /**
   * BhProgram実行環境終了のコマンドを生成する.
   *
   * @return BhProgram実行環境終了のコマンド. コマンドの生成に失敗した場合null.
   */
  private String genKillCmd() {
    Context cx = Context.enter();
    try {
      return (String) killCmdGenerator.exec(cx, cx.initStandardObjects());
    } catch (Exception e) {
      LogManager.logger().error(String.format(
          "Failed to generate BhProgram kill command  (%s).\n%s",
          BhConstants.Path.REMOTE_KILL_CMD_GENERATOR_JS, e));
    } finally {
      Context.exit();
    }
    return null;
  }

  /**
   * SSH 接続しリモートでコマンドを実行する.
   *
   * @param userInfo 接続先の情報
   * @param cmd 実行するコマンド
   * @return コマンドを実行中のチャンネル. コマンド実行に失敗した場合はnull
   */
  private ChannelExec execCmd(String cmd, UserInfoImpl userInfo) {
    JSch jsch = new JSch();
    ChannelExec channel = null;
    try {
      Session session = jsch.getSession(
          userInfo.getUname(),
          userInfo.getHost(),
          BhConstants.BhRuntime.SSH_PORT);
      session.setUserInfo(userInfo);
      session.connect();
      channel = (ChannelExec) session.openChannel("exec");
      channel.setInputStream(null);
      channel.setCommand(cmd);
      channel.connect();
    } catch (JSchException e) {
      LogManager.logger().error(
          "Failed to execute a cmd remotely (%s).\n%s".formatted(cmd, e));
      channel = null;
    }
    return channel;
  }

  /**
   * リモート環境にファイルをコピーする.
   *
   * @param host リモート環境のホスト名
   * @param uname リモート環境のユーザー名
   * @param password ユーザーのパスワード
   * @param srcPath 転送元のファイルパス
   * @param destPath 転送先のファイルパス
   * @return コピーが正常に終了した場合true
   */
  private boolean copyFile(
      String host,
      String uname,
      String password,
      String srcPath,
      String destPath) {
    msgService.info(TextDefs.BhRuntime.Remote.transferring.get());
    fileCopyIsCancelled.set(false);
    JSch jsch = new JSch();
    try {
      Session session = jsch.getSession(uname, host, BhRuntime.SSH_PORT);
      UserInfo ui = new UserInfoImpl(host, uname, password, msgService);
      session.setUserInfo(ui);
      session.connect();
      ChannelSftp channel = (ChannelSftp) session.openChannel("sftp");
      channel.connect();
      var monitor = new SftpProgressMonitorImpl(fileCopyIsCancelled, msgService);
      channel.put(srcPath, destPath, monitor, ChannelSftp.OVERWRITE);
      if (monitor.isFileCopyCancelled()) {
        throw new Exception("File transfer has been cancelled.");
      }
    } catch (Exception e) {
      LogManager.logger().error(e.toString());
      msgService.error(TextDefs.BhRuntime.Remote.failedToTransfer.get(e));
      return false;
    }
    return true;
  }

  /**
   * 現在実行中のプログラムを止めるかどうかを訪ねる.
   * プログラムを実行中でない場合は何も尋ねない.
   *
   * @retval YES プログラムを止める
   * @retval NO プログラムを止めない
   * @retval CANCEL キャンセルを選択
   * @retval NONE_OF_THEM 何も尋ねなかった場合
   */
  public ExclusiveSelection askIfStopProgram() {
    if (programRunning.get()) {
      Optional<ButtonType> selected = msgService.alert(
          AlertType.CONFIRMATION,
          TextDefs.BhRuntime.Remote.AskIfStop.title.get(),
          null,
          TextDefs.BhRuntime.Remote.AskIfStop.body.get(),
          ButtonType.YES, ButtonType.NO, ButtonType.CANCEL);

      return selected.map((btnType) -> {
        if (btnType.equals(ButtonType.YES)) {
          return ExclusiveSelection.YES;
        } else if (btnType.equals(ButtonType.NO)) {
          return ExclusiveSelection.NO;
        } else {
          return ExclusiveSelection.CANCEL;
        }
      }).orElse(ExclusiveSelection.YES);
    }
    return ExclusiveSelection.NONE_OF_THEM;
  }

  /**
   * プログラムが実行中かどうか調べる.
   *
   * @return プログラムが実行中の場合 true
   */
  public boolean isProgramRunning() {
    return programRunning.get();
  }

  /**
   * SSH チャンネルが閉じるのを待つ.
   *
   * @param channel 閉じるのを待つチャンネル
   * @param timeout タイムアウト(sec)
   * @return チャンネルで実行していたコマンドの終了コード. チャンネルのクローズに失敗した場合 Optiomal.empty.
   */
  private Optional<Integer> waitForChannelClosed(Channel channel, int timeout) {
    long begin = System.currentTimeMillis();
    try {
      while (true) {
        if (channel.isClosed()) {
          break;
        }
        channel.getInputStream().read();
        if ((System.currentTimeMillis() - begin) > (timeout * 1000)) {
          throw new Exception("timeout");
        }
      }
    } catch (Exception e) {
      msgService.error("channel close err " + e);
      return Optional.empty();
    }
    return Optional.of(channel.getExitStatus());
  }

  /**
   * 終了処理をする.
   *
   * @param terminate 実行中のプログラムを終了する場合true
   * @return 終了処理が正常に完了した場合 true
   */
  public boolean end(boolean terminate) {
    boolean success = true;
    if (programRunning.get()) {
      if (terminate) {
        terminate(BhConstants.BhRuntime.REMOTE_RUNTIME_TERMINATION_TIMEOUT);
      } else {
        helper.haltTransceiver();
      }
    }
    return success;
  }
}
