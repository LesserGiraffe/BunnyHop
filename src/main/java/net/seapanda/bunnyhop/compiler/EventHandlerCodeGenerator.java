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
import net.seapanda.bunnyhop.bhprogram.common.BhProgramData;
import net.seapanda.bunnyhop.model.node.TextNode;
import net.seapanda.bunnyhop.model.syntaxsymbol.SyntaxSymbol;

/**
 * イベントハンドラのコード生成を行うクラス.
 *
 * @author K.Koike
 */
public class EventHandlerCodeGenerator {

  private final CommonCodeGenerator common;
  private final StatCodeGenerator statCodeGen;
  private final VarDeclCodeGenerator varDeclCodeGen;

  private static final Map<String, BhProgramData.Event> KEY_TO_KEYPRESSED_EVENT =
      new HashMap<String, BhProgramData.Event>() {{
          put("LEFT",   BhProgramData.Event.KEY_LEFT_PRESSED);
          put("RIGHT",  BhProgramData.Event.KEY_RIGHT_PRESSED);
          put("UP",     BhProgramData.Event.KEY_UP_PRESSED);
          put("DOWN",   BhProgramData.Event.KEY_DOWN_PRESSED);
          put("SPACE",  BhProgramData.Event.KEY_SPACE_PRESSED);
          put("ENTER",  BhProgramData.Event.KEY_ENTER_PRESSED);
          put("SHIFT",  BhProgramData.Event.KEY_SHIFT_PRESSED);
          put("CTRL",   BhProgramData.Event.KEY_CTRL_PRESSED);
          put("DIGIT0", BhProgramData.Event.KEY_DIGIT0_PRESSED);
          put("DIGIT1", BhProgramData.Event.KEY_DIGIT1_PRESSED);
          put("DIGIT2", BhProgramData.Event.KEY_DIGIT2_PRESSED);
          put("DIGIT3", BhProgramData.Event.KEY_DIGIT3_PRESSED);
          put("DIGIT4", BhProgramData.Event.KEY_DIGIT4_PRESSED);
          put("DIGIT5", BhProgramData.Event.KEY_DIGIT5_PRESSED);
          put("DIGIT6", BhProgramData.Event.KEY_DIGIT6_PRESSED);
          put("DIGIT7", BhProgramData.Event.KEY_DIGIT7_PRESSED);
          put("DIGIT8", BhProgramData.Event.KEY_DIGIT8_PRESSED);
          put("DIGIT9", BhProgramData.Event.KEY_DIGIT9_PRESSED);
          put("A", BhProgramData.Event.KEY_A_PRESSED);
          put("B", BhProgramData.Event.KEY_B_PRESSED);
          put("C", BhProgramData.Event.KEY_C_PRESSED);
          put("D", BhProgramData.Event.KEY_D_PRESSED);
          put("E", BhProgramData.Event.KEY_E_PRESSED);
          put("F", BhProgramData.Event.KEY_F_PRESSED);
          put("G", BhProgramData.Event.KEY_G_PRESSED);
          put("H", BhProgramData.Event.KEY_H_PRESSED);
          put("I", BhProgramData.Event.KEY_I_PRESSED);
          put("J", BhProgramData.Event.KEY_J_PRESSED);
          put("K", BhProgramData.Event.KEY_K_PRESSED);
          put("L", BhProgramData.Event.KEY_L_PRESSED);
          put("M", BhProgramData.Event.KEY_M_PRESSED);
          put("N", BhProgramData.Event.KEY_N_PRESSED);
          put("O", BhProgramData.Event.KEY_O_PRESSED);
          put("P", BhProgramData.Event.KEY_P_PRESSED);
          put("Q", BhProgramData.Event.KEY_Q_PRESSED);
          put("R", BhProgramData.Event.KEY_R_PRESSED);
          put("S", BhProgramData.Event.KEY_S_PRESSED);
          put("T", BhProgramData.Event.KEY_T_PRESSED);
          put("U", BhProgramData.Event.KEY_U_PRESSED);
          put("V", BhProgramData.Event.KEY_V_PRESSED);
          put("W", BhProgramData.Event.KEY_W_PRESSED);
          put("X", BhProgramData.Event.KEY_X_PRESSED);
          put("Y", BhProgramData.Event.KEY_Y_PRESSED);
          put("Z", BhProgramData.Event.KEY_Z_PRESSED);
        }
      };

