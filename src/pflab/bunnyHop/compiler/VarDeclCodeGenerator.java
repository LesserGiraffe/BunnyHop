package pflab.bunnyHop.compiler;

import java.util.ArrayList;
import java.util.List;
import pflab.bunnyHop.common.Util;
import pflab.bunnyHop.model.BhNode;
import pflab.bunnyHop.model.SyntaxSymbol;
import pflab.bunnyHop.model.TextNode;

/**
 * @author K.Koike
 * 変数定義のコードを生成するクラス
 */
final public class VarDeclCodeGenerator {
	
	private final CommonCodeGenerator common;
		
	public VarDeclCodeGenerator(CommonCodeGenerator common) {
		this.common = common;
	}
	
	/**
	 * グローバル変数定義のコードを作成する
	 * @param compiledNodeList コンパイル対象のノードリスト
	 * @param code 生成したコードの格納先
	 * @param nestLevel ソースコードのネストレベル
	 * @param option コンパイルオプション
	 */
	public void genGlobalVarDecls(
		List<? extends SyntaxSymbol> compiledNodeList,
		StringBuilder code,
		int nestLevel, 
		CompileOption option) {
		
		List<VarDeclCodeGenerator.VarDeclInfo> varDeclInfoList = new ArrayList<>();
		compiledNodeList.forEach(node -> {
			if (SymbolNames.VarDecl.varDeclList.contains(node.getSymbolName())) {
				genVarDeclInfos(node, varDeclInfoList);
			}
		});
		genVarDecls(code, varDeclInfoList, nestLevel, option);
	}

	/**
	 * 仮引数
	 * @param paramNode 仮引数のノード
	 * @param code 生成したコードの格納先
	 * @param nestLevel ソースコードのネストレベル
	 * @param option コンパイルオプション
	 */	
	public void genParamList(
		SyntaxSymbol paramNode,
		StringBuilder code,
		int nestLevel,
		CompileOption option) {
		
		List<VarDeclCodeGenerator.VarDeclInfo> varDeclInfoList = new ArrayList<>();
		genVarDeclInfos(paramNode, varDeclInfoList);
		
		if (varDeclInfoList.size() >= 1)
			code.append(Util.LF);
		
		for (int i = 0; i < varDeclInfoList.size(); ++i) {
			
			VarDeclCodeGenerator.VarDeclInfo varDeclInfo = varDeclInfoList.get(i);
			code.append(common.indent(nestLevel))
				.append(varDeclInfo.varName);
			boolean isLastParam = i == (varDeclInfoList.size() - 1);
			if (!isLastParam) {
				code.append(",");
			}
			if (option.withComments) {
				code.append(" /*")
					.append(varDeclInfo.comment)
					.append("*/");
			}
			if (!isLastParam) {
				code.append(Util.LF);
			}
		}
	}
	
	
	/**
	 * 変数定義ノードから変数定義リストを取得する
	 * @param varDeclNode 変数定義ノード
	 * @param varDeclInfoList 変数定義に必要な情報のリスト
	 */
	private void genVarDeclInfos(SyntaxSymbol varDeclNode, List<VarDeclInfo> varDeclInfoList) {
	
		if (!SymbolNames.VarDecl.varDeclList.contains(varDeclNode.getSymbolName()))
			return;
				
		TextNode varIdTextNode = (TextNode)varDeclNode.findSymbolInDescendants("*", SymbolNames.VarDecl.varName, 	"*");
		String varName = common.genVarName(varDeclNode);
		String comment = varIdTextNode.getText();
		String initVal = SymbolNames.VarDecl.initValMap.get(varDeclNode.getSymbolName());
		varDeclInfoList.add(new VarDeclInfo(varName, initVal, comment));
		
		SyntaxSymbol nextVarDecl = varDeclNode.findSymbolInDescendants("*", SymbolNames.VarDecl.nextVarDecl, "*");

		genVarDeclInfos(nextVarDecl, varDeclInfoList);
	}
	
	/**
	 * 変数定義リストのコードを生成する
	 * @param code 生成したコードの格納先
	 * @param varDeclInfoList 変数定義に必要な情報のリスト
	 * @param nestLevel ソースコードのネストレベル
	 * @param option コンパイルオプション
	 */
	private void genVarDecls(
		StringBuilder code,
		List<VarDeclInfo> varDeclInfoList, 
		int nestLevel, 
		CompileOption option) {
		
		varDeclInfoList.forEach(varDeclInfo -> {
			
			code.append(common.indent(nestLevel))
				.append(BhCompiler.Keywords.JS._let)
				.append(varDeclInfo.varName)
				.append(" = ")
				.append(varDeclInfo.initVal)
				.append(";");
			
			if (option.withComments) {
				code.append(" /*")
					.append(varDeclInfo.comment)
					.append("*/");
			}
			code.append(Util.LF);
		});
	}
	
	/**
	 * 変数定義に必要な情報
	 */
	class VarDeclInfo {
		
		public final String varName;	//!< 変数名
		public final String initVal;	//!< 初期値
		public final String comment;	//!< コメント (デバッグ用)
		
		public VarDeclInfo(String varName, String initVal, String comment) {
			this.varName = varName;
			this.initVal = initVal;
			this.comment = comment;
		}
	}
}
