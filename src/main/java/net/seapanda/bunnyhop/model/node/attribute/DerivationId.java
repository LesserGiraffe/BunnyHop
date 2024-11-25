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

package net.seapanda.bunnyhop.model.node.attribute;

import java.io.Serializable;
import java.util.Objects;

/**
 * 派生先 ID (= 派生ノードを特定するための ID).
 *
 * <p>
 * ノードには, 複数の派生先を定義することができる.
 * 各派生先の定義において, この ID と派生ノードの ID が 1 つずつ指定される.
 * 従って, ノードに対しこの ID を指定すると, 対応する派生ノードを (あれば) 1 つ特定することができる.
 * </p>
 *
 * @author K.Koike
 */
public class DerivationId implements Serializable {

  /** 派生 ID が存在しないことを表す null オブジェクト. */
  public static final DerivationId NONE = new DerivationId("");
  private final String id;

  /**
   * コンストラクタ.
   *
   * @param id 識別子名
   */
  private DerivationId(String id) {
    this.id = id;
  }

  /**
   * 派生 ID を作成する.
   *
   * @param id 識別子名
   * @return 派生 ID
   */
  public static DerivationId of(String id) {
    return new DerivationId(id == null ? "" : id);
  }

  @Override
  public String toString() {
    return id;
  }

  @Override
  public boolean equals(Object obj) {
    if (obj instanceof DerivationId other) {
      return id.equals(other.id);
    }
    return false;
  }

  @Override
  public int hashCode() {
    return Objects.hashCode(this.id);
  }
}
