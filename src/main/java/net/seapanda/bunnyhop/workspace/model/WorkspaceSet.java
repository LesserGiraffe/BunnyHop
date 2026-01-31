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

package net.seapanda.bunnyhop.workspace.model;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.SequencedSet;
import java.util.function.Consumer;
import net.seapanda.bunnyhop.node.model.BhNode;
import net.seapanda.bunnyhop.node.model.BhNode.Swapped;
import net.seapanda.bunnyhop.node.model.derivative.Derivative;
import net.seapanda.bunnyhop.node.model.event.CauseOfDeletion;
import net.seapanda.bunnyhop.node.model.service.BhNodePlacer;
import net.seapanda.bunnyhop.service.undo.UserOperation;
import net.seapanda.bunnyhop.utility.event.ConsumerInvoker;
import net.seapanda.bunnyhop.utility.event.SimpleConsumerInvoker;

/**
 * ワークスペースの集合を保持、管理するクラス.
 *
 * @author K.Koike
 */
public class WorkspaceSet {

  /** 全てのワークスペース. */
  private final SequencedSet<Workspace> workspaceSet = new LinkedHashSet<>();
  /** 保存後に変更されたかどうかのフラグ. */
  private boolean isDirty = false;
  private Workspace currentWorkspace;
  /** このワークスペースセットに登録されたイベントハンドラを管理するオブジェクト. */
  private final CallbackRegistry cbRegistry = new CallbackRegistry();

  /** コンストラクタ. */
  public WorkspaceSet() {}

  /**
   * ワークスペースを追加する.
   *
   * @param workspace 追加するワークスペース
   * @param userOpe undo 用コマンドオブジェクト
   */
  public void addWorkspace(Workspace workspace, UserOperation userOpe) {
    workspaceSet.add(workspace);
    workspace.setWorkspaceSet(this);
    Workspace.CallbackRegistry registry = workspace.getCallbackRegistry();
    registry.getOnNodeSelectionStateChanged().add(cbRegistry.onNodeSelectionStateChanged);
    registry.getOnNodeCompileErrorStateUpdated().add(cbRegistry.onNodeCompileErrStateUpdated);
    registry.getOnNodeBreakpointSet().add(cbRegistry.onNodeBreakpointSet);
    registry.getOnOriginalNodeChanged().add(cbRegistry.onOriginalNodeChanged);
    registry.getOnNodeAdded().add(cbRegistry.onNodeAdded);
    registry.getOnNodeRemoved().add(cbRegistry.onNodeRemoved);
    registry.getOnRootNodeAdded().add(cbRegistry.onRootNodeAdded);
    registry.getOnRootNodeRemoved().add(cbRegistry.onRootNodeRemoved);
    registry.getOnNameChanged().add(cbRegistry.onWsNameChanged);
    userOpe.pushCmd(ope -> removeWorkspace(workspace, ope));
    cbRegistry.onWsAddedInvoker.invoke(new WorkspaceAddedEvent(this, workspace, userOpe));
  }

  /**
   * ワークスペースを取り除く.
   *
   * @param workspace 取り除くワークスペース
   * @param userOpe undo 用コマンドオブジェクト
   */
  public void removeWorkspace(Workspace workspace, UserOperation userOpe) {
    workspaceSet.remove(workspace);
    workspace.setWorkspaceSet(null);
    deleteNodesInWorkspace(workspace, userOpe);
    Workspace.CallbackRegistry registry = workspace.getCallbackRegistry();
    registry.getOnNodeSelectionStateChanged().remove(cbRegistry.onNodeSelectionStateChanged);
    registry.getOnNodeCompileErrorStateUpdated().remove(cbRegistry.onNodeCompileErrStateUpdated);
    registry.getOnNodeBreakpointSet().remove(cbRegistry.onNodeBreakpointSet);
    registry.getOnOriginalNodeChanged().remove(cbRegistry.onOriginalNodeChanged);
    registry.getOnNodeAdded().remove(cbRegistry.onNodeAdded);
    registry.getOnNodeRemoved().remove(cbRegistry.onNodeRemoved);
    registry.getOnRootNodeAdded().remove(cbRegistry.onRootNodeAdded);
    registry.getOnRootNodeRemoved().remove(cbRegistry.onRootNodeRemoved);
    registry.getOnNameChanged().remove(cbRegistry.onWsNameChanged);
    userOpe.pushCmd(ope -> addWorkspace(workspace, ope));
    cbRegistry.onWsRemovedInvoker.invoke(new WorkspaceRemovedEvent(this, workspace, userOpe));
  }

