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

import java.util.function.Predicate;

import org.mozilla.javascript.ContextFactory;
import org.mozilla.javascript.Script;
import org.mozilla.javascript.ScriptableObject;

import net.seapanda.bunnyhop.common.BhParams;
import net.seapanda.bunnyhop.common.tools.MsgPrinter;
import net.seapanda.bunnyhop.common.tools.Util;
import net.seapanda.bunnyhop.configfilereader.BhScriptManager;
import net.seapanda.bunnyhop.message.BhMsg;
import net.seapanda.bunnyhop.message.MsgData;
import net.seapanda.bunnyhop.message.MsgProcessor;
import net.seapanda.bunnyhop.message.MsgReceptionWindow;
import net.seapanda.bunnyhop.message.MsgService;
import net.seapanda.bunnyhop.model.Workspace;
import net.seapanda.bunnyhop.model.imitation.Imitatable;
import net.seapanda.bunnyhop.model.node.connective.ConnectiveNode;
import net.seapanda.bunnyhop.model.node.connective.Connector;
import net.seapanda.bunnyhop.model.templates.BhNodeTemplates;
import net.seapanda.bunnyhop.modelhandler.BhNodeHandler;
import net.seapanda.bunnyhop.modelprocessor.ImitationReplacer;
import net.seapanda.bunnyhop.modelprocessor.NodeDeleter;
import net.seapanda.bunnyhop.undo.UserOperationCommand;
import net.seapanda.bunnyhop.view.node.BhNodeView;

/**
 * ノードの基底クラス
 * @author K.Koike
 */
public abstract class BhNode extends SyntaxSymbol implements MsgReceptionWindow {

	private final BhNodeID bhID; //!< ノードID (\<Node\> タグの bhID)
	protected Connector parentConnector;	//!< このノードを繋いでいるコネクタ
	protected Workspace workspace;	//!< このノードがあるWorkSpace.
	private final String scriptNameOnMovedFromChildToWS;	//子ノードからワークスペースに移されたときに実行されるスクリプトの名前
	private final String scriptNameOnMovedToChild;	//!< ワークスペースもしくは, 子ノードから子ノードに移されたときに実行されるスクリプトの名前
	public final String type;	//!< ノードのタイプ (connective, void, textField, ...)
	private BhNode lastReplaced;	//!< 最後にこのノードと入れ替わったノード
	private boolean isDefaultNode = false;	//!< デフォルトノードである場合true
	transient protected ScriptableObject scriptScope;	//!< Javascript実行時の変数スコープ
	transient private MsgProcessor msgProcessor;	//!< このオブジェクト宛てに送られたメッセージを処理するオブジェクト

	/**
	 * BhNode がとり得る状態
	 * workspace
	 */
	public enum State {
		ROOT_DIRECTLY_UNDER_WS,	//!< ワークスペース直下のルートノード
		ROOT_DANGLING,			//!< ワークスペース直下に無いルートノード (宙ぶらりん状態)
		CHILD,						//!< 子ノード (ルートが宙ぶらりんかどうかは問わない)
		DELETED,					//!< 削除済み
	}

	/**
	 * 手動で子ノードからの取り外しができる場合 trueを返す<br>
	 * ルートノードの場合falseが返る
	 * @return 手動で子ノードからの取り外しができる場合 true
	 */
	public abstract boolean isRemovable();

	/**
	 * 引数のノードをこのノードの代わりに置き換えることができる場合 true を返す
	 * @param node 置き換え対象のBhNode
	 * @return このノードの代わりに置き換えられる場合 true
	 * */
	public abstract boolean canBeReplacedWith(BhNode node);

	/**
	 * このノードがイミテーションノードだった場合, そのオリジナルノードを返す<br>
	 * イミテーションノードで無かった場合 null を返す
	 * @return このノードのオリジナルノード. このノードがイミテーションノードで無い場合は null を返す
	 */
	public abstract Imitatable getOriginalNode();

	/**
	 * 外部ノードを取得する. 指定した世代に外部ノードがなかった場合, nullを返す.
	 * @param generation 取得する外部ノードの世代.
	 *               例 (0: 自分, 1: 子世代の外部ノード, 2: 孫世代の外部ノード. 負の数: 末尾の外部ノードを取得する)
	 * @return 外部ノード
	 */
	public abstract BhNode findOuterNode(int generation);

