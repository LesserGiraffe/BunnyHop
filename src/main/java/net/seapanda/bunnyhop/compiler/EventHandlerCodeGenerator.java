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

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import net.seapanda.bunnyhop.bhprogram.common.message.BhProgramEvent;
import net.seapanda.bunnyhop.model.node.TextNode;
import net.seapanda.bunnyhop.model.node.syntaxsymbol.InstanceId;
import net.seapanda.bunnyhop.model.node.syntaxsymbol.SyntaxSymbol;

/**
 * イベントハンドラのコード生成を行うクラス.
 *
 * @author K.Koike
 */
class EventHandlerCodeGenerator {

  private final CommonCodeGenerator common;
  private final StatCodeGenerator statCodeGen;
  private final VarDeclCodeGenerator varDeclCodeGen;

  private static final Map<String, BhProgramEvent.Name> KEY_TO_KEYPRESSED_EVENT =
      new HashMap<>() {{
          put("LEFT",   BhProgramEvent.Name.KEY_LEFT_PRESSED);
          put("RIGHT",  BhProgramEvent.Name.KEY_RIGHT_PRESSED);
          put("UP",     BhProgramEvent.Name.KEY_UP_PRESSED);
          put("DOWN",   BhProgramEvent.Name.KEY_DOWN_PRESSED);
          put("SPACE",  BhProgramEvent.Name.KEY_SPACE_PRESSED);
          put("ENTER",  BhProgramEvent.Name.KEY_ENTER_PRESSED);
          put("SHIFT",  BhProgramEvent.Name.KEY_SHIFT_PRESSED);
          put("CTRL",   BhProgramEvent.Name.KEY_CTRL_PRESSED);
          put("DIGIT0", BhProgramEvent.Name.KEY_DIGIT0_PRESSED);
          put("DIGIT1", BhProgramEvent.Name.KEY_DIGIT1_PRESSED);
          put("DIGIT2", BhProgramEvent.Name.KEY_DIGIT2_PRESSED);
          put("DIGIT3", BhProgramEvent.Name.KEY_DIGIT3_PRESSED);
          put("DIGIT4", BhProgramEvent.Name.KEY_DIGIT4_PRESSED);
          put("DIGIT5", BhProgramEvent.Name.KEY_DIGIT5_PRESSED);
          put("DIGIT6", BhProgramEvent.Name.KEY_DIGIT6_PRESSED);
          put("DIGIT7", BhProgramEvent.Name.KEY_DIGIT7_PRESSED);
          put("DIGIT8", BhProgramEvent.Name.KEY_DIGIT8_PRESSED);
          put("DIGIT9", BhProgramEvent.Name.KEY_DIGIT9_PRESSED);
          put("A", BhProgramEvent.Name.KEY_A_PRESSED);
          put("B", BhProgramEvent.Name.KEY_B_PRESSED);
          put("C", BhProgramEvent.Name.KEY_C_PRESSED);
          put("D", BhProgramEvent.Name.KEY_D_PRESSED);
          put("E", BhProgramEvent.Name.KEY_E_PRESSED);
          put("F", BhProgramEvent.Name.KEY_F_PRESSED);
          put("G", BhProgramEvent.Name.KEY_G_PRESSED);
          put("H", BhProgramEvent.Name.KEY_H_PRESSED);
          put("I", BhProgramEvent.Name.KEY_I_PRESSED);
          put("J", BhProgramEvent.Name.KEY_J_PRESSED);
          put("K", BhProgramEvent.Name.KEY_K_PRESSED);
          put("L", BhProgramEvent.Name.KEY_L_PRESSED);
          put("M", BhProgramEvent.Name.KEY_M_PRESSED);
          put("N", BhProgramEvent.Name.KEY_N_PRESSED);
          put("O", BhProgramEvent.Name.KEY_O_PRESSED);
          put("P", BhProgramEvent.Name.KEY_P_PRESSED);
          put("Q", BhProgramEvent.Name.KEY_Q_PRESSED);
          put("R", BhProgramEvent.Name.KEY_R_PRESSED);
          put("S", BhProgramEvent.Name.KEY_S_PRESSED);
          put("T", BhProgramEvent.Name.KEY_T_PRESSED);
          put("U", BhProgramEvent.Name.KEY_U_PRESSED);
          put("V", BhProgramEvent.Name.KEY_V_PRESSED);
          put("W", BhProgramEvent.Name.KEY_W_PRESSED);
          put("X", BhProgramEvent.Name.KEY_X_PRESSED);
          put("Y", BhProgramEvent.Name.KEY_Y_PRESSED);
          put("Z", BhProgramEvent.Name.KEY_Z_PRESSED);
        }
      };

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
   * @param compiledNodeList コンパイル対象のノードリスト
   * @param code 生成したコードの格納先
   * @param nestLevel ソースコードのネストレベル
   * @param option コンパイルオプション
   */
  void genEventHandlers(
      Collection<? extends SyntaxSymbol> compiledNodeList,
      StringBuilder code,
      int nestLevel,
      CompileOption option) {

    for (SyntaxSymbol symbol : compiledNodeList) {
      if (SymbolNames.Event.LIST.contains(symbol.getSymbolName())) {
        genEventHandler(symbol, code, nestLevel, option);
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
  private void genEventHandler(
      SyntaxSymbol eventNode,
      StringBuilder code,
      int nestLevel,
      CompileOption option) {

    String lockVar = Keywords.Prefix.lockVar + eventNode.getSerialNo().hexStr();
    String funcName = common.genFuncName(eventNode);
    genHeaderSnippetOfEventCall(
        code, eventNode.getInstanceId(), funcName, lockVar, nestLevel, option);
    // _sleep(...)
    if (eventNode.getSymbolName().equals(SymbolNames.Event.DELAYED_START_EVENT)) {
      TextNode delayTimeNode =
          (TextNode) eventNode.findDescendantOf("*", "*", SymbolNames.Event.DELAY_TIME);
      code.append(common.indent(nestLevel + 4))
          .append(common.genFuncCall(ScriptIdentifiers.Funcs.SLEEP, delayTimeNode.getText()))
          .append(";" + Keywords.newLine);
    }
    SyntaxSymbol stat = eventNode.findDescendantOf("*", SymbolNames.Stat.STAT_LIST, "*");
    statCodeGen.genStatement(stat, code, nestLevel + 4, option);
    genFooterSnippetOfEventCall(code, lockVar, nestLevel, option);
    // _addEvent(...);
    getEventType(eventNode).ifPresent(
        event -> genAddEventFuncCall(event, funcName, code, nestLevel));
  }

  /** イベントとそれに対応するメソッドを追加する関数を呼ぶコードを生成する. */
  private void genAddEventFuncCall(
      BhProgramEvent.Name event,
      String funcName,
      StringBuilder code,
      int nestLevel) {

    String addEventCallStat = common.genFuncCall(
        ScriptIdentifiers.Funcs.ADD_EVENT,
        funcName,
        "'%s'".formatted(event));
    code.append(common.indent(nestLevel))
        .append(addEventCallStat + ";" + Keywords.newLine.repeat(2));
  }

  /**
   * イベントノードからイベントの種類を取得する.
   *
   * @param eventNode イベントの種類を取得したいイベントノード
   * @return イベントの種類
   * */
  private Optional<BhProgramEvent.Name> getEventType(SyntaxSymbol eventNode) {
    switch (eventNode.getSymbolName()) {
      case SymbolNames.Event.KEY_PRESS_EVENT:
        TextNode eventTypeNode =
            (TextNode) eventNode.findDescendantOf("*", "*", SymbolNames.Event.KEY_CODE);
        return Optional.ofNullable(KEY_TO_KEYPRESSED_EVENT.get(eventTypeNode.getText()));

      case SymbolNames.Event.DELAYED_START_EVENT:
        return Optional.of(BhProgramEvent.Name.PROGRAM_START);

      default:
    }
    return Optional.empty();
  }

  /**
   * イベントハンドラ呼び出しの前の定型文を生成する.
   *
   * @param code 生成したコードの格納先
   * @param eventInstId イベントハンドラノードの {@link InstanceId}
   * @param lockVar ロックオブジェクトの変数
   * @param nestLevel ソースコードのネストレベル
   * @param option コンパイルオプション
   */
  void genHeaderSnippetOfEventCall(
      StringBuilder code,
      InstanceId eventInstId,
      String funcName,
      String lockVar,
      int nestLevel,
      CompileOption option) {
    // let lockVar = new _genLockObj();
    varDeclCodeGen.genVarDeclStat(
        code, lockVar, ScriptIdentifiers.Funcs.GEN_LOCK_OBJ + "(false)", nestLevel);

    // function funcName() {...
    code.append(common.indent(nestLevel))
        .append(Keywords.Js._function_)
        .append(funcName)
        .append("(){" + Keywords.newLine);

    // if (_tryLock(lockObj)) {
    code.append(common.indent(nestLevel + 1))
        .append(Keywords.Js._if_)
        .append("(")
        .append(common.genFuncCall(ScriptIdentifiers.Funcs.TRY_LOCK, lockVar))
        .append(") {" + Keywords.newLine);

    //try {
    code.append(common.indent(nestLevel + 2))
        .append(Keywords.Js._try_)
        .append("{" + Keywords.newLine);

    // let _threadContext = _createThreadContext();
    code.append(common.indent(nestLevel + 3))
        .append(Keywords.Js._const_)
        .append(ScriptIdentifiers.Vars.THREAD_CONTEXT)
        .append(" = ")
        .append(common.genFuncCall(ScriptIdentifiers.Funcs.CREATE_THREAD_CONTEXT))
        .append(";" + Keywords.newLine);
    
    // _notifyThreadStart(_threadContext);
    code.append(common.indent(nestLevel + 3))
        .append(common.genFuncCall(
            ScriptIdentifiers.Funcs.NOTIFY_THREAD_START,
            ScriptIdentifiers.Vars.THREAD_CONTEXT))
        .append(";" + Keywords.newLine);

    // _threadContext[_idxNextNodeInstId] = 'event-handler-instance-id';
    common.genSetNextNodeInstId(code, eventInstId, nestLevel + 3, option);

    // let _callStack = _threadContext[_idxCallStack];
    // _callStack[_callStack.length] = _threadContext[_idxNextNodeInstId];
    common.genPushToCallStack(code, nestLevel + 3, option);

    // let _varStack = _threadContext[_idxVarStack];
    // let _varFrame = [];
    // _varStack[_varStack.length] = _varFrame;
    common.genPushToVarStack(code, nestLevel + 3, option);

    // _end : {...
    code.append(common.indent(nestLevel + 3))
        .append(ScriptIdentifiers.Label.end)
        .append(" : {" + Keywords.newLine);
  }

  /**
   * イベントハンドラ呼び出しの後ろの定型文を生成する.
   *
   * @param code 生成したコードの格納先
   * @param lockVar ロックオブジェクトの変数
   * @param nestLevel ソースコードのネストレベル
   * @param option コンパイルオプション
   */
  void genFooterSnippetOfEventCall(
      StringBuilder code,
      String lockVar,
      int nestLevel,
      CompileOption option) {
    // end of "_end : {..."
    code.append(common.indent(nestLevel + 3))
        .append("}" + Keywords.newLine);
    
    // _varStack.pop();
    common.genPopFromVarStack(code, nestLevel + 3, option);

    // _callStack.pop();
    common.genPopFromCallStack(code, nestLevel + 3, option);

    // _notifyThreadEnd();
    code.append(common.indent(nestLevel + 3))
        .append(common.genFuncCall(ScriptIdentifiers.Funcs.NOTIFY_THREAD_END))
        .append(";" + Keywords.newLine);

    // end of "try {"
    code.append(common.indent(nestLevel + 2))
        .append("}" + Keywords.newLine);

    // finally {
    //     _unlock(...);
    // }
    code.append(common.indent(nestLevel + 2))
        .append(Keywords.Js._finally_)
        .append("{" + Keywords.newLine)
        .append(common.indent(nestLevel + 3))
        .append(common.genFuncCall(ScriptIdentifiers.Funcs.UNLOCK, lockVar))
        .append(";" + Keywords.newLine)
        .append(common.indent(nestLevel + 2))
        .append("}" + Keywords.newLine);

    // end of "if(_tryLock())"
    code.append(common.indent(nestLevel + 1))
        .append("}" + Keywords.newLine);

    // end of "function funcName() {..."
    code.append(common.indent(nestLevel))
        .append("}" + Keywords.newLine);
  }
}
