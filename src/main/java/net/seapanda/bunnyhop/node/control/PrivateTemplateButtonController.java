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

import static net.seapanda.bunnyhop.common.configuration.BhConstants.Css.Class.PRIVATE_NODE_TEMPLATE;
import static net.seapanda.bunnyhop.service.accesscontrol.ExclusiveTask.NODE_TREE_MODIFICATION;

import java.util.LinkedHashSet;
import java.util.Optional;
import java.util.SequencedSet;
import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.scene.control.Button;
import javafx.scene.input.MouseButton;
import javafx.scene.input.MouseEvent;
import net.seapanda.bunnyhop.node.model.BhNode;
import net.seapanda.bunnyhop.node.model.factory.MvcType;
import net.seapanda.bunnyhop.node.model.traverse.CallbackInvoker;
import net.seapanda.bunnyhop.nodeselection.control.BhNodeSelectionViewManager;
import net.seapanda.bunnyhop.nodeselection.view.BhNodeSelectionView;
import net.seapanda.bunnyhop.nodeselection.view.BhNodeSelectionViewFactory;
import net.seapanda.bunnyhop.service.LogManager;
import net.seapanda.bunnyhop.service.accesscontrol.TransactionContext;
import net.seapanda.bunnyhop.service.accesscontrol.TransactionManager;
import net.seapanda.bunnyhop.service.undo.UserOperation;
import net.seapanda.bunnyhop.ui.view.ViewConstructionException;

/**
 * ノード固有のテンプレートノードを作成するボタンのコントローラ.
 *
 * @author K.Koike
 */
public class PrivateTemplateButtonController {
  private final BhNode node;
  private final Button button;
  private final BhNodeSelectionViewManager nodeSelViewManager;
  private final BhNodeSelectionViewFactory nodeSelViewFactory;
  private final SequencedSet<BhNode> templateNodes = new LinkedHashSet<>();
  /** {@link #node} のコンパニオンノードのビューを格納するノード選択ビュー. */
  private BhNodeSelectionView nodeSelView;

  /**
   * コンストラクタ.
   *
   * @param node このノードの固有のテンプレートノードを作成する
   * @param button 管理するビュー
   * @param txnManager トランザクションを管理するオブジェクト
   * @param nodeSelViewManager ボタン押下時に操作するノード選択ビューの管理オブジェクト
   */
  public PrivateTemplateButtonController(
      BhNode node,
      Button button,
      BhNodeSelectionViewFactory nodeSelViewFactory,
      TransactionManager txnManager,
      BhNodeSelectionViewManager nodeSelViewManager) {
    this.node = node;
    this.button = button;
    this.nodeSelViewFactory = nodeSelViewFactory;
    this.nodeSelViewManager = nodeSelViewManager;
    setEventHandlers();
    scheduleTemplateNodeCreation(txnManager);
  }

  private void setEventHandlers() {
    button.setOnAction(this::showOrHideTemplateNodes);
    button.addEventFilter(MouseEvent.ANY, this::consumeIfNotAcceptable);
    button.setOnMouseEntered(
        event ->
            Optional.ofNullable(nodeSelView).ifPresent(nodeSelViewManager::addNodeSelectionView));
    button.focusedProperty().addListener(
        (obs, oldVal, newVal) ->
            Optional.ofNullable(nodeSelView).ifPresent(nodeSelViewManager::addNodeSelectionView));
  }

  /** ノード選択ビューの表示/非表示を切り替える. */
  private void showOrHideTemplateNodes(ActionEvent event) {
    if (node.isTemplate() || node.isDeleted() || nodeSelView == null) {
      return;
    }
    if (isThisNodeSelViewShowed()) {
      nodeSelViewManager.hideCurrentView();
      return;
    }
    nodeSelViewManager.addNodeSelectionView(nodeSelView);
    nodeSelViewManager.show(nodeSelView.getCategoryName());
    event.consume();
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

  /** 現在 {@link #nodeSelView} が表示されているか調べる. */
  private boolean isThisNodeSelViewShowed() {
    return nodeSelViewManager.getCurrentNodeSelectionView()
        .map(view -> nodeSelView == view)
        .orElse(false);
  }

  /**
   * トランザクションの終わりに, テンプレートノードの作成を実行するようにする.
   */
  private void scheduleTemplateNodeCreation(TransactionManager txnManager) {
    if (node.isTemplate()) {
      return;
    }
    TransactionContext context = txnManager.begin();
    context.onTransactionEnding().add(ctx -> {
      txnManager.begin();
      if (ctx.lockTasks(NODE_TREE_MODIFICATION)) {
        button.getScene().addPostLayoutPulseListener(new TemplateNodeViewCreator(ctx));
      }
    });
    txnManager.end();
  }


  /** レイアウト更新後にテンプレートノードとそれを表示するノード選択ビューを作成するクラス. */
  private class TemplateNodeViewCreator implements Runnable {

    private final TransactionContext context;

    private TemplateNodeViewCreator(TransactionContext context) {
      this.context = context;
    }

    @Override
    public void run() {
      Platform.runLater(() -> createTemplateNodeViews(context.userOpe()));
      button.getScene().removePostLayoutPulseListener(this);
    }

    /** テンプレートノードとそれを表示するノード選択ビューを作成する. */
    private void createTemplateNodeViews(UserOperation userOpe) {
      try {
        nodeSelView = createNodeSelectionView();
        if (nodeSelView == null) {
          return;
        }
        templateNodes.addAll(node.createCompanionNodes(MvcType.TEMPLATE, userOpe));
        templateNodes.forEach(node -> {
          nodeSelView.getWorkspace().addNodeTree(node, userOpe);
          invokeOnCreatedAsTemplate(node, userOpe);
        });
      } finally {
        context.releaseTasks(NODE_TREE_MODIFICATION);
        context.manager().end();
      }
    }

    /** テンプレートノードを表示するノード選択ビューを作成する. */
    private BhNodeSelectionView createNodeSelectionView() {
      try {
        BhNodeSelectionView view =
            nodeSelViewFactory.create(node.getInstanceId().toString(), PRIVATE_NODE_TEMPLATE);
        view.getCallbackRegistry().getOnVisibilityChanged().add(event -> {
          if (!event.isVisible()) {
            nodeSelViewManager.removeNodeSelectionView(view);
          }
        });
        return view;
      } catch (ViewConstructionException e) {
        LogManager.logger().error("Failed to create a template node selection view", e);
        return null;
      }
    }

    /** テンプレートノード作成時のコールバックを呼び出す. */
    private void invokeOnCreatedAsTemplate(BhNode templateNode, UserOperation userOpe) {
      CallbackInvoker.CallbackRegistry registry = CallbackInvoker.newCallbackRegistry()
          .setForAllNodes(bhNode -> bhNode.getEventInvoker().onCreatedAsTemplate(userOpe));
      CallbackInvoker.invoke(registry, templateNode);
    }
  }
}
