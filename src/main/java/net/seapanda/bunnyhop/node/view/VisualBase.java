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

import static javafx.css.PseudoClass.getPseudoClass;
import static net.seapanda.bunnyhop.common.configuration.BhConstants.Css.Pseudo.EXEC_STEP;
import static net.seapanda.bunnyhop.common.configuration.BhConstants.Css.Pseudo.JUMP_TARGET;
import static net.seapanda.bunnyhop.common.configuration.BhConstants.Css.Pseudo.MOVE_GROUP;
import static net.seapanda.bunnyhop.common.configuration.BhConstants.Css.Pseudo.OVERLAPPED;
import static net.seapanda.bunnyhop.common.configuration.BhConstants.Css.Pseudo.RELATED_NODE_GROUP;
import static net.seapanda.bunnyhop.common.configuration.BhConstants.Css.Pseudo.RUNTIME_ERROR;
import static net.seapanda.bunnyhop.common.configuration.BhConstants.Css.Pseudo.SEARCH_RESULT;
import static net.seapanda.bunnyhop.common.configuration.BhConstants.Css.Pseudo.SELECTED;
import static net.seapanda.bunnyhop.node.view.BhNodeViewBase.Shapes;

import java.util.HashSet;
import java.util.Set;
import net.seapanda.bunnyhop.node.view.effect.VisualEffectType;

/**
 * ノードビューの視覚効果に関する機能を提供するクラス.
 *
 * @author K.Koike
 */
abstract class VisualBase implements BhNodeView.Visual {

  private final BhNodeViewBase view;
  /** 現在適用されている視覚効果. */
  private final Set<VisualEffectType> appliedEffects = new HashSet<>();

  VisualBase(BhNodeViewBase view) {
    this.view = view;
  }

  /**
   * ノードビューのボディ部分に適用される CSS クラスを追加する.
   *
   * @param classNames css クラス名
   */
  void addCssClass(String... classNames) {
    for (var cssClassName : classNames) {
      view.getShapes().nodeShape().getStyleClass().add(cssClassName);
    }
  }

  /**
   * ノードビューの CSS の擬似クラスの有効無効を切り替える.
   *
   * @param enable 擬似クラスを有効にする場合 true
   * @param className 有効/無効を切り替える擬似クラス名
   */
  void setPseudoClassState(boolean enable, String className) {
    view.getShapes().nodeShape().pseudoClassStateChanged(getPseudoClass(className), enable);
  }

  @Override
  public void setVisible(boolean visible) {
    NvbCallbackInvoker.invoke(view -> view.getPanes().root().setVisible(visible), view);
  }

  @Override
  public void setEffectEnabled(boolean enable, VisualEffectType type) {
    if (enable) {
      enableEffect(type);
    } else {
      disableEffect(type);
    }
  }

  /** {@code type} で指定した視覚効果を有効にする. */
  private void enableEffect(VisualEffectType type) {
    Shapes shapes = view.getShapes();
    appliedEffects.add(type);
    switch (type) {
      case SELECTION -> setPseudoClassState(true, SELECTED);
      case MOVE_GROUP -> setPseudoClassState(true, MOVE_GROUP);
      case OVERLAP -> setPseudoClassState(true, OVERLAPPED);
      case SEARCH_RESULT -> view.getComponent().ifPresent(
            node -> node.pseudoClassStateChanged(getPseudoClass(SEARCH_RESULT), true));
      case NEXT_STEP -> {
        shapes.nextStep().setVisible(true);
        setPseudoClassState(true, EXEC_STEP);
      }
      case RUNTIME_ERROR -> {
        shapes.runtimeError().setVisible(true);
        setPseudoClassState(true, RUNTIME_ERROR);
      }
      case RELATED_NODE_GROUP -> setPseudoClassState(true, RELATED_NODE_GROUP);
      case JUMP_TARGET -> setPseudoClassState(true, JUMP_TARGET);
      case BREAKPOINT -> shapes.breakpoint().setVisible(true);
      case CORRUPTION -> shapes.corruption().setVisible(true);
      case ENTRY_POINT -> shapes.entryPoint().setVisible(true);
      case COMPILE_ERROR -> shapes.compileError().setVisible(true);
      default -> throw new AssertionError("Invalid Visual Effect " + type);
    }
  }

  /** {@code type} で指定した視覚効果を無効にする. */
  private void disableEffect(VisualEffectType type) {
    Shapes shapes = view.getShapes();
    appliedEffects.remove(type);
    switch (type) {
      case SELECTION -> setPseudoClassState(false, SELECTED);
      case MOVE_GROUP -> setPseudoClassState(false, MOVE_GROUP);
      case OVERLAP -> setPseudoClassState(false, OVERLAPPED);
      case SEARCH_RESULT -> view.getComponent().ifPresent(
            node -> node.pseudoClassStateChanged(getPseudoClass(SEARCH_RESULT), false));
      case NEXT_STEP -> {
        shapes.nextStep().setVisible(false);
        setPseudoClassState(false, EXEC_STEP);
      }
      case RUNTIME_ERROR -> {
        shapes.runtimeError().setVisible(false);
        setPseudoClassState(false, RUNTIME_ERROR);
      }
      case RELATED_NODE_GROUP -> setPseudoClassState(false, RELATED_NODE_GROUP);
      case JUMP_TARGET -> setPseudoClassState(false, JUMP_TARGET);
      case BREAKPOINT -> shapes.breakpoint().setVisible(false);
      case CORRUPTION -> shapes.corruption().setVisible(false);
      case ENTRY_POINT -> shapes.entryPoint().setVisible(false);
      case COMPILE_ERROR -> shapes.compileError().setVisible(false);
      default -> throw new AssertionError("Invalid Visual Effect " + type);
    }
  }

  @Override
  public Set<VisualEffectType> getAppliedEffects() {
    return new HashSet<>(appliedEffects);
  }

  @Override
  public boolean isEffectEnabled(VisualEffectType effect) {
    return appliedEffects.contains(effect);
  }
}
