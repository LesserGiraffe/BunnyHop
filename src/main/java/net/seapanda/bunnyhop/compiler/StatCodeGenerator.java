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
import net.seapanda.bunnyhop.node.model.BhNode;
import net.seapanda.bunnyhop.node.model.syntaxsymbol.SyntaxSymbol;

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
    if (statSymbolName.equals(SymbolNames.Stat.STAT_VOID)) {
      return;
    }
    if (SymbolNames.AssignStat.LIST.contains(statSymbolName)) {
      genAssignStat(statementNode, code, nestLevel, option);
    } else if (SymbolNames.UserDefFunc.CALL_STAT_LIST.contains(statSymbolName)) {
      expCodeGen.genUserDefFuncCallExp(statementNode, code, nestLevel, option, false);
    } else if (SymbolNames.PreDefFunc.STAT_LIST.contains(statSymbolName)) {
      expCodeGen.genPreDefFuncCallExp(statementNode, code, nestLevel, option, false);
    } else if (SymbolNames.ControlStat.LIST.contains(statSymbolName)) {
      genControlStat(statementNode, code, nestLevel, option);
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
   * @param assignStatNode 代入文のノード
   * @param code 生成したコードの格納先
   * @param nestLevel ソースコードのネストレベル
   * @param option コンパイルオプション
   */
  private void genAssignStat(
      SyntaxSymbol assignStatNode,
      StringBuilder code,
      int nestLevel,
      CompileOption option) {
    // 右辺の値が無い代入文でブレークが指定されたときのために, ここでインスタンス ID を保存する.
    genDebugCode(assignStatNode, code, nestLevel, option);
    SyntaxSymbol rightExp = 
        assignStatNode.findDescendantOf("*", SymbolNames.BinaryExp.RIGHT_EXP, "*");
    String rightExpCode = expCodeGen.genExpression(rightExp, code, nestLevel, option);
    if (rightExpCode == null) {
      return;
    }
    SyntaxSymbol varSymbol = 
        assignStatNode.findDescendantOf("*", SymbolNames.AssignStat.LEFT_VAR, "*");
    String addAssignStatName = assignStatNode.getSymbolName();
    boolean isAddAssign =
        addAssignStatName.equals(SymbolNames.AssignStat.NUM_ADD_ASSIGN_STAT)
        || addAssignStatName.equals(SymbolNames.AssignStat.STR_ADD_ASSIGN_STAT);
    genAssignCode(varSymbol, rightExpCode, isAddAssign, code, nestLevel);
  }

  /**
   * 代入文のコードを生成する.
   *
   * @param varSymbol 代入先の変数ノード
   * @param rightExp 右辺のコード
   * @param isAddAssign += 代入文を生成する場合 true
   * @param code 生成したコードの格納先
   * @param nestLevel ソースコードのネストレベル
   */
  private void genAssignCode(
      SyntaxSymbol varSymbol,
      String rightExp,
      boolean isAddAssign,
      StringBuilder code,
      int nestLevel) {
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
      SyntaxSymbol controlStatNode,
      StringBuilder code,
      int nestLevel,
      CompileOption option) {
    String symbolName = controlStatNode.getSymbolName();
    genDebugCode(controlStatNode, code, nestLevel, option);
    switch (symbolName) {
      case SymbolNames.ControlStat.BREAK_STAT:
        genBreakStat(code, nestLevel, option);
        break;

      case SymbolNames.ControlStat.CONTINUE_STAT:
        genContinueStat(code, nestLevel, option);
        break;

      case SymbolNames.ControlStat.IF_ELSE_STAT:
      case SymbolNames.ControlStat.IF_STAT:
        genIfElseStat(controlStatNode, code, nestLevel, option);
        break;

      case SymbolNames.ControlStat.WHILE_STAT:
        genWhileStat(controlStatNode, code, nestLevel, option);
        break;

      case SymbolNames.ControlStat.COMPOUND_STAT:
        genCompoundStat(controlStatNode, code, nestLevel, option);
        break;

      case SymbolNames.ControlStat.REPEAT_STAT:
        genRepeatStat(controlStatNode, code, nestLevel, option);
        break;

      case SymbolNames.ControlStat.RETURN_STAT:
        genReturnStat(code, nestLevel, option);
        break;

      case SymbolNames.ControlStat.MUTEX_BLOCK_STAT:
        genMutexBlockStat(controlStatNode, code, nestLevel, option);
        break;

      case SymbolNames.ControlStat.EXP_ADAPTER_STAT:
      case SymbolNames.ControlStat.LIST_ADAPTER_STAT:
        genAdapterStat(controlStatNode, code, nestLevel, option);
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
  private void genIfElseStat(
      SyntaxSymbol ifElseStatNode,
      StringBuilder code,
      int nestLevel,
      CompileOption option) {
    //conditional part
    SyntaxSymbol condExp =
        ifElseStatNode.findDescendantOf("*", SymbolNames.ControlStat.COND_EXP, "*");
    String condExpCode = expCodeGen.genExpression(condExp, code, nestLevel, option);
    condExpCode = condExpCode + " === " + Keywords.Js._true;
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
   * @param whileStatNode while文のノード
   * @param code 生成したコードの格納先
   * @param nestLevel ソースコードのネストレベル
   * @param option コンパイルオプション
   */
  private void genWhileStat(
      SyntaxSymbol whileStatNode,
      StringBuilder code,
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
    String condExpCode = expCodeGen.genExpression(condExp, code, nestLevel + 1, option);
    condExpCode = condExpCode + " !== " + Keywords.Js._true;
    code.append(common.indent(nestLevel + 1))
        .append(Keywords.Js._if_)
        .append("(")
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
   * @param compoundStatNode block文のノード
   * @param code 生成したコードの格納先
   * @param nestLevel ソースコードのネストレベル
   * @param option コンパイルオプション
   */
  private void genCompoundStat(
      SyntaxSymbol compoundStatNode,
      StringBuilder code,
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
   * @param repeatStatNode Repeat文のノード
   * @param code 生成したコードの格納先
   * @param nestLevel ソースコードのネストレベル
   * @param option コンパイルオプション
   */
  private void genRepeatStat(
      SyntaxSymbol repeatStatNode,
      StringBuilder code,
      int nestLevel,
      CompileOption option) {
    SyntaxSymbol condExp =
        repeatStatNode.findDescendantOf("*", SymbolNames.ControlStat.COND_EXP, "*");
    String condExpCode = expCodeGen.genExpression(condExp, code, nestLevel, option);
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
   * @param mutexBlockNode 排他区間のノード
   * @param code 生成したコードの格納先
   * @param nestLevel ソースコードのネストレベル
   * @param option コンパイルオプション
   */
  private void genMutexBlockStat(
      SyntaxSymbol mutexBlockNode,
      StringBuilder code,
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

  /**
   * 式を文に変換するコードを生成する.
   *
   * @param adapterStatNode 式を文に変換するノード
   * @param code 生成したコードの格納先
   * @param nestLevel ソースコードのネストレベル
   * @param option コンパイルオプション
   */
  private void genAdapterStat(
      SyntaxSymbol adapterStatNode,
      StringBuilder code,
      int nestLevel,
      CompileOption option) {
    SyntaxSymbol exp =
        adapterStatNode.findDescendantOf("*", SymbolNames.ControlStat.TARGET, "*");
    expCodeGen.genExpression(exp, code, nestLevel, option);
  }

  /**
   * ノードを処理する前のデバッグ用コードを生成する.
   *
   * @param code 生成したコードの格納先
   * @param nestLevel ソースコードのネストレベル
   * @param option コンパイルオプション
   */
  public void genDebugCode(
      SyntaxSymbol symbol,
      StringBuilder code,
      int nestLevel,
      CompileOption option) {
    String instVar = common.genAssignNodeInstId(symbol, code, nestLevel, option);
    common.genSetInstIdToThreadContext(instVar, code, nestLevel, option);
    if (!(symbol instanceof BhNode node)) {
      return;
    }
    if (node.isBreakpointGroupLeader()) {
      common.genConditionalWait(instVar, code, nestLevel, option);
    }
  }
}
