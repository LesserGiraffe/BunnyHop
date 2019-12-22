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

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import net.seapanda.bunnyhop.bhprogram.common.BhProgramData;
import net.seapanda.bunnyhop.model.node.SyntaxSymbol;
import net.seapanda.bunnyhop.model.node.TextNode;

/**
 * イベントハンドラのコード生成を行うクラス
 * @author K.Koike
 */
public class EventHandlerCodeGenerator {

	private final CommonCodeGenerator common;
	private final StatCodeGenerator statCodeGen;
	private final VarDeclCodeGenerator varDeclCodeGen;

	private static final Map<String, BhProgramData.EVENT> KEY_TO_KEYPRESSED_EVENT =

		new HashMap<String, BhProgramData.EVENT>() {{
			put("LEFT",   BhProgramData.EVENT.KEY_LEFT_PRESSED);
			put("RIGHT",  BhProgramData.EVENT.KEY_RIGHT_PRESSED);
			put("UP",     BhProgramData.EVENT.KEY_UP_PRESSED);
			put("DOWN",   BhProgramData.EVENT.KEY_DOWN_PRESSED);
			put("SPACE",  BhProgramData.EVENT.KEY_SPACE_PRESSED);
			put("ENTER",  BhProgramData.EVENT.KEY_ENTER_PRESSED);
			put("SHIFT",  BhProgramData.EVENT.KEY_SHIFT_PRESSED);
			put("CTRL",   BhProgramData.EVENT.KEY_CTRL_PRESSED);
			put("DIGIT0", BhProgramData.EVENT.KEY_DIGIT0_PRESSED);
			put("DIGIT1", BhProgramData.EVENT.KEY_DIGIT1_PRESSED);
			put("DIGIT2", BhProgramData.EVENT.KEY_DIGIT2_PRESSED);
			put("DIGIT3", BhProgramData.EVENT.KEY_DIGIT3_PRESSED);
			put("DIGIT4", BhProgramData.EVENT.KEY_DIGIT4_PRESSED);
			put("DIGIT5", BhProgramData.EVENT.KEY_DIGIT5_PRESSED);
			put("DIGIT6", BhProgramData.EVENT.KEY_DIGIT6_PRESSED);
			put("DIGIT7", BhProgramData.EVENT.KEY_DIGIT7_PRESSED);
			put("DIGIT8", BhProgramData.EVENT.KEY_DIGIT8_PRESSED);
			put("DIGIT9", BhProgramData.EVENT.KEY_DIGIT9_PRESSED);
			put("A", BhProgramData.EVENT.KEY_A_PRESSED);
			put("B", BhProgramData.EVENT.KEY_B_PRESSED);
			put("C", BhProgramData.EVENT.KEY_C_PRESSED);
			put("D", BhProgramData.EVENT.KEY_D_PRESSED);
			put("E", BhProgramData.EVENT.KEY_E_PRESSED);
			put("F", BhProgramData.EVENT.KEY_F_PRESSED);
			put("G", BhProgramData.EVENT.KEY_G_PRESSED);
			put("H", BhProgramData.EVENT.KEY_H_PRESSED);
			put("I", BhProgramData.EVENT.KEY_I_PRESSED);
			put("J", BhProgramData.EVENT.KEY_J_PRESSED);
			put("K", BhProgramData.EVENT.KEY_K_PRESSED);
			put("L", BhProgramData.EVENT.KEY_L_PRESSED);
			put("M", BhProgramData.EVENT.KEY_M_PRESSED);
			put("N", BhProgramData.EVENT.KEY_N_PRESSED);
			put("O", BhProgramData.EVENT.KEY_O_PRESSED);
			put("P", BhProgramData.EVENT.KEY_P_PRESSED);
			put("Q", BhProgramData.EVENT.KEY_Q_PRESSED);
			put("R", BhProgramData.EVENT.KEY_R_PRESSED);
			put("S", BhProgramData.EVENT.KEY_S_PRESSED);
			put("T", BhProgramData.EVENT.KEY_T_PRESSED);
			put("U", BhProgramData.EVENT.KEY_U_PRESSED);
			put("V", BhProgramData.EVENT.KEY_V_PRESSED);
			put("W", BhProgramData.EVENT.KEY_W_PRESSED);
			put("X", BhProgramData.EVENT.KEY_X_PRESSED);
			put("Y", BhProgramData.EVENT.KEY_Y_PRESSED);
			put("Z", BhProgramData.EVENT.KEY_Z_PRESSED);
		}};

	public EventHandlerCodeGenerator(
		CommonCodeGenerator common,
		StatCodeGenerator statCodeGen,
		VarDeclCodeGenerator varDeclCodeGen) {
		this.common = common;
		this.statCodeGen = statCodeGen;
		this.varDeclCodeGen = varDeclCodeGen;
	}

