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

package net.seapanda.bunnyhop.undo;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Deque;
import java.util.LinkedList;
import javafx.scene.Group;
import javafx.scene.Parent;
import javafx.scene.layout.Pane;
import net.seapanda.bunnyhop.model.node.BhNode;
import net.seapanda.bunnyhop.model.node.Connector;
import net.seapanda.bunnyhop.model.node.derivative.DerivativeBase;
import net.seapanda.bunnyhop.model.workspace.Workspace;
import net.seapanda.bunnyhop.model.workspace.WorkspaceSet;
import net.seapanda.bunnyhop.quadtree.QuadTreeManager;
import net.seapanda.bunnyhop.quadtree.QuadTreeRectangle;
import net.seapanda.bunnyhop.service.BhService;
import net.seapanda.bunnyhop.utility.Vec2D;
import net.seapanda.bunnyhop.view.node.BhNodeView;

/**
 * undo/redo 用コマンドクラス.
 *
 * @author K.Koike
 */
public class UserOperation {

  public UserOperation() {}

  /** このオブジェクトが表す操作を構成するサブ操作のリスト. */
  private Deque<SubOperation> subOpeList = new LinkedList<>();

  /**
   * このコマンドの逆の操作を行う (例えば, ノード追加ならノード削除を行う).
   *
   * @return このコマンドの逆の操作を表す UserOperationCommand オブジェクトを返す. <br>
   *          つまり, 戻りオブジェクトの doInverseOperation はこのコマンドの元になった操作を行う
   */
  UserOperation doInverseOperation() {
    UserOperation inverseCmd = new UserOperation();
    while (!subOpeList.isEmpty()) {
      subOpeList.removeLast().doInverseOperation(inverseCmd);
    }
    return inverseCmd;
  }

  /**
   * サブ操作の数を返す,.
   *
   * @return サブ操作の数
   */
  public int getNumSubOps() {
    return subOpeList.size();
  }

  /** for debug. */
  public void printSubOpeList() {
    for (SubOperation subope : subOpeList) {
      BhService.msgPrinter().println("subope  " + subope);
    }
  }

  /**
   * 派生ノードリストへの追加をコマンド化してサブ操作リストに加える.
   *
   * @param derivative 追加した派生ノード
   * @param original {@code derivative} のオリジナルノード
   */
  public <T extends DerivativeBase<T>> void pushCmdOfAddDerivative(T derivative, T original) {
    subOpeList.addLast(new AddDerivativeCmd<T>(derivative, original));
  }

  /**
   * 派生ノードリストからの削除をコマンド化してサブ操作リストに加える.
   *
   * @param derivative 削除した派生ノード
   * @param original {@code derivative} を保持していたオリジナルノード
   */
  public <T extends DerivativeBase<T>> void pushCmdOfRemoveDerivative(T derivative, T original) {
    subOpeList.addLast(new RemoveDerivativeCmd<T>(derivative, original));
  }

  /**
   * ワークスペース上での {@link BhNode} の位置指定をコマンド化してサブ操作リストに加える.
   *
   * @param node 位置を指定したノード
   * @param oldPos 移動前の位置
   */
  public void pushCmdOfSetNodePos(BhNode node, Vec2D oldPos) {
    subOpeList.addLast(new SetNodePos(node, oldPos));
  }

  /**
   * 4 分木ノードの 4 分木空間への登録をコマンド化してサブ操作リストに加える.
   *
   * @param rect 4 分木空間に登録した矩形オブジェクト
   * @param oldManager {@code node} が元々あった 4 分木空間を管理するオブジェクト
   * @param newManager {@code node} が新しく入った 4 分木空間を管理するオブジェクト
   */
  public void pushCmdOfSetQtRectangle(
      QuadTreeRectangle rect,
      QuadTreeManager oldManager,
      QuadTreeManager newManager) {
    subOpeList.addLast(new SetQtRectangleCmd(rect, oldManager, newManager));
  }

