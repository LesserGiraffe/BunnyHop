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
import java.util.concurrent.atomic.AtomicReference;
import javafx.event.ActionEvent;
import javafx.scene.control.Button;
import javafx.scene.input.MouseButton;
import javafx.scene.input.MouseEvent;
import net.seapanda.bunnyhop.common.configuration.BhConstants;
import net.seapanda.bunnyhop.node.model.BhNode;
import net.seapanda.bunnyhop.node.model.factory.BhNodeFactory.MvcType;
import net.seapanda.bunnyhop.node.model.traverse.CallbackInvoker;
import net.seapanda.bunnyhop.node.model.traverse.CallbackInvoker.CallbackRegistry;
import net.seapanda.bunnyhop.node.service.BhNodePlacer;
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
  
  /** 最後にプライベートテンプレートを作成したノード. */
  private static final AtomicReference<BhNode> lastClicked = new AtomicReference<>();

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
      if (event.newWs() == null) {
        // 変数ノード配置 -> プライベートテンプレート表示 -> undo  で, 
        // オリジナルノードがワークスペースに存在しない状況で, 派生ノードをワークスペースに配置できてしまうのを防ぐ.
        // ただし, プライベートテンプレートノードの削除は行わない.
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
    try {
      Context context = service.beginWrite();
      boolean isPrivateTemplateShowed = proxy.getCurrentCategoryName()
          .map(BhConstants.NodeTemplate.PRIVATE_NODE_TEMPLATE::equals)
          .orElse(false);
      // 現在表示しているプライベートテンプレートを作ったボタンを押下した場合, プライベートテンプレートを閉じる
      if (lastClicked.getAndSet(node) == node && isPrivateTemplateShowed) {
        proxy.hideCurrentView();
      } else {
        UserOperation userOpe = context.userOpe();
        deletePrivateTemplateNodes(userOpe);
        proxy.hideCurrentView();
        String categoryName = BhConstants.NodeTemplate.PRIVATE_NODE_TEMPLATE;
        SequencedSet<BhNode> templateNodes = createPrivateTemplates(userOpe);
        for (BhNode templateNode : templateNodes) {
          proxy.addNodeTree(categoryName, templateNode, userOpe);
          // ノード選択ビューに追加してからイベントハンドラを呼ぶ
          CallbackRegistry registry = CallbackInvoker.newCallbackRegistry()
              .setForAllNodes(bhNode -> bhNode.getEventInvoker().onCreatedAsTemplate(userOpe));
          CallbackInvoker.invoke(registry, templateNode);
        }
        proxy.show(categoryName);
      }
      event.consume();
    } finally {
      service.endWrite();
    }
  }

  /** {@link #node} のテンプレートを作成する. */
  private SequencedSet<BhNode> createPrivateTemplates(UserOperation userOpe) {
    return new LinkedHashSet<BhNode>(node.createCompanionNodes(MvcType.TEMPLATE, userOpe));
  }

  /** プライベートテンプレートノードを全て消す. */
  private void deletePrivateTemplateNodes(UserOperation userOpe) {
    String categoryName = BhConstants.NodeTemplate.PRIVATE_NODE_TEMPLATE;
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
}
