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
import java.util.SequencedSet;
import javafx.scene.Node;
import javafx.scene.control.Label;
import net.seapanda.bunnyhop.common.BhConstants;
import net.seapanda.bunnyhop.model.node.TextNode;
import net.seapanda.bunnyhop.utility.SimpleCache;
import net.seapanda.bunnyhop.utility.Vec2D;
import net.seapanda.bunnyhop.view.ViewConstructionException;
import net.seapanda.bunnyhop.view.node.style.BhNodeViewStyle;
import net.seapanda.bunnyhop.view.node.style.BhNodeViewStyle.ConnectorPos;
import net.seapanda.bunnyhop.view.traverse.NodeViewWalker;

/**
 * ラベルを入力フォームに持つビュー.
 *
 * @author K.Koike
 */
public final class LabelNodeView extends BhNodeViewBase {

  private Label label = new Label();
  private final TextNode model;
  /** コネクタ部分を含まないノードサイズのキャッシュデータ. */
  private SimpleCache<Vec2D> nodeSizeCache = new SimpleCache<Vec2D>(new Vec2D());
  /** コネクタ部分を含むノードサイズのキャッシュデータ. */
  private SimpleCache<Vec2D> nodeWithCnctrSizeCache = new SimpleCache<Vec2D>(new Vec2D());

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
    addComponent(label);
    initStyle();
  }

  /**
   * コンストラクタ.
   *
   * @param viewStyle このノードビューのスタイル
   * @throws ViewConstructionException ノードビューの初期化に失敗
   */
  public LabelNodeView(BhNodeViewStyle viewStyle)
      throws ViewConstructionException {
    this(null, viewStyle, null);
  }

  private void initStyle() {
    label.autosize();
    label.setMouseTransparent(true);
    label.setTranslateX(viewStyle.paddingLeft);
    label.setTranslateY(viewStyle.paddingTop);
    label.getStyleClass().add(viewStyle.label.cssClass);
    label.heightProperty().addListener(newValue -> onNodeSizeChanged());
    label.widthProperty().addListener(newValue -> onNodeSizeChanged());
    getLookManager().addCssClass(BhConstants.Css.CLASS_LABEL_NODE);
  }

  /** ノードサイズのキャッシュを dirty にして, ノードの大きさが変わったことを親グループに伝える. */
  private void onNodeSizeChanged() {
    nodeSizeCache.setDirty(true);
    nodeWithCnctrSizeCache.setDirty(true);
    BhNodeViewGroup group = getTreeManager().getParentGroup();
    if (group != null) {
      group.notifyChildSizeChanged();
    }
    if (getTreeManager().isRootView()) {
      getLookManager().requestArrangement();
    }
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
  public Optional<TextNode> getModel() {
    return Optional.ofNullable(model);
  }

  @Override
  public void show(int depth) {
    System.out.println("%s<LabelView>  %s".formatted(indent(depth), hashCode()));
    System.out.println("%s<content>  %s".formatted(indent(depth + 1), label.getText()));
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

    Vec2D cnctrSize = viewStyle.getConnectorSize(isFixed());
    double bodyWidth = viewStyle.paddingLeft + label.getWidth() + viewStyle.paddingRight;
    if (includeCnctr && (viewStyle.connectorPos == ConnectorPos.LEFT)) {
      bodyWidth += cnctrSize.x;
    }
    double bodyHeight = viewStyle.paddingTop + label.getHeight() + viewStyle.paddingBottom;
    if (includeCnctr && (viewStyle.connectorPos == ConnectorPos.TOP)) {
      bodyHeight += cnctrSize.y;
    }
    var nodeSize = new Vec2D(bodyWidth, bodyHeight);
    updateNodeSizeCache(includeCnctr, nodeSize);
    return nodeSize;
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
