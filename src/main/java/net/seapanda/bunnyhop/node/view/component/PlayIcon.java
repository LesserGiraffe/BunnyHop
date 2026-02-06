package net.seapanda.bunnyhop.node.view.component;

import javafx.scene.Group;
import javafx.scene.shape.Circle;
import javafx.scene.shape.Polygon;
import net.seapanda.bunnyhop.common.configuration.BhConstants;

/**
 * 再生マークを描画するクラス.
 *
 * @author K.Koike
 */
public class PlayIcon extends Group {

  /**
   * コンストラクタ.
   *
   * @param radius 再生マークの半径.
   * @param styleClass CSS で指定するクラス
   */
  public PlayIcon(double radius, String styleClass) {
    // 円を作成
    Circle circle = new Circle(radius);
    circle.getStyleClass().add(BhConstants.Css.Class.CIRCLE);

    // 右向きの三角形を作成
    double triangleSize = radius * 0.9;
    double offset = radius * 0.1;
    Polygon triangle = new Polygon();
    triangle.getPoints().addAll(
        -triangleSize / 2 + offset, -triangleSize / 1.5,
        -triangleSize / 2 + offset, triangleSize / 1.5,
        triangleSize * 2 / 3 + offset, 0.0
    );
    triangle.getStyleClass().add(BhConstants.Css.Class.TRIANGLE);

    getChildren().addAll(circle, triangle);
    setMouseTransparent(true);
    getStyleClass().add(styleClass);
  }
}
