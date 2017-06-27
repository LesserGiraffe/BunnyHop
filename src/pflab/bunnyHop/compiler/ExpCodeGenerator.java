package pflab.bunnyHop.compiler;

import java.util.ArrayList;
import java.util.List;
import pflab.bunnyHop.common.Util;
import pflab.bunnyHop.model.Imitatable;
import pflab.bunnyHop.model.SyntaxSymbol;
import pflab.bunnyHop.model.TextNode;

/**
 * 式のコード生成を行うクラス
 * @author K.Koike
 */
public class ExpCodeGenerator {
	
	private final CommonCodeGenerator common;
		
	public ExpCodeGenerator(CommonCodeGenerator common) {
		this.common = common;
	}
	
	/**
	 * 式を作成する
	 * @param code 途中式の格納先
	 * @param expNode 式のノード
	 * @param nestLevel ソースコードのネストレベル
	 * @param option コンパイルオプション
	 * @return 式もしくは式の評価結果を格納した変数
	 */
	public String genExpression(
		StringBuilder code, 
		SyntaxSymbol expNode, 
		int nestLevel,
		CompileOption option) {
		
		String expSymbolName = expNode.getSymbolName();
		if (SymbolNames.BinaryExp.list.contains(expSymbolName)) {
			return genBinaryExp(code, expNode, nestLevel, option);
		}
		else if (SymbolNames.UnaryExp.list.contains(expSymbolName)) {
			return genUnaryExp(code, expNode, nestLevel, option);
		}else if (SymbolNames.VarDecl.varList.contains(expSymbolName)) {
			Imitatable varNode = (Imitatable)expNode;
			return common.genVarName(varNode.getOriginalNode());
		}
		else if (SymbolNames.Literal.literalList.contains(expSymbolName)) {
			return genLiteral(expNode);
		}
		else if (SymbolNames.PreDefFunc.preDefFuncCallExpList.contains(expSymbolName)) {
			return genPreDefFuncCallExp(code, expNode, nestLevel, option, true);
		}
		return null;
	}
	
	/**
	 * 二項演算式を作成する
	 * @param code 途中式の格納先
	 * @param binaryExpNode 二項式のノード
	 * @param nestLevel ソースコードのネストレベル
	 * @param option コンパイルオプション
	 * @return 式もしくは式の評価結果を格納した変数
	 */
	private String genBinaryExp(
		StringBuilder code, 
		SyntaxSymbol binaryExpNode, 
		int nestLevel,
		CompileOption option) {
		
		SyntaxSymbol leftExp = binaryExpNode.findSymbolInDescendants("*",	SymbolNames.BinaryExp.leftExp, "*");
		String leftExpCode = genExpression(code, leftExp, nestLevel, option);
		SyntaxSymbol rightExp = binaryExpNode.findSymbolInDescendants("*", SymbolNames.BinaryExp.rightExp, "*");
		String rightExpCode = genExpression(code, rightExp, nestLevel, option);
		String operatorCode = null;
		if (binaryExpNode.getSymbolName().equals(SymbolNames.BinaryExp.modExp)) {
			operatorCode = " % ";
		}
		else {
			TextNode operator = (TextNode)binaryExpNode.findSymbolInDescendants("*", SymbolNames.BinaryExp.operator, "*");
			operatorCode = SymbolNames.BinaryExp.operatorMap.get(operator.getText());
		}
		
		if (leftExp == null || rightExp == null)
			return null;
		
		String tmpVar = common.genVarName(binaryExpNode);
		code.append(common.indent(nestLevel))
			.append(BhCompiler.Keywords.JS._let)
			.append(tmpVar)
			.append(" = ")
			.append(leftExpCode)
			.append(operatorCode)
			.append(rightExpCode)
			.append(";")
			.append(Util.LF);
		
		if (option.handleException) {
			if (SymbolNames.BinaryExp.arithExceptionExp.contains(binaryExpNode.getSymbolName())) {
				code.append(common.indent(nestLevel))
					.append(BhCompiler.Keywords.JS._if)
					.append("(!")
					.append(common.genFuncCallCode(SymbolNames.PreDefFunc.preDefFuncNameMap.get(SymbolNames.PreDefFunc.isFinite), tmpVar))
					.append(")")
					.append(Util.LF)
					.append(common.indent(nestLevel+1))
					.append(tmpVar)
					.append(" = ")
					.append(leftExpCode)
					.append(";")
					.append(Util.LF);
			}
		}
		return tmpVar;
	}
	
	/**
	 * 単項演算式を作成する
	 * @param code 途中式の格納先
	 * @param unaryExpNode 単項式のノード
	 * @param nestLevel ソースコードのネストレベル
	 * @param option コンパイルオプション
	 * @return 式もしくは式の評価結果を格納した変数
	 */
	private String genUnaryExp(
		StringBuilder code, 
		SyntaxSymbol unaryExpNode, 
		int nestLevel,
		CompileOption option) {
		
		SyntaxSymbol primaryExp = unaryExpNode.findSymbolInDescendants("*",	SymbolNames.UnaryExp.primaryExp, "*");
		String primaryExpCode = genExpression(code, primaryExp, nestLevel, option);
		String operatorCode = null;
		if (unaryExpNode.getSymbolName().equals(SymbolNames.UnaryExp.notExp)) {
			operatorCode = "!";
		}
		
		if (primaryExp == null)
			return null;
		
		String tmpVar = common.genVarName(unaryExpNode);
		code.append(common.indent(nestLevel))
			.append(BhCompiler.Keywords.JS._let)
			.append(tmpVar)
			.append(" = ")
			.append(operatorCode)
			.append(primaryExpCode)
			.append(";")
			.append(Util.LF);
		
		return tmpVar;
	}
	
