/**
 * Copyright 2018 K.Koike
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
package net.seapanda.bunnyhop.message;
import java.util.Collection;

import net.seapanda.bunnyhop.common.Pair;
import net.seapanda.bunnyhop.common.Vec2D;
import net.seapanda.bunnyhop.common.constant.BhParams;
import net.seapanda.bunnyhop.model.Workspace;
import net.seapanda.bunnyhop.model.WorkspaceSet;
import net.seapanda.bunnyhop.model.imitation.Imitatable;
import net.seapanda.bunnyhop.model.node.BhNode;
import net.seapanda.bunnyhop.model.node.TextNode;
import net.seapanda.bunnyhop.quadtree.QuadTreeRectangle;
import net.seapanda.bunnyhop.undo.UserOperationCommand;
import net.seapanda.bunnyhop.view.node.BhNodeView;

/**
 * メッセージ送信を伴う処理のサービスクラス
 * @author K.Koike
 * */
public class MsgService {

	public static final MsgService INSTANCE = new MsgService();	//!< シングルトンインスタンス
	private WorkspaceSet wss;

	private MsgService() {}

	public void setWorkspaceSet(WorkspaceSet wss) {
		this.wss = wss;
	}

	/**
	 * 引数で指定したノードのワークスペース上での位置を取得する
	 * */
	public Vec2D getPosOnWS(BhNode node) {
		return MsgTransporter.INSTANCE.sendMessage(BhMsg.GET_POS_ON_WORKSPACE, node).vec2d;
	}

	/**
	 * 引数で指定したノードのワークスペース上での位置を更新する.<br>
	 * 4分木空間上の位置も更新する
	 * @param node ワークスペース上での位置を更新するノード. (ルートノードを指定すること)
	 * @param x ワークスペース上でのx位置
	 * @param y ワークスペース上でのy位置
	 * */
	public void setPosOnWS(BhNode node, double x, double y) {
		MsgTransporter.INSTANCE.sendMessage(BhMsg.SET_POS_ON_WORKSPACE, new MsgData(new Vec2D(x, y)), node);
	}

	/**
	 * ノードの可視性を変更する
	 * @param node このノードの可視性を変更する
	 * @param visible 可視状態にする場合true, 不可視にする場合false
	 * @param userOpeCmd undo用コマンドオブジェクト
	 * */
	public void setNodeVisibility(BhNode node, boolean visible, UserOperationCommand userOpeCmd) {
		MsgTransporter.INSTANCE.sendMessage(BhMsg.SET_VISIBLE, new MsgData(visible, userOpeCmd), node);
	}

	/**
	 * ノードの構文エラー表示を変更する
	 * @param node 警告表示を変更するノード
	 * @param show 警告を表示する場合 true. 隠す場合 false.
	 * @param userOpeCmd undo用コマンドオブジェクト
	 * */
	public void setSyntaxErrorIndicator(BhNode node, boolean show, UserOperationCommand userOpeCmd) {
		MsgTransporter.INSTANCE.sendMessage(BhMsg.SET_SYNTAX_ERRPR_INDICATOR, new MsgData(show, userOpeCmd), node);
	}

	/**
	 * undo/redo スタックを解放する
	 * @param wss ワークスペースセット
	 * */
	public void deleteUndoRedoCommand(WorkspaceSet wss) {
		MsgTransporter.INSTANCE.sendMessage(BhMsg.DELETE_USER_OPE_CMD, wss);
	}

	/**
	 * 複数ノード移動用マルチノードシフタとリンクを更新する
	 * @param node マルチノードシフタ更新の原因を作ったノード
	 * @param ws 更新するマルチノードシフタを含むワークスペース
	 * */
	public void updateMultiNodeShifter(BhNode node, Workspace ws) {
		MsgTransporter.INSTANCE.sendMessage(BhMsg.UPDATE_MULTI_NODE_SHIFTER, new MsgData(node), ws);
	}

	/**
	 * ノードボディのワークスペース上での範囲を取得する
	 * @param node このノードのワークスペース上での範囲を取得する
	 * */
	public Pair<Vec2D, Vec2D> getNodeBodyRange(BhNode node) {

		BhNodeView nodeView = getBhNodeView(node);
		QuadTreeRectangle bodyRange = nodeView.getRegionManager().getRegions()._1;
		return new Pair<Vec2D, Vec2D>(bodyRange.getUpperLeftPos(), bodyRange.getLowerRightPos());
	}

