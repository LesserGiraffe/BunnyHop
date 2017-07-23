package pflab.bunnyHop.root;

import java.util.AbstractMap;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingDeque;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import pflab.bunnyHop.configFileReader.FXMLCollector;
import pflab.bunnyHop.configFileReader.BhScriptManager;
import javafx.application.Application;
import javafx.application.Platform;

import javafx.stage.Stage;
import pflab.bunnyHop.bhProgram.BhProgramData;
import pflab.bunnyHop.bhProgram.LocalBhProgramManager;
import pflab.bunnyHop.common.BhParams;
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
		
		boolean jsCompleHasSucceeded = BhScriptManager.instance.genCompiledCode();
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
	}
	
	/**
	 * 終了処理を登録する
	 */
	private void setOnCloseHandler(Stage stage) {
		stage.showingProperty().addListener((observable, oldValue, newValue) -> {
			if (oldValue == true && newValue == false) {
				LocalBhProgramManager.instance.end();
				System.exit(0);
			}
		});
	}
}

















