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

import static javafx.scene.input.MouseEvent.MOUSE_DRAGGED;

import java.util.HashSet;
import java.util.Optional;
import java.util.Set;
import javafx.event.EventTarget;
import javafx.fxml.FXML;
import javafx.scene.Node;
import javafx.scene.control.ButtonBase;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.SplitPane;
import javafx.scene.control.SplitPane.Divider;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import net.seapanda.bunnyhop.bhruntime.BhRuntimeType;
import net.seapanda.bunnyhop.bhruntime.launch.LocalBhProgramLauncher;
import net.seapanda.bunnyhop.bhruntime.launch.RemoteBhProgramLauncher;
import net.seapanda.bunnyhop.common.configuration.BhConstants;
import net.seapanda.bunnyhop.common.configuration.BhSettings;
import net.seapanda.bunnyhop.runtime.api.message.BhProgramEvent;
import net.seapanda.bunnyhop.service.KeyCodeConverter;
import net.seapanda.bunnyhop.ui.control.MenuBarController.MenuBarItem;
import net.seapanda.bunnyhop.ui.control.MenuViewController.MenuOperation;
import net.seapanda.bunnyhop.ui.model.NodeManipulationMode;
import net.seapanda.bunnyhop.utility.concurrent.reference.ConcurrentBoolean;
import net.seapanda.bunnyhop.utility.reference.WritableBoolean;
import net.seapanda.bunnyhop.utility.reference.WritableDouble;
import net.seapanda.bunnyhop.workspace.control.WorkspaceSetController;
import net.seapanda.bunnyhop.workspace.view.WorkspaceViewPane;

/**
 * GUIの基底部分のコントローラ.
 *
 * @author K.Koike
 */
public class FoundationController {

  //View
  @FXML private VBox foundationVbox;
  @FXML private SplitPane verticalSplitPane;
  @FXML private SplitPane horizontalSplitPane;

  //Controller
  @FXML private MenuViewController menuViewController;
  @FXML private WorkspaceSetController workspaceSetController;
  @FXML private MenuBarController menuBarController;

  /** 押下状態のキー. */
  private final Set<KeyCode> pressedKeys = new HashSet<>();
  private final Stage stage;
  private final LocalBhProgramLauncher localCtrl;
  private final RemoteBhProgramLauncher remoteCtrl;
  private final BhSettings settings;

  /**
   * コンストラクタ.
   *
   * @param stage アプリケーションのステージ
   * @param localCtrl ローカル環境で BhProgram を実行するためのオブジェクト
   * @param remoteCtrl リモート環境で BhProgram を実行するためのオブジェクト
   * @param settings BunnyHop の設定情報
   */
  public FoundationController(
      Stage stage,
      LocalBhProgramLauncher localCtrl,
      RemoteBhProgramLauncher remoteCtrl,
      BhSettings settings) {
    this.stage = stage;
    this.localCtrl = localCtrl;
    this.remoteCtrl = remoteCtrl;
    this.settings = settings;
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

    foundationVbox.setOnKeyReleased(event ->  pressedKeys.remove(event.getCode()));
    stage.focusedProperty().addListener((obs, oldVal, newVal) -> {
      if (!newVal) {
        pressedKeys.clear();
      }
    });

    settings.ui.notificationViewShowed.addOnValueChanged(
        event -> setNotificationViewVisibility(event.newVal()));

    stage.setOnShown(event -> {
      BhSettings.Ui ui = settings.ui;
      initializeSplitPane(
          verticalSplitPane, ui.nodeSelectionSplitPos, new ConcurrentBoolean(false));
      initializeSplitPane(horizontalSplitPane, ui.notificationSplitPos, ui.notificationViewShowed);
    });
  }

