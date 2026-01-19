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

package net.seapanda.bunnyhop.debugger.service;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import net.seapanda.bunnyhop.bhprogram.BhRuntimeController;
import net.seapanda.bunnyhop.bhprogram.BhRuntimeController.ConnectionEvent;
import net.seapanda.bunnyhop.bhprogram.BhRuntimeController.StartEvent;
import net.seapanda.bunnyhop.bhprogram.BhRuntimeController.TerminationEvent;
import net.seapanda.bunnyhop.bhprogram.common.message.debug.GetEntryPointsCmd;
import net.seapanda.bunnyhop.bhprogram.common.message.debug.GetEntryPointsResp;
import net.seapanda.bunnyhop.bhprogram.runtime.LocalBhRuntimeController;
import net.seapanda.bunnyhop.bhprogram.runtime.RemoteBhRuntimeController;
import net.seapanda.bunnyhop.node.model.BhNode;
import net.seapanda.bunnyhop.node.model.syntaxsymbol.InstanceId;
import net.seapanda.bunnyhop.node.view.BhNodeView;
import net.seapanda.bunnyhop.node.view.effect.VisualEffectManager;
import net.seapanda.bunnyhop.node.view.effect.VisualEffectType;
import net.seapanda.bunnyhop.ui.view.ViewUtil;
import net.seapanda.bunnyhop.workspace.model.WorkspaceSet;

/**
 * BhProgram のエントリポイントをアプリケーションユーザに提示する機能を持ったクラス.
 *
 * @author  K.Koike
 */
public class EntryPointPresenter {

  private final VisualEffectManager effectManager;
  /** 最後に BhRuntime と接続した {@link BhRuntimeController}. */
  private BhRuntimeController lastConnectedRuntimeCtrl;
  /** 最後に送信した {@link GetEntryPointsCmd}. */
  private GetEntryPointsCmd lastSentEntryPointsCmd;
  /** 現在表示中のエントリポイント. */
  private Collection<BhNodeView> entryPoints = new ArrayList<>();
  /**
   * key: {@link BhNode} のインスタンス ID.
   * value: key に対応する {@link BhNode}.
   */
  private final Map<InstanceId, BhNode> instIdToNode = new ConcurrentHashMap<>();

  /** コンストラクタ. */
  public EntryPointPresenter(
      WorkspaceSet wss,
      LocalBhRuntimeController localBhRuntimeCtrl,
      RemoteBhRuntimeController remoteBhRuntimeCtrl,
      VisualEffectManager visualEffectManager) {
    this.effectManager = visualEffectManager;

    wss.getCallbackRegistry().getOnNodeAdded().add(
        event -> instIdToNode.put(event.node().getInstanceId(), event.node()));
    wss.getCallbackRegistry().getOnNodeRemoved().add(
        event -> instIdToNode.remove(event.node().getInstanceId()));

    BhRuntimeController.CallbackRegistry cbRegistry = localBhRuntimeCtrl.getCallbackRegistry();
    cbRegistry.getOnConnectionConditionChanged().add(this::onConnCondChanged);
    cbRegistry.getOnBhProgramStarted().add(this::onBhProgramStarted);
    cbRegistry.getOnBhProgramTerminated().add(this::onBhProgramTerminated);

    cbRegistry = remoteBhRuntimeCtrl.getCallbackRegistry();
    cbRegistry.getOnConnectionConditionChanged().add(this::onConnCondChanged);
    cbRegistry.getOnBhProgramStarted().add(this::onBhProgramStarted);
    cbRegistry.getOnBhProgramTerminated().add(this::onBhProgramTerminated);
  }

  private synchronized void onConnCondChanged(ConnectionEvent event) {
    if (!event.isConnected()) {
      return;
    }
    var oldEntryPoints = new ArrayList<>(entryPoints);
    ViewUtil.runSafe(() ->
        oldEntryPoints.forEach(entryPoint ->
            effectManager.setEffectEnabled(entryPoint, false, VisualEffectType.ENTRY_POINT)));
    entryPoints = new ArrayList<>();
    lastConnectedRuntimeCtrl = event.ctrl();
    lastSentEntryPointsCmd = new GetEntryPointsCmd();
    event.ctrl().send(lastSentEntryPointsCmd);
  }

  private synchronized void onBhProgramStarted(StartEvent event) {
    lastSentEntryPointsCmd = new GetEntryPointsCmd();
    event.ctrl().send(lastSentEntryPointsCmd);
  }

  private synchronized void onBhProgramTerminated(TerminationEvent event) {
    if (event.ctrl() != lastConnectedRuntimeCtrl) {
      return;
    }
    var oldEntryPoints = new ArrayList<>(entryPoints);
    ViewUtil.runSafe(() ->
        oldEntryPoints.forEach(entryPoint ->
            effectManager.setEffectEnabled(entryPoint, false, VisualEffectType.ENTRY_POINT)));
  }

  /** {@code resp} に含まれるエントリポイントを表示する. */
  public synchronized void present(GetEntryPointsResp resp) {
    if (resp.getId() != lastSentEntryPointsCmd.getId()) {
      return;
    }
    var oldEntryPoints = new ArrayList<>(entryPoints);
    var newEntryPoints = resp.entryPoints.stream()
            .map(entryPoint -> InstanceId.of(entryPoint.toString()))
            .map(instIdToNode::get)
            .filter(Objects::nonNull)
            .map(node -> node.getView().orElse(null))
            .filter(Objects::nonNull)
            .toList();
    ViewUtil.runSafe(() -> {
      oldEntryPoints.forEach(entryPoint ->
          effectManager.setEffectEnabled(entryPoint, false, VisualEffectType.ENTRY_POINT));
      newEntryPoints.forEach(entryPoint ->
          effectManager.setEffectEnabled(entryPoint, true, VisualEffectType.ENTRY_POINT));
    });
    entryPoints = new ArrayList<>(newEntryPoints);
  }
}