  /**
   * {@link BhNodeView} の入れ替えをコマンド化してサブ操作リストに加える.
   *
   * @param oldNode 入れ替え前の古いView に対応するBhNode
   * @param newNode 入れ替え後の新しいView に対応するBhNode
   * @param newNodeHasParent 入れ替え前の newNode が親GUIコンポーネントを持っていた場合 true
   */
  public void pushCmdOfReplaceNodeView(BhNode oldNode, BhNode newNode, boolean newNodeHasParent) {
    subOpeList.addLast(new ReplaceNodeViewCmd(oldNode, newNode, newNodeHasParent));
  }

  /**
   * {@link BhNode} の繋ぎ換えをコマンド化してサブ操作リストに加える.
   *
   * @param oldNode 繋ぎ替え前の {@link BhNode}
   * @param connector ノードのつなぎ替えを行うコネクタ
   */
  public void pushCmdOfConnectNode(BhNode oldNode, Connector connector) {
    subOpeList.addLast(new ConnectNodeCmd(oldNode, connector));
  }

  /**
   * 入れ替わりノードの登録をコマンド化してサブ操作リストに加える.
   *
   * @param oldNode 元々セットされていたノード
   * @param nodeRegisteredWith 入れ替わったノードを登録するノード
   */
  public void pushCmdOfSetLastReplaced(BhNode oldNode, BhNode nodeRegisteredWith) {
    subOpeList.addLast(new SetLastReplacedCmd(oldNode, nodeRegisteredWith));
  }

  /**
   * ノードツリーのワークスペースへの追加をコマンド化してサブ操作リストに加える.
   *
   * @param root ワークスペースに追加するノードツリーのルートノード
   * @param ws {@code root} 以下のノードを追加するワークスペース
   */
  public void pushCmdOfAddNodeTreeToWorkspace(BhNode root, Workspace ws) {
    subOpeList.addLast(new AddNodeTreeToWorkspaceCmd(root, ws));
  }

  /**
   * ノードツリーのワークスペースからの削除をコマンド化してサブ操作リストに加える.
   *
   * @param root ワークスペースから削除するノードツリーのルートノード
   * @param ws {@code root} 以下のノードを削除するワークスペース
   */
  public void pushCmdOfRemoveNodeTreeFromWorkspace(BhNode root, Workspace ws) {
    subOpeList.addLast(new RemoveNodeTreeFromWorkspaceCmd(root, ws));
  }

  /**
   * {@link BhNode} の選択をコマンド化してサブ操作リストに加える.
   *
   * @param node 選択されたノード
   */
  public void pushCmdOfSelectNode(BhNode node) {
    subOpeList.addLast(new SelectNodeCmd(node));
  }

  /**
   * {@link BhNode} の選択解除をコマンド化してサブ操作リストに加える.
   *
   * @param node 選択解除されたノード
   */
  public void pushCmdOfDeselectNode(BhNode node) {
    subOpeList.addLast(new DeselectNodeCmd(node));
  }

  /**
   * ワークスペースの追加をコマンド化してサブ操作リストに加える.
   *
   * @param ws 追加されたワークスペース
   */
  public void pushCmdOfAddWorkspace(Workspace ws) {
    subOpeList.addLast(new AddWorkspaceCmd(ws));
  }

  /**
   * ワークスペースの削除をコマンド化してサブ操作リストに加える.
   *
   * @param ws 削除されたワークスペース
   * @param wss {@code ws} が削除される前に所属していたワークスペースセット.
   */
  public void pushCmdOfRemoveWorkspace(Workspace ws, WorkspaceSet wss) {
    subOpeList.addLast(new RemoveWorkspaceCmd(ws, wss));
  }

  /**
   * ノードのコンパイルエラー設定をコマンド化してサブ操作リストに加える.
   *
   * @param nodeView ノードのコンパイルエラー設定を変更したノード
   * @param oldVal 変更前の状態
   */
  public void pushCmdOfSetCompileError(BhNodeView nodeView, boolean oldVal) {
    subOpeList.addLast(new SetCompileErrorCmd(nodeView, oldVal));
  }

  /**
   * コレクションへの要素の追加をコマンド化してサブ操作リストに加える.
   *
   * @param list 要素を追加したコレクション
   * @param addedElems 追加された要素のコレクション
   */
  public <T> void pushCmdOfAddToList(Collection<T> list, Collection<T> addedElems) {
    subOpeList.addLast(new AddToListCmd<T>(list, addedElems));
  }