  /** {@code ws} の中のノードを全て消す. */
  private void deleteNodesInWorkspace(Workspace ws, UserOperation userOpe) {
    SequencedSet<BhNode> rootNodes = ws.getRootNodes();
    var nodesToDelete = rootNodes.stream()
        .filter(node -> node.getEventInvoker().onDeletionRequested(
            new ArrayList<>(rootNodes), CauseOfDeletion.WORKSPACE_DELETION, userOpe))
        .toList();
    List<Swapped> swappedNodes = BhNodePlacer.deleteNodes(nodesToDelete, true, userOpe);
    for (var swapped : swappedNodes) {
      swapped.newNode().findParentNode().getEventInvoker().onChildReplaced(
          swapped.oldNode(),
          swapped.newNode(),
          swapped.newNode().getParentConnector(),
          userOpe);
    }
  }

  /**
   * 現在保持しているワークスペース一覧を返す.
   *
   * @return 現在保持しているワークスペース一覧
   */
  public SequencedSet<Workspace> getWorkspaces() {
    return new LinkedHashSet<>(workspaceSet);
  }

  /**
   * 操作対象のワークスペースを設定する.
   *
   * @param ws 操作対象のワークスペース
   * @param userOpe undo 用コマンドオブジェクト
   */
  public void setCurrentWorkspace(Workspace ws, UserOperation userOpe) {
    if (currentWorkspace == ws) {
      return;
    }
    Workspace old = currentWorkspace;
    currentWorkspace = ws;
    cbRegistry.onCurrentWsChangedInvoker.invoke(
        new CurrentWorkspaceChangedEvent(this, old, ws, userOpe));
  }

  /**
   * 現在, 操作対象となっているワークスペースを取得する.
   *
   * @return 現在操作対象となっているワークスペース. 存在しない場合は null.
   */
  public Workspace getCurrentWorkspace() {
    return currentWorkspace;
  }

  /** ワークスペースセットが保存後に変更されていることを示すフラグをセットする. */
  public void setDirty(boolean val) {
    isDirty = val;
  }

  /** ワークスペースセットが保存後に変更されている場合 true を返す. */
  public boolean isDirty() {
    return isDirty;
  }
  
  /**
   * このワークスペースセットに対するイベントハンドラの追加と削除を行うオブジェクトを返す.
   *
   * @return このワークスペースセットに対するイベントハンドラの追加と削除を行うオブジェクト
   */
  public CallbackRegistry getCallbackRegistry() {
    return cbRegistry;
  }

  /** {@link WorkspaceSet} に対してイベントハンドラを追加または削除する機能を提供するクラス. */
  public class CallbackRegistry {

    /** ワークスペースセットのノードの選択状態が変更されたときのイベントハンドラを管理するオブジェクト. */
    private final ConsumerInvoker<NodeSelectionEvent> onNodeSelStateChangedInvoker =
        new SimpleConsumerInvoker<>();
    
    /** ワークスペースセットのノードのコンパイルエラー状態が更新されたときのイベントハンドラを管理するオブジェクト. */
    private final ConsumerInvoker<NodeCompileErrorEvent> onNodeCompileErrStateUpdatedInvoker =
        new SimpleConsumerInvoker<>();

    /** ワークスペースセットのノードのブレークポイントの状態が変更されたときのイベントハンドラを管理するオブジェクト. */
    private final ConsumerInvoker<NodeBreakpointSetEvent>
        onNodeBreakpointSetInvoker = new SimpleConsumerInvoker<>();

    /** ワークスペースセットのノードのオリジナルノードが変更されたときのイベントハンドラを管理するオブジェクト. */
    private final ConsumerInvoker<OriginalNodeChangeEvent>
        onOriginalNodeChangedInvoker = new SimpleConsumerInvoker<>();

    /** ワークスペースセットのワークスペースにノードが追加されたときのイベントハンドラを管理するオブジェクト. */
    private final ConsumerInvoker<NodeAddedEvent> onNodeAddedInvoker =
        new SimpleConsumerInvoker<>();

    /** ワークスペースセットのワークスペースからノードが削除されたときのイベントハンドラを管理するオブジェクト. */
    private final ConsumerInvoker<NodeRemovedEvent> onNodeRemovedInvoker = 
        new SimpleConsumerInvoker<>();

