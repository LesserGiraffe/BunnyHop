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

package net.seapanda.bunnyhop.view.connectionline;

import javafx.scene.Group;
import javafx.scene.paint.Paint;
import javafx.scene.shape.Line;
import javafx.scene.shape.Shape;
import net.seapanda.bunnyhop.common.Rem;
import net.seapanda.bunnyhop.model.workspace.Workspace;
import net.seapanda.bunnyhop.utility.Pair;
import net.seapanda.bunnyhop.utility.math.Vec2D;
import net.seapanda.bunnyhop.view.node.BhNodeView;

/**
 * {@link BhNodeView} を繋ぐ線を表すクラス.
 *
 * @author K.Koike
 */
public class ConnectionLine {

  /** 始点の先端の形. */
  private final TipType startPointTipType;
  /** 終点の先端の形. */
  private final TipType endPointTipType;
  /** 線の種類. */
  private final LineType lineType;
  /** 線の色. */
  private final Paint color;
  /** 始点となる {@link NodeView}. */
  private final BhNodeView startNode;
  /** 終点となる {@link NodeView}. */
  private final BhNodeView endNode;
  /** 接続線の GUI 部品を持つグループ. */
  private final Group shapeGroup = new Group();
  /** 始点パーツ. */
  private Shape startPointTip;
  /** 終点パーツ. */
  private Shape endPointTip;
  /** 線パーツ. */
  private Line line;

  private static double ARROW_LENGTH = 5.0 * Rem.VAL;
  private static double ARROW_ANGLE_OF_OPENING = Math.toRadians(30.0);
  private static double STROKE_WIDTH = 0.5 * Rem.VAL;
  private static double STROKE_DASH_LEN = 0.5 * Rem.VAL;

  /** 先端の形. */
  public enum TipType {
    /** 矢印. */
    ARROW,
    /** 装飾無し. */
    NONE,
  }

  /** 線の種類. */
  public enum LineType {
    /** 実線. */
    SOLID,
    /** 点線. */
    DOTTED,
  }

  /** コンストラクタ. */
  private ConnectionLine(
      TipType startPointTipType,
      TipType endPointTipType,
      LineType lineType,
      Paint color,
      BhNodeView startNode,
      BhNodeView endNode) {

    this.startPointTipType = startPointTipType;
    this.endPointTipType = endPointTipType;
    this.lineType = lineType;
    this.color = color;
    this.startNode = startNode;
    this.endNode = endNode;
  }

  /**
   * BhNodeView を繋ぐ線オブジェクトを作成する.
   *
   * @param startPointTipType 始点の先端の形
   * @param endPointTipType 終点の先端の形
   * @param lineType 線の種類
   * @param color 色
   * @param startNode 始点となる NodeView
   * @param endNode 終点となる NodeView
   */
  public static ConnectionLine create(
      TipType startPointTipType,
      TipType endPointTipType,
      LineType lineType,
      Paint color,
      BhNodeView startNode,
      BhNodeView endNode) {

    if (startNode == null || endNode == null) {
      throw new IllegalArgumentException(
          "ConnectionLine.create()    'startPoint' and 'endPoint' must not be null");
    }
    var line = new ConnectionLine(
        startPointTipType, endPointTipType, lineType, color, startNode, endNode);
    line.build();
    return line;
  }

  /** 線を構築する. */
  private void build() {
    createTipsOfLine();
    createLine();
    shapeGroup.getChildren().add(line);
    shapeGroup.getChildren().add(startPointTip);
    shapeGroup.getChildren().add(endPointTip);
  }

  /** 接続線のライン部分を作成する. */
  private void createLine() {
    line = new Line();
    line.setStroke(color);
    line.setStrokeWidth(STROKE_WIDTH);
    switch (lineType) {
      case SOLID:
        break;
      case DOTTED:
        line.getStrokeDashArray().addAll(STROKE_DASH_LEN, STROKE_DASH_LEN);
        break;
      default:
        throw new AssertionError("unknown line type");
    }
  }

  /** 接続線の先端部分を作成する. */
  private void createTipsOfLine() {
    switch (startPointTipType) {
      case ARROW:
        startPointTip = TipShapeFactory.createArrow(
          ARROW_ANGLE_OF_OPENING, ARROW_LENGTH, STROKE_WIDTH, 0, color);
        break;

      case NONE:
        startPointTip = TipShapeFactory.createNone();
        break;

      default:
        throw new AssertionError("unknown tip type");
    }

    switch (endPointTipType) {
      case ARROW:
        endPointTip = TipShapeFactory.createArrow(
          ARROW_ANGLE_OF_OPENING, ARROW_LENGTH, STROKE_WIDTH, Math.toRadians(180.0), color);
        break;

      case NONE:
        endPointTip = TipShapeFactory.createNone();
        break;

      default:
        throw new AssertionError("unknown tip type");
    }
  }

