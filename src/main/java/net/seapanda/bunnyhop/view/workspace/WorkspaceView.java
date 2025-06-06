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

package net.seapanda.bunnyhop.view.workspace;

import java.util.List;
import java.util.SequencedSet;
import java.util.function.Supplier;
import javafx.scene.input.MouseEvent;
import net.seapanda.bunnyhop.model.workspace.Workspace;
import net.seapanda.bunnyhop.quadtree.QuadTreeRectangle;
import net.seapanda.bunnyhop.quadtree.QuadTreeRectangle.OverlapOption;
import net.seapanda.bunnyhop.utility.function.ConsumerInvoker;
import net.seapanda.bunnyhop.utility.math.Vec2D;
import net.seapanda.bunnyhop.view.node.BhNodeView;

/**
 * {@link Workspace} のビューが持つ機能を規定したインタフェース.
 *
 * @author K.Koike
 */
public interface WorkspaceView {

  /**
   * このワークスペースビューに対し {@code view} をルートとして指定する.
   *
   * @param view ルートとして指定するビュー
   */
  void specifyNodeViewAsRoot(BhNodeView view);

  /**
   * このワークスペースビューに対し {@code view} がルートノードとして指定されているとき, その指定を解除する.
   *
   * @param view ルートの指定を解除するするビュー
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

  /**
   * 引数で指定した矩形と重なるこのワークスペースビュー上にあるノードを探す.
   *
   * @param rect この矩形と重なるノードを探す.
   * @param overlapWithBodyPart ノードのボディ部分と重なるノードを探す場合 true.
   *                            ノードのコネクタ部分と重なるノードを探す場合 false.
   * @param option 検索オプション
   * @return 引数の矩形と重なるノードのビュー
   */
  List<BhNodeView> searchForOverlappedNodeViews(
      QuadTreeRectangle rect, boolean overlapWithBodyPart, OverlapOption option);

  /**
   * ワークスペースビューの大きさを返す.
   *
   * @return ワークスペースビューの大きさ
   */
  Vec2D getSize();

  /**
   * このビューに対応している {@link Workspace} を返す.
   *
   * @return このビューに対応している {@link Workspace}
   */
  Workspace getWorkspace();

  /**
   * ワークスペースビューの大きさを変える.
   *
   * @param widen ワークスペースビューの大きさを大きくする場合 true
   */
  void changeViewSize(boolean widen);

  /**
   * Scene上の座標をWorkspace上の位置に変換して返す.
   *
   * @param x Scene座標の変換したいX位置
   * @param y Scene座標の変換したいY位置
   * @return 引数の座標のWorkspace上の位置
   */
  Vec2D sceneToWorkspace(double x, double y);

  /**
   * Scene上の座標をWorkspace上の位置に変換して返す.
   *
   * @param pos Scene 座標の変換したい位置
   * @return 引数の座標のWorkspace上の位置
   */
  Vec2D sceneToWorkspace(Vec2D pos);

  /**
   * ワークスペースビューのズーム処理を行う.
   *
   * @param zoomIn 拡大処理を行う場合true
   */
  void zoom(boolean zoomIn);

  /**ワークスペースビューの表示の拡大率を設定する. */
  void setZoomLevel(int level);

  /** 
   * ノードシフタをワークスペースビューに追加する.
   * ノードシフタ: 複数ノードを同時に移動させる GUI 部品
   */
  void addNodeShifterView(NodeShifterView view);

  /**
   * 矩形選択ツールを表示する.
   *
   * @param upperLeft 表示する矩形のワークスペースビュー上の左上の座標
   * @param lowerRight 表示する矩形のワークスペースビュー上の右下の座標
   */
  void showSelectionRectangle(Vec2D upperLeft, Vec2D lowerRight);

  /** 矩形選択ツールを非表示にする. */
  void hideSelectionRectangle();

  /**
   * {@code nodeView} が中央に表示されるようにスクロールする.
   * {@code nodeView} がこのワークスペースビューに存在しない場合なにもしない.
   *
   * @param view 中央に表示するノードビュー
   */
  void lookAt(BhNodeView view);

