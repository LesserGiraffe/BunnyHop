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
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import pflab.bunnyhop.common.BhParams;
import pflab.bunnyhop.root.MsgPrinter;

/**
 * リモート環境から受信したコマンドを処理する
 * @author K.koike
 */
public class RemoteCmdProcessor {

	private final BlockingQueue<BhProgramData> recvDataList = new ArrayBlockingQueue<>(BhParams.ExternalProgram.maxRemoteCmdQueueSize);
	private final ExecutorService remoteCmdExec = Executors.newSingleThreadExecutor();	//!< コマンド受信用
	
	public void RemoteCmdProcessor(){}
	
	public void init() {
		remoteCmdExec.submit(() -> {
			
			while (true) {
				
				BhProgramData data = null;
				try {
					data = recvDataList.poll(BhParams.ExternalProgram.popRecvDataTimeout, TimeUnit.SECONDS);
				}
				catch(InterruptedException e) {
					break;
				}
							
				if (data != null)
					processRemoteData(data);
			}
		});
	}
	
	/**
	 * リモート環境から受信したデータを処理する
	 * @param data リモート環境から受信したデータ. nullは駄目.
	 */
	private void processRemoteData(BhProgramData data) {
		
		switch(data.type) {
			case OUTPUT_STR:
				MsgPrinter.instance.MsgForUser(data.str + "\n");
				break;
		}
	}
	
	/**
	 * 処理対象のリモートデータを追加する
	 * @param data 処理対象のリモートデータ
	 */
	public void addRemoteData(BhProgramData data) throws InterruptedException {
		recvDataList.put(data);
	}
	
	/**
	 * このオブジェクトの終了処理を行う
	 * @return 終了処理が成功した場合true
	 */
	public boolean end() {
		
		boolean success = false;
		remoteCmdExec.shutdownNow();
		try {
			success = remoteCmdExec.awaitTermination(BhParams.executorShutdownTimeout, TimeUnit.SECONDS);
		}
		catch(InterruptedException e) {
			Thread.currentThread().interrupt();
		}
		return success;
	}
}