  /**
   * コレクションへの要素の追加をコマンド化してサブ操作リストに加える.
   *
   * @param list 要素を追加したコレクション
   * @param addedElems 追加された要素
   */
  public <T> void pushCmdOfAddToList(Collection<T> list, T addedElems) {
    subOpeList.addLast(new AddToListCmd<T>(list, addedElems));
  }

  /**
   * コレクションからの要素の削除をコマンド化してサブ操作リストに加える.
   *
   * @param list 要素を削除したコレクション
   * @param removedElems 削除された要素のコレクション
   */
  public <T> void pushCmdOfRemoveFromList(Collection<T> list, Collection<T> removedElems) {
    subOpeList.addLast(new RemoveFromListCmd<T>(list, removedElems));
  }

  /**
   * コレクションからの要素の削除をコマンド化してサブ操作リストに加える.
   *
   * @param list 要素を削除したコレクション
   * @param removedElem 削除された要素
   */
  public <T> void pushCmdOfRemoveFromList(Collection<T> list, T removedElem) {
    subOpeList.addLast(new RemoveFromListCmd<T>(list, removedElem));
  }

  /**
   * GUI ツリーへのノードビューの追加をコマンド化してサブ操作リストに加える.
   *
   * @param view GUIツリーに登録したノードビュー
   */
  public void pushCmdOfAddToGuiTree(BhNodeView view) {
    subOpeList.addLast(new AddToGuiTreeCmd(view));
  }

  /**
   * GUI ツリーへのノードビューの削除をコマンド化してサブ操作リストに加える.
   *
   * @param view GUI ツリーに登録したノードビュー
   * @param parent view を削除したGUIコンポーネント
   */
  public void pushCmdOfRemoveFromGuiTree(BhNodeView view, Parent parent) {
    subOpeList.addLast(new RemoveFromGuiTreeCmd(view, parent));
  }

  /**
   * コピー予定のノードを追加する操作をコマンド化してサブ操作リストに加える.
   *
   * @param wss コピー予定のノードを追加したワークスペースセット.
   * @param added 追加されたコピー予定のノード
   */
  public void pushCmdOfAddNodeToCopyList(WorkspaceSet wss, BhNode added) {
    subOpeList.addLast(new AddNodeToCopyListCmd(wss, added));
  }

  /**
   * コピー予定のノードを削除する操作をコマンド化してサブ操作リストに加える.
   *
   * @param wss コピー予定のノードを削除したワークスペースセット.
   * @param removed 削除されたコピー予定のノード
   */
  public void pushCmdOfRemoveNodeFromCopyList(WorkspaceSet wss, BhNode removed) {
    subOpeList.addLast(new RemoveNodeFromCopyListCmd(wss, removed));
  }

  /**
   * カット予定のノードを追加する操作をコマンド化してサブ操作リストに加える.
   *
   * @param wss カット予定のノードを追加したワークスペースセット.
   * @param added 追加されたカット予定のノード
   */
  public void pushCmdOfAddNodeToCutList(WorkspaceSet wss, BhNode added) {
    subOpeList.addLast(new AddNodeToCutListCmd(wss, added));
  }

  /**
   * カット予定のノードを削除する操作をコマンド化してサブ操作リストに加える.
   *
   * @param wss カット予定のノードを削除したワークスペースセット.
   * @param removed 削除されたコピー予定のノード
   */
  public void pushCmdOfRemoveNodeFromCutList(WorkspaceSet wss, BhNode removed) {
    subOpeList.addLast(new RemoveNodeFromCutListCmd(wss, removed));
  }

  /**
   * ノードが属するワークスペースを変更する操作をコマンド化してサブ操作リストに加える.
   *
   * @param oldWs {@code node} が属していた変更前のワークスペース.
   * @param node ワークスペースが変更されたノード
   */
  public void pushCmdOfSetWorkspace(Workspace oldWs, BhNode node) {
    subOpeList.addLast(new SetWorkspaceCmd(oldWs, node));
  }

