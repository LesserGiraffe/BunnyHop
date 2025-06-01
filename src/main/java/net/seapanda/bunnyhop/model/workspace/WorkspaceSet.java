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
  private CallbackRegistry eventManager = new CallbackRegistry();

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
    registry.getOnNodeSelectionStateChanged().add(eventManager.onNodeSelectionStateChanged);
    registry.getOnNodeCompileErrorStateChanged().add(eventManager.onNodeCompileErrStateChanged);
    registry.getOnNodeAdded().add(eventManager.onNodeAdded);
    registry.getOnNodeRemoved().add(eventManager.onNodeRemoved);
    registry.getOnRootNodeAdded().add(eventManager.onRootNodeAdded);
    registry.getOnRootNodeRemoved().add(eventManager.onRootNodeRemoved);
    eventManager.onWsAddedInvoker.invoke(new WorkspaceAddedEvent(this, workspace, userOpe));
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
    registry.getOnNodeSelectionStateChanged().remove(eventManager.onNodeSelectionStateChanged);
    registry.getOnNodeCompileErrorStateChanged().remove(eventManager.onNodeCompileErrStateChanged);
    registry.getOnNodeAdded().remove(eventManager.onNodeAdded);
    registry.getOnNodeRemoved().remove(eventManager.onNodeRemoved);
    registry.getOnRootNodeAdded().remove(eventManager.onRootNodeAdded);
    registry.getOnRootNodeRemoved().remove(eventManager.onRootNodeRemoved);
    eventManager.onWsRemovedInvoker.invoke(new WorkspaceRemovedEvent(this, workspace, userOpe));
    userOpe.pushCmdOfRemoveWorkspace(workspace, this);
  }

  /** {@link ws} の中のノードを全て消す. */
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
    eventManager.onCurrentWsChangedInvoker.invoke(new CurrentWorkspaceChangedEvent(this, old, ws));
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
    return eventManager;
  }

  /** {@link WorkspaceSet} に対するイベントハンドラの追加と削除を行うクラス. */
  public class CallbackRegistry {

    /** このワークスペースセット以下のノードの選択状態が変更されたときに呼び出すメソッドを管理するオブジェクト. */
    private final ConsumerInvoker<NodeSelectionEvent> onNodeSelStateChangedInvoker =
        new ConsumerInvoker<>();
    
    /** このワークスペースセット以下のノードのコンパイルエラー状態が変更されたときに呼び出すメソッドを管理するオブジェクト. */
    private final ConsumerInvoker<NodeCompileErrorEvent>
        onNodeCompileErrStateChangedInvoker = new ConsumerInvoker<>();

    /** このワークスペースセットのワークスペースにノードが追加されたときに呼び出すメソッドを管理するオブジェクト. */
    private final ConsumerInvoker<NodeAddedEvent> onNodeAddedInvoker =
        new ConsumerInvoker<>();

    /** このワークスペースセットのワークスペースからノードが削除されたときに呼び出すメソッドを管理するオブジェクト. */
    private final ConsumerInvoker<NodeRemovedEvent> onNodeRemovedInvoker = 
        new ConsumerInvoker<>();

    /**
     * このワークスペースセットのワークスペースのルートノード一式に新しくルートノードが追加されたときに
     * 呼び出すメソッドを管理するオブジェクト.
     */
    private final ConsumerInvoker<RootNodeAddedEvent> onRootNodeAddedInvoker =
        new ConsumerInvoker<>();

    /**
     * このワークスペースセットのワークスペースのルートノード一式からルートノードが削除されたときに
     * 呼び出すメソッドを管理するオブジェクト.
     */
    private final ConsumerInvoker<RootNodeRemovedEvent> onRootNodeRemovedInvoker = 
        new ConsumerInvoker<>();
    
    /** このワークスペースセットにワークスペースが追加されたときに呼び出すメソッドを管理するオブジェクト. */
    private final ConsumerInvoker<WorkspaceAddedEvent> onWsAddedInvoker =
        new ConsumerInvoker<>();

    /** このワークスペースセットからワークスペースが削除されたときに呼び出すメソッドを管理するオブジェクト. */
    private final ConsumerInvoker<WorkspaceRemovedEvent> onWsRemovedInvoker =
        new ConsumerInvoker<>();

    /** このワークスペースセットで操作対象のワークスペースが変わったときに呼び出すメソッドを管理するオブジェクト. */
    private final ConsumerInvoker<CurrentWorkspaceChangedEvent> onCurrentWsChangedInvoker =
        new ConsumerInvoker<>();

    /** ノードの選択状態が変わったときのイベントハンドラ. */
    private final Consumer<? super Workspace.NodeSelectionEvent> onNodeSelectionStateChanged =
        this::onNodeSelectionStateChanged;

    /** ノードのコンパイルエラー状態が変わったときのイベントハンドラ. */
    private final Consumer<? super Workspace.NodeCompileErrorEvent> onNodeCompileErrStateChanged =
        this::onNodeCompileErrStateChanged;

    /** このワークスペースセットのワークスペースにノードが追加されたときのイベントハンドラ. */
    private final Consumer<? super Workspace.NodeAddedEvent> onNodeAdded = this::onNodeAdded;

    /** このワークスペースセットのワークスペースからノードが削除されたときのイベントハンドラ. */
    private final Consumer<? super Workspace.NodeRemovedEvent> onNodeRemoved = this::onNodeRemoved;

    /** このワークスペースセットのワークスペースのルートノード一式に新しくルートノードが追加されたときのイベントハンドラ. */
    private final Consumer<? super Workspace.RootNodeAddedEvent> onRootNodeAdded =
        this::onRootNodeAdded;

    /** このワークスペースセットのワークスペースのルートノード一式に新しくルートノードが追加されたときのイベントハンドラ. */
    private final Consumer<? super Workspace.RootNodeRemovedEvent> onRootNodeRemoved =
        this::onRootNodeRemoved;

    /**
     * このワークスペースセット以下のノードの選択状態が変更されたときのイベントハンドラを
     * 登録 / 削除するためのオブジェクトを取得する.
     */
    public ConsumerInvoker<NodeSelectionEvent>.Registry getOnNodeSelectionStateChanged() {
      return onNodeSelStateChangedInvoker.getRegistry();
    }

    /**
     * このワークスペースセット以下のノードのコンパイルエラー状態が変更されたときのイベントハンドラを
     * 登録 / 削除するためのオブジェクトを取得する.
     */
    public ConsumerInvoker<NodeCompileErrorEvent>.Registry getOnNodeCompileErrStateChanged() {
      return onNodeCompileErrStateChangedInvoker.getRegistry();
    }

    /**
     * このワークスペースセットのワークスペースにノードが追加されたときのイベントハンドラを
     * 登録 / 削除するためのオブジェクトを取得する.
     */
    public ConsumerInvoker<NodeAddedEvent>.Registry getOnNodeAdded() {
      return onNodeAddedInvoker.getRegistry();
    }

    /**
     * このワークスペースセットのワークスペースからノードが削除されたときのイベントハンドラを
     * 登録 / 削除するためのオブジェクトを取得する.
     */
    public ConsumerInvoker<NodeRemovedEvent>.Registry getOnNodeRemoved() {
      return onNodeRemovedInvoker.getRegistry();
    }

    /**
     * このワークスペースセットのワークスペースのルートノード一式に新しくルートノードが追加されたときの
     * イベントハンドラを登録 / 削除するためのオブジェクトを取得する.
     */
    public ConsumerInvoker<RootNodeAddedEvent>.Registry getOnRootNodeAdded() {
      return onRootNodeAddedInvoker.getRegistry();
    }

    /**
     * このワークスペースセットのワークスペースのルートノード一式からルートノードが削除されたときの
     * イベントハンドラを登録 / 削除するためのオブジェクトを取得する.
     */
    public ConsumerInvoker<RootNodeRemovedEvent>.Registry getOnRootNodeRemoved() {
      return onRootNodeRemovedInvoker.getRegistry();
    }

    /**
     * このワークスペースセットにワークスペースが追加されたときのイベントハンドラを
     * 登録 / 削除するためのオブジェクトを取得する.
     */
    public ConsumerInvoker<WorkspaceAddedEvent>.Registry getOnWorkspaceAdded() {
      return onWsAddedInvoker.getRegistry();
    }

    /**
     * このワークスペースセットからワークスペースが削除されたときのイベントハンドラを
     * 登録 / 削除するためのオブジェクトを取得する.
     */    
    public ConsumerInvoker<WorkspaceRemovedEvent>.Registry getOnWorkspaceRemoved() {
      return onWsRemovedInvoker.getRegistry();
    }

    /**
     * このワークスペースセットで操作対象のワークスペースが変わったときのイベントハンドラを
     * 登録 / 削除するためのイブジェクトを取得する.
     */
    public ConsumerInvoker<CurrentWorkspaceChangedEvent>.Registry getOnCurrentWorkspaceChanged() {
      return onCurrentWsChangedInvoker.getRegistry();
    }

    /** ノードの選択状態に変化があった時のイベントハンドラを呼ぶ. */
    private void onNodeSelectionStateChanged(Workspace.NodeSelectionEvent event) {
      onNodeSelStateChangedInvoker.invoke(new NodeSelectionEvent(
          WorkspaceSet.this, event.ws(), event.node(), event.isSelected(), event.userOpe()));
    }

    /** ノードのコンパイルエラー状態に変化があった時のイベントハンドラを呼ぶ. */
    private void onNodeCompileErrStateChanged(Workspace.NodeCompileErrorEvent event) {
      if (event.hasError()) {
        WorkspaceSet.this.compileErrNodes.addLast(event.node());
      } else {
        WorkspaceSet.this.compileErrNodes.remove(event.node());
      } 
      onNodeCompileErrStateChangedInvoker.invoke(new NodeCompileErrorEvent(
          WorkspaceSet.this, event.ws(), event.node(), event.hasError(), event.userOpe()));
    }

    /** このワークスペースセットのワークスペースにノードが追加されたときのイベントハンドラを呼ぶ. */
    private void onNodeAdded(Workspace.NodeAddedEvent event) {
      if (event.node().getCompileErrState()) {
        WorkspaceSet.this.compileErrNodes.addLast(event.node());
      }
      onNodeAddedInvoker.invoke(
          new NodeAddedEvent(WorkspaceSet.this, event.ws(), event.node(), event.userOpe()));
    }

    /** このワークスペースセットのワークスペースからノードが削除されたときのイベントハンドラを呼ぶ. */
    private void onNodeRemoved(Workspace.NodeRemovedEvent event) {
      WorkspaceSet.this.compileErrNodes.remove(event.node());
      onNodeRemovedInvoker.invoke(
          new NodeRemovedEvent(WorkspaceSet.this, event.ws(), event.node(), event.userOpe()));
    }

    /** このワークスペースセットのワークスペースのルートノード一式に新しくルートノードが追加されたときのイベントハンドラを呼ぶ.*/
    private void onRootNodeAdded(Workspace.RootNodeAddedEvent event) {
      onRootNodeAddedInvoker.invoke(
          new RootNodeAddedEvent(WorkspaceSet.this, event.ws(), event.node(), event.userOpe()));
    }

    /** このワークスペースセットのワークスペースのルートノード一式からルートノードが削除されたときのイベントハンドラを呼ぶ.*/
    private void onRootNodeRemoved(Workspace.RootNodeRemovedEvent event) {
      onRootNodeRemovedInvoker.invoke(
          new RootNodeRemovedEvent(WorkspaceSet.this, event.ws(), event.node(), event.userOpe()));
    }
  }

  /**
   * このワークスペースセット以下のノードの選択状態が変更されたときの情報を格納したレコード.
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
   * このワークスペースセット以下のノードのコンパイルエラー状態が変更されたときの情報を格納したレコード.
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
   * このワークスペースセットのワークスペースにノードが追加されたときの情報を格納したレコード.
   *
   * @param wss {@code ws} を保持するワークスペースセット
   * @param ws {@code node} が追加されたワークスペース
   * @param node {@code ws} に追加されたノード
   * @param userOpe undo 用コマンドオブジェクト
   */
  public record NodeAddedEvent(
      WorkspaceSet wss, Workspace ws, BhNode node, UserOperation userOpe) {}

  /**
   * このワークスペースセットのワークスペースからノードが削除されたときの情報を格納したレコード.
   *
   * @param wss {@code ws} を保持するワークスペースセット
   * @param ws {@code node} が削除されたワークスペース
   * @param node {@code ws} から削除されたノード
   * @param userOpe undo 用コマンドオブジェクト
   */
  public record NodeRemovedEvent(
      WorkspaceSet wss, Workspace ws, BhNode node, UserOperation userOpe) {}

  /**
   * このワークスペースセットのワークスペースのルートノード一式に新しくルートノードが追加されたときの情報を格納したレコード.
   *
   * @param wss {@code ws} を保持するワークスペースセット
   * @param ws {@code node} をルートノードとして保持するワークスペース
   * @param node {@code ws} 上でルートノードとなったノード
   * @param userOpe undo 用コマンドオブジェクト
   */
  public record RootNodeAddedEvent(
      WorkspaceSet wss, Workspace ws, BhNode node, UserOperation userOpe) {}

  /**
   * このワークスペースセットのワークスペースのルートノード一式からルートノードが削除されたときの情報を格納したレコード.
   *
   * @param wss {@code ws} を保持するワークスペースセット
   * @param ws このワークスペース上の {@code node} が非ルートノードとなった
   * @param node 非ルートノードとなったノード
   * @param userOpe undo 用コマンドオブジェクト
   */  
  public record RootNodeRemovedEvent(
      WorkspaceSet wss, Workspace ws, BhNode node, UserOperation userOpe) {}

  /**
   * このワークスペースセットにワークスペースが追加されたときの情報を格納したレコード.
   *
   * @param wss {@code ws} が追加されたワークスペースセット
   * @param ws {@code wss} に追加されたワークスペース
   * @param userOpe undo 用コマンドオブジェクト
   */
  public record WorkspaceAddedEvent(WorkspaceSet wss, Workspace ws, UserOperation userOpe) {}

  /**
   * このワークスペースセットからワークスペースが削除されたときの情報を格納したレコード.
   *
   * @param wss {@code ws} が削除されたワークスペースセット
   * @param ws {@code wss} から削除されたワークスペース
   * @param userOpe undo 用コマンドオブジェクト
   */
  public record WorkspaceRemovedEvent(WorkspaceSet wss, Workspace ws, UserOperation userOpe) {}

  /**
   * このワークスペースセットで操作対象のワークスペースが変更されたときの情報を格納したレコード.
   *
   * @param wss 操作対象のワークスペースが変更されたワークスペースセット
   * @param oldWs {@code newWs} の前に操作対象であったワークスペース
   * @param newWs 新しく操作対象となったワークスペース
   */
  public record CurrentWorkspaceChangedEvent(WorkspaceSet wss, Workspace oldWs, Workspace newWs) {}
}
