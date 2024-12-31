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

package net.seapanda.bunnyhop.model.node.syntaxsymbol;

import java.io.Serializable;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicLong;

/**
 * 各 {@link SyntaxSymbol} オブジェクト固有の ID.
 *
 * @author K.Koike
 */
public class InstanceId implements Serializable {

  private static AtomicLong sequentialID = new AtomicLong(0);
  private final String id;

  /** この ID が存在しないことを表すオブジェクト. */
  public static final InstanceId NONE = new InstanceId("");

  /**
   * {@link InstanceId} を作成する.
   * このメソッドで作った {@link InstanceId} は重複しないことが保証される.
   */
  static InstanceId newId() {
    long id = sequentialID.addAndGet(1);
    return new InstanceId(Long.toHexString(id));
  }

  /**
   * {@link InstanceId} を作成する.
   *
   * @param id 識別子名
   * @return {@link InstanceId} オブジェクト.
   */
  public static InstanceId of(String id) {
    return new InstanceId(id == null ? "" : id);
  }

  private InstanceId(String id) {
    this.id = id;
  }

  /** デフォルトコンストラクタ. (デシリアライズ用) */
  public InstanceId() {
    id = NONE.id;
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
    return (getClass() == obj.getClass()) && (id.equals(((InstanceId) obj).id));
  }

  @Override
  public int hashCode() {
    return  Objects.hashCode(this.id);
  }
}
