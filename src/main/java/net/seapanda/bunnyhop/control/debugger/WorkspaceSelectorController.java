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

package net.seapanda.bunnyhop.control.debugger;

import java.util.ArrayList;
import javafx.fxml.FXML;
import javafx.scene.control.ComboBox;
import javafx.scene.control.ListCell;
import net.seapanda.bunnyhop.common.TextDefs;
import net.seapanda.bunnyhop.model.workspace.Workspace;
import net.seapanda.bunnyhop.model.workspace.WorkspaceSet;
import net.seapanda.bunnyhop.view.ViewUtil;

/**
 * ワークスペース選択コンポーネントのコントローラ.
 *
 * @author K.Koike
 */
public class WorkspaceSelectorController {

  @FXML private ComboBox<Workspace> wsComboBox;
  /** 全ワークスペースを表す選択肢に対応する {@link Workspace}. */
  private final Workspace allWs;

  /** コンストラクタ. */
  public WorkspaceSelectorController() {
    String allWsName = TextDefs.Debugger.WorkspaceSelector.allWs.get();
    allWs = new Workspace(allWsName);
  }

  /** 初期化する. */
  public void initialize(WorkspaceSet wss) {
    wsComboBox.setButtonCell(new WorkspaceSelectorListCell());
    wsComboBox.setCellFactory(items -> new WorkspaceSelectorListCell());
    wsComboBox.getItems().add(allWs);
    wsComboBox.getSelectionModel().selectFirst();
    ViewUtil.enableAutoResize(wsComboBox, Workspace::getName);
    wss.getWorkspaces().forEach(wsComboBox.getItems()::add);    
    wss.getCallbackRegistry().getOnWorkspaceAdded().add(
        event -> wsComboBox.getItems().add(event.ws()));
    wss.getCallbackRegistry().getOnWorkspaceRemoved().add(
        event -> wsComboBox.getItems().remove(event.ws()));
    wss.getCallbackRegistry().getOnWorkspaceNameChanged().add(event -> reregisterItems());
  }

  /** 現在 {@link #wsComboBox} が持つアイテムを再度設定する. */
  private void reregisterItems() {
    Workspace selected = wsComboBox.getValue();
    var wslist = new ArrayList<>(wsComboBox.getItems());
    wsComboBox.getItems().clear();
    wsComboBox.getItems().addAll(wslist);
    wsComboBox.setValue(selected);
  }

  private class WorkspaceSelectorListCell extends ListCell<Workspace> {

    @Override
    protected void updateItem(Workspace item, boolean empty) {
      super.updateItem(item, empty);
      if (!empty && item != null) {
        setText(item.getName());
      } else {
        setText(null);
      }
    }
  }    
}