  /** 基底ペインに転送すべきキーイベントかどうかを判断する. */
  private boolean shouldForwardKeyEvent(KeyEvent event) {
    EventTarget target = event.getTarget();
    if (target instanceof WorkspaceViewPane) {
      return true;
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

  /**
   * {@code splitPane} の divider の位置を初期化し, ドラッグ時に位置を保存するハンドラを設定する.
   *
   * @param splitPane 初期化対象の {@link SplitPane}
   * @param dividerPos divider の初期位置. およびドラッグ時の保存先.
   * @param viewShowed {@code splitPane} が分割しているビューが表示されているかどうか.
   *                   およびドラッグ時の保存先.
   */
  private void initializeSplitPane(
      SplitPane splitPane, WritableDouble dividerPos, WritableBoolean viewShowed) {
    getDivider(splitPane).ifPresent(
        divider -> divider.addEventHandler(
            MOUSE_DRAGGED, event -> onDividerDragged(splitPane, dividerPos, viewShowed)));

    Divider divider = splitPane.getDividers().getFirst();
    if (!viewShowed.get()) {
      divider.setPosition(1);
    } else if (dividerPos.get() >= 0) {
      divider.setPosition(dividerPos.get());
    }
  }

  /** {@code detDivider} を分割する divider ノードを取得する. */
  private Optional<Node> getDivider(SplitPane splitPane) {
    return splitPane.lookupAll(".split-pane-divider").stream()
            .filter(node -> node.getParent() == splitPane)
            .findFirst();
  }

  /**
   * {@code splitPane} の divider がマウスドラッグされたときに呼ばれる.
   *
   * <p>ドラッグ後の divider の位置を {@code dividerPos} に保存し,
   * {@code viewHidden} を false に設定する.
   *
   * @param splitPane ドラッグされた divider を持つ {@link SplitPane}
   * @param dividerPos divider の位置の保存先
   * @param viewShowed {@code splitPane} が分割しているビューが表示されているかどうかの保存先
   */
  private static void onDividerDragged(
      SplitPane splitPane, WritableDouble dividerPos, WritableBoolean viewShowed) {
    double pos = splitPane.getDividers().getFirst().getPosition();
    // 実際に divider をマウスドラッグしたときだけ, 分割位置を更新する.
    dividerPos.set(pos);
    viewShowed.set(true);
  }

  /** BunnyHop操作のためのイベントを発行する. */
  private void fireBhOpEvent(KeyEvent event) {
    switch (event.getCode()) {
      case C:
        if (event.isControlDown()) {
          menuViewController.fireEvent(MenuOperation.COPY);
        }
        break;

      case X:
        if (event.isControlDown()) {
          menuViewController.fireEvent(MenuOperation.CUT);
        }
        break;

      case V:
        if (event.isControlDown()) {
          menuViewController.fireEvent(MenuOperation.PASTE);
        }
        break;

      case Z:
        if (event.isControlDown()) {
          menuViewController.fireEvent(MenuOperation.UNDO);
        }
        break;

      case Y:
        if (event.isControlDown()) {
          menuViewController.fireEvent(MenuOperation.REDO);
        }
        break;

      case S:
        if (event.isControlDown()) {
          menuBarController.fireEvent(MenuBarItem.SAVE);
        }
        break;

      case F11:
        menuBarController.fireEvent(MenuBarItem.FREE_MEMORY);
        break;

      case F12:
        menuBarController.fireEvent(MenuBarItem.SAVE_AS);
        break;

      case DELETE:
        menuViewController.fireEvent(MenuOperation.DELETE);
        break;

      case AT:
        if (event.isControlDown()) {
          settings.ui.notificationViewShowed.negate();
        }
        break;

      default:
    }
  }

  /** 通知ビューの可視性を切り替える. */
  private void setNotificationViewVisibility(boolean visible) {
    if (visible) {
      horizontalSplitPane.setDividerPositions(settings.ui.notificationSplitPos.get());
    } else {
      horizontalSplitPane.setDividerPositions(1);
    }
  }

  /** BhProgram にキーイベントを送信する. */
  private void sendKeyEventToBhProgram(KeyCode keyCode) {
    // 押下済みのキーのイベントは送信しない
    if (pressedKeys.contains(keyCode)) {
      return;
    }
    var eventName = KeyCodeConverter.toBhProgramEventName(keyCode).orElse(null);
    if (eventName == null) {
      return;
    }
    pressedKeys.add(keyCode);
    var bhEvent = new BhProgramEvent(eventName);
    if (settings.runtime.currentBhRuntimeType.get() == BhRuntimeType.LOCAL) {
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
    settings.ui.nodeManipMode.set(mode);
    foundationVbox.getScene().setCursor(mode.getCursor());
  }

  /** ノードの操作モードを変えるためのキーが押されているか調べる. */
  private boolean isNodeManipModeKeyPressed(MouseEvent event) {
    return event.isShiftDown() || event.isControlDown();
  }
}