    /**
     * ワークスペースセットのワークスペースのルートノード一式に
     * 新しくルートノードが追加されたときのイベントハンドラを管理するオブジェクト.
     */
    private final ConsumerInvoker<RootNodeAddedEvent> onRootNodeAddedInvoker =
        new SimpleConsumerInvoker<>();

    /**
     * ワークスペースセットのワークスペースのルートノード一式から
     * ルートノードが削除されたときのイベントハンドラを管理するオブジェクト.
     */
    private final ConsumerInvoker<RootNodeRemovedEvent> onRootNodeRemovedInvoker = 
        new SimpleConsumerInvoker<>();
    
    /** ワークスペースセットにワークスペースが追加されたときのイベントハンドラを管理するオブジェクト. */
    private final ConsumerInvoker<WorkspaceAddedEvent> onWsAddedInvoker =
        new SimpleConsumerInvoker<>();

    /** ワークスペースセットからワークスペースが削除されたときのイベントハンドラを管理するオブジェクト. */
    private final ConsumerInvoker<WorkspaceRemovedEvent> onWsRemovedInvoker =
        new SimpleConsumerInvoker<>();

    /** ワークスペースセットで操作対象のワークスペースが変わったときのイベントハンドラを管理するオブジェクト. */
    private final ConsumerInvoker<CurrentWorkspaceChangedEvent> onCurrentWsChangedInvoker =
        new SimpleConsumerInvoker<>();

    /** ワークスペースセットのワークスペースの名前が変更されたときのイベントハンドラを管理するオブジェクト. */
    private final ConsumerInvoker<WorkspaceNameChangedEvent> onWsNameChangedInvoker =
        new SimpleConsumerInvoker<>();

    /** ワークスペースセットのノードの選択状態が変わったときのイベントハンドラ. */
    private final Consumer<? super Workspace.NodeSelectionEvent> onNodeSelectionStateChanged =
        this::onNodeSelectionStateChanged;

    /** ワークスペースセットのノードのコンパイルエラー状態が更新されたときのイベントハンドラ. */
    private final Consumer<? super Workspace.NodeCompileErrorEvent> onNodeCompileErrStateUpdated =
        this::onNodeCompileErrStateUpdated;

    /** ワークスペースセットのノードのブレークポイントの状態が変わったときのイベントハンドラ. */
    private final Consumer<? super Workspace.NodeBreakpointSetEvent> onNodeBreakpointSet =
        this::onNodeBreakpointSet;

    /** ワークスペースセットのノードのオリジナルノードが変わったときのイベントハンドラ. */
    private final Consumer<? super Workspace.OriginalNodeChangeEvent> onOriginalNodeChanged =
        this::onOriginalNodeChanged;

    /** ワークスペースセットのワークスペースにノードが追加されたときのイベントハンドラ. */
    private final Consumer<? super Workspace.NodeAddedEvent> onNodeAdded = this::onNodeAdded;

    /** ワークスペースセットのワークスペースからノードが削除されたときのイベントハンドラ. */
    private final Consumer<? super Workspace.NodeRemovedEvent> onNodeRemoved = this::onNodeRemoved;

    /** ワークスペースセットのワークスペースのルートノード一式に新しくルートノードが追加されたときのイベントハンドラ. */
    private final Consumer<? super Workspace.RootNodeAddedEvent> onRootNodeAdded =
        this::onRootNodeAdded;

    /** ワークスペースセットのワークスペースのルートノード一式に新しくルートノードが追加されたときのイベントハンドラ. */
    private final Consumer<? super Workspace.RootNodeRemovedEvent> onRootNodeRemoved =
        this::onRootNodeRemoved;

    /** ワークスペースセットのワークスペースの名前が変更されたときのイベントハンドラ. */
    private final Consumer<? super Workspace.NameChangedEvent> onWsNameChanged =
        this::onWsNameChanger;

    /**
     * ワークスペースセットのノードの選択状態が変更されたときのイベントハンドラを
     * 登録 / 削除するためのオブジェクトを取得する.
     */
    public ConsumerInvoker<NodeSelectionEvent>.Registry getOnNodeSelectionStateChanged() {
      return onNodeSelStateChangedInvoker.getRegistry();
    }

    /**
     * ワークスペースセットのノードのコンパイルエラー状態が更新されたときのイベントハンドラを
     * 登録 / 削除するためのオブジェクトを取得する.
     */
    public ConsumerInvoker<NodeCompileErrorEvent>.Registry getOnNodeCompileErrStateUpdated() {
      return onNodeCompileErrStateUpdatedInvoker.getRegistry();
    }

