package pflab.bunnyHop.bhProgram;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.MalformedURLException;
import java.rmi.Naming;
import java.rmi.NotBoundException;
import java.rmi.Remote;
import java.rmi.RemoteException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Supplier;
import pflab.bunnyHop.bhProgram.common.BhProgramData;
import pflab.bunnyHop.bhProgram.common.BhProgramHandler;
import pflab.bunnyHop.common.BhParams;
import pflab.bunnyHop.common.Util;
import pflab.bunnyHop.root.MsgPrinter;

/**
 * BhProgramの実行環境を操作するクラスが共通で持つ機能と変数をまとめたクラス
 * @author K.Koike
 */
public class BhProgramManagerCommon {
	
	public final ExecutorService runBhProgramExec = Executors.newSingleThreadExecutor();	//!< BhProgram実行用
	public final ExecutorService connectTaskExec = Executors.newSingleThreadExecutor();	//!< 接続, 切断処理用
	public final ExecutorService recvTaskExec = Executors.newSingleThreadExecutor();	//!< コマンド受信用
	public final ExecutorService sendTaskExec = Executors.newSingleThreadExecutor();	//!< コマンド送信用
	public final ExecutorService terminationExec = Executors.newSingleThreadExecutor();	//!< プロセス終了用
	public final RemoteCmdProcessor cmdProcessor = new RemoteCmdProcessor();	//!< BhProgramの実行環境から受信したデータを処理するオブジェクト
	private final AtomicReference<TransceiverAndFutureSet> transceiverAndFutureRef = new AtomicReference<>();

	public BhProgramManagerCommon(){}
	
	public boolean init() {
		cmdProcessor.init();
		return true;
	}
	
	/**
	 * RemoteCmdProcessorオブジェクト(RMIオブジェクト)を探す
	 * @param ipAddr リモートオブジェクトのIPアドレス
	 * @param port RMIレジストリのポート
	 * @param name オブジェクトバインド時の名前
	 * @return Remoteオブジェクト
	 */
	public Remote findRemoteObj(String ipAddr, int port, String name) 
		throws MalformedURLException, NotBoundException, RemoteException {

		return Naming.lookup("rmi://" + ipAddr + ":"+ port + "/" + name);
	}
	
	/**
	 * BhProgramを実行する
	 * @param execFunc BhProgram実行関数
	 * @return BhProgram実行タスクのFutureオブジェクト
	 */
	public Future<Boolean> executeAsync(Callable<Boolean> execFunc) {
		return runBhProgramExec.submit(execFunc);
	}
	
	/**
	 * BhProgramを終了する
	 * @param terminationFunc BhProgram終了関数
	 * @return BhProgram終了タスクのFutureオブジェクト
	 */
	public Future<Boolean> terminateAsync(Callable<Boolean> terminationFunc) {
		return terminationExec.submit(terminationFunc);
	}
	
	/**
	 * BhProgram の実行環境と通信を行うようにする
	 */
	public void connectAsync() {
		
		if (!getTransceiver().isPresent()) {
			MsgPrinter.instance.ErrMsgForUser("!! 接続失敗 (プログラム未実行) !!\n");
			return;
		}
		
		connectTaskExec.submit(() -> {
			getTransceiver().get().connect();
		});	
	}
	
	/**
	 * BhProgram の実行環境と通信を行わないようにする
	 */
	public void disconnectAsync() {
		
		if (!getTransceiver().isPresent()) {
			MsgPrinter.instance.ErrMsgForUser("!! 切断失敗 (プログラム未実行) !!\n");
			return;
		}
			
		connectTaskExec.submit(() -> {	
			getTransceiver().get().disconnect();
		});
	}
	
	/**
	 * 引数で指定したデータをBhProgramの実行環境に送る
	 * @param data 送信データ
	 * @return 送信データリストにデータを追加できた場合true
	 */
	public BhProgramExecEnvError sendAsync(BhProgramData data) {
		
		if (!getTransceiver().isPresent()) {
			return BhProgramExecEnvError.SEND_WHEN_DISCONNECTED;
		}
		
		return getTransceiver().get().addSendDataList(data);
	}
	
