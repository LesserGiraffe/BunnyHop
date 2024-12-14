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

package net.seapanda.bunnyhop.control.node;

import javafx.scene.input.MouseButton;

/**
 * マウス操作の排他制御を行うためのクラス.
 *
 * <p>
 * {@link MouseCtrlLock} オブジェクトは {@code lock} と {@code unlock} の 2 つの状態を取る.
 * このオブジェクトが unlock 状態のとき, 特定のマウスボタンを指定して {@link #tryLock} で {@code lock} 状態にできる.
 * lock 状態のとき, {@link #tryLock} で指定したマウスボタンを指定して {@link #unlock} で {@code unlock} 状態にできる.
 * </p>
 *
 * @author K.Koike
 */
class MouseCtrlLock {
  private boolean isLocked = false;
  private MouseButton button = null;
    
  /**
   * {@code button} に関連付けて, このオブジェクトを lock 状態にする.
   *
   * @return lock 状態になった場合 true
   */
  public synchronized boolean tryLock(MouseButton button) {
    if (isLocked || button == null) {
      return false;
    }
    this.button = button;
    isLocked = true;
    return true;
  }

  /**
   * このオブジェクトが {@code button} に関連付けられて {@code lock} 状態になっていた場合,
   * このオブジェクトを {@code unlock} 状態にする.
   *
   * @return unlock 状態になった場合 true
   */
  public synchronized boolean unlock(MouseButton button) {
    if (isLockedBy(button)) {
      button = null;
      isLocked = false;
      return true;
    }
    return false;
  }

  /**
   * このオブジェクトが {@code lock} 状態である場合, {@code unlock} 状態にする.
   *
   * @return unlock 状態になった場合 true
   */
  public synchronized boolean unlock() {
    if (isLocked) {
      button = null;
      isLocked = false;
      return true;
    }
    return false;
  }

  /** このオブジェクトが {@code button} に関連付けられて {@code lock} 状態になっている場合 true を返す. */
  public synchronized boolean isLockedBy(MouseButton button) {
    if (button == null) {
      return false;
    }
    return isLocked && button == this.button;
  }

  /** このオブジェクトが lock 状態である場合 true を返す. */
  public synchronized boolean isLocked() {
    return isLocked;
  }
}
