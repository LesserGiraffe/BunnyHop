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
import net.seapanda.bunnyhop.bhprogram.common.BhProgramHandler;
import net.seapanda.bunnyhop.bhprogram.common.message.BhProgramMessage;
import net.seapanda.bunnyhop.bhprogram.common.message.BhProgramResponse;
import net.seapanda.bunnyhop.common.BhConstants;
import net.seapanda.bunnyhop.common.TextDefs;
import net.seapanda.bunnyhop.service.BhService;
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
  /** BhProgram の実行環境に送信する {@link BhProgramMessage} を格納する FIFO. */
  private final BlockingQueue<BhProgramMessage> sendMsgList = 
      new ArrayBlockingQueue<>(BhConstants.BhRuntime.MAX_REMOTE_CMD_QUEUE_SIZE);
  /** BhProgram の実行環境に送信する {@link BhProgramResponse} を格納する FIFO. */
  private final BlockingQueue<BhProgramResponse> sendRespList =
      new ArrayBlockingQueue<>(BhConstants.BhRuntime.MAX_REMOTE_CMD_QUEUE_SIZE);
  /** BhProgram との通信用 {@link ExecutorService} のセット. */
  private ExecutorsSet executors = new ExecutorsSet(
      Executors.newSingleThreadExecutor(),
      Executors.newSingleThreadExecutor(),
      Executors.newSingleThreadExecutor(),
      Executors.newSingleThreadExecutor());
  private FutureSet futures;
  /** {@link BhProgramMessage} を受信したときに呼び出すメソッド. */
  private AtomicReference<Consumer<BhProgramMessage>> onMsgReceived =
      new AtomicReference<>(msg -> {});
  /** {@link BhProgramResponse} を受信したときに呼び出すメソッド. */
  private AtomicReference<Consumer<BhProgramResponse>> onRespReceived =
      new AtomicReference<>(msg -> {});
  private SynchronizingTimer connectionWait = new SynchronizingTimer(1, true);
  /**
   * BhProgramの実行環境と通信する用のRMIオブジェクト.
   * BhProgramHandler はリモート側の特定のプロセスと紐付いており, RMI Server が同じ TCP ポートでも新しく起動したプロセスと通信することはない.
   */
  private final BhProgramHandler programHandler;
  public final int id;

  /**
   * コンストラクタ.
   *
   * @param programHandler BhProgram と BunnyHop 間でデータを送受信するオブジェクト
   */
  public BhRuntimeTransceiver(BhProgramHandler programHandler) {
    this.programHandler = programHandler;
    id = nextId.getAndIncrement();
  }

  /**
   * BhProgram の実行環境と通信を行うようにする.
   *
   * @return 接続に成功した場合 true
   */
  public synchronized boolean connect() {
    try {
      programHandler.connect();
      connectionWait.reset(0);
    } catch (RemoteException e) {
      // 接続中に BhRuntime を kill した場合, ここで抜ける
      BhService.msgPrinter().errForUser(TextDefs.BhRuntime.Communication.failedToConnect.get());
      BhService.msgPrinter().errForDebug("Failed to connect to BhRuntime.\n" + e);
      return false;
    }
    connected.set(true);
    BhService.msgPrinter().infoForUser(TextDefs.BhRuntime.Communication.hasConnected.get());
    return true;
  }

  /**
   * BhProgram の実行環境と通信を行わないようにする.
   *
   * @return 切断に成功した場合 true
   */
  public synchronized boolean disconnect() {
    try {
      programHandler.disconnect();
      connectionWait.reset(1);
    } catch (RemoteException e) {
      // 接続中に BhRuntime を kill した場合, ここで抜ける
      BhService.msgPrinter().errForUser(TextDefs.BhRuntime.Communication.failedToDisconnect.get());
      BhService.msgPrinter().errForDebug("Failed to disconnect from BhRuntime\n" + e);
      return false;
    }
    connected.set(false);
    BhService.msgPrinter().infoForUser(TextDefs.BhRuntime.Communication.hasDisconnected.get());
    return true;
  }

  /** コマンド / レスポンスの送受信処理を開始する. */
  public synchronized void start() {
    if (futures != null) {
      return;
    }
    futures =  new FutureSet(
        executors.recvMsgTask.submit(this::recvMsg),
        executors.sendMsgTask.submit(this::sendMsg),
        executors.recvRespTask.submit(this::recvResp),
        executors.sendRespTask.submit(this::sendResp));
  }

  /**
   * コマンド / レスポンスの送受信処理を終了する.
   * {@link #start} を読んでいない場合は何もしない.
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
        BhService.msgPrinter().errForDebug(
            "Failed to cancel '%s' task.".formatted(futures.toName(future)));
      }  
    }
    futures = null;
    return success;
  }

  /** BhProgram の実行環境から {@link BhProgramMessage} を受信し続ける. */
  private void recvMsg() {
    while (true) {
      try {
        connectionWait.awaitInterruptibly();
        BhProgramMessage msg = programHandler.recvMsgFromScript();
        if (msg != null) {
          onMsgReceived.get().accept(msg);
        }
      } catch (RemoteException | InterruptedException e) {
        // 子プロセスをkillした場合, RemoteExceptionで抜ける.
        break;
      }
      if (Thread.currentThread().isInterrupted()) {
        break;
      }
    }
  }

  /** BhProgram の実行環境に {@link BhProgramMessage} を送信し続ける. */
  private void sendMsg() {
    while (true) {
      BhProgramMessage msg = null;
      try {
        msg = sendMsgList.poll(
            BhConstants.BhRuntime.POP_SEND_DATA_TIMEOUT, TimeUnit.SECONDS);
      } catch (InterruptedException e) {
        break;
      }
      if (msg == null) {
        continue;
      }
      try {
        programHandler.sendMsgToScript(msg);
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
        BhProgramResponse resp = programHandler.recvRespFromScript();
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
        resp = sendRespList.poll(
            BhConstants.BhRuntime.POP_SEND_DATA_TIMEOUT, TimeUnit.SECONDS);
      } catch (InterruptedException e) {
        break;
      }
      if (resp == null) {
        continue;
      }
      try {
        programHandler.sendRespToScript(resp);
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
   * 接続状態のとき, 引数で指定した {@link BhProgramMessage} を送信キューに追加する.
   *
   * @param msg 送信データ
   * @return ステータスコード
   */
  public BhRuntimeStatus pushSendMsg(BhProgramMessage msg) {
    if (!connected.get()) {
      return BhRuntimeStatus.SEND_WHEN_DISCONNECTED;
    }
    boolean success = sendMsgList.offer(msg);
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

  /** このオブジェクトが {@link BhProgramMessage} を受信したときに, このオブジェクトが呼び出すメソッドを設定する. */
  public void setOnMsgReceived(Consumer<BhProgramMessage> onMsgReceived) {
    if (onMsgReceived == null) {
      onMsgReceived = msg -> {};
    }
    this.onMsgReceived.set(onMsgReceived);
  }

  /**
   * このオブジェクトが {@link BhProgramResponse} を受信したときに,
   * このオブジェクトが呼び出すメソッドを設定する.
   */
  public void setOnRespReceived(Consumer<BhProgramResponse> onRespReceived) {
    if (onRespReceived == null) {
      onRespReceived = resp -> {};
    }
    this.onRespReceived.set(onRespReceived);
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
  private record ExecutorsSet(
      ExecutorService recvMsgTask,
      ExecutorService sendMsgTask,
      ExecutorService recvRespTask,
      ExecutorService sendRespTask) { }
}