    /**
     * ワークスペースセットのノードのブレークポイントの状態が変更されたときのイベントハンドラを
     * 登録 / 削除するためのオブジェクトを取得する.
     */
    public ConsumerInvoker<NodeBreakpointSetEvent>.Registry getOnNodeBreakpointSetEvent() {
      return onNodeBreakpointSetInvoker.getRegistry();
    }

    /**
     * ワークスペースセットのノードのオリジナルノードが変更されたときのイベントハンドラを
     * 登録 / 削除するためのオブジェクトを取得する.
     */
    public ConsumerInvoker<OriginalNodeChangeEvent>.Registry getOnOriginalNodeChanged() {
      return onOriginalNodeChangedInvoker.getRegistry();
    }

    /**
     * ワークスペースセットのワークスペースにノードが追加されたときのイベントハンドラを
     * 登録 / 削除するためのオブジェクトを取得する.
     */
    public ConsumerInvoker<NodeAddedEvent>.Registry getOnNodeAdded() {
      return onNodeAddedInvoker.getRegistry();
    }

    /**
     * ワークスペースセットのワークスペースからノードが削除されたときのイベントハンドラを
     * 登録 / 削除するためのオブジェクトを取得する.
     */
    public ConsumerInvoker<NodeRemovedEvent>.Registry getOnNodeRemoved() {
      return onNodeRemovedInvoker.getRegistry();
    }

    /**
     * ワークスペースセットのワークスペースのルートノード一式に新しくルートノードが追加されたときの
     * イベントハンドラのレジストリを取得する.
     */
    public ConsumerInvoker<RootNodeAddedEvent>.Registry getOnRootNodeAdded() {
      return onRootNodeAddedInvoker.getRegistry();
    }

    /**
     * ワークスペースセットのワークスペースのルートノード一式からルートノードが削除されたときの
     * イベントハンドラのレジストリを取得する.
     */
    public ConsumerInvoker<RootNodeRemovedEvent>.Registry getOnRootNodeRemoved() {
      return onRootNodeRemovedInvoker.getRegistry();
    }

    /**
     * ワークスペースセットにワークスペースが追加されたときのイベントハンドラを
     * 登録 / 削除するためのオブジェクトを取得する.
     */
    public ConsumerInvoker<WorkspaceAddedEvent>.Registry getOnWorkspaceAdded() {
      return onWsAddedInvoker.getRegistry();
    }

    /**
     * ワークスペースセットからワークスペースが削除されたときのイベントハンドラを
     * 登録 / 削除するためのオブジェクトを取得する.
     */    
    public ConsumerInvoker<WorkspaceRemovedEvent>.Registry getOnWorkspaceRemoved() {
      return onWsRemovedInvoker.getRegistry();
    }

    /**
     * ワークスペースセットで操作対象のワークスペースが変わったときのイベントハンドラを
     * 登録 / 削除するためのオブジェクトを取得する.
     */
    public ConsumerInvoker<CurrentWorkspaceChangedEvent>.Registry getOnCurrentWorkspaceChanged() {
      return onCurrentWsChangedInvoker.getRegistry();
    }

    /**
     * ワークスペースセットのワークスペースの名前が変更されたときのイベントハンドラを
     * 登録 / 削除するためのオブジェクトを取得する.
     */
    public ConsumerInvoker<WorkspaceNameChangedEvent>.Registry getOnWorkspaceNameChanged() {
      return onWsNameChangedInvoker.getRegistry();
    }

    /** ワークスペースセットのノードの選択状態に変化があったときのイベントハンドラを呼ぶ. */
    private void onNodeSelectionStateChanged(Workspace.NodeSelectionEvent event) {
      onNodeSelStateChangedInvoker.invoke(new NodeSelectionEvent(
          WorkspaceSet.this, event.ws(), event.node(), event.isSelected(), event.userOpe()));
    }

    /** ワークスペースセットのノードのコンパイルエラー状態が更新されたときのイベントハンドラを呼ぶ. */
    private void onNodeCompileErrStateUpdated(Workspace.NodeCompileErrorEvent event) {
      onNodeCompileErrStateUpdatedInvoker.invoke(new NodeCompileErrorEvent(
          WorkspaceSet.this, event.ws(), event.node(), event.hasError(), event.userOpe()));
    }

