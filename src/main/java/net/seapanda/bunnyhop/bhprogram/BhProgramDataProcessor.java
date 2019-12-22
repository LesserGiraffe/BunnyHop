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
package net.seapanda.bunnyhop.bhprogram;

import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import net.seapanda.bunnyhop.bhprogram.common.BhProgramData;
import net.seapanda.bunnyhop.common.constant.BhParams;
import net.seapanda.bunnyhop.common.tools.MsgPrinter;

/**
 * BhProgram の実行環境が送信したしたコマンドを処理するクラス
 * @author K.koike
 */
public class BhProgramDataProcessor {

	private final BlockingQueue<BhProgramData> recvDataList = new ArrayBlockingQueue<>(BhParams.ExternalApplication.MAX_REMOTE_CMD_QUEUE_SIZE);
	private final ExecutorService remoteCmdExec = Executors.newSingleThreadExecutor();	//!< コマンド受信用

	void init() {
		remoteCmdExec.submit(() -> {

			while (true) {

				BhProgramData data = null;
				try {
					data = recvDataList.poll(BhParams.ExternalApplication.POP_RECV_DATA_TIMEOUT, TimeUnit.SECONDS);
				}
				catch(InterruptedException e) {
					break;
				}

				if (data != null)
					process(data);
			}
		});
	}

	/**
	 * BhProgram の実行環境が送信したコマンドを処理する
	 * @param data リモート環境から受信したデータ. nullは駄目.
	 */
	private void process(BhProgramData data) {

		switch(data.type) {
			case OUTPUT_STR:
				MsgPrinter.INSTANCE.msgForUser(data.str + "\n");
				break;

			case OUTPUT_EXCEPTION:
				MsgPrinter.INSTANCE.msgForUser(data.exception.getMessage() + "\n");
				var iter = data.exception.getCallStack().descendingIterator();
				while (iter.hasNext())
					MsgPrinter.INSTANCE.msgForUser("	" + iter.next().toString() + "\n");
				break;

			default:
		}
	}

	/**
	 * 処理対象のデータを追加する
	 * @param data 処理対象のデータ
	 */
	public void add(BhProgramData data) throws InterruptedException {
		recvDataList.put(data);
	}

	/**
	 * 現在追加されている処理対象のデータを全て削除する
	 */
	public void clearAllData() {
		recvDataList.clear();
	}

	/**
	 * このオブジェクトの終了処理を行う
	 * @return 終了処理が成功した場合true
	 */
	public boolean end() {

		boolean success = false;
		remoteCmdExec.shutdownNow();
		try {
			success = remoteCmdExec.awaitTermination(BhParams.EXECUTOR_SHUTDOWN_TIMEOUT, TimeUnit.SECONDS);
		}
		catch(InterruptedException e) {
			Thread.currentThread().interrupt();
		}
		return success;
	}
}
