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


package net.seapanda.bunnyhop.view.proxy;

import net.seapanda.bunnyhop.model.node.BhNode;
import net.seapanda.bunnyhop.utility.Vec2D;
import net.seapanda.bunnyhop.view.node.BhNodeView;
import net.seapanda.bunnyhop.view.workspace.WorkspaceView;

/**
 * {@link WorkspaceView} を操作するプロキシクラスのインタフェース.
 *
 * @author K.Koike
 */
public interface WorkspaceViewProxy {

  /** 操作対象の {@link BhNodeView} を取得する. */
  public default WorkspaceView getView() {
    return null;
  }

  /** {@link WorkspaceView} に対し, ワークスペースの {@code node} がルートノードとして指定されたことを通知する. */
  public default void notifyNodeSpecifiedAsRoot(BhNode node) {}

  /** {@link WorkspaceView} に対し, ワークスペースの {@code node} がルートノードの指定を解除されたことを通知する. */
  public default void notifyNodeSpecifiedAsNotRoot(BhNode node) {}

  /** {@link WorkspaceView} に対し, ワークスペースに {@code node} が追加されたことを通知する. */
  public default void notifyNodeAdded(BhNode node) {}

  /** {@link WorkspaceView} に対し, ワークスペースから {@code node} が削除されたことを通知する. */
  public default void notifyNodeRemoved(BhNode node) {}

  /**
   * {@link WorkspaceView} のサイズを変更する.
   *
   * @param widen 広げる場合 true, 狭める場合 false
   */
  public default void changeViewSize(boolean widen) {}


  /** {@link WorkspaceView} のサイズを取得する. */
  public default Vec2D getViewSize() {
    return null;
  }

  /**
   * {@link javafx.scene.Scene} 上の座標を {@link WorkspaceView} 上の位置に変換して返す.
   *
   * @param posOnScene {@link javafx.scene.Scene} 上の座標
   * @return {@code posOnScene} の {@link WorkspaceView} 上の座標.
   */
  public default Vec2D sceneToWorkspace(Vec2D posOnScene) {
    return null;
  }

  /**
   * {@link WorkspaceView} の表示の拡大/縮小処理を行う.
   *
   * @param zoomIn 拡大する場合 true
   */
  public default void zoom(boolean zoomIn) {}

  /** {@link WorkspaceView} の表示の拡大率を設定する. */
  public default void setZoomLevel(int level) {}
}