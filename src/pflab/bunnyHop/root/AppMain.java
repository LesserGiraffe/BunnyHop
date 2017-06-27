package pflab.bunnyHop.root;


import pflab.bunnyHop.configFileReader.FXMLCollector;
import pflab.bunnyHop.configFileReader.BhScriptManager;
import javafx.application.Application;

import javafx.stage.Stage;
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
}

















