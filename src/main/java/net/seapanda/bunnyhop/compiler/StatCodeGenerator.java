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
package net.seapanda.bunnyhop.compiler;

import net.seapanda.bunnyhop.model.node.BhNode;
import net.seapanda.bunnyhop.model.syntaxsymbol.SyntaxSymbol;

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
    if (statSymbolName.equals(SymbolNames.Stat.VOID_STAT))  //void がつながっているときは, そこで文終了
      return;

    if (SymbolNames.AssignStat.LIST.contains(statSymbolName)) {
      genAssignStat(code, statementNode, nestLevel, option);
    }
    else if (SymbolNames.UserDefFunc.USER_DEF_FUNC_CALL_STAT_LIST.contains(statSymbolName)){
      expCodeGen.genUserDefFuncCallExp(code, statementNode, nestLevel, option, false);
    }
    else if (SymbolNames.PreDefFunc.PREDEF_FUNC_CALL_STAT_LIST.contains(statSymbolName)) {
      expCodeGen.genPreDefFuncCallExp(code, statementNode, nestLevel, option, false);
    }
    else if (SymbolNames.ControlStat.LIST.contains(statSymbolName)) {
      genControlStat(code, statementNode, nestLevel, option);
    }
    else if (SymbolNames.StatToBeIgnored.LIST.contains(statSymbolName)) {}
    else {
      return;
    }
    SyntaxSymbol nextStat = statementNode.findSymbolInDescendants("*", SymbolNames.Stat.NEXT_STAT, "*");
    if (nextStat == null)
      nextStat = statementNode.findSymbolInDescendants("*", "*", SymbolNames.Stat.NEXT_STAT, "*");  //for compoundStat

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

    SyntaxSymbol varSymbol = assignStatNode.findSymbolInDescendants("*", SymbolNames.AssignStat.LEFT_VAR, "*");
    String varName = null;
    if (SymbolNames.VarDecl.VAR_LIST.contains(varSymbol.getSymbolName())) {  //varNode である
      varName = expCodeGen.genExpression(code, varSymbol, nestLevel, option);
    }

    SyntaxSymbol rightExp = assignStatNode.findSymbolInDescendants("*", SymbolNames.BinaryExp.RIGHT_EXP, "*");
    String rightExpCode = expCodeGen.genExpression(code, rightExp, nestLevel, option);
    if (varName == null || rightExpCode == null)
      return;

    String assignOpe = " = ";
    if (assignStatNode.getSymbolName().equals(SymbolNames.AssignStat.NUM_ADD_ASSIGN_STAT))
      assignOpe = " += ";

    code.append(common.indent(nestLevel))
      .append(varName)
      .append(assignOpe)
      .append(rightExpCode)
      .append(";").append(Keywords.newLine);
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
      case SymbolNames.ControlStat.BREAK_STAT:
        code.append(common.indent(nestLevel)).append(Keywords.JS._break).append(";")
          .append(Keywords.newLine);
        break;

      case SymbolNames.ControlStat.CONTINUE_STAT:
        code.append(common.indent(nestLevel)).append(Keywords.JS._continue).append(";")
          .append(Keywords.newLine);
        break;

      case SymbolNames.ControlStat.IF_ELSE_STAT:
      case SymbolNames.ControlStat.IF_STAT:
        genIfElseStat(code, controlStatNode, nestLevel, option);
        break;

      case SymbolNames.ControlStat.WHILE_STAT:
        genWhileStat(code, controlStatNode, nestLevel, option);
        break;

      case SymbolNames.ControlStat.COMPOUND_STAT:
        genCompoundStat(code, controlStatNode, nestLevel, option);
        break;

      case SymbolNames.ControlStat.REPEAT_STAT:
        genRepeatStat(code, controlStatNode, nestLevel, option);
        break;

      case SymbolNames.ControlStat.RETURN_STAT:
        code.append(common.indent(nestLevel))
          .append(Keywords.JS._break_).append(ScriptIdentifiers.Label.end).append(";").append(Keywords.newLine);
        break;

      case SymbolNames.ControlStat.CRITICAL_SECTION_STAT:
        genCriticalSectionStat(code, controlStatNode, nestLevel, option);
        break;

      default:
        throw new AssertionError("invalid control stat " + symbolName);
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
    SyntaxSymbol condExp = ifElseStatNode.findSymbolInDescendants("*", SymbolNames.ControlStat.COND_EXP, "*");
    String condExpCode = expCodeGen.genExpression(code, condExp, nestLevel, option);
    code.append(common.indent(nestLevel))
      .append(Keywords.JS._if_)
      .append("(")
      .append(condExpCode)
      .append(") {")
      .append(Keywords.newLine);

    //then part
    SyntaxSymbol thenStat = ifElseStatNode.findSymbolInDescendants("*", SymbolNames.ControlStat.THEN_STAT, "*");
    genStatement(thenStat, code, nestLevel + 1, option);
    code.append(common.indent(nestLevel))
      .append("}")
      .append(Keywords.newLine);

    //else part
    SyntaxSymbol elseStat = ifElseStatNode.findSymbolInDescendants("*", SymbolNames.ControlStat.ELSE_STAT, "*");
    if (elseStat != null) {
      code.append(common.indent(nestLevel))
        .append(Keywords.JS._else_)
        .append("{").append(Keywords.newLine);
      genStatement(elseStat, code, nestLevel + 1, option);
      code.append(common.indent(nestLevel))
        .append("}").append(Keywords.newLine);
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
      .append(Keywords.JS._while_)
      .append("(")
      .append(Keywords.JS._true)
      .append(") {").append(Keywords.newLine);

    SyntaxSymbol condExp = whileStatNode.findSymbolInDescendants("*", SymbolNames.ControlStat.COND_EXP, "*");
    String condExpCode = expCodeGen.genExpression(code, condExp, nestLevel+1, option);
    code.append(common.indent(nestLevel + 1))
      .append(Keywords.JS._if_)
      .append("(!")
      .append(condExpCode)
      .append(") {").append(Keywords.newLine)
      .append(common.indent(nestLevel + 2))
      .append(Keywords.JS._break).append(";").append(Keywords.newLine)
      .append(common.indent(nestLevel + 1))
      .append("}").append(Keywords.newLine);

    //loop part
    SyntaxSymbol loopStat = whileStatNode.findSymbolInDescendants("*", SymbolNames.ControlStat.LOOP_STAT, "*");
    genStatement(loopStat, code, nestLevel + 1, option);
    code.append(common.indent(nestLevel))
      .append("}").append(Keywords.newLine);
  }

  /**
   * ブロック文のコードを生成する
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
      .append("{").append(Keywords.newLine);
    SyntaxSymbol param = compoundStatNode.findSymbolInDescendants("*", "*", SymbolNames.ControlStat.LOCAL_VAR_DECL, "*");
    varDeclCodeGen.genVarDecls(param, code, nestLevel + 1, option);
    SyntaxSymbol stat = compoundStatNode.findSymbolInDescendants("*", "*", SymbolNames.Stat.STAT_LIST, "*");
    genStatement(stat, code, nestLevel + 1, option);
    code.append(common.indent(nestLevel))
      .append("}").append(Keywords.newLine);
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

    SyntaxSymbol condExp = repeatStatNode.findSymbolInDescendants("*", SymbolNames.ControlStat.COND_EXP, "*");
    String condExpCode = expCodeGen.genExpression(code, condExp, nestLevel, option);
    String loopCounter = common.genVarName(repeatStatNode);
    String numRepetitionVar = "_" + loopCounter;
    code.append(common.indent(nestLevel))
      .append(Keywords.JS._const_)
      .append(numRepetitionVar)
      .append(" = ")
      .append("Math.floor")
      .append("(")
      .append(condExpCode)
      .append(");").append(Keywords.newLine);

    //for (init; cond; update)
    code.append(common.indent(nestLevel))
      .append(Keywords.JS._for_)
      .append("(")
      .append(Keywords.JS._let_)
      .append(loopCounter)
      .append(" = 0; ")
      .append(loopCounter)
      .append("<")
      .append(numRepetitionVar)
      .append("; ")
      .append("++")
      .append(loopCounter)
      .append(") {").append(Keywords.newLine);

    //loop part
    SyntaxSymbol loopStat = repeatStatNode.findSymbolInDescendants("*", SymbolNames.ControlStat.LOOP_STAT, "*");
    genStatement(loopStat, code, nestLevel+1, option);
    code.append(common.indent(nestLevel))
      .append("}").append(Keywords.newLine);
  }

  /**
   * クリティカルセクションのコードを生成する
   * @param code 生成したコードの格納先
   * @param criticalSctnNode クリティカルセクションのノード
   * @param nestLevel ソースコードのネストレベル
   * @param option コンパイルオプション
   * */
  private void genCriticalSectionStat(
    StringBuilder code,
    SyntaxSymbol criticalSctnNode,
    int nestLevel,
    CompileOption option) {
    BhNode lockVarNode = ((BhNode)criticalSctnNode).getOriginal();
    String lockVar = common.genVarName(lockVarNode);

    // try {
    code.append(common.indent(nestLevel))
      .append(Keywords.JS._try_)
      .append("{").append(Keywords.newLine);

    // lock
    code.append(common.indent(nestLevel + 1))
      .append(common.genFuncCallCode(ScriptIdentifiers.Funcs.LOCK, lockVar))
      .append(";").append(Keywords.newLine);

    SyntaxSymbol exclusiveStat =
      criticalSctnNode.findSymbolInDescendants("*", SymbolNames.ControlStat.EXCLUSIVE_STAT, "*");
    genStatement(exclusiveStat, code, nestLevel + 1, option);

    // end of "try {"
    code.append(common.indent(nestLevel))
      .append("}").append(Keywords.newLine);

    // fincally { _unlock(...); }
    code.append(common.indent(nestLevel))
    .append(Keywords.JS._finally_).append("{ ")
      .append(common.genFuncCallCode(ScriptIdentifiers.Funcs.UNLOCK, lockVar))
      .append("; }").append(Keywords.newLine);
  }
}
















