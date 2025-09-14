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
import java.util.Optional;
import java.util.function.Consumer;
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

  private final WorkspaceSet wss;
  /** 全ワークスペースを表す選択肢に対応する {@link Workspace}. */
  private final Workspace wsForAll;
  /** スレッドが選択されたときのイベントハンドラ. */
  private Consumer<? super WorkspaceSelectionEvent> onWsSelected = event -> {};

  /** コンストラクタ. */
  public WorkspaceSelectorController(WorkspaceSet wss) {
    this.wss = wss;
    wsForAll = new Workspace(TextDefs.Debugger.WorkspaceSelector.allWs.get());
  }

  /** {@link #wsComboBox} が持つアイテムを再設定する. */
  private void reregisterItems() {
    Workspace selected = wsComboBox.getValue();
    var wsList = new ArrayList<>(wsComboBox.getItems());
    wsComboBox.getItems().setAll(wsList);
    wsComboBox.setValue(selected);
  }

  /** このコントローラの UI 要素を初期化する. */
  @FXML
  public void initialize() {
    setEventHandlers();
    wss.getWorkspaces().forEach(wsComboBox.getItems()::add);
    ViewUtil.enableAutoResize(wsComboBox, Workspace::getName);
  }

  /** イベントハンドラを設定する. */
  private void setEventHandlers() {
    wss.getCallbackRegistry().getOnWorkspaceAdded()
        .add(event -> wsComboBox.getItems().add(event.ws()));
    wss.getCallbackRegistry().getOnWorkspaceRemoved()
        .add(event -> wsComboBox.getItems().remove(event.ws()));
    wss.getCallbackRegistry().getOnWorkspaceNameChanged().add(event -> reregisterItems());
    wsComboBox.setButtonCell(new WorkspaceSelectorListCell());
    wsComboBox.setCellFactory(items -> new WorkspaceSelectorListCell());
    wsComboBox.getItems().add(wsForAll);
    wsComboBox.getSelectionModel().selectFirst();
    wsComboBox.valueProperty().addListener((obs, oldVal, newVal) ->
        onWsSelected.accept(new WorkspaceSelectionEvent(oldVal, newVal, newVal == wsForAll)));
  }

  /**
   * 現在選択中のワークスペースを返す. 
   *
   * @return 現在選択中のワークスペース. 特定のワークスペースが選択されていない場合は Optional.empty.
   */
  synchronized Optional<Workspace> getSelected() {
    Workspace selected = wsComboBox.getValue();
    if (selected == wsForAll) {
      return Optional.empty();
    }
    return Optional.ofNullable(selected);
  }

  /** 「すべてのワークスペース」が選択されている場合 true を返す. */
  synchronized boolean isAllSelected() {
    return wsComboBox.getValue() == wsForAll;
  }

  /**
   * スレッドが選択されたときのイベントハンドラを登録する.
   *
   * @param handler 登録するイベントハンドラ.
   */
  void setOnWorkspaceSelected(Consumer<? super WorkspaceSelectionEvent> handler) {
    if (handler != null) {
      onWsSelected = handler;
    }
  }

  private static class WorkspaceSelectorListCell extends ListCell<Workspace> {

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
  
  /**
   * ワークスペースが選択されたときの情報を格納したレコード.
   *
   * @param oldWs {@code newWs} の前に選択されていたワークスペース.
   * @param newWs 新しく選択されたワークスペース.
   * @param isAllSelected 「すべてのワークスペース」が選択された場合 true
   */
  public record WorkspaceSelectionEvent(Workspace oldWs, Workspace newWs, boolean isAllSelected) {}
}