	/**
	 * リテラルを作成する
	 * @param literalNode リテラルのノード
	 */
	private String genLiteral(SyntaxSymbol literalNode) {
		
		String inputText = "";
		if (literalNode instanceof TextNode)
			inputText = ((TextNode)literalNode).getText();
		
		switch (literalNode.getSymbolName()) {
			case SymbolNames.Literal.numLiteral:
				return inputText;

			case SymbolNames.Literal.strLiteral:
				return "'" + inputText.replaceAll("\\\\", "\\\\\\\\").replaceAll("'", "\\\\'") + "'";
			
			case SymbolNames.Literal.lineFeed:
				return "'\\n'";
				
			case SymbolNames.Literal.boolLiteral:
				return SymbolNames.Literal.boolLiteralMap.get(inputText);
		}
		return null;
	}
	
	/**
	 * 定義済み関数の呼び出し式を作成する
	 * @param code 関数呼び出し式の格納先
	 * @param funcCallNode 関数呼び出し式のノード
	 * @param nestLevel ソースコードのネストレベル
	 * @param option コンパイルオプション
	 * @param storeRetVal 戻り値を変数に格納するコードを出力する場合true.
	 * @return 式もしくは式の評価結果を格納した変数. storeRetValがfalseの場合はnull.
	 */
	public String genPreDefFuncCallExp(
		StringBuilder code, 
		SyntaxSymbol funcCallNode, 
		int nestLevel,
		CompileOption option,
		boolean storeRetVal) {
		
		int idArg = 0;
		List<String> argList = new ArrayList<>();
		while (true) {
			String argCnctrName = SymbolNames.PreDefFunc.arg + idArg;
			SyntaxSymbol argExp = funcCallNode.findSymbolInDescendants("*",	argCnctrName, "*");
			if (argExp == null)
				break;
			String arg = genExpression(code, argExp, nestLevel, option);
			argList.add(arg);
			++idArg;
		}
		
		String retValName = null;
		code.append(common.indent(nestLevel));
		if (storeRetVal) {
			retValName = common.genVarName(funcCallNode);
			code.append(retValName)
				.append(" = ");
		}
		String funcName = SymbolNames.PreDefFunc.preDefFuncNameMap.get(funcCallNode.getSymbolName());
		String[] argArray = argList.toArray(new String[argList.size()]);
		String funcCallCode = common.genFuncCallCode(funcName, argArray);
		code.append(funcCallCode)
			.append(";")
			.append(Util.LF);
		return retValName;
	}
	
	/**
	 * ユーザー定義関数の呼び出し式を生成する
	 * @param code 生成したコードの格納先
	 * @param funcCallNode 関数呼び出しのノード
	 * @param nestLevel ソースコードのネストレベル
	 * @param option コンパイルオプション
	 * @param storeRetVal 戻り値を変数に格納するコードを出力する場合true.
	 * @return 式もしくは式の評価結果を格納した変数. storeRetValがfalseの場合はnull.
	 */
	public String genUserDefFuncCallExp(
		StringBuilder code, 
		SyntaxSymbol funcCallNode, 
		int nestLevel,
		CompileOption option,
		boolean storeRetVal) {
	
		String funcName = common.genFuncName(((Imitatable)funcCallNode).getOriginalNode());
		SyntaxSymbol argment = funcCallNode.findSymbolInDescendants("*", SymbolNames.UserDefFunc.arg, "*");
		String argArray[] = new String[0];
		if (!argment.getSymbolName().equals(SymbolNames.UserDefFunc.argVoid)) {
			List<String> argList = new ArrayList<>();
			genArgList(code, argment, argList, nestLevel, option);
			argArray = argList.toArray(new String[argList.size()]);
		}
		
		String  funcCallCode = common.genFuncCallCode(funcName, argArray);
		String retValName = null;
		code.append(common.indent(nestLevel));
		if (storeRetVal) {
			retValName = common.genVarName(funcCallNode);
			code.append(retValName)
				.append(" = ");
		}
		code.append(funcCallCode)
			.append(";")
			.append(Util.LF);
		
		return retValName;
	}
	
	/**
	 * ユーザ定義関数の引数リストを作成する
	 * @param code 生成したコードの格納先
	 * @param argNode 引数ノード
	 * @param argList 引数の格納先
	 * @param nestLevel ソースコードのネストレベル
	 * @param option コンパイルオプション
	 */
	private void genArgList(
		StringBuilder code,
		SyntaxSymbol argNode,
		List<String> argList,
		int nestLevel,
		CompileOption option) {
		
		SyntaxSymbol argment = argNode.findSymbolInDescendants("*", SymbolNames.UserDefFunc.arg, "*");
		String argCode = genExpression(code, argment, nestLevel, option);
		argList.add(argCode);
		
		SyntaxSymbol nextArg = argNode.findSymbolInDescendants("*", SymbolNames.UserDefFunc.nextArg, "*");
		
		if (!nextArg.getSymbolName().equals(SymbolNames.UserDefFunc.argVoid)) {
			genArgList(code, nextArg, argList, nestLevel, option);
		}
	}
}
