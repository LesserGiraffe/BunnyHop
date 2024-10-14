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
import net.seapanda.bunnyhop.common.constant.BhConstants;
import net.seapanda.bunnyhop.common.constant.BhConstants.BhRuntime;
import net.seapanda.bunnyhop.common.constant.ExclusiveSelection;
import net.seapanda.bunnyhop.common.tools.MsgPrinter;
import net.seapanda.bunnyhop.common.tools.Util;
import net.seapanda.bunnyhop.configfilereader.BhScriptManager;
import net.seapanda.bunnyhop.simulator.SimulatorCmdProcessor;
import org.mozilla.javascript.ContextFactory;
import org.mozilla.javascript.Script;
import org.mozilla.javascript.Scriptable;
import org.mozilla.javascript.ScriptableObject;

/**
 * BhProgram のリモート環境での実行、終了、通信を行うクラス.
 *
 * @author K.Koike
 */
public class RemoteBhProgramManager {
  
  private final BhProgramManagerCommon common;
  private UserInfoImpl userInfo;
  /** プログラム実行中なら true. */
  private AtomicReference<Boolean> programRunning = new AtomicReference<>(false);
  /** ファイルがコピー中の場合 true. */
  private AtomicReference<Boolean> fileCopyIsCancelled = new AtomicReference<>(true);

