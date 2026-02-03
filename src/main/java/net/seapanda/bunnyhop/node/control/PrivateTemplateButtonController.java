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

import static net.seapanda.bunnyhop.common.configuration.BhConstants.NodeSelection.PRE_RENDERING;
import static net.seapanda.bunnyhop.common.configuration.BhConstants.NodeSelection.PRIVATE_TEMPLATE;

import java.util.LinkedHashSet;
import java.util.SequencedSet;
import javafx.event.ActionEvent;
import javafx.scene.control.Button;
import javafx.scene.input.MouseButton;
import javafx.scene.input.MouseEvent;
import net.seapanda.bunnyhop.node.model.BhNode;
import net.seapanda.bunnyhop.node.model.factory.BhNodeFactory;
import net.seapanda.bunnyhop.node.model.traverse.CallbackInvoker;
import net.seapanda.bunnyhop.nodeselection.view.BhNodeSelectionViewProxy;
import net.seapanda.bunnyhop.service.accesscontrol.TransactionNotificationService;
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
  private final TransactionNotificationService service;
  private final BhNodeSelectionViewProxy proxy;
  private SequencedSet<BhNode> templateNodes;

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
      TransactionNotificationService service,
      BhNodeSelectionViewProxy proxy) {
    this.node = node;
    this.service = service;
    this.proxy = proxy;
    button.setOnAction(this::onTemplateCreating);
    button.addEventFilter(MouseEvent.ANY, this::consumeIfNotAcceptable);
    button.setOnMouseEntered(event -> prepareTemplateNodes());
    button.focusedProperty().addListener((obs, oldVal, newVal) -> prepareTemplateNodes());
  }

  /**
   * ノード固有のテンプレート作成時のイベントハンドラ.
   *
   * @param event ボタン押下イベント
   */
  private void onTemplateCreating(ActionEvent event) {
    if (node.isTemplate() || node.isDeleted()) {
      return;
    }
    try {
      service.begin();
      if (currentProducerNode == node && isPrivateTemplateShowed()) {
        proxy.hideCurrentView();
        return;
      }
      UserOperation userOpe = new UserOperation();
      proxy.clearNodeTrees(PRE_RENDERING, userOpe);
      proxy.clearNodeTrees(PRIVATE_TEMPLATE, userOpe);
      proxy.hideCurrentView();
      prepareTemplateNodes();
      templateNodes.forEach(
          template -> proxy.addNodeTree(PRIVATE_TEMPLATE, template, userOpe));
      proxy.show(PRIVATE_TEMPLATE);
      setCurrentProducerNode(node, userOpe);
    } finally {
      event.consume();
      service.end();
    }
  }

  /** テンプレートノード作成時のコールバックを呼び出す. */
  private void invokeOnCreatedAsTemplate(BhNode templateNode, UserOperation userOpe) {
    CallbackInvoker.CallbackRegistry registry = CallbackInvoker.newCallbackRegistry()
        .setForAllNodes(bhNode -> bhNode.getEventInvoker().onCreatedAsTemplate(userOpe));
    CallbackInvoker.invoke(registry, templateNode);
  }

  /** 最後にノード固有のテンプレートノードを作成したノードを設定する. */
  private void setCurrentProducerNode(BhNode node, UserOperation userOpe) {
    BhNode oldCurrentProducerNode = currentProducerNode;
    currentProducerNode = node;
    userOpe.pushCmd(ope -> setCurrentProducerNode(oldCurrentProducerNode, ope));
  }

  /** 受付不能なマウスイベントを consume する. */
  private void consumeIfNotAcceptable(MouseEvent event) {
    if (event.getEventType() == MouseEvent.MOUSE_ENTERED) {
      return;
    }
    if (event.getButton() != MouseButton.PRIMARY) {
      event.consume();
    }
  }

  /** ノード固有のテンプレートノードを作成し, プリレンダリングした後 {@link #templateNodes} に格納する. */
  private void prepareTemplateNodes() {
    if (node.isTemplate() || templateNodes != null) {
      return;
    }
    var userOpe = new UserOperation();
    templateNodes = new LinkedHashSet<>(
        node.createCompanionNodes(BhNodeFactory.MvcType.TEMPLATE, userOpe));
    proxy.clearNodeTrees(PRE_RENDERING, userOpe);
    templateNodes.forEach(templateNode -> proxy.addNodeTree(PRE_RENDERING, templateNode, userOpe));
    // ノード選択ビューに追加してからイベントハンドラを呼ぶ
    templateNodes.forEach(templateNode -> invokeOnCreatedAsTemplate(templateNode, userOpe));
  }

  /** 現在表示されているノード選択ビューがノード固有のテンプレートを表示するためのものであるか調べる. */
  private boolean isPrivateTemplateShowed() {
    return proxy.getCurrentCategoryName().map(PRIVATE_TEMPLATE::equals).orElse(false);
  }
}
