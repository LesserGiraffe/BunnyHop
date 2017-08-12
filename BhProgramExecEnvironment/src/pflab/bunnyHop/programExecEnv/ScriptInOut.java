package pflab.bunnyHop.programExecEnv;

import pflab.bunnyHop.bhProgram.common.BhProgramData;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.BooleanSupplier;

/**
 * 実行時スクリプトの入出力を取り扱うクラス
 * @author K.Koike
 */
public class ScriptInOut {
	
	private final BlockingQueue<BhProgramData> sendDataList;	//!< BunnyHopへの送信データキュー
	private final BlockingQueue<String> stdInDataList = new ArrayBlockingQueue<>(BhParams.maxQueueSize);
	private final AtomicBoolean connected;	//!< BunnyHopとの接続状況を取得する関数
	
	/**
	 * @param sendDataList BunnyHopへの送信データキュー
	 * @param connected BunnyHopとの接続状況を取得する関数
	 */
	public ScriptInOut(
		BlockingQueue<BhProgramData> sendDataList,
		AtomicBoolean connected) {
		
		this.sendDataList = sendDataList;
		this.connected = connected;
	}
	
	/**
	 * BunnyHop側で出力するデータを送信データキューに追加する
	 * @param str 出力する文字列
	 */
	public void println(String str) {
		
		if (!connected.get())
			return;
		
		boolean add = false;
		BhProgramData data = new BhProgramData(BhProgramData.TYPE.OUTPUT_STR, str);
		while (!add) {
			try {
				add = sendDataList.offer(data, BhParams.pushSendDataTimeout, TimeUnit.SECONDS);
				if (!connected.get()) {
					sendDataList.clear();
					return;
				}
			}
			catch(InterruptedException e) {
				Thread.currentThread().interrupt();
				break;
			}
		}
	}
	
	/**
	 * 標準入力に入力された文字をリストに追加する
	 * @param input 標準入力に入力された文字
	 */
	public void addStdInData(String input) {
		stdInDataList.offer(input);
	}
	
	/**
	 * 標準入力のデータを読み取って返す
	 * @return 標準入力に入力された文字
	 */
	public String scan() {

		String data = "";
		try {
			data = stdInDataList.take();
		}
		catch(InterruptedException e) {
			Thread.currentThread().interrupt();
		}
		return data;
	}
}
