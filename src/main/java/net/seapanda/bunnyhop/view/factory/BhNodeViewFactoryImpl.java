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

package net.seapanda.bunnyhop.view.factory;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.SequencedSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javafx.scene.Node;
import net.seapanda.bunnyhop.model.node.BhNode;
import net.seapanda.bunnyhop.model.node.ConnectiveNode;
import net.seapanda.bunnyhop.model.node.TextNode;
import net.seapanda.bunnyhop.model.node.parameter.BhNodeViewStyleId;
import net.seapanda.bunnyhop.view.ViewConstructionException;
import net.seapanda.bunnyhop.view.node.BhNodeView;
import net.seapanda.bunnyhop.view.node.ComboBoxNodeView;
import net.seapanda.bunnyhop.view.node.ConnectiveNodeView;
import net.seapanda.bunnyhop.view.node.LabelNodeView;
import net.seapanda.bunnyhop.view.node.NoContentNodeView;
import net.seapanda.bunnyhop.view.node.TextAreaNodeView;
import net.seapanda.bunnyhop.view.node.TextFieldNodeView;
import net.seapanda.bunnyhop.view.node.component.SelectableItem;
import net.seapanda.bunnyhop.view.node.style.BhNodeViewStyle;
import net.seapanda.bunnyhop.view.node.style.BhNodeViewStyleFactory;

/**
 * {@link BhNodeView} を作成する機能を提供するクラス.
 *
 * @author K.Koike
 */
public class BhNodeViewFactoryImpl implements BhNodeViewFactory {
  
  private static Pattern escapeLbrace = Pattern.compile(Pattern.quote("\\{"));
  private static Pattern escapeRbrace = Pattern.compile(Pattern.quote("\\}"));
  /** 疑似ビュー指定パターン `${a}{b}...{z}` の (a, b, ..., z) を取り出す用. */
  private static Pattern contents =
      Pattern.compile("\\{((?:(?:\\\\\\{)|(?:\\\\\\})|[^\\{\\}])*)\\}");

  private final BhNodeViewStyleFactory nodeStyleFactory;
  PrivateTemplateButtonFactory buttonFactory;

  /**
   * コンストラクタ.
   *
   * @param nodeStyleFactory ノードビューのスタイルを作成するのに使用するオブジェクト
   * @param buttonFactory プライベートテンプレートボタンを作成するためのオブジェクト
   */
  public BhNodeViewFactoryImpl(
      BhNodeViewStyleFactory nodeStyleFactory,
      PrivateTemplateButtonFactory buttonFactory) {
    this.nodeStyleFactory = nodeStyleFactory;
    this.buttonFactory = buttonFactory;
  }

  @Override
  public BhNodeView createViewOf(BhNode node) throws ViewConstructionException {
    return switch (node) {
      case TextNode textNode -> createViewOf(textNode);
      case ConnectiveNode connectiveNode -> createViewOf(connectiveNode);
      default -> throw new ViewConstructionException("Invalid BhNode type");
    };
  }

  @Override
  public BhNodeView createViewOf(String specification) throws ViewConstructionException {
    Matcher matcher = contents.matcher(specification);
    List<String> specifiers = matcher.results().map(
        result -> {  
          String tmp = escapeLbrace.matcher(result.group(1)).replaceAll("{");
          return escapeRbrace.matcher(tmp).replaceAll("}");
        }).toList();
    BhNodeViewStyleId styleId = BhNodeViewStyleId.of(
        specifiers.get(0).endsWith(".json") ? specifiers.get(0) : specifiers.get(0) + ".json");
    return createModellessNodeView(styleId, specifiers.get(1));
  }

  private BhNodeView createViewOf(TextNode node) throws ViewConstructionException {
    BhNodeViewStyle style = nodeStyleFactory.canCreateStyleOf(node.getStyleId())
        ? nodeStyleFactory.createStyleOf(node.getStyleId())
        : new BhNodeViewStyle();
    SequencedSet<Node> components = createComponents(node);

    return switch (style.component) {
      case TEXT_FIELD -> new TextFieldNodeView(node, style, components);
      case COMBO_BOX -> new ComboBoxNodeView(node, style, components);
      case LABEL -> new LabelNodeView(node, style, components);
      case TEXT_AREA -> new TextAreaNodeView(node, style, components);
      case NONE -> new NoContentNodeView(node, style, components);
      default -> throw new ViewConstructionException(
          "Invalid component type (%s)".formatted(style.component));
    };
  }

  private BhNodeView createViewOf(ConnectiveNode node) throws ViewConstructionException {
    BhNodeViewStyle style = nodeStyleFactory.canCreateStyleOf(node.getStyleId())
        ? nodeStyleFactory.createStyleOf(node.getStyleId())
        : new BhNodeViewStyle();
    SequencedSet<Node> components = createComponents(node);
    return new ConnectiveNodeView(node, style, components, this);
  }

  /** {@link BhNodeView} に追加する GUI コンポーネントを作成する. */
  private SequencedSet<Node> createComponents(BhNode node)
      throws ViewConstructionException {
    SequencedSet<Node> components = new LinkedHashSet<>();
    if (node.hasCompanionNodes()) {
      components.add(buttonFactory.createButtonOf(node));
    }
    return components;
  }

  /**
   * モデルを持たない {@link BhNodeView} を作成する.
   *
   * @param styleId 作成するビューのスタイル ID
   * @param text 作成するビューに設定する文字列
   * @return 作成した {@link BhNodeView}
   */
  private BhNodeView createModellessNodeView(BhNodeViewStyleId styleId, String text)
      throws ViewConstructionException {
    BhNodeViewStyle style = nodeStyleFactory.canCreateStyleOf(styleId)
        ? nodeStyleFactory.createStyleOf(styleId)
        : new BhNodeViewStyle();

    return switch (style.component) {
      case TEXT_FIELD -> {
        var view = new TextFieldNodeView(style);
        view.setTextChangeListener(str -> true);
        view.setText(text);
        yield view;
      }
      case COMBO_BOX -> {
        var view = new ComboBoxNodeView(style);
        view.setValue(new SelectableItem<>(text, text));
        yield view;
      }
      case LABEL -> {
        var view = new LabelNodeView(style);
        view.setText(text);
        yield view;
      }
      case TEXT_AREA -> {
        var view = new TextAreaNodeView(style);
        view.setTextChangeListener(str -> true);
        view.setText(text);
        yield view;
      }
      default -> {
        throw new ViewConstructionException(
            "Cannot create a modelless node view whose component is '%s'.  (%s)"
                .formatted(style.component, styleId));
      }
    };
  }  
}
