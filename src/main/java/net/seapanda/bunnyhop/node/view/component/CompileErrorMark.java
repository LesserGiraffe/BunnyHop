package net.seapanda.bunnyhop.node.view.component;

import javafx.scene.Group;
import javafx.scene.shape.Line;

/**
 * コンパイルエラーマークを描画するクラス.
 *
 * @author K.Koike
 */
public class CompileErrorMark extends Group {

  private final Line line;

  /**
   * コンストラクタ.
   *
   * @param width マークの幅
   * @param height マークの高さ
   * @param styleClass CSS で指定するクラス
   * @param visible 作成直後の可視性
   */
  public CompileErrorMark(double width, double height, String styleClass, boolean visible) {
    line = new Line(0, 0, width, height);
    line.getStyleClass().add(styleClass);
    getChildren().addAll(line);
    setMouseTransparent(true);
    setVisible(visible);
  }

  /** マークの大きさを変更する. */
  public void setSize(double width, double height) {
    line.setEndX(width);
    line.setEndY(height);
  }
}
