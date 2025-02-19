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
 * マウスイベントに関連する情報を格納するクラス.
 *
 * @author K.Koike
 */
public class MouseEventInfo {

  public final boolean isFromPrimaryButton;
  public final boolean isFromSecondaryButton;
  public final boolean isFromMiddleButton;
  public final boolean isFromBackButton;
  public final boolean isFromforwardButton;
  public final boolean isShiftDown;
  public final boolean isCtrlDown;
  public final boolean isAltDown;
  
  /** コンストラクタ. */
  public MouseEventInfo(
      boolean isFromPrimaryButton,
      boolean isFromSecondaryButton,
      boolean isFromMiddleButton,
      boolean isFromBackButton,
      boolean isFromforwardButton,
      boolean isShiftDown,
      boolean isCtrlDown,
      boolean isAltDown) {
    this.isFromPrimaryButton = isFromPrimaryButton;
    this.isFromSecondaryButton = isFromSecondaryButton;
    this.isFromMiddleButton = isFromMiddleButton;
    this.isFromBackButton = isFromBackButton;
    this.isFromforwardButton = isFromforwardButton;
    this.isShiftDown = isShiftDown;
    this.isCtrlDown = isCtrlDown;
    this.isAltDown = isAltDown;
  }
}
