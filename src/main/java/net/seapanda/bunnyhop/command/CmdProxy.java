/*
 * Copyright 2018 K.Koike
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

package net.seapanda.bunnyhop.command;

import java.util.Collection;
import java.util.Objects;
import net.seapanda.bunnyhop.common.BhConstants;
import net.seapanda.bunnyhop.control.workspace.WorkspaceController;
import net.seapanda.bunnyhop.model.node.BhNode;
import net.seapanda.bunnyhop.model.node.TextNode;
import net.seapanda.bunnyhop.model.node.derivative.Derivative;
import net.seapanda.bunnyhop.model.workspace.Workspace;
import net.seapanda.bunnyhop.model.workspace.WorkspaceSet;
import net.seapanda.bunnyhop.quadtree.QuadTreeRectangle;
import net.seapanda.bunnyhop.service.BhService;
import net.seapanda.bunnyhop.undo.UserOperation;
import net.seapanda.bunnyhop.utility.Pair;
import net.seapanda.bunnyhop.utility.Vec2D;
import net.seapanda.bunnyhop.view.ViewInitializationException;
import net.seapanda.bunnyhop.view.node.BhNodeView;
import net.seapanda.bunnyhop.view.nodeselection.BhNodeSelectionView;
import net.seapanda.bunnyhop.view.workspace.MultiNodeShifterView;
import net.seapanda.bunnyhop.view.workspace.WorkspaceView;

/**
 * コマンド送信を伴う処理をラップしたクラス.
 *
 * @author K.Koike
 */
public class CmdProxy {

  private final CmdTransporter transporter = new CmdTransporter();
  private final WorkspaceSet wss;

  /** コンストラクタ. */
  public CmdProxy(WorkspaceSet wss) {
    Objects.requireNonNull(wss);
    this.wss = wss;
  }

  /**
   * 引数で指定したノードのワークスペース上での位置を取得する.
   * 位置の取得ができなかった場合 null.
   */
  public Vec2D getPosOnWs(BhNode node) {
    CmdData result = transporter.sendCmd(BhCmd.GET_POS_ON_WORKSPACE, node);
    if (result == null) {
      return null;
    }
    return result.vec2d;
  }

  /**
   * 引数で指定したノードのワークスペース上での位置を更新する.
   * 4 分木空間上の位置も更新する.
   *
   * @param node ワークスペース上での位置を更新するノード. (ルートノードを指定すること)
   * @param x ワークスペース上での x 位置
   * @param y ワークスペース上での y 位置
   * @param userOpe undo 用コマンドオブジェクト
   */
  public void setPosOnWs(BhNode node, double x, double y, UserOperation userOpe) {
    Vec2D oldPos = getPosOnWs(node);
    Vec2D newPos = new Vec2D(x, y);
    if (oldPos != null) {
      userOpe.pushCmdOfSetPosOnWorkspace(oldPos, newPos, node);
    }
    transporter.sendCmd(BhCmd.SET_POS_ON_WORKSPACE, new CmdData(newPos), node);
  }

  /**
   * 引数で指定したノードのワークスペース上での位置を更新する.
   * 4 分木空間上の位置も更新する.
   *
   * @param node ワークスペース上での位置を更新するノード. (ルートノードを指定すること)
   * @param x ワークスペース上での x 位置
   * @param y ワークスペース上での y 位置
   */
  public void setPosOnWs(BhNode node, double x, double y) {
    Vec2D newPos = new Vec2D(x, y);
    transporter.sendCmd(BhCmd.SET_POS_ON_WORKSPACE, new CmdData(newPos), node);
  }

  /**
   * ノードの可視性を変更する.
   *
   * @param node このノードの可視性を変更する
   * @param visible 可視状態にする場合true, 不可視にする場合false
   * @param userOpe undo 用コマンドオブジェクト
   */
  public void setNodeVisibility(BhNode node, boolean visible, UserOperation userOpe) {
    transporter.sendCmd(BhCmd.SET_VISIBLE, new CmdData(visible, userOpe), node);
  }

  /**
   * ノードのコンパイルエラー表示を変更する.
   *
   * @param node 警告表示を変更するノード
   * @param show 警告を表示する場合 true. 隠す場合 false.
   * @param userOpe undo 用コマンドオブジェクト
   * */
  public void setCompileErrorMark(BhNode node, boolean show, UserOperation userOpe) {
    transporter.sendCmd(
        BhCmd.SET_SYNTAX_ERRPR_INDICATOR, new CmdData(show, userOpe), node);
  }
  
