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

package net.seapanda.bunnyhop.node.view.service;

import java.util.Optional;
import javafx.geometry.Bounds;
import net.seapanda.bunnyhop.node.model.BhNode;
import net.seapanda.bunnyhop.node.view.BhNodeView;
import net.seapanda.bunnyhop.utility.math.Vec2D;
import net.seapanda.bunnyhop.workspace.view.WorkspaceView;

/**
 * {@link BhNode} が属するワークスペースビューと位置を格納するクラス.
 *
 * @author K.Koike
 */
public class BhNodeLocation {

  /** このオブジェクトが位置情報を保持する {@link BhNode}. */
  public final BhNode node;
  /** このオブジェクトを作成した時点で {@code node} が属しているワークスペースビュー. (nullable) */
  public final WorkspaceView wsView;
  /** このオブジェクトを作成した時点でのワークスペース上における {@code node} の原点の位置. (nullable) */
  public final Vec2D origin;
  /** このオブジェクトを作成した時点でのワークスペース上における {@code node} の中心の位置. (nullable) */
  public final Vec2D center;

  /**
   *  コンストラクタ.
   *
   * @param node この {@link BhNode} の位置とワークスペースビューをこのオブジェクトに格納する. <br>
   *             null を指定した場合, {@link #node}, {@link #wsView}, {@link #origin},
   *             {@link #center} は null となる.
   */
  public BhNodeLocation(BhNode node) {
    this.node = node;
    wsView = Optional.ofNullable(node)
        .flatMap(BhNode::getView)
        .map(BhNodeView::getWorkspaceView)
        .orElse(null);
    Bounds bounds = Optional.ofNullable(node)
        .flatMap(BhNode::getView)
        .map(view -> view.getPositionManager().getBounds())
        .orElse(null);
    if (bounds == null) {
      origin = null;
      center = null;
      return;
    }
    origin = new Vec2D(bounds.getMinX(), bounds.getMinY());
    center = new Vec2D(bounds.getCenterX(), bounds.getCenterY());
  }

  public BhNodeLocation() {
    this(null);
  }
}
