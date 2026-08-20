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

package net.seapanda.bunnyhop.node.view;

import static net.seapanda.bunnyhop.ui.skin.HighlightingChangePolicy.REFRESH;

import java.util.LinkedHashSet;
import java.util.SequencedCollection;
import java.util.SequencedSet;
import java.util.regex.Pattern;
import javafx.scene.Node;
import javafx.scene.control.Label;
import net.seapanda.bunnyhop.common.configuration.BhConstants;
import net.seapanda.bunnyhop.node.model.TextNode;
import net.seapanda.bunnyhop.node.view.style.BhNodeViewStyle;
import net.seapanda.bunnyhop.node.view.traverse.NodeViewWalker;
import net.seapanda.bunnyhop.search.Substring;
import net.seapanda.bunnyhop.ui.skin.HighlightableLabelSkin;
import net.seapanda.bunnyhop.ui.view.ViewConstructionException;
import net.seapanda.bunnyhop.utility.math.Vec2D;

/**
 * ラベルを入力フォームに持つビュー.
 *
 * @author K.Koike
 */
public final class LabelNodeView extends TextNodeView {

  private final Label label = new Label();
  private final Visual visual = new Visual(this);
  private final Geometry geometry;

  /**
   * コンストラクタ.
   *
   * @param model このノードビューに対応するノード
   * @param style このノードビューのスタイル
   * @throws ViewConstructionException ノードビューの初期化に失敗
   */
  public LabelNodeView(
      TextNode model, BhNodeViewStyle style, SequencedSet<Node> components, boolean isTemplate)
      throws ViewConstructionException {
    super(model, style, components, isTemplate);
    geometry = new Geometry(this, new NodeSizeCalculator(this, this::getContentRegionSize)) {};
    setComponent(label);
    initializeStyle();
  }

  /**
   * コンストラクタ.
   *
   * @param style このノードビューのスタイル
   * @throws ViewConstructionException ノードビューの初期化に失敗
   */
  public LabelNodeView(BhNodeViewStyle style, boolean isTemplate) throws ViewConstructionException {
    this(null, style, new LinkedHashSet<>(), isTemplate);
  }

  private void initializeStyle() {
    label.autosize();
    label.setMouseTransparent(true);
    label.getStyleClass().add(getStyle().label.cssClass);
    visual.addCssClass(getStyle().cssClasses);
    visual.addCssClass(BhConstants.Css.Class.BH_NODE);
    visual.addCssClass(BhConstants.Css.Class.LABEL_NODE);
  }

  public String getText() {
    return label.getText();
  }

  public void setText(String text) {
    label.setText(text);
  }

  /** コンテンツを表示する領域の大きさを取得する. */
  private Vec2D getContentRegionSize() {
    return new Vec2D(label.getWidth(), label.getHeight());
  }

  @Override
  public Visual getVisual() {
    return visual;
  }

  @Override
  public Geometry getGeometry() {
    return geometry;
  }

  @Override
  public void accept(NodeViewWalker visitor) {
    visitor.visit(this);
  }

  /** ノードビューの視覚効果に関する機能を提供するクラス. */
  public static class Visual extends TextNodeView.Visual {

    /** ラベルに適用するスキン. */
    private final HighlightableLabelSkin skin;

    Visual(LabelNodeView view) {
      super(view);
      String styleClass = view.getStyle().label.textHighlight.cssClass;
      skin = new HighlightableLabelSkin(view.label, styleClass, REFRESH);
      view.label.setSkin(skin);
    }

    @Override
    public SequencedCollection<Substring> enableTextHighlighting(
        Pattern pattern, int maxHighlights) {
      return skin.enableHighlighting(pattern, maxHighlights);
    }

    @Override
    public void disableTextHighlighting() {
      skin.disableHighlighting();
    }

    @Override
    public SequencedCollection<Substring> getHighlightedTexts() {
      return skin.getHighlightedTexts();
    }

    @Override
    public boolean isTextHighlightingEnabled() {
      return skin.isHighlightingEnabled();
    }
  }
}