  /** {@link UserOperation} を構成するサブ操作. */
  interface SubOperation {
    /**
     * このSubOperation の逆の操作を行う.
     *
     * @param inverseCmd このサブ操作の逆の操作を作るための UserOperationCommand オブジェクト
     */
    public void doInverseOperation(UserOperation inverseCmd);
  }

  /** 派生ノードの追加を表すコマンド. */
  private static class AddDerivativeCmd<T extends DerivativeBase<T>> implements SubOperation {
    
    /** オリジナルノードの派生ノード一覧に追加された派生ノード. */
    private final T derivative;
    /** {@link #derivative} のオリジナルノード. */
    private final T original;

    public AddDerivativeCmd(T derivative, T original) {
      this.derivative = derivative;
      this.original = original;
    }

    @Override
    public void doInverseOperation(UserOperation inverseCmd) {
      original.removeDerivative(derivative, inverseCmd);
    }
  }

  /** 派生ノードの削除を表すコマンド. */
  private static class RemoveDerivativeCmd<T extends DerivativeBase<T>> implements SubOperation {

    /** オリジナルノードの派生ノード一覧から削除された派生ノード. */
    private final T derivative;
    /** {@link #derivative} のオリジナルノード. */
    private final T original;

    public RemoveDerivativeCmd(T derivative, T original) {
      this.derivative = derivative;
      this.original = original;
    }

    @Override
    public void doInverseOperation(UserOperation inverseCmd) {
      original.addDerivative(derivative, inverseCmd);
    }
  }

  /** ワークスペース上での {@link BhNode} の位置指定を表すコマンド. */
  private static class SetNodePos implements SubOperation {

    /** 位置を指定したノード. */
    private final BhNode node;
    /** 移動前の位置. */
    private final Vec2D oldPos;

    public SetNodePos(BhNode node, Vec2D oldPos) {
      this.node = node;
      this.oldPos = oldPos;
    }

    @Override
    public void doInverseOperation(UserOperation inverseCmd) {
      node.getViewProxy().setPosOnWorkspace(oldPos, inverseCmd);
    }
  }
  
  /** 4 分木空間への 4 分木ノードの登録を表すコマンド. */
  private static class SetQtRectangleCmd implements SubOperation {

    /** 4 分木空間に登録した矩形オブジェクト. */
    private final QuadTreeRectangle rect;
    /** {@code rect} が元々あった 4 分木空間を管理するオブジェクト. */
    private final QuadTreeManager oldManager;
    /** {@code rect} が新しく入った 4 分木空間を管理するオブジェクト. */
    private final QuadTreeManager newManager;

    public SetQtRectangleCmd(
        QuadTreeRectangle rect, QuadTreeManager oldManager, QuadTreeManager newManager) {
      this.rect = rect;
      this.oldManager = oldManager;
      this.newManager = newManager;
    }

    @Override
    public void doInverseOperation(UserOperation inverseCmd) {
      if (oldManager != null) {
        addQuadTree(inverseCmd);
      } else {
        QuadTreeManager.removeQuadTreeObj(rect);
      }
      inverseCmd.pushCmdOfSetQtRectangle(rect, newManager, oldManager);
    }

    private void addQuadTree(UserOperation inverseCmd) {
      oldManager.addQuadTreeObj(rect);
      // 4 分木空間での位置確定
      if (rect.getUserData() != null
          && rect.getUserData() instanceof BhNodeView view
          && view.getModel().isPresent()) {
        Vec2D pos = view.getPositionManager().getPosOnWorkspace();
        view.getPositionManager().setTreePosOnWorkspace(pos.x, pos.y);
      }
    }
  }

  /** BhNodeView の入れ替えを表すコマンド. */
  private static class ReplaceNodeViewCmd implements SubOperation {

    /** 入れ替え前の古い View に対応する {@link BhNode}. */
    private final BhNode oldNode;
    /** 入れ替え後の新しい View に対応する {@link BhNode}. */
    private final BhNode newNode;
    /** 入れ替え前に newNode が親 GUI コンポーネントを持っていた場合 true. */
    private final boolean newNodeHasParent;