	/**
	 * このノード以下のノードツリーのコピーを作成し返す
	 * @param userOpeCmd undo用コマンドオブジェクト
	 * @param isNodeToBeCopied ノードがコピーの対象かどうかを判別する関数.
	 *                          ただし, copyを呼んだBhNodeは判定対象にならず, 必ずコピーされる.
	 * @return このノード以下のノードツリーのコピー
	 */
	public abstract BhNode copy(UserOperationCommand userOpeCmd, Predicate<BhNode> isNodeToBeCopied);

	/**
	 * コンストラクタ
	 * @param bhID ノードID (\<Node\> タグの bhID)
	 * @param symbolName 終端, 非終端記号名 (\<Node\> タグの name)
	 * @param type xml のtype属性
	 * @param scriptNameOnMovedFromChildToWS 子ノードからワークスペースに移されたときに実行されるスクリプトの名前
	 * @param scriptNameOnMovedToChild ワークスペースもしくは, 子ノードから子ノードに移されたときに実行されるスクリプトの名前
	 * */
	protected BhNode(
		BhNodeID bhID,
		String symbolName,
		String type,
		String scriptNameOnMovedFromChildToWS,
		String scriptNameOnMovedToChild) {
		super(symbolName);
		this.bhID = bhID;
		this.type = type;
		this.scriptNameOnMovedFromChildToWS = scriptNameOnMovedFromChildToWS;
		this.scriptNameOnMovedToChild = scriptNameOnMovedToChild;
	}

	/**
	 * コピーコンストラクタ
	 * @param org コピー元オブジェクト
	 */
	protected BhNode(BhNode org) {
		super(org);
		bhID = org.bhID;
		parentConnector = null;
		workspace = null;
		scriptNameOnMovedFromChildToWS = org.scriptNameOnMovedFromChildToWS;
		scriptNameOnMovedToChild = org.scriptNameOnMovedToChild;
		type = org.type;
		lastReplaced = null;
		scriptScope = null;
		isDefaultNode = org.isDefaultNode;
	}

	public BhNodeID getID() {
		return bhID;
	}

	/**
	 * ノードをワークスペース上から削除する
	 * @param userOpeCmd undo用コマンドオブジェクト
	 */
	public void delete(UserOperationCommand userOpeCmd) {
		accept(new NodeDeleter(userOpeCmd));
	}

	/**
	 * newBhNodeとこのノードを入れ替える
	 * @param newNode このノードと入れ替えるノード
	 * @param userOpeCmd undo用コマンドオブジェクト
	 * */
	public void replacedWith(BhNode newNode, UserOperationCommand userOpeCmd) {

		setLastReplaced(newNode, userOpeCmd);
		parentConnector.connectNode(newNode, userOpeCmd);	//model 更新
		newNode.accept(new ImitationReplacer(userOpeCmd, this));
	}

	/**
	 * このノードをモデルツリーから取り除く. ノードの削除は行わない.
	 * @param userOpeCmd undo用コマンドオブジェクト
	 * @return このノードを取り除いた結果, 新しくできたノード
	 * */
	public BhNode remove(UserOperationCommand userOpeCmd) {
		return parentConnector.remove(userOpeCmd);
	}

	/**
	 * ノードの状態を取得する
	 * @retval State.ROOT_DANGLING ルートノードであるがワークスペースには属していない
	 * @retval State.ROOT_DIRECTLY_UNDER_WS ワークスペースに属しているルートノード
	 * @retval State.CHILD 子ノード. ワークスペースに属していてもいなくても子ノードならばこの値が返る
	 */
	public State getState() {

		if(parentConnector == null) {
			if (workspace == null)
				return State.DELETED;

			if(workspace.containsAsRoot(this))
				return State.ROOT_DIRECTLY_UNDER_WS;
			else
				return State.ROOT_DANGLING;
		}
		else {
			assert workspace.containsAsRoot(this) == false;
			return State.CHILD;
		}
	}

	/**
	 * 移動可能なノードであるかどうかを調べる
	 * @return 移動可能なノードである場合 true
	 */
	public boolean isMovable() {
		return isRemovable() || (getState() == BhNode.State.ROOT_DIRECTLY_UNDER_WS);
	}

	/**
	 * 接続されるコネクタを登録する
	 * @param parentConnector このノードと繋がるコネクタ
	 * */
	public void setParentConnector(Connector parentConnector) {
		this.parentConnector = parentConnector;
	}

	/**
	 * このノードの親コネクタを返す
	 * @return 親コネクタ. 親コネクタが無い場合は, null を返す.
	 */
	public Connector getParentConnector() {
		return parentConnector;
	}

