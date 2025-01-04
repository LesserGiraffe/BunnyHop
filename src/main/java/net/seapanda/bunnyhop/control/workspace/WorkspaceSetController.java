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
import javafx.scene.Node;
import javafx.scene.control.SplitPane;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;
import javafx.scene.control.TabPane.TabDragPolicy;
import javafx.scene.control.TextArea;
import javafx.scene.layout.Pane;
import javafx.scene.layout.StackPane;
import net.seapanda.bunnyhop.common.BhConstants;
import net.seapanda.bunnyhop.control.DebugBoardController;
import net.seapanda.bunnyhop.model.workspace.Workspace;
import net.seapanda.bunnyhop.model.workspace.WorkspaceSet;
import net.seapanda.bunnyhop.view.nodeselection.BhNodeSelectionView;
import net.seapanda.bunnyhop.view.workspace.WorkspaceView;

/**
 * ワークスペースセットのコントローラ.
 *
 * @author K.Koike
 */
public class WorkspaceSetController {

  private WorkspaceSet model;
  @FXML private SplitPane workspaceSetViewBase;
  @FXML private StackPane workspaceSetStackPane;
  /** ワークスペース表示タブ. */
  @FXML private TabPane workspaceSetTab;
  @FXML private Pane trashbox;
  @FXML private TextArea mainMsgArea;
  /** ノード削除用のゴミ箱のコントローラ. */
  @FXML private TrashboxController trashboxController;
  @FXML private DebugBoardController debugBoardController;

  /**
   * モデルとイベントハンドラをセットする.
   *
   * @param wss ワークスペースセットのモデル
   */
  public void initialize(WorkspaceSet wss) {
    model = wss;
    setEventHandlers();
    workspaceSetViewBase.setDividerPositions(BhConstants.LnF.DEFAULT_VERTICAL_DIV_POS);
  }

  /** イベントハンドラを登録する. */
  private void setEventHandlers() {
    setMessageAreaEvenHandlers();
    setTabPaneEventHandlers();
    model.getEventManager().addOnWorkspaceAdded((wss, ws, userOpe) -> addWorkspaceView(ws));
    model.getEventManager().addOnWorkspaceRemoved((wss, ws, userOpe) -> removeWorkspaceView(ws));
  }

  /** 現在選択中のワークスペースを設定する. */
  private void setCurrentWorkspace(Tab newTab) {
    if (newTab instanceof WorkspaceView wsView) {
      model.setCurrentWorkspace(wsView.getWorkspace());
    } else {
      model.setCurrentWorkspace(null);
    }
  }

  /** ワークスペースを表示するタブペインに関連するイベントハンドラを登録する. */
  private void setTabPaneEventHandlers() {
    //ワークスペースセットの大きさ変更時にノード選択ビューの高さを再計算する
    workspaceSetTab.heightProperty().addListener(
        (observable, oldValue, newValue) -> resizeNodeSelectionViewHeight(newValue.doubleValue()));

    // タブクローズイベントで TabDragPolicy.FIXED にするので, クリック時に再度 REORDER に変更する必要がある
    workspaceSetTab.setOnMousePressed(
        event -> workspaceSetTab.setTabDragPolicy(TabDragPolicy.REORDER));
    
    workspaceSetTab.getSelectionModel().selectedItemProperty().addListener(
        (observable, oldTab, newTab) -> setCurrentWorkspace(newTab));
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
  public void addNodeSelectionView(BhNodeSelectionView nodeSelectionView) {
    workspaceSetStackPane.getChildren().add(nodeSelectionView);
    nodeSelectionView.toFront();

    //タブの高さ分移動したときもノード選択ビューの高さを再計算する
    nodeSelectionView.translateYProperty().addListener((observable, oldValue, newValue) -> {
      nodeSelectionView.setMaxHeight(workspaceSetTab.getHeight() - newValue.doubleValue());
    });
    workspaceSetTab.widthProperty().addListener((oldval, newval, obs) ->
        Math.min(nodeSelectionView.getMaxWidth(), workspaceSetTab.getWidth() * 0.5));
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
   * 現在操作対象となっている Workspace を返す.
   *
   * @return 現在操作対象となっている Workspace
   */
  public Workspace getCurrentWorkspace() {
    WorkspaceView newWorkspaceView =
        (WorkspaceView) workspaceSetTab.getSelectionModel().getSelectedItem();
    if (newWorkspaceView == null) {
      return null;
    }
    return newWorkspaceView.getWorkspace();
  }

  /** アプリケーションのメッセージを表示する TextArea を取得する. */
  public TextArea getMsgArea() {
    return mainMsgArea;
  }

  /** ノード削除用のゴミ箱のコントローラオブジェクトを取得する. */
  public TrashboxController getTrashboxController() {
    return trashboxController;
  }

  /** {@code ws} のワークスペースビューをワークスペースセットのビューに追加する. */
  private void addWorkspaceView(Workspace ws) {
    WorkspaceView wsView = ws.getViewProxy().getView();
    if (wsView == null) {
      return;
    }
    workspaceSetTab.getTabs().add(wsView);
    workspaceSetTab.getSelectionModel().select(wsView);
    // ここで REORDER にしないと, undo でタブを戻した時, タブドラッグ時に例外が発生する
    workspaceSetTab.setTabDragPolicy(TabDragPolicy.REORDER);
    wsView.addOnMousePressed(event -> debugBoardController.removeMarksIn(ws));
  }

  /** {@code ws} のワークスペースビューをワークスペースセットのビューから削除する. */
  private void removeWorkspaceView(Workspace ws) {
    WorkspaceView wsView = ws.getViewProxy().getView();
    if (wsView == null) {
      return;
    }
    workspaceSetTab.getTabs().remove(wsView);
    // ここで REORDER にしないと, タブを消した後でタブドラッグすると例外が発生する
    workspaceSetTab.setTabDragPolicy(TabDragPolicy.REORDER);
  }
}
