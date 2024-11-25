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
 * コネクタパラメータセットの ID.
 *
 * @author K.Koike
 */
public class ConnectorParamSetId implements Serializable {

  /** コネクタパラメータセットが存在しないことを表すオブジェクト. */
  public static final ConnectorParamSetId NONE = new ConnectorParamSetId("");
  private final String id;

  /**
   * コンストラクタ.
   *
   * @param id 識別子名
   */
  private ConnectorParamSetId(String id) {
    this.id = id;
  }

  /**
   * {@link ConnectorParamSetId} を作成する.
   *
   * @param id 識別子名
   * @return ConnectorTemplateId オブジェクト.
   */
  public static ConnectorParamSetId of(String id) {
    return new ConnectorParamSetId(id == null ? "" : id);
  }

  @Override
  public String toString() {
    return id;
  }

  @Override
  public boolean equals(Object obj) {
    if (obj instanceof ConnectorParamSetId other) {
      return id.equals(other.id);
    }
    return false;
  }

  @Override
  public int hashCode() {
    return Objects.hashCode(id);
  }
}
