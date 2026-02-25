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

package net.seapanda.bunnyhop.ui.service.window;

import static net.seapanda.bunnyhop.common.configuration.BhSettings.Window.nodeSelectionSplitPos;
import static net.seapanda.bunnyhop.common.configuration.BhSettings.Window.notificationSplitPos;

import javafx.geometry.Rectangle2D;
import javafx.scene.Node;
import javafx.scene.control.SplitPane;
import javafx.stage.Screen;
import javafx.stage.Stage;
import net.seapanda.bunnyhop.common.configuration.BhConstants;
import net.seapanda.bunnyhop.common.configuration.BhSettings.WindowState;
import net.seapanda.bunnyhop.utility.math.Vec2D;

/**
 * ウィンドウのサイズ, 位置, 最大化状態の保存と復元を行うクラス.
 *
 * @author K.Koike
 */
class WindowStatePersistence {

  public final Stage stage;
  private final WindowState state;

  /** 最大化する前のウィンドウのサイズを保持するオブジェクト. */
  private final Vec2D sizeBeforeMaximized;
  /**
   * 変更前のウィンドウの位置を保持するオブジェクト.
   *
   * <p>ウィンドウ最大化イベントの発生前にウィンドウの位置が変わるので, 最大化前の古い値を保持する必要がある.
   * 最大化イベントが発生したときに, このオブジェクトの値で {@link #posBeforeMaximized} の値を上書きする.
   */
  private final Vec2D oldPos;
  /** 最大化する前のウィンドウの位置を保持するオブジェクト. */
  private final Vec2D posBeforeMaximized;
  /** ウィンドウが最大化されているかどうかのフラグ. */
  private boolean isMaximized;

  /**
   * コンストラクタ.
   *
   * @param stage 状態を管理する Stage オブジェクト
   * @param state ウィンドウの状態を保持するオブジェクト
   */
  WindowStatePersistence(Stage stage, WindowState state) {
    this.stage = stage;
    this.state = state;
    sizeBeforeMaximized = new Vec2D(state.width, state.height);
    oldPos = new Vec2D(state.posX, state.posY);
    posBeforeMaximized = new Vec2D(state.posX, state.posY);
    setEventHandlers();
  }

  private void setEventHandlers() {
    stage.widthProperty().addListener((obs, oldVal, newVal) ->
        sizeBeforeMaximized.x = isMaximized ? sizeBeforeMaximized.x : newVal.intValue());

    stage.heightProperty().addListener((obs, oldVal, newVal) ->
        sizeBeforeMaximized.y = isMaximized ? sizeBeforeMaximized.y : newVal.intValue());

    stage.xProperty().addListener((obs, oldVal, newVal) -> {
      oldPos.x = isMaximized ? oldPos.x : oldVal.intValue();
      posBeforeMaximized.x = isMaximized ? posBeforeMaximized.x : newVal.intValue();
    });

    stage.yProperty().addListener((obs, oldVal, newVal) -> {
      oldPos.y = isMaximized ? oldPos.y : oldVal.intValue();
      posBeforeMaximized.y = isMaximized ? posBeforeMaximized.y : newVal.intValue();
    });

    stage.maximizedProperty().addListener((obs, oldVal, newVal) -> {
      isMaximized = newVal;
      // 最大化したとき変更前の位置を保存する.
      posBeforeMaximized.x = isMaximized ? oldPos.x : posBeforeMaximized.x;
      posBeforeMaximized.y = isMaximized ? oldPos.y : posBeforeMaximized.y;
    });

    stage.showingProperty().addListener((obs, oldVal, newVal) -> {
      if (newVal) {
        isMaximized = stage.isMaximized();
        sizeBeforeMaximized.x  = stage.getWidth();
        sizeBeforeMaximized.y = stage.getHeight();
        oldPos.x = stage.getX();
        oldPos.y = stage.getY();
        posBeforeMaximized.x = oldPos.x;
        posBeforeMaximized.y = oldPos.y;
        setSplitPaneDividerPositions();
      }
    });
  }

  /**
   * 保存されたウィンドウの状態を復元する.
   * 復元可能な状態が存在しない場合は何もしない.
   */
  void restore() {
    if (!canRestoreWindowState()) {
      return;
    }
    stage.setWidth(state.width);
    stage.setHeight(state.height);
    stage.setX(state.posX);
    stage.setY(state.posY);
    stage.setMaximized(state.maximized);
  }

  /**
   * ウィンドウの状態を復元できるかどうか調べる.
   *
   * @return 復元可能な状態が保存されている場合は true
   */
  private boolean canRestoreWindowState() {
    return state.width >= 0
        && state.height >= 0
        && state.posX > Integer.MIN_VALUE
        && state.posY > Integer.MIN_VALUE
        && Screen.getScreens().stream().anyMatch(this::isWindowTopInScreen);
  }

  /** ウィンドウの右上端または左上端の矩形が {@code screen} の可視領域に入っているか調べる. */
  private boolean isWindowTopInScreen(Screen screen) {
    if (state.maximized) {
      return true;
    }
    // 矩形の長さ
    double length = 20;
    Rectangle2D upperLeftBound = new Rectangle2D(state.posX, state.posY, length, length);
    double upperRight = state.posX + state.width;
    Rectangle2D upperRightBound = new Rectangle2D(upperRight - length, state.posY, length, length);
    Rectangle2D screenBounds = screen.getVisualBounds();
    return screenBounds.contains(upperLeftBound) || screenBounds.contains(upperRightBound);
  }

  /**
   * 現在のウィンドウの状態を保存する.
   * ウィンドウのサイズ, 位置, 最大化状態を保存する.
   */
  void save() {
    state.width = Double.valueOf(sizeBeforeMaximized.x).intValue();
    state.height = Double.valueOf(sizeBeforeMaximized.y).intValue();
    state.posX = Double.valueOf(oldPos.x).intValue();
    state.posY = Double.valueOf(oldPos.y).intValue();
    state.maximized = stage.isMaximized();
    saveSplitPaneDividerPositions();
  }

  private void setSplitPaneDividerPositions() {
    Node node = stage.getScene().lookup("#" + BhConstants.UiId.VERTICAL_SPLIT_PANE);
    if (node instanceof SplitPane splitPane && nodeSelectionSplitPos >= 0) {
      splitPane.getDividers().getFirst().setPosition(nodeSelectionSplitPos);
    }

    node = stage.getScene().lookup("#" + BhConstants.UiId.HORIZONTALSPLIT_PANE);
    if (node instanceof SplitPane splitPane && notificationSplitPos >= 0) {
      splitPane.getDividers().getFirst().setPosition(notificationSplitPos);
    }
  }

  private void saveSplitPaneDividerPositions() {
    Node node = stage.getScene().lookup("#" + BhConstants.UiId.VERTICAL_SPLIT_PANE);
    if (node instanceof SplitPane splitPane) {
      nodeSelectionSplitPos = splitPane.getDividers().getFirst().getPosition();
    }

    node = stage.getScene().lookup("#" + BhConstants.UiId.HORIZONTALSPLIT_PANE);
    if (node instanceof SplitPane splitPane) {
      notificationSplitPos = splitPane.getDividers().getFirst().getPosition();
    }
  }
}
