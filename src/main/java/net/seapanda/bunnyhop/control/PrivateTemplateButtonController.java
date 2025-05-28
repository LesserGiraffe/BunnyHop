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

package net.seapanda.bunnyhop.control;

import java.util.LinkedHashSet;
import java.util.SequencedSet;
import java.util.concurrent.atomic.AtomicReference;
import javafx.event.ActionEvent;
import javafx.scene.control.Button;
import javafx.scene.input.MouseButton;
import javafx.scene.input.MouseEvent;
import net.seapanda.bunnyhop.common.BhConstants;
import net.seapanda.bunnyhop.model.BhNodePlacer;
import net.seapanda.bunnyhop.model.ModelAccessNotificationService;
import net.seapanda.bunnyhop.model.ModelAccessNotificationService.Context;
import net.seapanda.bunnyhop.model.factory.BhNodeFactory.MvcType;
import net.seapanda.bunnyhop.model.node.BhNode;
import net.seapanda.bunnyhop.model.traverse.CallbackInvoker;
import net.seapanda.bunnyhop.model.traverse.CallbackInvoker.CallbackRegistry;
import net.seapanda.bunnyhop.undo.UserOperation;
import net.seapanda.bunnyhop.view.nodeselection.BhNodeSelectionViewProxy;

/**
 * ノード固有のテンプレートノードを作成するボタンのコントローラ.
 *
 * @author K.Koike
 */
public class PrivateTemplateButtonController {
  
  /** 最後にプライベートテンプレートを作成したノード. */
  private static AtomicReference<BhNode> lastClicked = new AtomicReference<>();

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

    node.getEventManager().addOnWorkspaceChanged((bhNode, oldWs, newWs, userOpe) -> {
      if (newWs == null) {
        // 変数ノード配置 -> プライベートテンプレート表示 -> undo  で, 
        // オリジナルノードがワークスペースに存在しない状況で, 派生ノードをワークスペースに配置できてしまうのを防ぐ.
        // ただし, プライベートテンプレートノードの削除は行わない.
        proxy.hideAll();
      }
    });
    button.setOnAction(event -> onTemplateCreating(event));
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
      Context context = service.begin();
      // 現在表示しているプライベートテンプレートを作ったボタンを押下した場合, プライベートテンプレートを閉じる
      if (lastClicked.getAndSet(node) == node
          && proxy.isShowed(BhConstants.NodeTemplate.PRIVATE_NODE_TEMPLATE)) {
        proxy.hideAll();
      } else {
        UserOperation userOpe = context.userOpe();
        deletePrivateTemplateNodes(userOpe);
        proxy.hideAll();
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
      service.end();
    }
  }

  /**
   * 引数で指定したノード固有のテンプレートを作成する.
   *
   * @param model このノードのテンプレートノードを作成する.
   */
  private SequencedSet<BhNode> createPrivateTemplates(UserOperation userOpe) {
    var privateTempladeNodes = new LinkedHashSet<BhNode>();
    for (var templateNode : node.createCompanionNodes(MvcType.TEMPLATE, userOpe)) {
      privateTempladeNodes.add(templateNode);
    }
    return privateTempladeNodes;
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
