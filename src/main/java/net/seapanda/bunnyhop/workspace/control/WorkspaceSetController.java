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

import java.util.Optional;
import javafx.fxml.FXML;
import javafx.scene.Node;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;
import javafx.scene.control.TabPane.TabDragPolicy;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import net.seapanda.bunnyhop.nodeselection.view.BhNodeSelectionView;
import net.seapanda.bunnyhop.service.accesscontrol.ModelAccessNotificationService;
import net.seapanda.bunnyhop.service.accesscontrol.ModelAccessNotificationService.Context;
import net.seapanda.bunnyhop.service.undo.UserOperation;
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
  private final ModelAccessNotificationService notifService;

  /** コンストラクタ. */
  public WorkspaceSetController(WorkspaceSet wss, ModelAccessNotificationService notifService) {
    this.wss = wss;
    this.notifService = notifService;
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
    registry.getOnCurrentWorkspaceChanged().add(event -> selectTabOf(event.newWs()));
  }

  /** ワークスペースを表示するタブペインに関連するイベントハンドラを登録する. */
  private void setTabPaneEventHandlers() {
    //ワークスペースセットの大きさ変更時にノード選択ビューの高さを再計算する
    workspaceSetTab.heightProperty().addListener(
        (obs, oldValue, newValue) -> resizeNodeSelectionViewHeight(newValue.doubleValue()));

    // タブクローズイベントで TabDragPolicy.FIXED にするので, クリック時に再度 REORDER に変更する必要がある
    workspaceSetTab.setOnMousePressed(
        event -> workspaceSetTab.setTabDragPolicy(TabDragPolicy.REORDER));
    
    workspaceSetTab.getSelectionModel().selectedItemProperty().addListener(
        (obs, oldTab, newTab) -> onTabSelected(newTab));
  }

  /** 新しくタブが選択されたときの処理. */
  private void onTabSelected(Tab newTab) {
    Context context = notifService.beginWrite();
    try {
      setCurrentWorkspace(newTab, context.userOpe());
    } finally {
      notifService.endWrite();
    }
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

  /** 現在選択中のワークスペースを設定する. */
  private void setCurrentWorkspace(Tab tab, UserOperation userOpe) {
    if (tab instanceof WorkspaceView wsView) {
      wss.setCurrentWorkspace(wsView.getWorkspace(), userOpe);
    } else {
      wss.setCurrentWorkspace(null, userOpe);
    }
  }

  /** {@code ws} に対応する {@link Tab} オブジェクトを選択する. */
  private void selectTabOf(Workspace workspace) {
    Optional.ofNullable(workspace)
        .flatMap(Workspace::getView)
        .map(wsView -> (wsView instanceof Tab tab) ? tab : null)
        .ifPresent(tab -> {
          if (workspaceSetTab.getTabs().contains(tab)) {
            workspaceSetTab.getSelectionModel().select(tab);
          }
        });
  }
}
