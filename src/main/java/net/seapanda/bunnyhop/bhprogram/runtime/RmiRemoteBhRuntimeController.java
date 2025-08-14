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

package net.seapanda.bunnyhop.bhprogram.runtime;

import com.jcraft.jsch.Channel;
import com.jcraft.jsch.ChannelExec;
import com.jcraft.jsch.ChannelSftp;
import com.jcraft.jsch.JSch;
import com.jcraft.jsch.JSchException;
import com.jcraft.jsch.Session;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.file.Path;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.locks.ReentrantLock;
import net.seapanda.bunnyhop.bhprogram.BhRuntimeController;
import net.seapanda.bunnyhop.bhprogram.common.BhRuntimeFacade;
import net.seapanda.bunnyhop.bhprogram.common.message.BhProgramMessage;
import net.seapanda.bunnyhop.common.BhConstants;
import net.seapanda.bunnyhop.common.TextDefs;
import net.seapanda.bunnyhop.service.BhScriptRepository;
import net.seapanda.bunnyhop.service.LogManager;
import net.seapanda.bunnyhop.service.MessageService;
import net.seapanda.bunnyhop.utility.function.ConsumerInvoker;
import org.mozilla.javascript.Context;
import org.mozilla.javascript.Script;
import org.mozilla.javascript.Scriptable;

/**
 * リモート環境で動作する BhRuntime 操作を行うクラス.
 *
 * @author K.Koike
 */
public class RmiRemoteBhRuntimeController implements RemoteBhRuntimeController {
  
  private final MessageService msgService;
  private final CallbackRegistryImpl cbRegistry = new CallbackRegistryImpl();
  /** BhRuntime との通信用オブジェクト. */
  private BhRuntimeTransceiver transceiver;
  /** 現在の接続先に関する情報. */
  private volatile DestinationInfo currentDestInfo;
  /** ファイルがコピーのキャンセルフラグ. */
  private final AtomicBoolean fileCopyIsCancelled = new AtomicBoolean(true);
  private final CmdGenScripts scripts;
  private final ReentrantLock lock = new ReentrantLock();

  /** コンストラクタ. */
  public RmiRemoteBhRuntimeController(
      MessageService msgService,
      BhScriptRepository repository) 
      throws IllegalStateException {
    this.msgService = msgService;
    boolean success = repository.allExist(
        BhConstants.Path.File.GEN_REMOTE_EXEC_CMD_JS,
        BhConstants.Path.File.GEN_REMOTE_KILL_CMD_JS,
        BhConstants.Path.File.GEN_GET_PORT_CMD_JS,
        BhConstants.Path.File.GEN_REMOTE_DEST_PATH_JS);
    if (!success) {
      String msg = "Cannot find remote cmd scripts";
      LogManager.logger().error(msg);
      throw new IllegalStateException(msg);
    }
    scripts = new CmdGenScripts(
        repository.getScript(BhConstants.Path.File.GEN_REMOTE_EXEC_CMD_JS),
        repository.getScript(BhConstants.Path.File.GEN_REMOTE_KILL_CMD_JS),
        repository.getScript(BhConstants.Path.File.GEN_GET_PORT_CMD_JS),
        repository.getScript(BhConstants.Path.File.GEN_REMOTE_DEST_PATH_JS));
  }

  @Override
  public boolean start(Path filePath, String hostname, String uname, String password) {
    if (!lock.tryLock()) {
      return false;
    }
    try {
      var userInfo = new UserInfoImpl(hostname, uname, password, msgService);
      Session session = establishSshSession(userInfo).orElse(null);
      if (session != null) {
        boolean success = execute(session, filePath);
        session.disconnect();
        return success;
      }
      return false;
    } finally {
      lock.unlock();
    }
  }

  /**
   * SSH セッションを確立する.
   *
   * @param userInfo 接続先の情報を格納したオブジェクト
   * @return セッションを確率済みの {@link Session} オブジェクト.
   */
  private Optional<Session> establishSshSession(UserInfoImpl userInfo) {
    try {
      Session session = new JSch().getSession(
          userInfo.getUname(), userInfo.getHost(), BhConstants.BhRuntime.SSH_PORT);
      session.setUserInfo(userInfo);
      session.setConfig("StrictHostKeyChecking", "no");
      session.connect(BhConstants.BhRuntime.Timeout.SSH_CONNECTION);
      return Optional.of(session);
    } catch (JSchException e) {
      msgService.error(TextDefs.BhRuntime.Communication.failedToEstablishConnection.get());
      LogManager.logger().error("Failed to establiesh SSH session.\n%s".formatted(e));
    }
    return Optional.empty();
  }

