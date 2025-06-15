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


import java.util.Optional;
import java.util.function.Consumer;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ListView;
import javafx.scene.control.ToggleButton;
import javafx.scene.input.MouseButton;
import net.seapanda.bunnyhop.bhprogram.debugger.Debugger;
import net.seapanda.bunnyhop.common.BhConstants;
import net.seapanda.bunnyhop.model.node.BhNode;
import net.seapanda.bunnyhop.model.node.parameter.BreakpointSetting;
import net.seapanda.bunnyhop.model.workspace.WorkspaceSet;
import net.seapanda.bunnyhop.view.node.BhNodeView;

/**
 * ブレークポイントを表示する UI コンポーネントのコントローラ.
 *
 * @author K.Koike
 */
public class BreakpointListController {

  @FXML private ListView<String> bpListView;
  @FXML private WorkspaceSelectorController bpWsSelectorController;
  @FXML private Button bpSearchButton;
  @FXML private CheckBox bpJumpCheckBox;
  private ToggleButton breakpointBtn;

  /** マウスボタンが押されたときのイベントハンドラ. */
  private Consumer<BhNodeView.MouseEventInfo> onMousePressed = this::toggleBreakpoint;

  /** 初期化する. */
  public void initialize(WorkspaceSet wss, Debugger debugger) {
    bpWsSelectorController.initialize(wss);
    wss.getCallbackRegistry().getOnNodeAdded().add(
        event -> event.node().getView().ifPresent(this::addEventHandlerTo));
    wss.getCallbackRegistry().getOnNodeRemoved().add(
        event -> event.node().getView().ifPresent(this::removeEventHandlerFrom));
  }

  /** ブレークポイントの設定が有効かどうか調べる. */
  private boolean isBreakpointSettingEnabled() {
    if (breakpointBtn == null) {
      breakpointBtn =
          (ToggleButton) bpListView.getScene().lookup("#" + BhConstants.UiId.BREAKPOINT_BTN);
    }
    bpListView.getItems().addLast(null);
    return breakpointBtn.isSelected();
  }

  /** ノードビューにイベントハンドラを設定する. */
  private void addEventHandlerTo(BhNodeView view) {
    view.getCallbackRegistry().getOnMousePressed().add(onMousePressed);
  }

  /** ノードビューからイベントハンドラを削除する. */
  private void removeEventHandlerFrom(BhNodeView view) {
    view.getCallbackRegistry().getOnMousePressed().remove(onMousePressed);
  }

  /** ブレークポイントの有効 / 無効を切り替える. */
  private void toggleBreakpoint(BhNodeView.MouseEventInfo info) {
    if (isBreakpointSettingEnabled()
        && info.src() == null
        && info.event().getButton() == MouseButton.PRIMARY) {
      info.view().getModel()
          .flatMap(BreakpointListController::findNodeToSetBreakpointTo)
          .flatMap(BhNode::getView)
          .ifPresent(view -> view.getLookManager().setBreakpointVisibility(
              !view.getLookManager().isBreakpointVisible()));
    }
  }

  private static Optional<BhNode> findNodeToSetBreakpointTo(BhNode node) {
    if (node == null) {
      return Optional.empty();
    }
    while (true) {
      if (node.getBreakpointSetting() == BreakpointSetting.SET) {
        return Optional.of(node);
      }
      if (node.getBreakpointSetting() == BreakpointSetting.IGNORE) {
        return Optional.empty();
      }
      if (node.getBreakpointSetting() == BreakpointSetting.SPECIFY_PARENT) {
        return findNodeToSetBreakpointTo(node.findParentNode());
      }
      return Optional.empty();
    }
  }
}
