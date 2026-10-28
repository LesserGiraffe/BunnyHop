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

import javafx.geometry.Rectangle2D;
import javafx.stage.Screen;
import javafx.stage.Stage;
import net.seapanda.bunnyhop.common.configuration.BhSettings.WindowState;
import net.seapanda.bunnyhop.utility.math.Vec2D;

/**
 * ウィンドウのサイズ, 位置, 最大化状態の保存と復元を行うクラス.
 *
 * @author K.Koike
 */
class WindowStatePersistence {

  public final Stage stage;
  private final WindowState windowState;

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
   * @param windowState ウィンドウの状態を保持するオブジェクト
   */
  WindowStatePersistence(Stage stage, WindowState windowState) {
    this.stage = stage;
    this.windowState = windowState;
    sizeBeforeMaximized = new Vec2D(windowState.width.get(), windowState.height.get());
    oldPos = new Vec2D(windowState.posX.get(), windowState.posY.get());
    posBeforeMaximized = new Vec2D(oldPos);
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
    stage.setWidth(windowState.width.get());
    stage.setHeight(windowState.height.get());
    stage.setX(windowState.posX.get());
    stage.setY(windowState.posY.get());
    stage.setMaximized(windowState.maximized.get());
  }

  /**
   * ウィンドウの状態を復元できるかどうか調べる.
   *
   * @return 復元可能な状態が保存されている場合は true
   */
  private boolean canRestoreWindowState() {
    return windowState.width.get() >= 0
        && windowState.height.get() >= 0
        && windowState.posX.get() > Integer.MIN_VALUE
        && windowState.posY.get() > Integer.MIN_VALUE
        && Screen.getScreens().stream().anyMatch(this::isWindowTopInScreen);
  }

  /** ウィンドウの右上端または左上端の矩形が {@code screen} の可視領域に入っているか調べる. */
  private boolean isWindowTopInScreen(Screen screen) {
    if (windowState.maximized.get()) {
      return true;
    }
    // 矩形の長さ
    double length = 20;
    Rectangle2D upperLeftBound =
        new Rectangle2D(windowState.posX.get(), windowState.posY.get(), length, length);
    double upperRight = windowState.posX.get() + windowState.width.get();
    Rectangle2D upperRightBound =
        new Rectangle2D(upperRight - length, windowState.posY.get(), length, length);
    Rectangle2D screenBounds = screen.getVisualBounds();
    return screenBounds.contains(upperLeftBound) || screenBounds.contains(upperRightBound);
  }

  /**
   * 現在のウィンドウの状態を保存する.
   * ウィンドウのサイズ, 位置, 最大化状態を保存する.
   */
  void save() {
    windowState.width.set(Double.valueOf(sizeBeforeMaximized.x).intValue());
    windowState.height.set(Double.valueOf(sizeBeforeMaximized.y).intValue());
    windowState.posX.set(Double.valueOf(oldPos.x).intValue());
    windowState.posY.set(Double.valueOf(oldPos.y).intValue());
    windowState.maximized.set(stage.isMaximized());
  }
}
