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
import net.seapanda.bunnyhop.undo.UserOperation;
import net.seapanda.bunnyhop.utility.Vec2D;
import net.seapanda.bunnyhop.view.node.BhNodeView;

/**
 * {@link BhNodeView} を操作するプロキシクラスのインタフェース.
 *
 * @author K.Koike
 */
public interface BhNodeViewProxy {
  
  /** 操作対象の {@link BhNodeView} を取得する. */
  public default BhNodeView getView() {
    return null;
  }

  /** {@link BhNodeView} のワークスペース上の位置を取得する. */
  public default Vec2D getPosOnWorkspace() {
    return null;
  }

  /** {@link BhNodeView} のワークスペース上の位置を設定する. */
  public default void setPosOnWorkspace(Vec2D pos, UserOperation userOpe) {}

  /** {@link BhNodeView} を {@code distance} 分移動させる. */
  public default void move(Vec2D distance) {}

  /**
   * ボディ部分に末尾までの全外部ノードを加えた大きさを返す.
   *
   * @param includeCnctr コネクタ部分も含む場合 true
   */
  public default Vec2D getSizeIncludingOuters(boolean includeCnctr) {
    return null;
  }

  /**
   * ビューの木構造上で, 制御対象の {@link BhNodeView} を {@code newNode} の {@link BhNodeView} と入れ替える.
   * GUI コンポーネントのツリーからは取り除かない.
   *
   * @param newNode このノードの {@link BhNodeView} を制御対象の {@link BhNodeView} と入れ替える.
   */
  public default void replace(BhNode newNode, UserOperation userOpe) {}

  /**
   * {@code} で指定した css の疑似クラスの状態を変更する.
   *
   * @param className 疑似クラスの名前
   * @param enable 疑似クラスを有効にする場合 true.  無効にする場合 false.
   */
  public default void switchPseudoClassState(String className, boolean enable) {}

  /** {@link BhNodeView} を GUI コンポーネントのツリーから取り除く. */
  public default void removeFromGuiTree() {}

  /** コンパイルエラー表示の可視性を変更する. */
  public default void setCompileErrorVisibility(boolean visible, UserOperation userOpe) {}

  /** テンプレートノードかどうか調べる. */
  public default boolean isTemplateNode() {
    return false;
  }

  /** ビューを持っているか調べる. */
  public default boolean hasView() {
    return false;
  }

  /**
   * {@link BhNodeView} をワークスペースの中央に表示する.
   * {@link BhNodeView} がワークスペースに存在しない場合なにもしない.
   *
   */
  public default void lookAt() {}
}
