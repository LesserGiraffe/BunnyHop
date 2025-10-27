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
import net.seapanda.bunnyhop.node.view.style.BhNodeViewStyle.ConnectorAlignment;
import net.seapanda.bunnyhop.node.view.style.BhNodeViewStyle.ConnectorPos;
import net.seapanda.bunnyhop.node.view.traverse.NodeViewWalker;
import net.seapanda.bunnyhop.ui.view.ViewConstructionException;
import net.seapanda.bunnyhop.utility.SimpleCache;
import net.seapanda.bunnyhop.utility.math.Vec2D;

/**
 * ラベルを入力フォームに持つビュー.
 *
 * @author K.Koike
 */
public final class LabelNodeView extends BhNodeViewBase {

  private final Label label = new Label();
  private final TextNode model;
  /** コネクタ部分を含まないノードサイズのキャッシュデータ. */
  private final SimpleCache<Vec2D> nodeSizeCache = new SimpleCache<Vec2D>(new Vec2D());
  /** コネクタ部分を含むノードサイズのキャッシュデータ. */
  private final SimpleCache<Vec2D> nodeWithCnctrSizeCache = new SimpleCache<Vec2D>(new Vec2D());

  /**
   * コンストラクタ.
   *
   * @param model このノードビューに対応するノード
   * @param viewStyle このノードビューのスタイル
   * @throws ViewConstructionException ノードビューの初期化に失敗
   */
  public LabelNodeView(TextNode model, BhNodeViewStyle viewStyle, SequencedSet<Node> components)
      throws ViewConstructionException {
    super(viewStyle, model, components);
    this.model = model;
    setComponent(label);
    initStyle();
    updateNodeStatusVisibility();
  }

  /**
   * コンストラクタ.
   *
   * @param viewStyle このノードビューのスタイル
   * @throws ViewConstructionException ノードビューの初期化に失敗
   */
  public LabelNodeView(BhNodeViewStyle viewStyle)
      throws ViewConstructionException {
    this(null, viewStyle, new LinkedHashSet<>());
  }

  private void initStyle() {
    label.autosize();
    label.setMouseTransparent(true);
    label.getStyleClass().add(viewStyle.label.cssClass);
    getLookManager().addCssClass(BhConstants.Css.CLASS_LABEL_NODE);
  }

  /** ノードサイズのキャッシュ値を更新する. */
  private void updateNodeSizeCache(boolean includeCnctr, Vec2D nodeSize) {
    if (includeCnctr) {
      nodeWithCnctrSizeCache.update(new Vec2D(nodeSize));
    } else {
      nodeSizeCache.update(new Vec2D(nodeSize));
    }
  }

  @Override
  protected void onNodeSizeChanged() {
    nodeSizeCache.setDirty(true);
    nodeWithCnctrSizeCache.setDirty(true);
  }

  @Override
  public Optional<TextNode> getModel() {
    return Optional.ofNullable(model);
  }

  @Override
  public void show(int depth) {
    System.out.printf("%s<LabelView>  %s%n", indent(depth), hashCode());
    System.out.printf("%s<content>  %s%n", indent(depth + 1), label.getText());
  }

  @Override
  protected void updatePosOnWorkspace(double posX, double posY) {
    getPositionManager().setPosOnWorkspace(posX, posY);
  }

  @Override
  protected Vec2D getNodeSize(boolean includeCnctr) {
    if (includeCnctr && !nodeWithCnctrSizeCache.isDirty()) {
      return new Vec2D(nodeWithCnctrSizeCache.getVal());
    }
    if (!includeCnctr && !nodeSizeCache.isDirty()) {
      return new Vec2D(nodeSizeCache.getVal());
    }

    Vec2D nodeSize = calcBodySize();
    if (includeCnctr) {
      Vec2D cnctrSize = getRegionManager().getConnectorSize();
      if (viewStyle.connectorPos == ConnectorPos.LEFT) {
        nodeSize = calcSizeIncludingLeftConnector(nodeSize, cnctrSize);
      } else if (viewStyle.connectorPos == ConnectorPos.TOP) {
        nodeSize = calcSizeIncludingTopConnector(nodeSize, cnctrSize);
      }
    }
    updateNodeSizeCache(includeCnctr, nodeSize);
    return nodeSize;
  }

  /** ノードのボディ部分のサイズを求める. */
  private Vec2D calcBodySize() {
    Vec2D commonPartSize = getRegionManager().getCommonPartSize();
    Vec2D innerSize = switch (viewStyle.baseArrangement) {
      case ROW ->
        new Vec2D(
            commonPartSize.x + label.getWidth(),
            Math.max(commonPartSize.y, label.getHeight()));
      case COLUMN ->
        new Vec2D(
            Math.max(commonPartSize.x, label.getWidth()),
            commonPartSize.y + label.getHeight());
    };
    return new Vec2D(
        viewStyle.paddingLeft + innerSize.x + viewStyle.paddingRight,
        viewStyle.paddingTop + innerSize.y + viewStyle.paddingBottom);
  }

  /**
   * 左にコネクタがついている場合のコネクタを含んだサイズを求める.
   *
   * @param bodySize ボディ部分のサイズ
   * @param cnctrSize コネクタサイズ
   * @return 左にコネクタがついている場合のコネクタを含んだサイズ
   */
  private Vec2D calcSizeIncludingLeftConnector(Vec2D bodySize, Vec2D cnctrSize) {
    double wholeWidth = bodySize.x + cnctrSize.x;
    // ボディの左上を原点としたときのコネクタの上端の座標
    double cnctrTopPos = viewStyle.connectorShift;
    if (viewStyle.connectorAlignment == ConnectorAlignment.CENTER) {
      cnctrTopPos += (bodySize.y - cnctrSize.y) / 2;
    }
    // ボディの左上を原点としたときのコネクタの下端の座標
    double cnctrBottomPos = cnctrTopPos + cnctrSize.y;
    double wholeHeight = Math.max(cnctrBottomPos, bodySize.y) - Math.min(cnctrTopPos, 0);
    return new Vec2D(wholeWidth, wholeHeight);
  }

  /**
   * 上にコネクタがついている場合のコネクタを含んだサイズを求める.
   *
   * @param bodySize ボディ部分のサイズ
   * @param cnctrSize コネクタサイズ
   * @return 左にコネクタがついている場合のコネクタを含んだサイズ
   */
  private Vec2D calcSizeIncludingTopConnector(Vec2D bodySize, Vec2D cnctrSize) {
    double wholeHeight = bodySize.y + cnctrSize.y;
    // ボディの左上を原点としたときのコネクタの左端の座標
    double cnctrLeftPos = viewStyle.connectorShift;
    if (viewStyle.connectorAlignment == ConnectorAlignment.CENTER) {
      cnctrLeftPos += (bodySize.x - cnctrSize.x) / 2;
    }
    // ボディの左上を原点としたときのコネクタの右端の座標
    double cnctrRightPos = cnctrLeftPos + cnctrSize.x;
    double wholeWidth = Math.max(cnctrRightPos, bodySize.x) - Math.min(cnctrLeftPos, 0);
    return new Vec2D(wholeWidth, wholeHeight);
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
