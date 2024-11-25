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

import java.util.HashSet;
import java.util.Set;
import javafx.event.EventTarget;
import javafx.fxml.FXML;
import javafx.scene.control.ButtonBase;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.SplitPane;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.VBox;
import net.seapanda.bunnyhop.bhprogram.BhProgramService;
import net.seapanda.bunnyhop.bhprogram.common.message.BhProgramEvent;
import net.seapanda.bunnyhop.common.constant.BhConstants;
import net.seapanda.bunnyhop.compiler.ScriptIdentifiers;
import net.seapanda.bunnyhop.control.nodeselection.BhNodeCategoryListController;
import net.seapanda.bunnyhop.control.workspace.WorkspaceSetController;
import net.seapanda.bunnyhop.message.MsgService;
import net.seapanda.bunnyhop.model.nodeselection.BhNodeCategoryList;
import net.seapanda.bunnyhop.model.workspace.WorkspaceSet;
import net.seapanda.bunnyhop.service.KeyCodeConverter;

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
      sendKeyEventToBhProgram(event.getCode());
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
      if (((ScrollPane) target).getId().equals(BhConstants.Fxml.ID_WS_SCROLL_PANE)) {
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
  private void sendKeyEventToBhProgram(KeyCode keyCode) {
    // 押下済みのキーのイベントは送信しない
    if (pressedKey.contains(keyCode)) {
      return;
    }
    var eventName = KeyCodeConverter.toBhProgramEventName(keyCode).orElse(null);
    if (eventName == null) {
      return;
    }
    pressedKey.add(keyCode);
    var bhEvent = new BhProgramEvent(eventName, ScriptIdentifiers.Funcs.GET_EVENT_HANDLER_NAMES);
    if (menuOperationController.isLocalHost()) {
      BhProgramService.local().sendAsync(bhEvent);
    } else {
      BhProgramService.remote().sendAsync(bhEvent);
    }
  }

  /** 現在選択されている BhProgram の実行環境がローカルかリモートか調べる. */
  public boolean isBhRuntimeLocal() {
    return menuOperationController.isLocalHost();
  }
}
