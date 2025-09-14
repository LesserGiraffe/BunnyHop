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

package net.seapanda.bunnyhop.model.nodeselection;

import java.util.LinkedHashSet;
import java.util.SequencedSet;
import net.seapanda.bunnyhop.model.node.parameter.BhNodeId;

/**
 * ノードカテゴリを表す TreeView の各セルのモデルクラス.
 *
 * @author K.Koike
 */
public class BhNodeCategory {
  public final String name;
  private String cssClass = "";
  private final SequencedSet<BhNodeId> nodeIds = new LinkedHashSet<>();

  /**
   * コンストラクタ.
   *
   * @param name カテゴリ名
   */
  public BhNodeCategory(String name) {
    this.name = name;
  }

  @Override
  public String toString() {
    return name == null ? "" : name;
  }

  public void setCssClass(String cssClass) {
    this.cssClass = cssClass;
  }

  public String getCssClass() {
    return cssClass;
  }

  /**
   * このカテゴリが保持するノードの ID 一覧に ID を追加する.
   *
   * @param id 追加する ID
   */
  public void addNodeId(BhNodeId id) {
    nodeIds.add(id);
  }

  /** このカテゴリが保持するノードの ID 一覧を返す. */
  public SequencedSet<BhNodeId> getNodeIds() {
    return new LinkedHashSet<>(nodeIds);
  }
}
