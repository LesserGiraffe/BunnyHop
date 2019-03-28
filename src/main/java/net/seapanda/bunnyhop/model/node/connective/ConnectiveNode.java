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
package net.seapanda.bunnyhop.model.node.connective;

import java.util.List;
import java.util.Map;
import java.util.function.Predicate;

import org.mozilla.javascript.ContextFactory;
import org.mozilla.javascript.Script;
import org.mozilla.javascript.ScriptableObject;

import net.seapanda.bunnyhop.common.BhParams;
import net.seapanda.bunnyhop.common.tools.MsgPrinter;
import net.seapanda.bunnyhop.common.tools.Util;
import net.seapanda.bunnyhop.configfilereader.BhScriptManager;
import net.seapanda.bunnyhop.model.imitation.Imitatable;
import net.seapanda.bunnyhop.model.imitation.ImitationID;
import net.seapanda.bunnyhop.model.imitation.ImitationInfo;
import net.seapanda.bunnyhop.model.node.BhNode;
import net.seapanda.bunnyhop.model.node.BhNodeID;
import net.seapanda.bunnyhop.model.node.SyntaxSymbol;
import net.seapanda.bunnyhop.model.templates.BhNodeTemplates;
import net.seapanda.bunnyhop.modelprocessor.BhModelProcessor;
import net.seapanda.bunnyhop.undo.UserOperationCommand;

/**
 * 子ノードと接続されるノード
 * @author K.Koike
 * */
public class ConnectiveNode extends Imitatable {

	private Section childSection;							//!< セクションの集合 (ノード内部に描画されるもの)
	private ImitationInfo<ConnectiveNode> imitInfo;	//!< イミテーションノードに関連する情報がまとめられたオブジェクト
	private final String scriptNameOnChildReplaced;	//!< 子ノード入れ替え維持に実行されるスクリプト

	/**
	 * コンストラクタ<br>
	 * ノード内部・外部とは描画位置のこと
	 * @param id ノードID (\<Node\> タグの bhID)
	 * @param name ノード名 (\<Node\> タグの name)
	 * @param childSection 子セクション
	 * @param scopeName イミテーションノードがオリジナルノードと同じスコープにいるかチェックする際の名前
	 * @param scriptNameOnMovedFromChildToWS ワークスペース移動時に実行されるスクリプトの名前
	 * @param scriptNameOnMovedToChild 子ノードとして接続されたときに実行されるスクリプトの名前
	 * @param scriptNameOnChildReplaced 子ノードが入れ替わったときに実行されるスクリプトの名前
	 * @param scriptNameJustBeforeChildToBeDeleted 子ノードが選択削除される直前に実行されるスクリプトの名前
	 * @param imitID_imitNodeID イミテーションタグとそれに対応するイミテーションノードIDのマップ
	 * @param canCreateImitManually このノードがイミテーション作成機能を持つ場合true
	 * */
	public ConnectiveNode(
			BhNodeID id,
			String name,
			Section childSection,
			String scopeName,
			String scriptNameOnMovedFromChildToWS,
			String scriptNameOnMovedToChild,
			String scriptNameOnChildReplaced,
			Map<ImitationID, BhNodeID> imitID_imitNodeID,
			boolean canCreateImitManually) {
		super(id,
			name,
			BhParams.BhModelDef.ATTR_VALUE_CONNECTIVE,
			scriptNameOnMovedFromChildToWS,
			scriptNameOnMovedToChild);
		this.childSection = childSection;
		imitInfo = new ImitationInfo<>(imitID_imitNodeID, canCreateImitManually, scopeName);
		this.scriptNameOnChildReplaced = scriptNameOnChildReplaced;
	}

	/**
	 * コピーコンストラクタ
	 * @param org コピー元オブジェクト
	 */
	private ConnectiveNode(ConnectiveNode org) {
		super(org);
		scriptNameOnChildReplaced = org.scriptNameOnChildReplaced;
	}

	@Override
	public ConnectiveNode copy(UserOperationCommand userOpeCmd, Predicate<BhNode> isNodeToBeCopied) {

		ConnectiveNode newNode = new ConnectiveNode(this);
		newNode.childSection = childSection.copy(userOpeCmd, isNodeToBeCopied);
		newNode.childSection.setParent(newNode);
		newNode.imitInfo = new ImitationInfo<>(imitInfo, userOpeCmd, newNode);
		return newNode;
	}

	/**
	 * BhModelProcessor に自オブジェクトを渡す
	 * @param processor 自オブジェクトを渡す BhModelProcessorオブジェクト
	 * */
	@Override
	public void accept(BhModelProcessor processor) {
		processor.visit(this);
	}

