package pflab.bunnyHop.bhProgram;

import java.io.BufferedReader;
import pflab.bunnyHop.bhProgram.common.BhProgramData;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Stream;
import javafx.scene.control.Alert;
import pflab.bunnyHop.common.BhParams;
import pflab.bunnyHop.common.Util;
import pflab.bunnyHop.root.MsgPrinter;

/**
 * BunnyHopで作成したプログラムのローカル環境での実行、終了、通信を行うクラス
 * @author K.Koike
 */
public class LocalBhProgramManager {
	
	public static final LocalBhProgramManager instance = new LocalBhProgramManager();	//!< シングルトンインスタンス
	private final BhProgramManagerCommon common = new BhProgramManagerCommon();
	private Process process;
	private AtomicReference<Boolean> programRunning = new AtomicReference(false);	//!< プログラム実行中ならtrue
	
	private LocalBhProgramManager() {}
	
	public boolean init() {
		return common.init();
	}
	
	/**
	 * BhProgramの実行環境を立ち上げ、BhProgramを実行する
	 * @param filePath BhProgramのファイルパス
	 * @param ipAddr BhProgramを実行するマシンのIPアドレス
	 * @return BhProgram実行タスクのFutureオブジェクト
	 */
	public Future<Boolean> executeAsync(Path filePath, String ipAddr) {
		return common.executeAsync(() -> execute(filePath, ipAddr));
	}
	
	/**
	 * BhProgramの実行環境を立ち上げ、BhProgramを実行する
	 * @param filePath BhProgramのファイルパス
	 * @param ipAddr BhProgramを実行するマシンのIPアドレス
	 * @return BhProgramの実行に成功した場合true
	 */
	private synchronized boolean execute(Path filePath, String ipAddr) {
		
		boolean success = true;
		
		if (programRunning.get())
			success &= terminate();
		
		MsgPrinter.instance.MsgForUser("-- プログラム実行準備中 (local) --\n");
		if (success) {
			process = startExecEnvProcess();
			if (process == null) {
				success &= false;
			}
		}
		
		if (process != null) {
			String fileName = filePath.getFileName().toString();
			success &= common.runBhProgram(fileName, ipAddr, process.getInputStream());
		}
	
		if (!success) {	//リモートでのスクリプト実行失敗
			MsgPrinter.instance.ErrMsgForUser("!! プログラム実行準備失敗 (local) !!\n");
			MsgPrinter.instance.ErrMsgForDebug("failed to run " +filePath.getFileName().toString() + " (local)");
			terminate();
		}
		else {
			MsgPrinter.instance.MsgForUser("-- プログラム実行開始 (local) --\n");
			programRunning.set(true);
		}

		return success;
	}
	
	/**
	 * 現在実行中のBhProgramExecEnvironment を強制終了する
	 * @return BhProgram強制終了タスクのFutureオブジェクト
	 */
	public Future<Boolean> terminateAsync() {
		
		return common.terminateAsync(() -> {
			if (!programRunning.get()) {
				MsgPrinter.instance.ErrMsgForUser("!! プログラム終了済み (local) !!\n");
				return true;	//エラーメッセージは出すが, 終了処理の結果は成功とする
			}
			return terminate();
		});
	}
	
	/**
	 * 現在実行中のBhProgramExecEnvironment を強制終了する
	 * @return 強制終了に成功した場合true
	 */
	public synchronized boolean terminate() {
		
		MsgPrinter.instance.MsgForUser("-- プログラム終了中 (local)  --\n");
		boolean success = common.haltTransceiver();
		
		if (process != null) {
			success &= common.waitForProcessEnd(process, true, BhParams.ExternalProgram.programExecEnvTerminationTimeout);
		}
		process = null;		
		if (!success) {
			MsgPrinter.instance.ErrMsgForUser("!! プログラム終了失敗 (local)  !!\n"); 
		}
		else {
			MsgPrinter.instance.MsgForUser("-- プログラム終了完了 (local)  --\n");
			programRunning.set(false);
		}
		return success;
	}
		
	/**
	 * BhProgram の実行環境と通信を行うようにする
	 */
	public void connectAsync() {
		common.connectAsync();
	}
	
	/**
	 * BhProgram の実行環境と通信を行わないようにする
	 */
	public void disconnectAsync() {
		common.disconnectAsync();
	}
	
	/**
	 * 引数で指定したデータをBhProgramの実行環境に送る
	 * @param data 送信データ
	 * @return 送信データリストにデータを追加できた場合true
	 */
	public BhProgramExecEnvError sendAsync(BhProgramData data) {
		return common.sendAsync(data);
	}
	
	/**
	 * BhProgramの実行環境プロセスをスタートする
	 * @return スタートしたプロセスのオブジェクト. スタートに失敗した場合null.
	 */
	private Process startExecEnvProcess() {
		
		Process process = null;
		ProcessBuilder procBuilder = 
			new ProcessBuilder(
				"java", 
				"-jar", 
				Paths.get(Util.execPath, BhParams.ExternalProgram.bhProgramExecEnvironment).toString(),
				"true");	//localFlag == true
		procBuilder.redirectErrorStream(true);
		try {
			process = procBuilder.start();
		}
		catch (IOException e) {
		}
		
		return process;
	}
	
	/**
	 * 終了処理をする
	 * @return 終了処理が正常に完了した場合true
	 */
	public boolean end() {
		
		boolean success = terminate();
		success &= common.end();
		return success;
	}
}
