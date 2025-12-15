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

package net.seapanda.bunnyhop.node.control;

import java.util.LinkedHashSet;
import java.util.SequencedSet;
import javafx.event.ActionEvent;
import javafx.scene.control.Button;
import javafx.scene.input.MouseButton;
import javafx.scene.input.MouseEvent;
import net.seapanda.bunnyhop.common.configuration.BhConstants;
import net.seapanda.bunnyhop.node.model.BhNode;
import net.seapanda.bunnyhop.node.model.factory.BhNodeFactory.MvcType;
import net.seapanda.bunnyhop.node.model.service.BhNodePlacer;
import net.seapanda.bunnyhop.node.model.traverse.CallbackInvoker;
import net.seapanda.bunnyhop.nodeselection.view.BhNodeSelectionViewProxy;
import net.seapanda.bunnyhop.service.accesscontrol.ModelAccessNotificationService;
import net.seapanda.bunnyhop.service.accesscontrol.ModelAccessNotificationService.Context;
import net.seapanda.bunnyhop.service.undo.UserOperation;

/**
 * ノード固有のテンプレートノードを作成するボタンのコントローラ.
 *
 * @author K.Koike
 */
public class PrivateTemplateButtonController {
  
  /** 最後にテンプレートノードを作成したノード. */
  private static BhNode currentProducerNode = null;

  private final BhNode node;
  private final ModelAccessNotificationService service;
  private final BhNodeSelectionViewProxy proxy;

  /**
   * コンストラクタ.
   *
   * @param node このノードの固有のテンプレートノードを作成する
   * @param button 管理するビュー
   * @param service モデルへのアクセスの通知先となるオブジェクト
   * @param proxy ボタン押下時に操作するノード選択ビューのプロキシオブジェクト
   */
  public PrivateTemplateButtonController(
      BhNode node,
      Button button,
      ModelAccessNotificationService service,
      BhNodeSelectionViewProxy proxy) {
    this.node = node;
    this.service = service;
    this.proxy = proxy;

    node.getCallbackRegistry().getOnWorkspaceChanged().add(event -> {
      if (event.newWs() == null && isPrivateTemplateShowed() && currentProducerNode == node) {
        // ノード配置 -> 配置したノードのテンプレートノードを表示 -> undo という操作で,
        // 元のノードがワークスペースに存在しない状況で, テンプレートノードがワークスペースに配置できてしまうのを防ぐ.
        // ただし, テンプレートノードの削除は行わない.
        proxy.hideCurrentView();
      }
    });
    button.setOnAction(this::onTemplateCreating);
    button.addEventFilter(MouseEvent.ANY, this::consumeIfNotAcceptable);
  }

  /**
   * ノード固有のテンプレート作成時のイベントハンドラ.
   *
   * @param event ボタン押下イベント
   */
  private void onTemplateCreating(ActionEvent event) {
    if (isTemplate(node) || node.isDeleted()) {
      return;
    }
    if (currentProducerNode == node) {
      updateTemplateVisibility();
      return;
    }
    try {
      Context context = service.beginWrite();
      UserOperation userOpe = context.userOpe();
      deletePrivateTemplateNodes(userOpe);
      proxy.hideCurrentView();
      String categoryName = BhConstants.NodeSelection.PRIVATE_TEMPLATE_NODE;
      SequencedSet<BhNode> templateNodes = createPrivateTemplates(userOpe);
      for (BhNode templateNode : templateNodes) {
        proxy.addNodeTree(categoryName, templateNode, userOpe);
        // ノード選択ビューに追加してからイベントハンドラを呼ぶ
        CallbackInvoker.CallbackRegistry registry = CallbackInvoker.newCallbackRegistry()
            .setForAllNodes(bhNode -> bhNode.getEventInvoker().onCreatedAsTemplate(userOpe));
        CallbackInvoker.invoke(registry, templateNode);
      }
      proxy.show(BhConstants.NodeSelection.PRIVATE_TEMPLATE_NODE);
      setCurrentProducerNode(node, userOpe);
      event.consume();
    } finally {
      service.endWrite();
    }
  }

  /** ノード固有のテンプレートノードを表示するノード選択ビューの可視性を変更する. */
  private void updateTemplateVisibility() {
    if (isPrivateTemplateShowed()) {
      proxy.hideCurrentView();
    } else {
      proxy.show(BhConstants.NodeSelection.PRIVATE_TEMPLATE_NODE);
    }
  }

  /** 最後にノード固有のテンプレートノードを作成したノードを設定する. */
  private void setCurrentProducerNode(BhNode node, UserOperation userOpe) {
    BhNode oldCurrentProducerNode = currentProducerNode;
    currentProducerNode = node;
    userOpe.pushCmd(ope -> setCurrentProducerNode(oldCurrentProducerNode, ope));
  }

  /** {@link #node} のテンプレートを作成する. */
  private SequencedSet<BhNode> createPrivateTemplates(UserOperation userOpe) {
    return new LinkedHashSet<BhNode>(node.createCompanionNodes(MvcType.TEMPLATE, userOpe));
  }

  /** 現在ノード選択ビューに存在するノード固有のテンプレートノードを全て消す. */
  private void deletePrivateTemplateNodes(UserOperation userOpe) {
    String categoryName = BhConstants.NodeSelection.PRIVATE_TEMPLATE_NODE;
    proxy.getNodeTrees(categoryName).forEach(
        templateNode -> BhNodePlacer.deleteNode(templateNode, userOpe));
  }

  /** 受付不能なマウスイベントを consume する. */
  private void consumeIfNotAcceptable(MouseEvent event) {
    if (event.getButton() != MouseButton.PRIMARY) {
      event.consume();
    }
  }

  /** {@code node} がテンプレートノードか調べる. */
  private static boolean isTemplate(BhNode node) {
    while (node != null) {
      if (node.getView().isPresent()) {
        return node.getView().get().isTemplate();
      }
      node = node.findParentNode();
    }
    return false;
  }

  /** 現在表示されているノード選択ビューがノード固有のテンプレートを表示するためのものであるか調べる. */
  private boolean isPrivateTemplateShowed() {
    return proxy.getCurrentCategoryName()
        .map(BhConstants.NodeSelection.PRIVATE_TEMPLATE_NODE::equals)
        .orElse(false);
  }
}