  /**
   * BhProgram を実行する.
   *
   * @param filePath このパスのファイルをリモート環境にコピーして実行する
   * @param session SSH セッションオブジェクト
   * @return 成功した場合 true, 失敗した場合 false
   */
  private boolean execute(Session session, Path filePath) {
    msgService.info(TextDefs.BhRuntime.Remote.preparingToRun.get());
    try {
      disconnectImpl(0);
      String destPath = genCopyDestPath(session.getUserName()).orElseThrow();
      BhRuntimeFacade facade = prepareRemoteEnvironment(session, filePath, destPath).orElseThrow();
      var oldCarrier = (transceiver == null) ? null : transceiver.getMessageCarrier();
      transceiver = new BhRuntimeTransceiver(facade);
      currentDestInfo = new DestinationInfo(
          session.getHost(),
          session.getUserName(),
          session.getUserInfo().getPassword(),
          transceiver);
      var event = new MessageCarrierRenewedEvent(this, oldCarrier, transceiver.getMessageCarrier());
      cbRegistry.onMsgCarrierRenewed.invoke(event);
      transceiver.start();
      boolean success = transceiver.connect();
      if (success) {
        // BhProgram の開始前に実行したい処理に対応するため, イベントハンドラをここで呼ぶ
        cbRegistry.onConnCondChanged.invoke(new ConnectionEvent(this, true));
        success = BhRuntimeHelper.runScript(destPath, facade);
      }
      if (success) {
        msgService.info(TextDefs.BhRuntime.Remote.hasStarted.get());
        return true;
      }
      throw new Exception();
    } catch (Exception e) {
      disconnectImpl(0);
      msgService.error(TextDefs.BhRuntime.Remote.failedToRun.get());
      LogManager.logger().error("Failed to run %s. (remote)".formatted(filePath.getFileName()));
    }
    return false;
  }

  /**
   * リモート環境の準備をする.
   *
   * @return BhRuntime との通信用オブジェクト.
   */
  private Optional<BhRuntimeFacade> prepareRemoteEnvironment(
      Session session, Path filePath, String destPath) {
    boolean success = copyFile(session, filePath.toString(), destPath);
    if (!success) {
      return Optional.empty();
    }
    return startBhRuntime(session);
  }

  /**
   * BhRuntime をスタートする.
   *
   * @param session SSH セッションオブジェクト
   * @return BhRuntime との通信用オブジェクト.
   */
  private Optional<BhRuntimeFacade> startBhRuntime(Session session) {
    try {
      String startCmd = genStartCmd(session.getHost()).orElseThrow();
      CmdResultProvider provider = execCmd(session, startCmd).orElseThrow();
      Optional<BhRuntimeFacade> facade = getBhRuntimeFacade(
          provider.inputStream(), session.getHost(), BhConstants.BhRuntime.Timeout.REMOTE_START);
      provider.channel().disconnect();
      return facade;
    } catch (Exception e) {
      return Optional.empty();
    }
  } 

  @Override
  public boolean terminate(String hostname, String uname, String password) {
    fileCopyIsCancelled.set(true);
    if (!lock.tryLock()) {
      return false;
    }
    try {
      var userInfo = new UserInfoImpl(hostname, uname, password, msgService);
      Session session = establishSshSession(userInfo).orElse(null);
      if (session != null) {
        boolean success = terminate(session);
        session.disconnect();
        return success;
      }
      return false;
    } finally {
      lock.unlock();
    }
  }

  /**
   * リモートマシンで実行中の BhProgram 実行環境を終了する.
   *
   * @return 終了に成功した場合 true
   */
  private boolean terminate(Session session) {
    msgService.info(TextDefs.BhRuntime.Remote.preparingToEnd.get());
    try {
      disconnectImpl(0);
      String killCmd = genKillCmd().orElseThrow();
      CmdResultProvider provider = execCmd(session, killCmd).orElseThrow();
      Optional<Integer> status = waitForChannelClosed(
          provider.channel(), BhConstants.BhRuntime.Timeout.REMOTE_TERMINATE);
      provider.channel().disconnect();
      int statusCode = status.orElseThrow();
      if (statusCode == 0) {
        msgService.info(TextDefs.BhRuntime.Remote.hasEnded.get());
        return true;
      }
      throw new Exception();
    } catch (Exception e) {
      msgService.error(TextDefs.BhRuntime.Remote.failedToEnd.get());
      LogManager.logger().error("Failed to terminate BhRuntime. (remote)");
    }
    return false;
  }

