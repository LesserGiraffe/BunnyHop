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

package net.seapanda.bunnyhop.node.model.syntaxtree;

import java.io.Serializable;
import java.util.Objects;
import java.util.UUID;

/**
 * 各 {@link SyntaxTreeNode} オブジェクト固有の ID.
 *
 * @author K.Koike
 */
public class NodeInstanceId implements Serializable {

  private final String id;

  /** この ID が存在しないことを表すオブジェクト. */
  public static final NodeInstanceId NONE = new NodeInstanceId("");

  /**
   * {@link NodeInstanceId} を作成する.
   * このメソッドで作った {@link NodeInstanceId} は重複しないことが保証される.
   */
  public static NodeInstanceId newId() {
    return new NodeInstanceId(UUID.randomUUID().toString());
  }

  /**
   * {@link NodeInstanceId} を作成する.
   *
   * @param id 識別子名
   * @return {@link NodeInstanceId} オブジェクト.
   */
  public static NodeInstanceId of(String id) {
    return new NodeInstanceId(id == null ? "" : id);
  }

  private NodeInstanceId(String id) {
    this.id = id;
  }

  /** デフォルトコンストラクタ. (デシリアライズ用) */
  public NodeInstanceId() {
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
    return (getClass() == obj.getClass()) && (id.equals(((NodeInstanceId) obj).id));
  }

  @Override
  public int hashCode() {
    return Objects.hashCode(this.id);
  }
}