    /** ワークスペースセットのノードのブレークポイントの状態に変化があったときのイベントハンドラを呼ぶ. */
    private void onNodeBreakpointSet(Workspace.NodeBreakpointSetEvent event) {
      onNodeBreakpointSetInvoker.invoke(new NodeBreakpointSetEvent(
          WorkspaceSet.this, event.ws(), event.node(), event.isBreakpointSet(), event.userOpe()));
    }

    /** ワークスペースセットのノードのオリジナルノードが変更されたときのイベントハンドラを呼ぶ. */
    private void onOriginalNodeChanged(Workspace.OriginalNodeChangeEvent event) {
      onOriginalNodeChangedInvoker.invoke(new OriginalNodeChangeEvent(
          WorkspaceSet.this,
          event.ws(),
          event.node(),
          event.oldOriginal(),
          event.newOriginal(),
          event.userOpe()));
    }

    /** ワークスペースセットのワークスペースにノードが追加されたときのイベントハンドラを呼ぶ. */
    private void onNodeAdded(Workspace.NodeAddedEvent event) {
      onNodeAddedInvoker.invoke(
          new NodeAddedEvent(WorkspaceSet.this, event.ws(), event.node(), event.userOpe()));
    }

    /** ワークスペースセットのワークスペースからノードが削除されたときのイベントハンドラを呼ぶ. */
    private void onNodeRemoved(Workspace.NodeRemovedEvent event) {
      onNodeRemovedInvoker.invoke(
          new NodeRemovedEvent(WorkspaceSet.this, event.ws(), event.node(), event.userOpe()));
    }

    /** ワークスペースセットのワークスペースのルートノード一式に新しくルートノードが追加されたときのイベントハンドラを呼ぶ.*/
    private void onRootNodeAdded(Workspace.RootNodeAddedEvent event) {
      onRootNodeAddedInvoker.invoke(
          new RootNodeAddedEvent(WorkspaceSet.this, event.ws(), event.node(), event.userOpe()));
    }

    /** ワークスペースセットのワークスペースのルートノード一式からルートノードが削除されたときのイベントハンドラを呼ぶ.*/
    private void onRootNodeRemoved(Workspace.RootNodeRemovedEvent event) {
      onRootNodeRemovedInvoker.invoke(
          new RootNodeRemovedEvent(WorkspaceSet.this, event.ws(), event.node(), event.userOpe()));
    }

    /** ワークスペースセットのワークスペースの名前が変更されたときのイベントハンドラを呼ぶ. */
    private void onWsNameChanger(Workspace.NameChangedEvent event) {
      onWsNameChangedInvoker.invoke(new WorkspaceNameChangedEvent(
          WorkspaceSet.this, event.ws(), event.oldName(), event.newName()));
    }
  }

  /**
   * ワークスペースセットのノードの選択状態が変更されたときの情報を格納したレコード.
   *
   * @param wss {@code ws} を保持するワークスペースセット
   * @param ws {@code node} を保持するワークスペース
   * @param node 選択状態が変更されたノード
   * @param isSelected {@code node} が選択された場合 true
   * @param userOpe undo 用コマンドオブジェクト
   */
  public record NodeSelectionEvent(
      WorkspaceSet wss,
      Workspace ws,
      BhNode node,
      boolean isSelected,
      UserOperation userOpe) {}

  /**
   * ワークスペースセットのノードのコンパイルエラー状態が更新されたときの情報を格納したレコード.
   *
   * @param wss {@code ws} を保持するワークスペースセット
   * @param ws {@code node} を保持するワークスペース
   * @param node コンパイルエラー状態が更新されたノード
   * @param hasError {@code node} がコンパイルエラーを持つ場合 true
   * @param userOpe undo 用コマンドオブジェクト
   */      
  public record NodeCompileErrorEvent(
      WorkspaceSet wss,
      Workspace ws,
      BhNode node,
      boolean hasError,
      UserOperation userOpe) {}

  /**
   * ワークスペースセットのノードのブレークポイントの設定が変更されたときの情報を格納したレコード.
   *
   * @param wss {@code ws} を保持するワークスペースセット
   * @param ws {@code node} を保持するワークスペース
   * @param node ブレークポイントの状態が変更されたノード
   * @param isBreakpointSet ブレークポイントが設定された場合 true, 設定が解除された場合 false
   * @param userOpe undo 用コマンドオブジェクト
   */
  public record NodeBreakpointSetEvent(
      WorkspaceSet wss,
      Workspace ws,
      BhNode node,
      boolean isBreakpointSet,
      UserOperation userOpe) {}