	/**
	 * このノードがあるワークスペースを登録する
	 * @param workspace このノードを直接保持するワークスペース
	 * @param userOpeCmd undo用コマンドオブジェクト
	 * */
	public void setWorkspace(Workspace workspace, UserOperationCommand userOpeCmd) {
		userOpeCmd.pushCmdOfSetWorkspace(this.workspace, this);
		this.workspace = workspace;
	}

	/**
	 * このノードがあるワークスペースを登録する
	 * @return このノードがあるワークスペース
	 * */
	public Workspace getWorkspace() {
		return workspace;
	}

	/**
	 * このノードがワークスペースに存在するノードである場合 true を返す
	 * @return このノードがワークスペースに存在するノードである場合 true
	 */
	public boolean isInWorkspace() {
		return workspace != null;
	}

	/**
	 * このノードと入れ替わったノードをセットする
	 * @param lastReplaced このノードと入れ替わったノード
	 * @param userOpeCmd undo用コマンドオブジェクト
	 */
	public void setLastReplaced(BhNode lastReplaced, UserOperationCommand userOpeCmd) {
		userOpeCmd.pushCmdOfSetLastReplaced(this.lastReplaced, this);
		this.lastReplaced = lastReplaced;
	}

	/**
	 * このノードと入れ替わったノードを取得する
	 * @return 最後にこのオブジェクトと入れ替わったノード
	 */
	public BhNode getLastReplaced() {
		return lastReplaced;
	}

	/**
	 * このノードが選択されているかどうかを調べる
	 * @return このノードが選択されている場合 trueを返す
	 */
	public boolean isSelected() {
		if (workspace == null)
			return false;
		return workspace.getSelectedNodeList().contains(this);
	}

	/**
	 * このBhNodeがデフォルトノードの場合true を返す
	 * @return このBhNodeがデフォルトノードの場合true
	 */
	public boolean isDefaultNode() {
		return isDefaultNode;
	}

	/**
	 * デフォルトノードかどうかを設定する
	 * @param isDefaultNode デフォルトノードの場合true
	 * */
	public void setDefaultNode(boolean isDefaultNode) {
		this.isDefaultNode = isDefaultNode;
	}

	/**
	 * このノードの親となるノードを返す<br> ルートノードの場合は null を返す
	 * @return このノードの親となるノード
	 * */
	public ConnectiveNode findParentNode() {

		if (parentConnector == null)
			return null;

		return parentConnector.getParentNode();
	}

	/**
	 * このノードのルートであるノードを返す <br> ルートノードの場合は自身を帰す
	 * @return このノードのルートノード
	 * */
	public BhNode findRootNode() {

		if (parentConnector == null)
			return this;

		return parentConnector.getParentNode().findRootNode();
	}

	@Override
	public boolean isDescendantOf(SyntaxSymbol ancestor) {

		if (this == ancestor)
			return true;

		if (parentConnector == null)
			return false;

		return parentConnector.isDescendantOf(ancestor);
	}

	@Override
	public SyntaxSymbol findSymbolInAncestors(String symbolName, int generation, boolean toTop) {

		if (generation == 0) {
			if (Util.INSTANCE.equals(getSymbolName(), symbolName)) {
				return this;
			}
			if (!toTop) {
				return null;
			}
		}

		if (parentConnector == null)
			return null;

		return parentConnector.findSymbolInAncestors(symbolName, Math.max(0, generation-1), toTop);
	}

	/**
	 * スクリプト実行時のスコープ変数を登録する
	 * @param view スクリプト実行時のスコープとして登録するBhNodeView
	 */
	public void setScriptScope(BhNodeView view) {
		scriptScope = BhScriptManager.INSTANCE.createScriptScope();
		ScriptableObject.putProperty(scriptScope, BhParams.JsKeyword.KEY_BH_THIS, this);
		ScriptableObject.putProperty(scriptScope, BhParams.JsKeyword.KEY_BH_NODE_VIEW, view);
		ScriptableObject.putProperty(scriptScope, BhParams.JsKeyword.KEY_BH_NODE_HANDLER, BhNodeHandler.INSTANCE);
		ScriptableObject.putProperty(scriptScope, BhParams.JsKeyword.KEY_BH_MSG_SERVICE, MsgService.INSTANCE);
		ScriptableObject.putProperty(scriptScope, BhParams.JsKeyword.KEY_BH_COMMON, BhScriptManager.INSTANCE.getCommonJsObj());
		ScriptableObject.putProperty(scriptScope, BhParams.JsKeyword.KEY_BH_NODE_TEMPLATES, BhNodeTemplates.INSTANCE);
		ScriptableObject.putProperty(scriptScope, BhParams.JsKeyword.KEY_BH_NODE_UTIL, Util.INSTANCE);
	}

