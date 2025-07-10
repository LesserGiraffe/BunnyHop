/*
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

import java.util.Deque;
import java.util.LinkedList;
import java.util.SequencedCollection;
import net.seapanda.bunnyhop.model.node.BhNode;
import net.seapanda.bunnyhop.model.node.syntaxsymbol.SyntaxSymbol;

/**
 * 文のコード生成を行うクラス.
 *
 * @author K.Koike
 */
class StatCodeGenerator {

  private final CommonCodeGenerator common;
  private final ExpCodeGenerator expCodeGen;
  private final VarDeclCodeGenerator varDeclCodeGen;
  /** for, while 文の内側で宣言された変数の個数を保持するスタック. */
  private final Deque<Integer> numLocalVarsStack = new LinkedList<>();

  /** コンストラクタ. */
  StatCodeGenerator(
      CommonCodeGenerator common,
      ExpCodeGenerator expCodeGen,
      VarDeclCodeGenerator varDeclCodeGen) {
    this.common = common;
    this.expCodeGen = expCodeGen;
    this.varDeclCodeGen = varDeclCodeGen;
    numLocalVarsStack.addLast(0);
  }

  /**
   * statement のコードを生成する.
   *
   * @param statementNode statement系のノード (代入文, if文など)
   * @param code 生成したコードの格納先
   * @param nestLevel ソースコードのネストレベル
   * @param option コンパイルオプション
   */
  void genStatement(
      SyntaxSymbol statementNode,
      StringBuilder code,
      int nestLevel,
      CompileOption option) {
    String statSymbolName = statementNode.getSymbolName();
    // void がつながっているときは, そこで文は終了する
    if (statSymbolName.equals(SymbolNames.Stat.VOID_STAT)) {
      return;
    }
    if (SymbolNames.AssignStat.LIST.contains(statSymbolName)) {
      genAssignStat(code, statementNode, nestLevel, option);
    } else if (SymbolNames.UserDefFunc.CALL_STAT_LIST.contains(statSymbolName)) {
      expCodeGen.genUserDefFuncCallExp(code, statementNode, nestLevel, option, false);
    } else if (SymbolNames.PreDefFunc.STAT_LIST.contains(statSymbolName)) {
      expCodeGen.genPreDefFuncCallExp(code, statementNode, nestLevel, option, false);
    } else if (SymbolNames.ControlStat.LIST.contains(statSymbolName)) {
      genControlStat(code, statementNode, nestLevel, option);
    } else if (SymbolNames.StatToBeIgnored.LIST.contains(statSymbolName)) {
      /* do nothing */
    } else {
      return;
    }
    SyntaxSymbol nextStat = 
        statementNode.findDescendantOf("*", SymbolNames.Stat.NEXT_STAT, "*");
    if (nextStat == null) {
      // for compoundStat
      nextStat = statementNode.findDescendantOf("*", "*", SymbolNames.Stat.NEXT_STAT, "*");
    }
    if (nextStat != null) {
      genStatement(nextStat, code, nestLevel, option);
    }
  }

  /**
   * 代入文のコードを生成する.
   *
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

    // 右辺の値が無い代入文でブレークが指定されたときのために, ここでインスタンス ID を保存する.
    common.genSetNextNodeInstId(code, assignStatNode.getInstanceId(), nestLevel, option);
    SyntaxSymbol rightExp = 
        assignStatNode.findDescendantOf("*", SymbolNames.BinaryExp.RIGHT_EXP, "*");
    String rightExpCode = expCodeGen.genExpression(code, rightExp, nestLevel, option);
    if (rightExpCode == null) {
      return;
    }
    SyntaxSymbol varSymbol = 
        assignStatNode.findDescendantOf("*", SymbolNames.AssignStat.LEFT_VAR, "*");
    boolean isAddAssign =
        assignStatNode.getSymbolName().equals(SymbolNames.AssignStat.NUM_ADD_ASSIGN_STAT);
    genAssignCode(code, assignStatNode, varSymbol, rightExpCode, isAddAssign, nestLevel, option);
  }

  /**
   * 代入文のコードを生成する.
   *
   * @param code 生成したコードの格納先
   * @param assignStatNode 代入文のノード
   * @param varSymbol 代入先の変数ノード
   * @param rightExp 右辺のコード
   * @param isAddAssign += 代入文を生成する場合 true
   * @param nestLevel ソースコードのネストレベル
   * @param option コンパイルオプション
   */
  private void genAssignCode(
      StringBuilder code,
      SyntaxSymbol assignStatNode,
      SyntaxSymbol varSymbol,
      String rightExp,
      boolean isAddAssign,
      int nestLevel,
      CompileOption option) {
    if (!SymbolNames.VarDecl.VAR_LIST.contains(varSymbol.getSymbolName())) {
      return;
    }
    BhNode varDecl = ((BhNode) varSymbol).getOriginal();
    if (common.isOutputParam(varDecl)) {
      if (isAddAssign) {
        rightExp = "(%s + %s)".formatted(common.genGetOutputParamVal(varDecl), rightExp);
      }
      code.append(common.indent(nestLevel))
          .append(common.genSetOutputParamVal(varDecl, rightExp))
          .append(";" + Keywords.newLine);
    } else {
      String varName = common.genVarName(varDecl);
      if (isAddAssign) {
        rightExp = "(%s + %s)".formatted(varName, rightExp);
      }
      code.append(common.indent(nestLevel))
          .append(varName)
          .append(" = ")
          .append(rightExp)
          .append(";" + Keywords.newLine);
    }
  }