  /**
   * 複数ノード移動用マルチノードシフタとリンクを更新する.
   *
   * @param node マルチノードシフタ更新の原因を作ったノード
   * @param ws 更新するマルチノードシフタを含むワークスペース
   */
  public void updateMultiNodeShifter(BhNode node, Workspace ws) {
    transporter.sendCmd(BhCmd.UPDATE_MULTI_NODE_SHIFTER, new CmdData(node), ws);
  }

  /**
   * ノードボディのワークスペース上での範囲を取得する.
   *
   * @param node このノードのワークスペース上での範囲を取得する
   */
  public Pair<Vec2D, Vec2D> getNodeBodyRange(BhNode node) {
    BhNodeView nodeView = getBhNodeView(node);
    QuadTreeRectangle bodyRange = nodeView.getRegionManager().getRegions().body();
    return new Pair<Vec2D, Vec2D>(bodyRange.getUpperLeftPos(), bodyRange.getLowerRightPos());
  }

  /**
   * 外部ノードを含んだノードのサイズを取得する.
   *
   * @param node サイズを取得したいノード
   * @return 外部ノードを含んだノードのサイズ
   */
  public Vec2D getViewSizeIncludingOuter(BhNode node) {
    CmdData cmdData = transporter.sendCmd(
        BhCmd.GET_VIEW_SIZE_INCLUDING_OUTER, new CmdData(true), node);
    return cmdData.vec2d;
  }

  /**
   * ワークスペース上のノードを動かす.
   *
   * @param node 動かすノード
   * @param distance 移動距離
   */
  public void moveNodeOnWs(BhNode node, Vec2D distance) {
    transporter.sendCmd(BhCmd.MOVE_NODE_ON_WORKSPACE, new CmdData(distance), node);
  }

  /**
   * 貼り付け候補のノードのリストから引数で指定したノードを取り除く.
   *
   * @param nodeToRemove 取り除くノード
   * @param userOpe undo 用コマンドオブジェクト
   */
  public void removeFromPasteList(BhNode nodeToRemove, UserOperation userOpe) {
    transporter.sendCmd(BhCmd.REMOVE_NODE_TO_PASTE, new CmdData(nodeToRemove, userOpe), wss);
  }

  /**
   * ノードビューを入れ替える. GUI ツリーからは取り除かない.
   *
   * @param oldNode 入れ替えられる古いノードビューの BhNode
   * @param newNode 入れ替えられる新しいノードビューの BhNode
   * @param userOpe undo 用コマンドオブジェクト
   */
  public void replaceChildNodeView(
      BhNode oldNode, BhNode newNode, UserOperation userOpe) {
    BhNodeView newNodeView = getBhNodeView(newNode);
    boolean hasParent = newNodeView.getParent() != null;
    transporter.sendCmd(BhCmd.REPLACE_NODE_VIEW, new CmdData(newNodeView), oldNode);
    userOpe.pushCmdOfReplaceNodeView(oldNode, newNode, hasParent);
  }

  /**
   * ノードビューをGUIツリーから取り除く.
   *
   * @param node このノードのノードビューをGUIツリーから取り除く
   */
  public void removeFromGuiTree(BhNode node) {
    transporter.sendCmd(BhCmd.REMOVE_FROM_GUI_TREE, node);
  }

  /**
   * 4 分木空間にノードの領域を登録する.
   * 異なるワークスペースに {@code node} の領域が重複して登録されることはない.
   *
   * @param node このノードの領域をワークスペースの 4 分木空間に登録する
   * @param ws このワークスペースが持つ4 分木空間にノードの領域を登録する
   * @param userOpe undo 用コマンドオブジェクト
   */
  public void setQtRectangle(BhNode node, Workspace ws, UserOperation userOpe) {
    transporter.sendCmd(BhCmd.SET_QT_RECTANGLE, new CmdData(userOpe), node, ws);
  }

  /**
   * 4 分木空間からノードの領域を削除する.
   *
   * @param node このノードの領域をワークスペースの4 分木空間から削除する
   * @param userOpe undo 用コマンドオブジェクト
   * */
  public void removeQtRectangle(BhNode node, UserOperation userOpe) {
    transporter.sendCmd(BhCmd.REMOVE_QT_RECTANGLE, new CmdData(userOpe), node);
  }

  /**
   * ワークスペースにルートノードを追加する.
   *
   * @param node 追加するノード.  このノードは {@code ws} 上のルートノードとなる.
   * @param ws {@code node} を追加する {@link Workspace}
   * @param userOpe undo 用コマンドオブジェクト
   */
  public void addRootNode(BhNode node, Workspace ws, UserOperation userOpe) {
    transporter.sendCmd(BhCmd.ADD_ROOT_NODE, node, ws);
    userOpe.pushCmdOfAddRootNode(node);
  }

