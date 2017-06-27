package pflab.bunnyHop.compiler;

import java.util.List;
import pflab.bunnyHop.common.Util;
import pflab.bunnyHop.model.SyntaxSymbol;

/**
 * 関数定義のコード生成を行うクラス
 * @author K.Koike
 */
public class FuncDefCodeGenerator {
	
	private final CommonCodeGenerator common;
	private final StatCodeGenerator statCodeGen;
	private final VarDeclCodeGenerator varDeclCodeGen;
	
	public FuncDefCodeGenerator(
		CommonCodeGenerator common,
		StatCodeGenerator statCodeGen,
		VarDeclCodeGenerator varDeclCodeGen) {
		this.common = common;
		this.statCodeGen = statCodeGen;
		this.varDeclCodeGen = varDeclCodeGen;
	}
	
	/**
	 * 関数定義のコードを作成する
	 * @param compiledNodeList コンパイル対象のノードリスト
	 * @param code 生成したコードの格納先
	 * @param nestLevel ソースコードのネストレベル
	 * @param option コンパイルオプション
	 */
	public void genFuncDefs(
		List<? extends SyntaxSymbol> compiledNodeList,
		StringBuilder code, 
		int nestLevel,
		CompileOption option) {
		
		compiledNodeList.forEach(symbol -> {
			if (SymbolNames.UserDefFunc.userDefFuncList.contains(symbol.getSymbolName())) {
				genFuncDef(symbol, code, nestLevel, option);
			}
		});
	}
	
	/**
	 * 関数定義のコードを作成する
	 * @param funcDefNode 関数定義のノード
	 * @param code 生成したコードの格納先
	 * @param nestLevel ソースコードのネストレベル
	 * @param option コンパイルオプション
	 */
	private void genFuncDef(
		SyntaxSymbol funcDefNode,
		StringBuilder code, 
		int nestLevel,
		CompileOption option) {
	
		String funcName = common.genFuncName(funcDefNode);
		code.append(common.indent(nestLevel))
			.append(BhCompiler.Keywords.JS._function)
			.append(" ")
			.append(funcName)
			.append("(");

		SyntaxSymbol param = funcDefNode.findSymbolInDescendants("*", "*", SymbolNames.UserDefFunc.paramDecl, "*");
		varDeclCodeGen.genParamList(param, code, nestLevel + 1, option);
		code.append(") {")
			.append(Util.LF);

		SyntaxSymbol stat = funcDefNode.findSymbolInDescendants("*", "*", SymbolNames.UserDefFunc.stat, "*");
		statCodeGen.genStatement(stat, code, nestLevel + 1, option);
		code.append(common.indent(nestLevel))
			.append("}")
			.append(Util.LF)
			.append(Util.LF);
	}
}











