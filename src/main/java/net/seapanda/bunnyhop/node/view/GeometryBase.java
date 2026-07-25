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

import static net.seapanda.bunnyhop.node.view.BhNodeViewBase.Panes;
import static net.seapanda.bunnyhop.node.view.BhNodeViewBase.ViewOrderOffset;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import javafx.geometry.BoundingBox;
import javafx.geometry.Bounds;
import net.seapanda.bunnyhop.node.view.BhNodeView.Geometry;
import net.seapanda.bunnyhop.node.view.bodyshape.BodyShapeType;
import net.seapanda.bunnyhop.node.view.style.BhNodeViewStyle;
import net.seapanda.bunnyhop.node.view.style.ConnectorAlignment;
import net.seapanda.bunnyhop.node.view.style.ConnectorOrientation;
import net.seapanda.bunnyhop.ui.view.ViewUtil;
import net.seapanda.bunnyhop.utility.math.Vec2D;
import net.seapanda.bunnyhop.workspace.view.WorkspaceView;
import net.seapanda.bunnyhop.workspace.view.quadtree.QuadTreeItem;
import org.apache.commons.lang3.mutable.MutableDouble;

/**
 * ノードビューの幾何形状に関する操作を提供するクラス.
 *
 * @author K.Koike
 */
abstract class GeometryBase implements Geometry {

  private final BhNodeViewBase view;
  /** ボディ部分のの範囲を保持するオブジェクト. */
  private final QuadTreeItem bodyItem;
  /** コネクタ部分のの範囲を保持するオブジェクト. */
  private final QuadTreeItem cnctrItem;
  /** 親 {@link BhNodeViewGroup} からの相対位置. */
  private final Vec2D relativePos = new Vec2D();

  /** 関連するノードビューの子孫要素の親からの相対位置を更新する. */
  abstract void updateDescendantRelativePositions();

  /**
   * 関連するノードビュー以下の子孫要素の位置を更新する.
   *
   * @param posX このノードのボディ部分の左上の X 位置
   * @param posY このノードのボディ部分の左上の Y 位置
   */
  abstract void updateTreePosition(double posX, double posY);

  /** コンストラクタ. */
  GeometryBase(BhNodeViewBase view, QuadTreeItem bodyItem, QuadTreeItem cnctrItem) {
    this.view = view;
    this.bodyItem = bodyItem;
    this.cnctrItem = cnctrItem;
  }

  @Override
  public ConnectorOrientation getConnectorOrientation() {
    return view.getStyle().connectorOrientation;
  }

  @Override
  public List<BhNodeView> findOverlappedNodeViews() {
    return cnctrItem.findOverlappedItems(QuadTreeItem.OverlapOption.INTERSECT)
        .stream()
        .map(QuadTreeItem::<BhNodeView>getUserData)
        .collect(Collectors.toCollection(ArrayList::new));
  }

  @Override
  public boolean overlapsWith(BhNodeView view, QuadTreeItem.OverlapOption option) {
    if (view.getGeometry() instanceof GeometryBase rm) {
      return bodyItem.overlapsWith(rm.bodyItem, option);
    }
    return false;
  }

  @Override
  public Vec2D getConnectorSize() {
    return view.getStyle().getConnectorSize(view.isFixed());
  }

  @Override
  public Vec2D getNotchSize() {
    return view.getStyle().getNotchSize(view.isFixed());
  }

  @Override
  public Bounds getBodyBounds() {
    return new BoundingBox(
        bodyItem.getMinX(), bodyItem.getMinY(), bodyItem.getWidth(), bodyItem.getHeight());
  }

  @Override
  public Bounds getConnectorBounds() {
    return new BoundingBox(
        cnctrItem.getMinX(), cnctrItem.getMinY(), cnctrItem.getWidth(), cnctrItem.getHeight());
  }