	/**
	 * BhModelProcessor を子Section に渡す
	 * @param processor 子Section に渡す BhModelProcessor
	 * */
	public void introduceSectionsTo(BhModelProcessor processor) {
		childSection.accept(processor);
	}


	@Override
	public ConnectiveNode createImitNode(UserOperationCommand userOpeCmd, ImitationID imitID) {

		//イミテーションノード作成
		BhNode imitationNode = BhNodeTemplates.INSTANCE.genBhNode(imitInfo.getImitationNodeID(imitID), userOpeCmd);

		//オリジナルとイミテーションの関連付け
		ConnectiveNode connectiveImit = (ConnectiveNode)imitationNode; //ノードテンプレート作成時に整合性チェックしているのでキャストに問題はない
		imitInfo.addImitation(connectiveImit, userOpeCmd);
		connectiveImit.imitInfo.setOriginal(this, userOpeCmd);
		return connectiveImit;
	}

	@Override
	public ImitationInfo<ConnectiveNode> getImitationInfo() {
		return imitInfo;
	}

	@Override
	public ConnectiveNode getOriginalNode() {
		return imitInfo.getOriginal();
	}

	@Override
	public BhNode findOuterNode(int generation) {

		BhNode outerNode = childSection.findOuterNode(generation);
		if (outerNode != null)
			return outerNode;

		if (generation <= 0)
			return this;

		return null;
	}

	/**
	 * 子ノードが入れ替わったときのスクリプトを実行する
	 * @param oldChild 入れ替わった古いノード
	 * @param newChild 入れ替わった新しいノード
	 * @param parentCnctr 子が入れ替わったコネクタ
	 * @param userOpeCmd undo用コマンドオブジェクト
	 * */
	public void execScriptOnChildReplaced(
		BhNode oldChild,
		BhNode newChild,
		Connector parentCnctr,
		UserOperationCommand userOpeCmd) {

		Script onChildReplaced = BhScriptManager.INSTANCE.getCompiledScript(scriptNameOnChildReplaced);
		if (onChildReplaced == null)
			return;

		ScriptableObject.putProperty(scriptScope, BhParams.JsKeyword.KEY_BH_REPLACED_NEW_NODE, newChild);
		ScriptableObject.putProperty(scriptScope, BhParams.JsKeyword.KEY_BH_REPLACED_OLD_NODE, oldChild);
		ScriptableObject.putProperty(scriptScope, BhParams.JsKeyword.KEY_BH_PARENT_CONNECTOR, parentCnctr);
		ScriptableObject.putProperty(scriptScope, BhParams.JsKeyword.KEY_BH_USER_OPE_CMD, userOpeCmd);
		try {
			ContextFactory.getGlobal().call(cx -> onChildReplaced.exec(cx, scriptScope));
		}
		catch (Exception e) {
			MsgPrinter.INSTANCE.errMsgForDebug(
				BhNode.class.getSimpleName() +  ".execOnMovedToChildScript   " + scriptNameOnChildReplaced + "\n" +
				e.toString() + "\n");
		}
	}

	/**
	 * モデルの構造を表示する
	 * @param depth 表示インデント数
	 * */
	@Override
	public void show(int depth) {

		String parentHashCode = "null";
		if (parentConnector != null)
			parentHashCode = parentConnector.hashCode() + "";

		String lastReplacedHash = "";
		if (getLastReplaced() != null)
			lastReplacedHash =  getLastReplaced().hashCode() + "";

		MsgPrinter.INSTANCE.msgForDebug(indent(depth) + "<ConnectiveNode" + "  bhID=" + getID()  + "  parent=" + parentHashCode + "  > " + this.hashCode());
		MsgPrinter.INSTANCE.msgForDebug(indent(depth+1) + "<" + "last replaced " + lastReplacedHash + "> ");
		MsgPrinter.INSTANCE.msgForDebug(indent(depth+1) + "<" + "scopeName " + imitInfo.scopeName + "> ");
		MsgPrinter.INSTANCE.msgForDebug(indent(depth+1) + "<" + "imitation" + "> ");
		imitInfo.getImitationList().forEach(imit -> {
			MsgPrinter.INSTANCE.msgForDebug(indent(depth+2) + "imit " + imit.hashCode());
		});
		childSection.show(depth + 1);
	}

	@Override
	public void findSymbolInDescendants(int generation, boolean toBottom, List<SyntaxSymbol> foundSymbolList, String... symbolNames) {

		if (generation == 0) {
			for (String symbolName : symbolNames) {
				if (Util.INSTANCE.equals(getSymbolName(), symbolName)) {
					foundSymbolList.add(this);
				}
			}
			if (!toBottom) {
				return;
			}
		}

		childSection.findSymbolInDescendants(Math.max(0, generation-1), toBottom, foundSymbolList, symbolNames);
	}
}






















