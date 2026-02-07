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
import javafx.scene.shape.Circle;
import javafx.scene.shape.Line;
import javafx.scene.shape.Polygon;
import net.seapanda.bunnyhop.common.configuration.BhConstants;

/**
 * 警告マークを描画するクラス.
 *
 * @author K.Koike
 */
public class CorruptionIcon extends Group {

  /**
   * コンストラクタ.
   *
   * @param width マークの幅
   * @param height マークの高さ
   * @param styleClass CSS で指定するクラス
   * @param visible 作成直後の可視性
   */
  public CorruptionIcon(double width, double height, String styleClass, boolean visible) {
    // 三角形を作成
    Polygon triangle = new Polygon();
    double centerX = width * 0.5;
    double topY = height * 0.08;
    double bottomY = height;
    double leftX = 0;
    double rightX = width;

    triangle.getPoints().addAll(
        centerX, topY,      // 上の頂点
        leftX, bottomY,     // 左下の頂点
        rightX, bottomY     // 右下の頂点
    );
    triangle.getStyleClass().add(BhConstants.Css.Class.TRIANGLE);

    // エクスクラメーションマークの縦棒部分（長方形）
    double startY = height * 0.43;
    double barWidth = width * 0.1;
    double barHeight = height * 0.28;
    var exclamationBar = new Line(centerX, startY, centerX, startY + barHeight);
    exclamationBar.setStrokeWidth(barWidth);
    exclamationBar.getStyleClass().add(BhConstants.Css.Class.EXCLAMATION_BAR);

    // エクスクラメーションマークの点部分（円）
    Circle exclamationDot = new Circle();
    exclamationDot.setCenterX(centerX);
    exclamationDot.setCenterY(height * 0.85);
    exclamationDot.setRadius(barWidth * 0.5);
    exclamationDot.getStyleClass().add(BhConstants.Css.Class.EXCLAMATION_DOT);

    // すべての図形をグループに追加
    getChildren().addAll(triangle, exclamationBar, exclamationDot);
    setMouseTransparent(true);
    getStyleClass().add(styleClass);
    setVisible(visible);
  }
}
