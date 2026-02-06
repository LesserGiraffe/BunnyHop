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

import java.util.LinkedHashSet;
import java.util.Optional;
import java.util.SequencedSet;
import javafx.scene.Node;
import javafx.scene.control.Label;
import net.seapanda.bunnyhop.common.configuration.BhConstants;
import net.seapanda.bunnyhop.node.model.TextNode;
import net.seapanda.bunnyhop.node.view.style.BhNodeViewStyle;
import net.seapanda.bunnyhop.node.view.traverse.NodeViewWalker;
import net.seapanda.bunnyhop.ui.view.ViewConstructionException;
import net.seapanda.bunnyhop.utility.math.Vec2D;

/**
 * ラベルを入力フォームに持つビュー.
 *
 * @author K.Koike
 */
public final class LabelNodeView extends BhNodeViewBase {

  private final Label label = new Label();
  private final TextNode model;
  private final NodeSizeCalculator sizeCalculator;

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
    super(style, model, components, isTemplate);
    this.model = model;
    sizeCalculator = new NodeSizeCalculator(this, this::getLabelSize);
    setComponent(label);
    initStyle();
  }

  /**
   * コンストラクタ.
   *
   * @param style このノードビューのスタイル
   * @throws ViewConstructionException ノードビューの初期化に失敗
   */
  public LabelNodeView(BhNodeViewStyle style, boolean isTemplate)
      throws ViewConstructionException {
    this(null, style, new LinkedHashSet<>(), isTemplate);
  }

  private void initStyle() {
    label.autosize();
    label.setMouseTransparent(true);
    label.getStyleClass().add(style.label.cssClass);
    getLookManager().addCssClass(BhConstants.Css.Class.LABEL_NODE);
  }

  private Vec2D getLabelSize() {
    return new Vec2D(label.getWidth(), label.getHeight());
  }

  @Override
  protected void notifyChildSizeChanged() {
    sizeCalculator.notifyNodeSizeChanged();
    super.notifyChildSizeChanged();
  }

  @Override
  public Optional<TextNode> getModel() {
    return Optional.ofNullable(model);
  }

  @Override
  protected void updatePosOnWorkspace(double posX, double posY) {
    getPositionManager().setPosOnWorkspace(posX, posY);
  }

  @Override
  protected Vec2D getNodeSize(boolean includeCnctr) {
    return sizeCalculator.calcNodeSize(includeCnctr);
  }

  @Override
  protected Vec2D getNodeTreeSize(boolean includeCnctr) {
    return getNodeSize(includeCnctr);
  }

  @Override
  protected void updateChildRelativePos() {}

  public String getText() {
    return label.getText();
  }

  public void setText(String text) {
    label.setText(text);
  }

  @Override
  public void accept(NodeViewWalker visitor) {
    visitor.visit(this);
  }
}
