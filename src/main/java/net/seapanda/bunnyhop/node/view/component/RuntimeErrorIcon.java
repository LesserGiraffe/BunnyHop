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

package net.seapanda.bunnyhop.node.view.component;

import javafx.scene.Group;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.scene.shape.Line;
import net.seapanda.bunnyhop.common.configuration.BhConstants;

/**
 * ランタイムエラーマークを描画するクラス.
 *
 * @author K.Koike
 */
public class RuntimeErrorIcon extends Group {

  /**
   * コンストラクタ.
   *
   * @param radius 丸の半径
   * @param styleClass CSS で指定するクラス
   * @param visible 作成直後の可視性
   */
  public RuntimeErrorIcon(double radius, String styleClass, boolean visible) {
    var circle = new Circle(radius, radius, radius);
    circle.setFill(Color.YELLOW);
    circle.setStroke(Color.GOLD);
    circle.getStyleClass().add(BhConstants.Css.Class.CIRCLE);

    final double width = 2 * radius;
    final double height = 2 * radius;

    // エクスクラメーションマークの縦棒部分（長方形）
    double startY = height * 0.22;
    double barHeight = height * 0.33;
    var exclamationBar = new Line(radius, startY, radius, startY + barHeight);
    exclamationBar.getStyleClass().add(BhConstants.Css.Class.EXCLAMATION_BAR);

    // エクスクラメーションマークの点部分（円）
    Circle exclamationDot = new Circle();
    exclamationDot.setCenterX(radius);
    exclamationDot.setCenterY(height * 0.8);
    exclamationDot.setRadius(width * 0.09);
    exclamationDot.getStyleClass().add(BhConstants.Css.Class.EXCLAMATION_DOT);
    getChildren().addAll(circle, exclamationBar, exclamationDot);
    setMouseTransparent(true);
    getStyleClass().add(styleClass);
    setVisible(visible);
  }
}
