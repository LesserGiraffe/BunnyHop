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
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicReference;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.control.ButtonType;
import net.seapanda.bunnyhop.bhprogram.common.message.BhProgramMessage;
import net.seapanda.bunnyhop.common.BhConstants;
import net.seapanda.bunnyhop.common.BhConstants.BhRuntime;
import net.seapanda.bunnyhop.common.ExclusiveSelection;
import net.seapanda.bunnyhop.common.TextDefs;
import net.seapanda.bunnyhop.service.BhService;
import net.seapanda.bunnyhop.simulator.SimulatorCmdProcessor;
import org.mozilla.javascript.Context;
import org.mozilla.javascript.Script;
import org.mozilla.javascript.Scriptable;

/**
 * リモート環境で動作する BhRuntime 操作を行うクラス.
 *
 * @author K.Koike
 */
public class RemoteBhRuntimeManager {
  
  private final BhRuntimeManagerCommon common;
  private UserInfoImpl userInfo;
  /** プログラム実行中なら true. */
  private AtomicReference<Boolean> programRunning = new AtomicReference<>(false);
  /** ファイルがコピー中の場合 true. */
  private AtomicReference<Boolean> fileCopyIsCancelled = new AtomicReference<>(true);

  /** コンストラクタ. */
  RemoteBhRuntimeManager(SimulatorCmdProcessor simCmdProcessor) 
      throws IllegalStateException {

    common = new BhRuntimeManagerCommon(simCmdProcessor);
    boolean success = BhService.bhScriptManager().allExist(
        getClass().getSimpleName(),
        BhConstants.Path.REMOTE_EXEC_CMD_GENERATOR_JS,
        BhConstants.Path.REMOTE_KILL_CMD_GENERATOR_JS);

    if (!success) {
      String msg = "Cannot find remote cmd scripts";
      BhService.msgPrinter().errForDebug(msg);
      throw new IllegalStateException(msg);
    }
  }

  /**
   * BhProgramを実行する.
   *
   * @param filePath BhProgramのファイルパス
   * @param ipAddr BhProgramを実行するマシンのIPアドレス
   * @param uname BhProgramを実行するマシンにログインする際のユーザ名
   * @param password BhProgramを実行するマシンにログインする際のパスワード
   * @return BhProgram実行開始の完了待ちオブジェクト
   */
  public Future<Boolean> executeAsync(Path filePath, String ipAddr, String uname, String password) {
    boolean sel = true;
    if (userInfo != null && !userInfo.isSameAccessPoint(ipAddr, uname)) {
      switch (askIfStopProgram()) {
        case NO:
          sel = false;  //実行環境が現在のものと違ってかつ, プログラムを止めないが選択された場合
          break;
        case CANCEL:
          return common.executeAsync(() -> false);
        default:
          break;
      }
    }
    boolean terminate = sel;
    return common.executeAsync(() -> execute(filePath, ipAddr, uname, password, terminate));
  }