  /**
   * 制御文のコードを生成する.
   *
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
    common.genSetNextNodeInstId(code, controlStatNode.getInstanceId(), nestLevel, option);
    switch (symbolName) {
      case SymbolNames.ControlStat.BREAK_STAT:
        genBreakStat(code, nestLevel, option);
        break;

      case SymbolNames.ControlStat.CONTINUE_STAT:
        genContinueStat(code, nestLevel, option);
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
        genReturnStat(code, nestLevel, option);
        break;

      case SymbolNames.ControlStat.MUTEX_BLOCK_STAT:
        genMutexBlockStat(code, controlStatNode, nestLevel, option);
        break;

      default:
        throw new AssertionError("Invalid control stat " + symbolName);
    }
  }

  /**
   * 条件分岐文のコードを生成する.
   *
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
    SyntaxSymbol condExp =
        ifElseStatNode.findDescendantOf("*", SymbolNames.ControlStat.COND_EXP, "*");
    String condExpCode = expCodeGen.genExpression(code, condExp, nestLevel, option);
    code.append(common.indent(nestLevel))
        .append(Keywords.Js._if_)
        .append("(")
        .append(condExpCode)
        .append(") {" + Keywords.newLine);

    //then part
    SyntaxSymbol thenStat =
        ifElseStatNode.findDescendantOf("*", SymbolNames.ControlStat.THEN_STAT, "*");
    genStatement(thenStat, code, nestLevel + 1, option);
    code.append(common.indent(nestLevel))
        .append("}" + Keywords.newLine);

    //else part
    SyntaxSymbol elseStat =
        ifElseStatNode.findDescendantOf("*", SymbolNames.ControlStat.ELSE_STAT, "*");
    if (elseStat != null) {
      code.append(common.indent(nestLevel))
          .append(Keywords.Js._else_)
          .append("{" + Keywords.newLine);
      genStatement(elseStat, code, nestLevel + 1, option);
      code.append(common.indent(nestLevel))
          .append("}" + Keywords.newLine);
    }
  }

  /**
   * while 文のコードを生成する.
   *
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
        .append(Keywords.Js._while_)
        .append("(")
        .append(Keywords.Js._true)
        .append(") {" + Keywords.newLine);

    SyntaxSymbol condExp =
        whileStatNode.findDescendantOf("*", SymbolNames.ControlStat.COND_EXP, "*");
    String condExpCode = expCodeGen.genExpression(code, condExp, nestLevel + 1, option);
    code.append(common.indent(nestLevel + 1))
        .append(Keywords.Js._if_)
        .append("(!")
        .append(condExpCode)
        .append(") {" + Keywords.newLine)
        .append(common.indent(nestLevel + 2))
        .append(Keywords.Js._break)
        .append(";" + Keywords.newLine)
        .append(common.indent(nestLevel + 1))
        .append("}" + Keywords.newLine);

    //loop part
    SyntaxSymbol loopStat =
        whileStatNode.findDescendantOf("*", SymbolNames.ControlStat.LOOP_STAT, "*");
    numLocalVarsStack.addLast(0);
    genStatement(loopStat, code, nestLevel + 1, option);
    numLocalVarsStack.removeLast();
    code.append(common.indent(nestLevel))
        .append("}" + Keywords.newLine);
  }

  /**
   * ブロック文のコードを生成する.
   *
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
        .append("{" + Keywords.newLine);
    SyntaxSymbol param = compoundStatNode.findDescendantOf(
        "*", "*", SymbolNames.ControlStat.LOCAL_VAR_DECL, "*");
    SequencedCollection<SyntaxSymbol> varDecls =
        varDeclCodeGen.genVarDecls(param, code, nestLevel + 1, option);
    common.genPushToVarFrame(varDecls, code, nestLevel + 1, option);
    SyntaxSymbol stat =
        compoundStatNode.findDescendantOf("*", "*", SymbolNames.Stat.STAT_LIST, "*");
    numLocalVarsStack.addLast(numLocalVarsStack.removeLast() + varDecls.size());
    genStatement(stat, code, nestLevel + 1, option);
    numLocalVarsStack.addLast(numLocalVarsStack.removeLast() - varDecls.size());
    common.genPopFromVarFrame(varDecls.size(), code, nestLevel + 1, option);
    code.append(common.indent(nestLevel))
        .append("}" + Keywords.newLine);
  }

  /**
   * Repeat 文のコードを生成する.
   *
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

    SyntaxSymbol condExp =
        repeatStatNode.findDescendantOf("*", SymbolNames.ControlStat.COND_EXP, "*");
    String condExpCode = expCodeGen.genExpression(code, condExp, nestLevel, option);
    String loopCounter = common.genVarName(repeatStatNode);
    String numRepetitionVar = "_" + loopCounter;
    code.append(common.indent(nestLevel))
        .append(Keywords.Js._const_)
        .append(numRepetitionVar)
        .append(" = ")
        .append("Math.floor")
        .append("(")
        .append(condExpCode)
        .append(");" + Keywords.newLine);

    //for (init; cond; update)
    code.append(common.indent(nestLevel))
        .append(Keywords.Js._for_)
        .append("(")
        .append(Keywords.Js._let_)
        .append(loopCounter)
        .append(" = 0; ")
        .append(loopCounter)
        .append("<")
        .append(numRepetitionVar)
        .append("; ")
        .append("++")
        .append(loopCounter)
        .append(") {" + Keywords.newLine);

    //loop part
    SyntaxSymbol loopStat =
        repeatStatNode.findDescendantOf("*", SymbolNames.ControlStat.LOOP_STAT, "*");
    numLocalVarsStack.addLast(0);
    genStatement(loopStat, code, nestLevel + 1, option);
    numLocalVarsStack.removeLast();
    code.append(common.indent(nestLevel))
        .append("}" + Keywords.newLine);
  }

  /**
   * 排他制御区間のコードを生成する.
   *
   * @param code 生成したコードの格納先
   * @param mutexBlockNode クリティカルセクションのノード
   * @param nestLevel ソースコードのネストレベル
   * @param option コンパイルオプション
   */
  private void genMutexBlockStat(
      StringBuilder code,
      SyntaxSymbol mutexBlockNode,
      int nestLevel,
      CompileOption option) {

    BhNode lockVarNode = ((BhNode) mutexBlockNode).getOriginal();
    String lockVar = common.genVarName(lockVarNode);
    // try {
    code.append(common.indent(nestLevel))
        .append(Keywords.Js._try_)
        .append("{" + Keywords.newLine);

    // lock
    code.append(common.indent(nestLevel + 1))
        .append(common.genFuncCall(ScriptIdentifiers.Funcs.LOCK, lockVar))
        .append(";" + Keywords.newLine);

    SyntaxSymbol exclusiveStat =
        mutexBlockNode.findDescendantOf("*", SymbolNames.ControlStat.EXCLUSIVE_STAT, "*");
    genStatement(exclusiveStat, code, nestLevel + 1, option);

    // end of "try {"
    code.append(common.indent(nestLevel))
        .append("}" + Keywords.newLine);

    // fincally { _unlock(...); }
    code.append(common.indent(nestLevel))
        .append(Keywords.Js._finally_)
        .append("{ ")
        .append(common.genFuncCall(ScriptIdentifiers.Funcs.UNLOCK, lockVar))
        .append("; }" + Keywords.newLine);
  }

