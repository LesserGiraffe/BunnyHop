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

import java.io.Serializable;
import java.util.LinkedHashSet;
import java.util.SequencedSet;
import net.seapanda.bunnyhop.model.node.BhNode;
import net.seapanda.bunnyhop.model.traverse.CallbackInvoker;
import net.seapanda.bunnyhop.undo.UserOperation;
import net.seapanda.bunnyhop.view.proxy.WorkspaceViewProxy;
import org.apache.commons.lang3.function.TriConsumer;

/**
 * ワークスペースを表すクラス.
 *
 * @author K.Koike
 */
public class Workspace implements Serializable {
  
  /** ワークスペースのルートノードのリスト. */
  private final SequencedSet<BhNode> rootNodeList = new LinkedHashSet<>();
  /** 選択中のノード. 挿入順を保持したいので LinkedHashSet を使う. */
  private final SequencedSet<BhNode> selectedList = new LinkedHashSet<>();
  /** このワークスペースが保持する {@link BhNode} のセット. */
  private final SequencedSet<BhNode> nodeList = new LinkedHashSet<>();
  /** ワークスペース名. */
  private String name;
  /** このワークスペースを持つワークスペースセット. */
  private transient WorkspaceSet workspaceSet;
  /** このオブジェクトに対応するビューの処理を行うプロキシオブジェクト. */
  private transient WorkspaceViewProxy viewProxy = new WorkspaceViewProxy() {};
  /** このワークスペースに登録されたイベントハンドラを管理するオブジェクト. */
  private transient EventManager eventManager = new EventManager();

  /**
   * コンストラクタ.
   *
   * @param name ワークスペース名
   */
  public Workspace(String name) {
    name = (name == null) ? "" : name;
    this.name = name;
  }

  /**
   * このワークスペースに {@code root} 以下のノードを全て追加する.
   * このメソッドを同じノードに対して複数回呼んでも重複追加はされない.
   *
   * @param root このワークスペースに追加するノードツリーのルートノード
   * @param userOpe undo 用コマンドオブジェクト
   */
  public void addNodeTree(BhNode root, UserOperation userOpe) {
    Workspace currentWs = root.getWorkspace();
    // 1 つのノードツリーの全てのノードは同じワークスペースに入っていなければならないので, 
    // 同じワークスペースへの重複追加を避けたければ, 追加するツリーのルートを調べればよい.
    if (currentWs == this) {
      return;
    }
    if (currentWs != null) {
      currentWs.removeNodeTree(root, userOpe);
    }
    CallbackInvoker.CallbackRegistry callbacks = CallbackInvoker.newCallbackRegistry();
    callbacks.setForAllNodes(bhNode -> addNode(bhNode, userOpe));
    CallbackInvoker.invoke(callbacks, root);
    if (!rootNodeList.contains(root) && root.isRoot()) {
      rootNodeList.add(root);
      eventManager.invokeOnNodeTurnedIntoRoot(root, userOpe);
    }
    userOpe.pushCmdOfAddNodeTreeToWorkspace(root, this);
  }

  /**
   * このワークスペースに {@code node} を追加する.
   *
   * @param node このワークスペースに追加するノード
   * @param userOpe undo 用コマンドオブジェクト
   */  
  private void addNode(BhNode node, UserOperation userOpe) {
    if (nodeList.contains(node)) {
      return;
    }
    nodeList.add(node);
    node.setWorkspace(this, userOpe);
    node.getEventManager().addOnSelectionStateChanged(eventManager.onNodeSelectionStateChanged);
    node.getEventManager().addOnNodeReplaced(eventManager.onNodeReplaced);
    eventManager.invokeOnNodeAdded(node, userOpe);
  }

