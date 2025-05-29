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
import java.util.function.BiConsumer;
import net.seapanda.bunnyhop.model.BhNodePlacer;
import net.seapanda.bunnyhop.model.node.BhNode;
import net.seapanda.bunnyhop.model.node.BhNode.Swapped;
import net.seapanda.bunnyhop.model.node.event.CauseOfDeletion;
import net.seapanda.bunnyhop.undo.UserOperation;
import net.seapanda.bunnyhop.utility.function.TetraConsumer;
import net.seapanda.bunnyhop.utility.function.TriConsumer;

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
  private EventManager eventManager = new EventManager();

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
    workspace.getEventManager().addOnNodeSelectionStateChanged(
        eventManager.onNodeSelectionStateChanged);
    workspace.getEventManager().addOnNodeCompileErrStateChanged(
        eventManager.onNodeCompileErrStateChanged);
    workspace.getEventManager().addOnNodeAdded(eventManager.onNodeAdded);
    workspace.getEventManager().addOnNodeRemoved(eventManager.onNodeRemoved);
    workspace.getEventManager().addOnNodeTurnedIntoRoot(eventManager.onNodeTurnedIntoRoot);
    workspace.getEventManager().addOnNodeTurnedIntoNotRoot(eventManager.onNodeTurnedIntoNotRoot);
    eventManager.invokeOnWorkspaceAdded(workspace, userOpe);
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
    workspace.getEventManager().removeOnNodeSelectionStateChanged(
        eventManager.onNodeSelectionStateChanged);
    workspace.getEventManager().removeOnNodeCompileErrStateChanged(
        eventManager.onNodeCompileErrStateChanged);
    workspace.getEventManager().removeOnNodeAdded(eventManager.onNodeAdded);
    workspace.getEventManager().removeOnNodeRemoved(eventManager.onNodeRemoved);
    workspace.getEventManager().removeOnNodeTurnedIntoRoot(eventManager.onNodeTurnedIntoRoot);
    workspace.getEventManager().removeOnNodeTurnedIntoNotRoot(eventManager.onNodeTurnedIntoNotRoot);
    eventManager.invokeOnWorkspaceRemoved(workspace, userOpe);
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
   * 現在操作対象のワークスペースを設定する.
   * 
   * <pre>
   * この操作は undo の対象にしない.
   * 理由は, この操作はワークスペースのタブ切り替え時は単一の undo 操作となるが,
   * ワークスペースの追加 (redo によるものも含む) 時には, これに付随する undo 操作となるので
   * 本メソッドに渡すべき {@link UserOperation} オブジェクトの選択が複雑になる.
   * </pre>
   *
   * @param ws 現在操作対象のワークスペース
   */
  public void setCurrentWorkspace(Workspace ws) {
    Workspace old = currentWorkspace;
    currentWorkspace = ws;
    eventManager.invokeOnCurrentWorkspaceChanged(old, currentWorkspace);
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
  public EventManager getEventManager() {
    return eventManager;
  }

  /** イベントハンドラの管理を行うクラス. */
  public class EventManager {

    /** ワークスペースが追加されたときのイベントハンドラのセット. */
    private final
        SequencedSet<TriConsumer<? super WorkspaceSet, ? super Workspace, ? super UserOperation>>
        onWorkspaceAddedList = new LinkedHashSet<>();
    /** ワークスペースが削除されたときのイベントハンドラのセット. */
    private final
        SequencedSet<TriConsumer<? super WorkspaceSet, ? super Workspace, ? super UserOperation>>
        onWorkspaceRemovedList = new LinkedHashSet<>();

    /**
     * <pre>
     * ノードが選択されたときに呼び出すイベントハンドラのセット.
     * イベントハンドラの第1引数 : 選択状態に変化のあったノード
     * イベントハンドラの第2引数 : ノードの選択状態
     * </pre>
     */
    private final SequencedSet<TriConsumer<? super BhNode, ? super Boolean, ? super UserOperation>>
        onNodeSelectionStateChangedList = new LinkedHashSet<>();

    /**
     * <pre>
     * ノードのコンパイルエラー状態が変更されたときに呼び出すイベントハンドラのセット.
     * イベントハンドラの第1引数 : コンパイルエラー状態が変更されたノード
     * イベントハンドラの第2引数 : コンパイルエラー状態
     * </pre>
     */    
    private final SequencedSet<TriConsumer<? super BhNode, ? super Boolean, ? super UserOperation>>
        onNodeCompileErrStateChangedList = new LinkedHashSet<>();        
    /**
     * <pre>
     * 操作対象のワークスペースが切り替わった時に呼び出すイベントハンドラのセット.
     * 第1引数 : 前の操作対象のワークスペース
     * 第2引数 : 新しい操作対象のワークスペース
     * </pre>
     */
    private final SequencedSet<BiConsumer<? super Workspace, ? super Workspace>>
        onCurrentWorkspaceChangedList = new LinkedHashSet<>();
    /** このワークスペースセットのワークスペースにノードが追加されたときのイベントハンドラのセット. */
    private final SequencedSet<TetraConsumer<
        ? super WorkspaceSet, ? super Workspace, ? super BhNode, ? super UserOperation>>
        onNodeAddedList = new LinkedHashSet<>();
    /** このワークスペースセットのワークスペースからノードが削除されたときのイベントハンドラのセット. */
    private final SequencedSet<TetraConsumer<
        ? super WorkspaceSet, ? super Workspace, ? super BhNode, ? super UserOperation>>
        onNodeRemovedList = new LinkedHashSet<>();
    /** このワークスペースセットのワークスペースのノードがルートノードとなったときのイベントハンドラのセット. */
    private final SequencedSet<TetraConsumer<
        ? super WorkspaceSet, ? super Workspace, ? super BhNode, ? super UserOperation>>
        onNodeTurnedIntoRootList = new LinkedHashSet<>();
    /** このワークスペースセットのワークスペースのノードが非ルートノードとなったときのイベントハンドラのセット. */
    private final SequencedSet<TetraConsumer<
        ? super WorkspaceSet, ? super Workspace, ? super BhNode, ? super UserOperation>>
        onNodeTurnedIntoNotRootList = new LinkedHashSet<>();

    /** ノードの選択状態が変わったときのイベントハンドラを呼ぶ関数オブジェクト. */
    private final TriConsumer<? super BhNode, ? super Boolean, ? super UserOperation>
        onNodeSelectionStateChanged = this::invokeOnNodeSelectionStateChanged;
    /** ノードのコンパイルエラー状態が変わったときのイベントハンドラを呼ぶ関数オブジェクト. */
    private final TriConsumer<? super BhNode, ? super Boolean, ? super UserOperation>
        onNodeCompileErrStateChanged = this::invokeOnNodeCompileErrStateChanged;
    /** このワークスペースセットのワークスペースにノードが追加されたときのイベントハンドラを呼ぶ関数オブジェクト. */
    private final TriConsumer<? super Workspace, ? super BhNode, ? super UserOperation>
        onNodeAdded = this::invokeOnNodeAdded;
    /** このワークスペースセットのワークスペースからノードが削除されたときのイベントハンドラを呼ぶ関数オブジェクト. */
    private final TriConsumer<? super Workspace, ? super BhNode, ? super UserOperation>
        onNodeRemoved = this::invokeOnNodeRemoved;
    /**このワークスペースセットのワークスペースの非ルートノードがルートノードとなったときのイベントハンドラを呼ぶ関数オブジェクト.*/
    private final TriConsumer<? super Workspace, ? super BhNode, ? super UserOperation>
        onNodeTurnedIntoRoot = this::invokeOnNodeTurnedIntoRoot;
    /**このワークスペースセットのワークスペースのルートノードが非ルートノードとなったときのイベントハンドラを呼ぶ関数オブジェクト.*/
    private final TriConsumer<? super Workspace, ? super BhNode, ? super UserOperation>
        onNodeTurnedIntoNotRoot = this::invokeOnNodeTurnedIntoNotRoot;

    /**
     * ワークスペースが追加されたときのイベントハンドラを追加する.
     *
     * @param handler 追加するイベントハンドラ
     */
    public void addOnWorkspaceAdded(
        TriConsumer<? super WorkspaceSet, ? super Workspace, ? super UserOperation> handler) {
      onWorkspaceAddedList.addLast(handler);
    }

    /**
     * ワークスペースが追加されたときのイベントハンドラを削除する.
     *
     * @param handler 削除するイベントハンドラ
     */
    public void removeOnWorkspaceAdded(
        TriConsumer<? super WorkspaceSet, ? super Workspace, ? super UserOperation> handler) {
      onWorkspaceAddedList.remove(handler);
    }

    /** このワークスペースセットにワークスペースが追加されたときのイベントハンドラを呼び出す. */
    private void invokeOnWorkspaceAdded(Workspace ws, UserOperation userOpe) {
      onWorkspaceAddedList.forEach(handler -> handler.accept(WorkspaceSet.this, ws, userOpe));
    }

    /**
     * ワークスペースが削除されたときのイベントハンドラを追加する.
     *
     * @param handler 追加するイベントハンドラ
     */
    public void addOnWorkspaceRemoved(
        TriConsumer<? super WorkspaceSet, ? super Workspace, ? super UserOperation> handler) {
      onWorkspaceRemovedList.addLast(handler);
    }

    /**
     * ワークスペースが削除されたときのイベントハンドラを削除する.
     *
     * @param handler 削除するイベントハンドラ
     */
    public void removeOnWorkspaceRemoved(
        TriConsumer<? super WorkspaceSet, ? super Workspace, ? super UserOperation> handler) {
      onWorkspaceRemovedList.remove(handler);
    }

    /** このワークスペースセットからワークスペースが削除されたときのイベントハンドラを呼び出す. */
    private void invokeOnWorkspaceRemoved(Workspace ws, UserOperation userOpe) {
      onWorkspaceRemovedList.forEach(handler -> handler.accept(WorkspaceSet.this, ws, userOpe));
    }

    /**
     * ノードの選択状態に変化があった時のイベントハンドラを追加する.
     *
     * @param handler 追加するイベントハンドラ
     *     <pre>
     *     handler 第1引数 : 選択状態に変化のあったノード
     *     handler 第2引数 : 選択状態.  選択されたとき true.
     *     </pre>
     */
    public void addOnNodeSelectionStateChanged(
        TriConsumer<? super BhNode, ? super Boolean, ? super UserOperation> handler) {
      onNodeSelectionStateChangedList.addLast(handler);
    }

    /**
     * ノードの選択状態に変化があった時のイベントハンドラを削除する.
     *
     * @param handler 削除するイベントハンドラ
     */
    public void removeOnNodeSelectionStateChanged(
        TriConsumer<? super BhNode, ? super Boolean, ? super UserOperation> handler) {
      onNodeSelectionStateChangedList.remove(handler);
    }

    /**
     * ノードの選択状態に変化があった時のイベントハンドラを呼ぶ.
     *
     * @param node 選択状態に変化があったノード
     * @param isSelected {@code node} の選択状態.  選択されているとき true.
     * @param userOpe undo 用コマンドオブジェクト
     */
    private void invokeOnNodeSelectionStateChanged(
        BhNode node, Boolean isSelected, UserOperation userOpe) {
      onNodeSelectionStateChangedList.forEach(
          handler -> handler.accept(node, isSelected, userOpe));
    }

    /**
     * 操作対象のワークスペースが変わった時のイベントハンドラを追加する.
     *
     * @param handler 追加するイベントハンドラ
     *     <pre>
     *     handler 第1引数 : 前の操作対象のワークスペース
     *     handler 第2引数 : 新しい操作対象のワークスペース
     *     handler 第3引数 : undo 用コマンドオブジェクト
     *     </pre>
     */
    public void addOnCurrentWorkspaceChanged(
        BiConsumer<? super Workspace, ? super Workspace> handler) {
      onCurrentWorkspaceChangedList.addLast(handler);
    }

    /**
     * 操作対象のワークスペースが変わった時のイベントハンドラを削除する.
     *
     * @param handler 削除するイベントハンドラ
     */
    public void removeOnCurrentWorkspaceChanged(
        BiConsumer<? super Workspace, ? super Workspace> handler) {
      onCurrentWorkspaceChangedList.remove(handler);
    }

    /**
     * 操作対象のワークスペースが変わった時のイベントハンドラを呼ぶ.
     *
     * @param oldWs 前の操作対象のワークスペース
     * @param newWs 新しい操作対象のワークスペース
     */
    private void invokeOnCurrentWorkspaceChanged(Workspace oldWs, Workspace newWs) {
      onCurrentWorkspaceChangedList.forEach(handler -> handler.accept(oldWs, newWs));
    }

    /**
     * このワークスペースセットのワークスペースにノードが追加されたときのイベントハンドラを追加する.
     *
     * @param handler 追加するイベントハンドラ
     */
    public void addOnNodeAdded(
        TetraConsumer<
          ? super WorkspaceSet, ? super Workspace, ? super BhNode, ? super UserOperation> handler) {
      onNodeAddedList.addLast(handler);
    }

    /**
     * このワークスペースセットのワークスペースにノードが追加されたときのイベントハンドラを削除する.
     *
     * @param handler 削除するイベントハンドラ
     */
    public void removeOnNodeAdded(
        TetraConsumer<
          ? super WorkspaceSet, ? super Workspace, ? super BhNode, ? super UserOperation> handler) {
      onNodeAddedList.remove(handler);
    }

    /** このワークスペースセットのワークスペースにノードが追加されたときのイベントハンドラを呼ぶ. */
    private void invokeOnNodeAdded(Workspace ws, BhNode node, UserOperation userOpe) {
      if (node.getCompileErrState()) {
        WorkspaceSet.this.compileErrNodes.addLast(node);
      }
      onNodeAddedList.forEach(handler -> handler.accept(WorkspaceSet.this, ws, node, userOpe));
    }

    /**
     * このワークスペースセットのワークスペースからノードが削除されたときのイベントハンドラを追加する.
     *
     * @param handler 追加するイベントハンドラ
     */
    public void addOnNodeRemoved(
        TetraConsumer<
          ? super WorkspaceSet, ? super Workspace, ? super BhNode, ? super UserOperation> handler) {
      onNodeRemovedList.addLast(handler);
    }

    /**
     * このワークスペースセットのワークスペースからノードが削除されたときのイベントハンドラを削除する.
     *
     * @param handler 削除するイベントハンドラ
     */
    public void removeOnNodeRemoved(
        TetraConsumer<
          ? super WorkspaceSet, ? super Workspace, ? super BhNode, ? super UserOperation> handler) {
      onNodeRemovedList.remove(handler);
    }

    /** このワークスペースセットのワークスペースからノードが削除されたときのイベントハンドラを呼ぶ. */
    private void invokeOnNodeRemoved(Workspace ws, BhNode node, UserOperation userOpe) {
      WorkspaceSet.this.compileErrNodes.remove(node);
      onNodeRemovedList.forEach(handler -> handler.accept(WorkspaceSet.this, ws, node, userOpe));
    }

    /**
     * このワークスペースセットのワークスペースの非ルートノードがルートノードとなったときのイベントハンドラを追加する.
     *
     * @param handler 追加するイベントハンドラ
     */
    public void addOnNodeTurnedIntoRoot(
        TetraConsumer<
          ? super WorkspaceSet, ? super Workspace, ? super BhNode, ? super UserOperation> handler) {
      onNodeTurnedIntoRootList.addLast(handler);
    }

    /**
     * このワークスペースセットのワークスペースの非ルートノードがルートノードとなったときのイベントハンドラを削除する.
     *
     * @param handler 削除するイベントハンドラ
     */
    public void removeOnNodeTurnedIntoRoot(
        TetraConsumer<
          ? super WorkspaceSet, ? super Workspace, ? super BhNode, ? super UserOperation> handler) {
      onNodeTurnedIntoRootList.remove(handler);
    }

    /** このワークスペースセットのワークスペースの非ルートノードがルートノードとなったときのイベントハンドラを呼ぶ.*/
    private void invokeOnNodeTurnedIntoRoot(Workspace ws, BhNode node, UserOperation userOpe) {
      onNodeTurnedIntoRootList.forEach(
          handler -> handler.accept(WorkspaceSet.this, ws, node, userOpe));
    }

    /**
     * このワークスペースセットのワークスペースのルートノードが非ルートノードとなったときのイベントハンドラを追加する.
     *
     * @param handler 追加するイベントハンドラ
     */
    public void addOnNodeTurnedIntoNotRoot(
        TetraConsumer<
          ? super WorkspaceSet, ? super Workspace, ? super BhNode, ? super UserOperation> handler) {
      onNodeTurnedIntoNotRootList.addLast(handler);
    }

    /**
     * このワークスペースセットのワークスペースのルートノードが非ルートノードとなったときのイベントハンドラを削除する.
     *
     * @param handler 削除するイベントハンドラ
     */
    public void removeOnNodeTurnedIntoNotRoot(
        TetraConsumer<
          ? super WorkspaceSet, ? super Workspace, ? super BhNode, ? super UserOperation> handler) {
      onNodeTurnedIntoNotRootList.remove(handler);
    }

    /** このワークスペースセットのワークスペースのルートノードが非ルートノードとなったときのイベントハンドラを呼ぶ.*/
    private void invokeOnNodeTurnedIntoNotRoot(Workspace ws, BhNode node, UserOperation userOpe) {
      onNodeTurnedIntoNotRootList.forEach(
          handler -> handler.accept(WorkspaceSet.this, ws, node, userOpe));
    }

    /**
     * ノードのコンパイルエラー状態に変化があった時のイベントハンドラを追加する.
     *
     * @param handler 追加するイベントハンドラ
     *     <pre>
     *     handler 第1引数 : コンパイルエラー状態に変化のあったノード
     *     handler 第2引数 : コンパイルエラー状態.
     *     </pre>
     */
    public void addOnCompileErrStateChanged(
        TriConsumer<? super BhNode, ? super Boolean, ? super UserOperation> handler) {
      onNodeCompileErrStateChangedList.addLast(handler);
    }

    /**
     * ノードのコンパイルエラー状態に変化があった時のイベントハンドラを削除する.
     *
     * @param handler 削除するイベントハンドラ
     */
    public void removeCompileErrStateChanged(
        TriConsumer<? super BhNode, ? super Boolean, ? super UserOperation> handler) {
      onNodeCompileErrStateChangedList.remove(handler);
    }

    /**
     * ノードのコンパイルエラー状態に変化があった時のイベントハンドラを呼ぶ.
     *
     * @param node コンパイルエラー状態に変化があったノード
     * @param hasCompileError {@link node} のコンパイルエラー状態.
     * @param userOpe undo 用コマンドオブジェクト
     */
    private void invokeOnNodeCompileErrStateChanged(
        BhNode node, Boolean hasCompileError, UserOperation userOpe) {
      if (hasCompileError) {
        WorkspaceSet.this.compileErrNodes.addLast(node);
      } else {
        WorkspaceSet.this.compileErrNodes.remove(node);
      } 
      onNodeCompileErrStateChangedList.forEach(
          handler -> handler.accept(node, hasCompileError, userOpe));
    }
  }
}
