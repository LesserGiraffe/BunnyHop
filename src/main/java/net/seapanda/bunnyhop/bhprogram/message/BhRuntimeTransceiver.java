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

package net.seapanda.bunnyhop.bhprogram.message;

import java.rmi.RemoteException;
import java.util.List;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;
import net.seapanda.bunnyhop.bhprogram.BhRuntimeStatus;
import net.seapanda.bunnyhop.bhprogram.common.BhRuntimeFacade;
import net.seapanda.bunnyhop.bhprogram.common.message.BhProgramNotification;
import net.seapanda.bunnyhop.bhprogram.common.message.BhProgramResponse;
import net.seapanda.bunnyhop.common.BhConstants;
import net.seapanda.bunnyhop.common.TextDefs;
import net.seapanda.bunnyhop.service.LogManager;
import net.seapanda.bunnyhop.service.MessageService;
import net.seapanda.bunnyhop.utility.SynchronizingTimer;

/**
 * BhProgram の実行環境と通信をするクラス.
 *
 * @author K.Koike
 */
public class BhRuntimeTransceiver {

  private static final AtomicInteger nextId = new AtomicInteger(0);
  /** 接続状態. */
  private final AtomicBoolean connected = new AtomicBoolean(false);
  /** BhProgram の実行環境に送信する {@link BhProgramNotification} を格納する FIFO. */
  private final BlockingQueue<BhProgramNotification> sendNotifList = 
      new ArrayBlockingQueue<>(BhConstants.BhRuntime.MAX_REMOTE_CMD_QUEUE_SIZE);
  /** BhProgram の実行環境に送信する {@link BhProgramResponse} を格納する FIFO. */
  private final BlockingQueue<BhProgramResponse> sendRespList =
      new ArrayBlockingQueue<>(BhConstants.BhRuntime.MAX_REMOTE_CMD_QUEUE_SIZE);
  /** BhProgram との通信用 {@link ExecutorService} のセット. */
  private ExecutorSet executors = new ExecutorSet(
      Executors.newSingleThreadExecutor(),
      Executors.newSingleThreadExecutor(),
      Executors.newSingleThreadExecutor(),
      Executors.newSingleThreadExecutor());
  private FutureSet futures;
  /** {@link BhProgramNotification} を受信したときに呼び出すメソッド. */
  private AtomicReference<Consumer<BhProgramNotification>> onNotifReceived =
      new AtomicReference<>(notif -> {});
  /** {@link BhProgramResponse} を受信したときに呼び出すメソッド. */
  private AtomicReference<Consumer<BhProgramResponse>> onRespReceived =
      new AtomicReference<>(resp -> {});
  private SynchronizingTimer connectionWait = new SynchronizingTimer(1, true);
  /**
   * BhProgramの実行環境と通信する用のRMIオブジェクト.
   * BhRuntimeFacade はリモート側の特定のプロセスと紐付いており, 
   * RMI Server が同じ TCP ポートでも新しく起動したプロセスと通信することはない.
   */
  private final BhRuntimeFacade runtimeFacade;
  /** アプリケーションユーザにメッセージを出力するためのオブジェクト. */
  private final MessageService msgService;
  public final int id;


  /**
   * コンストラクタ.
   *
   * @param facade BhProgram と BunnyHop 間でデータを送受信するオブジェクト
   * @param msgService アプリケーションユーザにメッセージを出力するためのオブジェクト.
   */
  public BhRuntimeTransceiver(BhRuntimeFacade facade, MessageService msgService) {
    this.runtimeFacade = facade;
    this.msgService = msgService;
    id = nextId.getAndIncrement();
  }

  /**
   * BhProgram の実行環境と通信を行うようにする.
   *
   * @return 接続に成功した場合 true
   */
  public synchronized boolean connect() {
    try {
      runtimeFacade.connect();
      connectionWait.reset(0);
    } catch (RemoteException e) {
      // 接続中に BhRuntime を kill した場合, ここで抜ける
      msgService.error(TextDefs.BhRuntime.Communication.failedToConnect.get());
      LogManager.logger().error("Failed to connect to BhRuntime.\n" + e);
      return false;
    }
    connected.set(true);
    msgService.info(TextDefs.BhRuntime.Communication.hasConnected.get());
    return true;
  }

  /**
   * BhProgram の実行環境と通信を行わないようにする.
   *
   * @return 切断に成功した場合 true
   */
  public synchronized boolean disconnect() {
    try {
      runtimeFacade.disconnect();
      connectionWait.reset(1);
    } catch (RemoteException e) {
      // 接続中に BhRuntime を kill した場合, ここで抜ける
      msgService.error(TextDefs.BhRuntime.Communication.failedToDisconnect.get());
      LogManager.logger().error("Failed to disconnect from BhRuntime\n" + e);
      return false;
    }
    connected.set(false);
    msgService.info(TextDefs.BhRuntime.Communication.hasDisconnected.get());
    return true;
  }

  /** コマンド / レスポンスの送受信処理を開始する. */
  public synchronized void start() {
    if (futures != null) {
      return;
    }
    futures =  new FutureSet(
        executors.recvMsgTask.submit(this::recvNotif),
        executors.sendMsgTask.submit(this::sendNotif),
        executors.recvRespTask.submit(this::recvResp),
        executors.sendRespTask.submit(this::sendResp));
  }

  /**
   * コマンド / レスポンスの送受信処理を終了する.
   * {@link #start} を呼んでいない場合は何もしない.
   *
   * @return 正常に停止できた場合 true を返す.  送受信処理を開始していなかった場合も true を返す.
   */
  public synchronized boolean halt() {
    if (futures == null) {
      return true;
    }
    if (connected.get()) {
      disconnect();
    }
    boolean success = true;
    for (Future<?> future : futures.toList()) {
      boolean res = future.cancel(true);
      success &= res;
      if (!res) {
        LogManager.logger().error(
            "Failed to cancel '%s' task.".formatted(futures.toName(future)));
      }  
    }
    futures = null;
    return success;
  }

