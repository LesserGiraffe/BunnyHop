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

package net.seapanda.bunnyhop.node.view.effect;

import net.seapanda.bunnyhop.node.view.BhNodeView;
import net.seapanda.bunnyhop.service.undo.UserOperation;
import net.seapanda.bunnyhop.workspace.model.Workspace;

/**
 * 複数の {@link BhNodeView} を対象にする操作を提供するクラス.
 *
 * @author  K.Koike
 */
public interface VisualEffectManager {

  /**
   * {@code view} を起点とする {@link BhNodeView} 群の視覚効果の有効 / 無効を切り替える.
   *
   * @param view 視覚効果の有効 / 無効を切り替える {@link BhNodeView}
   * @param enable 視覚効果を有効にする場合 true
   * @param type 有効または無効にする視覚効果の種類
   * @param target 視覚効果の有効または無効を切り替える対象を指定する列挙子
   * @param userOpe undo 用コマンドオブジェクト
   */
  void setEffectEnabled(
      BhNodeView view,
      boolean enable,
      VisualEffectType type,
      VisualEffectTarget target,
      UserOperation userOpe);

  /**
   * {@code view} を起点とする {@link BhNodeView} 群の視覚効果の有効 / 無効を切り替える.
   *
   * @param view 視覚効果の有効 / 無効を切り替える {@link BhNodeView}
   * @param enable 視覚効果を有効にする場合 true
   * @param type 有効または無効にする視覚効果の種類
   * @param target 視覚効果の有効または無効を切り替える対象を指定する列挙子
   */
  void setEffectEnabled(
      BhNodeView view, boolean enable, VisualEffectType type, VisualEffectTarget target);

  /**
   * {@code view} の視覚効果の有効 / 無効を切り替える.
   *
   * @param view 視覚効果の有効 / 無効を切り替える {@link BhNodeView}
   * @param enable 視覚効果を有効にする場合 true
   * @param type 有効または無効にする視覚効果の種類
   * @param userOpe undo 用コマンドオブジェクト
   */
  void setEffectEnabled(
      BhNodeView view, boolean enable, VisualEffectType type, UserOperation userOpe);

  /**
   * {@code view} の視覚効果の有効 / 無効を切り替える.
   *
   * @param view 視覚効果の有効 / 無効を切り替える {@link BhNodeView}
   * @param enable 視覚効果を有効にする場合 true
   * @param type 有効または無効にする視覚効果の種類
   */
  void setEffectEnabled(BhNodeView view, boolean enable, VisualEffectType type);

  /**
   * {@code ws} で指定したワークスペースの全てのノードの視覚効果 ({@code type}) を無効にする.
   *
   * @param ws このワークスペースのノードの視覚効果を無効にする
   * @param type 無効にする視覚効果の種類
   * @param userOpe undo 用コマンドオブジェクト
   */
  void disableEffects(Workspace ws, VisualEffectType type, UserOperation userOpe);

  /**
   * {@code ws} で指定したワークスペースの全てのノードの視覚効果 ({@code type}) を無効にする.
   *
   * @param ws このワークスペースのノードの視覚効果を無効にする
   * @param type 無効にする視覚効果の種類
   */
  void disableEffects(Workspace ws, VisualEffectType type);

  /**
   * 全てのノードの視覚効果 ({@code type}) を無効にする.
   *
   * @param type 無効にする視覚効果の種類
   * @param userOpe undo 用コマンドオブジェクト
   */
  void disableEffects(VisualEffectType type, UserOperation userOpe);

  /**
   * 全てのノードの視覚効果 ({@code type}) を無効にする.
   *
   * @param type 無効にする視覚効果の種類
   */
  void disableEffects(VisualEffectType type);
}
