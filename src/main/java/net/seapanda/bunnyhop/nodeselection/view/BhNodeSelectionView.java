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

package net.seapanda.bunnyhop.nodeselection.view;

import java.util.SequencedSet;
import javafx.scene.layout.Region;
import net.seapanda.bunnyhop.node.view.BhNodeView;

/**
 * テンプレートノードを表示するビューの機能を規定したインタフェース.
 *
 * @author K.Koike
 */
public interface BhNodeSelectionView {

  /** このビューの {@link Region} オブジェクトを取得する. */
  Region getRegion();

  /**
   * このビューのカテゴリ名を取得する.
   *
   * @return このビューのカテゴリ名
   */
  String getCategoryName();

  /**
   * このノード選択ビューに対し {@code view} をルートとして指定する.
   *
   * @param view ルートとして指定するビュー
   */
  void specifyNodeViewAsRoot(BhNodeView view);

  /**
   * このノード選択ビューに対し {@code view} を非ルートノードとして指定する.
   *
   * @param view 非ルートとして指定するビュー
   */
  void specifyNodeViewAsNotRoot(BhNodeView view);

  /**
   * {@code view} をこのワークスペースビューに追加する.
   *
   * @param view 追加する {@link BhNodeView}
   */
  void addNodeView(BhNodeView view);

  /**
   * {@code view} をこのワークスペースビューから削除する.
   *
   * @param view 削除する {@link BhNodeView}
   */
  void removeNodeView(BhNodeView view);

  /** このオブジェクトが現在保持する {@link BhNodeView} のツリーの数を取得する. */
  long getNumNodeViewTrees();

  /**
   * ノード選択ビューのズーム処理を行う.
   *
   * @param zoomIn 拡大する場合 true
   */
  void zoom(boolean zoomIn);

  /** ノード選択ビューの拡大率を設定する. */
  void zoom(int level);

  /** 表示するノードを並べる. */
  void arrange();

  /**
   * このオブジェクトに登録したノードをリストに格納して返す.
   *
   * @return このオブジェクトに登録したノードのリスト.
   */
  SequencedSet<BhNodeView> getNodeViewList();

  /**
   * このビューを表示する.
   */
  void show();

  /**
   * このビューを非表示にする.
   */
  void hide();

  /**
   * このビューの現在の可視性を取得する.
   *
   * @return このビューの可視性.
   */
  boolean isShowed();
}
