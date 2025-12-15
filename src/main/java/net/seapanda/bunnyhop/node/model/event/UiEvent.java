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

package net.seapanda.bunnyhop.node.model.event;

/**
 * ノードをターゲットとする UI 操作に関する情報を格納するクラス.
 *
 * @author K.Koike
 */
public class UiEvent {

  public final Type type;
  public final boolean isPrimaryButtonDown;
  public final boolean isSecondaryButtonDown;
  public final boolean isMiddleButtonDown;
  public final boolean isBackButtonDown;
  public final boolean isForwardButtonDown;
  public final boolean isShiftDown;
  public final boolean isCtrlDown;
  public final boolean isAltDown;
  public final boolean isDragAndDropTarget;
  public final int clickCount;

  /** コンストラクタ. */
  public UiEvent(
      UiEventType eventType,
      boolean isPrimaryButtonDown,
      boolean isSecondaryButtonDown,
      boolean isMiddleButtonDown,
      boolean isBackButtonDown,
      boolean isForwardButtonDown,
      boolean isShiftDown,
      boolean isCtrlDown,
      boolean isAltDown,
      boolean isDragAndDropTarget,
      int clickCount) {
    this.type = new Type(eventType);
    this.isPrimaryButtonDown = isPrimaryButtonDown;
    this.isSecondaryButtonDown = isSecondaryButtonDown;
    this.isMiddleButtonDown = isMiddleButtonDown;
    this.isBackButtonDown = isBackButtonDown;
    this.isForwardButtonDown = isForwardButtonDown;
    this.isShiftDown = isShiftDown;
    this.isCtrlDown = isCtrlDown;
    this.isAltDown = isAltDown;
    this.isDragAndDropTarget = isDragAndDropTarget;
    this.clickCount = clickCount;
  }

  /**
   * UI イベント識別用のフィールドを定義したクラス.
   *
   * <p>boolean 型の各フィールドは外部スクリプトで簡便にイベントの種類を識別するために存在する.
   */
  public static class Type {
    public final UiEventType eventType;
    public final boolean isMousePressed;
    public final boolean isDragDetected;
    public final boolean isMouseReleased;

    private Type(UiEventType eventType) {
      this.eventType = eventType;
      this.isMousePressed = eventType == UiEventType.MOUSE_PRESSED;
      this.isDragDetected = eventType == UiEventType.DRAG_DETECTED;
      this.isMouseReleased = eventType == UiEventType.MOUSE_RELEASED;
    }
  }
}