  @Override
  public boolean connect(String hostname, String uname, String password) {
    if (!lock.tryLock()) {
      return false;
    }
    if (isCurrentDestSameAs(hostname, uname)) {
      lock.unlock();
      return true;
    }
    msgService.info(TextDefs.BhRuntime.Remote.preparingToConnect.get());
    try {
      // 実行 -> 終了 (失敗) -> 接続 と操作したときに, 終了操作で停止したトランシーバが
      // データを受信するのを避けるため, 接続時にトランシーバのタスク終了を待つ必要がある.
      disconnectImpl(BhConstants.BhRuntime.Timeout.HALT_TRANSCEIVER);      
      var userInfo = new UserInfoImpl(hostname, uname, password, msgService);
      Session session = establishSshSession(userInfo).orElse(null);
      if (session != null) {
        boolean success = connect(session);
        session.disconnect();
        return success;
      }
    } finally {
      lock.unlock();
    }
    return false;
  }

  /** BhRuntime に接続する. */
  private boolean connect(Session session) {
    try {
      String cmd = genGetRuntimePortCmd().orElseThrow();
      CmdResultProvider provider = execCmd(session, cmd).orElseThrow();
      BhRuntimeFacade facade = getBhRuntimeFacade(
          provider.inputStream(), session.getHost(), BhConstants.BhRuntime.Timeout.REMOTE_CONNECT)
          .orElseThrow();
      provider.channel().disconnect();
      BhRuntimeTransceiver oldTransceiver = transceiver;
      transceiver = new BhRuntimeTransceiver(facade);
      currentDestInfo = new DestinationInfo(
          session.getHost(),
          session.getUserName(),
          session.getUserInfo().getPassword(),
          transceiver);
      var event = new MessageCarrierRenewedEvent(
          this, oldTransceiver.getMessageCarrier(), transceiver.getMessageCarrier());
      cbRegistry.onMsgCarrierRenewed.invoke(event);
      transceiver.start();
      boolean success = transceiver.connect();
      if (success) {
        cbRegistry.onConnCondChanged.invoke(new ConnectionEvent(this, true));
        msgService.info(TextDefs.BhRuntime.Remote.hasConnected.get());
        return true;
      }
      throw new Exception();
    } catch (Exception e) {
      disconnectImpl(0);
      msgService.error(TextDefs.BhRuntime.Remote.failedToConnect.get());
      LogManager.logger().error("Failed to terminate BhRuntime. (remote)");
    }
    return false;
  }

  @Override
  public boolean disconnect() {
    if (!lock.tryLock()) {
      return false;
    }
    try {
      boolean success = disconnectImpl(BhConstants.BhRuntime.Timeout.HALT_TRANSCEIVER);
      if (success) {
        msgService.info(TextDefs.BhRuntime.Remote.hasDisconnected.get());
      } else {
        msgService.error(TextDefs.BhRuntime.Remote.failedToDisconnect.get());
      }
      return success;
    } finally {
      lock.unlock();
    }
  }

  /**
   * 現在接続中の BhRuntime から切断し, 接続先の情報を捨てる.
   *
   * @param timeout トランシーバの終了処理のタイムアウト時間 (ms)
   */
  private boolean disconnectImpl(int timeout) {
    if (currentDestInfo == null) {
      return true;
    }
    boolean success = currentDestInfo.transceiver().halt(timeout);
    currentDestInfo = null;
    cbRegistry.onConnCondChanged.invoke(new ConnectionEvent(this, true));
    return success;
  }

  @Override
  public BhRuntimeStatus send(BhProgramMessage message) {
    if (!lock.tryLock()) {
      return BhRuntimeStatus.BUSY;
    }
    try {
      if (currentDestInfo == null) {
        return BhRuntimeStatus.SEND_WHEN_DISCONNECTED;
      }
      return currentDestInfo.transceiver().getMessageCarrier().pushMessage(message);
    } finally {
      lock.unlock();
    }
  }

  /** BhRuntime との通信用オブジェクトを取得する. */
  private Optional<BhRuntimeFacade> getBhRuntimeFacade(
      InputStream is, String hostname, int timeout) {
    msgService.info(TextDefs.BhRuntime.Communication.preparingToCommunicate.get());
    BhRuntimeFacade facade = null;
    try (var br = new BufferedReader(new InputStreamReader(is))) {
      facade = BhRuntimeHelper.getBhRuntimeFacade(hostname, br, timeout);
    } catch (Exception e) {
      LogManager.logger().error("Failed to get BhRuntime facade.\n%s".formatted(e));
    }
    return Optional.ofNullable(facade);
  }

