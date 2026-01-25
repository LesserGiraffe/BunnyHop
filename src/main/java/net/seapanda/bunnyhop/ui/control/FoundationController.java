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

package net.seapanda.bunnyhop.ui.control;

import java.util.HashSet;
import java.util.Set;
import javafx.event.EventTarget;
import javafx.fxml.FXML;
import javafx.scene.control.ButtonBase;
import javafx.scene.control.ScrollPane;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.Pane;
import javafx.scene.layout.VBox;
import net.seapanda.bunnyhop.bhprogram.LocalBhProgramLauncher;
import net.seapanda.bunnyhop.bhprogram.RemoteBhProgramController;
import net.seapanda.bunnyhop.bhprogram.common.message.BhProgramEvent;
import net.seapanda.bunnyhop.bhprogram.runtime.BhRuntimeType;
import net.seapanda.bunnyhop.common.configuration.BhConstants;
import net.seapanda.bunnyhop.common.configuration.BhSettings;
import net.seapanda.bunnyhop.compiler.ScriptIdentifiers;
import net.seapanda.bunnyhop.service.KeyCodeConverter;
import net.seapanda.bunnyhop.ui.model.NodeManipulationMode;
import net.seapanda.bunnyhop.workspace.control.WorkspaceSetController;

/**
 * GUIの基底部分のコントローラ.
 *
 * @author K.Koike
 */
public class FoundationController {

  //View
  @FXML private VBox foundationVbox;

  //Controller
  @FXML private MenuViewController menuViewController;
  @FXML private WorkspaceSetController workspaceSetController;
  @FXML private MenuBarController menuBarController;

  /** 押下状態のキー. */
  private final Set<KeyCode> pressedKey = new HashSet<>();
  private final LocalBhProgramLauncher localCtrl;
  private final RemoteBhProgramController remoteCtrl;

  public FoundationController(
      LocalBhProgramLauncher localCtrl, RemoteBhProgramController remoteCtrl) {
    this.localCtrl = localCtrl;
    this.remoteCtrl = remoteCtrl;
  }

  /** このコントローラを初期化する. */
  @FXML
  public void initialize() {
    setEventHandlers();
  }

  /** イベントハンドラを設定する. */
  private void setEventHandlers() {
    foundationVbox.addEventFilter(KeyEvent.ANY, event -> {
      if (shouldForwardKeyEvent(event)) {
        event.consume();
        forwardKeyEvent(event);
      }
      changeNodeManipMode(event);
    });
    foundationVbox.addEventFilter(MouseEvent.MOUSE_PRESSED, event -> {
      if (!isNodeManipModeKeyPressed(event)) {
        setNodeManipMode(NodeManipulationMode.MODE_0);
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
  private boolean shouldForwardKeyEvent(KeyEvent event) {
    EventTarget target = event.getTarget();
    if (target instanceof Pane pane) {
      if (pane.getId().equals(BhConstants.UiId.WS_PANE)) {
        return true;
      }
    }
    // タブペインが矢印キーで切り替わらないようにする
    if (target == workspaceSetController.getTabPane()) {
      return true;
    }
    // スクロールペインが矢印やスペースキーでスクロールしないようにする。
    if (target instanceof ScrollPane scrollPane) {
      if (scrollPane.getId().equals(BhConstants.UiId.WS_SCROLL_PANE)) {
        return true;
      }
    }
    // ボタンが矢印やスぺーキーイベントを受け付けないようにする
    return target instanceof ButtonBase;
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
          menuViewController.fireEvent(MenuViewController.MenuOperation.COPY);
        }
        break;

      case X:
        if (event.isControlDown()) {
          menuViewController.fireEvent(MenuViewController.MenuOperation.CUT);
        }
        break;

      case V:
        if (event.isControlDown()) {
          menuViewController.fireEvent(MenuViewController.MenuOperation.PASTE);
        }
        break;

      case Z:
        if (event.isControlDown()) {
          menuViewController.fireEvent(MenuViewController.MenuOperation.UNDO);
        }
        break;

      case Y:
        if (event.isControlDown()) {
          menuViewController.fireEvent(MenuViewController.MenuOperation.REDO);
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
        menuViewController.fireEvent(MenuViewController.MenuOperation.DELETE);
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
    if (BhSettings.BhRuntime.currentBhRuntimeType == BhRuntimeType.LOCAL) {
      localCtrl.getBhRuntimeCtrl().send(bhEvent);
    } else {
      remoteCtrl.getBhRuntimeCtrl().send(bhEvent);
    }
  }

  /** ノードの操作モードを変更する. */
  private void changeNodeManipMode(KeyEvent event) {
    if (event.getEventType() == KeyEvent.KEY_PRESSED) {
      switch (event.getCode()) {
        case SHIFT -> setNodeManipMode(NodeManipulationMode.MODE_1);
        case CONTROL -> setNodeManipMode(NodeManipulationMode.MODE_2);
        default -> setNodeManipMode(NodeManipulationMode.MODE_0);
      }
    } else if (event.getEventType() == KeyEvent.KEY_RELEASED) {
      setNodeManipMode(NodeManipulationMode.MODE_0);
    }
  }

  /**
   * ノードの操作モードを変更する.
   *
   * @param mode このモードに変更する
   */
  private void setNodeManipMode(NodeManipulationMode mode) {
    BhSettings.Ui.nodeManipMode = mode;
    foundationVbox.getScene().setCursor(mode.getCursor());
  }

  /** ノードの操作モードを変えるためのキーが押されているか調べる. */
  private boolean isNodeManipModeKeyPressed(MouseEvent event) {
    return event.isShiftDown() || event.isControlDown();
  }
}
