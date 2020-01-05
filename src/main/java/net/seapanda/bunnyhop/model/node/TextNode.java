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
package net.seapanda.bunnyhop.model.node;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Predicate;

import org.mozilla.javascript.ContextFactory;
import org.mozilla.javascript.NativeObject;
import org.mozilla.javascript.Script;
import org.mozilla.javascript.ScriptableObject;

import net.seapanda.bunnyhop.common.Pair;
import net.seapanda.bunnyhop.common.constant.BhParams;
import net.seapanda.bunnyhop.common.constant.VersionInfo;
import net.seapanda.bunnyhop.common.tools.MsgPrinter;
import net.seapanda.bunnyhop.common.tools.Util;
import net.seapanda.bunnyhop.configfilereader.BhScriptManager;
import net.seapanda.bunnyhop.message.MsgService;
import net.seapanda.bunnyhop.model.node.attribute.BhNodeID;
import net.seapanda.bunnyhop.model.node.attribute.BhNodeViewType;
import net.seapanda.bunnyhop.model.node.event.BhNodeEvent;
import net.seapanda.bunnyhop.model.node.imitation.ImitationBase;
import net.seapanda.bunnyhop.model.node.imitation.ImitationID;
import net.seapanda.bunnyhop.model.syntaxsynbol.SyntaxSymbol;
import net.seapanda.bunnyhop.model.templates.BhNodeAttributes;
import net.seapanda.bunnyhop.model.templates.BhNodeTemplates;
import net.seapanda.bunnyhop.modelprocessor.BhModelProcessor;
import net.seapanda.bunnyhop.undo.UserOperationCommand;

/**
 * 文字情報を持つ終端BhNode
 * @author K.Koike
 */
public class TextNode  extends ImitationBase<TextNode> {

	private static final long serialVersionUID = VersionInfo.SERIAL_VERSION_UID;
	private String text = "";	//!< このノードの管理する文字列データ

	/**
	 * コンストラクタ<br>
	 * ノード内部・外部とは描画位置のこと
	 * @param type 関連する BhNodeView の種類
	 * @param imitIdToImitNodeID イミテーションIDとそれに対応するイミテーションノードIDのマップ
	 * @param attributes ノードの設定情報
	 * */
	public TextNode(
		BhNodeViewType type,
		Map<ImitationID, BhNodeID> imitIdToImitNodeID,
		BhNodeAttributes attributes) {

		super(type, attributes, imitIdToImitNodeID);
		registerScriptName(BhNodeEvent.ON_TEXT_FORMATTING, attributes.getTextFormatter());
		registerScriptName(BhNodeEvent.ON_TEXT_ACCEPTABILITY_CHECKING, attributes.getTextAcceptabilityChecker());
		text = attributes.getIinitString();
	}

	/**
	 * コピーコンストラクタ
	 * @param org コピー元オブジェクト
	 */
	private TextNode(TextNode org, UserOperationCommand userOpeCmd) {

		super(org, userOpeCmd);
		text = org.text;
	}

	@Override
	public TextNode copy(UserOperationCommand userOpeCmd, Predicate<BhNode> isNodeToBeCopied) {

		TextNode newNode = new TextNode(this, userOpeCmd);
		return newNode;
	}

	@Override
	public void accept(BhModelProcessor processor) {
		processor.visit(this);
	}

	/**
	 * このノードが保持している文字列を返す
	 * @return 表示文字列
	 */
	public String getText() {
		return text;
	}

	/**
	 * 引数の文字列をセットする
	 * @param text セットする文字列
	 */
	public void setText(String text) {
		this.text = text;
	}


	/**
	 * 引数の文字列がセット可能かどうか判断する
	 * @param text セット可能かどうか判断する文字列
	 * @return 引数の文字列がセット可能だった
	 */
	public boolean isTextAcceptable(String text) {

		Optional<String> scriptName = getScriptName(BhNodeEvent.ON_TEXT_ACCEPTABILITY_CHECKING);
		Script textAcceptabilityChecker =
			scriptName.map(BhScriptManager.INSTANCE::getCompiledScript).orElse(null);

		if (textAcceptabilityChecker == null)
			return true;

		ScriptableObject scriptScope = getEventDispatcher().newDefaultScriptScope();
		ScriptableObject.putProperty(scriptScope, BhParams.JsKeyword.KEY_BH_TEXT, text);
		Object jsReturn = null;
		try {
			jsReturn = ContextFactory.getGlobal().call(cx -> textAcceptabilityChecker.exec(cx, scriptScope));
		}
		catch (Exception e) {
			MsgPrinter.INSTANCE.errMsgForDebug(
				TextNode.class.getSimpleName() +  "::isTextAcceptable   " + scriptName.get() + "\n" +
				e.toString() + "\n");
		}

		if(jsReturn instanceof Boolean)
			return (Boolean)jsReturn;

		return false;
	}

