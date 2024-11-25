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
import net.seapanda.bunnyhop.common.Vec2D;
import net.seapanda.bunnyhop.common.constant.BhConstants;
import net.seapanda.bunnyhop.model.node.TextNode;
import net.seapanda.bunnyhop.service.MsgPrinter;
import net.seapanda.bunnyhop.view.ViewInitializationException;
import net.seapanda.bunnyhop.view.bodyshape.BodyShapeBase.BodyShape;
import net.seapanda.bunnyhop.view.node.part.BhNodeViewStyle;
import net.seapanda.bunnyhop.view.node.part.BhNodeViewStyle.ConnectorPos;
import net.seapanda.bunnyhop.viewprocessor.NodeViewProcessor;

/** 内部に何も表示しないノードビュー. */
public class NoContentNodeView extends BhNodeView {

  private final TextNode model;

  /**
   * コンストラクタ.
   *
   * @param model ビューに対応するモデル
   * @param viewStyle ビューのスタイル
   */
  public NoContentNodeView(TextNode model, BhNodeViewStyle viewStyle) 
      throws ViewInitializationException {
    super(viewStyle, model);
    this.model = model;
    getLookManager().addCssClass(BhConstants.Css.CLASS_NO_CONTENT_NODE);
    setMouseTransparent(true);
  }

  /**
   * このビューのモデルであるBhNodeを取得する.
   *
   * @return このビューのモデルであるBhNode
   */
  @Override
  public Optional<TextNode> getModel() {
    return Optional.ofNullable(model);
  }

  @Override
  protected void arrangeAndResize() {
    boolean inner = (parent == null) ? true : parent.inner;
    if (inner) {
      getLookManager().setBodyShape(viewStyle.bodyShape);
    } else {
      getLookManager().setBodyShape(BodyShape.BODY_SHAPE_NONE);
    }
    getLookManager().updatePolygonShape();
  }

  @Override
  protected Vec2D getBodySize(boolean includeCnctr) {
    double paddingLeft = 0.0;
    double paddingRight = 0.0;
    double paddingTop = 0.0;
    double paddingBottom = 0.0;

    boolean inner = (parent == null) ? true : parent.inner;
    if (inner) {
      paddingLeft = viewStyle.paddingLeft;
      paddingRight = viewStyle.paddingRight;
      paddingTop = viewStyle.paddingTop;
      paddingBottom = viewStyle.paddingBottom;
    }

    Vec2D cnctrSize = viewStyle.getConnectorSize(isFixed());
    double bodyWidth = paddingLeft + paddingRight;
    if (includeCnctr && (viewStyle.connectorPos == ConnectorPos.LEFT)) {
      bodyWidth += cnctrSize.x;
    }
    double bodyHeight = paddingTop + paddingBottom;
    if (includeCnctr && (viewStyle.connectorPos == ConnectorPos.TOP)) {
      bodyHeight += cnctrSize.y;
    }
    return new Vec2D(bodyWidth, bodyHeight);
  }

  @Override
  protected Vec2D getNodeSizeIncludingOuter(boolean includeCnctr) {
    return getBodySize(includeCnctr);
  }

  @Override
  public void accept(NodeViewProcessor visitor) {
    visitor.visit(this);
  }

  @Override
  public void show(int depth) {
    MsgPrinter.INSTANCE.println(
        "%s<NoContentNodeView>  %s".formatted(indent(depth), hashCode()));
  }
}
