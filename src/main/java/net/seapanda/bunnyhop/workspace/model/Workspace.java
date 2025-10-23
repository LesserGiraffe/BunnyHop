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

import java.io.Serializable;
import java.util.LinkedHashSet;
import java.util.Objects;
import java.util.Optional;
import java.util.SequencedSet;
import java.util.function.Consumer;
import net.seapanda.bunnyhop.node.model.BhNode;
import net.seapanda.bunnyhop.node.model.traverse.CallbackInvoker;
import net.seapanda.bunnyhop.service.undo.UserOperation;
import net.seapanda.bunnyhop.utility.event.ConsumerInvoker;
import net.seapanda.bunnyhop.utility.event.SimpleConsumerInvoker;
import net.seapanda.bunnyhop.workspace.view.WorkspaceView;

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
  private final transient CallbackRegistry cbRegistry = new CallbackRegistry();

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
    boolean shouldAddRoot = !rootNodes.contains(root) && root.isRoot();
    if (shouldAddRoot) {
      rootNodes.add(root);
    }
    userOpe.pushCmd(ope -> removeNodeTree(root, ope));
    invokeOnNodeAdded(root, shouldAddRoot, userOpe);
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
    var cbRegistry = getCallbackRegistry();
    registry.getOnSelectionStateChanged().add(cbRegistry.onNodeSelStateChanged);
    registry.getOnCompileErrorStateChanged().add(cbRegistry.onNodeCompileErrStateChanged);
    registry.getOnBreakpointSet().add(cbRegistry.onNodeBreakpointSet);
    registry.getOnConnected().add(cbRegistry.onNodeConnected);
  }

  /** ノードツリーの追加に伴うコールバック関数を呼ぶ. */
  private void invokeOnNodeAdded(BhNode root, boolean invokeOnRootAdded, UserOperation userOpe) {
    var cbRegistry = getCallbackRegistry();
    CallbackInvoker.CallbackRegistry callbacks = CallbackInvoker.newCallbackRegistry();
    callbacks.setForAllNodes(
        node -> cbRegistry.onNodeAddedInvoker.invoke(new NodeAddedEvent(this, node, userOpe)));
    CallbackInvoker.invoke(callbacks, root);

    if (invokeOnRootAdded) {
      cbRegistry.onRootNodeAddedInvoker.invoke(new RootNodeAddedEvent(this, root, userOpe));
    }
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
    boolean shouldRemoveRoot = rootNodes.contains(root);
    if (shouldRemoveRoot) {
      rootNodes.remove(root);
    }
    CallbackInvoker.CallbackRegistry callbacks = CallbackInvoker.newCallbackRegistry();
    callbacks.setForAllNodes(bhNode -> removeNode(bhNode, userOpe));
    CallbackInvoker.invoke(callbacks, root);
    userOpe.pushCmd(ope -> addNodeTree(root, ope));
    invokeOnNodeRemoved(root, shouldRemoveRoot, userOpe);
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
    var cbRegistry = getCallbackRegistry();
    registry.getOnSelectionStateChanged().remove(cbRegistry.onNodeSelStateChanged);
    registry.getOnCompileErrorStateChanged().remove(cbRegistry.onNodeCompileErrStateChanged);
    registry.getOnBreakpointSet().remove(cbRegistry.onNodeBreakpointSet);
    registry.getOnConnected().remove(cbRegistry.onNodeConnected);
    node.setWorkspace(null, userOpe);
    nodeList.remove(node);
  }

  /** ノードツリーの削除に伴うコールバック関数を呼ぶ. */
  private void invokeOnNodeRemoved(
      BhNode root, boolean invokeOnRootRemoved, UserOperation userOpe) {
    var cbRegistry = getCallbackRegistry();
    if (invokeOnRootRemoved) {
      cbRegistry.onRootNodeRemovedInvoker.invoke(new RootNodeRemovedEvent(this, root, userOpe));
    }

    CallbackInvoker.CallbackRegistry callbacks = CallbackInvoker.newCallbackRegistry();
    callbacks.setForAllNodes(
        node -> cbRegistry.onNodeRemovedInvoker.invoke(new NodeRemovedEvent(this, node, userOpe)));
    CallbackInvoker.invoke(callbacks, root);
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
   */
  private void addToSelectedNodeList(BhNode toAdd) {
    if (selectedList.contains(toAdd)) {
      return;
    }
    selectedList.add(toAdd);
  }

  /**
   * {@code toRemove} を選択済みノードリストから削除する.
   *
   * @param toRemove 選択済みリストから削除する {@link BhNode}
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
   * @param userOpe undo 用コマンドオブジェクト
   */
  public void setName(String name, UserOperation userOpe) {
    name = (name == null) ? "" : name;
    var oldName = this.name;
    this.name = name;
    userOpe.pushCmd(ope -> setName(oldName, ope));
    getCallbackRegistry().onNameChangedInvoker.invoke(new NameChangedEvent(this, oldName, name));
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
    // シリアライズしたノードを操作したときに null が返るのを防ぐ.
    if (cbRegistry == null) {
      return new CallbackRegistry();
    }
    return cbRegistry;
  }

  /** {@link Workspace} に対してイベントハンドラを追加または削除する機能を提供するクラス. */
  public class CallbackRegistry {

    /** 関連するワークスペースのノードの選択状態が変更されたときのイベントハンドラを管理するオブジェクト. */
    private final ConsumerInvoker<NodeSelectionEvent> onNodeSelStateChangedInvoker =
        new SimpleConsumerInvoker<>();
    
    /** 関連するワークスペースのノードのコンパイルエラー状態が変更されたときのイベントハンドラを管理するオブジェクト. */
    private final ConsumerInvoker<NodeCompileErrorEvent> onNodeCompileErrStateChangedInvoker =
        new SimpleConsumerInvoker<>();

    /** 関連するワークスペースのノードのブレークポイントの設定が変更されたときのイベントハンドラを管理するオブジェクト. */
    private final ConsumerInvoker<NodeBreakpointSetEvent> onNodeBreakpointSetInvoker =
        new SimpleConsumerInvoker<>();

    /** 関連するワークスペースにノードが追加されたときのイベントハンドラを管理するオブジェクト. */
    private final ConsumerInvoker<NodeAddedEvent> onNodeAddedInvoker =
        new SimpleConsumerInvoker<>();

    /** 関連するワークスペースからノードが削除されたときのイベントハンドラを管理するオブジェクト. */
    private final ConsumerInvoker<NodeRemovedEvent> onNodeRemovedInvoker = 
        new SimpleConsumerInvoker<>();

    /** 関連するワークスペースのルートノード一式に新しくルートノードが追加されたときのイベントハンドラを管理するオブジェクト. */
    private final ConsumerInvoker<RootNodeAddedEvent> onRootNodeAddedInvoker =
        new SimpleConsumerInvoker<>();

    /** 関連するワークスペースのルートノード一式からルートノードが削除されたときのイベントハンドラを管理するオブジェクト. */
    private final ConsumerInvoker<RootNodeRemovedEvent> onRootNodeRemovedInvoker = 
        new SimpleConsumerInvoker<>();

    /** 関連するワークスペースの名前が変わったときのイベントハンドラを管理するオブジェクト. */
    private final ConsumerInvoker<NameChangedEvent> onNameChangedInvoker =
        new SimpleConsumerInvoker<>();

    /** ノードがコネクタに接続されたときのイベントハンドラ. */
    private final Consumer<BhNode.ConnectionEvent> onNodeConnected =
        this::onNodeConnected;

    /** 関連するワークスペースのノードが選択されたときのイベントハンドラ. */
    private final Consumer<? super BhNode.SelectionEvent> onNodeSelStateChanged =
        this::onNodeSelectionStateChanged;

    /** 関連するワークスペースのノードのコンパイルエラー状態が変更されたときのイベントハンドラ. */
    private final Consumer<? super BhNode.CompileErrorEvent> onNodeCompileErrStateChanged =
        this::onNodeCompileErrStateChanged;

    /** 関連するワークスペースのノードのブレークポイントの設定が変更されたときのイベントハンドラ. */
    private final Consumer<? super BhNode.BreakpointSetEvent> onNodeBreakpointSet =
        this::onNodeBreakpointSet;

    /** 関連するワークスペースのノードの選択状態が変更されたときのイベントハンドラのレジストリを取得する. */
    public ConsumerInvoker<NodeSelectionEvent>.Registry getOnNodeSelectionStateChanged() {
      return onNodeSelStateChangedInvoker.getRegistry();
    }

    /** 関連するワークスペースのノードのコンパイルエラー状態が変更されたときのイベントハンドラのレジストリを取得する. */
    public ConsumerInvoker<NodeCompileErrorEvent>.Registry getOnNodeCompileErrorStateChanged() {
      return onNodeCompileErrStateChangedInvoker.getRegistry();
    }

    /** 関連するワークスペースのノードのブレークポイントの設定が変更されたときのイベントハンドラのレジストリを取得する. */
    public ConsumerInvoker<NodeBreakpointSetEvent>.Registry getOnNodeBreakpointSet() {
      return onNodeBreakpointSetInvoker.getRegistry();
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

    /** 関連するワークスペースの名前が変わったときのイベントハンドラを登録 / 削除するためのオブジェクトを取得する. */
    public ConsumerInvoker<NameChangedEvent>.Registry getOnNameChanged() {
      return onNameChangedInvoker.getRegistry();
    }

    /** 関連するワークスペースのノードが他のノードと入れ替わったときのイベントハンドラを呼び出す. */
    private void onNodeConnected(BhNode.ConnectionEvent event) {
      if (event.disconnected() != null
          && event.disconnected().isRoot()
          && event.disconnected().getWorkspace() == Workspace.this) {
        rootNodes.add(event.disconnected());
        onRootNodeAddedInvoker.invoke(
            new RootNodeAddedEvent(Workspace.this, event.disconnected(), event.userOpe()));
      }
      if (rootNodes.contains(event.connected())) {
        rootNodes.remove(event.connected());
        onRootNodeRemovedInvoker.invoke(
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

    /** ノードのブレークポイントの設定が変わったときのイベントハンドラを呼び出す. */
    private void onNodeBreakpointSet(BhNode.BreakpointSetEvent event) {
      onNodeBreakpointSetInvoker.invoke(new  NodeBreakpointSetEvent(
          Workspace.this, event.node(), event.isBreakpointSet(), event.userOpe()));
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
   * ワークスペースのノードのブレークポイントの設定が変更されたときの情報を格納したレコード.
   *
   * @param ws {@code node} を保持するワークスペース
   * @param node ブレークポイントの状態が変更されたノード
   * @param isBreakpointSet ブレークポイントが設定された場合 true, 設定が解除された場合 false
   * @param userOpe undo 用コマンドオブジェクト
   */
  public record NodeBreakpointSetEvent(
      Workspace ws, BhNode node, boolean isBreakpointSet, UserOperation userOpe) {}

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
   * @param ws このワークスペースの {@code node} が非ルートノードとなった
   * @param node 非ルートノードとなったノード
   * @param userOpe undo 用コマンドオブジェクト
   */  
  public record RootNodeRemovedEvent(Workspace ws, BhNode node, UserOperation userOpe) {}

  /**
   * ワークスペース名が変わったときの情報を格納したレコード.
   *
   * @param ws 名前が変わったワークスペース
   * @param oldName 変更前のワークスペース名
   * @param newName 変更後のワークスペース名
   */
  public record NameChangedEvent(Workspace ws, String oldName, String newName) {}
}