  /** このワークスペースビューが持つ全てのルートノードビューを取得する. */
  SequencedSet<BhNodeView> getRootNodeViews();

  /**
   * {@code view} を含むノードビューツリー全体をこのワークスペースビュー上で最前に移動させる.
   * {@code view} を含むノードビューツリーのルートノードビューがこのワークスペースビューにない場合, 何もしない.
   *
   * @param view このノードビューを含むノードビューツリー全体をこのワークスペースビュー上で最前に移動させる.
   */
  void moveNodeViewToFront(BhNodeView view);

  /**
   * このワークスペースビューに対するイベントハンドラの追加と削除を行うオブジェクトを返す.
   *
   * @return このワークスペースビューに対するイベントハンドラの追加と削除を行うオブジェクト
   */
  CallbackRegistry getCallbackRegistry();

  /** {@link WorkspaceView} に対してイベントハンドラを追加または削除する機能を規定したインタフェース. */
  public interface CallbackRegistry {
    
    /** 関連するワークスペースビュー上でマウスボタンが押下されたときのイベントハンドラのレジストリを取得する. */
    ConsumerInvoker<MouseEventInfo>.Registry getOnMousePressed();

    /** 関連するワークスペースビュー上でマウスがドラッグされたときのイベントハンドラのレジストリを取得する. */
    ConsumerInvoker<MouseEventInfo>.Registry getOnMouseDragged();

    /** 関連するワークスペースビュー上でマウスボタンが離されたときのイベントハンドラのレジストリを取得する. */
    ConsumerInvoker<MouseEventInfo>.Registry getOnMouseReleased();

    /** 関連するワークスペースビューのノードビューの位置が変更されたときのイベントハンドラのレジストリを取得する. */
    ConsumerInvoker<NodeMoveEvent>.Registry getOnNodeMoved();

    /** 関連するワークスペースビューのノードビューのサイズが変更されたときのイベントハンドラのレジストリを取得する. */
    ConsumerInvoker<NodeSizeChangedEvent>.Registry getOnNodeSizeChanged();

    /**
     * 関連するワークスペースビューを閉じるリクエストを受け取ったときに呼ぶイベントハンドラを設定する.
     * 
     * <p>イベントハンドラの戻り値が false であった場合, ワークスペースビューを閉じるリクエストをキャンセルする.
     *
     * @param handler 設定するイベントハンドラ (nullable)
     */
    void setOnCloseRequested(Supplier<? extends Boolean> handler);

    /** 関連するワークスペースビューが閉じられたときのイベントハンドラのレジストリを取得する. */
    ConsumerInvoker<CloseEvent>.Registry getOnClosed();
  }

  /**
   * ワークスペースビュー上でのマウス操作の情報を格納したレコード.
   *
   * @param view マウス操作が行われたワークスペースビュー
   * @param event マウス操作の情報を格納したオブジェクト
   */
  record MouseEventInfo(WorkspaceView view, MouseEvent event) {}

  /**
   * ワークスペースビュー上でノードビューの位置が変更されたときの情報を格納したレコード.
   *
   * @param wsView {@code nodeView } を保持するワークスペースビュー
   * @param nodeView 位置が変更されたノードビュー
   */
  record NodeMoveEvent(WorkspaceView wsView, BhNodeView nodeView) {}

  /**
   * ワークスペースビュー上でノードビューのサイズが変更されたときの情報を格納したレコード.
   *
   * @param wsView {@code nodeView } を保持するワークスペースビュー
   * @param nodeView サイズが変更されたノードビュー
   */
  record NodeSizeChangedEvent(WorkspaceView wsView, BhNodeView nodeView) {}

  /**
   * ワークスペースビューが閉じられたときの情報を格納したレコード.
   *
   * @param view 閉じられたワークスペースビュー
   */
  record CloseEvent(WorkspaceView view) {}
}
