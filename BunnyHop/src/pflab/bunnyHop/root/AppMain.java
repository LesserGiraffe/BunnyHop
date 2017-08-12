package pflab.bunnyHop.root;

import java.io.BufferedReader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Scanner;

import pflab.bunnyHop.configFileReader.FXMLCollector;
import pflab.bunnyHop.configFileReader.BhScriptManager;
import javafx.application.Application;
import javafx.stage.Stage;
import pflab.bunnyHop.bhProgram.LocalBhProgramManager;
import pflab.bunnyHop.bhProgram.RemoteBhProgramManager;
import pflab.bunnyHop.common.BhParams;
import pflab.bunnyHop.common.Util;
import pflab.bunnyHop.compiler.BhCompiler;
import pflab.bunnyHop.model.templates.BhNodeTemplates;
import pflab.bunnyHop.view.BhNodeViewStyle;

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

