  /**
   * このワークスペースから {@code root} 以下のノードを全て削除する.
   *
   * @param root このワークスペースから削除するノードツリーのルートノード.
   * @param userOpe undo 用コマンドオブジェクト
   */
  public void removeNodeTree(BhNode root, UserOperation userOpe) {
    if (root.getWorkspace() != this) {
      return;
    }
    if (rootNodeList.contains(root)) {
      rootNodeList.remove(root);
      eventManager.invokeOnNodeTurnedIntoNotRoot(root, userOpe);
    }
    CallbackInvoker.CallbackRegistry callbacks = CallbackInvoker.newCallbackRegistry();
    callbacks.setForAllNodes(bhNode -> removeNode(bhNode, userOpe));
    CallbackInvoker.invoke(callbacks, root);
    userOpe.pushCmdOfRemoveNodeTreeFromWorkspace(root, this);
  }

  /**
   * このワークスペースから {@link BhNode} を削除する.
   *
   * @param node このワークスペースから削除するノード.
   * @param userOpe undo 用コマンドオブジェクト
   */
  private void removeNode(BhNode node, UserOperation userOpe) {
    if (!nodeList.contains(node)) {
      return;
    }
    if (node.isSelected()) {
      node.deselect(userOpe);
    }
    node.getEventManager().removeOnSelectionStateChanged(eventManager.onNodeSelectionStateChanged);
    node.getEventManager().removeOnNodeReplaced(eventManager.onNodeReplaced);
    node.setWorkspace(null, userOpe);
    nodeList.remove(node);
    eventManager.invokeOnNodeRemoved(node, userOpe);
  }

  /**
   * このワークスペースを持つワークスペースセットをセットする.
   *
   * @param wss このワークスペースを持つワークスペースセット
   */
  public void setWorkspaceSet(WorkspaceSet wss) {
    workspaceSet = wss;
  }

  /**
   * このワークスペースを持つワークスペースセットを返す.
   *
   * @return このワークスペースを持つワークスペースセット
   */
  public WorkspaceSet getWorkspaceSet() {
    return workspaceSet;
  }

  /**
   * ワークスペース内のルート {@link BhNode} の集合を返す.
   *
   * @return ワークスペース内のルート {@link BhNode} の集合
   */
  public SequencedSet<BhNode> getRootNodes() {
    return new LinkedHashSet<>(rootNodeList);
  }

  /**
   * このワークスペース内の全ての {@link BhNode} を返す.
   *
   * @return このワークスペース内の全ての {@link BhNode}
   */
  public SequencedSet<BhNode> getNodes() {
    return new LinkedHashSet<>(nodeList);
  }

  /**
   * {@code toSelect} を選択済みノードリストに追加する.
   *
   * @param toAdd 選択済みノードリストに追加するノード
   * @param userOpe undo 用コマンドオブジェクト
   */
  private void addToSelectedNodeList(BhNode toAdd) {
    if (selectedList.contains(toAdd)) {
      return;
    }
    selectedList.add(toAdd);
  }

  /**
   * {@link toRemove} を選択済みノードリストから削除する.
   *
   * @param toRemove 選択済みリストから削除する {@link BhNode}
   * @param userOpe undo 用コマンドオブジェクト
   */
  private void removeFromSelectedNodeList(BhNode toRemove) {
    if (!selectedList.contains(toRemove)) {
      return;
    }
    selectedList.remove(toRemove);
  }

  /**
   * このワークスペース内で選択済みの {@link BhNode} のリストを返す.
   *
   * @return 選択中の {@link BhNode} のリスト
   */
  public SequencedSet<BhNode> getSelectedNodes() {
    return new LinkedHashSet<>(selectedList);
  }

  /**
   * ワークスペース名を取得する.
   *
   * @return ワークスペース名
   */
  public String getName() {
    return name;
  }

  /**
   * ワークスペース名を設定する.
   *
   * @param name 設定するワークスペース名
   */
  public void setName(String name) {
    name = (name == null) ? "" : name;
    this.name = name;
  }

  /** このオブジェクトに対応するビューの処理を行うプロキシオブジェクトを取得する. */
  public WorkspaceViewProxy getViewProxy() {
    return viewProxy;
  }

  /** このオブジェクトに対応するビューの処理を行うプロキシオブジェクトを設定する. */
  public void setViewProxy(WorkspaceViewProxy viewProxy) {
    this.viewProxy = viewProxy;
  }