  /**
   * 線を表示する.
   * 始点 NodeView と終点 NodeView が同じワークスペースに無い場合は表示しない.
   */
  public void show() {
    Workspace startNodeWs = startNode.getModel().map(node -> node.getWorkspace()).orElse(null);
    Workspace endNodeWs = startNode.getModel().map(node -> node.getWorkspace()).orElse(null);
    if (startNodeWs != null && startNodeWs == endNodeWs) {
      shapeGroup.setVisible(false);
      return;
    }

    shapeGroup.setVisible(true);
    Vec2D startPosOnWs = startNode.getPositionManager().getPosOnWorkspace();
    Vec2D endPosOnWs = endNode.getPositionManager().getPosOnWorkspace();
    Vec2D startNodeSize = startNode.getRegionManager().getNodeSize(false);
    Vec2D endNodeSize = endNode.getRegionManager().getNodeSize(false);

    Pair<Double, Double> pointsX =
        calcConnectionPointsX(startPosOnWs, endPosOnWs, startNodeSize, endNodeSize);
    Pair<Double, Double> pointsY =
        calcConnectionPointsY(startPosOnWs, endPosOnWs, startNodeSize, endNodeSize);
    double lenX = pointsX.v2 - pointsX.v1;
    double lenY = pointsY.v2 - pointsY.v1;
    double lineLen = Math.sqrt(lenX * lenX + lenY * lenY);
    line.setEndX(lineLen);
    endPointTip.setTranslateX(lineLen);
    shapeGroup.setTranslateX(pointsX.v1);
    shapeGroup.setTranslateY(pointsY.v1);
    double rotAngle = Math.atan2(-(pointsY.v2 - pointsY.v1), pointsX.v2 - pointsX.v1);
    shapeGroup.setRotate(rotAngle);
  }

  /**
   * 接続点のX座標を求める.
   *
   * @return 始点ノードの X 位置と終点ノードの X 位置のペア
   */
  private Pair<Double, Double> calcConnectionPointsX(
      Vec2D startPosOnWs, Vec2D endPosOnWs, Vec2D startNodeSize, Vec2D endNodeSize) {
    double startLeft = startPosOnWs.x;
    double startRight = startLeft + startNodeSize.x;
    double endLeft = endPosOnWs.x;
    double endRight = endLeft + endNodeSize.x;

    double startPointX = startLeft;
    double endPointX = endLeft;
    double lenX = Math.abs(endLeft - startLeft);

    double tmpLen = Math.abs(endLeft - startRight);
    if (tmpLen < lenX) {
      startPointX = startRight;
      endPointX = endLeft;
      lenX = tmpLen;
    }

    tmpLen = Math.abs(endRight - startLeft);
    if (tmpLen < lenX) {
      startPointX = startLeft;
      endPointX = endRight;
      lenX = tmpLen;
    }

    tmpLen = Math.abs(endRight - startRight);
    if (tmpLen < lenX) {
      startPointX = startRight;
      endPointX = endRight;
    }
    return new Pair<Double, Double>(startPointX, endPointX);
  }

  /**
   * 接続点の Y 座標を求める.
   *
   * @return 始点ノードの Y 位置と終点ノードの Y 位置のペア
   */
  private Pair<Double, Double> calcConnectionPointsY(
      Vec2D startPosOnWs, Vec2D endPosOnWs, Vec2D startNodeSize, Vec2D endNodeSize) {
    double startTop = startPosOnWs.y;
    double startBottom = startTop + startNodeSize.y;
    double endTop = endPosOnWs.y;
    double endBottom = endTop + endNodeSize.y;

    double startPointY = startTop;
    double endPointY = endTop;
    double lenY = Math.abs(endTop - startTop);

    double tmpLen = Math.abs(endTop - startBottom);
    if (tmpLen < lenY) {
      startPointY = startBottom;
      endPointY = endTop;
      lenY = tmpLen;
    }

    tmpLen = Math.abs(endBottom - startTop);
    if (tmpLen < lenY) {
      startPointY = startTop;
      endPointY = endBottom;
      lenY = tmpLen;
    }

    tmpLen = Math.abs(endBottom - startBottom);
    if (tmpLen < lenY) {
      startPointY = startBottom;
      endPointY = endBottom;
    }
    return new Pair<Double, Double>(startPointY, endPointY);
  }
}
