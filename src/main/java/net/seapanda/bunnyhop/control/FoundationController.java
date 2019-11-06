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

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;

import javafx.event.EventTarget;
import javafx.fxml.FXML;
import javafx.scene.control.ButtonBase;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.SplitPane;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.VBox;
import net.seapanda.bunnyhop.bhprogram.LocalBhProgramManager;
import net.seapanda.bunnyhop.bhprogram.RemoteBhProgramManager;
import net.seapanda.bunnyhop.bhprogram.common.BhProgramData;
import net.seapanda.bunnyhop.common.constant.BhParams;
import net.seapanda.bunnyhop.compiler.CommonCodeDefinition;
import net.seapanda.bunnyhop.model.WorkspaceSet;
import net.seapanda.bunnyhop.model.node.BhNodeCategoryList;

/**
 * GUIの基底部分のコントローラ
 * @author K.Koike
 */
public class FoundationController {

	//View
	@FXML VBox foundationVBox;
	@FXML SplitPane horizontalSplitter;

	//Controller
	@FXML private MenuOperationController menuOperationController;
	@FXML private WorkspaceSetController workspaceSetController;
	@FXML private BhNodeCategoryListController nodeCategoryListController;
	@FXML private MenuBarController menuBarController;

	private Set<KeyCode> pressedKey = new HashSet<>(); //!< 押下状態のキー
	private Map<KeyCode, BhProgramData.EVENT> keyCode_keyEvent = new HashMap<KeyCode, BhProgramData.EVENT>() {{
		put(KeyCode.DIGIT0, BhProgramData.EVENT.KEY_DIGIT0_PRESSED);
		put(KeyCode.DIGIT1, BhProgramData.EVENT.KEY_DIGIT1_PRESSED);
		put(KeyCode.DIGIT2, BhProgramData.EVENT.KEY_DIGIT2_PRESSED);
		put(KeyCode.DIGIT3, BhProgramData.EVENT.KEY_DIGIT3_PRESSED);
		put(KeyCode.DIGIT4, BhProgramData.EVENT.KEY_DIGIT4_PRESSED);
		put(KeyCode.DIGIT5, BhProgramData.EVENT.KEY_DIGIT5_PRESSED);
		put(KeyCode.DIGIT6, BhProgramData.EVENT.KEY_DIGIT6_PRESSED);
		put(KeyCode.DIGIT7, BhProgramData.EVENT.KEY_DIGIT7_PRESSED);
		put(KeyCode.DIGIT8, BhProgramData.EVENT.KEY_DIGIT8_PRESSED);
		put(KeyCode.DIGIT9, BhProgramData.EVENT.KEY_DIGIT9_PRESSED);
		put(KeyCode.UP, BhProgramData.EVENT.KEY_UP_PRESSED);
		put(KeyCode.DOWN, BhProgramData.EVENT.KEY_DOWN_PRESSED);
		put(KeyCode.RIGHT, BhProgramData.EVENT.KEY_RIGHT_PRESSED);
		put(KeyCode.LEFT, BhProgramData.EVENT.KEY_LEFT_PRESSED);
		put(KeyCode.SHIFT, BhProgramData.EVENT.KEY_SHIFT_PRESSED);
		put(KeyCode.CONTROL, BhProgramData.EVENT.KEY_CTRL_PRESSED);
		put(KeyCode.SPACE, BhProgramData.EVENT.KEY_SPACE_PRESSED);
		put(KeyCode.ENTER, BhProgramData.EVENT.KEY_ENTER_PRESSED);
		put(KeyCode.A, BhProgramData.EVENT.KEY_A_PRESSED);
		put(KeyCode.B, BhProgramData.EVENT.KEY_B_PRESSED);
		put(KeyCode.C, BhProgramData.EVENT.KEY_C_PRESSED);
		put(KeyCode.D, BhProgramData.EVENT.KEY_D_PRESSED);
		put(KeyCode.E, BhProgramData.EVENT.KEY_E_PRESSED);
		put(KeyCode.F, BhProgramData.EVENT.KEY_F_PRESSED);
		put(KeyCode.G, BhProgramData.EVENT.KEY_G_PRESSED);
		put(KeyCode.H, BhProgramData.EVENT.KEY_H_PRESSED);
		put(KeyCode.I, BhProgramData.EVENT.KEY_I_PRESSED);
		put(KeyCode.J, BhProgramData.EVENT.KEY_J_PRESSED);
		put(KeyCode.K, BhProgramData.EVENT.KEY_K_PRESSED);
		put(KeyCode.L, BhProgramData.EVENT.KEY_L_PRESSED);
		put(KeyCode.M, BhProgramData.EVENT.KEY_M_PRESSED);
		put(KeyCode.N, BhProgramData.EVENT.KEY_N_PRESSED);
		put(KeyCode.O, BhProgramData.EVENT.KEY_O_PRESSED);
		put(KeyCode.P, BhProgramData.EVENT.KEY_P_PRESSED);
		put(KeyCode.Q, BhProgramData.EVENT.KEY_Q_PRESSED);
		put(KeyCode.R, BhProgramData.EVENT.KEY_R_PRESSED);
		put(KeyCode.S, BhProgramData.EVENT.KEY_S_PRESSED);
		put(KeyCode.T, BhProgramData.EVENT.KEY_T_PRESSED);
		put(KeyCode.U, BhProgramData.EVENT.KEY_U_PRESSED);
		put(KeyCode.V, BhProgramData.EVENT.KEY_V_PRESSED);
		put(KeyCode.W, BhProgramData.EVENT.KEY_W_PRESSED);
		put(KeyCode.X, BhProgramData.EVENT.KEY_X_PRESSED);
		put(KeyCode.Y, BhProgramData.EVENT.KEY_Y_PRESSED);
		put(KeyCode.Z, BhProgramData.EVENT.KEY_Z_PRESSED);
	}};	//!< キーコードと送信イベントのマップ


