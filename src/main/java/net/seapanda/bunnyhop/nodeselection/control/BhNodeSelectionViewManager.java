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


package net.seapanda.bunnyhop.nodeselection.control;

import java.util.Optional;
import net.seapanda.bunnyhop.nodeselection.view.BhNodeSelectionView;
import net.seapanda.bunnyhop.utility.event.ConsumerInvoker;

/**
 * 複数のノード選択ビューの登録・表示切替・拡大縮小を管理する機能を規定したインタフェース.
 *
 * @author K.Koike
 */
public interface BhNodeSelectionViewManager {

  /**
   * {@code view} をノード選択ビュー一覧に登録する.
   *
   * <p>{@code view} が既に登録済みの場合, 何もしない.
   *
   * <p>{@code view} と同じカテゴリ名の {@link BhNodeSelectionView} が既に登録されている場合,
   * 古いビューは一覧から取り除かれる.
   *
   * @param view 登録するノード選択ビュー
   */
  void addNodeSelectionView(BhNodeSelectionView view);

  /**
   * {@code view} をノード選択ビュー一覧から取り除く.
   *
   * <p>{@code view} が一覧に無い場合, 何もしない.
   *
   * @param view 取り除くノード選択ビュー
   */
  void removeNodeSelectionView(BhNodeSelectionView view);

  /**
   * 引数で指定したノード選択ビューを取得する.
   *
   * @param categoryName 取得するノード選択ビューのカテゴリ名
   * @return カテゴリ名が {@code categoryName} であるノード選択ビュー
   */
  Optional<BhNodeSelectionView> getNodeSelectionView(String categoryName);

  /**
   * 現在表示されているノード選択ビューを取得する.
   *
   * @return 現在表示されているノード選択ビュー
   */
  Optional<BhNodeSelectionView> getCurrentNodeSelectionView();

  /**
   * 全てのノード選択ビューを拡大もしくは縮小する.
   *
   * @param zoomIn 拡大する場合 true
   */
  void zoom(boolean zoomIn);

  /** 全てのノード選択ビューの拡大率を設定する. */
  void zoom(int level);

  /**
   * {@code categoryName} で指定したカテゴリのノード選択ビューを表示する.
   *
   * @param categoryName 表示するノード選択ビューのカテゴリ名
   */
  void show(String categoryName);

  /** 表示中のノード選択ビューを非表示にする. */
  void hideCurrentView();

  /** {@link BhNodeSelectionViewManager} に対するイベントハンドラの追加と削除を行うオブジェクトを返す. */
  CallbackRegistry getCallbackRegistry();


  /** {@link BhNodeSelectionViewManager} に対してイベントハンドラを追加または削除する機能を規定したインタフェース. */
  interface CallbackRegistry {
    /** 選択中の BhNode のカテゴリが変更されたときのイベントハンドラのレジストリを取得する. */
    ConsumerInvoker<CurrentCategoryChangedEvent>.Registry getOnCurrentCategoryChanged();
  }

  /**
   * 現在表示されている BhNode のカテゴリが変更されたときの情報を格納したレコード.
   *
   * @param nodeSelViewManager 選択中の BhNode のカテゴリが変更された {@link BhNodeSelectionViewManager}
   * @param oldVal 変更前の BhNode のカテゴリ名 (nullable)
   * @param newVal 変更後の BhNode のカテゴリ名 (nullable)
   */
  record CurrentCategoryChangedEvent(
      BhNodeSelectionViewManager nodeSelViewManager, String oldVal, String newVal) {}
}
