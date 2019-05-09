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
package net.seapanda.bunnyhop.model.imitation;

import org.mozilla.javascript.ContextFactory;
import org.mozilla.javascript.Script;
import org.mozilla.javascript.ScriptableObject;

import net.seapanda.bunnyhop.common.BhParams;
import net.seapanda.bunnyhop.common.VersionInfo;
import net.seapanda.bunnyhop.common.tools.MsgPrinter;
import net.seapanda.bunnyhop.configfilereader.BhScriptManager;
import net.seapanda.bunnyhop.model.node.BhNode;
import net.seapanda.bunnyhop.model.templates.BhNodeAttributes;
import net.seapanda.bunnyhop.modelprocessor.ImitationBuilder;
import net.seapanda.bunnyhop.undo.UserOperationCommand;
/**
 * イミテーションノード操作のインタフェース<br>
 * @author K.Koike
 */
public abstract class Imitatable extends BhNode {

	private static final long serialVersionUID = VersionInfo.SERIAL_VERSION_UID;
	/** オリジナルノードとのつながりが切れた際のイミテーションノードの削除直前に呼ばれるスクリプトの名前 */
	private final String scriptNameOnImitDeletionRequested; //!<
	private final String scriptNameOfScopeChecker; //!< イミテーションノードがスコープ内かどうかをチェックするスクリプトの名前

	public Imitatable(String type, BhNodeAttributes attributes) {
		super(type, attributes);
		scriptNameOnImitDeletionRequested = attributes.getOnImitDeletionRequested();
		scriptNameOfScopeChecker = attributes.getScopeChecker();
	}

	/**
	 * コピーコンストラクタ
	 * @param org コピー元オブジェクト
	 */
	public Imitatable(Imitatable org) {
		super(org);
		scriptNameOnImitDeletionRequested = org.scriptNameOnImitDeletionRequested;
		scriptNameOfScopeChecker = org.scriptNameOfScopeChecker;
	}

	/**
	 * 引数で指定したイミテーションタグに対応したイミテーションノードを作成する
	 * @param userOpeCmd undo用コマンドオブジェクト
	 * @param imitID このイミテーションIDに対応したイミテーションノードを作成する
	 * @return 作成されたイミテーションノード. イミテーションを持たないノードの場合nullを返す
	 */
	public abstract BhNode createImitNode(UserOperationCommand userOpeCmd, ImitationID imitID);

	/**
	 * イミテーションノードであった場合true を返す
	 * @return イミテーションノードであった場合true を返す
	 */
	public boolean isImitationNode() {
		return getImitationInfo().isImitationNode();
	}

	/**
	 * イミテーションノード情報を格納するオブジェクトを返す
	 * @return イミテーション情報
	 */
	public abstract <T extends Imitatable> ImitationInfo<T> getImitationInfo();

	/**
	 * 入れ替え用の既存のイミテーションノードを探す. <br>
	 * 見つからない場合は, 新規作成する.
	 * @param oldNode この関数が返すノードと入れ替えられる古いノード
	 * @param userOpeCmd undo用コマンドオブジェクト
	 * @return oldNodeと入れ替えるためのイミテーションノード
	 */
	public Imitatable findExistingOrCreateNewImit(BhNode oldNode, UserOperationCommand userOpeCmd) {

		BhNode outerTailOfOldNode = oldNode.findOuterNode(-1);
		for(Imitatable imit : getImitationInfo().getImitationList()) {
			//新しく入れ替わるノードの外部末尾ノードが最後に入れ替わったノードの外部末尾ノードと一致するイミテーションノードを入れ替えイミテーションノードとする
			if  (imit.getLastReplaced() != null) {
				if(!imit.isInWorkspace() && imit.getLastReplaced().findOuterNode(-1) == outerTailOfOldNode) {
					return imit;
				}
			}
		}
		return ImitationBuilder.buildForAutoCreation(this, userOpeCmd);
	}

