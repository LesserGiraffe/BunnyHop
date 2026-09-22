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

package net.seapanda.bunnyhop.workspace.view;

import java.util.function.Consumer;
import net.seapanda.bunnyhop.node.view.BhNodeView;
import net.seapanda.bunnyhop.node.view.TextNodeView;
import net.seapanda.bunnyhop.utility.event.ConsumerInvoker;
import net.seapanda.bunnyhop.utility.event.SimpleConsumerInvoker;
import net.seapanda.bunnyhop.workspace.model.Workspace;
import net.seapanda.bunnyhop.workspace.model.WorkspaceSet;

/**
 * ワークスペースセット以下のビューに対してイベントハンドラを登録する機能を提供するクラス.
 *
 * @author K.Koike
 */
public class WorkspaceSetViewCallBackRegistry {

  private final WorkspaceSet wss;

  /** ワークスペースビューにノードビューが追加されたときのイベントハンドラを管理するオブジェクト. */
  private final ConsumerInvoker<NodeAddedEvent> onNodeAddedInvoker =
      new SimpleConsumerInvoker<>();

  /** ワークスペースビューにノードビューが追加されたときのイベントハンドラを管理するオブジェクト. */
  private final ConsumerInvoker<NodeRemovedEvent> onNodeRemovedInvoker =
      new SimpleConsumerInvoker<>();

  /** ワークスペースビュー上のノードのテキストが変更されたときのイベントハンドラを管理するオブジェクト. */
  private final ConsumerInvoker<NodeTextChangedEvent> onNodeTextChangedInvoker =
      new SimpleConsumerInvoker<>();

  /** ワークスペースビューにノードビューが追加されたときのイベントハンドラ. */
  private final Consumer<? super WorkspaceView.NodeAddedEvent> onNodeAdded =
      this::onNodeAdded;

  /** ワークスペースビューからノードビューが削除されたときのイベントハンドラ. */
  private final Consumer<? super WorkspaceView.NodeRemovedEvent> onNodeRemoved =
      this::onNodeRemoved;

  /** ワークスペースビュー上のノードビューのテキストが変更されたときのイベントハンドラ. */
  private final Consumer<? super WorkspaceView.NodeTextChangedEvent> onNodeTextChanged =
      this::onNodeTextChanged;

  /** コンストラクタ. */
  public WorkspaceSetViewCallBackRegistry(WorkspaceSet wss) {
    this.wss = wss;
    WorkspaceSet.CallbackRegistry cbRegistry = wss.getCallbackRegistry();
    cbRegistry.getOnWorkspaceAdded().add(event -> addEventHandlers(event.ws()));
    cbRegistry.getOnWorkspaceRemoved().add(event -> removeEventHandlers(event.ws()));
  }

  /** {@code ws} の {@link WorkspaceView} にイベントハンドラを登録する. */
  private void addEventHandlers(Workspace ws) {
    WorkspaceView wsView = ws.getView().orElse(null);
    if (wsView == null) {
      return;
    }
    WorkspaceView.CallbackRegistry cbRegistry = wsView.getCallbackRegistry();
    cbRegistry.getOnNodeAdded().add(onNodeAdded);
    cbRegistry.getOnNodeRemoved().add(onNodeRemoved);
    cbRegistry.getOnNodeTextChanged().add(onNodeTextChanged);
  }

  /** {@code ws} の {@link WorkspaceView} にからイベントハンドラを削除する. */
  private void removeEventHandlers(Workspace ws) {
    WorkspaceView wsView = ws.getView().orElse(null);
    if (wsView == null) {
      return;
    }
    WorkspaceView.CallbackRegistry cbRegistry = wsView.getCallbackRegistry();
    cbRegistry.getOnNodeAdded().remove(onNodeAdded);
    cbRegistry.getOnNodeRemoved().remove(onNodeRemoved);
    cbRegistry.getOnNodeTextChanged().remove(onNodeTextChanged);
  }

  private void onNodeAdded(WorkspaceView.NodeAddedEvent event) {
    onNodeAddedInvoker.invoke(new NodeAddedEvent(wss, event.wsView(), event.nodeView()));
  }

  private void onNodeRemoved(WorkspaceView.NodeRemovedEvent event) {
    onNodeRemovedInvoker.invoke(new NodeRemovedEvent(wss, event.wsView(), event.nodeView()));
  }

  private void onNodeTextChanged(WorkspaceView.NodeTextChangedEvent event) {
    onNodeTextChangedInvoker
        .invoke(new NodeTextChangedEvent(wss, event.wsView(), event.nodeView()));
  }

  /** ワークスペースビューにノードビューが追加されたときのイベントハンドラのレジストリを取得する. */
  public ConsumerInvoker<NodeAddedEvent>.Registry getOnNodeAdded() {
    return onNodeAddedInvoker.getRegistry();
  }

  /** ワークスペースビューからノードビューが削除されたときのイベントハンドラのレジストリを取得する. */
  public ConsumerInvoker<NodeRemovedEvent>.Registry getOnNodeRemoved() {
    return onNodeRemovedInvoker.getRegistry();
  }

  /** ワークスペースビューのノードビューのテキストが変更れたときのイベントハンドラのレジストリを取得する. */
  public ConsumerInvoker<NodeTextChangedEvent>.Registry getOnNodeTextChanged() {
    return onNodeTextChangedInvoker.getRegistry();
  }

  /**
   * ワークスペースにビューにノードビューが追加されたときの情報を格納したレコード.
   *
   * @param wss {@code wsView} に対応する {@link Workspace} を保持している {@link WorkspaceSet}
   * @param wsView {@code nodeView} が追加されたワークスペース
   * @param nodeView {@code wsView} に追加されたノードビュー
   */
  public record NodeAddedEvent(WorkspaceSet wss, WorkspaceView wsView, BhNodeView nodeView) {}

  /**
   * ワークスペースビューからノードビューが削除されたときの情報を格納したレコード.
   *
   * @param wss {@code wsView} に対応する {@link Workspace} を保持している {@link WorkspaceSet}
   * @param wsView {@code nodeView} が追加されたワークスペース
   * @param nodeView {@code wsView} に追加されたノードビュー
   */
  public record NodeRemovedEvent(WorkspaceSet wss, WorkspaceView wsView, BhNodeView nodeView) {}

  /**
   * ワークスペースビュー上のノードビューのテキストが変更されたときの情報を格納したレコード.
   *
   * @param wss {@code wsView} に対応する {@link Workspace} を保持している {@link WorkspaceSet}
   * @param wsView {@code nodeView} を保持するワークスペースビュー
   * @param nodeView テキストが変更されたノードビュー
   */
  public record NodeTextChangedEvent(
      WorkspaceSet wss, WorkspaceView wsView, TextNodeView nodeView) {}
}