  /**
   * BhProgramを実行する.
   *
   * @param filePath BhProgramのファイルパス
   * @param ipAddr BhProgramを実行するマシンのIPアドレス
   * @param uname BhProgramを実行するマシンにログインする際のユーザ名
   * @param terminate 現在実行中のプログラムを停止する場合 true. 切断だけする場合 false.
   * @param password BhProgramを実行するマシンにログインする際のパスワード
   * @return BhProgramの実行処理でエラーが発生しなかった場合 true
   */
  private synchronized boolean execute(
      Path filePath,
      String ipAddr,
      String uname,
      String password,
      boolean terminate) {
    terminateOrDisconnect(terminate);

    BhService.msgPrinter().infoForUser(TextDefs.BhRuntime.Remote.preparingToRun.get());
    try {
      var destPath = Paths.get(
          BhConstants.Path.REMOTE_BUNNYHOP_DIR,
          BhConstants.Path.REMOTE_COMPILED_DIR,
          BhConstants.Path.APP_FILE_NAME_JS);
      boolean success = copyFile(ipAddr, uname, password, filePath.toString(), destPath.toString());
      if (!success) {
        throw new Exception();
      }
      userInfo = new UserInfoImpl(ipAddr, uname, password);
      ChannelExec channel = startRuntimeProcess(userInfo);  // リモート実行環境起動
      if (channel == null) {
        throw new Exception();
      }
      String fileName = filePath.getFileName().toString();
      success = common.runBhProgram(fileName, ipAddr, channel.getInputStream());  // BhProgram 実行
      // チャンネルは開いたままだが切断する
      channel.disconnect();
      channel.getSession().disconnect();
      if (!success) {
        throw new Exception();
      }
    } catch (Exception e) {
      BhService.msgPrinter().errForUser(TextDefs.BhRuntime.Remote.failedToRun.get());
      BhService.msgPrinter().errForDebug(
          "Failed to run %s. (remote)".formatted(filePath.getFileName()));
      terminate(BhConstants.BhRuntime.REMOTE_RUNTIME_TERMINATION_TIMEOUT_SHORT);
      return false;
    }
    BhService.msgPrinter().infoForUser(TextDefs.BhRuntime.Remote.startToRun.get());
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
        common.haltTransceiver();
        programRunning.set(false);
      }
    }
  }

  /**
   * 現在実行中の BhProgram を強制終了する.
   *
   * @return 強制終了タスクの結果. タスクを実行しなかった場合 Optional.empty.
   */
  public Future<Boolean> terminateAsync() {
    fileCopyIsCancelled.set(true);
    if (!programRunning.get()) {
      BhService.msgPrinter().errForUser(TextDefs.BhRuntime.Remote.hasAlreadyEnded.get());
      return common.terminateAsync(() -> false);
    }
    return common.terminateAsync(
      () -> terminate(BhConstants.BhRuntime.REMOTE_RUNTIME_TERMINATION_TIMEOUT));
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
  private synchronized boolean terminate(int timeout) {
    BhService.msgPrinter().infoForUser(TextDefs.BhRuntime.Remote.preparingToEnd.get());
    try {
      boolean success = common.haltTransceiver();
      if (!success) {
        throw new Exception();
      }
      String killCmd = genKillCmd();
      if (killCmd == null) {
        throw new Exception();
      }
      ChannelExec channel = execCmd(killCmd, new UserInfoImpl(userInfo));
      if (channel == null) {
        throw new Exception();
      }
      Optional<Integer> status = waitForChannelClosed(channel, timeout);
      int statusCode = status.orElseThrow(() -> new Exception());
      if (statusCode != 0) {
        BhService.msgPrinter().errForDebug("terminate status err  (%s)".formatted(statusCode));
        throw new Exception("");
      }
    } catch (Exception e) {
      BhService.msgPrinter().errForUser(TextDefs.BhRuntime.Remote.failedToEnd.get());
      return false;
    }
    BhService.msgPrinter().infoForUser(TextDefs.BhRuntime.Remote.hasTeminated.get());
    programRunning.set(false);
    return true;
  }

  /**
   * BhProgram の実行環境と通信を行うようにする.
   *
   * @return 接続タスクのFutureオブジェクト. タスクを実行しなかった場合null.
   */
  public Future<Boolean> connectAsync() {
    return common.connectAsync();
  }

  /**
   * BhProgram の実行環境と通信を行わないようにする.
   *
   * @return 切断タスクのFutureオブジェクト. タスクを実行しなかった場合null.
   */
  public Future<Boolean> disconnectAsync() {
    return common.disconnectAsync();
  }

  /**
   * 引数で指定した {@link BhProgramMessage} を BhProgram の実行環境に送る.
   *
   * @param msg 送信データ
   * @return ステータスコード
   */
  public BhRuntimeStatus sendAsync(BhProgramMessage msg) {
    return common.sendAsync(msg);
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
      BhService.msgPrinter().errForDebug(
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
    Script cs = BhService.bhScriptManager().getCompiledScript(
        BhConstants.Path.REMOTE_EXEC_CMD_GENERATOR_JS);
    Context cx = Context.enter();
    Scriptable scope = cx.initStandardObjects();
    scope.put(BhConstants.JsIdName.IP_ADDR, scope, Context.javaToJS(host, scope));
    try {
      return (String) cs.exec(cx, scope);
    } catch (Exception e) {
      BhService.msgPrinter().errForDebug(String.format(
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
    Script cs = BhService.bhScriptManager().getCompiledScript(
        BhConstants.Path.REMOTE_KILL_CMD_GENERATOR_JS);
    Context cx = Context.enter();
    try {
      return (String) cs.exec(cx, cx.initStandardObjects());
    } catch (Exception e) {
      BhService.msgPrinter().errForDebug(String.format(
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
      BhService.msgPrinter().errForDebug(
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

    BhService.msgPrinter().infoForUser(TextDefs.BhRuntime.Remote.transferring.get());
    fileCopyIsCancelled.set(false);
    JSch jsch = new JSch();
    try {
      Session session = jsch.getSession(uname, host, BhRuntime.SSH_PORT);
      UserInfo ui = new UserInfoImpl(host, uname, password);
      session.setUserInfo(ui);
      session.connect();
      ChannelSftp channel = (ChannelSftp) session.openChannel("sftp");
      channel.connect();
      SftpProgressMonitorImpl monitor = new SftpProgressMonitorImpl(fileCopyIsCancelled);
      channel.put(srcPath, destPath, monitor, ChannelSftp.OVERWRITE);
      if (monitor.isFileCopyCancelled()) {
        throw new Exception("File transfer has been cancelled.");
      }
    } catch (Exception e) {
      BhService.msgPrinter().errForDebug(e.toString());
      BhService.msgPrinter().errForUser(TextDefs.BhRuntime.Remote.failedToTransfer.get(e));
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
      Optional<ButtonType> selected = BhService.msgPrinter().alert(
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
   * SSH チャンネルが閉じるのを待つ.
   *
   * @param channel 閉じるのを待つチャンネル
   * @param timeout タイムアウト(sec)
   * @return チャンネルで実行していたコマンドの終了コード. チャンネルのクローズに失敗した場合 Optiomal.empty.
   * */
  private static Optional<Integer> waitForChannelClosed(Channel channel, int timeout) {
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
      BhService.msgPrinter().errForUser("channel close err " + e);
      return Optional.empty();
    }
    return Optional.of(channel.getExitStatus());
  }

  /**
   * 終了処理をする.
   *
   * @param terminate 実行中のプログラムを終了する場合true
   * @return 終了処理が正常に完了した場合true
   */
  public boolean end(boolean terminate) {
    boolean success = true;
    if (programRunning.get()) {
      if (terminate) {
        success = terminate(BhConstants.BhRuntime.REMOTE_RUNTIME_TERMINATION_TIMEOUT);
      } else {
        success = common.haltTransceiver();
      }
    }
    success &= common.end();
    return success;
  }
}
