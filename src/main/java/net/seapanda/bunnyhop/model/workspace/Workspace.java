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
import java.util.Objects;
import java.util.Optional;
import java.util.SequencedSet;
import java.util.function.Consumer;
import net.seapanda.bunnyhop.model.node.BhNode;
import net.seapanda.bunnyhop.model.traverse.CallbackInvoker;
import net.seapanda.bunnyhop.undo.UserOperation;
import net.seapanda.bunnyhop.utility.function.ConsumerInvoker;
import net.seapanda.bunnyhop.view.workspace.WorkspaceView;

/**
 * ワークスペースを表すクラス.
 *
 * @author K.Koike
 */
public class Workspace implements Serializable {
  
  /** ワークスペースのルートノードのリスト. */
  private final SequencedSet<BhNode> rootNodes = new LinkedHashSet<>();
  /** 選択中のノード. 挿入順を保持したいので LinkedHashSet を使う. */
  private final SequencedSet<BhNode> selectedList = new LinkedHashSet<>();
  /** このワークスペースが保持する {@link BhNode} のセット. */
  private final SequencedSet<BhNode> nodeList = new LinkedHashSet<>();
  /** ワークスペース名. */
  private String name;
  /** このワークスペースを持つワークスペースセット. */
  private transient WorkspaceSet workspaceSet;
  /** このワークスペースに対応するビュー. */
  private transient WorkspaceView view;
  /** このワークスペースに登録されたイベントハンドラを管理するオブジェクト. */
  private transient CallbackRegistry cbRegistry = new CallbackRegistry();

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
    if (!rootNodes.contains(root) && root.isRoot()) {
      rootNodes.add(root);
      cbRegistry.onRootNodeAddedInvoker.invoke(new RootNodeAddedEvent(this, root, userOpe));
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
    BhNode.CallbackRegistry registry = node.getCallbackRegistry();
    registry.getOnSelectionStateChanged().add(cbRegistry.onNodeSelStateChanged);
    registry.getOnCompileErrorStateChanged().add(cbRegistry.onNodeCompileErrStateChanged);
    registry.getOnConnected().add(cbRegistry.onNodeConnected);
    cbRegistry.onNodeAddedInvoker.invoke(new NodeAddedEvent(this, node, userOpe));
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
    if (rootNodes.contains(root)) {
      rootNodes.remove(root);
      cbRegistry.onRootNodeRemovedInvoker.invoke(new RootNodeRemovedEvent(this, root, userOpe));
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
    BhNode.CallbackRegistry registry = node.getCallbackRegistry();
    registry.getOnSelectionStateChanged().remove(cbRegistry.onNodeSelStateChanged);
    registry.getOnCompileErrorStateChanged().remove(cbRegistry.onNodeCompileErrStateChanged);
    registry.getOnConnected().remove(cbRegistry.onNodeConnected);
    node.setWorkspace(null, userOpe);
    nodeList.remove(node);
    cbRegistry.onNodeRemovedInvoker.invoke(new NodeRemovedEvent(this, node, userOpe));
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
    return new LinkedHashSet<>(rootNodes);
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

  /** このワークスペースに対応するビューを取得する. */
  public Optional<WorkspaceView> getView() {
    return Optional.ofNullable(view);
  }

  /** このワークスペースに対応するビューを設定する. */
  public void setView(WorkspaceView view) {
    Objects.requireNonNull(view);
    this.view = view;
  }

  /**
   * このワークスペースに対するイベントハンドラの追加と削除を行うオブジェクトを返す.
   *
   * @return このワークスペースに対するイベントハンドラの追加と削除を行うオブジェクト
   */
  public CallbackRegistry getCallbackRegistry() {
    return cbRegistry;
  }

  /** {@link Workspace} に対してイベントハンドラを追加または削除する機能を提供するクラス. */
  public class CallbackRegistry {

    /** 関連するワークスペースのノードの選択状態が変更されたときのイベントハンドラをを管理するオブジェクト. */
    private final ConsumerInvoker<NodeSelectionEvent> onNodeSelStateChangedInvoker =
        new ConsumerInvoker<>();
    
    /** 関連するワークスペースのノードのコンパイルエラー状態が変更されたときのイベントハンドラをを管理するオブジェクト. */
    private final ConsumerInvoker<NodeCompileErrorEvent>
        onNodeCompileErrStateChangedInvoker = new ConsumerInvoker<>();

    /** 関連するワークスペースにノードが追加されたときのイベントハンドラをを管理するオブジェクト. */
    private final ConsumerInvoker<NodeAddedEvent> onNodeAddedInvoker =
        new ConsumerInvoker<>();

    /** 関連するワークスペースからノードが削除されたときのイベントハンドラをを管理するオブジェクト. */
    private final ConsumerInvoker<NodeRemovedEvent> onNodeRemovedInvoker = 
        new ConsumerInvoker<>();

    /** 関連するワークスペースのルートノード一式に新しくルートノードが追加されたときのイベントハンドラを管理するオブジェクト. */
    private final ConsumerInvoker<RootNodeAddedEvent> onRootNodeAddedInvoker =
        new ConsumerInvoker<>();

    /** 関連するワークスペースのルートノード一式からルートノードが削除されたときのイベントハンドラを管理するオブジェクト. */
    private final ConsumerInvoker<RootNodeRemovedEvent> onRootNodeRemovedInvoker = 
        new ConsumerInvoker<>();

    /** ノードがコネクタに接続されたときのイベントハンドラ. */
    private final Consumer<BhNode.ConnectionEvent> onNodeConnected =
        this::onNodeConnected;

    /** 関連するワークスペースのノードが選択されたときのイベントハンドラ. */
    private final Consumer<? super BhNode.SelectionEvent> onNodeSelStateChanged =
        this::onNodeSelectionStateChanged;

    /** 関連するワークスペースのコンパイルエラー状態が変更されたときのイベントハンドラ. */
    private final Consumer<? super BhNode.CompileErrorEvent> onNodeCompileErrStateChanged =
        this::onNodeCompileErrStateChanged;

    /** 関連するワークスペースのノードの選択状態が変更されたときのイベントハンドラのレジストリを取得する. */
    public ConsumerInvoker<NodeSelectionEvent>.Registry getOnNodeSelectionStateChanged() {
      return onNodeSelStateChangedInvoker.getRegistry();
    }

    /** 関連するワークスペースのノードのコンパイルエラー状態が変更されたときのイベントハンドラのレジストリを取得する. */
    public ConsumerInvoker<NodeCompileErrorEvent>.Registry getOnNodeCompileErrorStateChanged() {
      return onNodeCompileErrStateChangedInvoker.getRegistry();
    }

    /** 関連するワークスペースにノードが追加されたときのイベントハンドラのレジストリを取得する. */
    public ConsumerInvoker<NodeAddedEvent>.Registry getOnNodeAdded() {
      return onNodeAddedInvoker.getRegistry();
    }

    /** 関連するワークスペースからノードが削除されたときのイベントハンドラのレジストリを取得する. */
    public ConsumerInvoker<NodeRemovedEvent>.Registry getOnNodeRemoved() {
      return onNodeRemovedInvoker.getRegistry();
    }

    /**
     * 関連するワークスペースのルートノード一式に新しくルートノードが追加されたときのイベントハンドラを
     * 登録 / 削除するためのオブジェクトを取得する.
     */
    public ConsumerInvoker<RootNodeAddedEvent>.Registry getOnRootNodeAdded() {
      return onRootNodeAddedInvoker.getRegistry();
    }

    /**
     * 関連するワークスペースのルートノード一式からルートノードが削除されたときのイベントハンドラを
     * 登録 / 削除するためのオブジェクトを取得する.
     */
    public ConsumerInvoker<RootNodeRemovedEvent>.Registry getOnRootNodeRemoved() {
      return onRootNodeRemovedInvoker.getRegistry();
    }

    /** 関連するワークスペースのノードが他のノードと入れ替わったときのイベントハンドラを呼び出す. */
    private void onNodeConnected(BhNode.ConnectionEvent event) {
      if (event.disconnected() != null
          && event.disconnected().isRoot()
          && event.disconnected().getWorkspace() == Workspace.this) {
        rootNodes.add(event.disconnected());
        cbRegistry.onRootNodeAddedInvoker.invoke(
            new RootNodeAddedEvent(Workspace.this, event.disconnected(), event.userOpe()));
      }
      if (rootNodes.contains(event.connected())) {
        rootNodes.remove(event.connected());
        cbRegistry.onRootNodeRemovedInvoker.invoke(
            new RootNodeRemovedEvent(Workspace.this, event.connected(), event.userOpe()));
      }
    }

    /** ノードの選択状態が変わったときのイベントハンドラを呼び出す. */
    private void onNodeSelectionStateChanged(BhNode.SelectionEvent event) {
      if (event.isSelected()) {
        addToSelectedNodeList(event.node());
      } else {
        removeFromSelectedNodeList(event.node());
      }
      onNodeSelStateChangedInvoker.invoke(new NodeSelectionEvent(
          Workspace.this, event.node(), event.isSelected(), event.userOpe()));
    }

    /** ノードのコンパイルエラー状態が変わったときのイベントハンドラを呼び出す. */
    private void onNodeCompileErrStateChanged(BhNode.CompileErrorEvent event) {
      onNodeCompileErrStateChangedInvoker.invoke(new NodeCompileErrorEvent(
          Workspace.this, event.node(), event.hasError(), event.userOpe()));
    }
  }

  /**
   * ワークスペースのノードの選択状態が変更されたときの情報を格納したレコード.
   *
   * @param ws {@code node} を保持するワークスペース
   * @param node 選択状態が変更されたノード
   * @param isSelected {@code node} が選択された場合 true
   * @param userOpe undo 用コマンドオブジェクト
   */
  public record NodeSelectionEvent(
      Workspace ws, BhNode node, boolean isSelected, UserOperation userOpe) {}

  /**
   * ワークスペースのノードのコンパイルエラー状態が変更されたときの情報を格納したレコード.
   *
   * @param ws {@code node} を保持するワークスペース
   * @param node コンパイルエラー状態が変更されたノード
   * @param hasError {@code node} がコンパイルエラーを起こした場合 true
   * @param userOpe undo 用コマンドオブジェクト
   */      
  public record NodeCompileErrorEvent(
      Workspace ws, BhNode node, boolean hasError, UserOperation userOpe) {}

  /**
   * ワークスペースにノードが追加されたときの情報を格納したレコード.
   *
   * @param ws {@code node} が追加されたワークスペース
   * @param node {@code ws} に追加されたノード
   * @param userOpe undo 用コマンドオブジェクト
   */
  public record NodeAddedEvent(Workspace ws, BhNode node, UserOperation userOpe) {}

  /**
   * ワークスペースからノードが削除されたときの情報を格納したレコード.
   *
   * @param ws {@code node} が削除されたワークスペース
   * @param node {@code ws} から削除されたノード
   * @param userOpe undo 用コマンドオブジェクト
   */
  public record NodeRemovedEvent(Workspace ws, BhNode node, UserOperation userOpe) {}

  /**
   * ワークスペースのルートノード一式に新しくルートノードが追加されたときの情報を格納したレコード.
   *
   * @param ws {@code node} をルートノードとして保持するワークスペース
   * @param node {@code ws} 上でルートノードとなったノード
   * @param userOpe undo 用コマンドオブジェクト
   */
  public record RootNodeAddedEvent(Workspace ws, BhNode node, UserOperation userOpe) {}

  /**
   * ワークスペースのルートノード一式からルートノードが削除されたときの情報を格納したレコード.
   *
   * @param ws ワークスペース上の {@code node} が非ルートノードとなった
   * @param node 非ルートノードとなったノード
   * @param userOpe undo 用コマンドオブジェクト
   */  
  public record RootNodeRemovedEvent(Workspace ws, BhNode node, UserOperation userOpe) {}
}
