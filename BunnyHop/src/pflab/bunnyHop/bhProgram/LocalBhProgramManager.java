package pflab.bunnyHop.bhProgram;

import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.nio.charset.Charset;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.rmi.Naming;
import java.rmi.NotBoundException;
import java.rmi.Remote;
import java.rmi.RemoteException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicBoolean;
import pflab.bunnyHop.common.BhParams;
import pflab.bunnyHop.common.Util;
import pflab.bunnyHop.root.MsgPrinter;

/**
 * BHで作成したスクリプトを実行するクラス
 * @author K.Koike
 */
public class LocalBhProgramManager {
	
	public static final LocalBhProgramManager instance = new LocalBhProgramManager();	//!< シングルトンインスタンス
	private final ExecutorService runBhProgramExec = Executors.newSingleThreadExecutor();	//!< スクリプト実行命令
	private final ExecutorService connectProcExec = Executors.newSingleThreadExecutor();	//!< 接続, 切断処理用
	private final ExecutorService recvProcExec = Executors.newSingleThreadExecutor();	//!< コマンド受信用
	private final ExecutorService sendProcExec = Executors.newSingleThreadExecutor();	//!< コマンド送信用
	private final ExecutorService terminationExec = Executors.newSingleThreadExecutor();	//!< プロセス終了用
	private final RemoteCmdProcessor cmdProcessor = new RemoteCmdProcessor();
	private BhProgramHandler programHandler;
	private Process programRunner;
	private final AtomicBoolean connected = new AtomicBoolean(false);	//!< 接続状態
	private final AtomicBoolean running = new AtomicBoolean(false);	//!< BhProgramRunner.jar が動いている場合true
	private final BlockingQueue<BhProgramData> sendDataList = new ArrayBlockingQueue<>(BhParams.ExternalProgram.maxRemoteCmdQueueSize);
	
	private LocalBhProgramManager() {
		cmdProcessor.init();
	}
	
	/**
	 * BhProgramを実行する
	 * @param filePath BhProgramのファイルパス
	 * @param ipAddr BhProgramを実行するマシンのIPアドレス
	 * @return BhProgram実行開始の完了待ちオブジェクト
	 */
	public Future<Boolean> executeAsync(Path filePath, String ipAddr) {
		
		Future<Boolean> executionFuture = runBhProgramExec.submit(() -> execute(filePath, ipAddr));
		recvProcExec.submit(() -> recv(executionFuture));
		sendProcExec.submit(() -> send(executionFuture));
		return executionFuture;
	}
	
	/**
	 * BhProgramを実行する
	 * @param filePath BhProgramのファイルパス
	 * @param ipAddr BhProgramを実行するマシンのIPアドレス
	 * @return 実行ハンドラの処理完了待ちオブジェクト
	 */
	private synchronized boolean execute(Path filePath, String ipAddr) {
					
		boolean success = terminate();
		if (success) {
			
			ProcessBuilder procBuilder = 
				new ProcessBuilder(
					"java", 
					"-jar", 
					Paths.get(Util.execPath, BhParams.ExternalProgram.bhProgramRunnerName).toString(),
					"true");	//localFlag == true

			try {
				programRunner = procBuilder.start();
				int port = Integer.parseInt(readStream(programRunner.getInputStream(), '@'));
				programHandler = (BhProgramHandler)findRmoteObj(ipAddr, port, BhProgramHandler.class.getSimpleName());
				running.set(true);	//この時点で BhProgramRunner.jar の起動確定
				success &= connect();
				success &= programHandler.runScript(filePath.getFileName().toString());
			}
			catch(IOException | NotBoundException | NumberFormatException e) {
				MsgPrinter.instance.ErrMsgForDebug(e.toString());
				success &= false;
			}
		}
				
		if (!success) {	//リモートでのスクリプト実行失敗
			terminate();
			MsgPrinter.instance.ErrMsgForDebug("failed to run " +filePath.getFileName().toString());
		}
		return success;
	}
	
	/**
	 * 現在実行中のBhProgramRunner を強制終了する
	 * @return 強制終了タスクの結果
	 */
	public Future<Boolean> terminateAsync() {
		
		return terminationExec.submit(()-> {
			return terminate();
		});
	}
	
	/**
	 * 現在実行中のBhProgramRunner を強制終了する
	 * @return 強制終了に成功した場合true
	 */
	public synchronized boolean terminate() {
		
		if (programRunner == null) {
			running.set(false);
			return true;
		}
		
		disconnect();
		sendDataList.clear();
		
		try {
			programRunner.getErrorStream().close();
			programRunner.getInputStream().close();
			programRunner.getOutputStream().close();
		} catch (IOException e) {
			MsgPrinter.instance.ErrMsgForDebug("failed to close iostream " + BhParams.ExternalProgram.bhProgramRunnerName
				+ "\n" + e.getMessage());
		}
		
		boolean success = false;
		try {
			programRunner.destroy();
			success = programRunner.waitFor(BhParams.ExternalProgram.programRunnerTerminationTimeout, TimeUnit.SECONDS);
		}
		catch(InterruptedException e) {
			Thread.currentThread().interrupt();
			MsgPrinter.instance.ErrMsgForDebug("failed to destroy " + BhParams.ExternalProgram.bhProgramRunnerName
				+ "\n" + e.getMessage());
		}

		if (!success) {
			MsgPrinter.instance.ErrMsgForDebug("failed to terminate " + BhParams.ExternalProgram.bhProgramRunnerName);
		}
		else {
			running.set(false);
		}
		programRunner = null;
		return success;
	}
		
