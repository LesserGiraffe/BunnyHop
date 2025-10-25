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

import com.badlogic.gdx.backends.lwjgl3.Lwjgl3Window;
import java.util.Deque;
import java.util.LinkedList;
import java.util.SequencedCollection;
import javafx.scene.input.MouseButton;
import javafx.scene.input.MouseEvent;
import javafx.stage.Stage;
import net.seapanda.bunnyhop.ui.view.ViewUtil;

/**
 * アプリケーションを構成するウィンドウに対する操作を提供するクラス.
 *
 * @author K.Koike
 */
public class BhWindowManager implements WindowManager {

  private final Stage primaryStage;
  private final Lwjgl3Window simulatorWindow;
  private final Deque<MouseEvent> mousePressedEvents = new LinkedList<>();
  /** マウスボタンが押されているかどうかのフラグ. */
  private boolean isMousePressed = false;
  /**
   * マウスボタンが押されている間にウィンドウフォーカスが外れた場合,
   * ウィンドウフォーカス復帰直後のマウスドラッグイベントが, 最初のドラッグ対象を追跡しない不具合がある.
   * これをはウィンドウフォーカス復帰直後に一度ウィンドウの任意の場所をクリックさせると回避できる.
   * このフラグは, このクリックを強制するために使用する.
   */
  private boolean needsOneClick = false;
  private MouseEvent lastMouseEvent = new MouseEvent(
      MouseEvent.ANY,
      0, 0, 0, 0,
      MouseButton.PRIMARY, 1,
      false, false, false, false, false, false, false, false, false, false, null);

  /**
   * コンストラクタ.
   *
   * @param primaryStage アプリケーションのメインウィンドウに対応する {@link Stage}.
   * @param simulatorWindow シミュレータを表示するウィンドウに対応する {@link Lwjgl3Window}
   */
  public BhWindowManager(Stage primaryStage, Lwjgl3Window simulatorWindow) {
    this.primaryStage = primaryStage;
    this.simulatorWindow = simulatorWindow;
    primaryStage.focusedProperty().addListener(
        (obs, oldVal, newVal) -> handleWindowFocusLoss(!newVal));

    primaryStage.addEventFilter(MouseEvent.ANY, this::onMouseEventsDetected);
  }

  /** マウスイベントを検出した時の処理. */
  private void onMouseEventsDetected(MouseEvent event) {
    if (primaryStage.focusedProperty().get() && needsOneClick) {
      event.consume();
      if (event.getEventType() == MouseEvent.MOUSE_CLICKED) {
        needsOneClick = false;
      }
    }
    if (event.getEventType() == MouseEvent.MOUSE_PRESSED) {
      mousePressedEvents.addLast(event);
      isMousePressed = true;
    } else if (event.getEventType() == MouseEvent.MOUSE_RELEASED) {
      mousePressedEvents.clear();
      isMousePressed = false;
    }
    lastMouseEvent = event;
  }

  /**
   * ウィンドウフォーカスが失われた際にマウス状態をクリーンアップする.
   *
   * <p>マウスボタンが押されている状態でフォーカスを失った場合,
   * マウスリリースイベントとクリックイベントを強制的に発火させ,
   * 次回のフォーカス復帰時に 1 クリックを要求するフラグを立てる.
   *
   * @param windowFocusLost ウィンドウフォーカスが失われた場合 true.
   */
  private void handleWindowFocusLoss(boolean windowFocusLost) {
    if (windowFocusLost && isMousePressed) {
      needsOneClick = true;
      isMousePressed = false;
      createMouseReleasedEvents().forEach(
          event -> MouseEvent.fireEvent(event.getTarget(), event));
    }
  }

  /** マウスボタンを離したときに発生するイベントを作成する. */
  private SequencedCollection<MouseEvent> createMouseReleasedEvents() {
    var events = new LinkedList<MouseEvent>();
    for (MouseEvent event : mousePressedEvents.reversed()) {
      var clickedEvent = new MouseEvent(
          event.getSource(),
          event.getTarget(),
          MouseEvent.MOUSE_CLICKED,
          lastMouseEvent.getX(),
          lastMouseEvent.getY(),
          lastMouseEvent.getScreenX(),
          lastMouseEvent.getScreenY(),
          event.getButton(),
          event.getClickCount(),
          false,
          false,
          false,
          false,
          event.getButton() != MouseButton.PRIMARY && event.isPrimaryButtonDown(),
          event.getButton() != MouseButton.MIDDLE && event.isMiddleButtonDown(),
          event.getButton() != MouseButton.SECONDARY && event.isSecondaryButtonDown(),
          event.getButton() != MouseButton.BACK && event.isBackButtonDown(),
          event.getButton() != MouseButton.FORWARD && event.isForwardButtonDown(),
          event.isSynthesized(),
          event.isPopupTrigger(),
          lastMouseEvent.isStillSincePress(),
          null);
      var releasedEvent = new MouseEvent(
          event.getSource(),
          event.getTarget(),
          MouseEvent.MOUSE_RELEASED,
          lastMouseEvent.getX(),
          lastMouseEvent.getY(),
          lastMouseEvent.getScreenX(),
          lastMouseEvent.getScreenY(),
          event.getButton(),
          event.getClickCount(),
          false,
          false,
          false,
          false,
          clickedEvent.isPrimaryButtonDown(),
          clickedEvent.isMiddleButtonDown(),
          clickedEvent.isSecondaryButtonDown(),
          clickedEvent.isBackButtonDown(),
          clickedEvent.isForwardButtonDown(),
          event.isSynthesized(),
          event.isPopupTrigger(),
          lastMouseEvent.isStillSincePress(),
          null);
      events.addLast(releasedEvent);
      events.addLast(clickedEvent);
    }
    return events;
  }

  @Override
  public void focusSimulator(boolean doForcibly) {
    ViewUtil.runSafe(() -> {
      if (doForcibly || !simulatorWindow.isIconified()) {
        simulatorWindow.restoreWindow();
        simulatorWindow.focusWindow();
      }
    });
  }

  @Override
  public void focusSimulator() {
    focusSimulator(false);
  }
}