  /**
   * 現在接続中の BhRuntime が引数で指定したものと同じかどうかを調べる.
   *
   * @return 同じ場合 true, 異なる場合 false
   */
  private boolean isCurrentDestSameAs(String hostname, String uname) {
    if (currentDestInfo == null) {
      return false;
    }
    return currentDestInfo.hostname().equals(hostname) && currentDestInfo.uname().equals(uname);
  }
 
  /**
   * BhRuntime を起動するコマンドを生成する.
   *
   * @return BhRuntime を起動するコマンド.
   */
  private Optional<String> genStartCmd(String host) {
    Context cx = Context.enter();
    Scriptable scope = cx.initStandardObjects();
    scope.put(BhConstants.JsIdName.IP_ADDR, scope, Context.javaToJS(host, scope));
    try {
      return Optional.ofNullable((String) scripts.start().exec(cx, scope));
    } catch (Exception e) {
      LogManager.logger().error(String.format(
          "Failed to generate BhProgram start command  (%s).\n%s",
          BhConstants.Path.File.GEN_REMOTE_EXEC_CMD_JS, e));
    } finally {
      Context.exit();
    }
    return Optional.empty();
  }

  /**
   * BhRuntime を終了するコマンドを生成する.
   *
   * @return BhRuntime を終了するコマンド. コマンドの生成に失敗した場合null.
   */
  private Optional<String> genKillCmd() {
    Context cx = Context.enter();
    try {
      return Optional.ofNullable((String) scripts.kill().exec(cx, cx.initStandardObjects()));
    } catch (Exception e) {
      LogManager.logger().error(String.format(
          "Failed to generate BhProgram kill command  (%s).\n%s",
          BhConstants.Path.File.GEN_REMOTE_KILL_CMD_JS, e));
    } finally {
      Context.exit();
    }
    return Optional.empty();
  }

  /**
   * BhProgram のコピー先のパスを生成する.
   *
   * @param userName リモートマシン上で BhProgram を実行するユーザ名
   * @return BhProgram のコピー先のパスを生成する.
   */
  private Optional<String> genCopyDestPath(String userName) {
    Context cx = Context.enter();
    Scriptable scope = cx.initStandardObjects();
    scope.put(BhConstants.JsIdName.UNAME, scope, Context.javaToJS(userName, scope));
    try {
      return Optional.ofNullable((String) scripts.destPath().exec(cx, scope));
    } catch (Exception e) {
      LogManager.logger().error(String.format(
          "Failed to generate the destination path to which BhProgram shuld be copied.\n%s", e));
    } finally {
      Context.exit();
    }
    return Optional.empty();
  }

  /**
   * BhRuntime の TCP ポートを取得するコマンドを生成する.
   *
   * @return BhRuntime の TCP ポートを取得するコマンド.
   */
  private Optional<String> genGetRuntimePortCmd() {
    Context cx = Context.enter();
    try {
      return Optional.ofNullable((String) scripts.getPort().exec(cx, cx.initStandardObjects()));
    } catch (Exception e) {
      LogManager.logger().error(String.format(
          "Failed to generate Get BhRuntime Port command  (%s).\n%s",
          BhConstants.Path.File.GEN_GET_PORT_CMD_JS, e));
    } finally {
      Context.exit();
    }
    return Optional.empty();
  }

  /**
   * SSH 接続しリモートでコマンドを実行する.
   *
   * @param session SSH セッションオブジェクト
   * @param cmd 実行するコマンド
   * @return 実行したコマンドの結果を提供するオブジェクト.
   */
  private Optional<CmdResultProvider> execCmd(Session session, String cmd) {
    try {
      ChannelExec channel = (ChannelExec) session.openChannel("exec");
      InputStream inputStream = channel.getInputStream();
      channel.setCommand(cmd);
      channel.connect(BhConstants.BhRuntime.Timeout.SSH_CONNECTION);
      return Optional.of(new CmdResultProvider(channel, inputStream));
    } catch (IOException | JSchException e) {
      LogManager.logger().error("Failed to execute a cmd remotely (%s).\n%s".formatted(cmd, e));
    }
    return Optional.empty();
  }

