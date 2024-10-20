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

package net.seapanda.bunnyhop.model.node.imitation;

import java.io.Serializable;
import java.util.Objects;
import net.seapanda.bunnyhop.common.constant.BhConstants;
import net.seapanda.bunnyhop.common.constant.VersionInfo;

/**
 * 作成するイミテーションを識別するための ID.
 *
 * @author K.Koike
 */
public class ImitationId implements Serializable {

  private static final long serialVersionUID = VersionInfo.SERIAL_VERSION_UID;
  /** イミテーションIDが存在しないことを表す null オブジェクト. */
  public static final ImitationId NONE = new ImitationId("");
  /** イミテーション手動作成時のID. */
  public static final ImitationId MANUAL =
      new ImitationId(BhConstants.BhModelDef.ATTR_VAL_IMIT_ID_MANUAL);
  private final String id;

  /**
   * コンストラクタ.
   *
   * @param id 識別子名
   */
  private ImitationId(String id) {
    this.id = id;
  }

  /**
   * イミテーションIDを作成する.
   *
   * @param id 識別子名
   * @return イミテーションID
   */
  public static ImitationId create(String id) {
    return new ImitationId(id == null ? "" : id);
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
    return (getClass() == obj.getClass()) && (id.equals(((ImitationId) obj).id));
  }

  @Override
  public int hashCode() {
    int hash = 7;
    hash = 59 * hash + Objects.hashCode(this.id);
    return hash;
  }
}