	/**
	 * 引数で指定したプロセスの終了処理を待つ
	 * @param process 終わらせるプロセスのオブジェクト (nullだめ)
	 * @param destroy 強制終了する場合true. 
	 * @param timeout 終了待ちタイムアウト時間 (sec)
	 * @return プロセスを正常に終了できた場合true. 
	 */
	public boolean waitForProcessEnd(Process process, boolean destroy, int timeout) {
		
		boolean success = true;
		
		if (destroy) {
			//ストリームクローズ
			try {
				process.getErrorStream().close();
				process.getInputStream().close();
				process.getOutputStream().close();
			} catch (IOException e) {
				MsgPrinter.instance.ErrMsgForDebug("failed to close iostream"	+ "\n" + e.toString());
				success = false;
			}
			//強制終了
			process.destroy();
			//終了待ち
			try {
				success &= process.waitFor(timeout, TimeUnit.SECONDS);
			}
			catch(InterruptedException e) {
				Thread.currentThread().interrupt();
				MsgPrinter.instance.ErrMsgForDebug("failed to waitFor" +  "\n" + e.toString());
			}
		}
		else {
			//終了待ち
			try {
				success &= process.waitFor(timeout, TimeUnit.SECONDS);
			}
			catch(InterruptedException e) {
				Thread.currentThread().interrupt();
				MsgPrinter.instance.ErrMsgForDebug("failed to waitFor" +  "\n" + e.toString());
			}
			//ストリームクローズ
			try {
				process.getErrorStream().close();
				process.getInputStream().close();
				process.getOutputStream().close();
			} catch (IOException e) {
				MsgPrinter.instance.ErrMsgForDebug("failed to close iostream"	+ "\n" + e.toString());
				success = false;
			}
			process.destroy();
		}
		return success;
	}
	
	/**
	 * このオブジェクトの終了処理をする
	 * @return 終了処理が正常に完了した場合true
	 */
	public boolean end() {
		
		boolean success = true;
		runBhProgramExec.shutdownNow();
		connectTaskExec.shutdownNow();
		recvTaskExec.shutdownNow();
		sendTaskExec.shutdownNow();
		terminationExec.shutdownNow();
		success &= cmdProcessor.end();
		
		try {
			success &= runBhProgramExec.awaitTermination(BhParams.executorShutdownTimeout, TimeUnit.SECONDS);
			success &= connectTaskExec.awaitTermination(BhParams.executorShutdownTimeout, TimeUnit.SECONDS);
			success &= recvTaskExec.awaitTermination(BhParams.executorShutdownTimeout, TimeUnit.SECONDS);
			success &= sendTaskExec.awaitTermination(BhParams.executorShutdownTimeout, TimeUnit.SECONDS);
			success &= terminationExec.awaitTermination(BhParams.executorShutdownTimeout, TimeUnit.SECONDS);
		}
		catch(InterruptedException e) {
			success &= false;
		}
		return success;
	}
	
	/**
	 * BhProgramと通信をするオブジェクトとそのメソッドを呼び出しているタスクのFutureオブジェクトをセットする.
	 * @param transceiver BhProgramと通信をするオブジェクト. null禁止.
	 * @param recvTaskFuture BhProgram実行環境からのデータ受信処理を行うタスクのFutureオブジェクト
	 * @param sendTaskFuture BhProgram実行環境へのデータ送信処理を行うタスクのFutureオブジェクト
	 */
	private void setTransceiverAndFutures(
		BhProgramTransceiver transceiver,
		Future<Boolean> recvTaskFuture,
		Future<Boolean> sendTaskFuture) {
		
		assert transceiver != null;
		assert recvTaskFuture != null;
		assert sendTaskFuture != null;
		transceiverAndFutureRef.set(new TransceiverAndFutureSet(transceiver, recvTaskFuture, sendTaskFuture));
	}
	
	/**
	 * BhProgramと通信をするオブジェクトを取得する.
	 * haltTransceiver関数が正常終了した場合, 新しくTransceiverAndFutureSetオブジェクトをセットするまでこの関数はemptyを返す.
	 * @return BhProgramと通信をするオブジェクト.
	 */
	public Optional<BhProgramTransceiver> getTransceiver() {
		
		if (transceiverAndFutureRef.get() == null)
			return Optional.empty();
		
		return Optional.of(transceiverAndFutureRef.get().transceiver);
	}
	
	/**
	 * BhProgramTransceiverの処理を終了する.<br>
	 * @return 終了処理が成功した場合true. トランシーバが登録されていない場合もtrueを返す.
	 */
	public boolean haltTransceiver() {
		
		if (transceiverAndFutureRef.get() == null)
			return true;
		
		boolean success = true;
		transceiverAndFutureRef.get().transceiver.disconnect();
		transceiverAndFutureRef.get().transceiver.clearSendDataList();

		boolean res = transceiverAndFutureRef.get().recvTaskFuture.cancel(true);	//タスクの終了を待たなくてよい. 新しく起動したプロセスと, 古いトランシーバが通信をしてしまうことはない.
		success &= res;
		if (!res) {
			MsgPrinter.instance.ErrMsgForDebug("failed to cancel recv task " + BhParams.ExternalProgram.bhProgramExecEnvironment);
		}
			
		res = transceiverAndFutureRef.get().sendTaskFuture.cancel(true);
		success &= res;
		if (!res) {
			MsgPrinter.instance.ErrMsgForDebug("failed to cancel send task " + BhParams.ExternalProgram.bhProgramExecEnvironment);
		}
		
		if (success)
			transceiverAndFutureRef.set(null);
		
		return success;
	}
	