  /**
   * break 文を作成する.
   *
   * @param code 生成したコードの格納先
   * @param nestLevel ソースコードのネストレベル
   * @param option コンパイルオプション
   */  
  private void genBreakStat(
      StringBuilder code,
      int nestLevel,
      CompileOption option) {
    common.genPopFromVarFrame(numLocalVarsStack.peekLast(), code, nestLevel, option);
    code.append(common.indent(nestLevel))
        .append(Keywords.Js._break)
        .append(";" + Keywords.newLine);
  }

  /**
   * continue 文を作成する.
   *
   * @param code 生成したコードの格納先
   * @param nestLevel ソースコードのネストレベル
   * @param option コンパイルオプション
   */  
  private void genContinueStat(
      StringBuilder code,
      int nestLevel,
      CompileOption option) {
    common.genPopFromVarFrame(numLocalVarsStack.peekLast(), code, nestLevel, option);
    code.append(common.indent(nestLevel))
        .append(Keywords.Js._continue)
        .append(";" + Keywords.newLine);
  }

  /**
   * return 文を作成する.
   *
   * @param code 生成したコードの格納先
   * @param nestLevel ソースコードのネストレベル
   * @param option コンパイルオプション
   */  
  private void genReturnStat(
      StringBuilder code,
      int nestLevel,
      CompileOption option) {
    code.append(common.indent(nestLevel))
        .append(Keywords.Js._break_)
        .append(ScriptIdentifiers.Label.end)
        .append(";" + Keywords.newLine);
  }    
}