  /** BhProgram の実行環境から {@link BhProgramNotification} を受信し続ける. */
  private void recvNotif() {
    while (true) {
      try {
        connectionWait.awaitInterruptibly();
        BhProgramNotification notif = runtimeFacade.recvNotifFromRuntime();
        if (notif != null) {
          onNotifReceived.get().accept(notif);
        }
      } catch (RemoteException | InterruptedException e) {
        // 子プロセスを kill した場合, RemoteException で抜ける.
        break;
      }
      if (Thread.currentThread().isInterrupted()) {
        break;
      }
    }
  }

  /** BhProgram の実行環境に {@link BhProgramNotification} を送信し続ける. */
  private void sendNotif() {
    while (true) {
      BhProgramNotification notif = null;
      try {
        notif = sendNotifList.poll(
            BhConstants.BhRuntime.POP_SEND_DATA_TIMEOUT, TimeUnit.SECONDS);
      } catch (InterruptedException e) {
        break;
      }
      if (notif == null) {
        continue;
      }
      try {
        runtimeFacade.sendNotifToRuntime(notif);
      } catch (RemoteException e) {
        // 子プロセスをkillした場合, ここで抜ける.
        break;
      }
      if (Thread.currentThread().isInterrupted()) {
        break;
      }
    }
  }

  /** BhProgram の実行環境から {@link BhProgramResponse} を受信し続ける. */
  private void recvResp() {
    while (true) {
      try {
        BhProgramResponse resp = runtimeFacade.recvRespFromRuntime();
        if (resp != null) {
          onRespReceived.get().accept(resp);
        }
      } catch (RemoteException e) {
        // 子プロセスをkillした場合, RemoteExceptionで抜ける.
        break;
      }
      if (Thread.currentThread().isInterrupted()) {
        break;
      }
    }
  }

  /** BhProgram の実行環境に {@link BhProgramResponse} を送信し続ける. */
  private void sendResp() {
    while (true) {
      BhProgramResponse resp = null;
      try {
        resp = sendRespList.poll(BhConstants.BhRuntime.POP_SEND_DATA_TIMEOUT, TimeUnit.SECONDS);
      } catch (InterruptedException e) {
        break;
      }
      if (resp == null) {
        continue;
      }
      try {
        runtimeFacade.sendRespToRuntime(resp);
      } catch (RemoteException e) {
        // 子プロセスをkillした場合, ここで抜ける.
        break;
      }
      if (Thread.currentThread().isInterrupted()) {
        break;
      }
    }
  }

  /**
   * 接続状態のとき, 引数で指定した {@link BhProgramNotification} を送信キューに追加する.
   *
   * @param notif 送信データ
   * @return ステータスコード
   */
  public BhRuntimeStatus pushSendNotif(BhProgramNotification notif) {
    if (!connected.get()) {
      return BhRuntimeStatus.SEND_WHEN_DISCONNECTED;
    }
    boolean success = sendNotifList.offer(notif);
    if (!success) {
      return BhRuntimeStatus.SEND_QUEUE_FULL;
    }
    return BhRuntimeStatus.SUCCESS;
  }

  /**
   * 引数で指定した {@link BhProgramResponse} を送信キューに追加する.
   *
   * @param resp 送信データ
   * @return ステータスコード
   */
  public BhRuntimeStatus pushSendResp(BhProgramResponse resp) {
    boolean success = sendRespList.offer(resp);
    if (!success) {
      return BhRuntimeStatus.SEND_QUEUE_FULL;
    }
    return BhRuntimeStatus.SUCCESS;
  }

  /** このオブジェクトが {@link BhProgramNotification} を受信したときに, このオブジェクトが呼び出すメソッドを設定する. */
  public void setOnNotifReceived(Consumer<BhProgramNotification> handler) {
    if (handler == null) {
      handler = notif -> {};
    }
    this.onNotifReceived.set(handler);
  }

  /**
   * このオブジェクトが {@link BhProgramResponse} を受信したときに,
   * このオブジェクトが呼び出すメソッドを設定する.
   */
  public void setOnRespReceived(Consumer<BhProgramResponse> handler) {
    if (handler == null) {
      handler = resp -> {};
    }
    this.onRespReceived.set(handler);
  }

  /** BhProgram と通信するタスクの {@link Future} オブジェクトのセット. */
  private record FutureSet(
      Future<?> recvMsgFuture,
      Future<?> sendMsgFuture,
      Future<?> recvRespFuture,
      Future<?> sendRespFuture) {
    
    public List<Future<?>> toList() {
      return List.of(recvMsgFuture, sendMsgFuture, recvRespFuture, sendRespFuture);
    }

    public String toName(Future<?> future) {
      if (future == recvMsgFuture) {
        return "recv msg";
      } else if (future == sendMsgFuture) {
        return "send msg";
      } else if (future == recvRespFuture) {
        return "recv resp";
      } else if (future == sendRespFuture) {
        return "send resp";
      }
      return "";
    }
  }

  /**
   * BhProgram と通信するタスクを実行する {@link ExecutorService} オブジェクト.
   *
   * @param recvMsgTask コマンド受信用.
   * @param sendMsgTask コマンド送信用.
   * @param recvRespTask レスポンス受信用.
   * @param sendRespTask レスポンス送信用.
   */
  private record ExecutorSet(
      ExecutorService recvMsgTask,
      ExecutorService sendMsgTask,
      ExecutorService recvRespTask,
      ExecutorService sendRespTask) { }
}
