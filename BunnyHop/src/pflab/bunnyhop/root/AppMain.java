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
package pflab.bunnyhop.root;

import java.nio.file.Paths;

import pflab.bunnyhop.configfilereader.FXMLCollector;
import pflab.bunnyhop.configfilereader.BhScriptManager;
import javafx.application.Application;
import javafx.stage.Stage;
import pflab.bunnyhop.bhprogram.LocalBhProgramManager;
import pflab.bunnyhop.bhprogram.RemoteBhProgramManager;
import pflab.bunnyhop.common.BhParams;
import pflab.bunnyhop.common.Util;
import pflab.bunnyhop.compiler.BhCompiler;
import pflab.bunnyhop.model.templates.BhNodeTemplates;
import pflab.bunnyhop.view.BhNodeViewStyle;

/**
 * メインクラス
 * @author K.Koike
 */
public class AppMain extends Application {

	public static void main(String[] args) {
		launch(args);
	}

	@Override
	public void start(Stage stage) throws Exception {	
		
		setOnCloseHandler(stage);
		
		if (!MsgPrinter.instance.init())
			System.exit(-1);
		
		if (!FXMLCollector.instance.collectFXMLFiles())
			System.exit(-1);
		
		boolean success = BhScriptManager.instance.genCompiledCode(
			Paths.get(Util.EXEC_PATH, BhParams.Path.BH_DEF_DIR, BhParams.Path.FUNCTIONS_DIR),
			Paths.get(Util.EXEC_PATH, BhParams.Path.BH_DEF_DIR, BhParams.Path.TEMPLATE_LIST_DIR),
			Paths.get(Util.EXEC_PATH, BhParams.Path.REMOTE_DIR));
		if (!success) {
			System.exit(-1);
		}
		
		if (!BhCompiler.instance.init()) {
			System.exit(-1);
		}

		success =  BhNodeTemplates.instance().genTemplate();
		success &= BhNodeViewStyle.genViewStyleTemplate();
		success &= BhNodeViewStyle.checkNodeIdAndNodeTemplate();
		if (!success)
			System.exit(-1);
		
		BunnyHop.instance.createWindow(stage);
	
		if (!BunnyHop.instance.genNodeCategoryList())
			System.exit(-1);
		
		if (!LocalBhProgramManager.instance.init())
			System.exit(-1);
		
		if (!RemoteBhProgramManager.instance.init())
			System.exit(-1);
	}
	
	/**
	 * 終了処理を登録する
	 */
	private void setOnCloseHandler(Stage stage) {
		
		stage.setOnCloseRequest(event ->{
			if (!BunnyHop.instance.processCloseRequest())
				event.consume();
		});
		
		stage.showingProperty().addListener((observable, oldValue, newValue) -> {
			if (oldValue == true && newValue == false) {
				LocalBhProgramManager.instance.end();
				RemoteBhProgramManager.instance.end();
				MsgPrinter.instance.end();
				System.exit(0);
			}
		});
	}
}

