    public ReplaceNodeViewCmd(BhNode oldNode, BhNode newNode, boolean newNodeHasParent) {
      this.oldNode = oldNode;
      this.newNode = newNode;
      this.newNodeHasParent = newNodeHasParent;
    }

    @Override
    public void doInverseOperation(UserOperation inverseCmd) {
      //元々付いていた古いViewに付け替える
      newNode.getViewProxy().replace(oldNode, inverseCmd);
      // 入れ替え前に newNode の親がなかった場合GUIツリーから消す.
      if (!newNodeHasParent) {
        newNode.getViewProxy().removeFromGuiTree();
      }
    }
  }

  /** ノードの接続を表すコマンド. */
  private static class ConnectNodeCmd implements SubOperation {

    /** 繋ぎ替え前の {@link BhNode}. */
    private final BhNode oldNode;
    /** 繋ぎ替えを行うコネクタ. */
    private final Connector connector;

    public ConnectNodeCmd(BhNode oldNode, Connector connector) {
      this.oldNode = oldNode;
      this.connector = connector;
    }

    @Override
    public void doInverseOperation(UserOperation inverseCmd) {
      connector.connectNode(oldNode, inverseCmd);
    }
  }

  /** 最後に入れ替わったノードをセットする操作を表すコマンド. */
  private static class SetLastReplacedCmd implements SubOperation {

    /** 元々セットされていたノード. */
    private final BhNode oldNode;
    /** 入れ替わったノードを登録するノード. */
    private final BhNode nodeRegisteredWith;

    public SetLastReplacedCmd(BhNode oldNode, BhNode nodeRegisteredWith) {
      this.oldNode = oldNode;
      this.nodeRegisteredWith = nodeRegisteredWith;
    }

    @Override
    public void doInverseOperation(UserOperation inverseCmd) {
      nodeRegisteredWith.setLastReplaced(oldNode, inverseCmd);
    }
  }

  /** ワークスペースへのノードの追加を表すコマンド. */
  private static class AddNodeTreeToWorkspaceCmd implements SubOperation {

    /** ワークスペースに追加するノードツリーのルートノード. */
    private final BhNode root;
    /** {@link #root} 以下のノードを削除するワークスペース. */
    private final Workspace ws;

    public AddNodeTreeToWorkspaceCmd(BhNode node, Workspace ws) {
      this.root = node;
      this.ws = ws;
    }

    @Override
    public void doInverseOperation(UserOperation inverseCmd) {
      ws.removeNodeTree(root, inverseCmd);
    }
  }

  /** ワークスペースからのノードの削除を表すコマンド. */
  private static class RemoveNodeTreeFromWorkspaceCmd implements SubOperation {

    /** ワークスペースから削除するノードツリーのルートノード. */
    private final BhNode root;
    /** {@link #root} 以下のノードを削除するワークスペース. */
    private final Workspace ws;

    public RemoveNodeTreeFromWorkspaceCmd(BhNode node, Workspace ws) {
      this.root = node;
      this.ws = ws;
    }

    @Override
    public void doInverseOperation(UserOperation inverseCmd) {
      ws.addNodeTree(root, inverseCmd);
    }
  }

  /** {@link BhNode} の選択を表すコマンド. */
  private static class SelectNodeCmd implements SubOperation {

    /** 選択されたノード. */
    private final BhNode node;

    public SelectNodeCmd(BhNode node) {
      this.node = node;
    }

    @Override
    public void doInverseOperation(UserOperation inverseCmd) {
      node.deselect(inverseCmd);
    }
  }

  /** {@link BhNode} の選択解除を表すコマンド. */
  private static class DeselectNodeCmd implements SubOperation {

    /** 選択解除されたノード. */
    private final BhNode node;

    public DeselectNodeCmd(BhNode node) {
      this.node = node;
    }

    @Override
    public void doInverseOperation(UserOperation inverseCmd) {
      node.select(inverseCmd);
    }
  }

  /** ワークスペースの追加を表すコマンド. */
  private static class AddWorkspaceCmd implements SubOperation {

    /** 追加されたワークスペース. */
    Workspace ws;

    public AddWorkspaceCmd(Workspace ws) {
      this.ws = ws;
    }

