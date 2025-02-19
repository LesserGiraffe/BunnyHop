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

package net.seapanda.bunnyhop.model.node.parameter;

import java.io.Serializable;
import java.util.Objects;

/**
 * {@link BhNodeView} のスタイルの ID.
 *
 * @author K.Koike
 */
public class BhNodeViewStyleId implements Serializable {

  /** {@link BhNodeView} のスタイルの ID が存在しないことを表すオブジェクト. */
  public static final BhNodeViewStyleId NONE = new BhNodeViewStyleId("");
  private final String id;

  /**
   * コンストラクタ.
   *
   * @param id 識別子名
   */
  private BhNodeViewStyleId(String id) {
    this.id = id;
  }

  /** デフォルトコンストラクタ. (デシリアライズ用) */
  public BhNodeViewStyleId() {
    this.id = NONE.id;
  }

  /**
   * {@link BhNodeId} を作成する.
   *
   * @param id 識別子名
   * @return {@link BhNodeId} オブジェクト.
   */
  public static BhNodeViewStyleId of(String id) {
    return new BhNodeViewStyleId(id == null ? "" : id);
  }

  @Override
  public String toString() {
    return id;
  }

  @Override
  public boolean equals(Object obj) {
    if (obj instanceof BhNodeViewStyleId other) {
      return id.equals(other.id);
    }
    return false;
  }

  @Override
  public int hashCode() {
    return Objects.hashCode(id);
  }
}
