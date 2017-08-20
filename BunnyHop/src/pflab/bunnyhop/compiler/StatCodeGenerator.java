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
package pflab.bunnyhop.compiler;

import pflab.bunnyhop.common.Util;
import pflab.bunnyhop.model.Imitatable;
import pflab.bunnyhop.model.SyntaxSymbol;

/**
 * 文のコード生成を行うクラス
 * @author K.Koike
 */
public class StatCodeGenerator {

	private final CommonCodeGenerator common;
	private final ExpCodeGenerator expCodeGen;
	private final VarDeclCodeGenerator varDeclCodeGen;
	
	public StatCodeGenerator(
		CommonCodeGenerator common, 
		ExpCodeGenerator expCodeGen,
		VarDeclCodeGenerator varDeclCodeGen) {
		this.common = common;
		this.expCodeGen = expCodeGen;
		this.varDeclCodeGen = varDeclCodeGen;
	}

	/**
	 * statement のコードを生成する
	 * @param statementNode statement系のノード (代入文, if文など)
	 * @param code 生成したコードの格納先
	 * @param nestLevel ソースコードのネストレベル
	 * @param option コンパイルオプション
	 */
	public void genStatement(
		SyntaxSymbol statementNode, 
		StringBuilder code, 
		int nestLevel,
		CompileOption option) {

		String statSymbolName = statementNode.getSymbolName();
		if (statSymbolName.equals(SymbolNames.Stat.voidStat))	//void がつながっているときは, そこで文終了
			return;
		
		if (SymbolNames.AssignStat.list.contains(statSymbolName)) {
			genAssignStat(code, statementNode, nestLevel, option);
		}
		else if (SymbolNames.UserDefFunc.userDefFuncCallStatList.contains(statSymbolName)){
			expCodeGen.genUserDefFuncCallExp(code, statementNode, nestLevel, option, false);
		}
		else if (SymbolNames.PreDefFunc.preDefFuncCallStatList.contains(statSymbolName)) {
			expCodeGen.genPreDefFuncCallExp(code, statementNode, nestLevel, option, false);
		}
		else if (SymbolNames.ControlStat.list.contains(statSymbolName)) {
			genControlStat(code, statementNode, nestLevel, option);
		}
		else {
			return;
		}
		SyntaxSymbol nextStat = statementNode.findSymbolInDescendants("*", SymbolNames.AssignStat.nextStat, "*");
		if (nextStat == null)
			nextStat = statementNode.findSymbolInDescendants("*", "*", SymbolNames.AssignStat.nextStat, "*");	//for compoundStat
		
		if (nextStat != null)
			genStatement(nextStat, code, nestLevel, option);
	}
	
	
	/**
	 * 代入文のコードを生成する
	 * @param code 生成したコードの格納先
	 * @param assignStatNode 代入文のノード
	 * @param nestLevel ソースコードのネストレベル
	 * @param option コンパイルオプション
	 */
	private void genAssignStat(
		StringBuilder code, 
		SyntaxSymbol assignStatNode, 
		int nestLevel,
		CompileOption option) {
	
		SyntaxSymbol varSymbol = assignStatNode.findSymbolInDescendants("*", SymbolNames.AssignStat.leftVar, "*");
		String varName = null;
		if (SymbolNames.VarDecl.varList.contains(varSymbol.getSymbolName())) {	//varNode である
			Imitatable varNode = (Imitatable)varSymbol;
			varName = common.genVarName(varNode.getOriginalNode());
		}
		
		SyntaxSymbol rightExp = assignStatNode.findSymbolInDescendants("*", SymbolNames.BinaryExp.rightExp, "*");
		String rightExpCode = expCodeGen.genExpression(code, rightExp, nestLevel, option);
		
		if (varName == null || rightExpCode == null)
			return;
		
		String assignOpe = " = ";
		if (assignStatNode.getSymbolName().equals(SymbolNames.AssignStat.numAddAssignStat))
			assignOpe = " += ";
		
		code.append(common.indent(nestLevel))
			.append(varName)
			.append(assignOpe)
			.append(rightExpCode)
			.append(";")
			.append(Util.LF);
	}
	