    @Override
    public void doInverseOperation(UserOperation inverseCmd) {
      WorkspaceSet wss = ws.getWorkspaceSet();
      if (wss != null) {
        wss.removeWorkspace(ws, inverseCmd);
      }
    }
  }

  /** ワークスペースの削除を表すコマンド. */
  private static class RemoveWorkspaceCmd implements SubOperation {

    /** 削除されたワークスペース. */
    Workspace ws;
    /** {@link #ws} が削除される前に所属していたワークスペースセット. */
    WorkspaceSet wss;

    public RemoveWorkspaceCmd(Workspace ws, WorkspaceSet wss) {
      this.ws = ws;
      this.wss = wss;
    }

    @Override
    public void doInverseOperation(UserOperation inverseCmd) {
      wss.addWorkspace(ws, inverseCmd);
    }
  }

  /** {@link BhNodeView} のコンパイルエラー表示の変更を表すコマンド. */
  private static class SetCompileErrorCmd implements SubOperation {

    /** ノードのコンパイルエラー設定を変更したノード. */
    private final BhNodeView nodeView;
    /** 変更前の状態. */
    private final boolean oldVal;

    public SetCompileErrorCmd(BhNodeView nodeView, boolean oldVal) {
      this.nodeView = nodeView;
      this.oldVal = oldVal;
    }

    @Override
    public void doInverseOperation(UserOperation inverseCmd) {
      boolean curVal = nodeView.getLookManager().isCompileErrorVisible();
      nodeView.getLookManager().setCompileErrorVisibility(oldVal);
      inverseCmd.pushCmdOfSetCompileError(nodeView, curVal);
    }
  }

  /** コレクションへの追加を表すコマンド. */
  private static class AddToListCmd<T> implements SubOperation {

    /** 要素を追加されたコレクション. */
    private final Collection<T> list;
    /** 追加された要素のコレクション. */
    private final Collection<T> addedElems;

    public AddToListCmd(Collection<T> list, Collection<T> addedElems) {
      this.list = list;
      this.addedElems = new ArrayList<>(addedElems);
    }

    public AddToListCmd(Collection<T> list, T addedElem) {
      this.list = list;
      this.addedElems = new ArrayList<>(Arrays.asList(addedElem));
    }

    @Override
    public void doInverseOperation(UserOperation inverseCmd) {
      for (Object elem : addedElems) {
        list.remove(elem);
      }
      inverseCmd.pushCmdOfRemoveFromList(list, addedElems);
    }
  }

  /** コレクションからの削除を表すコマンド. */
  private static class RemoveFromListCmd<T> implements SubOperation {

    /** 要素を削除されたされたコレクション. */
    private final Collection<T> list;
    /** 削除された要素のコレクション. */
    private final Collection<T> removedElems;

    public RemoveFromListCmd(Collection<T> list, T removedElem) {
      this.list = list;
      this.removedElems = new ArrayList<>(Arrays.asList(removedElem));
    }

    public RemoveFromListCmd(Collection<T> list, Collection<T> removedElems) {
      this.list = list;
      this.removedElems = new ArrayList<>(removedElems);
    }

    @Override
    public void doInverseOperation(UserOperation inverseCmd) {
      list.addAll(removedElems);
      inverseCmd.pushCmdOfAddToList(list, removedElems);
    }
  }

  /** ノードのGUIツリーへの追加を表すコマンド. */
  private static class AddToGuiTreeCmd implements SubOperation {

    /** GUI ツリーに登録したノードビュー. */
    private final BhNodeView view;

    public AddToGuiTreeCmd(BhNodeView view) {
      this.view = view;
    }

    @Override
    public void doInverseOperation(UserOperation inverseCmd) {
      Parent parent = view.getTreeManager().getParentGuiComponent();
      view.getTreeManager().removeFromGuiTree();
      inverseCmd.pushCmdOfRemoveFromGuiTree(view, parent);
    }
  }

  /** ノードのGUIツリーからの削除を表すコマンド. */
  private static class RemoveFromGuiTreeCmd implements SubOperation {

