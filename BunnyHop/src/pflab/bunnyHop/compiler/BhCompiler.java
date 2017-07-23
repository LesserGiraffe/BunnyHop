package pflab.bunnyHop.compiler;

import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.List;
import java.util.Optional;
import javafx.scene.control.Alert;
import pflab.bunnyHop.root.MsgPrinter;
import pflab.bunnyHop.common.BhParams;
import pflab.bunnyHop.common.Util;
import pflab.bunnyHop.model.BhNode;
import pflab.bunnyHop.modelProcessor.SyntaxSymbolIDCreator;

/**
 * BhNode をコンパイルするクラス.
 * @author K.Koike
 */
public class BhCompiler {

	public static final BhCompiler instance = new BhCompiler() {};	//!< シングルトンインスタンス
	private final VarDeclCodeGenerator varDeclCodeGen;
	private final FuncDefCodeGenerator funcDefCodeGen;
	private final StatCodeGenerator statCodeGen;
	private String commonCode;

	private BhCompiler(){
		CommonCodeGenerator common = new CommonCodeGenerator();
		ExpCodeGenerator expCodeGen = new ExpCodeGenerator(common);
		varDeclCodeGen = new VarDeclCodeGenerator(common);
		statCodeGen = new StatCodeGenerator(common, expCodeGen, varDeclCodeGen);
		funcDefCodeGen = new FuncDefCodeGenerator(common, statCodeGen, varDeclCodeGen);
	}
	
	/**
	 * コンパイルに必要な初期化処理を行う.
	 * @return 初期化に成功した場合true
	 */
	public boolean init() {
		
		Path commonCodePath = Paths.get(Util.execPath, BhParams.Path.bhDefDir, BhParams.Path.javascriptDir, BhParams.Path.compiler, BhParams.Path.commonCode);
		try {
			byte[] content = Files.readAllBytes(commonCodePath);
			commonCode = new String(content, StandardCharsets.UTF_8);
		}
		catch (IOException e) {
			MsgPrinter.instance.ErrMsgForDebug(e.getMessage());
			return false;
		}		
		return true;
	}
	
	/**
	 * ワークスペース中のノードをコンパイルし, 作成されたファイルのパスを返す
	 * @param execNode 実行するノード
	 * @param compiledNodeList コンパイル対象のノードリスト (execNodeは含まない)
	 * @param option コンパイルオプション
	 * @return コンパイルした結果作成されたファイルのパス(コンパイルできた場合). <br>
	 *          コンパイルできなかった場合はOptional.empty
	 */
	public Optional<Path> compile(
		BhNode execNode, 
		List<BhNode> compiledNodeList, 
		CompileOption option) {
		
		if (!isExecutable(execNode))
			return Optional.empty();
		
		SyntaxSymbolIDCreator idCreator = new SyntaxSymbolIDCreator();
		execNode.accept(idCreator);
		compiledNodeList.forEach(compiledNode -> compiledNode.accept(idCreator));
		
		StringBuilder code = new StringBuilder();
		genCode(code, execNode, compiledNodeList, option);
		
		Path appFilePath = Paths.get(Util.execPath, BhParams.Path.compiled, BhParams.Path.appFileName);
		try (BufferedWriter writer = 
			Files.newBufferedWriter(
				appFilePath, 
				StandardCharsets.UTF_8, 
				StandardOpenOption.CREATE,
				StandardOpenOption.TRUNCATE_EXISTING, 
				StandardOpenOption.WRITE)) {
			writer.write(code.toString());
		}
		catch (IOException e) {
			Alert alert = new Alert(Alert.AlertType.ERROR);
			alert.setTitle("ファイル書き込みエラー");
			alert.setHeaderText(null);
			alert.setContentText(e.getMessage() + "\n" + appFilePath.toString());
			alert.showAndWait();
			return Optional.empty();
		}
		MsgPrinter.instance.MsgForUser("-- コンパイル成功 --\n");
		return Optional.of(appFilePath);
	}
	
	/**
	 * プログラム全体のコードを生成する.
	 * @param code 生成したソースコードの格納先
	 * @param execNode 実行するノード
	 * @param compiledNodeList コンパイル対象のノードリスト (execNodeは含まない)
	 * @param option コンパイルオプション
	 */
	private void genCode(
		StringBuilder code, 
		BhNode execNode, 
		List<BhNode> compiledNodeList, 
		CompileOption option) {
		
		code.append("(")
			.append(Keywords.JS._function)
			.append("(){")
			.append(Util.LF);
		code.append(commonCode);
		varDeclCodeGen.genGlobalVarDecls(compiledNodeList, code, 1, option);
		funcDefCodeGen.genFuncDefs(compiledNodeList, code, 1, option);
		statCodeGen.genStatement(execNode, code, 1, option);
		code.append("})();");
	}
	
	/**
	 * 引数で指定したノードが実行可能なノードかどうか判断する.
	 */
	private boolean isExecutable(BhNode node) {
		
		if (node.getState() != BhNode.State.ROOT_DIRECTLY_UNDER_WS) {
			Alert alert = new Alert(Alert.AlertType.ERROR);
			alert.setTitle("実行ノードエラー");
			alert.setHeaderText(null);
			alert.setContentText("処理の途中からは実行できません.");
			alert.showAndWait();
			return false;
		}
		return true;
	}
					
	public static class Keywords {
		public static final String varPrefix = "_v";
		public static final String funcPrefix = "_f";
		
		public static class JS {
			public static final String _if = "if ";
			public static final String _else = "else ";
			public static final String _while = "while ";
			public static final String _break = "break";
			public static final String _continue = "continue";
			public static final String _let = "let ";
			public static final String _const = "const ";
			public static final String _function = "function";
			public static final String _true = "true";
			public static final String _false = "false";
			public static final String _undefined = "undefined";
		}
	}
}


