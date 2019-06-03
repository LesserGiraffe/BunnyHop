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
package net.seapanda.bunnyhop.control;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedList;
import java.util.List;
import java.util.stream.Collectors;

import net.seapanda.bunnyhop.common.Vec2D;
import net.seapanda.bunnyhop.common.tools.MsgPrinter;
import net.seapanda.bunnyhop.message.BhMsg;
import net.seapanda.bunnyhop.message.MsgData;
import net.seapanda.bunnyhop.message.MsgProcessor;
import net.seapanda.bunnyhop.model.Workspace;
import net.seapanda.bunnyhop.model.node.BhNode;
import net.seapanda.bunnyhop.model.node.CauseOfDletion;
import net.seapanda.bunnyhop.modelhandler.BhNodeHandler;
import net.seapanda.bunnyhop.modelhandler.DelayedDeleter;
import net.seapanda.bunnyhop.quadtree.QuadTreeManager;
import net.seapanda.bunnyhop.quadtree.QuadTreeRectangle;
import net.seapanda.bunnyhop.quadtree.QuadTreeRectangle.OVERLAP_OPTION;
import net.seapanda.bunnyhop.root.BunnyHop;
import net.seapanda.bunnyhop.undo.UserOperationCommand;
import net.seapanda.bunnyhop.view.MultiNodeShifterView;
import net.seapanda.bunnyhop.view.ViewHelper;
import net.seapanda.bunnyhop.view.WorkspaceView;
import net.seapanda.bunnyhop.view.node.BhNodeView;


/**
 * ワークスペースとそれに関連するビューのコントローラ
 * @author K.Koike
 */
public class WorkspaceController implements MsgProcessor {

	private Workspace model; // 操作対象のモデル
	private WorkspaceView view;
	private MultiNodeShifterController nodeShifterController;

	public WorkspaceController () {}

	/**
	 * コンストラクタ
	 * @param model コントローラが操作するモデル
	 * @param view コントローラが操作するビュー
	 */
	public WorkspaceController(Workspace model, WorkspaceView view) {
		this.model = model;
		this.view = view;
	}

	/**
	 * このコントローラを初期化する
	 * */
	public boolean init() {

		var multiNodeShifterView = new MultiNodeShifterView();
		boolean success = multiNodeShifterView.init();
		if (success) {
			view.addtMultiNodeShifterView(multiNodeShifterView);
			nodeShifterController = new MultiNodeShifterController(multiNodeShifterView, model);
			nodeShifterController.init();
		}
		setMouseEventHandlers();
		return success;
	}

	/**
	 * マウスイベント関連のハンドラを登録する.
	 * */
	private void setMouseEventHandlers() {

		Vec2D mousePressedPos = new Vec2D(0.0, 0.0);

		// WSをクリックしたときにテキストフィールドのカーソルが消えなくなるので, マウスイベントをconsumeしない.

		// マウスボタン押下
		view.setOnMousePressedHandler(
			mouseEvent -> {
				if (!mouseEvent.isShiftDown()) {
					UserOperationCommand userOpeCmd = new UserOperationCommand();
					BunnyHop.INSTANCE.hideTemplatePanel();
					model.clearSelectedNodeList(userOpeCmd);
					BunnyHop.INSTANCE.pushUserOpeCmd(userOpeCmd);
				}
				ViewHelper.INSTANCE.deleteShadow(model);
				mousePressedPos.x = mouseEvent.getX();
				mousePressedPos.y = mouseEvent.getY();
				view.showSelectionRectangle(mousePressedPos, mousePressedPos);
			});

		// マウスドラッグ
		view.setOnMouseDraggedHandler(
			mouseEvent -> {
				view.showSelectionRectangle(mousePressedPos, new Vec2D(mouseEvent.getX(), mouseEvent.getY()));
			});

		// マウスボタン離し
		view.setOnMouseReleasedHandler(
			mouseEvent -> {
				view.hideSelectionRectangle();
				var selectionRange = new QuadTreeRectangle(
					Math.min(mousePressedPos.x, mouseEvent.getX()), Math.min(mousePressedPos.y, mouseEvent.getY()),
					Math.max(mousePressedPos.x, mouseEvent.getX()), Math.max(mousePressedPos.y, mouseEvent.getY()),
					null);

				List<BhNodeView> containedNodes =
					view.searchForOverlappedNodeViews(selectionRange, true, OVERLAP_OPTION.CONTAIN).stream()
					.filter(nodeView -> nodeView.getModel().isMovable() && !nodeView.getModel().isSelected())
					.collect(Collectors.toCollection(ArrayList::new));

				// 面積の大きい順にソート
				containedNodes.sort(this::compareViewSize);

				var userOpeCmd = new UserOperationCommand();
				selectNodes(containedNodes, userOpeCmd);
				BunnyHop.INSTANCE.pushUserOpeCmd(userOpeCmd);
			});
	}

