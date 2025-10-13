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

import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;
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
 * <p>本クラスはスレッド間で排他制御を行うものではない.
 * 単一のスレッド上で, マウスボタンの同時押しによる操作を却下する目的で使用することを想定している.
 *
 * @author K.Koike
 */
public class MouseCtrlLock {
  private boolean isLocked = false;
  private MouseButton button = null;
  private final Set<MouseButton> acceptables;
    
  /**
   * コンストラクタ.
   *
   * @param acceptables ロックの取得に使えるボタン.  何も指定しなかった場合は全てのボタンでロックを取得可能となる.
   */
  public MouseCtrlLock(MouseButton... acceptables) {
    this.acceptables = Stream.of(acceptables)
        .filter(Objects::nonNull)
        .collect(Collectors.toSet());
  }

  /**
   * {@code button} に関連付けて, このオブジェクトを lock 状態にする.
   *
   * @return lock 状態になった場合 true
   */
  public boolean tryLock(MouseButton button) {
    if (isLocked || (!acceptables.isEmpty() && !acceptables.contains(button))) {
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
  public boolean unlock(MouseButton button) {
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
  public boolean unlock() {
    if (isLocked) {
      button = null;
      isLocked = false;
      return true;
    }
    return false;
  }

  /** このオブジェクトが {@code button} に関連付けられて {@code lock} 状態になっている場合 true を返す. */
  public boolean isLockedBy(MouseButton button) {
    if (button == null) {
      return false;
    }
    return isLocked && button == this.button;
  }

  /** このオブジェクトが lock 状態である場合 true を返す. */
  public boolean isLocked() {
    return isLocked;
  }
}