    /** GUI ツリーから削除したノードビュー. */
    private final BhNodeView view;
    /** {@code view} を削除したGUIコンポーネント. */
    private final Parent parent;

    public RemoveFromGuiTreeCmd(BhNodeView view, Parent parent) {
      this.view = view;
      this.parent = parent;
    }

    @Override
    public void doInverseOperation(UserOperation inverseCmd) {
      if (parent instanceof Group group) {
        view.getTreeManager().addToGuiTree(group);  
        inverseCmd.pushCmdOfAddToGuiTree(view);
      } else if (parent instanceof Pane pane) {
        view.getTreeManager().addToGuiTree(pane);  
        inverseCmd.pushCmdOfAddToGuiTree(view);
      }
    }
  }

  /** コピー予定のノードをワークスペースセットに追加する操作を表すコマンド. */
  private static class AddNodeToCopyListCmd implements SubOperation {
    
    /** コピー予定のノードを追加したワークスペースセット. */
    private final WorkspaceSet wss;
    /** 追加されたコピー予定のノード. */
    private final BhNode added;

    public AddNodeToCopyListCmd(WorkspaceSet wss, BhNode added) {
      this.wss = wss;
      this.added = added;
    }

    @Override
    public void doInverseOperation(UserOperation inverseCmd) {
      wss.removeNodeFromCopyList(added, inverseCmd);
      inverseCmd.pushCmdOfRemoveNodeFromCopyList(wss, added);
    }
  }

  /** コピー予定のノードをワークスペースセットから削除する操作を表すコマンド. */
  private static class RemoveNodeFromCopyListCmd implements SubOperation {
    
    /** コピー予定のノードを削除したワークスペースセット. */
    private final WorkspaceSet wss;
    /** 削除されたコピー予定のノード. */
    private final BhNode removed;

    public RemoveNodeFromCopyListCmd(WorkspaceSet wss, BhNode removed) {
      this.wss = wss;
      this.removed = removed;
    }

    @Override
    public void doInverseOperation(UserOperation inverseCmd) {
      wss.addNodeToCopyList(removed, inverseCmd);
      inverseCmd.pushCmdOfAddNodeToCopyList(wss, removed);
    }
  }

  /** カット予定のノードをワークスペースセットに追加する操作を表すコマンド. */
  private static class AddNodeToCutListCmd implements SubOperation {
  
    /** カット予定のノードを追加したワークスペースセット. */
    private final WorkspaceSet wss;
    /** 追加されたカット予定のノード. */
    private final BhNode added;

    public AddNodeToCutListCmd(WorkspaceSet wss, BhNode added) {
      this.wss = wss;
      this.added = added;
    }

    @Override
    public void doInverseOperation(UserOperation inverseCmd) {
      wss.removeNodeFromCutList(added, inverseCmd);
      inverseCmd.pushCmdOfRemoveNodeFromCutList(wss, added);
    }
  }

  /** カット予定のノードをワークスペースセットから削除する操作を表すコマンド. */
  private static class RemoveNodeFromCutListCmd implements SubOperation {
    
    /** カット予定のノードを削除したワークスペースセット. */
    private final WorkspaceSet wss;
    /** 削除されたカット予定のノード. */
    private final BhNode removed;

    public RemoveNodeFromCutListCmd(WorkspaceSet wss, BhNode removed) {
      this.wss = wss;
      this.removed = removed;
    }

    @Override
    public void doInverseOperation(UserOperation inverseCmd) {
      wss.addNodeToCutList(removed, inverseCmd);
      inverseCmd.pushCmdOfAddNodeToCutList(wss, removed);
    }
  }

  /** ノードが属するワークスペースを変更する操作を表すコマンド. */
  private static class SetWorkspaceCmd implements SubOperation {

    /** {@link #node} が属していた変更前のワークスペース. */
    private final Workspace oldWs;
    /** ワークスペースが変更されたノード. */
    private final BhNode node;

    public SetWorkspaceCmd(Workspace oldWs, BhNode node) {
      this.oldWs = oldWs;
      this.node = node;
    }

    @Override
    public void doInverseOperation(UserOperation inverseCmd) {
      node.setWorkspace(oldWs, inverseCmd);
    }
  }
}