  /**
   * ワークスペースからルートノードを削除する.
   *
   * @param node 削除するルートノード
   * @param userOpe undo 用コマンドオブジェクト
   */
  public void removeRootNode(BhNode node, UserOperation userOpe) {
    Workspace ws = node.getWorkspace();
    transporter.sendCmd(BhCmd.REMOVE_ROOT_NODE, node, ws);
    userOpe.pushCmdOfRemoveRootNode(node, ws);
  }

  /**
   * ノードビューの選択表示を切り替える.
   *
   * @param node このノードのノードビューの選択表示を切り替える
   * @param enable 選択表示を有効にする場合 true. 無効にする場合 false.
   */
  public void selectNodeView(BhNode node, boolean enable) {
    transporter.sendCmd(BhCmd.SELECT_NODE_VIEW, new CmdData(enable), node);
  }

  /**
   * orgNode の派生ノードの強調表示を切り替える.
   *
   * @param orgNode このノードの派生ノードの強調表示を切り替える
   * @param enable 強調表示を有効にする場合 true.  無効にする場合 false.
   */
  public void hilightDerivatives(BhNode orgNode, boolean enable) {
    if (orgNode instanceof Derivative) {
      Collection<? extends Derivative> derivatives = ((Derivative) orgNode).getDerivatives();
      derivatives.forEach(derivative -> switchPseudoClassActivation(
          derivative, BhConstants.Css.PSEUDO_HIGHLIGHT_DERIVATIVE, enable));
    }
  }

  /**
   * orgNode の外見に適用されるの疑似クラスの有効/無効を切り替える.
   *
   * @param node 疑似クラスの有効/無効を切り替えるノード
   * @param pseudoClassName 有効/無効を切り替える疑似クラス
   * @param enable 疑似クラスを有効にする場合 true.  無効にする場合 false.
   */
  public void switchPseudoClassActivation(BhNode node, String pseudoClassName, boolean enable) {
    transporter.sendCmd(
        BhCmd.SWITCH_PSEUDO_CLASS_ACTIVATION, new CmdData(enable, pseudoClassName), node);
  }

  /**
   * 引数で指定したワークスペースの描画サイズを取得する.
   *
   * @param ws このワークスペースの描画サイズを取得する
   * @return 引数で指定したワークスペースの描画サイズ.
   *         {@code ws} のワークスペースビューが存在しない場合 null を返す.
   */
  public Vec2D getWorkspaceSize(Workspace ws) {
    CmdData result = transporter.sendCmd(BhCmd.GET_WORKSPACE_SIZE, ws);
    if (result == null) {
      return null;
    }
    return result.vec2d;
  }

  /**
   * {@code node} が持つ文字列に合わせて, その View の内容を変更する.
   * {@code node} がビューを持たない場合, 何もしない.
   *
   * @param node このノードのビューを変更する.
   */
  public void matchViewContentToModel(TextNode node) {
    transporter.sendCmd(BhCmd.MATCH_VIEW_CONTENT_TO_MODEL, node);
  }

  /**
   * Scene 上の位置を引数で指定したワークスペース上の位置に変換する.
   *
   * @param scenePosX Scene 上の X 位置
   * @param scenePosY Scene 上の Y 位置
   * @param ws scenePosX と scenePosY をこのワークスペース上の位置に変換する
   * @return scenePosX と scenePosY のワークスペース上の位置
   */
  public Vec2D sceneToWorkspace(double scenePosX, double scenePosY, Workspace ws) {
    return transporter.sendCmd(
        BhCmd.SCENE_TO_WORKSPACE,
        new CmdData(new Vec2D(scenePosX, scenePosY)),
        ws).vec2d;
  }

  /**
   * 引数で指定した BhNode に対応する BhNodeView を取得する.
   *
   * @param node このノードに対応するビューを取得する
   * @return node に対応する BhNodeView
   */
  public BhNodeView getBhNodeView(BhNode node) {
    return transporter.sendCmd(BhCmd.GET_VIEW, node).nodeView;
  }

  /**
   * 引数で指定した BhNode に対応する BhNodeView が存在するか調べる.
   *
   * @param node このノードに対応するビューが存在するか調べる.
   * @return node に対応するビューが存在する場合 true
   */
  public boolean hasView(BhNode node) {
    CmdData result = transporter.sendCmd(BhCmd.GET_VIEW, node);
    return result != null;
  }
  
