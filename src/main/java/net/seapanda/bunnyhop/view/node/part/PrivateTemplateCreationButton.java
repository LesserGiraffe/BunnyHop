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
import net.seapanda.bunnyhop.common.constant.BhConstants;
import net.seapanda.bunnyhop.common.tools.MsgPrinter;
import net.seapanda.bunnyhop.common.tools.Util;
import net.seapanda.bunnyhop.message.MsgService;
import net.seapanda.bunnyhop.model.node.BhNode;
import net.seapanda.bunnyhop.modelprocessor.NodeMvcBuilder;
import net.seapanda.bunnyhop.modelprocessor.TextPrompter;
import net.seapanda.bunnyhop.modelservice.ModelExclusiveControl;
import net.seapanda.bunnyhop.root.BunnyHop;
import net.seapanda.bunnyhop.undo.UserOperation;
import net.seapanda.bunnyhop.view.nodeselection.BhNodeSelectionService;

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
      MsgPrinter.INSTANCE.errMsgForDebug(
          Util.INSTANCE.getCurrentMethodName() 
          + " - failed to create a PrivateTemplateCreationButton\n" + e);
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
    if (MsgService.INSTANCE.isTemplateNode(node)) {
      return;
    }
    ModelExclusiveControl.INSTANCE.lockForModification();
    try {
      // 現在表示しているプライベートテンプレートを作ったボタンを押下した場合, プライベートテンプレートを閉じる
      boolean isPrivateTemplateShowed =
          BhNodeSelectionService.INSTANCE.isShowed(BhConstants.NodeTemplate.PRIVATE_NODE_TEMPLATE);
      if (lastClicked.getAndSet(this) == this && isPrivateTemplateShowed) {
        BhNodeSelectionService.INSTANCE.hideAll();
      } else {
        createPrivateTemplate(node);
        BhNodeSelectionService.INSTANCE.show(BhConstants.NodeTemplate.PRIVATE_NODE_TEMPLATE);
      }
      event.consume();
    } finally {
      ModelExclusiveControl.INSTANCE.unlockForModification();
    }
  }

  /**
   * 引数で指定したノード固有のテンプレートを作成する.
   *
   * @param model このノードのテンプレートノードを作成する.
   */
  private static void createPrivateTemplate(BhNode node) {
    var userOpe = new UserOperation();
    BhNodeSelectionService.INSTANCE.deleteAllNodes(
        BhConstants.NodeTemplate.PRIVATE_NODE_TEMPLATE, userOpe);
    
    for (var templateNode : node.genPrivateTemplateNodes(userOpe)) {
      NodeMvcBuilder.buildTemplate(templateNode);
      TextPrompter.prompt(templateNode);
      BhNodeSelectionService.INSTANCE.addTemplateNode(
          BhConstants.NodeTemplate.PRIVATE_NODE_TEMPLATE, templateNode, userOpe);
    }
    BunnyHop.INSTANCE.pushUserOperation(userOpe);
  }
}