  /**
   * このワークスペースに対するイベントハンドラの追加と削除を行うオブジェクトを返す.
   *
   * @return このワークスペースに対するイベントハンドラの追加と削除を行うオブジェクト
   */
  public EventManager getEventManager() {
    return eventManager;
  }

  /** イベントハンドラの管理を行うクラス. */
  public class EventManager {
    
    /** ノードが選択されたときのイベントハンドラのセット. */
    private transient
        SequencedSet<TriConsumer<? super BhNode, ? super Boolean, ? super UserOperation>>
        onNodeSelectionStateChangedList = new LinkedHashSet<>();
    /** このワークスペースにノードが追加されたときのイベントハンドラのセット. */
    private transient
        SequencedSet<TriConsumer<? super Workspace, ? super BhNode, ? super UserOperation>>
        onNodeAddedList = new LinkedHashSet<>();
    /** このワークスペースからノードが削除されたときのイベントハンドラのセット. */
    private transient
        SequencedSet<TriConsumer<? super Workspace, ? super BhNode, ? super UserOperation>>
        onNodeRemoved = new LinkedHashSet<>();
    /** このワークスペースのノードがルートノードとなったときのイベントハンドラのセット. */
    private transient
        SequencedSet<TriConsumer<? super Workspace, ? super BhNode, ? super UserOperation>>
        onNodeTurnedIntoRootList = new LinkedHashSet<>();
    /** このワークスペースのノードが非ルートノードとなったときのイベントハンドラのセット. */
    private transient
        SequencedSet<TriConsumer<? super Workspace, ? super BhNode, ? super UserOperation>>
        onNodeTurnedIntoNotRootList = new LinkedHashSet<>();
    /** このワークスペースからノードが削除されたときのイベントハンドラを呼び出す関数オブジェクト. */
    private transient TriConsumer<? super BhNode, ? super Boolean, ? super UserOperation>
        onNodeSelectionStateChanged = this::invokeOnNodeSelectionStateChanged;
    /** このワークスペースのノードが他のノードと入れ替わったときのイベントハンドラを呼び出す関数オブジェクト. */
    private transient TriConsumer<? super BhNode, ? super BhNode, ? super UserOperation>
        onNodeReplaced = this::invokeOnNodeReplaced;

    /**
     * このワークスペースのノードの選択状態に変化があったときのイベントハンドラを追加する.
     *
     * @param handler 追加するイベントハンドラ
     */
    public void addOnNodeSelectionStateChanged(
        TriConsumer<? super BhNode, ? super Boolean, ? super UserOperation> handler) {
      onNodeSelectionStateChangedList.addLast(handler);
    }

    /**
     * このワークスペースのノードの選択状態に変化があったときのイベントハンドラを削除する.
     *
     * @param handler 削除するイベントハンドラ
     */
    public void removeOnNodeSelectionStateChanged(
        TriConsumer<? super BhNode, ? super Boolean, ? super UserOperation> handler) {
      onNodeSelectionStateChangedList.remove(handler);
    }

    /**
     * このワークスペースにノードが追加されたときのイベントハンドラを追加する.
     *
     * @param handler 追加するイベントハンドラ
     */
    public void addOnNodeAdded(
        TriConsumer<? super Workspace, ? super BhNode, ? super UserOperation> handler) {
      onNodeAddedList.addLast(handler);
    }

    /**
     * このワークスペースにノードが追加されたときのイベントハンドラを削除する.
     *
     * @param handler 削除するイベントハンドラ
     */
    public void removeOnNodeAdded(
        TriConsumer<? super Workspace, ? super BhNode, ? super UserOperation> handler) {
      onNodeAddedList.remove(handler);
    }

    /**
     * このワークスペースからノードが削除されたときのイベントハンドラを追加する.
     *
     * @param handler 追加するイベントハンドラ
     */
    public void addOnNodeRemoved(
        TriConsumer<? super Workspace, ? super BhNode, ? super UserOperation> handler) {
      onNodeRemoved.addLast(handler);
    }

