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
 * コネクタ ID.
 *
 * @author K.Koike
 */
public class ConnectorId implements Serializable {

  /** コネクタ ID が存在しないことを表すオブジェクト. */
  public static final ConnectorId NONE = new ConnectorId("");
  private final String id;

  /**
   * コンストラクタ.
   *
   * @param id 識別子名
   */
  private ConnectorId(String id) {
    this.id = id;
  }

  /** デフォルトコンストラクタ. (デシリアライズ用) */
  public ConnectorId() {
    id = NONE.id;
  }

  /**
   * {@link ConnectorId} を作成する.
   *
   * @param id 識別子名
   * @return ConnectorId オブジェクト.
   */
  public static ConnectorId of(String id) {
    return new ConnectorId(id == null ? "" : id);
  }

  @Override
  public String toString() {
    return id;
  }

  @Override
  public boolean equals(Object obj) {
    if (obj instanceof ConnectorId other) {
      return id.equals(other.id);
    }
    return false;
  }

  @Override
  public int hashCode() {
    return Objects.hashCode(id);
  }
}