  /** コンストラクタ. */
  public EventHandlerCodeGenerator(
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
  public void genEventHandlers(
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

    String lockVar = Keywords.Prefix.lockVarPrefix + eventNode.getSymbolId();
    String funcName = common.genFuncName(eventNode);
    genHeaderSnippetOfEventCall(code, funcName, lockVar, nestLevel);
    // _sleep(...)
    if (eventNode.getSymbolName().equals(SymbolNames.Event.DELAYED_START_EVENT)) {
      TextNode delayTimeNode =
          (TextNode) eventNode.findSymbolInDescendants("*", "*", SymbolNames.Event.DELAY_TIME);
      code.append(common.indent(nestLevel + 4))
          .append(common.genFuncCallCode(ScriptIdentifiers.Funcs.SLEEP, delayTimeNode.getText()))
          .append(";" + Keywords.newLine);
    }

    SyntaxSymbol stat = eventNode.findSymbolInDescendants("*", SymbolNames.Stat.STAT_LIST, "*");
    statCodeGen.genStatement(stat, code, nestLevel + 4, option);
    genFooterSnippetOfEventCall(code, lockVar, nestLevel);
    // _addEvent(...);
    getEventType(eventNode).ifPresent(
        event -> genAddEventFuncCall(event, funcName, code, nestLevel));
  }

  private void genAddEventFuncCall(
      BhProgramData.Event event,
      String funcName,
      StringBuilder code,
      int nestLevel) {

    String addEventCallStat = common.genFuncCallCode(
        ScriptIdentifiers.Funcs.ADD_EVENT,
        funcName,
        "'" + event.toString() + "'");
    code.append(common.indent(nestLevel))
        .append(addEventCallStat + "; " + Keywords.newLine + Keywords.newLine);
  }

  /**
   * イベントノードからイベントの種類を取得する.
   *
   * @param eventNode イベントの種類を取得したいイベントノード
   * @return イベントの種類
   * */
  private Optional<BhProgramData.Event> getEventType(SyntaxSymbol eventNode) {
    switch (eventNode.getSymbolName()) {
      case SymbolNames.Event.KEY_PRESS_EVENT:
        TextNode eventTypeNode =
            (TextNode) eventNode.findSymbolInDescendants("*", "*", SymbolNames.Event.KEY_CODE);
        return Optional.ofNullable(KEY_TO_KEYPRESSED_EVENT.get(eventTypeNode.getText()));

      case SymbolNames.Event.DELAYED_START_EVENT:
        return Optional.of(BhProgramData.Event.PROGRAM_START);

      default:
    }
    return Optional.empty();
  }

  /**
   * イベントハンドラ呼び出しの前の定型文を生成する.
   *
   * @param code 生成したコードの格納先
   * @param lockVar ロックオブジェクトの変数
   * @param nestLevel ソースコードのネストレベル
   */
  public void genHeaderSnippetOfEventCall(
      StringBuilder code,
      String funcName,
      String lockVar,
      int nestLevel) {

    // let lockVar = new _genLockObj();
    varDeclCodeGen.genVarDeclStat(
        code, lockVar, ScriptIdentifiers.Funcs.GEN_LOCK_OBJ + "()", nestLevel);

    // function funcName() {...
    code.append(common.indent(nestLevel))
        .append(Keywords.Js._function_)
        .append(funcName)
        .append("(){" + Keywords.newLine);

    // if (_tryLock(lockObj)) {
    code.append(common.indent(nestLevel + 1))
        .append(Keywords.Js._if_)
        .append("(")
        .append(common.genFuncCallCode(ScriptIdentifiers.Funcs.TRY_LOCK, lockVar))
        .append(") {" + Keywords.newLine);

    //try {
    code.append(common.indent(nestLevel + 2))
        .append(Keywords.Js._try_)
        .append("{" + Keywords.newLine);

    // _end : {...
    code.append(common.indent(nestLevel + 3))
        .append(ScriptIdentifiers.Label.end)
        .append(" : {" + Keywords.newLine);

    // _initThisObj.call(this);
    code.append(common.indent(nestLevel + 4))
        .append(common.genFuncPrototypeCallCode(
            ScriptIdentifiers.Funcs.INIT_THIS_OBJ, Keywords.Js._this))
        .append(";" + Keywords.newLine);
  }

  /**
   * イベントハンドラ呼び出しの後ろの定型文を生成する.
   *
   * @param code 生成したコードの格納先
   * @param lockVar ロックオブジェクトの変数
   * @param nestLevel ソースコードのネストレベル
   */
  public void genFooterSnippetOfEventCall(
      StringBuilder code,
      String lockVar,
      int nestLevel) {

    // end of "_end : {..."
    code.append(common.indent(nestLevel + 3))
        .append("}" + Keywords.newLine);

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
        .append(common.genFuncCallCode(ScriptIdentifiers.Funcs.UNLOCK, lockVar))
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