	/**
	 * BhProgram の実行環境と通信を行うようにする
	 */
	public void connectAsync() {
		
		if (connected.get())
			return;
		
		connectProcExec.submit(() -> {
			connect();
		});
	}
	
	/**
	 * BhProgram の実行環境と通信を行うようにする
	 */
	private boolean connect() {

		if (!running.get())
			return false;
		
		synchronized(connected) {
	
			try {
				programHandler.connect();
			}
			catch(RemoteException e) {	//接続中にBhProgramRunnerをkillした場合, ここで抜ける
				MsgPrinter.instance.ErrMsgForDebug("failed to connect " + e.getMessage());
				return false;
			}
			connected.set(true);
			connected.notifyAll();
		}
		return true;
	}
	
	/**
	 * BhProgram の実行環境と通信を行わないようにする
	 */
	public void disconnectAsync() {
		
		if (!connected.get())
			return;
		
		connectProcExec.submit(() -> {	
			disconnect();
		});		
	}
	
	private boolean disconnect() {
		
		if (!running.get())
			return false;

		synchronized(connected) {			
			try {
				programHandler.disconnect();
			}
			catch(RemoteException e) {	//接続中にBhProgramRunnerをkillした場合, ここで抜ける
				MsgPrinter.instance.ErrMsgForDebug("failed to disconnect " + e.getMessage());
				return false;
			}
			connected.set(false);
		}
		return true;
	}
	
	/**
	 * BhProgramの実行環境から送られるデータを受信し続ける
	 * @param execFuture BhProgram開始完了待ちオブジェクト
	 * @return 受信待ち処理に入れた場合true
	 */
	private boolean recv(Future<Boolean> execFuture) {
					
		boolean success = false;
		try {
			success = execFuture.get(BhParams.ExternalProgram.programRunnerStartTimeout, TimeUnit.SECONDS);	//子プロセス起動待ち
		}
		catch(InterruptedException | TimeoutException | ExecutionException e) {}
			
		if (!success) {
			return false;
		}
			
		while (true) {
					
			synchronized(connected) {
				try {
					if (!connected.get())	//切断時は接続待ち
						connected.wait();
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
		}
		return true;
	}
	
	/**
	 * 引数で指定したデータをBhProgramの実行環境に送る
	 * @param data 送信データ
	 * @return 送信データリストにデータを追加できた場合true
	 */
	public boolean sendAsync(BhProgramData data) {
	
		if (running.get() && connected.get()) {
			return sendDataList.offer(data);
		}
		else {
			return false;
		}		
	}
	
	/**
	 * BhProgramの実行環境にデータを送り続ける
	 * @param execFuture BhProgram開始完了待ちオブジェクト.
	 * @return 送信待ち処理に入れた場合true
	 */
	private boolean send(Future<Boolean> execFuture) {
		
		boolean success = false;
		try {
			success = execFuture.get(BhParams.ExternalProgram.programRunnerStartTimeout, TimeUnit.SECONDS);	//子プロセス起動待ち
		}
		catch(InterruptedException | TimeoutException | ExecutionException e) {}
			
		if (!success) {
			return false;
		}
			
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
		}
		return true;
	}
	
	/**
	 * 終了処理をする
	 * @return 終了処理が正常に完了した場合true
	 */
	public boolean end() {
		
		boolean success = true;
		runBhProgramExec.shutdownNow();
		success = terminate();
		connectProcExec.shutdownNow();
		recvProcExec.shutdownNow();
		sendProcExec.shutdownNow();
		terminationExec.shutdownNow();
		success &= cmdProcessor.end();
		
		try {
			success &= runBhProgramExec.awaitTermination(BhParams.executorShutdownTimeout, TimeUnit.SECONDS);
			success &= connectProcExec.awaitTermination(BhParams.executorShutdownTimeout, TimeUnit.SECONDS);
			success &= recvProcExec.awaitTermination(BhParams.executorShutdownTimeout, TimeUnit.SECONDS);
			success &= sendProcExec.awaitTermination(BhParams.executorShutdownTimeout, TimeUnit.SECONDS);
			success &= terminationExec.awaitTermination(BhParams.executorShutdownTimeout, TimeUnit.SECONDS);
		}
		catch(InterruptedException e) {
			success &= false;
		}
		return success;
	}
	
	/**
	 * RemoteCmdProcessorオブジェクト(RMIオブジェクト)を探す
	 * @param ipAddr リモートオブジェクトのIPアドレス
	 * @param port RMIレジストリのポート
	 * @param name オブジェクトバインド時の名前
	 * @return Remoteオブジェクト
	 */
	public static Remote findRmoteObj(String ipAddr, int port, String name) 
		throws MalformedURLException, NotBoundException, RemoteException {

		return Naming.lookup("rmi://" + ipAddr + ":"+ port + "/" + name);
	}
	
	/**
	 * 引数で指定した文字が出るまでInputStreamから読み、文字列にして返す.<br>
	 * 返される文字列に endChar は含まれない.
	 * @param is 文字を読み込むストリーム
	 * @param endChar この文字が出るまでストリームを読む
	 * @return ストリームから読み込んだ文字列
	 */
	private String readStream(InputStream is, int endChar) {
		
		List<Byte> charList = new ArrayList<>();
		try {
			while (true) {
				charList.add((byte)is.read());
				byte[] s = new byte[] {charList.get(charList.size()-1)};
				if (charList.get(charList.size()-1) == endChar)
					break;
			}
		}
		catch(IOException e) {
			return null;
		}
		
		byte[] charArray = new byte[charList.size()-1];
		for (int i = 0; i < charArray.length; ++i) {
			charArray[i] = charList.get(i);
		}
		return new String(charArray, Charset.defaultCharset());
	}
}
