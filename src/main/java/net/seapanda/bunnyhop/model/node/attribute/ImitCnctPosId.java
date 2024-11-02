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
 * イミテーションの接続先の識別子.
 *
 * @author K.Koike
 */
public class ImitCnctPosId implements Serializable {

  private static final long serialVersionUID = VersionInfo.SERIAL_VERSION_UID;
  /** イミテーション接続位置が存在しないことを表す. */
  public static final ImitCnctPosId NONE = new ImitCnctPosId("");
  private final String id;

  /**
   * コンストラクタ.
   *
   * @param point 接続先名
   */
  private ImitCnctPosId(String point) {
    this.id = point;
  }

  /**
   * イミテーション接続位置の識別子を作成する.
   *
   * @param point イミテーション接続位置名
   * @return イミテーション接続位置の識別子
   */
  public static ImitCnctPosId create(String point) {
    return new ImitCnctPosId(point == null ? "" : point);
  }

  @Override
  public String toString() {
    return id;
  }

  @Override
  public boolean equals(Object obj) {
    if (obj instanceof ImitCnctPosId other) {
      return id.equals(other.id);
    }
    return false;
  }

  @Override
  public int hashCode() {
    return Objects.hashCode(this.id);
  }
}
