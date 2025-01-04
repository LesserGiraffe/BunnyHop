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
import java.util.LinkedHashSet;
import java.util.Optional;
import java.util.SequencedSet;
import java.util.concurrent.atomic.AtomicReference;
import javafx.event.ActionEvent;
import javafx.scene.control.Button;
import javafx.scene.input.MouseButton;
import javafx.scene.input.MouseEvent;
import net.seapanda.bunnyhop.common.BhConstants;
import net.seapanda.bunnyhop.model.node.BhNode;
import net.seapanda.bunnyhop.model.traverse.CallbackInvoker;
import net.seapanda.bunnyhop.model.traverse.CallbackInvoker.CallbackRegistry;
import net.seapanda.bunnyhop.model.traverse.NodeMvcBuilder;
import net.seapanda.bunnyhop.service.BhService;
import net.seapanda.bunnyhop.service.ModelExclusiveControl;
import net.seapanda.bunnyhop.undo.UserOperation;
import net.seapanda.bunnyhop.view.proxy.BhNodeSelectionViewProxy;

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
      node.getEventManager().addOnWorkspaceChanged((oldWs, newWs, userOpe) -> {
        if (newWs == null) {
          // 変数ノード配置 -> プライベートテンプレート表示 -> undo  で, 
          // オリジナルノードがワークスペースに存在しない状況で, 派生ノードをワークスペースに配置できてしまうのを防ぐ.
          // ただし, プライベートテンプレートノードの削除は行わない.
          BhService.appRoot().getNodeSelectionViewProxy().hideAll();
        }
      });

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
    addEventFilter(MouseEvent.ANY, this::consumeIfNotAcceptable);
  }

  /**
   * ノード固有のテンプレート作成時のイベントハンドラ.
   *
   * @param event ボタン押下イベント
   * @param node このノードのプライベートテンプレートを作成する
   */
  private void onTemplateCreating(ActionEvent event, BhNode node) {
    if (node.getViewProxy().isTemplateNode() || node.isDeleted()) {
      return;
    }
    ModelExclusiveControl.lockForModification();
    try {
      // 現在表示しているプライベートテンプレートを作ったボタンを押下した場合, プライベートテンプレートを閉じる
      BhNodeSelectionViewProxy selectionViewProxy = BhService.appRoot().getNodeSelectionViewProxy();
      if (lastClicked.getAndSet(this) == this
          && selectionViewProxy.isShowed(BhConstants.NodeTemplate.PRIVATE_NODE_TEMPLATE)) {
        selectionViewProxy.hideAll();
      } else {
        UserOperation userOpe = new UserOperation();
        deletePrivateTemplateNodes(userOpe);
        selectionViewProxy.hideAll();
        String categoryName = BhConstants.NodeTemplate.PRIVATE_NODE_TEMPLATE;
        SequencedSet<BhNode> templateNodes = createPrivateTemplates(node, userOpe);        
        for (BhNode templateNode : templateNodes) {
          selectionViewProxy.addNodeTree(categoryName, templateNode, userOpe);
          // ノード選択ビューに追加してからフック処理を呼ぶ
          CallbackRegistry registry = CallbackInvoker.newCallbackRegistry()
              .setForAllNodes(bhNode -> bhNode.getHookAgent().execOnTemplateCreated(userOpe));
          CallbackInvoker.invoke(registry, templateNode);
        }
        selectionViewProxy.show(categoryName);
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
  private static SequencedSet<BhNode> createPrivateTemplates(BhNode node, UserOperation userOpe) {
    var privateTempladeNodes = new LinkedHashSet<BhNode>();
    for (var templateNode : node.genPrivateTemplateNodes(userOpe)) {
      NodeMvcBuilder.buildTemplate(templateNode);
      privateTempladeNodes.add(templateNode);
    }
    return privateTempladeNodes;
  }

  /** プライベートテンプレートノードを全て消す. */
  private static void deletePrivateTemplateNodes(UserOperation userOpe) {
    BhNodeSelectionViewProxy selectionViewProxy = BhService.appRoot().getNodeSelectionViewProxy();
    String categoryName = BhConstants.NodeTemplate.PRIVATE_NODE_TEMPLATE;
    selectionViewProxy.getNodeTrees(categoryName).forEach(
        templateNode -> BhService.bhNodePlacer().deleteNode(templateNode, userOpe));
  }


  /** 受付不能なマウスイベントを consume する. */
  private void consumeIfNotAcceptable(MouseEvent event) {
    MouseButton button = event.getButton();
    if (button != MouseButton.PRIMARY) {
      event.consume();
    }
  }
}
