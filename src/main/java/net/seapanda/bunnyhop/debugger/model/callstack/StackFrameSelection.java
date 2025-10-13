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

package net.seapanda.bunnyhop.debugger.model.callstack;

import java.util.Objects;

/**
 * スタックフレームの選択状態を表すクラス.
 *
 * @author K.Koike
 */
public class StackFrameSelection {

  /** スタックフレームが選択されていないことを表すオブジェクト. */
  public static final StackFrameSelection NONE = new StackFrameSelection(-1);

  private final int idx;

  /**
   * コンストラクタ.
   *
   * @param idx スタックフレームのインデックス
   */
  private StackFrameSelection(int idx) {
    this.idx = idx;
  }

  /** デフォルトコンストラクタ. (デシリアライズ用) */
  public StackFrameSelection() {
    this.idx = NONE.idx;
  }

  /**
   * {@link StackFrameSelection} を作成する.
   *
   * @param idx 選択されたスタックフレームのインデックス
   * @return {@link StackFrameSelection} オブジェクト.
   */
  public static StackFrameSelection of(int idx) {
    if (idx < 0) {
      throw new AssertionError("Invalid stack frame index (%s)".formatted(idx));
    }
    return new StackFrameSelection(idx);
  }

  /**
   * 選択されたスタックフレームのインデックスを返す.
   *
   * @return スタックフレームのインデックス
   */
  public int getIndex() {
    return idx;
  }

  @Override
  public String toString() {
    return Long.toString(idx);
  }

  @Override
  public boolean equals(Object obj) {
    if (obj instanceof StackFrameSelection other) {
      return idx == other.idx;
    }
    return false;
  }

  @Override
  public int hashCode() {
    return Objects.hashCode(idx);
  }
}