  @Override
  public Vec2D getBodyPosFromConnector() {
    Vec2D relPos = new Vec2D();
    Vec2D cnctrSize = getConnectorSize();
    Vec2D bodySize = getNodeSize(false);
    BhNodeViewStyle style = view.getStyle();
    if (style.connectorOrientation == ConnectorOrientation.LEFT) {
      relPos.x += cnctrSize.x;
      relPos.y -= style.connectorShift;
      if (style.connectorAlignment == ConnectorAlignment.CENTER) {
        relPos.y += (cnctrSize.y - bodySize.y) / 2;
      }
    } else if (style.connectorOrientation == ConnectorOrientation.TOP) {
      relPos.x -= style.connectorShift;
      relPos.y += cnctrSize.y;
      if (style.connectorAlignment == ConnectorAlignment.CENTER) {
        relPos.x += (cnctrSize.x - bodySize.x) / 2;
      }
    }
    return relPos;
  }

  @Override
  public Vec2D getPosition() {
    return bodyItem.getUpperLeft();
  }

  @Override
  public void setTreePosition(double posX, double posY) {
    updateTreePosition(posX, posY);
    NvbCallbackInvoker.invoke(
        nodeView -> nodeView.getCallbackRegistry().onMoved(),
        view);
  }

  /**
   * 四分木空間上での位置を更新する.
   *
   * @param posX 本体部分左上のX位置
   * @param posY 本体部分左上のY位置
   */
  void setPositionsOnQuadTreeSpace(double posX, double posY) {
    BhNodeViewStyle style = view.getStyle();
    Vec2D bodySize = getNodeSize(false);
    final double bodyLowerRightX = posX + bodySize.x;
    final double bodyLowerRightY = posY + bodySize.y;
    double cnctrUpperLeftX = 0.0;
    double cnctrUpperLeftY = 0.0;
    double cnctrLowerRightX = 0.0;
    double cnctrLowerRightY = 0.0;
    double boundsWidth = style.connectorWidth * style.connectorBoundsRate;
    double boundsHeight = style.connectorHeight * style.connectorBoundsRate;
    double alignOffsetX = 0.0;
    double alignOffsetY = 0.0;
    if (style.connectorAlignment == ConnectorAlignment.CENTER) {
      alignOffsetX = (bodySize.x - style.connectorWidth) / 2.0;
      alignOffsetY = (bodySize.y - style.connectorHeight) / 2.0;
    }
    if (style.connectorOrientation == ConnectorOrientation.LEFT) {
      cnctrUpperLeftX = posX - (boundsWidth + style.connectorWidth) / 2.0;
      cnctrUpperLeftY = (posY + alignOffsetY)
          - (boundsHeight - style.connectorHeight) / 2.0 + style.connectorShift;
      cnctrLowerRightX = cnctrUpperLeftX + boundsWidth;
      cnctrLowerRightY = cnctrUpperLeftY + boundsHeight;
    } else if (style.connectorOrientation == ConnectorOrientation.TOP) {
      cnctrUpperLeftX = (posX + alignOffsetX)
          - (boundsWidth - style.connectorWidth) / 2.0 + style.connectorShift;
      cnctrUpperLeftY = posY - (boundsHeight + style.connectorHeight) / 2.0;
      cnctrLowerRightX = cnctrUpperLeftX + boundsWidth;
      cnctrLowerRightY = cnctrUpperLeftY + boundsHeight;
    }
    bodyItem.setPos(posX, posY, bodyLowerRightX, bodyLowerRightY);
    cnctrItem.setPos(cnctrUpperLeftX, cnctrUpperLeftY, cnctrLowerRightX, cnctrLowerRightY);
  }

