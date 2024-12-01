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

package net.seapanda.bunnyhop.model.traverse;

import java.util.Deque;
import java.util.LinkedList;
import java.util.Optional;
import net.seapanda.bunnyhop.command.BhCmd;
import net.seapanda.bunnyhop.command.CmdData;
import net.seapanda.bunnyhop.control.node.ComboBoxNodeController;
import net.seapanda.bunnyhop.control.node.ConnectiveNodeController;
import net.seapanda.bunnyhop.control.node.LabelNodeController;
import net.seapanda.bunnyhop.control.node.NoContentNodeController;
import net.seapanda.bunnyhop.control.node.TemplateNodeController;
import net.seapanda.bunnyhop.control.node.TextInputNodeController;
import net.seapanda.bunnyhop.model.node.BhNode;
import net.seapanda.bunnyhop.model.node.ConnectiveNode;
import net.seapanda.bunnyhop.model.node.Connector;
import net.seapanda.bunnyhop.model.node.TextNode;
import net.seapanda.bunnyhop.service.BhService;
import net.seapanda.bunnyhop.view.ViewInitializationException;
import net.seapanda.bunnyhop.view.node.BhNodeView;
import net.seapanda.bunnyhop.view.node.ComboBoxNodeView;
import net.seapanda.bunnyhop.view.node.ConnectiveNodeView;
import net.seapanda.bunnyhop.view.node.LabelNodeView;
import net.seapanda.bunnyhop.view.node.NoContentNodeView;
import net.seapanda.bunnyhop.view.node.TextAreaNodeView;
import net.seapanda.bunnyhop.view.node.TextFieldNodeView;
import net.seapanda.bunnyhop.view.node.part.BhNodeViewStyle;

/**
 * ノードの MVC 構造を構築するクラス.
 *
 * @author K.Koike
 */
public class NodeMvcBuilder implements BhNodeWalker {

  /** MVC を構築した {@link BhNode} ツリーのルートノードのビュー. */
  private BhNodeView root;
  /** 子ノードの追加先のビュー. */
  private final Deque<ConnectiveNodeView> parentStack = new LinkedList<>();
  private MvcConnector mvcConnector;

  /**
   * 引数で指定したノード以下のノードに対し, MVC 構造を構築する. (ワークスペースに追加するノード用).
   *
   * @return 引数で指定したノードに対応する {@link BhNodeView}.
   *         {@code node} に対応する {@link BhNodeView} の定義が見つからなかった場合は null
   */
  public static BhNodeView build(BhNode node) {
    if (BhService.cmdProxy().hasView(node)) {
      return BhService.cmdProxy().getBhNodeView(node);
    }
    var builder = new NodeMvcBuilder(ControllerType.Default);
    node.accept(builder);
    if (builder.root != null) {
      builder.root.getLookManager().arrangeAndResize();
    }
    return builder.root;
  }

  /**
   * 引数で指定したノード以下のノードに対し, MVC 構造を構築する. (ノード選択ビューに追加するノード用)
   *
   * @return 引数で指定したノードに対応する BhNodeView.
   */
  public static BhNodeView buildTemplate(BhNode node) {
    if (BhService.cmdProxy().hasView(node)) {
      return BhService.cmdProxy().getBhNodeView(node);
    }
    var builder = new NodeMvcBuilder(ControllerType.Template);
    node.accept(builder);
    builder.root.getLookManager().arrangeAndResize();
    return builder.root;
  }

  /**
   * コンストラクタ.
   *
   * @param type Controller の種類 (ワークスペースのノード用かノードセレクタ用)
   */
  private NodeMvcBuilder(ControllerType type) {
    if (type == ControllerType.Default) {
      mvcConnector = new DefaultConnector();
    } else if (type == ControllerType.Template) {
      mvcConnector = new TemplateConnector();
    }
  }

  private void addChildView(BhNode node, BhNodeView view) {    
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
    Optional<ConnectiveNodeView> nodeView = BhNodeViewStyle
        .getStyleFromNodeId(node.getId())
        .flatMap(style -> createViewForConnectiveNode(node, style));
    
    if (nodeView.isEmpty()) {
      node.setMsgProcessor((BhCmd msg, CmdData data) -> null);
    }

    if (root == null) {
      root = nodeView.orElse(null);
    }
    parentStack.addLast(nodeView.orElse(null));
    node.sendToSections(this);
    parentStack.removeLast();
    nodeView.ifPresent(view -> addChildView(node, view));
  }

  @Override
  public void visit(TextNode node) {
    Optional<BhNodeView> nodeView = BhNodeViewStyle
        .getStyleFromNodeId(node.getId())
        .flatMap(style -> createViewForTextNode(node, style));
    
    if (nodeView.isEmpty()) {
      node.setMsgProcessor((BhCmd msg, CmdData data) -> null);
    }
    if (root == null) {
      root = nodeView.orElse(null);
    }
    nodeView.ifPresent(view -> addChildView(node, view));
  }

  @Override
  public void visit(Connector connector) {
    connector.sendToConnectedNode(this);
  }

  /**
   * 引数で指定したコネクティブノードに応じたノードビューを作成する.
   *
   * @param node このノードに対応するノードビューを作成する
   * @param viewStyle ノードビューに適用するスタイル
   * @return {@code node} に対応するノードビュー
   */
  private Optional<ConnectiveNodeView> createViewForConnectiveNode(
      ConnectiveNode node, BhNodeViewStyle viewStyle) {
    ConnectiveNodeView nodeView = null;
    try {
      nodeView = new ConnectiveNodeView(node, viewStyle);
      mvcConnector.connect(node, nodeView);
    } catch (ViewInitializationException e) {
      BhService.msgPrinter().errForDebug(e.toString());
      return Optional.empty();
    }
    return Optional.of(nodeView);
  }
  
