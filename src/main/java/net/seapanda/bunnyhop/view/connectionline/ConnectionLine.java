package net.seapanda.bunnyhop.view.connectionline;

import javafx.scene.Group;
import javafx.scene.paint.Paint;
import javafx.scene.shape.Line;
import javafx.scene.shape.Shape;
import net.seapanda.bunnyhop.common.Pair;
import net.seapanda.bunnyhop.common.Vec2D;
import net.seapanda.bunnyhop.common.constant.Rem;
import net.seapanda.bunnyhop.view.ViewHelper;
import net.seapanda.bunnyhop.view.node.BhNodeView;
/**
 * BhNodeView を繋ぐ線を表すクラス
 * @author K.Koike
 */
public class ConnectionLine {

  private final TIP_TYPE startPointTipType;  //!< 始点の先端の形
  private final TIP_TYPE endPointTipType;  //!< 終点の先端の形
  private final LINE_TYPE lineType;  //!< 線の種類
  private final Paint color;  ///!< 色
  private final BhNodeView startNode;  //!< 始点となる NodeView
  private final BhNodeView endNode;  //!< 終点となる NodeView
  private final Group shapeGroup = new Group();  //!< 接続線のGUI部品を持つグループ
  private Shape startPointTip;  //!< 始点パーツ
  private Shape endPointTip;  //!< 終点パーツ
  private Line line;  //!< 線パーツ

  private static double ARROW_LENGTH = 5.0 * Rem.VAL;
  private static double ARROW_ANGLE_OF_OPENING = Math.toRadians(30.0);
  private static double STROKE_WIDTH = 0.5 * Rem.VAL;
  private static double STROKE_DASH_LEN = 0.5 * Rem.VAL;

  /**
   * 先端の形
   */
  public enum TIP_TYPE {
    ARROW,  //!< 矢印
    NONE,  //!< 装飾無し
  }

  /**
   * 線の種類
   */
  public enum LINE_TYPE {
    SOLID,  //!< 実線
    DOTTED,  //!<点線
  }

  public ConnectionLine(
    TIP_TYPE startPointTipType, TIP_TYPE endPointTipType, LINE_TYPE lineType, Paint color,
    BhNodeView startNode, BhNodeView endNode) {

    this.startPointTipType = startPointTipType;
    this.endPointTipType = endPointTipType;
    this.lineType = lineType;
    this.color = color;
    this.startNode = startNode;
    this.endNode = endNode;
  }

  /**
   * BhNodeView を繋ぐ線オブジェクトを作成する
   * @param startPointTipType 始点の先端の形
   * @param endPointTipType 終点の先端の形
   * @param lineType 線の種類
   * @param color 色
   * @param startNode 始点となる NodeView
   * @param endNode 終点となる NodeView
   */
  public static ConnectionLine create(
    TIP_TYPE startPointTipType, TIP_TYPE endPointTipType, LINE_TYPE lineType, Paint color,
    BhNodeView startNode, BhNodeView endNode) {

    if (startNode == null || endNode == null)
      throw new IllegalArgumentException(
        "ConnectionLine.create()    'startPoint' and 'endPoint' must not be null");

    var line = new ConnectionLine(startPointTipType, endPointTipType, lineType, color, startNode, endNode);
    line.build();
    return line;
  }

  /**
   * 線を構築する
   */
  private void build() {

    createTipsOfLine();
    createLine();
    shapeGroup.getChildren().add(line);
    shapeGroup.getChildren().add(startPointTip);
    shapeGroup.getChildren().add(endPointTip);
  }

  /**
   * 接続線のライン部分を作成する
   */
  private void createLine() {

    line = new Line();
    line.setStroke(color);
    line.setStrokeWidth(STROKE_WIDTH);
    switch (lineType) {
      case SOLID:
        break;
      case DOTTED:
        line.getStrokeDashArray().addAll(STROKE_DASH_LEN, STROKE_DASH_LEN);
      default:
        throw new AssertionError("ConnectionLine.build()    unknown line type");
    }
  }

  /**
   * 接続線の先端部分を作成する
   */
  private void createTipsOfLine() {

    switch (startPointTipType) {
      case ARROW :
        startPointTip = TipShapeFactory.createArrow(
          ARROW_ANGLE_OF_OPENING, ARROW_LENGTH, STROKE_WIDTH, 0, color);
        break;

      case NONE:
        startPointTip = TipShapeFactory.createNone();
        break;

      default:
        throw new AssertionError("ConnectionLine.build()    unknown tip type");
    }

    switch (endPointTipType) {
      case ARROW :
        endPointTip = TipShapeFactory.createArrow(
          ARROW_ANGLE_OF_OPENING, ARROW_LENGTH, STROKE_WIDTH, Math.toRadians(180.0), color);
        break;

      case NONE:
        endPointTip = TipShapeFactory.createNone();
        break;

      default:
        throw new AssertionError("ConnectionLine.build()    unknown tip type");
    }
  }

  /**
   * 線を表示する. <br>
   * 始点 NodeView と終点 NodeView が同じワークスペースに無い場合は表示しない.
   */
  public void show() {

    if (!areInSameWorkspaceView(startNode, endNode)) {
      shapeGroup.setVisible(false);
      return;
    }

    shapeGroup.setVisible(true);
    Vec2D startPosOnWs = ViewHelper.INSTANCE.getPosOnWorkspace(startNode);
    Vec2D endPosOnWs = ViewHelper.INSTANCE.getPosOnWorkspace(endNode);
    Vec2D startNodeSize = startNode.getRegionManager().getBodySize(false);
    Vec2D endNodeSize = endNode.getRegionManager().getBodySize(false);

    Pair<Double, Double> xPoints = calcConnectionPointX(startPosOnWs, endPosOnWs, startNodeSize, endNodeSize);
    Pair<Double, Double> yPoints = calcConnectionPointY(startPosOnWs, endPosOnWs, startNodeSize, endNodeSize);
    double xLen = xPoints._2 - xPoints._1;
    double yLen = yPoints._2 - yPoints._1;
    double lineLen = Math.sqrt(xLen * xLen + yLen * yLen);
    line.setEndX(lineLen);
    endPointTip.setTranslateX(lineLen);
    shapeGroup.setTranslateX(xPoints._1);
    shapeGroup.setTranslateY(yPoints._1);
    double rotAngle = Math.atan2(-(yPoints._2 - yPoints._1), xPoints._2 - xPoints._1);
    shapeGroup.setRotate(rotAngle);
  }

  /**
   * 接続点のX座標を求める
   * @return 始点ノードの X 位置と終点ノードの X 位置のペア
   */
  private Pair<Double, Double> calcConnectionPointX(
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
   * 接続点のY座標を求める
   * @return 始点ノードの Y 位置と終点ノードの Y 位置のペア
   * */
  private Pair<Double, Double> calcConnectionPointY(
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

  /**
   * 引数の2つの BhNodeView が同じワークスペースビューにあるかどうか調べる
   */
  private boolean areInSameWorkspaceView(BhNodeView viewA, BhNodeView viewB) {
    return ViewHelper.INSTANCE.getWorkspaceView(viewA) == ViewHelper.INSTANCE.getWorkspaceView(viewB);
  }
}



