  /**
   * 引数で指定したノードをワークスペース中央に表示する.
   *
   * @param node 中央に表示するノード
   */
  public void lookAt(BhNode node) {
    BhNodeView nodeView = getBhNodeView(node);
    transporter.sendCmd(
        BhCmd.LOOK_AT_NODE_VIEW, new CmdData(nodeView), node.getWorkspace());
  }

  /**
   * ノード選択ビューを GUI ツリーに追加する.
   *
   * @param view 追加するノード選択ビュー
   */
  public void addNodeSelectionView(BhNodeSelectionView view) {
    transporter.sendCmd(BhCmd.ADD_NODE_SELECTION_PANEL, new CmdData(view), wss);
  }

  /**
   * 引数で指定したノードがテンプレートノードかどうかを調べる.
   *
   * @param node テンプレートノードかどうかを調べるノード
   * @return {@code node} がテンプレートノードである場合 true
   */
  public boolean isTemplateNode(BhNode node) {
    return transporter.sendCmd(BhCmd.IS_TEMPLATE_NODE, node).bool;
  }

  /**
   * ワークスペースの表示を拡大する.
   *
   * @param ws 拡大表示するワークスペース
   */
  public void zoomInOnWorkspace(Workspace ws) {
    transporter.sendCmd(BhCmd.ZOOM, new CmdData(true), ws);
  }

  /** 
   * ワークスペースの表示を縮小する.
   *
   * @param ws 縮小表示するワークスペース
   */
  public void zoomOutOnWorkspace(Workspace ws) {
    transporter.sendCmd(BhCmd.ZOOM, new CmdData(false), ws);
  }

  /** 
   * ワークスペースの表示の拡大率を設定する.
   *
   * @param ws 拡大率を設定するワークスペース
   * @param zoomLv 拡大レベル
   */
  public void setWorkspaceZoomLevel(Workspace ws, int zoomLv) {
    transporter.sendCmd(BhCmd.SET_ZOOM_LEVEL, new CmdData(zoomLv), ws);
  }

  /**
   * ワークスペースを広げる.
   *
   * @param ws 広げるワークスペース
   */
  public void extendWorkspace(Workspace ws) {
    transporter.sendCmd(BhCmd.CHANGE_WORKSPACE_VIEW_SIZE, new CmdData(true), ws);
  }

  /**
   * ワークスペースを狭める.
   *
   * @param ws 狭めるワークスペース
   */
  public void narrowWorkspace(Workspace ws) {
    transporter.sendCmd(BhCmd.CHANGE_WORKSPACE_VIEW_SIZE, new CmdData(false), ws);
  }

  /**
   * プロジェクトにワークスペースを追加する.
   *
   * @param ws プロジェクトに追加するワークスペース
   * @param userOpe undo 用コマンドオブジェクト
   */
  public void addWorkspace(Workspace ws, UserOperation userOpe) {
    transporter.sendCmd(BhCmd.ADD_WORKSPACE, new CmdData(userOpe), ws, wss);
  }

  /**
   * ロジェクトからワークスペースを削除する.
   *
   * @param ws プロジェクトから削除するワークスペース
   * @param userOpe undo 用コマンドオブジェクト
   */
  public void deleteWorkspace(Workspace ws, UserOperation userOpe) {
    transporter.sendCmd(BhCmd.DELETE_WORKSPACE, new CmdData(userOpe), ws, wss);
  }

  /**
   * ワークスペースを新しく作成し追加する.
   *
   * @param name ワークスペース名
   * @param width ワークスペース幅
   * @param height ワークスペース高さ
   * @param userOpe undo 用コマンドオブジェクト
   */
  public void addNewWorkspace(String name, double width, double height, UserOperation userOpe) {
    Workspace ws = new Workspace(name);
    WorkspaceView wsView;
    WorkspaceController wsController;
    try {
      wsView = new WorkspaceView(ws, width, height);
      wsController = new WorkspaceController(ws, wsView, new MultiNodeShifterView());
    } catch (ViewInitializationException e) {
      BhService.msgPrinter().errForDebug(e.toString());
      return;
    }
    ws.setMsgProcessor(wsController);
    transporter.sendCmd(BhCmd.ADD_WORKSPACE, new CmdData(userOpe), ws, wss);
    setWorkspaceZoomLevel(ws, BhConstants.LnF.INITIAL_ZOOM_LEVEL);
  }

  /** {@code node} のコントローラに {@link UserOperation} オブジェクトを渡す. */
  public void setUserOpeCmd(BhNode node, UserOperation userOpe) {
    transporter.sendCmd(BhCmd.SET_USER_OPE_CMD, new CmdData(userOpe), node);
  }
}