  /**
   * ワークスペースのノードのオリジナルノードが変更されたときの情報を格納したレコード.
   *
   * @param wss {@code ws} を保持するワークスペースセット
   * @param ws {@code node} を保持するワークスペース
   * @param node オリジナルノードが変更されたノード
   * @param oldOriginal 変更前のオリジナルノード (nullable)
   * @param newOriginal 変更後のオリジナルノード (nullable)
   * @param userOpe undo 用コマンドオブジェクト
   */
  public record OriginalNodeChangeEvent(
      WorkspaceSet wss,
      Workspace ws,
      BhNode node,
      Derivative oldOriginal,
      Derivative newOriginal,
      UserOperation userOpe) {}

  /**
   * ワークスペースセットのワークスペースにノードが追加されたときの情報を格納したレコード.
   *
   * @param wss {@code ws} を保持するワークスペースセット
   * @param ws {@code node} が追加されたワークスペース
   * @param node {@code ws} に追加されたノード
   * @param userOpe undo 用コマンドオブジェクト
   */
  public record NodeAddedEvent(
      WorkspaceSet wss, Workspace ws, BhNode node, UserOperation userOpe) {}

  /**
   * ワークスペースセットのワークスペースからノードが削除されたときの情報を格納したレコード.
   *
   * @param wss {@code ws} を保持するワークスペースセット
   * @param ws {@code node} が削除されたワークスペース
   * @param node {@code ws} から削除されたノード
   * @param userOpe undo 用コマンドオブジェクト
   */
  public record NodeRemovedEvent(
      WorkspaceSet wss, Workspace ws, BhNode node, UserOperation userOpe) {}

  /**
   * ワークスペースセットのワークスペースのルートノード一式に新しくルートノードが追加されたときの情報を格納したレコード.
   *
   * @param wss {@code ws} を保持するワークスペースセット
   * @param ws {@code node} をルートノードとして保持するワークスペース
   * @param node {@code ws} 上でルートノードとなったノード
   * @param userOpe undo 用コマンドオブジェクト
   */
  public record RootNodeAddedEvent(
      WorkspaceSet wss, Workspace ws, BhNode node, UserOperation userOpe) {}

  /**
   * ワークスペースセットのワークスペースのルートノード一式からルートノードが削除されたときの情報を格納したレコード.
   *
   * @param wss {@code ws} を保持するワークスペースセット
   * @param ws このワークスペース上の {@code node} が非ルートノードとなった
   * @param node 非ルートノードとなったノード
   * @param userOpe undo 用コマンドオブジェクト
   */  
  public record RootNodeRemovedEvent(
      WorkspaceSet wss, Workspace ws, BhNode node, UserOperation userOpe) {}

  /**
   * ワークスペースセットにワークスペースが追加されたときの情報を格納したレコード.
   *
   * @param wss {@code ws} が追加されたワークスペースセット
   * @param ws {@code wss} に追加されたワークスペース
   * @param userOpe undo 用コマンドオブジェクト
   */
  public record WorkspaceAddedEvent(WorkspaceSet wss, Workspace ws, UserOperation userOpe) {}

  /**
   * ワークスペースセットからワークスペースが削除されたときの情報を格納したレコード.
   *
   * @param wss {@code ws} が削除されたワークスペースセット
   * @param ws {@code wss} から削除されたワークスペース
   * @param userOpe undo 用コマンドオブジェクト
   */
  public record WorkspaceRemovedEvent(WorkspaceSet wss, Workspace ws, UserOperation userOpe) {}

  /**
   * ワークスペースセットで操作対象のワークスペースが変更されたときの情報を格納したレコード.
   *
   * @param wss 操作対象のワークスペースが変更されたワークスペースセット
   * @param oldWs {@code newWs} の前に操作対象であったワークスペース
   * @param newWs 新しく操作対象となったワークスペース
   * @param userOpe undo 用コマンドオブジェクト
   */
  public record CurrentWorkspaceChangedEvent(
      WorkspaceSet wss, Workspace oldWs, Workspace newWs, UserOperation userOpe) {}

  /**
   * ワークスペースの名前が変更されたときの情報を格納したレコード.
   *
   * @param wss 名前が変更されたワークスペースを含むワークスペースセット
   * @param ws 名前が変更されたワークスペース
   * @param oldName 変更前の名前
   * @param newName 変更後の名前
   */
  public record WorkspaceNameChangedEvent(
      WorkspaceSet wss, Workspace ws, String oldName, String newName) {}
}
