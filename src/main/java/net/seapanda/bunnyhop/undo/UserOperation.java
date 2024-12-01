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
import javafx.scene.Parent;
import net.seapanda.bunnyhop.model.node.BhNode;
import net.seapanda.bunnyhop.model.node.Connector;
import net.seapanda.bunnyhop.model.node.derivative.DerivativeBase;
import net.seapanda.bunnyhop.model.workspace.Workspace;
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
  public int getNumSubOpe() {
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
   * ワークスペース直下へのルートノードの追加をコマンド化してサブ操作リストに加える.
   *
   * @param node ワークスペース直下に追加したルートノード
   */
  public void pushCmdOfAddRootNode(BhNode node) {
    subOpeList.addLast(new AddRootNodeCmd(node));
  }

  /**
   * ワークスペース直下からのルートノードの削除をコマンド化してサブ操作リストに加える.
   *
   * @param node ワークスペース直下から削除したルートノード
   * @param ws ルートノードを削除したワークスペース
   */
  public void pushCmdOfRemoveRootNode(BhNode node, Workspace ws) {
    subOpeList.addLast(new RemoveRootNodeCmd(node, ws));
  }

  /**
   * ワークスペース上での位置指定をコマンド化してサブ操作リストに加える.
   *
   * @param oldPos 移動前に位置
   * @param newPos 移動後に位置
   * @param node 位置指定をしたノード
   */
  public void pushCmdOfSetPosOnWorkspace(Vec2D oldPos, Vec2D newPos, BhNode node) {
    subOpeList.addLast(new SetPosOnWorkspaceCmd(oldPos, newPos, node));
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
   * ノードへのワークスペースの登録をコマンド化してサブ操作リストに加える.
   *
   * @param oldWs 元々セットされていたワークスペース
   * @param node ワークスペースのセットを行うノード
   */
  public void pushCmdOfSetWorkspace(Workspace oldWs, BhNode node) {
    subOpeList.addLast(new SetWorkspaceCmd(oldWs, node));
  }

  /**
   * 選択されたノードのリストへのノードの追加をコマンド化してサブ操作リストに加える.
   *
   * @param ws 選択されたノードのリストを持つワークスペース
   * @param node 選択されたノードのリストに追加するノード
   */
  public void pushCmdOfAddSelectedNode(Workspace ws, BhNode node) {
    subOpeList.addLast(new AddSelectedNodeCmd(ws, node));
  }

  /**
   * 選択されたノードのリストからのノードの削除をコマンド化してサブ操作リストに加える.
   *
   * @param ws 選択されたノードのリストを持つワークスペース
   * @param node 選択されたノードのリストから削除するノード
   */
  public void pushCmdOfRemoveSelectedNode(Workspace ws, BhNode node) {
    subOpeList.addLast(new RemoveSelectedNodeCmd(ws, node));
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
   */
  public void pushCmdOfDeleteWorkspace(Workspace ws) {
    subOpeList.addLast(new DeleteWorkspaceCmd(ws));
  }

  /**
   * ノードの可視性変更をコマンド化してサブ操作リストに加える.
   *
   * @param nodeView 可視性を変更したノード
   * @param visible 変更した可視性 (true -> 可視, false -> 不可視)
   */
  public void pushCmdOfSetVisible(BhNodeView nodeView, boolean visible) {
    subOpeList.addLast(new SetVisibleCmd(nodeView, visible));
  }

  /**
   * ノードのコンパイルエラー設定をコマンド化してサブ操作リストに加える.
   *
   * @param nodeView ノードのコンパイルエラー設定を変更したノード
   * @param setVal 設定した状態
   * @param prevVal 前の状態
   */
  public void pushCmdOfSetCompileError(BhNodeView nodeView, boolean setVal, boolean prevVal) {
    subOpeList.addLast(new SetCompileErrorCmd(nodeView, setVal, prevVal));
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
   * */
  public void pushCmdOfAddToGuiTree(BhNodeView view) {
    subOpeList.addLast(new AddToGuiTreeCmd(view));
  }

  /**
   * GUI ツリーへのノードビューの削除をコマンド化してサブ操作リストに加える.
   *
   * @param view GUI ツリーに登録したノードビュー
   * @param parent view を削除したGUIコンポーネント
   * */
  public void pushCmdOfRemoveFromGuiTree(BhNodeView view, Parent parent) {
    subOpeList.addLast(new RemoveFromGuiTreeCmd(view, parent));
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

  /** ワークスペースへのノードの追加を表すコマンド. */
  private static class AddRootNodeCmd implements SubOperation {

    /** ワークスペース直下に追加したノード. */
    private final BhNode node;

    public AddRootNodeCmd(BhNode node) {
      this.node = node;
    }

    @Override
    public void doInverseOperation(UserOperation inverseCmd) {
      BhService.cmdProxy().removeRootNode(node, inverseCmd);
    }
  }

  /** ワークスペースからのノードの削除を表すコマンド. */
  private static class RemoveRootNodeCmd implements SubOperation {

    /** ワークスペース直下から削除したノード. */
    private final BhNode node;
    /** ノードを削除したワークスペース. */
    private final Workspace ws;

    public RemoveRootNodeCmd(BhNode node, Workspace ws) {
      this.node = node;
      this.ws = ws;
    }

    @Override
    public void doInverseOperation(UserOperation inverseCmd) {
      BhService.cmdProxy().addRootNode(node, ws, new UserOperation());
      inverseCmd.pushCmdOfAddRootNode(node);
    }
  }

  /** ワークスペース上の位置指定を表すコマンド. */
  private static class SetPosOnWorkspaceCmd implements SubOperation {

    /** 移動前の位置. */
    private final Vec2D oldPos;
    /** 移動後の位置. */
    private final Vec2D newPos;
    /** 位置を指定したノード. */
    private final BhNode node;

    public SetPosOnWorkspaceCmd(Vec2D oldPos, Vec2D newPos, BhNode node) {
      this.oldPos = oldPos;
      this.newPos = newPos;
      this.node = node;
    }

    @Override
    public void doInverseOperation(UserOperation inverseCmd) {
      inverseCmd.pushCmdOfSetPosOnWorkspace(newPos, oldPos, node);
      BhService.cmdProxy().setPosOnWs(node, oldPos.x, oldPos.y);
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
        view.getPositionManager().setPosOnWorkspace(pos.x, pos.y);
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
      BhService.cmdProxy().replaceChildNodeView(newNode, oldNode, inverseCmd);
      // 入れ替え前に newNode の親がなかった場合GUIツリーから消す.
      if (!newNodeHasParent) {
        BhService.cmdProxy().removeFromGuiTree(newNode);
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

  /** ノードに対してワークスペースの登録を行う操作を表すコマンド. */
  private static class SetWorkspaceCmd implements SubOperation {

    /** ワークスペースを登録するノード. */
    private final BhNode node;
    /** 以前登録されていたワークスペース. */
    private final Workspace oldWs;

    public SetWorkspaceCmd(Workspace oldWs, BhNode node) {
      this.node = node;
      this.oldWs = oldWs;
    }

    @Override
    public void doInverseOperation(UserOperation inverseCmd) {
      node.setWorkspace(oldWs, inverseCmd);
    }
  }

  /** 選択されたノードのリストへの BhNode の追加を表すコマンド. */
  private static class AddSelectedNodeCmd implements SubOperation {

    /** 選択されたノードのリストを持つワークスペース. */
    private final Workspace ws;
    /** 選択されたノードのリストに追加するノード. */
    private final BhNode node;

    public AddSelectedNodeCmd(Workspace ws, BhNode node) {
      this.node = node;
      this.ws = ws;
    }

    @Override
    public void doInverseOperation(UserOperation inverseCmd) {
      ws.removeSelectedNode(node, inverseCmd);
    }
  }

  /** 選択されたノードのリストからの BhNode の削除を表すコマンド. */
  private static class RemoveSelectedNodeCmd implements SubOperation {

    /** 選択されたノードのリストを持つワークスペース. */
    private final Workspace ws;
    /** 選択されたノードのリストから削除するノード. */
    private final BhNode node;

    public RemoveSelectedNodeCmd(Workspace ws, BhNode node) {
      this.node = node;
      this.ws = ws;
    }

    @Override
    public void doInverseOperation(UserOperation inverseCmd) {
      ws.addSelectedNode(node, inverseCmd);
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
      BhService.cmdProxy().deleteWorkspace(ws, inverseCmd);
    }
  }

  /** ワークスペースの削除を表すコマンド. */
  private static class DeleteWorkspaceCmd implements SubOperation {

    /** 削除されたワークスペース. */
    Workspace ws;

    public DeleteWorkspaceCmd(Workspace ws) {
      this.ws = ws;
    }

    @Override
    public void doInverseOperation(UserOperation inverseCmd) {
      BhService.cmdProxy().addWorkspace(ws, inverseCmd);
    }
  }

  /** {@link BhNodeView} の可視性変更を表すコマンド. */
  private static class SetVisibleCmd implements SubOperation {

    /** 可視性を変更したノード. */
    private final BhNodeView nodeView;
    /** 設定した可視性. */
    private final boolean visible;

    public SetVisibleCmd(BhNodeView nodeView, boolean visible) {
      this.nodeView = nodeView;
      this.visible = visible;
    }

    @Override
    public void doInverseOperation(UserOperation inverseCmd) {
      nodeView.getLookManager().setVisible(!visible);
      inverseCmd.pushCmdOfSetVisible(nodeView, !visible);
    }
  }

  /** {@link BhNodeView} のコンパイルエラー表示の変更を表すコマンド. */
  private static class SetCompileErrorCmd implements SubOperation {

    /** ノードのコンパイルエラー設定を変更したノード. */
    private final BhNodeView nodeView;
    /** 設定した状態. */
    private final boolean setVal;
    /** 以前の状態. */
    private final boolean prevVal;

    public SetCompileErrorCmd(BhNodeView nodeView, boolean setVal, boolean prevVal) {
      this.nodeView = nodeView;
      this.setVal = setVal;
      this.prevVal = prevVal;
    }

    @Override
    public void doInverseOperation(UserOperation inverseCmd) {
      nodeView.getLookManager().setSytaxErrorVisibility(prevVal);
      inverseCmd.pushCmdOfSetCompileError(nodeView, prevVal, setVal);
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
      Parent parent = view.getParent();
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
      view.getTreeManager().addToGuiTree(parent);
      inverseCmd.pushCmdOfAddToGuiTree(view);
    }
  }
}
