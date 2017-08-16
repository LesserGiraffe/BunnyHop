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

import java.io.BufferedReader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;

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
		boolean fxmlCollectionHasSucceeded = FXMLCollector.instance.collectFXMLFiles();
		if (!fxmlCollectionHasSucceeded)
			System.exit(-1);
		
		boolean jsCompleHasSucceeded = BhScriptManager.instance.genCompiledCode(
			Paths.get(Util.execPath, BhParams.Path.bhDefDir, BhParams.Path.javascriptDir),
			Paths.get(Util.execPath, BhParams.Path.remoteDir));
		if (!jsCompleHasSucceeded) {
			System.exit(-1);
		}

		boolean compilerInitHasSucceeded = BhCompiler.instance.init();
		if (!compilerInitHasSucceeded) {
			System.exit(-1);
		}

		boolean templateGenHasSucceeded =  BhNodeTemplates.instance().genTemplate();
		templateGenHasSucceeded &= BhNodeViewStyle.genViewStyleTemplate();
		templateGenHasSucceeded &= BhNodeViewStyle.checkNodeIdAndNodeTemplate();
		if (!templateGenHasSucceeded) {
			System.exit(-1);
		}
		
		BunnyHop.instance().createWindow(stage);

		boolean selectorGenHasSucceeded = BunnyHop.instance().genNodeCategoryList();
		if (!selectorGenHasSucceeded) {
			System.exit(-1);
		}
		
		if (!LocalBhProgramManager.instance.init())
			System.exit(-1);
		
		if (!RemoteBhProgramManager.instance.init())
			System.exit(-1);
	}
	
	/**
	 * 終了処理を登録する
	 */
	private void setOnCloseHandler(Stage stage) {
		stage.showingProperty().addListener((observable, oldValue, newValue) -> {
			if (oldValue == true && newValue == false) {
				LocalBhProgramManager.instance.end();
				RemoteBhProgramManager.instance.end();
				System.exit(0);
			}
		});
	}
}

















