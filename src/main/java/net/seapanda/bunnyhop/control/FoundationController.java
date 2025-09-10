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
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.Pane;
import javafx.scene.layout.VBox;
import net.seapanda.bunnyhop.bhprogram.LocalBhProgramLauncher;
import net.seapanda.bunnyhop.bhprogram.RemoteBhProgramController;
import net.seapanda.bunnyhop.bhprogram.common.message.BhProgramEvent;
import net.seapanda.bunnyhop.bhprogram.debugger.Debugger;
import net.seapanda.bunnyhop.common.BhConstants;
import net.seapanda.bunnyhop.compiler.ScriptIdentifiers;
import net.seapanda.bunnyhop.control.debugger.DebugWindowController;
import net.seapanda.bunnyhop.control.nodeselection.BhNodeCategoryListController;
import net.seapanda.bunnyhop.control.workspace.WorkspaceSetController;
import net.seapanda.bunnyhop.export.ProjectExporter;
import net.seapanda.bunnyhop.export.ProjectImporter;
import net.seapanda.bunnyhop.model.ModelAccessNotificationService;
import net.seapanda.bunnyhop.model.factory.WorkspaceFactory;
import net.seapanda.bunnyhop.model.workspace.CopyAndPaste;
import net.seapanda.bunnyhop.model.workspace.CutAndPaste;
import net.seapanda.bunnyhop.model.workspace.WorkspaceSet;
import net.seapanda.bunnyhop.service.KeyCodeConverter;
import net.seapanda.bunnyhop.service.MessageService;
import net.seapanda.bunnyhop.undo.UndoRedoAgent;
import net.seapanda.bunnyhop.view.factory.DebugViewFactory;
import net.seapanda.bunnyhop.view.nodeselection.BhNodeCategoryProvider;
import net.seapanda.bunnyhop.view.nodeselection.BhNodeSelectionViewProxy;

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
  @FXML private BhNodeCategoryListController nodeCategoryListController;
  @FXML private MenuBarController menuBarController;
  @FXML private NotificationViewController notifViewController;

  /** 押下状態のキー. */
  private final Set<KeyCode> pressedKey = new HashSet<>();
  private LocalBhProgramLauncher localCtrl;
  private RemoteBhProgramController remoteCtrl;

  /** 初期化する. */
  public boolean initialize(
      WorkspaceSet wss,
      BhNodeCategoryProvider nodeCategoryProvider,
      ModelAccessNotificationService notifService,
      WorkspaceFactory wsFactory,
      DebugViewFactory debugViewFactory,
      UndoRedoAgent undoRedoAgent,
      BhNodeSelectionViewProxy proxy,
      LocalBhProgramLauncher localCtrl,
      RemoteBhProgramController remoteCtrl,
      ProjectImporter importer,
      ProjectExporter exporter,
      CopyAndPaste copyAndPaste,
      CutAndPaste cutAndPaste,
      MessageService msgService,
      Debugger debugger,
      DebugWindowController debugWindowCtrl) {
    this.localCtrl = localCtrl;
    this.remoteCtrl = remoteCtrl;
    workspaceSetController.initialize(wss);
    boolean success = nodeCategoryListController.initialize(nodeCategoryProvider, proxy);
    success &= menuViewController.initialize(
        workspaceSetController,
        notifService,
        wsFactory,
        undoRedoAgent,
        proxy,
        localCtrl,
        remoteCtrl,
        copyAndPaste,
        cutAndPaste,
        msgService,
        debugWindowCtrl);
    menuBarController.initialize(wss, notifService, undoRedoAgent, importer, exporter, msgService);
    success &= notifViewController.initialize(wss, debugger, debugViewFactory, notifService);
    setKeyEvents();
    return success;
  }

  public MenuBarController getMenuBarController() {
    return menuBarController;
  }

  public NotificationViewController getNotificationViewController() {
    return notifViewController;
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
    if (menuViewController.isLocalHost()) {
      localCtrl.getBhRuntimeCtrl().send(bhEvent);
    } else {
      remoteCtrl.getBhRuntimeCtrl().send(bhEvent);
    }
  }

  /** 現在選択されている BhProgram の実行環境がローカルかリモートか調べる. */
  public boolean isBhRuntimeLocal() {
    return menuViewController.isLocalHost();
  }

  /** ワークスペースセットのコントローラを返す. */
  public WorkspaceSetController getWorkspaceSetController() {
    return workspaceSetController;
  }
}
