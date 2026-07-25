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

package net.seapanda.bunnyhop.node.view;


/**
 * このノードを根とする部分木のサイズの更新状態を管理するクラス.
 *
 * @author K.koike
 */
class SizeChangeNotifier {

  private final BhNodeViewBase view;
  /** ノードのサイズが最新の値であることを示すフラグ. */
  private boolean isNodeSizeUpToDate = true;
  private Runnable callback;


  SizeChangeNotifier(BhNodeViewBase view) {
    this.view = view;
  }

  /**
   * このノードを根とする部分木のサイズが変化したことを通知する.
   *
   * <p>未反映の通知がない場合にだけサイズ更新フラグを無効化し, 親グループへ変更を伝搬する.
   * ルートノードであれば再配置も要求し, 最後にサイズ変化時のコールバックを呼び出す.
   */
  void notifySubtreeSizeChanged() {
    if (!isNodeSizeUpToDate) {
      return;
    }
    BhNodeViewGroup group = view.getTreeControl().getParentGroup();
    if (group != null) {
      group.getGeometry().notifySubTreeSizeChanged();
    }
    if (view.getTreeControl().isRoot()) {
      view.getArrangement().requestArrangement();
    }
    isNodeSizeUpToDate = false;
    callback.run();
  }


  /** このノードを根とする部分木のサイズの更新状態を最新にする. */
  void markSubtreeSizeUpToDate() {
    NvbCallbackInvoker.invoke(
        nodeView -> nodeView.getSizeChangeNotifier().isNodeSizeUpToDate = true,
        view);
  }

  /** 子孫ノードのサイズが変わったときに呼び出す関数を設定する. */
  void setOnDescendantSizeChanged(Runnable callback) {
    this.callback = callback;
  }
}
