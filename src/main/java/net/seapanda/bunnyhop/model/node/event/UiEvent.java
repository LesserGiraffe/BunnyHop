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

package net.seapanda.bunnyhop.model.node.event;

/**
 * UI 操作に関連する情報を格納するクラス.
 *
 * @author K.Koike
 */
public class UiEvent {

  public final boolean isPrimaryButtonDown;
  public final boolean isSecondaryButtonDown;
  public final boolean isMiddleButtonDown;
  public final boolean isBackButtonDown;
  public final boolean isForwardButtonDown;
  public final boolean isShiftDown;
  public final boolean isCtrlDown;
  public final boolean isAltDown;
  
  /** コンストラクタ. */
  public UiEvent(
      boolean isPrimaryButtonDown,
      boolean isSecondaryButtonDown,
      boolean isMiddleButtonDown,
      boolean isBackButtonDown,
      boolean isForwardButtonDown,
      boolean isShiftDown,
      boolean isCtrlDown,
      boolean isAltDown) {
    this.isPrimaryButtonDown = isPrimaryButtonDown;
    this.isSecondaryButtonDown = isSecondaryButtonDown;
    this.isMiddleButtonDown = isMiddleButtonDown;
    this.isBackButtonDown = isBackButtonDown;
    this.isForwardButtonDown = isForwardButtonDown;
    this.isShiftDown = isShiftDown;
    this.isCtrlDown = isCtrlDown;
    this.isAltDown = isAltDown;
  }
}