  /** コンストラクタ. */
  RemoteBhProgramManager(SimulatorCmdProcessor simCmdProcessor) 
      throws IllegalStateException {

    common = new BhProgramManagerCommon(simCmdProcessor);
    boolean success = BhScriptManager.INSTANCE.scriptsExist(
        getClass().getSimpleName(),
        BhConstants.Path.REMOTE_EXEC_CMD_GENERATOR_JS,
        BhConstants.Path.REMOTE_KILL_CMD_GENERATOR_JS);

    if (!success) {
      String msg = "connot find remote cmd scripts";
      MsgPrinter.INSTANCE.errMsgForDebug(msg + " " + getClass().getSimpleName());
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
    MsgPrinter.INSTANCE.msgForUser("-- プログラム実行準備中 (remote) --\n");
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
      ChannelExec channel = startRuntimeProcess(userInfo);  //リモート実行環境起動
      if (channel == null) {
        throw new Exception();
      }
      String fileName = filePath.getFileName().toString();
      success = common.runBhProgram(fileName, ipAddr, channel.getInputStream());  //BhProgram実行
      //チャンネルは開いたままだが切断する
      channel.disconnect();
      channel.getSession().disconnect();
      if (!success) {
        throw new Exception();
      }
    } catch (Exception e) {
      MsgPrinter.INSTANCE.errMsgForUser("!! プログラム実行準備失敗 (remote) !!\n");
      MsgPrinter.INSTANCE.errMsgForDebug("failed to run " + filePath.getFileName() + " (remote)");
      terminate(BhConstants.BhRuntime.REMOTE_RUNTIME_TERMINATION_TIMEOUT_SHORT);
      return false;
    }
    MsgPrinter.INSTANCE.msgForUser("-- プログラム実行開始 (remote) --\n");
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
      MsgPrinter.INSTANCE.errMsgForUser("!! プログラム終了済み (remote) !!\n");
      return common.terminateAsync(() -> false);
    }
    return common.terminateAsync(
      () -> terminate(BhConstants.BhRuntime.REMOTE_RUNTIME_TERMINATION_TIMEOUT));
  }

  /**
   * <pre>
   * リモートマシンで実行中のBhProgram実行環境を強制終了する.
   * BhProgram実行環境を終了済みの場合に呼んでも問題ない.
   * </pre>
   *
   * @param timeout タイムアウト
   * @return 強制終了に成功した場合true
   */
  private synchronized boolean terminate(int timeout) {
    MsgPrinter.INSTANCE.msgForUser("-- プログラム終了中 (remote)  --\n");
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
      if (status.isEmpty()) {
        throw new Exception();
      }
      if (status.get() != 0) {
        MsgPrinter.INSTANCE.errMsgForDebug("terminate status err " + status.get());
        throw new Exception("");
      }
    } catch (Exception e) {
      MsgPrinter.INSTANCE.errMsgForUser("!! プログラム終了失敗 (remote)  !!\n");
      return false;
    }
    MsgPrinter.INSTANCE.msgForUser("-- プログラム終了完了 (remote)  --\n");
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
      MsgPrinter.INSTANCE.errMsgForDebug(
          "failed to start " +  BhConstants.BhRuntime.BH_PROGRAM_RUNTIME_JAR + " " + e);
    }
    return channel;
  }

  /**
   * BhProgram実行環境開始のコマンドを生成する.
   *
   * @return BhProgram実行環境開始のコマンド. コマンドの生成に失敗した場合null.
   */
  private String genStartCmd(String host) {
    Script cs =
        BhScriptManager.INSTANCE.getCompiledScript(BhConstants.Path.REMOTE_EXEC_CMD_GENERATOR_JS);
    Scriptable scope = BhScriptManager.INSTANCE.createScriptScope();
    ScriptableObject.putProperty(scope, BhConstants.JsKeyword.KEY_IP_ADDR, host);
    Object retVal = null;
    try {
      retVal = ContextFactory.getGlobal().call(cx -> cs.exec(cx, scope));
    } catch (Exception e) {
      MsgPrinter.INSTANCE.errMsgForDebug(
          "failed to eval " +  BhConstants.Path.REMOTE_EXEC_CMD_GENERATOR_JS + " " + e);
      return null;
    }
    if (retVal instanceof String) {
      return (String) retVal;
    }
    return null;
  }

  /**
   * BhProgram実行環境終了のコマンドを生成する.
   *
   * @return BhProgram実行環境終了のコマンド. コマンドの生成に失敗した場合null.
   */
  private String genKillCmd() {
    Script cs =
        BhScriptManager.INSTANCE.getCompiledScript(BhConstants.Path.REMOTE_KILL_CMD_GENERATOR_JS);
    Object retVal;
    try {
      retVal = ContextFactory.getGlobal().call(
          cx -> cs.exec(cx, BhScriptManager.INSTANCE.createScriptScope()));
    } catch (Exception e) {
      MsgPrinter.INSTANCE.errMsgForDebug(
          "failed to eval" +  BhConstants.Path.REMOTE_KILL_CMD_GENERATOR_JS + " " + e);
      return null;
    }
    if (retVal instanceof String) {
      return (String) retVal;
    }
    return null;
  }

  /**
   * SSH接続しリモートでコマンドを実行する.
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
      MsgPrinter.INSTANCE.errMsgForDebug(
          "failed to exec cmd remotely (" + cmd + ")  " + e);
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

    MsgPrinter.INSTANCE.msgForUser("-- プログラム転送中 --\n");
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
        throw new Exception("file transfer has been cancelled");
      }
    } catch (Exception e) {
      MsgPrinter.INSTANCE.errMsgForDebug(
          Util.INSTANCE.getCurrentMethodName() + "\n" + e + "\n");
      MsgPrinter.INSTANCE.errMsgForUser("!! プログラム転送失敗 !!\n  " + e + "\n");
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
      Optional<ButtonType> selected = MsgPrinter.INSTANCE.alert(
          AlertType.CONFIRMATION,
          "プログラム停止の確認",
          null,
          "現在実行しているプログラムを停止しますか?",
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
   * SSHチャンネルが閉じるのを待つ.
   *
   * @param channel 閉じるのを待つチャンネル
   * @param timeout タイムアウト(sec)
   * @return チャンネルで実行していたコマンドの終了コード. チャンネルのクローズに失敗した場合 Optiomal.empty.
   * */
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
      MsgPrinter.INSTANCE.errMsgForUser("channel close err " + e);
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
