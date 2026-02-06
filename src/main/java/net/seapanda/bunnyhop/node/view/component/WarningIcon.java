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
import javafx.scene.shape.Polygon;
import javafx.scene.shape.Rectangle;
import net.seapanda.bunnyhop.common.configuration.BhConstants;

/**
 * 警告マークを描画するクラス.
 *
 * @author K.Koike
 */
public class WarningIcon extends Group {

  /**
   * コンストラクタ.
   *
   * @param width マークの幅
   * @param height マークの高さ
   * @param styleClass CSS で指定するクラス
   */
  public WarningIcon(double width, double height, String styleClass) {
    // 黄色い三角形を作成
    Polygon triangle = new Polygon();
    double centerX = width / 2;
    double topY = height * 0.1;
    double bottomY = height * 0.9;
    double leftX = width * 0.05;
    double rightX = width * 0.95;

    triangle.getPoints().addAll(
        centerX, topY,      // 上の頂点
        leftX, bottomY,     // 左下の頂点
        rightX, bottomY     // 右下の頂点
    );
    triangle.setFill(Color.YELLOW);
    triangle.setStroke(Color.BLACK);
    triangle.setStrokeWidth(width * 0.03);
    triangle.getStyleClass().add(BhConstants.Css.Class.TRIANGLE);

    // エクスクラメーションマークの縦棒部分（長方形）
    Rectangle exclamationBar = new Rectangle();
    double barWidth = width * 0.08;
    double barHeight = height * 0.35;
    exclamationBar.setX(centerX - barWidth / 2);
    exclamationBar.setY(height * 0.3);
    exclamationBar.setWidth(barWidth);
    exclamationBar.setHeight(barHeight);
    exclamationBar.setFill(Color.BLACK);
    exclamationBar.getStyleClass().add(BhConstants.Css.Class.EXCLAMATION_BAR);

    // エクスクラメーションマークの点部分（円）
    Circle exclamationDot = new Circle();
    exclamationDot.setCenterX(centerX);
    exclamationDot.setCenterY(height * 0.75);
    exclamationDot.setRadius(width * 0.04);
    exclamationDot.setFill(Color.BLACK);
    exclamationDot.getStyleClass().add(BhConstants.Css.Class.EXCLAMATION_DOT);

    // すべての図形をグループに追加
    getChildren().addAll(triangle, exclamationBar, exclamationDot);
    setMouseTransparent(true);
    getStyleClass().add(styleClass);
  }
}