  @Override
  public void setTreePositionByUpperLeft(double posX, double posY) {
    BhNodeViewStyle style = view.getStyle();
    Vec2D cnctrSize = getConnectorSize();
    Vec2D offset = new Vec2D();
    if (style.connectorOrientation == ConnectorOrientation.LEFT) {
      offset.x = cnctrSize.x;
      offset.y = -style.connectorShift;
      if (style.connectorAlignment == ConnectorAlignment.CENTER) {
        offset.y += (style.connectorHeight - getNodeSize(false).y) / 2;
      }
      offset.y = Math.max(offset.y, 0);
    } else if (style.connectorOrientation == ConnectorOrientation.TOP) {
      offset.y = cnctrSize.y;
      offset.x = -style.connectorShift;
      if (style.connectorAlignment == ConnectorAlignment.CENTER) {
        posX += (style.connectorWidth - getNodeSize(false).x) / 2;
      }
      offset.x = Math.max(offset.x, 0);
    }
    setTreePosition(posX + offset.x, posY + offset.y);
  }

  @Override
  public void setTreeZposition(double pos) {
    MutableDouble posZ = new MutableDouble(pos);
    NvbCallbackInvoker.invoke(
        nodeView -> {
          nodeView.getGeometry().setComponentZposition(posZ.getValue());
          posZ.add(ViewOrderOffset.CHILD.value());
        },
        view);
  }

  /** GUI 部品の Z 位置を設定する. */
  private void setComponentZposition(double pos) {
    view.getShapes().compileError().setViewOrder(pos + ViewOrderOffset.COMPILE_ERR_MARK.value());
    view.getPanes().root().setViewOrder(pos + ViewOrderOffset.NODE_BASE.value());
  }

  @Override
  public double getZposition() {
    return view.getPanes().root().getViewOrder();
  }

  @Override
  public void moveTree(double diffX, double diffY) {
    WorkspaceView wsView = view.getWorkspaceView();
    if (wsView == null) {
      return;
    }
    Vec2D wsSize = wsView.getSize();
    Vec2D newPos = ViewUtil.newPosition(new Vec2D(diffX, diffY), wsSize, getPosition());
    setTreePosition(newPos.x, newPos.y);
  }

  @Override
  public void moveTree(Vec2D diff) {
    moveTree(diff.x, diff.y);
  }

  @Override
  public Vec2D sceneToLocal(Vec2D pos) {
    var localPos = view.getPanes().root().sceneToLocal(pos.x, pos.y);
    return new Vec2D(localPos.getX(), localPos.getY());
  }

  @Override
  public Vec2D localToScene(Vec2D pos) {
    var scenePos = view.getPanes().root().localToScene(pos.x, pos.y);
    return new Vec2D(scenePos.getX(), scenePos.getY());
  }

  /** GUI 部品のワークスペース上での位置を更新する. */
  void setComponentPositions(double posX, double posY) {
    view.getPanes().root().setTranslateX(posX);
    view.getPanes().root().setTranslateY(posY);
    view.getShapes().compileError().setTranslateX(posX);
    view.getShapes().compileError().setTranslateY(posY);
  }

  /** ノードの共通部分のサイズを取得する. */
  Vec2D getCommonPartSize() {
    Panes panes = view.getPanes();
    if (!panes.common().isManaged()) {
      return new Vec2D();
    }
    return new Vec2D(panes.common().getWidth(), panes.common().getHeight());
  }

  /**
   * {@link #view} のボディの形状の種類を取得する.
   *
   * @return {@link #view} のボディの形状の種類
   */
  BodyShapeType getBodyShape() {
    BhNodeViewGroup parent = view.getTreeControl().getParentGroup();
    return view.getStyle().getBodyShape(parent == null || parent.inner);
  }

  /**
   * 親ノードまたはグループからの相対位置を取得する.
   *
   * @return 親ノードまたは親グループからの相対位置
   */
  Vec2D getRelativePosition() {
    return new Vec2D(relativePos);
  }

  /**
   * 親ノードまたはグループからの相対位置をセットする.
   *
   * @param posX 親ノードまたは親グループからの X 相対位置
   * @param posY 親ノードまたは親グループからの Y 相対位置
   */
  void setRelativePosition(double posX, double posY) {
    relativePos.x = posX;
    relativePos.y = posY;
  }
}
