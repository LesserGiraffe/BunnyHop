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

package net.seapanda.bunnyhop.bhprogram.debugger;

import java.util.Objects;

/**
 * スレッドの選択状態を表すクラス.
 *
 * @author K.Koike
 */
public class ThreadSelection {

  /** スレッドが選択されていないことを表すオブジェクト. */
  public static final ThreadSelection NONE = new ThreadSelection(0L);

  /** 全てのスレッドが選択されていることを表すオブジェクト. */
  public static final ThreadSelection ALL = new ThreadSelection(-1L);

  private final long id;

  /**
   * コンストラクタ.
   *
   * @param id スレッド ID
   */
  private ThreadSelection(long id) {
    this.id = id;
  }

  /** デフォルトコンストラクタ. (デシリアライズ用) */
  public ThreadSelection() {
    this.id = NONE.id;
  }

  /**
   * {@link ThreadSelection} を作成する.
   *
   * @param id 選択されたスレッドの ID
   * @return {@link ThreadSelection} オブジェクト.
   */
  public static ThreadSelection of(long id) {
    if (id <= 0) {
      throw new AssertionError("Invalid thread id (%s)".formatted(id));
    }
    return new ThreadSelection(id);
  }

  /**
   * 選択されたスレッドの ID を返す.
   *
   * @return 選択されたスレッドの ID
   */
  public long getThreadId() {
    return id;
  }

  @Override
  public String toString() {
    return Long.toString(id);
  }

  @Override
  public boolean equals(Object obj) {
    if (obj instanceof ThreadSelection other) {
      return id == other.id;
    }
    return false;
  }

  @Override
  public int hashCode() {
    return Objects.hashCode(id);
  }  
}
