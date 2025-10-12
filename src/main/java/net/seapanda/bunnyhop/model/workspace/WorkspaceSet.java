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

package net.seapanda.bunnyhop.model.workspace;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.SequencedSet;
import java.util.function.Consumer;
import net.seapanda.bunnyhop.model.BhNodePlacer;
import net.seapanda.bunnyhop.model.node.BhNode;
import net.seapanda.bunnyhop.model.node.BhNode.Swapped;
import net.seapanda.bunnyhop.model.node.event.CauseOfDeletion;
import net.seapanda.bunnyhop.undo.UserOperation;
import net.seapanda.bunnyhop.utility.function.ConsumerInvoker;
import net.seapanda.bunnyhop.utility.function.SimpleConsumerInvoker;

/**
 * ワークスペースの集合を保持、管理するクラス.
 *
 * @author K.Koike
 */
public class WorkspaceSet {

  /** 全てのワークスペース. */
  private final SequencedSet<Workspace> workspaceSet = new LinkedHashSet<>();
  /** コンパイルエラーを起こしている全てのノード. */
  private final SequencedSet<BhNode> compileErrNodes = new LinkedHashSet<>();
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
    registry.getOnNodeCompileErrorStateChanged().add(cbRegistry.onNodeCompileErrStateChanged);
    registry.getOnNodeBreakpointSet().add(cbRegistry.onNodeBreakpointSet);
    registry.getOnNodeAdded().add(cbRegistry.onNodeAdded);
    registry.getOnNodeRemoved().add(cbRegistry.onNodeRemoved);
    registry.getOnRootNodeAdded().add(cbRegistry.onRootNodeAdded);
    registry.getOnRootNodeRemoved().add(cbRegistry.onRootNodeRemoved);
    registry.getOnNameChanged().add(cbRegistry.onWsNamechanged);
    cbRegistry.onWsAddedInvoker.invoke(new WorkspaceAddedEvent(this, workspace, userOpe));
    userOpe.pushCmdOfAddWorkspace(workspace);
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
    registry.getOnNodeCompileErrorStateChanged().remove(cbRegistry.onNodeCompileErrStateChanged);
    registry.getOnNodeBreakpointSet().remove(cbRegistry.onNodeBreakpointSet);
    registry.getOnNodeAdded().remove(cbRegistry.onNodeAdded);
    registry.getOnNodeRemoved().remove(cbRegistry.onNodeRemoved);
    registry.getOnRootNodeAdded().remove(cbRegistry.onRootNodeAdded);
    registry.getOnRootNodeRemoved().remove(cbRegistry.onRootNodeRemoved);
    registry.getOnNameChanged().remove(cbRegistry.onWsNamechanged);
    cbRegistry.onWsRemovedInvoker.invoke(new WorkspaceRemovedEvent(this, workspace, userOpe));
    userOpe.pushCmdOfRemoveWorkspace(workspace, this);
  }

  /** {@code ws} の中のノードを全て消す. */
  private void deleteNodesInWorkspace(Workspace ws, UserOperation userOpe) {
    SequencedSet<BhNode> rootNodes = ws.getRootNodes();
    var nodesToDelete = rootNodes.stream()
        .filter(node -> node.getEventInvoker().onDeletionRequested(
            new ArrayList<>(rootNodes), CauseOfDeletion.WORKSPACE_DELETION, userOpe))
        .toList();
    List<Swapped> swappedNodes = BhNodePlacer.deleteNodes(nodesToDelete, userOpe);
    for (var swapped : swappedNodes) {
      swapped.newNode().findParentNode().getEventInvoker().onChildReplaced(
          swapped.oldNode(),
          swapped.newNode(),
          swapped.newNode().getParentConnector(),
          userOpe);
    }
  }

  /**
   * コンパイルエラーを起こしている全てのノードを取得する.
   *
   * @return コンパイルエラーを起こしている全てのノード
   */
  public SequencedSet<BhNode> getCompileErrNodes() {
    return new LinkedHashSet<>(compileErrNodes);
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
   * <pre>
   * この操作は undo の対象にしない.
   * 理由は, この操作はワークスペースのタブ切り替え時は単一の undo 操作となるが,
   * ワークスペースの追加 (redo によるものも含む) 時には, これに付随する undo 操作となるので
   * 本メソッドに渡すべき {@link UserOperation} オブジェクトの選択が複雑になる.
   * </pre>
   *
   * @param ws 操作対象のワークスペース
   */
  public void setCurrentWorkspace(Workspace ws) {
    Workspace old = currentWorkspace;
    currentWorkspace = ws;
    cbRegistry.onCurrentWsChangedInvoker.invoke(
        new CurrentWorkspaceChangedEvent(this, old, ws));
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

    /** 関連するワークスペースセット以下のノードの選択状態が変更されたときのイベントハンドラを管理するオブジェクト. */
    private final ConsumerInvoker<NodeSelectionEvent> onNodeSelStateChangedInvoker =
        new SimpleConsumerInvoker<>();
    
    /** 関連するワークスペースセット以下のノードのコンパイルエラー状態が変更されたときのイベントハンドラを管理するオブジェクト. */
    private final ConsumerInvoker<NodeCompileErrorEvent>
        onNodeCompileErrStateChangedInvoker = new SimpleConsumerInvoker<>();

    /** 関連するワークスペースセット以下のノードのブレークポイントの状態が変更されたときのイベントハンドラを管理するオブジェクト. */
    private final ConsumerInvoker<NodeBreakpointSetEvent>
        onNodeBreakpointSetInvoker = new SimpleConsumerInvoker<>();

    /** 関連するワークスペースセットのワークスペースにノードが追加されたときのイベントハンドラを管理するオブジェクト. */
    private final ConsumerInvoker<NodeAddedEvent> onNodeAddedInvoker =
        new SimpleConsumerInvoker<>();

    /** 関連するワークスペースセットのワークスペースからノードが削除されたときのイベントハンドラを管理するオブジェクト. */
    private final ConsumerInvoker<NodeRemovedEvent> onNodeRemovedInvoker = 
        new SimpleConsumerInvoker<>();

    /**
     * 関連するワークスペースセットのワークスペースのルートノード一式に
     * 新しくルートノードが追加されたときのイベントハンドラを管理するオブジェクト.
     */
    private final ConsumerInvoker<RootNodeAddedEvent> onRootNodeAddedInvoker =
        new SimpleConsumerInvoker<>();

    /**
     * 関連するワークスペースセットのワークスペースのルートノード一式から
     * ルートノードが削除されたときのイベントハンドラを管理するオブジェクト.
     */
    private final ConsumerInvoker<RootNodeRemovedEvent> onRootNodeRemovedInvoker = 
        new SimpleConsumerInvoker<>();
    
    /** 関連するワークスペースセットにワークスペースが追加されたときのイベントハンドラを管理するオブジェクト. */
    private final ConsumerInvoker<WorkspaceAddedEvent> onWsAddedInvoker =
        new SimpleConsumerInvoker<>();

    /** 関連するワークスペースセットからワークスペースが削除されたときのイベントハンドラを管理するオブジェクト. */
    private final ConsumerInvoker<WorkspaceRemovedEvent> onWsRemovedInvoker =
        new SimpleConsumerInvoker<>();

    /** 関連するワークスペースセットで操作対象のワークスペースが変わったときのイベントハンドラを管理するオブジェクト. */
    private final ConsumerInvoker<CurrentWorkspaceChangedEvent> onCurrentWsChangedInvoker =
        new SimpleConsumerInvoker<>();

    /* 関連するワークスペースセットのワークスペースの名前が変更されたときのイベントハンドラを管理するオブジェクト. */
    private final ConsumerInvoker<WorkspaceNameChangedEvent> onWsNameChangedInvoker =
        new SimpleConsumerInvoker<>();

    /** ノードの選択状態が変わったときのイベントハンドラ. */
    private final Consumer<? super Workspace.NodeSelectionEvent> onNodeSelectionStateChanged =
        this::onNodeSelectionStateChanged;

    /** ノードのコンパイルエラー状態が変わったときのイベントハンドラ. */
    private final Consumer<? super Workspace.NodeCompileErrorEvent> onNodeCompileErrStateChanged =
        this::onNodeCompileErrStateChanged;

    /** ノードのブレークポイントの状態が変わったときのイベントハンドラ. */
    private final Consumer<? super Workspace.NodeBreakpointSetEvent> onNodeBreakpointSet =
        this::onNodeBreakpointSetEvent;

    /** 関連するワークスペースセットのワークスペースにノードが追加されたときのイベントハンドラ. */
    private final Consumer<? super Workspace.NodeAddedEvent> onNodeAdded = this::onNodeAdded;

    /** 関連するワークスペースセットのワークスペースからノードが削除されたときのイベントハンドラ. */
    private final Consumer<? super Workspace.NodeRemovedEvent> onNodeRemoved = this::onNodeRemoved;

    /** 関連するワークスペースセットのワークスペースのルートノード一式に新しくルートノードが追加されたときのイベントハンドラ. */
    private final Consumer<? super Workspace.RootNodeAddedEvent> onRootNodeAdded =
        this::onRootNodeAdded;

    /** 関連するワークスペースセットのワークスペースのルートノード一式に新しくルートノードが追加されたときのイベントハンドラ. */
    private final Consumer<? super Workspace.RootNodeRemovedEvent> onRootNodeRemoved =
        this::onRootNodeRemoved;

    /** 関連するワークスペースセットのワークスペースの名前が変更されたときのイベントハンドラ. */
    private final Consumer<? super Workspace.NameChangedEvent> onWsNamechanged =
        this::onWsNameChanger;

    /**
     * 関連するワークスペースセット以下のノードの選択状態が変更されたときのイベントハンドラを
     * 登録 / 削除するためのオブジェクトを取得する.
     */
    public ConsumerInvoker<NodeSelectionEvent>.Registry getOnNodeSelectionStateChanged() {
      return onNodeSelStateChangedInvoker.getRegistry();
    }

    /**
     * 関連するワークスペースセット以下のノードのコンパイルエラー状態が変更されたときのイベントハンドラを
     * 登録 / 削除するためのオブジェクトを取得する.
     */
    public ConsumerInvoker<NodeCompileErrorEvent>.Registry getOnNodeCompileErrStateChanged() {
      return onNodeCompileErrStateChangedInvoker.getRegistry();
    }

    /**
     * 関連するワークスペースセット以下のノードのブレークポイントの状態が変更されたときのイベントハンドラを
     * 登録 / 削除するためのオブジェクトを取得する.
     */
    public ConsumerInvoker<NodeBreakpointSetEvent>.Registry getOnNodeBreakpointSetEvent() {
      return onNodeBreakpointSetInvoker.getRegistry();
    }

    /**
     * 関連するワークスペースセットのワークスペースにノードが追加されたときのイベントハンドラを
     * 登録 / 削除するためのオブジェクトを取得する.
     */
    public ConsumerInvoker<NodeAddedEvent>.Registry getOnNodeAdded() {
      return onNodeAddedInvoker.getRegistry();
    }

    /**
     * 関連するワークスペースセットのワークスペースからノードが削除されたときのイベントハンドラを
     * 登録 / 削除するためのオブジェクトを取得する.
     */
    public ConsumerInvoker<NodeRemovedEvent>.Registry getOnNodeRemoved() {
      return onNodeRemovedInvoker.getRegistry();
    }

    /**
     * 関連するワークスペースセットのワークスペースのルートノード一式に新しくルートノードが追加されたときの
     * イベントハンドラのレジストリを取得する.
     */
    public ConsumerInvoker<RootNodeAddedEvent>.Registry getOnRootNodeAdded() {
      return onRootNodeAddedInvoker.getRegistry();
    }

    /**
     * 関連するワークスペースセットのワークスペースのルートノード一式からルートノードが削除されたときの
     * イベントハンドラのレジストリを取得する.
     */
    public ConsumerInvoker<RootNodeRemovedEvent>.Registry getOnRootNodeRemoved() {
      return onRootNodeRemovedInvoker.getRegistry();
    }

    /**
     * 関連するワークスペースセットにワークスペースが追加されたときのイベントハンドラを
     * 登録 / 削除するためのオブジェクトを取得する.
     */
    public ConsumerInvoker<WorkspaceAddedEvent>.Registry getOnWorkspaceAdded() {
      return onWsAddedInvoker.getRegistry();
    }

    /**
     * 関連するワークスペースセットからワークスペースが削除されたときのイベントハンドラを
     * 登録 / 削除するためのオブジェクトを取得する.
     */    
    public ConsumerInvoker<WorkspaceRemovedEvent>.Registry getOnWorkspaceRemoved() {
      return onWsRemovedInvoker.getRegistry();
    }

    /**
     * 関連するワークスペースセットで操作対象のワークスペースが変わったときのイベントハンドラを
     * 登録 / 削除するためのオブジェクトを取得する.
     */
    public ConsumerInvoker<CurrentWorkspaceChangedEvent>.Registry getOnCurrentWorkspaceChanged() {
      return onCurrentWsChangedInvoker.getRegistry();
    }

    /**
     * 関連するワークスペースセットのワークスペースの名前が変更されたときのイベントハンドラを
     * 登録 / 削除するためのオブジェクトを取得する.
     */
    public ConsumerInvoker<WorkspaceNameChangedEvent>.Registry getOnWorkspaceNameChanged() {
      return onWsNameChangedInvoker.getRegistry();
    }

    /** ノードの選択状態に変化があったときのイベントハンドラを呼ぶ. */
    private void onNodeSelectionStateChanged(Workspace.NodeSelectionEvent event) {
      onNodeSelStateChangedInvoker.invoke(new NodeSelectionEvent(
          WorkspaceSet.this, event.ws(), event.node(), event.isSelected(), event.userOpe()));
    }

    /** ノードのコンパイルエラー状態に変化があったときのイベントハンドラを呼ぶ. */
    private void onNodeCompileErrStateChanged(Workspace.NodeCompileErrorEvent event) {
      if (event.hasError()) {
        WorkspaceSet.this.compileErrNodes.addLast(event.node());
      } else {
        WorkspaceSet.this.compileErrNodes.remove(event.node());
      } 
      onNodeCompileErrStateChangedInvoker.invoke(new NodeCompileErrorEvent(
          WorkspaceSet.this, event.ws(), event.node(), event.hasError(), event.userOpe()));
    }

    /** ノードのブレークポイントの状態に変化があったときのイベントハンドラを呼ぶ. */
    private void onNodeBreakpointSetEvent(Workspace.NodeBreakpointSetEvent event) {
      onNodeBreakpointSetInvoker.invoke(new NodeBreakpointSetEvent(
          WorkspaceSet.this, event.ws(), event.node(), event.isBreakpointSet(), event.userOpe()));
    }

    /** 関連するワークスペースセットのワークスペースにノードが追加されたときのイベントハンドラを呼ぶ. */
    private void onNodeAdded(Workspace.NodeAddedEvent event) {
      if (event.node().getCompileErrState()) {
        WorkspaceSet.this.compileErrNodes.addLast(event.node());
      }
      onNodeAddedInvoker.invoke(
          new NodeAddedEvent(WorkspaceSet.this, event.ws(), event.node(), event.userOpe()));
    }

    /** 関連するワークスペースセットのワークスペースからノードが削除されたときのイベントハンドラを呼ぶ. */
    private void onNodeRemoved(Workspace.NodeRemovedEvent event) {
      WorkspaceSet.this.compileErrNodes.remove(event.node());
      onNodeRemovedInvoker.invoke(
          new NodeRemovedEvent(WorkspaceSet.this, event.ws(), event.node(), event.userOpe()));
    }

    /** 関連するワークスペースセットのワークスペースのルートノード一式に新しくルートノードが追加されたときのイベントハンドラを呼ぶ.*/
    private void onRootNodeAdded(Workspace.RootNodeAddedEvent event) {
      onRootNodeAddedInvoker.invoke(
          new RootNodeAddedEvent(WorkspaceSet.this, event.ws(), event.node(), event.userOpe()));
    }

    /** 関連するワークスペースセットのワークスペースのルートノード一式からルートノードが削除されたときのイベントハンドラを呼ぶ.*/
    private void onRootNodeRemoved(Workspace.RootNodeRemovedEvent event) {
      onRootNodeRemovedInvoker.invoke(
          new RootNodeRemovedEvent(WorkspaceSet.this, event.ws(), event.node(), event.userOpe()));
    }

    /** 関連するワークスペースセットのワークスペースの名前が変更されたときのイベントハンドラを呼ぶ. */
    private void onWsNameChanger(Workspace.NameChangedEvent event) {
      onWsNameChangedInvoker.invoke(new WorkspaceNameChangedEvent(
          WorkspaceSet.this, event.ws(), event.oldName(), event.newName()));
    }
  }

  /**
   * ワークスペースセット以下のノードの選択状態が変更されたときの情報を格納したレコード.
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
   * ワークスペースセット以下のノードのコンパイルエラー状態が変更されたときの情報を格納したレコード.
   *
   * @param wss {@code ws} を保持するワークスペースセット
   * @param ws {@code node} を保持するワークスペース
   * @param node コンパイルエラー状態が変更されたノード
   * @param hasError {@code node} がコンパイルエラーを起こした場合 true
   * @param userOpe undo 用コマンドオブジェクト
   */      
  public record NodeCompileErrorEvent(
      WorkspaceSet wss,
      Workspace ws,
      BhNode node,
      boolean hasError,
      UserOperation userOpe) {}

  /**
   * ワークスペースセット以下のノードのコンパイルエラー状態が変更されたときの情報を格納したレコード.
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
   */
  public record CurrentWorkspaceChangedEvent(WorkspaceSet wss, Workspace oldWs, Workspace newWs) {}

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