	/**
	 * 初期化する
	 * @param wss ワークスペースセットのモデル
	 * @param nodeCategoryList ノードカテゴリリストのモデル
	 */
	public void init(WorkspaceSet wss, BhNodeCategoryList nodeCategoryList) {

		workspaceSetController.init(wss);
		nodeCategoryListController.init(nodeCategoryList);
		menuOperationController.init(
			wss,
			workspaceSetController.getTabPane(),
			nodeCategoryListController.getView());
		menuBarController.init(wss);

		wss.setMsgProcessor(workspaceSetController);
		nodeCategoryList.setMsgProcessor(nodeCategoryListController);
		setKeyEvents();
	}

	public MenuBarController getMenuBarController() {
		return menuBarController;
	}

	/**
	 * キーボード押下時のイベントを登録する
	 */
	private void setKeyEvents() {

		Consumer<KeyEvent> forwardEvent = (event) -> {
			foundationVBox.fireEvent(
				new KeyEvent(
					foundationVBox,
					foundationVBox,
					event.getEventType(),
					event.getCharacter(),
					event.getText(),
					event.getCode(),
					event.isShiftDown(),
					event.isControlDown(),
					event.isAltDown(),
					event.isMetaDown()));
		};

		foundationVBox.addEventFilter(KeyEvent.ANY, (event) -> {

			EventTarget target = event.getTarget();
			// タブペインが矢印キーで切り替わらないようにする
			if (target == workspaceSetController.getTabPane()) {
				event.consume();
				forwardEvent.accept(event);
			}
			// スクロールペインが矢印やスペースキーでスクロールしないようにする。
			if (target instanceof ScrollPane) {
				if (((ScrollPane)target).getId().equals(BhParams.Fxml.ID_WS_SCROLL_PANE)) {
					event.consume();
					forwardEvent.accept(event);
				}
			}
			// ボタンが矢印やスぺーキーイベントを受け付けないようにする
			if (event.getTarget() instanceof ButtonBase) {
				event.consume();
				forwardEvent.accept(event);
			}
		});

		foundationVBox.setOnKeyPressed(event -> {
			fireBhOpEvent(event);
			sendKeyEventToBhProgram(event);
			event.consume();
		});

		foundationVBox.setOnKeyReleased(event ->  pressedKey.remove(event.getCode()));
	}

	/**
	 * BunnyHop操作のためのイベントを発行する
	 * */
	private void fireBhOpEvent(KeyEvent event) {
		switch (event.getCode()) {
			case C:
				if (event.isControlDown())
					menuOperationController.fireEvent(MenuOperationController.MENU_OPERATION.COPY);
				break;

			case X:
				if (event.isControlDown())
					menuOperationController.fireEvent(MenuOperationController.MENU_OPERATION.CUT);
				break;

			case V:
				if (event.isControlDown())
					menuOperationController.fireEvent(MenuOperationController.MENU_OPERATION.PASTE);
				break;

			case Z:
				if (event.isControlDown())
					menuOperationController.fireEvent(MenuOperationController.MENU_OPERATION.UNDO);
				break;

			case Y:
				if (event.isControlDown())
					menuOperationController.fireEvent(MenuOperationController.MENU_OPERATION.REDO);
				break;

			case S:
				if (event.isControlDown())
					menuBarController.fireEvent(MenuBarController.MENU_BAR.SAVE);
				break;

			case F11:
				menuBarController.fireEvent(MenuBarController.MENU_BAR.FREE_MEMORY);
				break;

			case F12:
				menuBarController.fireEvent(MenuBarController.MENU_BAR.SAVE_AS);
				break;

			case DELETE:
				menuOperationController.fireEvent(MenuOperationController.MENU_OPERATION.DELETE);
				break;

			default:
		}
	}

	/**
	 * BhProgram にキーイベントを送信する
	 * */
	private void sendKeyEventToBhProgram(KeyEvent event) {

		// 押下済みのキーのイベントは送信しない
		if (pressedKey.contains(event.getCode()))
				return;

		BhProgramData.EVENT bhEvent = keyCode_keyEvent.get(event.getCode());
		if (bhEvent != null) {
			pressedKey.add(event.getCode());
			var sendData = new BhProgramData(
				BhProgramData.TYPE.INPUT_EVENT, bhEvent, CommonCodeDefinition.Funcs.GET_EVENT_HANDLER_NAMES);

			if (menuOperationController.isLocalHost())
				LocalBhProgramManager.INSTANCE.sendAsync(sendData);
			else
				RemoteBhProgramManager.INSTANCE.sendAsync(sendData);
		}
	}
}




























