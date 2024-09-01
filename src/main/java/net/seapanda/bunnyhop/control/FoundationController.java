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

package net.seapanda.bunnyhop.control;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
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
import net.seapanda.bunnyhop.compiler.ScriptIdentifiers;
import net.seapanda.bunnyhop.control.nodeselection.BhNodeCategoryListController;
import net.seapanda.bunnyhop.control.workspace.WorkspaceSetController;
import net.seapanda.bunnyhop.message.MsgService;
import net.seapanda.bunnyhop.model.nodeselection.BhNodeCategoryList;
import net.seapanda.bunnyhop.model.workspace.WorkspaceSet;

/**
 * GUIの基底部分のコントローラ.
 *
 * @author K.Koike
 */
public class FoundationController {

  //View
  @FXML VBox foundationVbox;
  @FXML SplitPane horizontalSplitter;

  //Controller
  @FXML private MenuOperationController menuOperationController;
  @FXML private WorkspaceSetController workspaceSetController;
  @FXML private BhNodeCategoryListController nodeCategoryListController;
  @FXML private MenuBarController menuBarController;

  /** 押下状態のキー. */
  private Set<KeyCode> pressedKey = new HashSet<>();
  private Map<KeyCode, BhProgramData.Event> keyCodeToKeyEvent =
      new HashMap<KeyCode, BhProgramData.Event>() {{
          put(KeyCode.DIGIT0, BhProgramData.Event.KEY_DIGIT0_PRESSED);
          put(KeyCode.DIGIT1, BhProgramData.Event.KEY_DIGIT1_PRESSED);
          put(KeyCode.DIGIT2, BhProgramData.Event.KEY_DIGIT2_PRESSED);
          put(KeyCode.DIGIT3, BhProgramData.Event.KEY_DIGIT3_PRESSED);
          put(KeyCode.DIGIT4, BhProgramData.Event.KEY_DIGIT4_PRESSED);
          put(KeyCode.DIGIT5, BhProgramData.Event.KEY_DIGIT5_PRESSED);
          put(KeyCode.DIGIT6, BhProgramData.Event.KEY_DIGIT6_PRESSED);
          put(KeyCode.DIGIT7, BhProgramData.Event.KEY_DIGIT7_PRESSED);
          put(KeyCode.DIGIT8, BhProgramData.Event.KEY_DIGIT8_PRESSED);
          put(KeyCode.DIGIT9, BhProgramData.Event.KEY_DIGIT9_PRESSED);
          put(KeyCode.UP, BhProgramData.Event.KEY_UP_PRESSED);
          put(KeyCode.DOWN, BhProgramData.Event.KEY_DOWN_PRESSED);
          put(KeyCode.RIGHT, BhProgramData.Event.KEY_RIGHT_PRESSED);
          put(KeyCode.LEFT, BhProgramData.Event.KEY_LEFT_PRESSED);
          put(KeyCode.SHIFT, BhProgramData.Event.KEY_SHIFT_PRESSED);
          put(KeyCode.CONTROL, BhProgramData.Event.KEY_CTRL_PRESSED);
          put(KeyCode.SPACE, BhProgramData.Event.KEY_SPACE_PRESSED);
          put(KeyCode.ENTER, BhProgramData.Event.KEY_ENTER_PRESSED);
          put(KeyCode.A, BhProgramData.Event.KEY_A_PRESSED);
          put(KeyCode.B, BhProgramData.Event.KEY_B_PRESSED);
          put(KeyCode.C, BhProgramData.Event.KEY_C_PRESSED);
          put(KeyCode.D, BhProgramData.Event.KEY_D_PRESSED);
          put(KeyCode.E, BhProgramData.Event.KEY_E_PRESSED);
          put(KeyCode.F, BhProgramData.Event.KEY_F_PRESSED);
          put(KeyCode.G, BhProgramData.Event.KEY_G_PRESSED);
          put(KeyCode.H, BhProgramData.Event.KEY_H_PRESSED);
          put(KeyCode.I, BhProgramData.Event.KEY_I_PRESSED);
          put(KeyCode.J, BhProgramData.Event.KEY_J_PRESSED);
          put(KeyCode.K, BhProgramData.Event.KEY_K_PRESSED);
          put(KeyCode.L, BhProgramData.Event.KEY_L_PRESSED);
          put(KeyCode.M, BhProgramData.Event.KEY_M_PRESSED);
          put(KeyCode.N, BhProgramData.Event.KEY_N_PRESSED);
          put(KeyCode.O, BhProgramData.Event.KEY_O_PRESSED);
          put(KeyCode.P, BhProgramData.Event.KEY_P_PRESSED);
          put(KeyCode.Q, BhProgramData.Event.KEY_Q_PRESSED);
          put(KeyCode.R, BhProgramData.Event.KEY_R_PRESSED);
          put(KeyCode.S, BhProgramData.Event.KEY_S_PRESSED);
          put(KeyCode.T, BhProgramData.Event.KEY_T_PRESSED);
          put(KeyCode.U, BhProgramData.Event.KEY_U_PRESSED);
          put(KeyCode.V, BhProgramData.Event.KEY_V_PRESSED);
          put(KeyCode.W, BhProgramData.Event.KEY_W_PRESSED);
          put(KeyCode.X, BhProgramData.Event.KEY_X_PRESSED);
          put(KeyCode.Y, BhProgramData.Event.KEY_Y_PRESSED);
          put(KeyCode.Z, BhProgramData.Event.KEY_Z_PRESSED);
        }};