	/**
	 * オリジナルノードと同じスコープにいるかチェックする. <br>
	 * スコープが無いノードの場合 -> スコープ内 <br>
	 * スコープがあるときこのノードが, <br>
	 * &nbsp;&nbsp; ROOT_DANGLING -> スコープ外 <br>
	 * &nbsp;&nbsp; ROOT_DIRECTLY_UNDER_WS -> オリジナルノードも ROOT_DIRECTLY_UNDER_WS ならスコープ内 <br>
	 * &nbsp;&nbsp; CHILD -> オリジナルノードと同じ同じスコープにいればスコープ内 <br>
	 * &nbsp;&nbsp; DELETED -> スコープ内
	 *
	 * @return スコープ外なら true. スコープ内なら false.
	 */
	public boolean isUnscoped() {

		if (!isImitationNode() || !hasScope())
			return false;

		if (getState() == BhNode.State.ROOT_DANGLING)
			return true;

		if (getState() == BhNode.State.DELETED)
			return false;

		return !isScoped();
	}

	/**
	 * オリジナル - イミテーションの関係を削除する
	 * @param toDelete 削除するイミテーションノード
	 * @param userOpeCmd undo用コマンドオブジェクト
	 */
	public void disconnectOrgImitRelation(Imitatable toDelete, UserOperationCommand userOpeCmd) {
		getImitationInfo().removeImitation(toDelete, userOpeCmd);
		toDelete.getImitationInfo().setOriginal(null, userOpeCmd);
	}

	@Override
	public boolean isRemovable() {
		if (parentConnector == null)
			return false;

		if (isDefaultNode())	//デフォルトノードは移動不可
			return false;

		return !parentConnector.isFixed();
	}

	@Override
	public boolean canBeReplacedWith(BhNode node) {

		if (getState() != BhNode.State.CHILD)
			return false;

		if (findRootNode().getState() != BhNode.State.ROOT_DIRECTLY_UNDER_WS)
			return false;

		if (node.isDescendantOf(this) || this.isDescendantOf(node))	//同じtree に含まれている場合置き換え不可
			return false;

		return parentConnector.isConnectedNodeReplaceableWith(node);
	}

	/**
	 * オリジナルノードとのつながりが切れた際に, イミテーションノードを削除する直前に呼ばれるイベント処理を実行する.
	 * @param userOpeCmd undo用コマンドオブジェクト
	 * */
	public void execScriptOnImitDeletionRequested(UserOperationCommand userOpeCmd) {

		Script onImitDeletionRequested = BhScriptManager.INSTANCE.getCompiledScript(scriptNameOnImitDeletionRequested);
		if (onImitDeletionRequested == null)
			return;

		ScriptableObject.putProperty(scriptScope, BhParams.JsKeyword.KEY_BH_USER_OPE_CMD, userOpeCmd);
		try {
			ContextFactory.getGlobal().call(cx -> onImitDeletionRequested.exec(cx, scriptScope));
		}
		catch (Exception e) {
			MsgPrinter.INSTANCE.errMsgForDebug(
				Imitatable.class.getSimpleName() + ".execScriptOnImitDeletionRequested   " +
				scriptNameOnImitDeletionRequested + "\n" + e.toString() + "\n");
		}
	}

	/**
	 * このノードがスコープ内ノードかどうか調べる.
	 * @return スコープ内ノードである場合 true.
	 * */
	private boolean isScoped() {

		Script scopeChecker = BhScriptManager.INSTANCE.getCompiledScript(scriptNameOfScopeChecker);
		if (scopeChecker == null) {
			if (hasScope()) {
				throw new AssertionError(this.getClass().getSimpleName() + ".isInScope  (scope check script not found)");
			}
			else {
				return true;
			}
		}

		Object isInScope = null;
		try {
			isInScope = ContextFactory.getGlobal().call(cx -> scopeChecker.exec(cx, scriptScope));
		}
		catch (Exception e) {
			MsgPrinter.INSTANCE.errMsgForDebug(
				Imitatable.class.getSimpleName() + ".isInScope   " + scriptNameOfScopeChecker + "\n" +
				e.toString() + "\n");
		}

		if (isInScope instanceof Boolean)
			return (Boolean)isInScope;

		throw new AssertionError(this.getClass().getSimpleName() + ".isInScope  (Scope checker must return a boolean value.)");
	}

	private boolean hasScope() {
		return !scriptNameOfScopeChecker.isEmpty();
	}
}


