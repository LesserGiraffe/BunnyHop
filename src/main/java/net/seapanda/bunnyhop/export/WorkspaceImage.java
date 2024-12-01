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

package net.seapanda.bunnyhop.export;

import java.util.ArrayList;
import java.util.Collection;
import net.seapanda.bunnyhop.utility.Vec2D;

/**
 * ワークスペースの保存用イメージ.
 *
 * @author K.Koike
 */
class WorkspaceImage {

  /** ワークスペース名. */
  final String name;
  /** ワークスペースの大きさ. */
  final Vec2D size;
  /** ワークスペースのルートノードのリスト. */
  private final ArrayList<BhNodeImage> rootNodes;

  /**
   * コンストラクタ.
   * 保存対象の 1 つのワークスペースが持つ情報をこのオブジェクトに格納する.
   *
   * @param name ワークスペースの名前
   * @param size ワークスペースの大きさ
   * @param rootNodes ワークスペースが持つルートノードの保存用イメージ
   */
  WorkspaceImage(String name, Vec2D size, Collection<BhNodeImage> rootNodes) {
    this.name = name;
    this.size = size;
    this.rootNodes = new ArrayList<>(rootNodes);
  }

  /** デフォルトコンストラクタ. (デシリアライズ用) */
  public WorkspaceImage() {
    this.name = "";
    this.size = new Vec2D(0, 0);
    this.rootNodes = new ArrayList<>();
  }

  /** ルートノードの保存用イメージ一式を返す. */
  public Collection<BhNodeImage> getRootNodes() {
    return new ArrayList<>(rootNodes);
  }
}
