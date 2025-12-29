
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

package net.seapanda.bunnyhop.node.model.factory;

import java.util.Deque;
import java.util.LinkedList;
import net.seapanda.bunnyhop.node.control.BhNodeController;
import net.seapanda.bunnyhop.node.control.ComboBoxNodeController;
import net.seapanda.bunnyhop.node.control.ConnectiveNodeController;
import net.seapanda.bunnyhop.node.control.DefaultBhNodeController;
import net.seapanda.bunnyhop.node.control.LabelNodeController;
import net.seapanda.bunnyhop.node.control.NoContentNodeController;
import net.seapanda.bunnyhop.node.control.TemplateNodeController;
import net.seapanda.bunnyhop.node.control.TextInputNodeController;
import net.seapanda.bunnyhop.node.model.BhNode;
import net.seapanda.bunnyhop.node.model.ConnectiveNode;
import net.seapanda.bunnyhop.node.model.TextNode;
import net.seapanda.bunnyhop.node.model.derivative.DerivativeTextSetter;
import net.seapanda.bunnyhop.node.model.parameter.BhNodeId;
import net.seapanda.bunnyhop.node.model.traverse.BhNodeWalker;
import net.seapanda.bunnyhop.node.view.BhNodeView;
import net.seapanda.bunnyhop.node.view.BhNodeViewBase;
import net.seapanda.bunnyhop.node.view.ComboBoxNodeView;
import net.seapanda.bunnyhop.node.view.ConnectiveNodeView;
import net.seapanda.bunnyhop.node.view.LabelNodeView;
import net.seapanda.bunnyhop.node.view.NoContentNodeView;
import net.seapanda.bunnyhop.node.view.TextAreaNodeView;
import net.seapanda.bunnyhop.node.view.TextFieldNodeView;
import net.seapanda.bunnyhop.node.view.effect.VisualEffectManager;
import net.seapanda.bunnyhop.node.view.factory.BhNodeViewFactory;
import net.seapanda.bunnyhop.nodeselection.view.BhNodeSelectionViewProxy;
import net.seapanda.bunnyhop.service.LogManager;
import net.seapanda.bunnyhop.service.accesscontrol.TransactionNotificationService;
import net.seapanda.bunnyhop.service.undo.UserOperation;
import net.seapanda.bunnyhop.ui.view.ViewConstructionException;
import net.seapanda.bunnyhop.workspace.model.WorkspaceSet;
import net.seapanda.bunnyhop.workspace.view.TrashCan;

/**
 * {@link BhNode} の作成と MVC 構造の作成処理を提供するクラス.
 *
 * @author K.Koike
 */
public class BhNodeFactoryImpl implements BhNodeFactory {
  
  private final BhNodeRepository repository;
  private final BhNodeViewFactory viewFactory;
  private final TransactionNotificationService notifService;
  private final TrashCan trashCan;
  private final WorkspaceSet wss;
  private final BhNodeSelectionViewProxy proxy;
  private final VisualEffectManager effectManager;

  /** コンストラクタ. */
  public BhNodeFactoryImpl(
      BhNodeRepository repository,
      BhNodeViewFactory viewFactory,
      TransactionNotificationService notifService,
      TrashCan trashCan,
      WorkspaceSet wss,
      BhNodeSelectionViewProxy proxy,
      VisualEffectManager visualEffectManager) {
    this.repository = repository;
    this.viewFactory = viewFactory;
    this.notifService = notifService;
    this.trashCan = trashCan;
    this.wss = wss;
    this.proxy = proxy;
    this.effectManager = visualEffectManager;
  }

  @Override
  public BhNode create(BhNodeId id, UserOperation userOpe) {
    return repository.getNodeOf(id).copy(userOpe);
  }

  @Override
  public BhNode create(BhNodeId id, MvcType type, UserOperation userOpe) {
    BhNode node = create(id, userOpe);
    if (node != null && type != MvcType.NONE) {
      setMvc(node, type);
    }
    return node;
  }

  @Override
  public BhNodeView setMvc(BhNode node, MvcType type) {
    if (node.getView().isPresent()) {
      return node.getView().get();
    }
    var mvcBuilder = this.new NodeMvcBuilder(type);
    node.accept(mvcBuilder);
    DerivativeTextSetter.set(node);
    return mvcBuilder.root;
  }

  @Override
  public boolean canCreate(BhNodeId id) {
    return repository.hasNodeOf(id);
  }

  /** ノードの MVC 構造を構築するクラス. */
  private class NodeMvcBuilder implements BhNodeWalker {

    /** MVC を構築した {@link BhNode} ツリーのルートノードのビュー. */
    private BhNodeView root;
    /** 子ノードの追加先のビュー. */
    private final Deque<ConnectiveNodeView> parentStack = new LinkedList<>();
    private final MvcType type;


    private NodeMvcBuilder(MvcType type) {
      this.type = type;
    }

    private void addChildView(BhNode node, BhNodeViewBase view) {
      if ((node.getParentConnector() != null) && (parentStack.peekLast() != null)) {
        parentStack.peekLast().addToGroup(view);
      }
    }

    /**
     * node のビューとコントロールを作成しMVCとして結びつける.
     *
     * @param node ビューとコントロールを結びつけるノード
     */
    @Override
    public void visit(ConnectiveNode node) {
      BhNodeView view = null;
      try {
        view = viewFactory.createViewOf(node);
        connectMvc(node, view);
      } catch (ViewConstructionException e) {
        LogManager.logger().error(e.toString());
      }

      if (root == null) {
        root = view;
      }
      if (view instanceof ConnectiveNodeView connectiveNodeView) {
        parentStack.addLast(connectiveNodeView);
        node.sendToSections(this);
        parentStack.removeLast();
        addChildView(node, connectiveNodeView);
      }
    }

    @Override
    public void visit(TextNode node) {
      BhNodeView view = null;
      try {
        view = viewFactory.createViewOf(node);
        connectMvc(node, view);
      } catch (ViewConstructionException e) {
        LogManager.logger().error(e.toString());
      }
      
      if (root == null) {
        root = view;
      }
      if (view instanceof BhNodeViewBase nodeViewBase) {
        addChildView(node, nodeViewBase);
      }
    }

    /** MVC のコントローラを作って {@link BhNode} と {@link BhNodeView} を渡す. */
    private void connectMvc(BhNode node, BhNodeView nodeView) {
      if (node.getView().isPresent()) {
        throw new AssertionError("Duplicated NodeView.  (BhNode = %s)".formatted(node.getId()));
      }
      BhNodeController ctrl = createBaseController(node, nodeView);
      switch (nodeView) {
        case TextFieldNodeView view -> new TextInputNodeController(ctrl);
        case ComboBoxNodeView view -> new ComboBoxNodeController(ctrl);
        case LabelNodeView view -> new LabelNodeController(ctrl);
        case TextAreaNodeView view -> new TextInputNodeController(ctrl);
        case NoContentNodeView view -> new NoContentNodeController(ctrl);
        case ConnectiveNodeView view -> new ConnectiveNodeController(ctrl);
        default ->
          throw new IllegalStateException("Invalid BhNodeView type (%s)".formatted(nodeView));
      };
    }

    private BhNodeController createBaseController(BhNode node, BhNodeView nodeView) {
      return type == MvcType.DEFAULT
          ? new DefaultBhNodeController(node, nodeView, notifService, trashCan, effectManager)
          : new TemplateNodeController(
              node, nodeView, BhNodeFactoryImpl.this, notifService, wss, proxy);
    }
  }  
}