  /**
   * 引数で指定したテキストノードに応じたノードビューを作成する.
   *
   * @param node このノードに対応するノードビューを作成する
   * @param viewStyle ノードビューに適用するスタイル
   * @return {@code node} に対応するノードビュー
   */
  private Optional<BhNodeView> createViewForTextNode(TextNode node, BhNodeViewStyle viewStyle) {
    try {
      switch (viewStyle.component) {
        case TEXT_FIELD:
          var textNodeView = new TextFieldNodeView(node, viewStyle);
          mvcConnector.connect(node, textNodeView);
          return Optional.of(textNodeView);
  
        case COMBO_BOX:
          var comboBoxNodeView = new ComboBoxNodeView(node, viewStyle);
          mvcConnector.connect(node, comboBoxNodeView);
          return Optional.of(comboBoxNodeView);
  
        case LABEL:
          var labelNodeView = new LabelNodeView(node, viewStyle);
          mvcConnector.connect(node, labelNodeView);
          return Optional.of(labelNodeView);
  
        case TEXT_AREA:
          var textAreaNodeView = new TextAreaNodeView(node, viewStyle);
          mvcConnector.connect(node, textAreaNodeView);
          return Optional.of(textAreaNodeView);
  
        case NONE:
          var noContentNodeView = new NoContentNodeView(node, viewStyle);
          mvcConnector.connect(node, noContentNodeView);
          return Optional.of(noContentNodeView);
    
        default:
          throw new ViewInitializationException(
              "Invalid component type (%s)".formatted(viewStyle.component));
      }
    } catch (ViewInitializationException e) {
      BhService.msgPrinter().errForDebug(e.toString());
    }
    return Optional.empty();
  }

  private interface MvcConnector {
    public void connect(ConnectiveNode node, ConnectiveNodeView view);
    
    public void connect(TextNode node, TextFieldNodeView view);
    
    public void connect(TextNode node, LabelNodeView view);
    
    public void connect(TextNode node, ComboBoxNodeView view);
    
    public void connect(TextNode node, TextAreaNodeView view);
    
    public void connect(TextNode node, NoContentNodeView view);
  }

  /** ワークスペースに追加されるノードのModel と View をつなぐ機能を提供するクラス. */
  private static class DefaultConnector implements MvcConnector {
    @Override
    public void connect(ConnectiveNode node, ConnectiveNodeView view) {
      var controller = new ConnectiveNodeController(node, view);
      node.setMsgProcessor(controller);
    }

    @Override
    public void connect(TextNode node, TextFieldNodeView view) {
      var controller = new TextInputNodeController(node, view);
      node.setMsgProcessor(controller);
    }

    @Override
    public void connect(TextNode node, LabelNodeView view) {
      var controller = new LabelNodeController(node, view);
      node.setMsgProcessor(controller);
    }

    @Override
    public void connect(TextNode node, ComboBoxNodeView view) {
      var controller = new ComboBoxNodeController(node, view);
      node.setMsgProcessor(controller);
    }

    @Override
    public void connect(TextNode node, TextAreaNodeView  view) {
      var controller = new TextInputNodeController(node, view);
      node.setMsgProcessor(controller);
    }

    @Override
    public void connect(TextNode node, NoContentNodeView view) {
      var controller = new NoContentNodeController(node, view);
      node.setMsgProcessor(controller);
    }
  }

  /** テンプレートノードリストに追加されるノードの Model と View をつなぐ機能を提供するクラス. */
  private static class TemplateConnector implements MvcConnector {

    BhNodeView rootView = null;  //トップノードのビュー

    @Override
    public void connect(ConnectiveNode node, ConnectiveNodeView view) {
      if (rootView == null) {
        rootView = view;
      }
      var control = new TemplateNodeController(node, view, rootView);
      node.setMsgProcessor(control);
    }

    @Override
    public void connect(TextNode node, TextFieldNodeView view) {
      if (rootView == null) {
        rootView = view;
      }
      var control = new TemplateNodeController(node, view, rootView);
      node.setMsgProcessor(control);
    }

    @Override
    public void connect(TextNode node, LabelNodeView view) {
      if (rootView == null) {
        rootView = view;
      }
      var control = new TemplateNodeController(node, view, rootView);
      node.setMsgProcessor(control);
    }

    @Override
    public void connect(TextNode node, ComboBoxNodeView view) {
      if (rootView == null) {
        rootView = view;
      }
      var control = new TemplateNodeController(node, view, rootView);
      node.setMsgProcessor(control);
    }

    @Override
    public void connect(TextNode node, TextAreaNodeView view) {
      if (rootView == null) {
        rootView = view;
      }
      var control = new TemplateNodeController(node, view, rootView);
      node.setMsgProcessor(control);
    }

    @Override
    public void connect(TextNode node, NoContentNodeView view) {
      if (rootView == null) {
        rootView = view;
      }
      var control = new TemplateNodeController(node, view, rootView);
      node.setMsgProcessor(control);
    }
  }

  /** MVC のコントローラのタイプ. */
  public static enum ControllerType {
    /** ワークスペース上で操作される BhNode 用のコントローラ. */
    Default,
    /** テンプレートリスト上にあるBhNode 用のMVCコネクタ. */
    Template,
  }
}
