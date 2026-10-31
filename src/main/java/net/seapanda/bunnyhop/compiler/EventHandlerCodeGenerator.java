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
import static net.seapanda.bunnyhop.compiler.Keywords.Prefix;
import static net.seapanda.bunnyhop.compiler.Keywords.newLine;

import java.util.Collection;
import net.seapanda.bunnyhop.compiler.api.CompilableSyntaxTreeNode;
import net.seapanda.bunnyhop.compiler.api.CompileOption;

/**
 * イベントハンドラのコード生成を行うクラス.
 *
 * @author K.Koike
 */
class EventHandlerCodeGenerator {

  private final CommonCodeGenerator common;
  private final StatCodeGenerator statCodeGen;
  private final VarDeclCodeGenerator varDeclCodeGen;

  /** コンストラクタ. */
  EventHandlerCodeGenerator(
      CommonCodeGenerator common,
      StatCodeGenerator statCodeGen,
      VarDeclCodeGenerator varDeclCodeGen) {
    this.common = common;
    this.statCodeGen = statCodeGen;
    this.varDeclCodeGen = varDeclCodeGen;
  }

  /**
   * イベントハンドラのコードを作成する.
   *
   * @param nodes コンパイル対象のノードリスト
   * @param code 生成したコードの格納先
   * @param nestLevel ソースコードのネストレベル
   * @param option コンパイルオプション
   */
  void generateEventHandlers(
      Collection<CompilableSyntaxTreeNode> nodes,
      StringBuilder code,
      int nestLevel,
      CompileOption option) {
    for (CompilableSyntaxTreeNode symbol : nodes) {
      if (SymbolNames.Event.LIST.contains(symbol.getSymbolName())) {
        generateEventHandler(symbol, code, nestLevel, option);
      }
    }
  }

  /**
   * イベントハンドラのコードを作成する.
   *
   * @param eventNode イベントノード
   * @param code 生成したコードの格納先
   * @param nestLevel ソースコードのネストレベル
   * @param option コンパイルオプション
   */
  private void generateEventHandler(
      CompilableSyntaxTreeNode eventNode,
      StringBuilder code,
      int nestLevel,
      CompileOption option) {
    String lockVar = Prefix.lockVar + eventNode.getTag();
    String funcName = common.generateFuncName(eventNode);
    generateHeaderSnippetOfEventCall(code, eventNode, funcName, lockVar, nestLevel, option);
    // _sleep(...)
    if (eventNode.getSymbolName().equals(SymbolNames.Event.DELAYED_START_EVENT)) {
      CompilableSyntaxTreeNode delayTimeNode =
          eventNode.findDescendantOf("*", "*", SymbolNames.Event.DELAY_TIME);
      code.append(common.indent(nestLevel + 4))
          .append(common.generateFuncCall(ScriptIdentifiers.Funcs.SLEEP, delayTimeNode.getLexeme()))
          .append(";")
          .append(newLine);
    }
    CompilableSyntaxTreeNode stat =
        eventNode.findDescendantOf("*", SymbolNames.Stat.STAT_LIST, "*");
    statCodeGen.generateStatement(stat, code, nestLevel + 4, option);
    generateFooterSnippetOfEventCall(code, lockVar, nestLevel, option);
    // _addEvent(...);
    String eventName = getEventType(eventNode, option);
    if (!eventName.isEmpty()) {
      generateAddEventFuncCall(eventName, funcName, code, nestLevel);
    }
  }

  /** イベントとそれに対応するメソッドを追加する関数を呼ぶコードを生成する. */
  private void generateAddEventFuncCall(
      String eventName,
      String funcName,
      StringBuilder code,
      int nestLevel) {
    String addEventCallStat = common.generateFuncCall(
        ScriptIdentifiers.Funcs.ADD_EVENT, funcName, "'%s'".formatted(eventName));
    code.append(common.indent(nestLevel))
        .append(addEventCallStat)
        .append(";")
        .repeat(newLine, 2);
  }

  /**
   * イベントノードからイベントの種類を取得する.
   *
   * @param eventNode イベントの種類を取得したいイベントノード
   * @param option コンパイルオプション
   * @return イベントの種類
   * */
  private String getEventType(CompilableSyntaxTreeNode eventNode, CompileOption option) {
    return switch (eventNode.getSymbolName()) {
      case SymbolNames.Event.KEY_PRESS_EVENT -> {
        CompilableSyntaxTreeNode keyCodeNode =
            eventNode.findDescendantOf("*", "*", SymbolNames.Event.KEY_CODE);
        yield keyCodeNode.getLexeme();
      }
      case SymbolNames.Event.DELAYED_START_EVENT -> option.mainRoutineLaunchEvent;
      default -> "";
    };
  }