  /**
   * リモート環境にファイルをコピーする.
   *
   * @param session SSH セッションオブジェクト
   * @param srcPath 転送元のファイルパス
   * @param destPath 転送先のファイルパス
   * @return コピーが正常に終了した場合true
   */
  private boolean copyFile(Session session, String srcPath, String destPath) {
    msgService.info(TextDefs.BhRuntime.Remote.transferring.get());
    try {
      ChannelSftp channel = (ChannelSftp) session.openChannel("sftp");
      channel.connect();
      fileCopyIsCancelled.set(false);
      var monitor = new SftpProgressMonitorImpl(fileCopyIsCancelled, msgService);
      channel.put(srcPath, destPath, monitor, ChannelSftp.OVERWRITE);
      channel.disconnect();
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
   * プログラムが実行中かどうか調べる.
   *
   * @return プログラムが実行中の場合 true
   */
  public boolean isProgramRunning() {
    return currentDestInfo != null;
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
    try (var br = new BufferedReader(new InputStreamReader(channel.getInputStream()))) {
      while (!channel.isClosed()) {
        if (br.ready()) {
          br.read();
        }
        if ((System.currentTimeMillis() - begin) > timeout) {
          throw new Exception("timeout");
        }
      }
    } catch (Exception e) {
      LogManager.logger().error("channel close err " + e);
      return Optional.empty();
    }
    return Optional.of(channel.getExitStatus());
  }

  /**
   * 終了処理をする.
   *
   * @param terminate 実行中のプログラムを終了する場合 true
   * @param timeout 終了処理の開始を待つ時間 (sec).
   * @return 終了処理が正常に完了した場合 true
   */
  public boolean end(boolean terminate, int timeout) {
    try {
      if (!lock.tryLock(timeout, TimeUnit.MILLISECONDS)) {
        return false;
      }
      if (!isProgramRunning()) {
        return true;
      }
      if (terminate) {
        return terminate(
            currentDestInfo.hostname(),
            currentDestInfo.uname(),
            currentDestInfo.password());
      }
      return disconnectImpl(BhConstants.BhRuntime.Timeout.HALT_TRANSCEIVER);
    } catch (InterruptedException e) {
      return false;
    } finally {
      lock.unlock();
    }
  }

  @Override
  public CallbackRegistry getCallbackRegistry() {
    return cbRegistry;
  }

  /**
   * リモート環境の操作に必要なコマンドを生成するスクリプトを格納するレコード.
   *
   * @param start BhRuntime を起動するコマンドを生成するスクリプト
   * @param kill BhRuntime を終了するコマンドを生成するスクリプト
   * @param getPort BhRuntime の TCP ポートを取得するコマンドを生成するスクリプト
   * @param destPath BhProgram のコピー先パスを生成するスクリプト
   */
  private record CmdGenScripts(
      Script start,
      Script kill,
      Script getPort,
      Script destPath) {}

  /**
   * 現在の接続先に関する情報を格納するレコード.
   *
   * @param hostname ホスト名
   * @param uname ユーザ名
   * @param password ログインパスワード
   * @param transceiver 接続先との通信に使用する {@link BhRuntimeTransceiver}
   */
  private record DestinationInfo(
      String hostname, String uname, String password, BhRuntimeTransceiver transceiver) {}
  
  /** SSH 越しに実行したコマンドの結果を提供するオブジェクトを格納したレコード. */
  private record CmdResultProvider(Channel channel, InputStream inputStream) {}

  /** {@link BhRuntimeController} に対するイベントハンドラの登録および削除操作を提供するクラス. */
  public class CallbackRegistryImpl implements BhRuntimeController.CallbackRegistry {

    /** BhRuntime との通信用オブジェクトが置き換わったときのイベントハンドラをを管理するオブジェクト. */
    private final ConsumerInvoker<MessageCarrierRenewedEvent> onMsgCarrierRenewed =
        new ConsumerInvoker<>();

    /** BhRuntime との通信が有効または無効になったときのイベントハンドラをを管理するオブジェクト. */
    private final ConsumerInvoker<ConnectionEvent> onConnCondChanged = new ConsumerInvoker<>();

    @Override
    public ConsumerInvoker<MessageCarrierRenewedEvent>.Registry getOnMsgCarrierRenewed() {
      return onMsgCarrierRenewed.getRegistry();
    }

    @Override
    public ConsumerInvoker<ConnectionEvent>.Registry getOnConnectionConditionChanged() {
      return onConnCondChanged.getRegistry();
    }
  }
}
