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

package net.seapanda.bunnyhop.view.node.part;

import java.io.IOException;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;
import javafx.event.ActionEvent;
import javafx.scene.control.Button;
import net.seapanda.bunnyhop.common.BhConstants;
import net.seapanda.bunnyhop.model.node.BhNode;
import net.seapanda.bunnyhop.model.traverse.CallbackInvoker;
import net.seapanda.bunnyhop.model.traverse.CallbackInvoker.CallbackRegistry;
import net.seapanda.bunnyhop.model.traverse.NodeMvcBuilder;
import net.seapanda.bunnyhop.model.traverse.TextPrompter;
import net.seapanda.bunnyhop.service.BhService;
import net.seapanda.bunnyhop.service.ModelExclusiveControl;
import net.seapanda.bunnyhop.undo.UserOperation;

/**
 * ノード固有のテンプレートノードを作成するボタン.
 *
 * @author K.Koike
 */
public final class PrivateTemplateCreationButton extends Button {

  /** 最後にクリックされたプライベートテンプレート作成ボタン. */
  private static AtomicReference<PrivateTemplateCreationButton> lastClicked =
      new AtomicReference<>();

  /**
   * プライベートテンプレート作成ボタンを作成する.
   *
   * @param node ボタンを持つビューに対応するノード
   * @param buttonStyle ボタンに適用するスタイル
   */
  public static Optional<PrivateTemplateCreationButton> create(
      BhNode node, BhNodeViewStyle.Button buttonStyle) {
    try {
      return Optional.of(new PrivateTemplateCreationButton(node, buttonStyle));
    } catch (IOException | ClassCastException e) {
      BhService.msgPrinter().errForDebug(
          "Failed to create Private Template Creation Button.\n" + e);
      return Optional.empty();
    }
  }

  private PrivateTemplateCreationButton(BhNode node, BhNodeViewStyle.Button buttonStyle)
      throws IOException, ClassCastException {
    ComponentLoader.loadButton(BhConstants.Path.PRIVATE_TEMPLATE_BUTTON_FXML, this, buttonStyle);
    // setPrefHeight(20);
    setOnAction(event -> onTemplateCreating(event, node));
  }

  /**
   * ノード固有のテンプレート作成時のイベントハンドラ.
   *
   * @param event ボタン押下イベント
   * @param node このノードのプライベートテンプレートを作成する
   */
  private void onTemplateCreating(ActionEvent event, BhNode node) {
    if (BhService.cmdProxy().isTemplateNode(node)) {
      return;
    }
    ModelExclusiveControl.lockForModification();
    try {
      // 現在表示しているプライベートテンプレートを作ったボタンを押下した場合, プライベートテンプレートを閉じる
      boolean isPrivateTemplateShowed = BhService.bhNodeSelectionService().isShowed(
          BhConstants.NodeTemplate.PRIVATE_NODE_TEMPLATE);
      if (lastClicked.getAndSet(this) == this && isPrivateTemplateShowed) {
        BhService.bhNodeSelectionService().hideAll();
      } else {
        createPrivateTemplate(node);
        BhService.bhNodeSelectionService().show(BhConstants.NodeTemplate.PRIVATE_NODE_TEMPLATE);
      }
      event.consume();
    } finally {
      ModelExclusiveControl.unlockForModification();
    }
  }

  /**
   * 引数で指定したノード固有のテンプレートを作成する.
   *
   * @param model このノードのテンプレートノードを作成する.
   */
  private static void createPrivateTemplate(BhNode node) {
    var userOpe = new UserOperation();
    BhService.bhNodeSelectionService().deleteAllNodes(
        BhConstants.NodeTemplate.PRIVATE_NODE_TEMPLATE, userOpe);
    
    for (var templateNode : node.genPrivateTemplateNodes(userOpe)) {
      CallbackRegistry registry = CallbackInvoker.newCallbackRegistry()
          .setForAllNodes(bhNode -> bhNode.getEventAgent().execOnTemplateCreated(userOpe));
      CallbackInvoker.invoke(registry, templateNode);
      NodeMvcBuilder.buildTemplate(templateNode);
      TextPrompter.prompt(templateNode);
      BhService.bhNodeSelectionService().addTemplateNode(
          BhConstants.NodeTemplate.PRIVATE_NODE_TEMPLATE, templateNode, userOpe);
    }
    BhService.undoRedoAgent().pushUndoCommand(userOpe);
  }
}
