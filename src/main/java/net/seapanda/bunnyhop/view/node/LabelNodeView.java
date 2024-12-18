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

package net.seapanda.bunnyhop.view.node;

import java.util.Optional;
import javafx.scene.control.Label;
import net.seapanda.bunnyhop.common.BhConstants;
import net.seapanda.bunnyhop.model.node.TextNode;
import net.seapanda.bunnyhop.service.BhService;
import net.seapanda.bunnyhop.utility.Vec2D;
import net.seapanda.bunnyhop.view.ViewInitializationException;
import net.seapanda.bunnyhop.view.node.part.BhNodeViewStyle;
import net.seapanda.bunnyhop.view.node.part.BhNodeViewStyle.ConnectorPos;
import net.seapanda.bunnyhop.view.traverse.NodeViewProcessor;

/**
 * ラベルを入力フォームに持つビュー.
 *
 * @author K.Koike
 */
public final class LabelNodeView extends BhNodeView {

  private Label label = new Label();
  private final TextNode model;

  /**
   * コンストラクタ.
   *
   * @param model このノードビューに対応するノード
   * @param viewStyle このノードビューのスタイル
   * @throws ViewInitializationException ノードビューの初期化に失敗
   */
  public LabelNodeView(TextNode model, BhNodeViewStyle viewStyle)
      throws ViewInitializationException {
    super(viewStyle, model);
    this.model = model;
    getTreeManager().addChild(label);
    initStyle();
  }

  /**
   * コンストラクタ.
   *
   * @param viewStyle このノードビューのスタイル
   * @throws ViewInitializationException ノードビューの初期化に失敗
   */
  public LabelNodeView(BhNodeViewStyle viewStyle)
      throws ViewInitializationException {
    this(null, viewStyle);
  }

  private void initStyle() {
    label.autosize();
    label.setMouseTransparent(true);
    label.setTranslateX(viewStyle.paddingLeft);
    label.setTranslateY(viewStyle.paddingTop);
    label.getStyleClass().add(viewStyle.label.cssClass);
    label.heightProperty().addListener(newValue -> notifySizeChange());
    label.widthProperty().addListener(newValue -> notifySizeChange());
    getLookManager().addCssClass(BhConstants.Css.CLASS_LABEL_NODE);
  }

  @Override
  public Optional<TextNode> getModel() {
    return Optional.ofNullable(model);
  }

  @Override
  public void show(int depth) {
    BhService.msgPrinter().println("%s<LabelView>  %s".formatted(indent(depth), hashCode()));
    BhService.msgPrinter().println("%s<content>  %s".formatted(indent(depth + 1), label.getText()));
  }

  @Override
  protected void arrangeAndResize() {
    getLookManager().updatePolygonShape();
  }

  @Override
  protected Vec2D getBodySize(boolean includeCnctr) {
    Vec2D cnctrSize = viewStyle.getConnectorSize(isFixed());
    double bodyWidth = viewStyle.paddingLeft + label.getWidth() + viewStyle.paddingRight;
    if (includeCnctr && (viewStyle.connectorPos == ConnectorPos.LEFT)) {
      bodyWidth += cnctrSize.x;
    }
    double bodyHeight = viewStyle.paddingTop + label.getHeight() + viewStyle.paddingBottom;
    if (includeCnctr && (viewStyle.connectorPos == ConnectorPos.TOP)) {
      bodyHeight += cnctrSize.y;
    }
    return new Vec2D(bodyWidth, bodyHeight);
  }

  @Override
  protected Vec2D getNodeSizeIncludingOuter(boolean includeCnctr) {
    return getBodySize(includeCnctr);
  }

  public String getText() {
    return label.getText();
  }

  public void setText(String text) {
    label.setText(text);
  }

  @Override
  public void accept(NodeViewProcessor visitor) {
    visitor.visit(this);
  }
}