    /**
     * このワークスペースからノードが削除されたときのイベントハンドラを削除する.
     *
     * @param handler 削除するイベントハンドラ
     */
    public void removeOnNodeRemoved(
        TriConsumer<? super Workspace, ? super BhNode, ? super UserOperation> handler) {
      onNodeRemoved.remove(handler);
    }

    /**
     * このワークスペースの非ルートノードがルートノードとなったときのイベントハンドラを追加する.
     *
     * @param handler 追加するイベントハンドラ
     */
    public void addOnNodeTurnedIntoRoot(
        TriConsumer<? super Workspace, ? super BhNode, ? super UserOperation> handler) {
      onNodeTurnedIntoRootList.addLast(handler);
    }

    /**
     * このワークスペースの非ルートノードがルートノードとなったときのイベントハンドラを削除する.
     *
     * @param handler 削除するイベントハンドラ
     */
    public void removeOnNodeTurnedIntoRoot(
        TriConsumer<? super Workspace, ? super BhNode, ? super UserOperation> handler) {
      onNodeTurnedIntoRootList.remove(handler);
    }

    /**
     * このワークスペースのルートノードが非ルートノードとなったときのイベントハンドラを追加する.
     *
     * @param handler 追加するイベントハンドラ
     */
    public void addOnNodeTurnedIntoNotRoot(
        TriConsumer<? super Workspace, ? super BhNode, ? super UserOperation> handler) {
      onNodeTurnedIntoNotRootList.addLast(handler);
    }

    /**
     * このワークスペースのルートノードが非ルートノードとなったときのイベントハンドラを削除する.
     *
     * @param handler 削除するイベントハンドラ
     */
    public void removeOnNodeTurnedIntoNotRoot(
        TriConsumer<? super Workspace, ? super BhNode, ? super UserOperation> handler) {
      onNodeTurnedIntoNotRootList.remove(handler);
    }

    /** ノードの選択状態が変わったときのイベントハンドラを呼び出す. */
    private void invokeOnNodeSelectionStateChanged(
        BhNode node, boolean isSelected, UserOperation userOpe) {
      if (isSelected) {
        addToSelectedNodeList(node);
      } else {
        removeFromSelectedNodeList(node);
      }
      onNodeSelectionStateChangedList.forEach(handler -> handler.accept(node, isSelected, userOpe));
    }

    /** このワークスペースにノードが追加されたときのイベントハンドラを呼び出す. */
    private void invokeOnNodeAdded(BhNode node, UserOperation userOpe) {
      onNodeAddedList.forEach(handler -> handler.accept(Workspace.this, node, userOpe));
    }

    /** このワークスペースからノードが削除されたときのイベントハンドラを呼び出す. */
    private void invokeOnNodeRemoved(BhNode node, UserOperation userOpe) {
      onNodeRemoved.forEach(handler -> handler.accept(Workspace.this, node, userOpe));
    }

    /** このワークスペースのノードが他のノードと入れ替わったときのイベントハンドラを呼び出す. */
    private void invokeOnNodeReplaced(BhNode oldNode, BhNode newNode, UserOperation userOpe) {
      if (oldNode != null) {
        if (!oldNode.isRoot()) {
          throw new AssertionError("A replaced old node is not a root node.");
        }
        rootNodeList.add(oldNode);
        invokeOnNodeTurnedIntoRoot(oldNode, userOpe);
      }
      if (newNode != null) {
        rootNodeList.remove(newNode);
        invokeOnNodeTurnedIntoNotRoot(newNode, userOpe);
      }
    }

    /** このワークスペースの非ルートノードがルートノードになったときのイベントハンドラを呼び出す. */
    private void invokeOnNodeTurnedIntoRoot(BhNode node, UserOperation userOpe) {
      onNodeTurnedIntoRootList.forEach(handler -> handler.accept(Workspace.this, node, userOpe));
    }

    /** このワークスペースのルートノードが非ルートノードになったときのイベントハンドラを呼び出す. */
    private void invokeOnNodeTurnedIntoNotRoot(BhNode node, UserOperation userOpe) {
      onNodeTurnedIntoNotRootList.forEach(handler -> handler.accept(Workspace.this, node, userOpe));
    }
  }
}