	/**
	 * イベントハンドラのコードを作成する
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

		compiledNodeList.forEach(symbol -> {
			if (SymbolNames.Event.LIST.contains(symbol.getSymbolName())) {
				genEventHandler(symbol, code, nestLevel, option);
			}
		});
	}

	/**
	 * イベントハンドラのコードを作成する
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

		String lockVar = Keywords.Prefix.lockVarPrefix + eventNode.getSymbolID();
		String funcName = common.genFuncName(eventNode);
		genHeaderSnippetOfEventCall(code, funcName, lockVar, nestLevel);

		// _sleep(...)
		if (eventNode.getSymbolName().equals(SymbolNames.Event.DELAYED_START_EVENT)) {
			TextNode delayTimeNode = (TextNode)eventNode.findSymbolInDescendants("*", "*", SymbolNames.Event.DELAY_TIME);
			code.append(common.indent(nestLevel + 4))
				.append(common.genFuncCallCode(ScriptIdentifiers.Funcs.SLEEP, delayTimeNode.getText()))
				.append(";")
				.append(Keywords.newLine);
		}

		SyntaxSymbol stat = eventNode.findSymbolInDescendants("*", SymbolNames.Stat.STAT_LIST, "*");
		statCodeGen.genStatement(stat, code, nestLevel + 4, option);

		genFooterSnippetOfEventCall(code, lockVar, nestLevel);

		// _addEvent(...);
		getEventType(eventNode).ifPresent(eventType -> {
			String addEventCallStat =
				common.genFuncCallCode(
					ScriptIdentifiers.Funcs.ADD_EVENT,
					funcName,
					"'" + eventType.toString() + "'");
			addEventCallStat += ";" + Keywords.newLine;
			code.append(common.indent(nestLevel)).append(addEventCallStat).append(Keywords.newLine);
		});
	}

	/**
	 * イベントノードからイベントの種類を取得する
	 * @param eventNode イベントの種類を取得したいイベントノード
	 * @return イベントの種類
	 * */
	private Optional<BhProgramData.EVENT> getEventType(SyntaxSymbol eventNode) {

		switch (eventNode.getSymbolName()) {
			case SymbolNames.Event.KEY_PRESS_EVENT:
				TextNode eventTypeNode = (TextNode)eventNode.findSymbolInDescendants("*", "*", SymbolNames.Event.KEY_CODE);
				return Optional.ofNullable(KEY_TO_KEYPRESSED_EVENT.get(eventTypeNode.getText()));

			case SymbolNames.Event.DELAYED_START_EVENT:
				return Optional.of(BhProgramData.EVENT.PROGRAM_START);

			default:
		}

		return Optional.empty();
	}

	/**
	 * イベントハンドラ呼び出しの前の定型文を生成する
	 * @param code 生成したコードの格納先
	 * @param lockVar ロックオブジェクトの変数
	 * @param nestLevel ソースコードのネストレベル
	 * */
	public void genHeaderSnippetOfEventCall(
		StringBuilder code,
		String funcName,
		String lockVar,
		int nestLevel) {

		// let lockVar = new _genLockObj();
		varDeclCodeGen.genVarDeclStat(code, lockVar, ScriptIdentifiers.Funcs.GEN_LOCK_OBJ + "()", nestLevel);

		// function funcName() {...
		code.append(common.indent(nestLevel))
			.append(Keywords.JS._function_)
			.append(funcName)
			.append("(){").append(Keywords.newLine);

		// if (_tryLock(lockObj)) {
		code.append(common.indent(nestLevel + 1))
			.append(Keywords.JS._if_)
			.append("(")
			.append(common.genFuncCallCode(ScriptIdentifiers.Funcs.TRY_LOCK, lockVar))
			.append(") {").append(Keywords.newLine);

		//try {
		code.append(common.indent(nestLevel + 2))
			.append(Keywords.JS._try_)
			.append("{").append(Keywords.newLine);

		// _end : {...
		code.append(common.indent(nestLevel + 3))
			.append(ScriptIdentifiers.Label.end).append(" : {").append(Keywords.newLine);

		// _initThisObj.call(this);
		code.append(common.indent(nestLevel + 4))
			.append(common.genFuncPrototypeCallCode(ScriptIdentifiers.Funcs.INIT_THIS_OBJ, Keywords.JS._this))
			.append(";").append(Keywords.newLine);
	}

	/**
	 * イベントハンドラ呼び出しの後ろの定型文を生成する
	 * @param code 生成したコードの格納先
	 * @param lockVar ロックオブジェクトの変数
	 * @param nestLevel ソースコードのネストレベル
	 * */
	public void genFooterSnippetOfEventCall(
		StringBuilder code,
		String lockVar,
		int nestLevel) {

		// end of "_end : {..."
		code.append(common.indent(nestLevel + 3)).append("}").append(Keywords.newLine);

		// end of "try {"
		code.append(common.indent(nestLevel + 2)).append("}").append(Keywords.newLine);

		// finally {
		//     _unlock(...);
		// }
		code.append(common.indent(nestLevel + 2))
			.append(Keywords.JS._finally_).append("{").append(Keywords.newLine)
			.append(common.indent(nestLevel + 3))
			.append(common.genFuncCallCode(ScriptIdentifiers.Funcs.UNLOCK, lockVar))
			.append(";").append(Keywords.newLine)
			.append(common.indent(nestLevel + 2)).append("}").append(Keywords.newLine);

		// end of "if(_tryLock())"
		code.append(common.indent(nestLevel + 1)).append("}").append(Keywords.newLine);

		// end of "function funcName() {..."
		code.append(common.indent(nestLevel)).append("}").append(Keywords.newLine);
	}
}





