	/**
	 * 入力されたテキストを整形して返す.
	 * @param text 整形対象の全文字列
	 * @param addedText 前回整形したテキストから新たに追加された文字列
	 * @return _1 -> テキスト全体を整形した場合 true. 追加分だけ整形した場合 false. <br>
	 *          _2 -> 整形したテキスト
	 */
	public Pair<Boolean, String> formatText(String text, String addedText) {

		Optional<String> scriptName = getScriptName(BhNodeEvent.ON_TEXT_FORMATTING);
		Script textFormatter = scriptName.map(BhScriptManager.INSTANCE::getCompiledScript).orElse(null);

		if (textFormatter == null)
			return new Pair<Boolean, String>(false, addedText);

		ScriptableObject scriptScope = getEventDispatcher().newDefaultScriptScope();
		ScriptableObject.putProperty(scriptScope, BhParams.JsKeyword.KEY_BH_TEXT, text);
		ScriptableObject.putProperty(scriptScope, BhParams.JsKeyword.KEY_BH_ADDED_TEXT, addedText);
		try {
			NativeObject jsObj = (NativeObject)ContextFactory.getGlobal().call(	cx -> textFormatter.exec(cx, scriptScope));
			Boolean isEntireTextFormatted = (Boolean)jsObj.get(BhParams.JsKeyword.KEY_BH_IS_ENTIRE_TEXT_FORMATTED);
			String formattedText = (String)jsObj.get(BhParams.JsKeyword.KEY_BH_FORMATTED_TEXT);
			return new Pair<Boolean, String>(isEntireTextFormatted, formattedText);
		}
		catch (Exception e) {
			MsgPrinter.INSTANCE.errMsgForDebug(
				TextNode.class.getSimpleName() +  "::getFormattedText   " + scriptName.get() + "\n" +
				e.toString() + "\n");
		}

		return new Pair<Boolean, String>(false, addedText);
	}

	/**
	 * イミテーションノードにこのノードを模倣させる
	 */
	public void getImitNodesToImitateContents() {

		String viewText = MsgService.INSTANCE.getViewText(this);
		getImitationList().forEach(imit -> MsgService.INSTANCE.imitateText(imit, text, viewText));
	}

	@Override
	public BhNode findOuterNode(int generation) {
		if (generation <= 0)
			return this;

		return null;
	}

	@Override
	public void findSymbolInDescendants(
		int generation,
		boolean toBottom,
		List<SyntaxSymbol> foundSymbolList,
		String... symbolNames) {

		if (generation == 0)
			for (String symbolName : symbolNames)
				if (Util.INSTANCE.equals(getSymbolName(), symbolName))
					foundSymbolList.add(this);
	}

	@Override
	public TextNode createImitNode(ImitationID imitID, UserOperationCommand userOpeCmd) {

		//イミテーションノード作成
		BhNode imitationNode = BhNodeTemplates.INSTANCE.genBhNode(getImitationNodeID(imitID), userOpeCmd);

		if (!(imitationNode instanceof TextNode))
			throw new AssertionError("imitation node type inconsistency");

		//オリジナルとイミテーションの関連付け
		TextNode textImit = (TextNode)imitationNode;
		addImitation(textImit, userOpeCmd);
		textImit.setOriginal(this, userOpeCmd);
		return textImit;
	}

	@Override
	protected TextNode self() {
		return this;
	}

	@Override
	public void show(int depth) {

		String parentHash = "";
		if (parentConnector != null)
			parentHash = parentConnector.hashCode() + "";

		String lastReplacedHash = "";
		if (getLastReplaced() != null)
			lastReplacedHash =  getLastReplaced().hashCode() + "";

		MsgPrinter.INSTANCE.msgForDebug(indent(depth) + "<TextNode" + "string=" + text + "  bhID=" + getID() + "  parent="+ parentHash + "> " + this.hashCode());
		MsgPrinter.INSTANCE.msgForDebug(indent(depth+1) + "<" + "ws " + workspace + "> ");
		MsgPrinter.INSTANCE.msgForDebug(indent(depth+1) + "<" + "last replaced " + lastReplacedHash + "> ");
		MsgPrinter.INSTANCE.msgForDebug(indent(depth+1) + "<" + "imitation" + "> ");
		getImitationList().forEach(imit -> {
			MsgPrinter.INSTANCE.msgForDebug(indent(depth+2) + "imit " + imit.hashCode());
		});

	}
}



















