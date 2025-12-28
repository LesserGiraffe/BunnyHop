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

import java.util.LinkedHashSet;
import java.util.function.Consumer;
import net.seapanda.bunnyhop.node.view.BhNodeView;
import net.seapanda.bunnyhop.node.view.effect.VisualEffectManager;
import net.seapanda.bunnyhop.node.view.effect.VisualEffectTarget;
import net.seapanda.bunnyhop.node.view.effect.VisualEffectType;
import net.seapanda.bunnyhop.node.view.traverse.CallbackInvoker;
import net.seapanda.bunnyhop.service.undo.UserOperation;
import net.seapanda.bunnyhop.workspace.model.Workspace;
import net.seapanda.bunnyhop.workspace.model.WorkspaceSet;
import net.seapanda.bunnyhop.workspace.view.WorkspaceView;

/**
 * 複数の {@link BhNodeView} を対象にする操作を提供するクラス.
 *
 * @author  K.Koike
 */
public class BhNodeViewSupervisor implements VisualEffectManager {

  private final WorkspaceSet wss;

  /** コンストラクタ. */
  public BhNodeViewSupervisor(WorkspaceSet wss) {
    this.wss = wss;
  }

  @Override
  public void setEffectEnabled(
      BhNodeView view,
      boolean enable,
      VisualEffectType type,
      VisualEffectTarget target,
      UserOperation userOpe) {
    Consumer<? super BhNodeView> renderer =
        nodeView -> setEffectEnabled(nodeView, enable, type, userOpe);
    applyVisualEffect(view, target, renderer);
  }

  @Override
  public void setEffectEnabled(
      BhNodeView view, boolean enable, VisualEffectType type, VisualEffectTarget target) {
    setEffectEnabled(view, enable, type, target, new UserOperation());
  }

  @Override
  public void setEffectEnabled(
      BhNodeView view, boolean enable, VisualEffectType type, UserOperation userOpe) {
    if (view.getLookManager().isEffectEnabled(type) == enable) {
      return;
    }
    view.getLookManager().setEffectEnabled(enable, type);
    userOpe.pushCmd(ope -> setEffectEnabled(view, !enable, type, ope));
  }

  @Override
  public void setEffectEnabled(BhNodeView view, boolean enable, VisualEffectType type) {
    setEffectEnabled(view, enable, type, new UserOperation());
  }

  @Override
  public void disableEffects(Workspace ws, VisualEffectType type, UserOperation userOpe) {
    ws.getView()
        .map(WorkspaceView::getRootNodeViews)
        .orElse(new LinkedHashSet<>())
        .forEach(nodeView -> {
          Consumer<BhNodeView> renderer = view -> setEffectEnabled(view, false, type, userOpe);
          applyVisualEffect(nodeView, VisualEffectTarget.CHILDREN, renderer);
        });
  }

  @Override
  public void disableEffects(Workspace ws, VisualEffectType type) {
    disableEffects(ws, type, new UserOperation());
  }

  @Override
  public void disableEffects(VisualEffectType type, UserOperation userOpe) {
    wss.getWorkspaces().forEach(ws -> disableEffects(ws, type, userOpe));
  }

  @Override
  public void disableEffects(VisualEffectType type) {
    var userOpe = new UserOperation();
    wss.getWorkspaces().forEach(ws -> disableEffects(ws, type, userOpe));
  }

  /**
   * {@code target} で指定した対象に視覚効果を与える処理を適用する.
   *
   * @param view {@code target} の起点となる {@link BhNodeView}
   * @param target エフェクトを適用するターゲット
   * @param renderer エフェクトを描画するメソッド
   */
  private static void applyVisualEffect(
      BhNodeView view, VisualEffectTarget target, Consumer<? super BhNodeView> renderer) {
    switch (target) {
      case SELF -> renderer.accept(view);
      case CHILDREN -> CallbackInvoker.invoke(renderer, view);
      case OUTERS -> CallbackInvoker.invokeForOuters(renderer, view);
      default ->
          throw new IllegalArgumentException("Invalid Rendering Target {%s}".formatted(target));
    }
  }
}
