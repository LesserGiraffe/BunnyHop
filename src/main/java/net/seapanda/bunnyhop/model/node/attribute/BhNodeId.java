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
import net.seapanda.bunnyhop.common.constant.VersionInfo;

/**
 * Bh ノード ID.
 *
 * @author K.Koike
 */
public class BhNodeId implements Serializable {

  private static final long serialVersionUID = VersionInfo.SERIAL_VERSION_UID;
  /** BhNodeIDが存在しないことを表す null オブジェクト. */
  public static final BhNodeId NONE = new BhNodeId("");
  String id;

  /**
   * コンストラクタ.
   *
   * @param id 識別子名
   */
  private BhNodeId(String id) {
    this.id = id;
  }

  /**
   * BhノードIDを作成する.
   *
   * @param id 識別子名
   * @return BhノードID
   */
  public static BhNodeId create(String id) {
    return new BhNodeId(id == null ? "" : id);
  }

  @Override
  public String toString() {
    return id;
  }

  @Override
  public boolean equals(Object obj) {
    if (obj == null) {
      return false;
    }
    return (getClass() == obj.getClass()) && (id.equals(((BhNodeId) obj).id));
  }

  @Override
  public int hashCode() {
    int hash = 5;
    hash = 11 * hash + Objects.hashCode(this.id);
    return hash;
  }
}
