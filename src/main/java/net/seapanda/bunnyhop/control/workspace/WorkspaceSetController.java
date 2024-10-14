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

package net.seapanda.bunnyhop.control.workspace;

import javafx.fxml.FXML;
import javafx.geometry.Point2D;
import javafx.scene.Node;
import javafx.scene.control.SplitPane;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;
import javafx.scene.control.TabPane.TabDragPolicy;
import javafx.scene.control.TextArea;
import javafx.scene.image.ImageView;
import javafx.scene.layout.StackPane;
import net.seapanda.bunnyhop.common.constant.BhConstants;
import net.seapanda.bunnyhop.common.tools.MsgPrinter;
import net.seapanda.bunnyhop.message.BhMsg;
import net.seapanda.bunnyhop.message.MsgData;
import net.seapanda.bunnyhop.message.MsgProcessor;
import net.seapanda.bunnyhop.model.workspace.Workspace;
import net.seapanda.bunnyhop.model.workspace.WorkspaceSet;
import net.seapanda.bunnyhop.root.BunnyHop;
import net.seapanda.bunnyhop.undo.UserOpeCmdManager;
import net.seapanda.bunnyhop.undo.UserOperationCommand;
import net.seapanda.bunnyhop.view.nodeselection.BhNodeSelectionView;
import net.seapanda.bunnyhop.view.workspace.WorkspaceView;

/**
 * ワークスペースセットのコントローラ.
 *
 * @author K.Koike
 */
public class WorkspaceSetController implements MsgProcessor {

  private WorkspaceSet model;
  @FXML private SplitPane workspaceSetViewBase;
  @FXML private StackPane workspaceSetStackPane;
  @FXML private TabPane workspaceSetTab;  //!< ワークスペース表示タブ
  @FXML private TextArea mainMsgArea;
  @FXML private ImageView openedTrashboxIv;
  @FXML private ImageView closedTrashboxIv;
  private final UserOpeCmdManager userOpeCmdManager = new UserOpeCmdManager();

  /**
   * モデルとイベントハンドラをセットする.
   *
   * @param wss ワークスペースセットのモデル
   */
  public void init(WorkspaceSet wss) {
    model = wss;
    setEventHandlers();
    workspaceSetViewBase.setDividerPositions(BhConstants.LnF.DEFAULT_VERTICAL_DIV_POS);
    MsgPrinter.INSTANCE.setMainMsgArea(mainMsgArea); //メインメッセージエリアの登録
  }

  /** イベントハンドラを登録する. */
  private void setEventHandlers() {
    setMessageAreaEvenHandlers();
    setResizeEventHandlers();
    workspaceSetTab.getSelectionModel().selectedItemProperty().addListener(
        (observable, oldTab, newTab) -> setCurrentWorkspace(newTab));
  }

  /** 現在走査中のワークスペースを設定する. */
  private void setCurrentWorkspace(Tab newTab) {
    if (newTab != null) {
      model.setCurrentWorkspace(((WorkspaceView) newTab).getWorkspace());
    } else {
      model.setCurrentWorkspace(null);
    }
  }

  /** ワークスペースリサイズ時におこるイベントハンドラを登録する. */
  private void setResizeEventHandlers() {
    //ワークスペースセットの大きさ変更時にノード選択ビューの高さを再計算する
    workspaceSetTab.heightProperty().addListener(
        (observable, oldValue, newValue) -> resizeNodeSelectionViewHeight(newValue.doubleValue()));

    // タブクローズイベントで TabDragPolicy.FIXED にするので, クリック時に再度 REORDER に変更する必要がある
    workspaceSetTab.setOnMousePressed(
        event ->   workspaceSetTab.setTabDragPolicy(TabDragPolicy.REORDER));
  }

  /**
   * タブペインの高さに合わせてノード選択ビューの高さを変更する.
   *
   * @param tabViewHeight タブペインの高さ
   */
  private void resizeNodeSelectionViewHeight(double tabViewHeight) {
    for (Node node : workspaceSetStackPane.getChildren()) {
      if (node instanceof BhNodeSelectionView) {
        ((BhNodeSelectionView) node).setMaxHeight(tabViewHeight - node.getTranslateY());
      }
    }
  }

  /** メッセージエリアのイベントハンドラを登録する. */
  private void setMessageAreaEvenHandlers() {
    mainMsgArea.textProperty().addListener((observable, oldVal, newVal) -> {
      if (newVal.length() > BhConstants.Message.MAX_MAIN_MSG_AREA_CHARS) {
        int numDeleteChars = newVal.length() - BhConstants.Message.MAX_MAIN_MSG_AREA_CHARS;
        mainMsgArea.deleteText(0, numDeleteChars);
      }
      mainMsgArea.setScrollTop(Double.MAX_VALUE);
    });

    mainMsgArea.scrollTopProperty().addListener((observable, oldVal, newVal) -> {
      if (oldVal.doubleValue() == Double.MAX_VALUE && newVal.doubleValue() == 0.0) {
        mainMsgArea.setScrollTop(Double.MAX_VALUE);
      }
    });
  }

  /**
   * ノード選択ビューを追加する.
   *
   * @param nodeSelectionView 表示するノードテンプレート
   */
  private void addNodeSelectionView(BhNodeSelectionView nodeSelectionView) {
    workspaceSetStackPane.getChildren().add(nodeSelectionView);
    nodeSelectionView.toFront();

    //タブの高さ分移動したときもノード選択ビューの高さを再計算する
    nodeSelectionView.translateYProperty().addListener((observable, oldValue, newValue) -> {
      nodeSelectionView.setMaxHeight(workspaceSetTab.getHeight() - newValue.doubleValue());
    });
  }