	/**
	 * 制御文のコードを生成する
	 * @param code 生成したコードの格納先
	 * @param controlStatNode 制御文のノード
	 * @param nestLevel ソースコードのネストレベル
	 * @param option コンパイルオプション
	 */
	private void genControlStat(
		StringBuilder code, 
		SyntaxSymbol controlStatNode, 
		int nestLevel,
		CompileOption option) {
	
		String symbolName = controlStatNode.getSymbolName();
		
		switch (symbolName) {
			case SymbolNames.ControlStat.breakStat:
				code.append(common.indent(nestLevel)).append(BhCompiler.Keywords.JS._break).append(";").append(Util.LF);
				break;
				
			case SymbolNames.ControlStat.continueStat:
				code.append(common.indent(nestLevel)).append(BhCompiler.Keywords.JS._continue).append(";").append(Util.LF);
				break;
				
			case SymbolNames.ControlStat.ifElseStat:
			case SymbolNames.ControlStat.ifStat:
				genIfElseStat(code, controlStatNode, nestLevel, option);
				break;
				
			case SymbolNames.ControlStat.whileStat:
				genWhileStat(code, controlStatNode, nestLevel, option);
				break;
				
			case SymbolNames.ControlStat.compoundStat:
				genCompoundStat(code, controlStatNode, nestLevel, option);
				break;
			
			case SymbolNames.ControlStat.repeatStat:
				genRepeatStat(code, controlStatNode, nestLevel, option);
				break;
				
			default:
				break;
		}
	}
	
	/**
	 * 条件分岐文のコードを生成する
	 * @param code 生成したコードの格納先
	 * @param ifElseStatNode 制御文のノード
	 * @param nestLevel ソースコードのネストレベル
	 * @param option コンパイルオプション
	 */
	private void genIfElseStat(
		StringBuilder code, 
		SyntaxSymbol ifElseStatNode, 
		int nestLevel,
		CompileOption option) {
	
		//conditional part
		SyntaxSymbol condExp = ifElseStatNode.findSymbolInDescendants("*", SymbolNames.ControlStat.condExp, "*");
		String condExpCode = expCodeGen.genExpression(code, condExp, nestLevel, option);
		code.append(common.indent(nestLevel))
			.append(BhCompiler.Keywords.JS._if)
			.append("(")
			.append(condExpCode)
			.append(") {")
			.append(Util.LF);
		
		//then part
		SyntaxSymbol thenStat = ifElseStatNode.findSymbolInDescendants("*", SymbolNames.ControlStat.thenStat, "*");
		genStatement(thenStat, code, nestLevel+1, option);
		code.append(common.indent(nestLevel))
			.append("}")
			.append(Util.LF);
		
		//else part
		SyntaxSymbol elseStat = ifElseStatNode.findSymbolInDescendants("*", SymbolNames.ControlStat.elseStat, "*");
		if (elseStat != null) {
			code.append(common.indent(nestLevel))
				.append(BhCompiler.Keywords.JS._else)
				.append("{")
				.append(Util.LF);
			genStatement(elseStat, code, nestLevel+1, option);
			code.append(common.indent(nestLevel))
				.append("}")
				.append(Util.LF);
		}
	}
	
