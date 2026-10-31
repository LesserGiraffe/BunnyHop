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

import static net.seapanda.bunnyhop.compiler.Keywords.Js;
import static net.seapanda.bunnyhop.compiler.Keywords.newLine;

import java.util.Deque;
import java.util.LinkedList;
import java.util.SequencedCollection;
import net.seapanda.bunnyhop.compiler.api.CompilableSyntaxTreeNode;
import net.seapanda.bunnyhop.compiler.api.CompileOption;

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
  void generateStatement(
      CompilableSyntaxTreeNode statementNode,
      StringBuilder code,
      int nestLevel,
      CompileOption option) {
    String statSymbolName = statementNode.getSymbolName();
    // void がつながっているときは, そこで文は終了する
    if (statSymbolName.equals(SymbolNames.Stat.STAT_VOID)) {
      return;
    }
    if (SymbolNames.AssignStat.LIST.contains(statSymbolName)) {
      generateAssignStat(statementNode, code, nestLevel, option);
    } else if (SymbolNames.UserDefFunc.CALL_STAT_LIST.contains(statSymbolName)) {
      expCodeGen.generateUserDefFuncCallExp(statementNode, code, nestLevel, option, false);
    } else if (SymbolNames.PreDefFunc.STAT_LIST.contains(statSymbolName)) {
      expCodeGen.generatePreDefFuncCallExp(statementNode, code, nestLevel, option, false);
    } else if (SymbolNames.ControlStat.LIST.contains(statSymbolName)) {
      generateControlStat(statementNode, code, nestLevel, option);
    } else if (SymbolNames.StatToBeIgnored.LIST.contains(statSymbolName)) {
      /* do nothing */
    } else {
      return;
    }
    CompilableSyntaxTreeNode nextStat =
        statementNode.findDescendantOf("*", SymbolNames.Stat.NEXT_STAT, "*");
    if (nextStat == null) {
      // for compoundStat
      nextStat = statementNode.findDescendantOf("*", "*", SymbolNames.Stat.NEXT_STAT, "*");
    }
    if (nextStat != null) {
      generateStatement(nextStat, code, nestLevel, option);
    }
  }

  /**
   * 代入文のコードを生成する.
   *
   * @param assignStatNode 代入文のノード
   * @param code 生成したコードの格納先
   * @param nestLevel ソースコードのネストレベル
   * @param option コンパイルオプション
   */
  private void generateAssignStat(
      CompilableSyntaxTreeNode assignStatNode,
      StringBuilder code,
      int nestLevel,
      CompileOption option) {
    // 右辺の値が無い代入文でブレークが指定されたときのために, ここでインスタンス ID を保存する.
    generateDebugCode(assignStatNode, code, nestLevel, option);
    CompilableSyntaxTreeNode rightExp =
        assignStatNode.findDescendantOf("*", SymbolNames.BinaryExp.RIGHT_EXP, "*");
    String rightExpCode = expCodeGen.generateExpression(rightExp, code, nestLevel, option);
    if (rightExpCode == null) {
      return;
    }
    CompilableSyntaxTreeNode destVarNode =
        assignStatNode.findDescendantOf("*", SymbolNames.AssignStat.LEFT_VAR, "*");
    String addAssignStatName = assignStatNode.getSymbolName();
    boolean isAddAssign =
        addAssignStatName.equals(SymbolNames.AssignStat.NUM_ADD_ASSIGN_STAT)
        || addAssignStatName.equals(SymbolNames.AssignStat.STR_ADD_ASSIGN_STAT);
    generateAssignCode(destVarNode, rightExpCode, isAddAssign, code, nestLevel);
  }

  /**
   * 代入文のコードを生成する.
   *
   * @param destVarNode 代入先の変数ノード
   * @param rightExp 右辺のコード
   * @param isAddAssign += 代入文を生成する場合 true
   * @param code 生成したコードの格納先
   * @param nestLevel ソースコードのネストレベル
   */
  private void generateAssignCode(
      CompilableSyntaxTreeNode destVarNode,
      String rightExp,
      boolean isAddAssign,
      StringBuilder code,
      int nestLevel) {
    if (!SymbolNames.VarDecl.VAR_LIST.contains(destVarNode.getSymbolName())) {
      return;
    }
    CompilableSyntaxTreeNode varDecl = destVarNode.getOriginal();
    if (common.isOutputParam(varDecl)) {
      if (isAddAssign) {
        rightExp = "(%s + %s)".formatted(common.generateGetOutputParamVal(varDecl), rightExp);
      }
      code.append(common.indent(nestLevel))
          .append(common.generateSetOutputParamVal(varDecl, rightExp))
          .append(";")
          .append(newLine);
    } else {
      String varName = common.generateVarName(varDecl);
      if (isAddAssign) {
        rightExp = "(%s + %s)".formatted(varName, rightExp);
      }
      code.append(common.indent(nestLevel))
          .append(varName)
          .append(" = ")
          .append(rightExp)
          .append(";")
          .append(newLine);
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
  private void generateControlStat(
      CompilableSyntaxTreeNode controlStatNode,
      StringBuilder code,
      int nestLevel,
      CompileOption option) {
    String symbolName = controlStatNode.getSymbolName();
    generateDebugCode(controlStatNode, code, nestLevel, option);
    switch (symbolName) {
      case SymbolNames.ControlStat.BREAK_STAT:
        generateBreakStat(code, nestLevel, option);
        break;

      case SymbolNames.ControlStat.CONTINUE_STAT:
        generateContinueStat(code, nestLevel, option);
        break;

      case SymbolNames.ControlStat.IF_ELSE_STAT:
      case SymbolNames.ControlStat.IF_STAT:
        generateIfElseStat(controlStatNode, code, nestLevel, option);
        break;

      case SymbolNames.ControlStat.WHILE_STAT:
        generateWhileStat(controlStatNode, code, nestLevel, option);
        break;

      case SymbolNames.ControlStat.COMPOUND_STAT:
        generateCompoundStat(controlStatNode, code, nestLevel, option);
        break;

      case SymbolNames.ControlStat.REPEAT_STAT:
        generateRepeatStat(controlStatNode, code, nestLevel, option);
        break;

      case SymbolNames.ControlStat.RETURN_STAT:
        generateReturnStat(code, nestLevel);
        break;

      case SymbolNames.ControlStat.MUTEX_BLOCK_STAT:
        generateMutexBlockStat(controlStatNode, code, nestLevel, option);
        break;

      case SymbolNames.ControlStat.EXP_ADAPTER_STAT:
      case SymbolNames.ControlStat.LIST_ADAPTER_STAT:
        generateAdapterStat(controlStatNode, code, nestLevel, option);
        break;

      default:
        throw new AssertionError("Invalid control stat " + symbolName);
    }
  }

  /**
   * 条件分岐文のコードを生成する.
   *
   * @param ifElseStatNode 制御文のノード
   * @param code 生成したコードの格納先
   * @param nestLevel ソースコードのネストレベル
   * @param option コンパイルオプション
   */
  private void generateIfElseStat(
      CompilableSyntaxTreeNode ifElseStatNode,
      StringBuilder code,
      int nestLevel,
      CompileOption option) {
    //conditional part
    CompilableSyntaxTreeNode condExp =
        ifElseStatNode.findDescendantOf("*", SymbolNames.ControlStat.COND_EXP, "*");
    String condExpCode = expCodeGen.generateExpression(condExp, code, nestLevel, option);
    condExpCode = condExpCode + " === " + Js._true;
    code.append(common.indent(nestLevel))
        .append(Js._if_)
        .append("(")
        .append(condExpCode)
        .append(") {")
        .append(newLine);

    //then part
    CompilableSyntaxTreeNode thenStat =
        ifElseStatNode.findDescendantOf("*", SymbolNames.ControlStat.THEN_STAT, "*");
    generateStatement(thenStat, code, nestLevel + 1, option);
    code.append(common.indent(nestLevel))
        .append("}")
        .append(newLine);

    //else part
    CompilableSyntaxTreeNode elseStat =
        ifElseStatNode.findDescendantOf("*", SymbolNames.ControlStat.ELSE_STAT, "*");
    if (elseStat != null) {
      code.append(common.indent(nestLevel))
          .append(Js._else_)
          .append("{")
          .append(newLine);
      generateStatement(elseStat, code, nestLevel + 1, option);
      code.append(common.indent(nestLevel))
          .append("}")
          .append(newLine);
    }
  }

  /**
   * while 文のコードを生成する.
   *
   * @param whileStatNode while文のノード
   * @param code 生成したコードの格納先
   * @param nestLevel ソースコードのネストレベル
   * @param option コンパイルオプション
   */
  private void generateWhileStat(
      CompilableSyntaxTreeNode whileStatNode,
      StringBuilder code,
      int nestLevel,
      CompileOption option) {
    //conditional part
    code.append(common.indent(nestLevel))
        .append(Js._while_)
        .append("(")
        .append(Js._true)
        .append(") {")
        .append(newLine);

    CompilableSyntaxTreeNode condExp =
        whileStatNode.findDescendantOf("*", SymbolNames.ControlStat.COND_EXP, "*");
    String condExpCode = expCodeGen.generateExpression(condExp, code, nestLevel + 1, option);
    condExpCode = condExpCode + " !== " + Js._true;
    code.append(common.indent(nestLevel + 1))
        .append(Js._if_)
        .append("(")
        .append(condExpCode)
        .append(") {")
        .append(newLine)
        .append(common.indent(nestLevel + 2))
        .append(Js._break)
        .append(";")
        .append(newLine)
        .append(common.indent(nestLevel + 1))
        .append("}")
        .append(newLine);

    //loop part
    CompilableSyntaxTreeNode loopStat =
        whileStatNode.findDescendantOf("*", SymbolNames.ControlStat.LOOP_STAT, "*");
    numLocalVarsStack.addLast(0);
    generateStatement(loopStat, code, nestLevel + 1, option);
    numLocalVarsStack.removeLast();
    code.append(common.indent(nestLevel))
        .append("}")
        .append(newLine);
  }

  /**
   * ブロック文のコードを生成する.
   *
   * @param compoundStatNode block文のノード
   * @param code 生成したコードの格納先
   * @param nestLevel ソースコードのネストレベル
   * @param option コンパイルオプション
   */
  private void generateCompoundStat(
      CompilableSyntaxTreeNode compoundStatNode,
      StringBuilder code,
      int nestLevel,
      CompileOption option) {
    code.append(common.indent(nestLevel))
        .append("{")
        .append(newLine);
    CompilableSyntaxTreeNode param = compoundStatNode.findDescendantOf(
        "*", "*", SymbolNames.ControlStat.LOCAL_VAR_DECL, "*");
    SequencedCollection<CompilableSyntaxTreeNode> varDecls =
        varDeclCodeGen.generateVarDecls(param, code, nestLevel + 1, option);
    common.generatePushToVarFrame(varDecls, code, nestLevel + 1, option);
    CompilableSyntaxTreeNode stat =
        compoundStatNode.findDescendantOf("*", "*", SymbolNames.Stat.STAT_LIST, "*");
    numLocalVarsStack.addLast(numLocalVarsStack.removeLast() + varDecls.size());
    generateStatement(stat, code, nestLevel + 1, option);
    numLocalVarsStack.addLast(numLocalVarsStack.removeLast() - varDecls.size());
    common.generatePopFromVarFrame(varDecls.size(), code, nestLevel + 1, option);
    code.append(common.indent(nestLevel))
        .append("}")
        .append(newLine);
  }

  /**
   * Repeat 文のコードを生成する.
   *
   * @param repeatStatNode Repeat文のノード
   * @param code 生成したコードの格納先
   * @param nestLevel ソースコードのネストレベル
   * @param option コンパイルオプション
   */
  private void generateRepeatStat(
      CompilableSyntaxTreeNode repeatStatNode,
      StringBuilder code,
      int nestLevel,
      CompileOption option) {
    CompilableSyntaxTreeNode condExp =
        repeatStatNode.findDescendantOf("*", SymbolNames.ControlStat.COND_EXP, "*");
    String condExpCode = expCodeGen.generateExpression(condExp, code, nestLevel, option);
    String loopCounter = common.generateVarName(repeatStatNode);
    String numRepetitionVar = "_" + loopCounter;
    code.append(common.indent(nestLevel))
        .append(Js._const_)
        .append(numRepetitionVar)
        .append(" = ")
        .append("Math.floor")
        .append("(")
        .append(condExpCode)
        .append(");")
        .append(newLine);

    //for (init; cond; update)
    code.append(common.indent(nestLevel))
        .append(Js._for_)
        .append("(")
        .append(Js._let_)
        .append(loopCounter)
        .append(" = 0; ")
        .append(loopCounter)
        .append("<")
        .append(numRepetitionVar)
        .append("; ")
        .append("++")
        .append(loopCounter)
        .append(") {")
        .append(newLine);

    //loop part
    CompilableSyntaxTreeNode loopStat =
        repeatStatNode.findDescendantOf("*", SymbolNames.ControlStat.LOOP_STAT, "*");
    numLocalVarsStack.addLast(0);
    generateStatement(loopStat, code, nestLevel + 1, option);
    numLocalVarsStack.removeLast();
    code.append(common.indent(nestLevel))
        .append("}")
        .append(newLine);
  }

  /**
   * 排他制御区間のコードを生成する.
   *
   * @param mutexBlockNode 排他区間のノード
   * @param code 生成したコードの格納先
   * @param nestLevel ソースコードのネストレベル
   * @param option コンパイルオプション
   */
  private void generateMutexBlockStat(
      CompilableSyntaxTreeNode mutexBlockNode,
      StringBuilder code,
      int nestLevel,
      CompileOption option) {
    CompilableSyntaxTreeNode lockVarNode = mutexBlockNode.getOriginal();
    String lockVar = common.generateVarName(lockVarNode);
    // try {
    code.append(common.indent(nestLevel))
        .append(Js._try_)
        .append("{")
        .append(newLine);

    // lock
    code.append(common.indent(nestLevel + 1))
        .append(common.generateFuncCall(ScriptIdentifiers.Funcs.LOCK, lockVar))
        .append(";")
        .append(newLine);

    CompilableSyntaxTreeNode exclusiveStat =
        mutexBlockNode.findDescendantOf("*", SymbolNames.ControlStat.EXCLUSIVE_STAT, "*");
    generateStatement(exclusiveStat, code, nestLevel + 1, option);

    // end of "try {"
    code.append(common.indent(nestLevel))
        .append("}")
        .append(newLine);

    // fincally { _unlock(...); }
    code.append(common.indent(nestLevel))
        .append(Js._finally_)
        .append("{ ")
        .append(common.generateFuncCall(ScriptIdentifiers.Funcs.UNLOCK, lockVar))
        .append("; }")
        .append(newLine);
  }

  /**
   * break 文を作成する.
   *
   * @param code 生成したコードの格納先
   * @param nestLevel ソースコードのネストレベル
   * @param option コンパイルオプション
   */  
  private void generateBreakStat(
      StringBuilder code,
      int nestLevel,
      CompileOption option) {
    common.generatePopFromVarFrame(numLocalVarsStack.peekLast(), code, nestLevel, option);
    code.append(common.indent(nestLevel))
        .append(Js._break)
        .append(";")
        .append(newLine);
  }

  /**
   * continue 文を作成する.
   *
   * @param code 生成したコードの格納先
   * @param nestLevel ソースコードのネストレベル
   * @param option コンパイルオプション
   */  
  private void generateContinueStat(
      StringBuilder code,
      int nestLevel,
      CompileOption option) {
    common.generatePopFromVarFrame(numLocalVarsStack.peekLast(), code, nestLevel, option);
    code.append(common.indent(nestLevel))
        .append(Js._continue)
        .append(";")
        .append(newLine);
  }

  /**
   * return 文を作成する.
   *
   * @param code 生成したコードの格納先
   * @param nestLevel ソースコードのネストレベル
   */  
  private void generateReturnStat(StringBuilder code, int nestLevel) {
    code.append(common.indent(nestLevel))
        .append(Js._break_)
        .append(ScriptIdentifiers.Label.end)
        .append(";")
        .append(newLine);
  }

  /**
   * 式を文に変換するコードを生成する.
   *
   * @param adapterStatNode 式を文に変換するノード
   * @param code 生成したコードの格納先
   * @param nestLevel ソースコードのネストレベル
   * @param option コンパイルオプション
   */
  private void generateAdapterStat(
      CompilableSyntaxTreeNode adapterStatNode,
      StringBuilder code,
      int nestLevel,
      CompileOption option) {
    CompilableSyntaxTreeNode exp =
        adapterStatNode.findDescendantOf("*", SymbolNames.ControlStat.TARGET, "*");
    expCodeGen.generateExpression(exp, code, nestLevel, option);
  }

  /**
   * ノードを処理する前のデバッグ用コードを生成する.
   *
   * @param code 生成したコードの格納先
   * @param nestLevel ソースコードのネストレベル
   * @param option コンパイルオプション
   */
  public void generateDebugCode(
      CompilableSyntaxTreeNode node,
      StringBuilder code,
      int nestLevel,
      CompileOption option) {
    String instVar = common.generateAssignNodeInstId(node, code, nestLevel, option);
    common.generateSetInstIdToThreadContext(instVar, code, nestLevel, option);
    if (node.canSetBreakpoint()) {
      common.generateConditionalWait(instVar, code, nestLevel, option);
    }
  }
}
