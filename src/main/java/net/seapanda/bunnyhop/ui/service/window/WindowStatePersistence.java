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
import net.seapanda.bunnyhop.common.configuration.BhSettings.Window;
import net.seapanda.bunnyhop.utility.math.Vec2D;

/**
 * ウィンドウのサイズ, 位置, 最大化状態の保存と復元を行うクラス.
 *
 * @author K.Koike
 */
class WindowStatePersistence {

  final Stage stage;
  private final Window window;

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
   * @param window ウィンドウの状態を保持するオブジェクト
   */
  WindowStatePersistence(Stage stage, Window window) {
    this.stage = stage;
    this.window = window;
    sizeBeforeMaximized = new Vec2D(window.width.get(), window.height.get());
    oldPos = new Vec2D(window.posX.get(), window.posY.get());
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
    stage.setWidth(window.width.get());
    stage.setHeight(window.height.get());
    stage.setX(window.posX.get());
    stage.setY(window.posY.get());
    stage.setMaximized(window.maximized.get());
  }

  /**
   * ウィンドウの状態を復元できるかどうか調べる.
   *
   * @return 復元可能な状態が保存されている場合は true
   */
  private boolean canRestoreWindowState() {
    return window.width.get() > 0
        && window.height.get() > 0
        && window.posX.get() > Integer.MIN_VALUE
        && window.posY.get() > Integer.MIN_VALUE
        && Screen.getScreens().stream().anyMatch(this::isWindowTopInScreen);
  }

  /** ウィンドウの右上端または左上端の矩形が {@code screen} の可視領域に入っているか調べる. */
  private boolean isWindowTopInScreen(Screen screen) {
    if (window.maximized.get()) {
      return true;
    }
    // 矩形の長さ
    double length = 20;
    Rectangle2D upperLeftBound =
        new Rectangle2D(window.posX.get(), window.posY.get(), length, length);
    double upperRight = window.posX.get() + window.width.get();
    Rectangle2D upperRightBound =
        new Rectangle2D(upperRight - length, window.posY.get(), length, length);
    Rectangle2D screenBounds = screen.getVisualBounds();
    return screenBounds.contains(upperLeftBound) || screenBounds.contains(upperRightBound);
  }

  /**
   * 現在のウィンドウの状態を保存する.
   * ウィンドウのサイズ, 位置, 最大化状態を保存する.
   */
  void save() {
    window.width.set(Double.valueOf(sizeBeforeMaximized.x).intValue());
    window.height.set(Double.valueOf(sizeBeforeMaximized.y).intValue());
    window.posX.set(Double.valueOf(oldPos.x).intValue());
    window.posY.set(Double.valueOf(oldPos.y).intValue());
    window.maximized.set(stage.isMaximized());
  }
}