  /**
   * 初期化する.
   *
   * @param wss ワークスペースセットのモデル
   * @param nodeCategoryList ノードカテゴリリストのモデル
   */
  public boolean init(WorkspaceSet wss, BhNodeCategoryList nodeCategoryList) {
    workspaceSetController.init(wss);
    boolean success = nodeCategoryListController.init(nodeCategoryList);
    if (!success) {
      return false;
    }
    menuOperationController.init(wss, workspaceSetController.getTabPane());
    menuBarController.init(wss);
    wss.setMsgProcessor(workspaceSetController);
    nodeCategoryListController.getView().getSelectionViewList().forEach(
        view -> MsgService.INSTANCE.addNodeSelectionView(wss, view));
    nodeCategoryList.setMsgProcessor(nodeCategoryListController);
    setKeyEvents();
    return true;
  }

  public MenuBarController getMenuBarController() {
    return menuBarController;
  }

  /** キーボード押下時のイベントを登録する. */
  private void setKeyEvents() {
    foundationVbox.addEventFilter(KeyEvent.ANY, event -> {
      if (isKeyEventToForward(event)) {
        event.consume();
        forwardKeyEvent(event);
      }
    });
    foundationVbox.setOnKeyPressed(event -> {
      fireBhOpEvent(event);
      sendKeyEventToBhProgram(event);
      event.consume();
    });
    foundationVbox.setOnKeyReleased(event ->  pressedKey.remove(event.getCode()));
  }

  /** 基底ペインに転送すべきキーイベントかどうかを判断する. */
  private boolean isKeyEventToForward(KeyEvent event) {
    EventTarget target = event.getTarget();
    // タブペインが矢印キーで切り替わらないようにする
    if (target == workspaceSetController.getTabPane()) {
      return true;
    }
    // スクロールペインが矢印やスペースキーでスクロールしないようにする。
    if (target instanceof ScrollPane) {
      if (((ScrollPane) target).getId().equals(BhParams.Fxml.ID_WS_SCROLL_PANE)) {
        return true;
      }
    }
    // ボタンが矢印やスぺーキーイベントを受け付けないようにする
    if (event.getTarget() instanceof ButtonBase) {
      return true;
    }
    return false;
  }

  /** 引数で指定したイベントを基底ペインに転送する. */
  private void forwardKeyEvent(KeyEvent event) {
    foundationVbox.fireEvent(
        new KeyEvent(
            foundationVbox,
            foundationVbox,
            event.getEventType(),
            event.getCharacter(),
            event.getText(),
            event.getCode(),
            event.isShiftDown(),
            event.isControlDown(),
            event.isAltDown(),
            event.isMetaDown()));
  }

  /** BunnyHop操作のためのイベントを発行する. */
  private void fireBhOpEvent(KeyEvent event) {
    switch (event.getCode()) {
      case C:
        if (event.isControlDown()) {
          menuOperationController.fireEvent(MenuOperationController.MenuOperation.COPY);
        }
        break;

      case X:
        if (event.isControlDown()) {
          menuOperationController.fireEvent(MenuOperationController.MenuOperation.CUT);
        }
        break;

      case V:
        if (event.isControlDown()) {
          menuOperationController.fireEvent(MenuOperationController.MenuOperation.PASTE);
        }
        break;

      case Z:
        if (event.isControlDown()) {
          menuOperationController.fireEvent(MenuOperationController.MenuOperation.UNDO);
        }
        break;

      case Y:
        if (event.isControlDown()) {
          menuOperationController.fireEvent(MenuOperationController.MenuOperation.REDO);
        }
        break;

      case S:
        if (event.isControlDown()) {
          menuBarController.fireEvent(MenuBarController.MenuBarItem.SAVE);
        }
        break;

      case F11:
        menuBarController.fireEvent(MenuBarController.MenuBarItem.FREE_MEMORY);
        break;

      case F12:
        menuBarController.fireEvent(MenuBarController.MenuBarItem.SAVE_AS);
        break;

      case DELETE:
        menuOperationController.fireEvent(MenuOperationController.MenuOperation.DELETE);
        break;

      default:
    }
  }

  /** BhProgram にキーイベントを送信する. */
  private void sendKeyEventToBhProgram(KeyEvent event) {
    // 押下済みのキーのイベントは送信しない
    if (pressedKey.contains(event.getCode())) {
      return;
    }

    BhProgramData.Event bhEvent = keyCodeToKeyEvent.get(event.getCode());
    if (bhEvent != null) {
      pressedKey.add(event.getCode());
      var sendData = new BhProgramData(bhEvent, ScriptIdentifiers.Funcs.GET_EVENT_HANDLER_NAMES);

      if (menuOperationController.isLocalHost()) {
        LocalBhProgramManager.INSTANCE.sendAsync(sendData);
      } else {
        RemoteBhProgramManager.INSTANCE.sendAsync(sendData);
      }
    }
  }
}




