	/**
	 * 子ノードに移ったときのスクリプトを実行する
	 * @param oldParent 移る前に接続されていた親. ワークスペースから子ノードに移動したときはnull.
	 * @param oldRoot 移る前に所属していたノードツリーのルートノード. ワークスペースから子ノードに移動したときは, このオブジェクト.
	 * @param oldReplaced 元々子ノードとしてつながっていたノード
	 * @param userOpeCmd undo用コマンドオブジェクト
	 * */
	public void execScriptOnMovedToChild(
		ConnectiveNode oldParent,
		BhNode oldRoot,
		BhNode oldReplaced,
		UserOperationCommand userOpeCmd) {

		Script onMovedToChild = BhScriptManager.INSTANCE.getCompiledScript(scriptNameOnMovedToChild);
		if (onMovedToChild == null)
			return;

		ScriptableObject.putProperty(scriptScope, BhParams.JsKeyword.KEY_BH_OLD_PARENT, oldParent);
		ScriptableObject.putProperty(scriptScope, BhParams.JsKeyword.KEY_BH_OLD_ROOT, oldRoot);
		ScriptableObject.putProperty(scriptScope, BhParams.JsKeyword.KEY_BH_REPLACED_OLD_NODE, oldReplaced);
		ScriptableObject.putProperty(scriptScope, BhParams.JsKeyword.KEY_BH_USER_OPE_CMD, userOpeCmd);
		try {
			ContextFactory.getGlobal().call(cx -> onMovedToChild.exec(cx, scriptScope));
		}
		catch (Exception e) {
			MsgPrinter.INSTANCE.errMsgForDebug(
				BhNode.class.getSimpleName() +  ".execOnMovedToChildScript   " + scriptNameOnMovedToChild + "\n" +
				e.toString() + "\n");
		}
	}

	/**
	 * 子ノードからワークスペースに移ったときのスクリプトを実行する
	 * @param oldParent 移る前に接続されていた親
	 * @param oldRoot 移る前に所属していたルートノード
	 * @param newReplaced WSに移る際, このノードの替わりにつながったノード
	 * @param manuallyRemoved D&Dで子ノードからワークスペースに移された場合true
	 * @param userOpeCmd undo用コマンドオブジェクト
	 */
	public void execScriptOnMovedFromChildToWS(
		ConnectiveNode oldParent,
		BhNode oldRoot,
		BhNode newReplaced,
		Boolean manuallyRemoved,
		UserOperationCommand userOpeCmd) {

		Script onMovedFromChildToWS = BhScriptManager.INSTANCE.getCompiledScript(scriptNameOnMovedFromChildToWS);
		if (onMovedFromChildToWS == null)
			return;

		ScriptableObject.putProperty(scriptScope, BhParams.JsKeyword.KEY_BH_OLD_PARENT, oldParent);
		ScriptableObject.putProperty(scriptScope, BhParams.JsKeyword.KEY_BH_OLD_ROOT, oldRoot);
		ScriptableObject.putProperty(scriptScope, BhParams.JsKeyword.KEY_BH_REPLACED_NEW_NODE, newReplaced);
		ScriptableObject.putProperty(scriptScope, BhParams.JsKeyword.KEY_BH_MANUALLY_REMOVED, manuallyRemoved);
		ScriptableObject.putProperty(scriptScope, BhParams.JsKeyword.KEY_BH_USER_OPE_CMD, userOpeCmd);
		try {
			ContextFactory.getGlobal().call(cx -> onMovedFromChildToWS.exec(cx, scriptScope));
		}
		catch (Exception e) {
			MsgPrinter.INSTANCE.errMsgForDebug(
				BhNode.class.getSimpleName() + ".execOnMovedFromChildToWSScript   " + scriptNameOnMovedFromChildToWS + "\n" +
				e.toString() + "\n");
		}
	}

	@Override
	public void setMsgProcessor(MsgProcessor processor) {
		msgProcessor = processor;
	}

	@Override
	public MsgData passMsg(BhMsg msg, MsgData data) {
		return msgProcessor.processMsg(msg, data);
	}
}









