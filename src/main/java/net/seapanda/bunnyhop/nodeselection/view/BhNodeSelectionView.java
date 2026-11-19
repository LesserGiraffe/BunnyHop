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
import net.seapanda.bunnyhop.utility.event.ConsumerInvoker;
import net.seapanda.bunnyhop.workspace.model.Workspace;
import net.seapanda.bunnyhop.workspace.view.BhNodeViewContainer;

/**
 * テンプレートノードを表示するビュー (= ノード選択ビュー) の機能を規定したインタフェース.
 *
 * @author K.Koike
 */
public interface BhNodeSelectionView extends BhNodeViewContainer {

  /** このビューの {@link Region} オブジェクトを取得する. */
  Region getRegion();

  /**
   * このビューのカテゴリ名を取得する.
   *
   * @return このビューのカテゴリ名
   */
  String getCategoryName();

  /**
   * テンプレートノードが所属するワークスペースを取得する.
   *
   * @return テンプレートノードが所属するワークスペース
   */
  Workspace getWorkspace();

  /** このオブジェクトが現在保持する {@link BhNodeView} のツリーの数を取得する. */
  long getNumNodeViewTrees();

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

  /**
   * このノード選択ビューに対するイベントハンドラの追加と削除を行うオブジェクトを返す.
   *
   * @return このノード選択ビューに対するイベントハンドラの追加と削除を行うオブジェクト
   */
  CallbackRegistry getCallbackRegistry();


  /** {@link BhNodeSelectionView} に対してイベントハンドラを追加または削除する機能を規定したインタフェース. */
  interface CallbackRegistry {

    /** 関連するノード選択ビューにノードビューが追加されたときのイベントハンドラのレジストリを取得する. */
    ConsumerInvoker<NodeAddedEvent>.Registry getOnNodeAdded();

    /** 関連するノード選択ビューからノードビューが削除されたときのイベントハンドラのレジストリを取得する. */
    ConsumerInvoker<NodeRemovedEvent>.Registry getOnNodeRemoved();

    /** 関連するノード選択ビューの可視性が変更されたときのイベントハンドラのレジストリを取得する. */
    ConsumerInvoker<VisibilityChangedEvent>.Registry getOnVisibilityChanged();
  }

  /**
   * ノード選択ビューノードビューが追加されたときの情報を格納したレコード.
   *
   * @param nodeSelectionView {@code nodeView} が追加されたノード選択ビュー
   * @param nodeView {@code wsView} に追加されたノードビュー
   */
  record NodeAddedEvent(BhNodeSelectionView nodeSelectionView, BhNodeView nodeView) {}

  /**
   * ノード選択ビューからノードビューが削除されたときの情報を格納したレコード.
   *
   * @param nodeSelectionView {@code nodeView} が追加されたノード選択ビュー
   * @param nodeView {@code wsView} に追加されたノードビュー
   */
  record NodeRemovedEvent(BhNodeSelectionView nodeSelectionView, BhNodeView nodeView) {}

  /**
   * ノード選択ビューの可視性が変更されたときの情報を格納したレコード.
   *
   * @param nodeSelectionView 可視性が変更されたノード選択ビュー
   * @param isVisible {@code nodeSelectionView} が表示されたとき true
   */
  record VisibilityChangedEvent(BhNodeSelectionView nodeSelectionView, boolean isVisible) {}
}
