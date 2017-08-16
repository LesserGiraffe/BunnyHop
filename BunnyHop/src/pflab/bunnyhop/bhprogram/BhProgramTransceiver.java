/**
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
package pflab.bunnyhop.bhprogram;

import pflab.bunnyhop.bhprogram.common.BhProgramData;
import pflab.bunnyhop.bhprogram.common.BhProgramHandler;
import java.rmi.RemoteException;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicBoolean;
import pflab.bunnyhop.common.BhParams;
import pflab.bunnyhop.root.MsgPrinter;

/**
 * BhProgramと通信をするクラス
 * @author K.Koike
 */
public class BhProgramTransceiver {
	
	private final AtomicBoolean connected = new AtomicBoolean(false);	//!< 接続状態
	private final BlockingQueue<BhProgramData> sendDataList = new ArrayBlockingQueue<>(BhParams.ExternalProgram.maxRemoteCmdQueueSize);
	private final BhProgramHandler programHandler;	//!< BhProgramの実行環境と通信する用のRMIオブジェクト
													// BhProgramHandlerは特定のプロセスと紐付いており, RMI Serverが同じTCPポートでも新しく起動したプロセスと通信することはない.
	private final RemoteCmdProcessor cmdProcessor;	//!< BhProgramの実行環境から受信したデータを処理するオブジェクト
	
	/**
	 * コンストラクタ
	 * @param cmdProcessor BhProgramの実行環境から受信したデータを処理するオブジェクト
	 * @param programHandler BhProgramの実行環境から受信したデータを処理するオブジェクト
	 */
	public BhProgramTransceiver (RemoteCmdProcessor cmdProcessor, BhProgramHandler programHandler) {
		this.programHandler = programHandler;
		this.cmdProcessor = cmdProcessor;
	}
	
	/**
	 * BhProgram の実行環境と通信を行うようにする
	 * @return 接続に成功した場合true
	 */
	public boolean connect() {
		
		synchronized(connected) {			
			try {
				programHandler.connect();
			}
			catch(RemoteException e) {	//接続中にBhProgramExecEnvironmentをkillした場合, ここで抜ける
				MsgPrinter.instance.ErrMsgForUser("!! 接続失敗 !!\n");
				MsgPrinter.instance.ErrMsgForDebug("failed to connect. " + e.toString());
				return false;
			}
			connected.set(true);
			connected.notifyAll();
		}
		MsgPrinter.instance.MsgForUser("-- 接続完了 --\n");
		return true;
	}
	
	/**
	 * BhProgram の実行環境と通信を行わないようにする
	 * @return 接続に成功した場合true
	 */
	public boolean disconnect() {

		synchronized(connected) {			
			try {
				programHandler.disconnect();
			}
			catch(RemoteException e) {	//接続中にBhProgramExecEnvironmentをkillした場合, ここで抜ける
				MsgPrinter.instance.ErrMsgForUser("!! 切断失敗 !!\n");
				MsgPrinter.instance.ErrMsgForDebug("failed to disconnect " + e.toString());
				return false;
			}
			connected.set(false);
		}
		MsgPrinter.instance.MsgForUser("-- 切断完了 --\n");
		return true;
	}
	
	/**
	 * BhProgramの実行環境から送られるデータを受信し続ける
	 * @return 受信待ち処理に入れた場合true
	 */
	public boolean recv() {

		while (true) {
			synchronized(connected) {
				try {
					if (!connected.get()) {	//切断時は接続待ち
						connected.wait();
					}
				}
				catch(InterruptedException e) {
					break;
				}
			}
			
			try {
				BhProgramData data = programHandler.recvDataFromScript();
				if (data != null)
					cmdProcessor.addRemoteData(data);
			}
			catch(RemoteException | InterruptedException e) {	//子プロセスをkillした場合, RemoteExceptionで抜ける.
				break;
			}
			
			if (Thread.currentThread().isInterrupted())
				break;
		}
		return true;
	}
	
	/**
	 * BhProgramの実行環境にデータを送り続ける
	 * @return 送信待ち処理に入れた場合true
	 */
	public boolean send() {
		
		while (true) {
						
			BhProgramData data = null;
			try {
				data = sendDataList.poll(BhParams.ExternalProgram.popSendDataTimeout, TimeUnit.SECONDS);
			}
			catch(InterruptedException e) {
				break;
			}

			if (data == null)
				continue;
			
			try {
				programHandler.sendDataToScript(data);
			}
			catch(RemoteException e) {	//子プロセスをkillした場合, ここで抜ける.
				break;
			}
			
			if (Thread.currentThread().isInterrupted())
				break;
		}
		return true;
	}
	
	/**
	 * 引数で指定したデータを送信データリストに追加する
	 * @param data 送信データ
	 * @return エラーコード
	 */
	public BhProgramExecEnvError addSendDataList(BhProgramData data) {
		
		if (!connected.get()) {
			return BhProgramExecEnvError.SEND_WHEN_DISCONNECTED;
		}
		
		boolean success = sendDataList.offer(data);
		if (!success) {
			return BhProgramExecEnvError.SEND_QUEUE_FULL;
		}
		return BhProgramExecEnvError.SUCCESS;
	}
	
	/**
	 * 送信データリストをクリアする
	 */
	public void clearSendDataList() {
		sendDataList.clear();
	}
}
