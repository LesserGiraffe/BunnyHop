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
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.SequencedSet;
import javafx.application.Platform;
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
  /** ノードが選択された時のイベントハンドラとそれを呼び出すスレッドのフラグのマップ. */
  private transient
      Map<TriConsumer<? super BhNode, ? super Boolean, ? super UserOperation>, Boolean>
      onNodeSelectionStateChangedToThreadFlag = new HashMap<>();
  /** このワークスペースにノードが追加された時のイベントハンドラとそれを呼び出すスレッドのフラグのマップ. */
  private transient
      Map<TriConsumer<? super Workspace, ? super BhNode, ? super UserOperation>, Boolean>
      onNodeAddedToThreadFlag = new HashMap<>();
  /** このワークスペースからノードが削除された時のイベントハンドラとそれを呼び出すスレッドのフラグのマップ. */
  private transient
      Map<TriConsumer<? super Workspace, ? super BhNode, ? super UserOperation>, Boolean>
      onNodeRemovedToThreadFlag = new HashMap<>();
  /** このワークスペースからノードが削除された時のイベントハンドラを呼び出す関数オブジェクト. */
  private transient TriConsumer<? super BhNode, ? super Boolean, ? super UserOperation>
      onNodeSelectionStateChanged = this::invokeOnNodeSelectionStateChanged;
  /** このワークスペースを持つワークスペースセット. */
  private transient WorkspaceSet workspaceSet;
  /** このオブジェクトに対応するビューの処理を行うプロキシオブジェクト. */
  private transient WorkspaceViewProxy viewProxy = new WorkspaceViewProxy() {};

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
   * このワークスペースに対し {@code node} をルートノードとして指定する.
   *
   * @param node ルートノードとして指定するノード
   * @param userOpe undo 用コマンドオブジェクト
   */
  public void specifyNodeAsRoot(BhNode node, UserOperation userOpe) {
    if (!nodeList.contains(node) || rootNodeList.contains(node)) {
      return;
    }
    rootNodeList.add(node);
    getViewProxy().notifyNodeSpecifiedAsRoot(node);
    userOpe.pushCmdOfAddRootNode(node);
  }

  /**
   * このワークスペースに対し {@code node} がルートノードとして指定されているとき, その指定を解除する.
   *
   * @param node ルートの指定を解除するするノード
   * @param userOpe undo 用コマンドオブジェクト
   */
  public void specifyNodeAsNotRoot(BhNode node, UserOperation userOpe) {
    if (!nodeList.contains(node) || !rootNodeList.contains(node)) {
      return;
    }
    rootNodeList.remove(node);
    getViewProxy().notifyNodeSpecifiedAsNotRoot(node);
    userOpe.pushCmdOfRemoveRootNode(node, this);
  }

  /**
   * このワークスペースに {@link BhNode} 以下のノードを全て追加する.
   *
   * @param node このワークスペースに追加するノード
   * @param userOpe undo 用コマンドオブジェクト
   */
  public void addNodeTree(BhNode node, UserOperation userOpe) {
    CallbackInvoker.CallbackRegistry callbacks = CallbackInvoker.newCallbackRegistry();
    callbacks.setForAllNodes(bhNode -> addNode(bhNode, userOpe));
    CallbackInvoker.invoke(callbacks, node);
  }

  /**
   * このワークスペースに {@link BhNode} を追加する.
   *
   * @param node このワークスペースに追加するノード
   * @param userOpe undo 用コマンドオブジェクト
   */  
  public void addNode(BhNode node, UserOperation userOpe) {
    if (nodeList.contains(node)) {
      return;
    }
    Workspace currentWs = node.getWorkspace();
    if (currentWs != null && currentWs != this) {
      currentWs.removeNodeTree(node, userOpe);
    }
    nodeList.add(node);
    node.setWorkspace(this);
    node.addOnSelectionStateChanged(onNodeSelectionStateChanged, true);
    getViewProxy().notifyNodeAdded(node);
    invokeOnNodeAdded(node, userOpe);
    userOpe.pushCmdOfAddToWorkspace(node, this);
  }

  /**
   * このワークスペースから {@link BhNode} 以下のノードを全て削除する.
   *
   * @param node このワークスペースから削除するノード.
   * @param userOpe undo 用コマンドオブジェクト
   */
  public void removeNodeTree(BhNode node, UserOperation userOpe) {
    CallbackInvoker.CallbackRegistry callbacks = CallbackInvoker.newCallbackRegistry();
    callbacks.setForAllNodes(bhNode -> removeNode(bhNode, userOpe));
    CallbackInvoker.invoke(callbacks, node);
  }

  /**
   * このワークスペースから {@link BhNode} を削除する.
   *
   * @param node このワークスペースから削除するノード.
   * @param userOpe undo 用コマンドオブジェクト
   */
  public void removeNode(BhNode node, UserOperation userOpe) {
    if (!nodeList.contains(node)) {
      return;
    }
    if (node.isSelected()) {
      node.deselect(userOpe);
    }
    if (rootNodeList.contains(node)) {
      specifyNodeAsNotRoot(node, userOpe);
    }
    node.removeOnSelectionStateChanged(onNodeSelectionStateChanged);
    node.setWorkspace(null);
    nodeList.remove(node);
    getViewProxy().notifyNodeRemoved(node);
    invokeOnNodeRemoved(node, userOpe);
    userOpe.pushCmdOfRemoveFromWorkspace(node, this);
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
   * {@code node} をルートノードとして持っているかどうかチェックする.
   *
   * @param node ワークスペース直下のルートノードかどうかを調べるノード
   * @return {@code node} をルートノードとして持っている場合 true
   */
  public boolean containsAsRoot(BhNode node) {
    return rootNodeList.contains(node);
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
   * このワークスペースのノードの選択状態に変化があった時のイベントハンドラを追加する.
   *
   * @param handler 追加するイベントハンドラ
   * @param invokeOnUiThread UI スレッド上で呼び出す場合 true
   */
  public void addOnNodeSelectionStateChanged(
      TriConsumer<? super BhNode, ? super Boolean, ? super UserOperation> handler,
      boolean invokeOnUiThread) {
    onNodeSelectionStateChangedToThreadFlag.put(handler, invokeOnUiThread);
  }

  /**
   * このワークスペースのノードの選択状態に変化があった時のイベントハンドラを削除する.
   *
   * @param handler 削除するイベントハンドラ
   */
  public void removeOnNodeSelectionStateChanged(
      TriConsumer<? super BhNode, ? super Boolean, ? super UserOperation> handler) {
    onNodeSelectionStateChangedToThreadFlag.remove(handler);
  }

  /**
   * このワークスペースにノードが追加された時のイベントハンドラを追加する.
   *
   * @param handler 追加するイベントハンドラ
   * @param invokeOnUiThread UI スレッド上で呼び出す場合 true
   */
  public void addOnNodeAdded(
      TriConsumer<? super Workspace, ? super BhNode, ? super UserOperation> handler,
      boolean invokeOnUiThread) {
    onNodeAddedToThreadFlag.put(handler, invokeOnUiThread);
  }

  /**
   * このワークスペースにノードが追加された時のイベントハンドラを削除する.
   *
   * @param handler 削除するイベントハンドラ
   */
  public void removeOnNodeAdded(
      TriConsumer<? super Workspace, ? super BhNode, ? super UserOperation> handler) {
    onNodeAddedToThreadFlag.remove(handler);
  }

  /**
   * このワークスペースからノードが削除された時のイベントハンドラを追加する.
   *
   * @param handler 追加するイベントハンドラ
   * @param invokeOnUiThread UI スレッド上で呼び出す場合 true
   */
  public void addOnNodeRemoved(
      TriConsumer<? super Workspace, ? super BhNode, ? super UserOperation> handler,
      boolean invokeOnUiThread) {
    onNodeRemovedToThreadFlag.put(handler, invokeOnUiThread);
  }

  /**
   * このワークスペースからノードが削除された時のイベントハンドラを削除する.
   *
   * @param handler 削除するイベントハンドラ
   */
  public void removeOnNodeRemoved(
      TriConsumer<? super Workspace, ? super BhNode, ? super UserOperation> handler) {
    onNodeRemovedToThreadFlag.remove(handler);
  }

  /** ノードの選択状態が変わった時のイベントハンドラを呼び出す. */
  private void invokeOnNodeSelectionStateChanged(
      BhNode node, boolean isSelected, UserOperation userOpe) {
    if (isSelected) {
      addToSelectedNodeList(node);
    } else {
      removeFromSelectedNodeList(node);
    }
    onNodeSelectionStateChangedToThreadFlag.forEach((handler, invokeOnUiThread) -> {
      if (invokeOnUiThread && !Platform.isFxApplicationThread()) {
        Platform.runLater(() -> handler.accept(node, isSelected, userOpe));
      } else {
        handler.accept(node, isSelected, userOpe);
      }
    });
  }

  /** このワークスペースにノードが追加された時のイベントハンドラを呼び出す. */
  private void invokeOnNodeAdded(BhNode node, UserOperation userOpe) {
    onNodeAddedToThreadFlag.forEach((handler, invokeOnUiThread) -> {
      if (invokeOnUiThread && !Platform.isFxApplicationThread()) {
        Platform.runLater(() -> handler.accept(this, node, userOpe));
      } else {
        handler.accept(this, node, userOpe);
      }
    });
  }

  /** このワークスペースからノードが削除された時のイベントハンドラを呼び出す. */
  private void invokeOnNodeRemoved(BhNode node, UserOperation userOpe) {
    onNodeRemovedToThreadFlag.forEach((handler, invokeOnUiThread) -> {
      if (invokeOnUiThread && !Platform.isFxApplicationThread()) {
        Platform.runLater(() -> handler.accept(this, node, userOpe));
      } else {
        handler.accept(this, node, userOpe);
      }      
    });
  }
}