	private int compareViewSize(BhNodeView viewA, BhNodeView viewB) {

		Vec2D sizeA = viewA.getRegionManager().getBodySize(false);
		double areaA = sizeA.x * sizeA.y;
		Vec2D sizeB = viewB.getRegionManager().getBodySize(false);
		double areaB = sizeB.x * sizeB.y;

		if (areaA < areaB)
			return 1;
		else if (areaA > areaB)
			return -1;

		return 0;
	}

	/**
	 * 矩形選択するノードを選び出す
	 * @param candidates 矩形選択される候補ノード
	 * @param userOpeCmd undo 用コマンドオブジェクト
	 * */
	private void selectNodes(List<BhNodeView> candidates, UserOperationCommand userOpeCmd) {

		// 親ノードが選択候補でかつ, 親ノードのボディの領域に包含されているノードは選択対象としない.
		LinkedList<BhNodeView> nodesToSelect = new LinkedList<>(candidates);
		while (nodesToSelect.size() != 0) {
			BhNodeView larger = nodesToSelect.pop();
			model.addSelectedNode(larger.getModel(), userOpeCmd);	// ノード選択
			var iter = nodesToSelect.iterator();
			while (iter.hasNext()) {
				BhNodeView smaller = iter.next();
				// 子孫 - 先祖関係にあってかつ領域が包含関係にある -> 矩形選択の対象としない
				if (larger.getRegionManager().overlapsWith(smaller, OVERLAP_OPTION.CONTAIN) &&
					smaller.getModel().isDescendantOf(larger.getModel())) {
					iter.remove();
				}
			}
		}
	}

	/**
	 * メッセージ受信
	 * @param msg メッセージの種類
	 * @param data メッセージの種類に応じて処理するもの
	 * */
	@Override
	public MsgData processMsg(BhMsg msg, MsgData data) {

		switch (msg) {

			case ADD_ROOT_NODE:
				model.addRootNode(data.node);
				view.addNodeView(data.nodeView);
				nodeShifterController.updateMultiNodeShifter(data.node);
				break;

			case REMOVE_ROOT_NODE:
				model.removeRootNode(data.node);
				view.removeNodeView(data.nodeView);
				nodeShifterController.updateMultiNodeShifter(data.node);
				break;

			case ADD_QT_RECTANGLE:
				view.addRectangleToQTSpace(data.nodeView);
				break;

			case CHANGE_WORKSPACE_VIEW_SIZE:
				view.changeWorkspaceViewSize(data.bool);
				break;

			case SCENE_TO_WORKSPACE:
				javafx.geometry.Point2D pos = view.sceneToWorkspace(data.vec2d.x, data.vec2d.y);
				return new MsgData(new Vec2D(pos.getX(), pos.getY()));

			case ZOOM:
				view.zoom(data.bool);
				break;

			case GET_WORKSPACE_SIZE:
				Vec2D size = view.getWorkspaceSize();
				return new MsgData(new Vec2D(size.x, size.y));

			case ADD_WORKSPACE:
				return new MsgData(model, view, data.userOpeCmd);

			case DELETE_WORKSPACE:
				return deleteWorkspace(data);

			case UPDATE_MULTI_NODE_SHIFTER:
				nodeShifterController.updateMultiNodeShifter(data.node);
				break;

			default:
				throw new AssertionError("receive an unknown msg " + msg);
		}

		return null;
	};

	private MsgData deleteWorkspace(MsgData data) {

		Collection<BhNode> rootNodes = model.getRootNodeList();
		rootNodes.forEach(node ->
			node.execScriptOnDeletionRequested(rootNodes, CauseOfDletion.WORKSPACE_DELETION, data.userOpeCmd));
		BhNodeHandler.INSTANCE.deleteNodes(model.getRootNodeList(), data.userOpeCmd);
		return new MsgData(model, view, data.userOpeCmd);
	}

	//デバッグ用
	private void printDebugInfo() {

		//4分木登録ノード数表示
		Class<WorkspaceView> c = WorkspaceView.class;
		Field f = null;
		try {
			f = c.getDeclaredField("quadTreeMngForConnector");
			f.setAccessible(true);
			QuadTreeManager quadTreeMngForConnector = (QuadTreeManager)f.get(view);
			MsgPrinter.INSTANCE.msgForDebug("num of QuadTreeNodes " + quadTreeMngForConnector.calcRegisteredNodeNum());
		} catch (IllegalAccessException | IllegalArgumentException | NoSuchFieldException | SecurityException e) {
			MsgPrinter.INSTANCE.errMsgForDebug(e.toString());
		}

		MsgPrinter.INSTANCE.msgForDebug("num of root nodes " + model.getRootNodeList().size());
		MsgPrinter.INSTANCE.msgForDebug("num of deletion candidates " + DelayedDeleter.INSTANCE.getDeletionCadidateList().size());
		MsgPrinter.INSTANCE.msgForDebug("num of selected nodes " + model.getSelectedNodeList().size() + "\n");
	}
}