	/**
	 * 外部ノードを含んだノードのサイズを取得する
	 * @param node サイズを取得したいノード
	 * @return 外部ノードを含んだノードのサイズ
	 * */
	public Vec2D getViewSizeIncludingOuter(BhNode node) {

		MsgData msgData = MsgTransporter.INSTANCE.sendMessage(
			BhMsg.GET_VIEW_SIZE_INCLUDING_OUTER, new MsgData(true), node);
		return msgData.vec2d;
	}

	/**
	 * ワークスペース上のノードを動かす
	 * @param node 動かすノード
	 * @param distance 移動距離
	 * */
	public void moveNodeOnWS(BhNode node, Vec2D distance) {
		MsgTransporter.INSTANCE.sendMessage(BhMsg.MOVE_NODE_ON_WORKSPACE, new MsgData(distance), node);
	}

	/**
	 * 貼り付け候補のノードのリストから引数で指定したノードを取り除く.
	 * @param nodeToRemove 取り除くノード
	 * @param userOpeCmd undo用コマンドオブジェクト
	 * */
	public void removeNodeToPaste(BhNode nodeToRemove, UserOperationCommand userOpeCmd) {
		MsgTransporter.INSTANCE.sendMessage(BhMsg.REMOVE_NODE_TO_PASTE, new MsgData(nodeToRemove, userOpeCmd), wss);
	}

	/**
	 * ノードビューを入れ替える.
	 * @param oldNode 入れ替えられる古いノードビューの BhNode
	 * @param newNode 入れ替えられる新しいノードビューの BhNode
	 * @param userOpeCmd undo用コマンドオブジェクト
	 * */
	public void replaceChildNodeView(BhNode oldNode, BhNode newNode, UserOperationCommand userOpeCmd) {

		BhNodeView newNodeView = getBhNodeView(newNode);
		boolean hasParent = newNodeView.getParent() != null;
		MsgTransporter.INSTANCE.sendMessage(BhMsg.REPLACE_NODE_VIEW, new MsgData(newNodeView), oldNode);
		userOpeCmd.pushCmdOfReplaceNodeView(oldNode, newNode, hasParent);
	}

	/**
	 * ノードビューをGUIツリーから取り除く
	 * @param node このノードのノードビューをGUIツリーから取り除く
	 * */
	public void removeFromGUITree(BhNode node) {
		MsgTransporter.INSTANCE.sendMessage(BhMsg.REMOVE_FROM_GUI_TREE, node);
	}

	/**
	 * 4分木空間にノードの領域を登録する
	 * @param node このノードの領域をワークスペースの4分木空間に登録する
	 * @param ws このワークスペースが持つ4分木空間にノードの領域を登録する
	 * @param userOpeCmd undo用コマンドオブジェクト
	 * */
	public void addQTRectangle(BhNode node, Workspace ws, UserOperationCommand userOpeCmd) {

		MsgTransporter.INSTANCE.sendMessage(BhMsg.ADD_QT_RECTANGLE, node, ws);	//4分木ノード登録(重複登録はされない)
		userOpeCmd.pushCmdOfAddQtRectangle(node, ws);
	}

	/**
	 * 4分木空間からノードの領域を削除する
	 * @param node このノードの領域をワークスペースの4分木空間から削除する
	 * @param userOpeCmd undo用コマンドオブジェクト
	 * */
	public void removeQTRectablge(BhNode node, UserOperationCommand userOpeCmd) {

		MsgTransporter.INSTANCE.sendMessage(BhMsg.REMOVE_QT_RECTANGLE, node);
		userOpeCmd.pushCmdOfRemoveQtRectangle(node, node.getWorkspace());
	}

	/**
	 * ワークスペースにルートノードを追加する
	 * @param rootNode 追加するルートノード
	 * @param ws rootNode を追加するワークスペース
	 * @param userOpeCmd undo用コマンドオブジェクト
	 * */
	public void addRootNode(BhNode rootNode, Workspace ws, UserOperationCommand userOpeCmd) {

		MsgTransporter.INSTANCE.sendMessage(BhMsg.ADD_ROOT_NODE, rootNode, ws);
		userOpeCmd.pushCmdOfAddRootNode(rootNode);
	}

	/**
	 * ワークスペースからルートノードを削除する
	 * @param rootNode 削除するルートノード
	 * @param userOpeCmd undo用コマンドオブジェクト
	 * */
	public void removeRootNode(BhNode rootNode, UserOperationCommand userOpeCmd) {

		Workspace ws = rootNode.getWorkspace();
		MsgTransporter.INSTANCE.sendMessage(BhMsg.REMOVE_ROOT_NODE, rootNode, ws);
		userOpeCmd.pushCmdOfRemoveRootNode(rootNode, ws);
	}