	/**
	 * BhProgram実行環境にBhProgramの開始を命令する
	 * @param fileName BhProgramのファイル名
	 * @param ipAddr BhProgramの実行環境が動作しているマシンのIPアドレス
	 * @param is BhProgram実行環境からの出力を受け取るためのInputStream
	 * @return 正常にBhProgramを開始できた場合true
	 */
	public boolean runBhProgram(String fileName, String ipAddr, InputStream is) {
		
		MsgPrinter.instance.MsgForUser("-- 通信準備中 --\n");
		
		boolean success = true;
		try (BufferedReader br = new BufferedReader(new InputStreamReader(is))){
				
			String portStr = 
				getSuffixedLine(
					br, 
					BhParams.ExternalProgram.rmiTcpPortSuffix,
					BhParams.ExternalProgram.tcpPortReadTimeout);
			int port = Integer.parseInt(portStr);
			BhProgramHandler programHandler = (BhProgramHandler)findRemoteObj(ipAddr, port, BhProgramHandler.class.getSimpleName());	//リモートオブジェクト取得
			BhProgramTransceiver transceiver = new BhProgramTransceiver(cmdProcessor, programHandler);
			success &= transceiver.connect();
			success &= programHandler.runScript(fileName);
			Future<Boolean> recvTaskFuture = recvTaskExec.submit(() -> transceiver.recv());
			Future<Boolean> sendTaskFuture = sendTaskExec.submit(() -> transceiver.send());
			setTransceiverAndFutures(transceiver, recvTaskFuture, sendTaskFuture);
		}
		catch(IOException | NotBoundException | NumberFormatException | TimeoutException e) {
			MsgPrinter.instance.ErrMsgForDebug("failed to run BhProgram " + e.toString());
			success &= false;
		}
		if (!success)
			MsgPrinter.instance.ErrMsgForUser("!! 通信準備失敗 !!\n");
		
		return success;
	}
	
	/**
	* BhProgramと通信をするオブジェクトとそのメソッドを呼び出しているタスクのFutureオブジェクトのセット
	* @author K.Koike
	*/
	private class TransceiverAndFutureSet {

		public final BhProgramTransceiver transceiver;
		public final Future<Boolean> recvTaskFuture;
		public final Future<Boolean> sendTaskFuture;

		/**
		 * @param transceiver BhProgramと通信をするオブジェクト
		 * @param recvTaskFuture transceiverの受信メソッドを実行するタスクのFutureオブジェクト
		 * @param sendTaskFuture transceiverの送信メソッドを実行するタスクのFutureオブジェクト
		 */
		public TransceiverAndFutureSet(
			BhProgramTransceiver transceiver,
			Future<Boolean> recvTaskFuture,
			Future<Boolean> sendTaskFuture) {

			this.transceiver = transceiver;
			this.recvTaskFuture = recvTaskFuture;
			this.sendTaskFuture = sendTaskFuture;
		}
	}
	
	/**
	 * 引数で指定したサフィックスが付いた1行をBufferedReaderから読み込んで, サフィックスを取り除いて返す <br>
	 * 指定したサフィックスが付いていな行は読み飛ばす. <br>
	 * EOFのある行は判定対象外 <br>
	 * @param br テキストを読み込むbuffered reader
	 * @param suffix このサフィックスが付いた行を返す
	 * @param timeout 読み取りを試みる時間 (sec)
	 * @throws IOException 入出力エラーが発生した際の例外
	 * @throws TimeoutException タイムアウトした際の例外
	 * @return 引数で指定したサフィックスが付いた1行からサフィックスを取り除いた文字列. <br>
	 */
	private String getSuffixedLine(BufferedReader br, String suffix, long timeout) 
		throws IOException, TimeoutException {
		
		timeout *= 1000;
		String readStr = "";
		long begin = System.currentTimeMillis();
		List<Character> charCodeList = new ArrayList<>();
		try {			
			while (true) {
				if ((System.currentTimeMillis() - begin) > timeout)
					throw new TimeoutException("getSuffixedLine timeout");
				
				if (br.ready()) {	//次の読み出し結果がEOFの場合 false
					int charCode = br.read();
					switch (charCode) {
						case '\r':
						case '\n':
							char[] charCodeArray = new char[charCodeList.size()];
							for (int i = 0; i < charCodeArray.length; ++i)
								charCodeArray[i] = charCodeList.get(i);							
							readStr = new String(charCodeArray);	//サイズ0の配列の場合 readStr == '\0'
							charCodeList.clear();
							break;
							
						default:	//改行以外の文字コード
							charCodeList.add((char)charCode);
							continue;
					}
				}
				else {
					Thread.sleep(100);
					continue;
				}
				
				if (readStr.endsWith(suffix))
					break;
			}
		}
		catch(InterruptedException  e) {}
		readStr = readStr.substring(0, readStr.length() - suffix.length());
		return readStr;
	}
}
