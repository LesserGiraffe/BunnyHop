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

package net.seapanda.bunnyhop.workspace.control;

import javafx.fxml.FXML;
import javafx.scene.Node;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;
import javafx.scene.control.TabPane.TabDragPolicy;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import net.seapanda.bunnyhop.nodeselection.view.BhNodeSelectionView;
import net.seapanda.bunnyhop.workspace.model.Workspace;
import net.seapanda.bunnyhop.workspace.model.WorkspaceSet;
import net.seapanda.bunnyhop.workspace.view.WorkspaceView;

/**
 * ワークスペースセットのコントローラ.
 *
 * @author K.Koike
 */
public class WorkspaceSetController {

  @FXML private StackPane workspaceSetViewBase;
  @FXML private TabPane workspaceSetTab;

  private final WorkspaceSet wss;

  /** コンストラクタ. */
  public WorkspaceSetController(WorkspaceSet wss) {
    this.wss = wss;
  }

  /** このコントローラを初期化する. */
  @FXML
  public void initialize() {
    setEventHandlers();
  }

  /** イベントハンドラを登録する. */
  private void setEventHandlers() {
    setTabPaneEventHandlers();
    WorkspaceSet.CallbackRegistry registry = wss.getCallbackRegistry();
    registry.getOnWorkspaceAdded().add(event -> addWorkspaceView(event.ws()));
    registry.getOnWorkspaceRemoved().add(event -> removeWorkspaceView(event.ws()));
  }

  /** 現在選択中のワークスペースを設定する. */
  private void setCurrentWorkspace(Tab newTab) {
    if (newTab instanceof WorkspaceView wsView) {
      wss.setCurrentWorkspace(wsView.getWorkspace());
    } else {
      wss.setCurrentWorkspace(null);
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
    for (Node node : workspaceSetViewBase.getChildren()) {
      if (node instanceof BhNodeSelectionView view) {
        view.getRegion().setMaxHeight(tabViewHeight - node.getTranslateY());
      }
    }
  }

  /**
   * ノード選択ビューを追加する.
   *
   * @param view 追加するノード選択ビュー
   */
  public void addNodeSelectionView(BhNodeSelectionView view) {
    Region region = view.getRegion();
    workspaceSetViewBase.getChildren().add(region);
    region.toFront();
    //タブの高さ分移動したときもノード選択ビューの高さを再計算する
    region.translateYProperty().addListener((observable, oldValue, newValue) -> 
        region.setMaxHeight(workspaceSetTab.getHeight() - newValue.doubleValue()));
  }

  /**
   * ワークスペース表示用のタブペインを返す.
   *
   * @return ワークスペース表示用のタブペイン
   */
  public TabPane getTabPane() {
    return workspaceSetTab;
  }

  /** このコントローラが管理する {@link WorkspaceSet} オブジェクトを返す. */
  public WorkspaceSet getWorkspaceSet() {
    return wss;
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

  /** {@code ws} のワークスペースビューをワークスペースセットのビューに追加する. */
  private void addWorkspaceView(Workspace ws) {
    if (ws.getView().orElse(null) instanceof Tab tab) {
      workspaceSetTab.getTabs().add(tab);
      workspaceSetTab.getSelectionModel().select(tab);
      // ここで REORDER にしないと, undo でタブを戻した時, タブドラッグ時に例外が発生する
      workspaceSetTab.setTabDragPolicy(TabDragPolicy.REORDER);  
    }
  }

  /** {@code ws} のワークスペースビューをワークスペースセットのビューから削除する. */
  private void removeWorkspaceView(Workspace ws) {
    if (ws.getView().orElse(null) instanceof Tab tab) {
      workspaceSetTab.getTabs().remove(tab);
      // ここで REORDER にしないと, タブを消した後でタブドラッグすると例外が発生する
      workspaceSetTab.setTabDragPolicy(TabDragPolicy.REORDER);
    }
  }
}
