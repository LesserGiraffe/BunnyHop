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

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Future;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Stream;
import javax.script.Bindings;
import javax.script.CompiledScript;
import javax.script.ScriptException;
import jdk.nashorn.api.scripting.ScriptObjectMirror;
import pflab.bunnyhop.bhprogram.common.BhProgramData;
import pflab.bunnyhop.common.BhParams;
import pflab.bunnyhop.common.Util;
import pflab.bunnyhop.configfilereader.BhScriptManager;
import pflab.bunnyhop.root.MsgPrinter;

/**
 * BunnyHopで作成したプログラムのローカル環境での実行、終了、通信を行うクラス
 * @author K.Koike
 */
public class RemoteBhProgramManager {
	
	public static final RemoteBhProgramManager instance = new RemoteBhProgramManager();	//!< シングルトンインスタンス
	private final BhProgramManagerCommon common = new BhProgramManagerCommon();
	private final Bindings bindings = BhScriptManager.instance.createScriptScope();
	private String[] killCmd;
	private AtomicReference<Boolean> programRunning = new AtomicReference(false);	//!< プログラム実行中ならtrue
	private AtomicReference<Boolean> continueCopyingFile = new AtomicReference(false);	//!< ファイルがコピー中の場合true
	
	private RemoteBhProgramManager() {}
	
	public boolean init() {
		boolean success = common.init();
		success &= BhScriptManager.instance.checkIfScriptsExist(
			RemoteBhProgramManager.class.getSimpleName(),
			BhParams.ExternalProgram.remoteExecCmdGenerator, 
			BhParams.ExternalProgram.remoteKillCmdGenerator,
			BhParams.ExternalProgram.copyCmdGenerator);
		
		if (!success)
			MsgPrinter.instance.ErrMsgForDebug("failed to initialize " + RemoteBhProgramManager.class.getSimpleName());			
		return success;
	}
	
	/**
	 * BhProgramを実行する
	 * @param filePath BhProgramのファイルパス
	 * @param ipAddr BhProgramを実行するマシンのIPアドレス
	 * @param uname BhProgramを実行するマシンにログインする際のユーザ名
	 * @param password BhProgramを実行するマシンにログインする際のパスワード
	 * @return BhProgram実行開始の完了待ちオブジェクト
	 */
	public Future<Boolean> executeAsync(Path filePath, String ipAddr, String uname, String password) {
		return common.executeAsync(() -> execute(filePath, ipAddr, uname, password));
	}
	
	/**
	 * BhProgramを実行する
	 * @param filePath BhProgramのファイルパス
	 * @param ipAddr BhProgramを実行するマシンのIPアドレス
	 * @return BhProgramの実行に成功した場合true
	 */
	private synchronized boolean execute(Path filePath, String ipAddr, String uname, String password) {
		
		boolean success = true;
		
		if (programRunning.get())
			success &= terminate();

		MsgPrinter.instance.MsgForUser("-- プログラム実行準備中 (remote) --\n");
		setScriptBindings(ipAddr, uname, password);
		killCmd = genKillCmd();						
		String[] copyCmd = genCopyCmd();	
		if (killCmd == null || copyCmd == null)
			success = false;
		
		if (copyCmd != null) {
			success &= copyFile(copyCmd);
		}
		
		Process remoteEnvProcess = null;
		if (success)
			remoteEnvProcess = startExecEnvProcess();	//リモート実行環境起動
		
		if (remoteEnvProcess != null) {
			String fileName = filePath.getFileName().toString();
			success &= common.runBhProgram(fileName, ipAddr, remoteEnvProcess.getInputStream());
			// リモートの実行環境の起動に使ったローカルのプロセスは終了しておく
			success &= common.waitForProcessEnd(remoteEnvProcess, true, BhParams.ExternalProgram.programExecEnvTerminationTimeout);
		}
		
		if (!success) {	//リモートでのスクリプト実行失敗
			MsgPrinter.instance.ErrMsgForUser("!! プログラム実行準備失敗 (remote) !!\n");
			MsgPrinter.instance.ErrMsgForDebug("failed to run " +filePath.getFileName().toString() + " (remote)");
			terminate();
		}
		else {
			MsgPrinter.instance.MsgForUser("-- プログラム実行開始 (remote) --\n");
			programRunning.set(true);
		}
		
		return success;
	}
	
	/**
	 * 現在実行中のBhProgramExecEnvironment を強制終了する
	 * @return 強制終了タスクの結果
	 */
	public Future<Boolean> terminateAsync() {
		
		continueCopyingFile.set(false);
		return common.terminateAsync(() -> {
			if (!programRunning.get()) {
				MsgPrinter.instance.ErrMsgForUser("!! プログラム終了済み (remote) !!\n");
				return true;	//エラーメッセージは出すが, 終了処理の結果は成功とする
			}
			return terminate();
		});
	}
	
