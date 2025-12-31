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

import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import net.seapanda.bunnyhop.node.view.BhNodeView;
import net.seapanda.bunnyhop.node.view.effect.VisualEffectManager;
import net.seapanda.bunnyhop.node.view.effect.VisualEffectTarget;
import net.seapanda.bunnyhop.node.view.effect.VisualEffectType;
import net.seapanda.bunnyhop.node.view.traverse.CallbackInvoker;
import net.seapanda.bunnyhop.service.undo.UserOperation;
import net.seapanda.bunnyhop.workspace.model.Workspace;
import net.seapanda.bunnyhop.workspace.model.WorkspaceSet;

/**
 * 複数の {@link BhNodeView} を対象にする操作を提供するクラス.
 *
 * @author  K.Koike
 */
public class BhNodeViewSupervisor implements VisualEffectManager {

  private final WorkspaceSet wss;
  /**
   * 視覚効果ごとにそれが適用されているノードを格納するマップ.
   * 視覚効果の無効化を高速に行うために導入する.
   */
  private final Map<VisualEffectType, Set<BhNodeView>> effectToNodes = new HashMap<>();

  /** コンストラクタ. */
  public BhNodeViewSupervisor(WorkspaceSet wss) {
    this.wss = wss;
    Arrays.stream(VisualEffectType.values())
        .forEach(type -> effectToNodes.put(type, new HashSet<>()));
    setEventHandlers();
  }

  private void setEventHandlers() {
    wss.getCallbackRegistry().getOnNodeAdded().add(
        event -> event.node().getView().ifPresent(this::registerNodeByEffect));
    wss.getCallbackRegistry().getOnNodeRemoved().add(
        event -> event.node().getView().ifPresent(this::deregisterNodeByEffect));
  }

  /** {@link #effectToNodes} に {@code view} を追加する. */
  private void registerNodeByEffect(BhNodeView view) {
    view.getLookManager().getAppliedEffects()
        .forEach(effect -> effectToNodes.get(effect).add(view));
  }

  /** {@link #effectToNodes} から {@code view} を削除する. */
  private void deregisterNodeByEffect(BhNodeView view) {
    view.getLookManager().getAppliedEffects()
        .forEach(effect -> effectToNodes.get(effect).remove(view));
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
    if (enable) {
      effectToNodes.get(type).add(view);
    } else {
      effectToNodes.get(type).remove(view);
    }
    userOpe.pushCmd(ope -> setEffectEnabled(view, !enable, type, ope));
  }

  @Override
  public void setEffectEnabled(BhNodeView view, boolean enable, VisualEffectType type) {
    setEffectEnabled(view, enable, type, new UserOperation());
  }

  @Override
  public void disableEffects(Workspace ws, VisualEffectType type, UserOperation userOpe) {
    Set<BhNodeView> nodeViews = effectToNodes.get(type).stream()
        .filter(view -> nodeViewBelongsToWorkspace(view, ws))
        .collect(Collectors.toSet());
    for (BhNodeView view : nodeViews) {
      view.getLookManager().setEffectEnabled(false, type);
    }
    effectToNodes.get(type).removeAll(nodeViews);
  }

  @Override
  public void disableEffects(Workspace ws, VisualEffectType type) {
    disableEffects(ws, type, new UserOperation());
  }

  @Override
  public void disableEffects(VisualEffectType type, UserOperation userOpe) {
    effectToNodes.get(type).forEach(view -> view.getLookManager().setEffectEnabled(false, type));
    effectToNodes.get(type).clear();
  }

  @Override
  public void disableEffects(VisualEffectType type) {
    disableEffects(type, new UserOperation());
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

  /** {@code view} が所属しているワークスペースが {@code ws} と一致するか調べる. */
  private static boolean nodeViewBelongsToWorkspace(BhNodeView view, Workspace ws) {
    return view.getModel()
        .map(node -> node.getWorkspace() == ws)
        .orElse(false);
  }
}