  /**
   * イベントハンドラ呼び出しの前の定型文を生成する.
   *
   * @param code 生成したコードの格納先
   * @param eventNode イベントノード
   * @param lockVar ロックオブジェクトの変数
   * @param funcName イベントハンドラの名前
   * @param nestLevel ソースコードのネストレベル
   * @param option コンパイルオプション
   */
  void generateHeaderSnippetOfEventCall(
      StringBuilder code,
      CompilableSyntaxTreeNode eventNode,
      String funcName,
      String lockVar,
      int nestLevel,
      CompileOption option) {
    // let lockVar = new _genLockObj();
    varDeclCodeGen.generateVarDeclStat(
        code, lockVar, ScriptIdentifiers.Funcs.NEW_LOCK_OBJ + "(false)", nestLevel);

    // function funcName() {...
    code.append(common.indent(nestLevel))
        .append(Js._function_)
        .append(funcName)
        .append("(){")
        .append(newLine);

    // if (_tryLock(lockObj)) {
    code.append(common.indent(nestLevel + 1))
        .append(Js._if_)
        .append("(")
        .append(common.generateFuncCall(ScriptIdentifiers.Funcs.TRY_LOCK, lockVar))
        .append(") {")
        .append(newLine);

    //try {
    code.append(common.indent(nestLevel + 2))
        .append(Js._try_)
        .append("{")
        .append(newLine);

    // let _threadContext = _createThreadContext();
    code.append(common.indent(nestLevel + 3))
        .append(Js._const_)
        .append(ScriptIdentifiers.Vars.THREAD_CONTEXT)
        .append(" = ")
        .append(common.generateFuncCall(ScriptIdentifiers.Funcs.CREATE_THREAD_CONTEXT))
        .append(";")
        .append(newLine);
    
    // _notifyThreadStart(_threadContext);
    code.append(common.indent(nestLevel + 3))
        .append(common.generateFuncCall(
            ScriptIdentifiers.Funcs.NOTIFY_THREAD_START,
            ScriptIdentifiers.Vars.THREAD_CONTEXT))
        .append(";")
        .append(newLine);

    // _threadContext[_idxNextNodeInstId] = 'event-handler-instance-id';
    String instIdVar = common.generateAssignNodeInstId(eventNode, code, nestLevel + 3, option);
    common.generateSetInstIdToThreadContext(instIdVar, code, nestLevel + 3, option);
    
    // _condWait('step-id');
    if (eventNode.canSetBreakpoint()) {
      common.generateConditionalWait(instIdVar, code, nestLevel + 3, option);
    }

    // let _callStack = _threadContext[_idxCallStack];
    // _callStack[_callStack.length] = _threadContext[_idxNextNodeInstId];
    common.generatePushToCallStack(code, nestLevel + 3, option);

    // let _varStack = _threadContext[_idxVarStack];
    // let _varFrame = [];
    // _varStack[_varStack.length] = _varFrame;
    common.generatePushToVarStack(code, nestLevel + 3, option);

    // _end : {...
    code.append(common.indent(nestLevel + 3))
        .append(ScriptIdentifiers.Label.end)
        .append(" : {")
        .append(newLine);
  }

  /**
   * イベントハンドラ呼び出しの後ろの定型文を生成する.
   *
   * @param code 生成したコードの格納先
   * @param lockVar ロックオブジェクトの変数
   * @param nestLevel ソースコードのネストレベル
   * @param option コンパイルオプション
   */
  void generateFooterSnippetOfEventCall(
      StringBuilder code,
      String lockVar,
      int nestLevel,
      CompileOption option) {
    // end of "_end : {..."
    code.append(common.indent(nestLevel + 3))
        .append("}")
        .append(newLine);

    // _varStack.pop();
    common.generatePopFromVarStack(code, nestLevel + 3, option);

    // _callStack.pop();
    common.generatePopFromCallStack(code, nestLevel + 3, option);

    // _notifyThreadEnd();
    code.append(common.indent(nestLevel + 3))
        .append(common.generateFuncCall(ScriptIdentifiers.Funcs.NOTIFY_THREAD_END))
        .append(";")
        .append(newLine);

    // end of "try {"
    code.append(common.indent(nestLevel + 2))
        .append("}")
        .append(newLine);

    // finally {
    //     _unlock(...);
    // }
    code.append(common.indent(nestLevel + 2))
        .append(Js._finally_)
        .append("{")
        .append(newLine)
        .append(common.indent(nestLevel + 3))
        .append(common.generateFuncCall(ScriptIdentifiers.Funcs.UNLOCK, lockVar))
        .append(";")
        .append(newLine)
        .append(common.indent(nestLevel + 2))
        .append("}")
        .append(newLine);

    // end of "if(_tryLock())"
    code.append(common.indent(nestLevel + 1))
        .append("}")
        .append(newLine);

    // end of "function funcName() {..."
    code.append(common.indent(nestLevel))
        .append("}")
        .append(newLine);
  }
}