	/**
	 * ノードビューの選択表示を切り替える
	 * @param node このノードのノードビューの選択表示を切り替える
	 * @param enable 選択表示を有効にする場合 true. 無効にする場合 false.
	 * */
	public void selectNodeView(BhNode node, boolean enable) {
		MsgTransporter.INSTANCE.sendMessage(BhMsg.SELECT_NODE_VIEW, new MsgData(enable), node);
	}


	/**
	 * orgNode のイミテーションノードの強調表示を切り替える
	 * @param orgNode このノードのイミテーションノードの強調表示を切り替える
	 * @param enable 強調表示を有効にする場合 true.  無効にする場合 false.
	 * */
	public void hilightImit(BhNode orgNode, boolean enable) {

		if (orgNode instanceof Imitatable) {
			Collection<? extends Imitatable> imitationList = ((Imitatable)orgNode).getImitationList();
			imitationList.forEach(
				imitation -> switchPseudoClassActivation(imitation, BhParams.CSS.PSEUDO_HIGHLIGHT_IMIT, enable));
		}
	}

	/**
	 * orgNode の外見に適用されるの疑似クラスの有効/無効を切り替える
	 * @param node 疑似クラスの有効/無効を切り替えるノード
	 * @param pseudoClassName 有効/無効を切り替える疑似クラス
	 * @param enable 疑似クラスを有効にする場合 true.  無効にする場合 false.
	 * */
	public void switchPseudoClassActivation(BhNode node, String pseudoClassName, boolean enable) {

		MsgTransporter.INSTANCE.sendMessage(
			BhMsg.SWITCH_PSEUDO_CLASS_ACTIVATION,
			new MsgData(enable, pseudoClassName),
			node);
	}

	/**
	 * 引数で指定したワークスペースの描画サイズを取得する
	 * @param ws このワークスペースの描画サイズを取得する
	 * @return 引数で指定したワークスペースの描画サイズ
	 */
	public Vec2D getWorkspaceSize(Workspace ws) {
		return MsgTransporter.INSTANCE.sendMessage(BhMsg.GET_WORKSPACE_SIZE, ws).vec2d;
	}

	/**
	 * 引数で指定した TextNode のビューのテキストを取得する
	 * @param node ビューのテキストを取得するノード
	 * @param node に対応するビューのテキスト
	 */
	public String getViewText(TextNode node) {
		return MsgTransporter.INSTANCE.sendMessage(BhMsg.GET_VIEW_TEXT, node).text;
	}

	/**
	 * テキストノードとそのビューにテキストを模倣させる
	 * @param node このノードにテキストを模倣させる
	 * @param modelText モデルに模倣させるテキスト
	 * @param viewText ビューに模倣させるテキスト
	 */
	public void imitateText(TextNode node, String modelText, String viewText) {
		MsgTransporter.INSTANCE.sendMessage(BhMsg.IMITATE_TEXT, new MsgData(modelText, viewText), node);
	}

	/**
	 * Scene 上の位置を引数で指定したワークスペース上の位置に変換する
	 * @param scenePosX Scene 上の X 位置
	 * @param scenePosY Scene 上の Y 位置
	 * @param ws scenePosX と scenePosY をこのワークスペース上の位置に変換する
	 * @return scenePosX と scenePosY のワークスペース上の位置
	 */
	public Vec2D sceneToWorkspace(double scenePosX, double scenePosY, Workspace ws) {

		return MsgTransporter.INSTANCE.sendMessage(
			BhMsg.SCENE_TO_WORKSPACE,
			new MsgData(new Vec2D(scenePosX, scenePosY)),
			ws).vec2d;
	}

	/**
	 * 引数で指定した BhNode に対応する BhNodeView を取得する
	 * @param node このノードに対応するビューを取得する
	 * @return node に対応する BhNodeView
	 */
	public BhNodeView getBhNodeView(BhNode node) {
		return MsgTransporter.INSTANCE.sendMessage(BhMsg.GET_VIEW, node).nodeView;
	}

	/**
	 * 引数で指定したノードをワークスペース中央に表示する
	 * @param node 中央に表示するノード
	 */
	public void lookAt(BhNode node) {

		BhNodeView nodeView = getBhNodeView(node);
		MsgTransporter.INSTANCE.sendMessage(BhMsg.LOOK_AT_NODE_VIEW, new MsgData(nodeView), node.getWorkspace());
	}
}

