  /**
   * ワークスペース表示用のタブペインを返す.
   *
   * @return ワークスペース表示用のタブペイン
   */
  public TabPane getTabPane() {
    return workspaceSetTab;
  }

  /**
   * ゴミ箱を開閉する.
   *
   * @param open ゴミ箱を開ける場合true
   */
  private void openTrashBox(boolean open) {
    if (open) {
      openedTrashboxIv.setVisible(true);
      closedTrashboxIv.setVisible(false);
    } else {
      openedTrashboxIv.setVisible(false);
      closedTrashboxIv.setVisible(true);
    }
  }

  /**
   * 引数で指定した位置がゴミ箱エリアにあるかどうか調べる.
   *
   * @param sceneX シーン上でのX位置
   * @param sceneY シーン上でのY位置
   * @return 引数で指定した位置がゴミ箱エリアにある場合true
   */
  private boolean isPointInTrashBoxArea(double sceneX, double sceneY) {
    Point2D localPos = closedTrashboxIv.sceneToLocal(sceneX, sceneY);
    return closedTrashboxIv.contains(localPos.getX(), localPos.getY());
  }

  @Override
  public MsgData processMsg(BhMsg msg, MsgData data) {
    switch (msg) {
      case ADD_WORKSPACE:
        addWorkspace(data.workspace, data.workspaceView, data.userOpeCmd);
        break;

      case DELETE_WORKSPACE:
        deleteWorkspace(data.workspace, data.workspaceView, data.userOpeCmd);
        break;

      case ADD_NODE_SELECTION_PANEL:
        addNodeSelectionView(data.nodeSelectionView);
        break;

      case GET_CURRENT_WORKSPACE:
        return new MsgData(getCurrentWorkspace());

      case IS_IN_TRASHBOX_AREA:
        return new MsgData(isPointInTrashBoxArea(data.vec2d.x, data.vec2d.y));

      case OPEN_TRASHBOX:
        openTrashBox(data.bool);
        break;

      case UNDO:
        userOpeCmdManager.undo();
        BunnyHop.INSTANCE.shouldSave(true);
        break;

      case REDO:
        userOpeCmdManager.redo();
        BunnyHop.INSTANCE.shouldSave(true);
        break;

      case DELETE_USER_OPE_CMD:
        userOpeCmdManager.delete();
        break;

      case PUSH_USER_OPE_CMD:
        pushUserOpeCmd(data.userOpeCmd);
        break;

      case REMOVE_NODE_TO_PASTE:
        model.removeNodeFromCopyList(data.node, data.userOpeCmd);
        model.removeNodeFromCutList(data.node, data.userOpeCmd);
        break;

      default:
        throw new AssertionError("received an unknown msg " + msg);
    }

    return null;
  }

  /**
   * ワークスペースを追加する.
   *
   * @param workspace 追加するワークスペース
   * @param workspaceView workspace に対応するビュー
   * @param userOpeCmd undo 用コマンドオブジェクト
   */
  private void addWorkspace(
      Workspace workspace, WorkspaceView workspaceView, UserOperationCommand userOpeCmd) {
    model.addWorkspace(workspace);
    workspaceSetTab.getTabs().add(workspaceView);
    workspaceSetTab.getSelectionModel().select(workspaceView);
    // ここで REORDER にしないと, undo でタブを戻した時, タブドラッグ時に例外が発生する
    workspaceSetTab.setTabDragPolicy(TabDragPolicy.REORDER);
    userOpeCmd.pushCmdOfAddWorkspace(workspace, workspaceView, model);
  }

  /**
   * ワークスペースを削除する.
   *
   * @param workspace 削除するワークスペース
   * @param workspaceView workspace に対応するビュー
   * @param userOpeCmd undo 用コマンドオブジェクト
   */
  private void deleteWorkspace(
      Workspace workspace, WorkspaceView workspaceView, UserOperationCommand userOpeCmd) {
    model.removeWorkspace(workspace);
    workspaceSetTab.getTabs().remove(workspaceView);
    // ここで REORDER にしないと, タブを消した後でタブドラッグすると例外が発生する
    workspaceSetTab.setTabDragPolicy(TabDragPolicy.REORDER);
    userOpeCmd.pushCmdOfDeleteWorkspace(workspace, workspaceView, model);
  }

  /**
   * 現在選択中の Workspace を返す.
   *
   * @return 現在選択中のWorkspace
   * */
  private Workspace getCurrentWorkspace() {
    WorkspaceView newWorkspaceView =
        (WorkspaceView) workspaceSetTab.getSelectionModel().getSelectedItem();
    if (newWorkspaceView == null) {
      return null;
    }
    return newWorkspaceView.getWorkspace();
  }

  /**
   * 現在選択中の Workspace を返す.
   *
   * @return 現在選択中のWorkspace
   */
  public WorkspaceView getCurrentWorkspaceView() {
    return (WorkspaceView) workspaceSetTab.getSelectionModel().getSelectedItem();
  }

  /** 引数で指定した undo 用コマンドオブジェクトを undo スタックに追加する. */
  private void pushUserOpeCmd(UserOperationCommand userOpeCmd) {
    if (userOpeCmd.getNumSubOpe() > 0) {
      userOpeCmdManager.pushUndoCommand(userOpeCmd);
      BunnyHop.INSTANCE.shouldSave(true);
    }
  }
}
