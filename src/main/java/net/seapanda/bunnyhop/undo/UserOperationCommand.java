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
import net.seapanda.bunnyhop.common.Vec2D;
import net.seapanda.bunnyhop.common.tools.MsgPrinter;
import net.seapanda.bunnyhop.message.BhMsg;
import net.seapanda.bunnyhop.message.MsgData;
import net.seapanda.bunnyhop.message.MsgService;
import net.seapanda.bunnyhop.message.MsgTransporter;
import net.seapanda.bunnyhop.model.node.BhNode;
import net.seapanda.bunnyhop.model.node.Connector;
import net.seapanda.bunnyhop.model.node.imitation.ImitationBase;
import net.seapanda.bunnyhop.model.workspace.Workspace;
import net.seapanda.bunnyhop.model.workspace.WorkspaceSet;
import net.seapanda.bunnyhop.view.node.BhNodeView;
import net.seapanda.bunnyhop.view.workspace.WorkspaceView;

/**
 * undo/redo 用コマンドクラス.
 *
 * @author K.Koike
 */
public class UserOperationCommand {

  public UserOperationCommand() {}

  /** このオブジェクトが表す操作を構成するサブ操作のリスト. */
  private Deque<SubOperation> subOpeList = new LinkedList<>();

  /**
   * このコマンドの逆の操作を行う (例えば, ノード追加ならノード削除を行う).
   *
   * @return このコマンドの逆の操作を表す UserOperationCommand オブジェクトを返す. <br>
   *          つまり, 戻りオブジェクトの doInverseOperation はこのコマンドの元になった操作を行う
   */
  UserOperationCommand doInverseOperation() {
    UserOperationCommand inverseCmd = new UserOperationCommand();
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
      MsgPrinter.INSTANCE.msgForDebug("subope  " + subope);
    }
    MsgPrinter.INSTANCE.msgForDebug("");
  }

  /**
   * イミテーションノードリストへの追加をコマンド化してサブ操作リストに加える.
   *
   * @param imit 追加したイミテーションノード
   * @param org imit のオリジナルノード
   */
  public <T extends ImitationBase<T>> void pushCmdOfAddImitation(T imit, T org) {
    subOpeList.addLast(new AddImitationCmd<T>(imit, org));
  }

  /**
   * イミテーションノードリストからの削除をコマンド化してサブ操作リストに加える.
   *
   * @param imit 削除したイミテーションノード
   * @param org imit を保持していたオリジナルノード
   */
  public <T extends ImitationBase<T>> void pushCmdOfRemoveImitation(T imit, T org) {
    subOpeList.addLast(new RemoveImitationCmd<T>(imit, org));
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
   * @param x ワークスペース上での元の位置 x
   * @param y ワークスペース上での元の位置 y
   * @param node 位置指定をしたノード
   */
  public void pushCmdOfSetPosOnWorkspace(double x, double y, BhNode node) {
    subOpeList.addLast(new SetPosOnWorkspaceCmd(x, y, node));
  }

  /**
   * 4 分木ノードの4 分木空間への登録をコマンド化してサブ操作リストに加える.
   *
   * @param node 4 分木ノードを登録するBhNode
   * @param ws 追加した4 分木ノードがあった4 分木空間に対応するワークスペース
   */
  public void pushCmdOfaddQtRectangle(BhNode node, Workspace ws) {
    subOpeList.addLast(new AddQtRectangleCmd(node, ws));
  }

  /**
   * 4 分木ノードの 4 分木空間からの削除をコマンド化してサブ操作リストに加える.
   *
   * @param node 4 分木ノードを削除したBhNode
   * @param ws 削除した4 分木ノードがあった4 分木空間に対応するワークスペース
   */
  public void pushCmdOfRemoveQtRectangle(BhNode node, Workspace ws) {
    subOpeList.addLast(new RemoveQtRectangleCmd(node, ws));
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
   * @param wsView 追加されたワークスペースのビュー
   * @param wss  ワークスペースを追加したワークスペースセット
   */
  public void pushCmdOfAddWorkspace(Workspace ws, WorkspaceView wsView, WorkspaceSet wss) {
    subOpeList.addLast(new AddWorkspaceCmd(ws, wsView, wss));
  }

  /**
   * ワークスペースの削除をコマンド化してサブ操作リストに加える.
   *
   * @param ws 削除されたワークスペース
   * @param wsView 削除されたワークスペースのビュー
   * @param wss  ワークスペースを削除したワークスペースセット
   */
  public void pushCmdOfDeleteWorkspace(Workspace ws, WorkspaceView wsView, WorkspaceSet wss) {
    subOpeList.addLast(new DeleteWorkspaceCmd(ws, wsView, wss));
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
   * ノードの構文エラー設定をコマンド化してサブ操作リストに加える.
   *
   * @param nodeView ノードの構文エラー設定を変更したノード
   * @param setVal 設定した状態
   * @param prevVal 前の状態
   */
  public void pushCmdOfSetSyntaxError(BhNodeView nodeView, boolean setVal, boolean prevVal) {
    subOpeList.addLast(new SetSyntaxErrorCmd(nodeView, setVal, prevVal));
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

  /** {@link UserOperationCommand} を構成するサブ操作. */
  interface SubOperation {
    /**
     * このSubOperation の逆の操作を行う.
     *
     * @param inverseCmd このサブ操作の逆の操作を作るための UserOperationCommand オブジェクト
     */
    public void doInverseOperation(UserOperationCommand inverseCmd);
  }

  /** イミテーションノードの追加を表すコマンド. */
  private static class AddImitationCmd<T extends ImitationBase<T>> implements SubOperation {
    
    /** リストに追加されたイミテーション. */
    private final T imit;
    /** {@code imit} のオリジナルノード. */
    private final T org;

    public AddImitationCmd(T imit, T org) {
      this.imit = imit;
      this.org = org;
    }

    @Override
    public void doInverseOperation(UserOperationCommand inverseCmd) {
      org.removeImitation(imit, inverseCmd);
    }
  }

  /** イミテーションノードの削除を表すコマンド. */
  private static class RemoveImitationCmd<T extends ImitationBase<T>> implements SubOperation {

    /** リストから削除されたイミテーション. */
    private final T imit;
    /** {@code imit} のオリジナルノード. */
    private final T org;

    public RemoveImitationCmd(T imit, T org) {
      this.imit = imit;
      this.org = org;
    }

    @Override
    public void doInverseOperation(UserOperationCommand inverseCmd) {
      org.addImitation(imit, inverseCmd);
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
    public void doInverseOperation(UserOperationCommand inverseCmd) {
      MsgService.INSTANCE.removeRootNode(node, inverseCmd);
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
    public void doInverseOperation(UserOperationCommand inverseCmd) {
      MsgTransporter.INSTANCE.sendMessage(BhMsg.ADD_ROOT_NODE, node, ws);
      inverseCmd.pushCmdOfAddRootNode(node);
    }
  }

  /** ワークスペース上の位置指定を表すコマンド. */
  private static class SetPosOnWorkspaceCmd implements SubOperation {

    /** 指定前のワークスペース上での位置X. */
    private final double posX;
    /** 指定前のワークスペース上での位置y. */
    private final double posY;
    /** 位置を指定したノード. */
    private final BhNode node;

    public SetPosOnWorkspaceCmd(double x, double y, BhNode node) {
      this.posX = x;
      this.posY = y;
      this.node = node;
    }

    @Override
    public void doInverseOperation(UserOperationCommand inverseCmd) {
      Vec2D curPos = MsgService.INSTANCE.getPosOnWs(node);
      inverseCmd.pushCmdOfSetPosOnWorkspace(curPos.x, curPos.y, node);
      MsgService.INSTANCE.setPosOnWs(node, posX, posY);
    }
  }

  /** 4 分木空間への 4 分木ノード登録を表すコマンド. */
  private static class AddQtRectangleCmd implements SubOperation {

    /** 4 分木ノードを登録した {@link BhNode}. */
    private final BhNode node;
    /** 追加した4 分木ノードがあった 4 分木空間に対応するワークスペース. */
    private final Workspace ws;

    public AddQtRectangleCmd(BhNode node, Workspace ws) {
      this.node = node;
      this.ws = ws;
    }

    @Override
    public void doInverseOperation(UserOperationCommand inverseCmd) {
      MsgTransporter.INSTANCE.sendMessage(BhMsg.REMOVE_QT_RECTANGLE, node);
      inverseCmd.pushCmdOfRemoveQtRectangle(node, ws);
    }
  }

  /** 4 分木空間からの4 分木ノード削除を表すコマンド. */
  private static class RemoveQtRectangleCmd implements SubOperation {

    /** 4 分木ノードから削除した {@link BhNode}. */
    private final BhNode node;
    /** 削除した 4 分木ノードがあった 4 分木空間に対応するワークスペース. */
    private final Workspace ws;

    public RemoveQtRectangleCmd(BhNode node, Workspace ws) {
      this.node = node;
      this.ws = ws;
    }

    @Override
    public void doInverseOperation(UserOperationCommand inverseCmd) {
      MsgService.INSTANCE.addQtRectangle(node, ws, inverseCmd);
      MsgTransporter.INSTANCE.sendMessage(BhMsg.UPDATE_ABS_POS, node);    //4 分木空間での位置確定
    }
  }

  /** BhNodeView の入れ替えを表すコマンド. */
  private static class ReplaceNodeViewCmd implements SubOperation {

    /** 入れ替え前の古い View に対応する {@link BhNode}. */
    private final BhNode oldNode;
    /** 入れ替え後の新しい View に対応する {@link BhNode}. */
    private final BhNode newNode;
    /** 入れ替え前に newNode が親GUIコンポーネントを持っていた場合 true. */
    private final boolean newNodeHasParent;

    public ReplaceNodeViewCmd(BhNode oldNode, BhNode newNode, boolean newNodeHasParent) {
      this.oldNode = oldNode;
      this.newNode = newNode;
      this.newNodeHasParent = newNodeHasParent;
    }

    @Override
    public void doInverseOperation(UserOperationCommand inverseCmd) {
      //元々付いていた古いViewに付け替える
      MsgService.INSTANCE.replaceChildNodeView(newNode, oldNode, inverseCmd);
      // 入れ替え前に newNode の親がなかった場合GUIツリーから消す.
      if (!newNodeHasParent) {
        MsgService.INSTANCE.removeFromGuiTree(newNode);
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
    public void doInverseOperation(UserOperationCommand inverseCmd) {
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
    public void doInverseOperation(UserOperationCommand inverseCmd) {
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
    public void doInverseOperation(UserOperationCommand inverseCmd) {
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
    public void doInverseOperation(UserOperationCommand inverseCmd) {
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
    public void doInverseOperation(UserOperationCommand inverseCmd) {
      ws.addSelectedNode(node, inverseCmd);
    }
  }

  /** ワークスペースの追加を表すコマンド. */
  private static class AddWorkspaceCmd implements SubOperation {

    /** 追加されたワークスペース. */
    Workspace ws;
    /** 追加されたワークスペースのビュー. */
    WorkspaceView wsView;
    /** ワークスペースを追加したワークスペースセット. */
    WorkspaceSet wss;

    public AddWorkspaceCmd(Workspace ws, WorkspaceView wsView, WorkspaceSet wss) {
      this.ws = ws;
      this.wsView = wsView;
      this.wss = wss;
    }

    @Override
    public void doInverseOperation(UserOperationCommand inverseCmd) {
      MsgTransporter.INSTANCE.sendMessage(
          BhMsg.DELETE_WORKSPACE, new MsgData(ws, wsView, inverseCmd), wss);
    }
  }

  /** ワークスペースの削除を表すコマンド. */
  private static class DeleteWorkspaceCmd implements SubOperation {

    /** 削除されたワークスペース. */
    Workspace ws;
    /** 削除されたワークスペースのビュー. */
    WorkspaceView wsView;
    /** ワークスペースを削除したワークスペースセット. */
    WorkspaceSet wss;

    public DeleteWorkspaceCmd(Workspace ws, WorkspaceView wsView, WorkspaceSet wss) {
      this.ws = ws;
      this.wsView = wsView;
      this.wss = wss;
    }

    @Override
    public void doInverseOperation(UserOperationCommand inverseCmd) {
      MsgTransporter.INSTANCE.sendMessage(
          BhMsg.ADD_WORKSPACE, new MsgData(ws, wsView, inverseCmd), wss);
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
    public void doInverseOperation(UserOperationCommand inverseCmd) {
      nodeView.getLookManager().setVisible(!visible);
      inverseCmd.pushCmdOfSetVisible(nodeView, !visible);
    }
  }

  /** {@link BhNodeView} の構文エラー表示の変更を表すコマンド. */
  private static class SetSyntaxErrorCmd implements SubOperation {

    /** ノードの構文エラー設定を変更したノード. */
    private final BhNodeView nodeView;
    /** 設定した状態. */
    private final boolean setVal;
    /** 以前の状態. */
    private final boolean prevVal;

    public SetSyntaxErrorCmd(BhNodeView nodeView, boolean setVal, boolean prevVal) {
      this.nodeView = nodeView;
      this.setVal = setVal;
      this.prevVal = prevVal;
    }

    @Override
    public void doInverseOperation(UserOperationCommand inverseCmd) {
      nodeView.getLookManager().setSytaxErrorVisibility(prevVal);
      inverseCmd.pushCmdOfSetSyntaxError(nodeView, prevVal, setVal);
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
    public void doInverseOperation(UserOperationCommand inverseCmd) {
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
    public void doInverseOperation(UserOperationCommand inverseCmd) {
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
    public void doInverseOperation(UserOperationCommand inverseCmd) {
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
    public void doInverseOperation(UserOperationCommand inverseCmd) {
      view.getTreeManager().addToGuiTree(parent);
      inverseCmd.pushCmdOfAddToGuiTree(view);
    }
  }
}
