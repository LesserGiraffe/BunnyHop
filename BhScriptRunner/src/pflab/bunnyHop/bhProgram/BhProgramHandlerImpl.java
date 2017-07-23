package pflab.bunnyHop.bhProgram;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.Serializable;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import javax.script.Bindings;
import javax.script.Compilable;
import javax.script.CompiledScript;
import javax.script.ScriptEngine;
import javax.script.ScriptException;
import jdk.nashorn.api.scripting.NashornScriptEngineFactory;

/**
 * スクリプトとBunnyHop間でデータを送受信するクラス
 * @author K.Koike
 */
public class BhProgramHandlerImpl implements BhProgramHandler, Serializable {	
	
	private final ExecutorService bhProgramExec = Executors.newSingleThreadExecutor();
	private final ExecutorService recvDataProcessor = Executors.newSingleThreadExecutor();
	private final BlockingQueue<BhProgramData> sendDataList = new ArrayBlockingQueue<>(BhParams.maxQueueSize);	//!< to BunnyHop
	private final BlockingQueue<BhProgramData> recvDataList = new ArrayBlockingQueue<>(BhParams.maxQueueSize);	//!< from BunnyHop
	private final AtomicBoolean connected = new AtomicBoolean(false);	//!< BunnyHopとの通信が有効な場合true
	private final ScriptInOut scriptIO = new ScriptInOut(sendDataList, connected);	//!< BhProgramの入出力用オブジェクト
	
	public BhProgramHandlerImpl(){}
	
	/**
	 * 初期化する
	 */
	public boolean init() {
		
		recvDataProcessor.submit(() ->{
			processRecvData();
		});
		return true;
	}
	
	@Override
	public boolean runScript(String fileName) {
		
		ScriptEngine engine = (new NashornScriptEngineFactory()).getScriptEngine("--language=es6");
		Path scriptPath = Paths.get(Util.execPath, Util.scriptDir, fileName);
		boolean success = true;

		try (BufferedReader reader = Files.newBufferedReader(scriptPath, StandardCharsets.UTF_8)){
			CompiledScript cs = ((Compilable)engine).compile(reader);
			bhProgramExec.submit(() -> {
				try {
					Bindings binding = engine.createBindings();
					binding.put(BhParams.BhProgram.inoutModuleName, scriptIO);
					cs.eval(binding);
				}
				catch (ScriptException e) {}
			});
		}
		catch (IOException | ScriptException e) {
			success = false;
		}
		return success;
	}
	
	@Override
	public boolean sendDataToScript(BhProgramData data) {
		
		boolean success = false;
		try {
			success = recvDataList.offer(data, BhParams.pushRecvDataTimeout, TimeUnit.SECONDS);
		}
		catch(InterruptedException e) {}
		return success;
	}

	@Override
	public BhProgramData recvDataFromScript() {
		
		BhProgramData data = null;
		try {
			data = sendDataList.poll(BhParams.popSendDataTimeout, TimeUnit.SECONDS);
		}
		catch(InterruptedException e) {}
		return data;
	}
	
	@Override
	public void connect() {
		connected.set(true);
	}
	
	@Override
	public void disconnect() {
		connected.set(false);
		sendDataList.clear();
	}
	
	/**
	 * BunnyHopから受信したデータを処理し続ける
	 */
	private void processRecvData() {
		
		while(true) {
		
			BhProgramData data = null;
			try {
				data = recvDataList.take();
			}
			catch(InterruptedException e) {
				break;
			}
			
			switch (data.type) {
				case INPUT_STR:
					scriptIO.addStdInData(data.str);
					break;
			}
		}
	}
	
}
