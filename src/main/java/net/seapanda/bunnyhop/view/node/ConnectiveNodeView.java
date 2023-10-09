/**
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

import net.seapanda.bunnyhop.common.Vec2D;
import net.seapanda.bunnyhop.common.constant.BhParams;
import net.seapanda.bunnyhop.common.tools.MsgPrinter;
import net.seapanda.bunnyhop.model.node.connective.ConnectiveNode;
import net.seapanda.bunnyhop.view.ViewInitializationException;
import net.seapanda.bunnyhop.view.node.part.BhNodeViewStyle;
import net.seapanda.bunnyhop.view.node.part.BhNodeViewStyle.CNCTR_POS;
import net.seapanda.bunnyhop.viewprocessor.NodeViewProcessor;

/**
 * ConnectiveNode に対応するビュークラス
 * @author K.Koike
 */
public final class ConnectiveNodeView extends BhNodeView {

  private final BhNodeViewGroup innerGroup = new BhNodeViewGroup(this, true); //!< ノード内部に描画されるノードのGroup
  private final BhNodeViewGroup outerGroup = new BhNodeViewGroup(this, false); //!< ノード外部に描画されるノードのGroup
  private ConnectiveNode model;

  /**
   * コンストラクタ
   * @param model このノードビューに対応するノード
   * @param viewStyle このノードビューのスタイル
   * @throws ViewInitializationException ノードビューの初期化に失敗
   */
  public ConnectiveNodeView(ConnectiveNode model, BhNodeViewStyle viewStyle)
    throws ViewInitializationException {

    super(viewStyle, model);
    this.model = model;
    innerGroup.buildSubGroup(viewStyle.connective.inner);
    outerGroup.buildSubGroup(viewStyle.connective.outer);
    getPositionManager().setOnAbsPosUpdated(this::updateAbsPos);
    getAppearanceManager().addCssClass(BhParams.CSS.CLASS_CONNECTIVE_NODE);
  }

  /**
   * このビューのモデルであるBhNodeを取得する
   * @return このビューのモデルであるBhNode
   */
  @Override
  public ConnectiveNode getModel() {
    return model;
  }

  /**
   * ノード内部に描画されるノードを追加する
   * @param view ノード内部に描画されるノード
   * */
  public void addToGroup(BhNodeView view) {

    // innerGroup に追加できなかったらouterGroupに入れる
    if (!innerGroup.addNodeView(view))
      outerGroup.addNodeView(view);
  }

  /**
   * このノード以下のグループの絶対位置を更新する
   * @param posX ノードの絶対位置 X
   * @param posY ノードの絶対位置 Y
   */
  private void updateAbsPos(double posX, double posY) {

    //内部ノード絶対位置更新
    Vec2D relativePos = innerGroup.getRelativePosFromParent();
    innerGroup.updateAbsPos(posX + relativePos.x, posY + relativePos.y);

    //外部ノード絶対位置更新
    Vec2D bodySize = getRegionManager().getBodySize(false);
    //外部ノードが右に繋がる
    if (viewStyle.connectorPos == CNCTR_POS.LEFT)
      outerGroup.updateAbsPos(posX + bodySize.x, posY);
     //外部ノードが下に繋がる
    else
      outerGroup.updateAbsPos(posX, posY + bodySize.y);
  }

  @Override
  protected void arrangeAndResize() {

    getAppearanceManager().updatePolygonShape();
    updateChildRelativePos();
  }

  @Override
  protected Vec2D getBodySize(boolean includeCnctr) {

    Vec2D innerSize = innerGroup.getSize();
    Vec2D cnctrSize = viewStyle.getConnectorSize();

    double bodyWidth = viewStyle.paddingLeft + innerSize.x + viewStyle.paddingRight;
    if (includeCnctr && (viewStyle.connectorPos == CNCTR_POS.LEFT))
      bodyWidth += cnctrSize.x;

    double bodyHeight = viewStyle.paddingTop + innerSize.y + viewStyle.paddingBottom;
    if (includeCnctr && (viewStyle.connectorPos == CNCTR_POS.TOP))
      bodyHeight += cnctrSize.y;

    return new Vec2D(bodyWidth, bodyHeight);
  }

  @Override
  protected Vec2D getNodeSizeIncludingOuter(boolean includeCnctr) {

    Vec2D bodySize = getBodySize(includeCnctr);
    Vec2D outerSize = outerGroup.getSize();
    double totalWidth = bodySize.x;
    double totalHeight = bodySize.y;

    //外部ノードが右に接続される
    if (viewStyle.connectorPos == CNCTR_POS.LEFT) {
      totalWidth += outerSize.x;
      totalHeight = Math.max(totalHeight, outerSize.y);
    }
    //外部ノードが下に接続される
    else {
      totalWidth = Math.max(totalWidth, outerSize.x);
      totalHeight += outerSize.y;
    }

    return new Vec2D(totalWidth, totalHeight);
  }

  /**
   * ノードの親からの相対位置を指定する
   */
  private void updateChildRelativePos() {

    innerGroup.setRelativePosFromParent(viewStyle.paddingLeft, viewStyle.paddingTop);
    Vec2D bodySize = getRegionManager().getBodySize(false);
    if (viewStyle.connectorPos == CNCTR_POS.LEFT)  //外部ノードが右に繋がる
      outerGroup.setRelativePosFromParent(bodySize.x, 0.0);
    else                       //外部ノードが下に繋がる
      outerGroup.setRelativePosFromParent(0.0, bodySize.y);
  }

  /**
   * visitor を内部ノードを管理するグループに渡す
   * @param visitor 内部ノードを管理するグループに渡す visitor
   * */
  public void sendToInnerGroup(NodeViewProcessor visitor) {
    innerGroup.accept(visitor);
  }

  /**
   * visitor を外部ノードを管理するグループに渡す
   * @param visitor 外部ノードを管理するグループに渡す visitor
   * */
  public void sendToOuterGroup(NodeViewProcessor visitor) {
    outerGroup.accept(visitor);
  }

  @Override
  public void accept(NodeViewProcessor visitor) {
    visitor.visit(this);
  }

  @Override
  public void show(int depth) {

    try {
      MsgPrinter.INSTANCE.msgForDebug(indent(depth) + "<ConnectiveNodeView" + ">   " + this.hashCode());
      innerGroup.show(depth + 1);
      outerGroup.show(depth + 1);
    }
    catch (Exception e) {
      MsgPrinter.INSTANCE.msgForDebug("connectiveNodeView show exception " + e);
    }
  }
}