	/**
	 * リモートマシンで実行中のBhProgram実行環境を強制終了する. <br>
	 * BhProgram終了済みの場合に呼んでも問題ない.
	 * @return 強制終了に成功した場合true
	 */
	private synchronized boolean terminate() {
		
		MsgPrinter.instance.MsgForUser("-- プログラム終了中 (remote)  --\n");
		boolean success = common.haltTransceiver();

		if (killCmd != null) {
			Process process = execCmd(killCmd);
			if (process != null)
				success &= common.waitForProcessEnd(process, false, BhParams.ExternalProgram.programExecEnvTerminationTimeout);
			else
				success = false;
		}
		
		if (!success) {
			MsgPrinter.instance.ErrMsgForUser("!! プログラム終了失敗 (remote)  !!\n"); 
		}
		else {
			MsgPrinter.instance.MsgForUser("-- プログラム終了完了 (remote)  --\n");
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
		CompiledScript cs = BhScriptManager.instance.getCompiledScript(BhParams.ExternalProgram.remoteExecCmdGenerator);		
		Object retVal = null;
		try {
			retVal = cs.eval(bindings);
		}
		catch(ScriptException e) {
			MsgPrinter.instance.ErrMsgForDebug("failed to eval " +  BhParams.ExternalProgram.remoteExecCmdGenerator + " " + e.toString());
			return null;
		}
	
		String[] cmdArray = null;
		if (retVal instanceof ScriptObjectMirror)
			cmdArray = convertToStrArray((ScriptObjectMirror)retVal);
	
		if (cmdArray == null)
			return null;
		
		ProcessBuilder procBuilder = new ProcessBuilder(cmdArray);
		procBuilder.redirectErrorStream(true);
		try {
			process = procBuilder.start();
		}
		catch (IOException | IndexOutOfBoundsException | SecurityException e) {	
			MsgPrinter.instance.ErrMsgForDebug("failed to start " +  BhParams.ExternalProgram.bhProgramExecEnvironment + "\n" + e.toString());
		}		
		return process;
	}
	
	/**
	 * BhProgram実行環境終了のコマンドを作成する
	 * @return BhProgram実行環境終了のコマンド. 作成に失敗した場合null.
	 */
	private String[] genKillCmd() {
		
		CompiledScript cs = BhScriptManager.instance.getCompiledScript(BhParams.ExternalProgram.remoteKillCmdGenerator);		
		Object retVal = null;
		try {
			retVal = cs.eval(bindings);
		}
		catch(ScriptException e) {
			MsgPrinter.instance.ErrMsgForDebug("failed to eval" +  BhParams.ExternalProgram.remoteKillCmdGenerator + " " + e.toString());
			return null;
		}
	
		String[] cmdArray = null;
		if (retVal instanceof ScriptObjectMirror)
			cmdArray = convertToStrArray((ScriptObjectMirror)retVal);
		return cmdArray;
	}
	
	/**
	 * BhProgram実行環境終了のコマンドを作成する
	 * @return BhProgramファイルコピーのコマンド. 作成に失敗した場合null.
	 */
	private String[] genCopyCmd() {
		
		CompiledScript cs = BhScriptManager.instance.getCompiledScript(BhParams.ExternalProgram.copyCmdGenerator);		
		Object retVal = null;
		try {
			retVal = cs.eval(bindings);
		}
		catch(ScriptException e) {
			MsgPrinter.instance.ErrMsgForDebug("failed to eval" +  BhParams.ExternalProgram.copyCmdGenerator + " " + e.toString());
			return null;
		}
	
		String[] cmdArray = null;
		if (retVal instanceof ScriptObjectMirror)
			cmdArray = convertToStrArray((ScriptObjectMirror)retVal);
		return cmdArray;
	}
	
	/**
	 * 引数で指定したコマンドを実行する
	 * @param cmd 実行するコマンド
	 * @return コマンドを実行したプロセスのオブジェクト. コマンド実行した場合はnull
	 */
	private Process execCmd(String[] cmd) {
		
		Process process = null;
		ProcessBuilder procBuilder = new ProcessBuilder(cmd);		
		procBuilder.redirectErrorStream(true);
		try {
			process = procBuilder.start();
		}
		catch (IOException | IndexOutOfBoundsException | SecurityException e) {	
			String cmdStr = Stream.of(cmd).reduce("", (p1, p2) -> p1 + " " + p2);
			MsgPrinter.instance.ErrMsgForDebug("failed to execCmd " +  cmdStr + "\n" + e.toString());
		}
		return process;
	}
	
	/**
	 * リモート環境にファイルをコピーする
	 * @param copyCmd コピーコマンド
	 * @return コピーが正常に終了した場合true
	 */
	private boolean copyFile(String[] copyCmd) {
		
		MsgPrinter.instance.MsgForUser("-- プログラム転送中 --\n");
		boolean success = true;
		
		continueCopyingFile.set(true);
		
		Process copyProcess = execCmd(copyCmd);
		if (copyProcess != null) {
			
			success &= showFileCopyProgress(copyProcess);
			boolean terminate = !success;	//進捗表示を途中で終わらせていた場合, ファイルコピープロセスを強制終了する
			success &= common.waitForProcessEnd(copyProcess, terminate, BhParams.ExternalProgram.fileCopyTerminationTimeout);
			success &= (copyProcess.exitValue() == 0);
		}
		
		if (!success) {
			MsgPrinter.instance.ErrMsgForDebug(RemoteBhProgramManager.class.getSimpleName() + ".copyFile  \n");
			MsgPrinter.instance.ErrMsgForUser("!! プログラム転送失敗 !!\n");
		}
		
		return success;
	}
	
	/**
	 * ファイルコピーの進捗状況を知らせる
	 * @param fileCopyProc ファイルコピーをしているプロセス
	 * @return コピープロセスが終了後にこのメソッドから戻った場合true
	 **/
	private boolean showFileCopyProgress(Process copyProcess) {
		
		boolean success = true;
		List<Character> charCodeList = new ArrayList<>();
		try (BufferedReader br = new BufferedReader(new InputStreamReader(copyProcess.getInputStream()))){		
			while (true) {
				if (!continueCopyingFile.get())	{//コピー停止命令を受けた
					success = false;
					break;
				}

				if (!copyProcess.isAlive())	//コピープロセス終了
					break;

				if (br.ready()) {	//次の読み出し結果がEOFの場合 false
					int charCode = br.read();
					switch (charCode) {
						case '\r':
						case '\n':	//<- pscp の場合 '\n' は入力されないので不要.
							if (!charCodeList.isEmpty()) {
								char[] charCodeArray = new char[charCodeList.size()];
								for (int i = 0; i < charCodeArray.length; ++i)
									charCodeArray[i] = charCodeList.get(i);							
								String progressInfo = new String(charCodeArray);	//サイズ0の配列の場合 readStr == '\0'
								MsgPrinter.instance.MsgForUser(progressInfo + "\n");
								charCodeList.clear();
							}
							break;

						default:	//改行以外の文字コード
							charCodeList.add((char)charCode);
					}
				}
				else {
					Thread.sleep(100);
				}					
			}
		}
		catch(IOException | InterruptedException e) {
			success = false;
		}
		return success;
	}
	
	/**
	 * コマンド生成スクリプトが使う情報をマップオブジェクトにセットする
	 * @param ipAddr 実行環境があるマシンのIPアドレス
	 * @param uname 実行環境があるマシンに接続する際のユーザ名
	 * @param password 実行環境があるマシンに接続する際のパスワード
	 */
	private void setScriptBindings(String ipAddr, String uname, String password) {
		
		bindings.put(BhParams.JsKeyword.keyIpAddr, ipAddr);
		bindings.put(BhParams.JsKeyword.keyUname, uname);
		bindings.put(BhParams.JsKeyword.keyPassword, password);
		bindings.put(BhParams.JsKeyword.keyExecExnvironment, BhParams.ExternalProgram.bhProgramExecEnvironment);
		bindings.put(BhParams.JsKeyword.keyBhProgramFilePath, 
					 Paths.get(Util.execPath, BhParams.Path.compiled, BhParams.Path.appFileName).toString());
	}
	
	/**
	 * BhProgramファイルをリモート環境にコピーする
	 * @param cmd コピーコマンド
	 * @return コピーに成功した場合true
	 */
	private boolean copyBhProgramFile(String[] cmd) {
		return true;
	}
	
	/**
	 * ScriptObjectMirrorをString配列に変換する
	 * @param scriptObj 文字列配列に変換するScriptObjectMirror
	 * @return scriptObjを変換したString配列. scriptObjが配列でない場合nullを返す.
	 */
	private String[] convertToStrArray(ScriptObjectMirror scriptObj) {
	
		if (!scriptObj.isArray())
			return null;
		
		String[] cmdArray = new String[scriptObj.size()];
		for (String key : scriptObj.keySet()) {
			Object elem = scriptObj.get(key);
			try {
				int idx = Integer.parseInt(key);
				cmdArray[idx] = elem.toString();
			}
			catch (NumberFormatException e) {
			}
		}
		return cmdArray;
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