	/**
	 * While文のコードを生成する
	 * @param code 生成したコードの格納先
	 * @param whileStatNode while文のノード
	 * @param nestLevel ソースコードのネストレベル
	 * @param option コンパイルオプション
	 */
	private void genWhileStat(
		StringBuilder code, 
		SyntaxSymbol whileStatNode, 
		int nestLevel,
		CompileOption option) {
	
		//conditional part
		code.append(common.indent(nestLevel))
			.append(BhCompiler.Keywords.JS._while)
			.append("(")
			.append(BhCompiler.Keywords.JS._true)
			.append(") {")
			.append(Util.LF);
		
		SyntaxSymbol condExp = whileStatNode.findSymbolInDescendants("*", SymbolNames.ControlStat.condExp, "*");
		String condExpCode = expCodeGen.genExpression(code, condExp, nestLevel+1, option);
		code.append(common.indent(nestLevel + 1))
			.append(BhCompiler.Keywords.JS._if)
			.append("(!")
			.append(condExpCode)
			.append(") {")
			.append(Util.LF)
			.append(common.indent(nestLevel + 2))
			.append(BhCompiler.Keywords.JS._break)
			.append(";")
			.append(Util.LF)
			.append(common.indent(nestLevel + 1))
			.append("}")
			.append(Util.LF);
		
		//loop part
		SyntaxSymbol loopStat = whileStatNode.findSymbolInDescendants("*", SymbolNames.ControlStat.loopStat, "*");
		genStatement(loopStat, code, nestLevel+1, option);
		code.append(common.indent(nestLevel))
			.append("}")
			.append(Util.LF);
	}
	
	/**
	 * While文のコードを生成する
	 * @param code 生成したコードの格納先
	 * @param compoundStatNode block文のノード
	 * @param nestLevel ソースコードのネストレベル
	 * @param option コンパイルオプション
	 */
	private void genCompoundStat(
		StringBuilder code, 
		SyntaxSymbol compoundStatNode, 
		int nestLevel,
		CompileOption option) {
	
		code.append(common.indent(nestLevel))
			.append("{")
			.append(Util.LF);
		SyntaxSymbol param = compoundStatNode.findSymbolInDescendants("*", "*", SymbolNames.ControlStat.localVarDecl, "*");
		varDeclCodeGen.genVarDecls(param, code, nestLevel + 1, option);
		SyntaxSymbol stat = compoundStatNode.findSymbolInDescendants("*", "*", SymbolNames.Stat.statList, "*");
		genStatement(stat, code, nestLevel + 1, option);
		code.append(common.indent(nestLevel))
			.append("}")
			.append(Util.LF);
	}

	/**
	 * Repeat文のコードを生成する
	 * @param code 生成したコードの格納先
	 * @param repeatStatNode Repeat文のノード
	 * @param nestLevel ソースコードのネストレベル
	 * @param option コンパイルオプション
	 */	
	private void genRepeatStat(
		StringBuilder code, 
		SyntaxSymbol repeatStatNode, 
		int nestLevel,
		CompileOption option) {
	
		SyntaxSymbol condExp = repeatStatNode.findSymbolInDescendants("*", SymbolNames.ControlStat.condExp, "*");
		String condExpCode = expCodeGen.genExpression(code, condExp, nestLevel, option);
		String loopCounter = common.genVarName(repeatStatNode);
		String numRepetitionVar = "_" + loopCounter;
		code.append(common.indent(nestLevel))
			.append(BhCompiler.Keywords.JS._const)
			.append(numRepetitionVar)
			.append(" = ")
			.append(condExpCode)
			.append(";")
			.append(Util.LF);

		//for (init; cond; update)
		code.append(common.indent(nestLevel))
			.append(BhCompiler.Keywords.JS._for)
			.append("(")
			.append(BhCompiler.Keywords.JS._let)
			.append(loopCounter)
			.append(" = 0; ")
			.append(loopCounter)
			.append("<")
			.append(numRepetitionVar)
			.append("; ")
			.append("++")
			.append(loopCounter)
			.append(") {")
			.append(Util.LF);
		
		//loop part
		SyntaxSymbol loopStat = repeatStatNode.findSymbolInDescendants("*", SymbolNames.ControlStat.loopStat, "*");
		genStatement(loopStat, code, nestLevel+1, option);
		code.append(common.indent(nestLevel))
			.append("}")
			.append(Util.LF);
	}
}
