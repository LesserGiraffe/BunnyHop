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
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.SequencedSet;
import javafx.scene.Node;
import net.seapanda.bunnyhop.common.configuration.BhSettings;
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
import net.seapanda.bunnyhop.node.model.traverse.BhNodeWalker;
import net.seapanda.bunnyhop.node.view.BhNodeView;
import net.seapanda.bunnyhop.node.view.ComboBoxNodeView;
import net.seapanda.bunnyhop.node.view.ConnectiveNodeView;
import net.seapanda.bunnyhop.node.view.LabelNodeView;
import net.seapanda.bunnyhop.node.view.NoContentNodeView;
import net.seapanda.bunnyhop.node.view.TextAreaNodeView;
import net.seapanda.bunnyhop.node.view.TextFieldNodeView;
import net.seapanda.bunnyhop.node.view.effect.VisualEffectManager;
import net.seapanda.bunnyhop.node.view.factory.BhNodeViewFactory;
import net.seapanda.bunnyhop.node.view.factory.NodeViewComponentFactory;
import net.seapanda.bunnyhop.node.view.factory.PrivateTemplateButtonFactory;
import net.seapanda.bunnyhop.nodeselection.control.BhNodeSelectionViewManager;
import net.seapanda.bunnyhop.service.LogManager;
import net.seapanda.bunnyhop.service.accesscontrol.TransactionNotificationService;
import net.seapanda.bunnyhop.ui.view.ViewConstructionException;
import net.seapanda.bunnyhop.utility.reference.ReadOnlyBoolean;
import net.seapanda.bunnyhop.workspace.model.WorkspaceSet;
import net.seapanda.bunnyhop.workspace.view.TrashCan;

/**
 * {@link BhNode} の MVC 構造を構築する機能を提供するクラス.
 *
 * @author K.Koike
 */
public class BhNodeMvcBuilderImpl implements BhNodeMvcBuilder {

  private final BhNodeViewFactory viewFactory;
  private final TransactionNotificationService notifService;
  private final TrashCan trashCan;
  private final WorkspaceSet wss;
  private final BhNodeSelectionViewManager nodeSelViewManager;
  private final VisualEffectManager effectManager;
  private final BhSettings settings;
  private final NodeViewComponentFactory componentFactory;

  /** コンストラクタ. */
  public BhNodeMvcBuilderImpl(
      BhNodeViewFactory viewFactory,
      PrivateTemplateButtonFactory buttonFactory,
      TransactionNotificationService notifService,
      TrashCan trashCan,
      WorkspaceSet wss,
      BhNodeSelectionViewManager nodeSelViewManager,
      VisualEffectManager visualEffectManager,
      BhSettings settings) {
    this.viewFactory = viewFactory;
    this.notifService = notifService;
    this.trashCan = trashCan;
    this.wss = wss;
    this.nodeSelViewManager = nodeSelViewManager;
    this.effectManager = visualEffectManager;
    this.settings = settings;
    this.componentFactory = new NodeViewComponentFactoryImpl(buttonFactory);
  }

  @Override
  public BhNodeView build(BhNode node, MvcType type) {
    if (type == MvcType.NONE) {
      return null;
    }
    var builder = this.new Builder(type);
    node.accept(builder);
    DerivativeTextSetter.set(node);
    return builder.root;
  }


  /** ノードの MVC 構造を構築するクラス. */
  private class Builder implements BhNodeWalker {

    /** MVC を構築した {@link BhNode} ツリーのルートノードのビュー. */
    private BhNodeView root;
    /** 子ノードの追加先のビュー. */
    private final Deque<ConnectiveNodeView> parentStack = new LinkedList<>();
    private final MvcType type;


    private Builder(MvcType type) {
      this.type = type;
    }

    private void addChildView(BhNode node, BhNodeView view) {
      if (node.getParent() != null && parentStack.peekLast() != null) {
        boolean success = parentStack.peekLast().addChild(view);
        if (!success) {
          view.getVisual().setVisible(false);
        }
      }
    }

    private boolean isTemplate() {
      return type == MvcType.TEMPLATE;
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
        if (node.getView().isPresent()) {
          view = node.getView().get();
        } else {
          node.setTemplate(isTemplate());
          view = viewFactory.createView(node, isTemplate(), componentFactory);
          connectMvc(node, view);
        }
      } catch (ViewConstructionException e) {
        LogManager.logger().error(e);
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
        if (node.getView().isPresent()) {
          view = node.getView().get();
        } else {
          node.setTemplate(isTemplate());
          view = viewFactory.createView(node, isTemplate(), componentFactory);
          connectMvc(node, view);
        }
      } catch (ViewConstructionException e) {
        LogManager.logger().error(e);
      }

      if (root == null) {
        root = view;
      }
      addChildView(node, view);
    }

    /** MVC のコントローラを作って {@link BhNode} と {@link BhNodeView} を渡す. */
    private void connectMvc(BhNode node, BhNodeView nodeView) {
      if (node.getView().isPresent()) {
        throw new AssertionError("Duplicated NodeView.  (BhNode = %s)".formatted(node.getId()));
      }
      BhNodeController ctrl = createBaseController(node, nodeView);
      ReadOnlyBoolean textChangeUndoEnabled = settings.undo.textChangeUndoEnabled;
      switch (nodeView) {
        case TextFieldNodeView ignored -> new TextInputNodeController(ctrl, textChangeUndoEnabled);
        case ComboBoxNodeView ignored -> new ComboBoxNodeController(ctrl, textChangeUndoEnabled);
        case LabelNodeView ignored -> new LabelNodeController(ctrl);
        case TextAreaNodeView ignored -> new TextInputNodeController(ctrl, textChangeUndoEnabled);
        case NoContentNodeView ignored -> new NoContentNodeController(ctrl);
        case ConnectiveNodeView ignored -> new ConnectiveNodeController(ctrl);
        default ->
          throw new IllegalStateException("Invalid BhNodeView type (%s)".formatted(nodeView));
      }
    }

    private BhNodeController createBaseController(BhNode node, BhNodeView nodeView) {
      return type == MvcType.DEFAULT
          ? new DefaultBhNodeController(
              node, nodeView, notifService, trashCan, effectManager, settings)
          : new TemplateNodeController(
              node, nodeView, BhNodeMvcBuilderImpl.this, notifService, wss, nodeSelViewManager);
    }
  }

  private static class NodeViewComponentFactoryImpl implements NodeViewComponentFactory {

    PrivateTemplateButtonFactory buttonFactory;

    NodeViewComponentFactoryImpl(PrivateTemplateButtonFactory buttonFactory) {
      this.buttonFactory = buttonFactory;
    }

    @Override
    public SequencedSet<Node> create(BhNode node) throws ViewConstructionException {
      SequencedSet<Node> components = new LinkedHashSet<>();
      if (node.hasCompanionNodes()) {
        components.add(buttonFactory.create(node));
      }
      return components;
    }
  }
}
