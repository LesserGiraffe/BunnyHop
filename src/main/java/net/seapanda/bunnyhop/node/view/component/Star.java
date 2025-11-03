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

import javafx.scene.shape.Polygon;

/**
 * 星形を描画するクラス.
 *
 * @author K.Koike
 */
public class Star extends Polygon {

  /**
   * コンストラクタ.
   *
   * @param width 星の幅
   * @param height 星の高さ
   * @param numConvexPoints 凸部分の超点数
   * @param styleClass CSS で指定するクラス
   */
  public Star(double width, double height, int numConvexPoints, String styleClass) {
    double innerRadiusRatio = 0.5; // 内側の半径と外側の半径の比率（0.0-1.0）
    double centerX = width / 2.0;
    double centerY = height / 2.0;
    double outerRadiusX = width / 2.0;
    double outerRadiusY = height / 2.0;
    double innerRadiusX = outerRadiusX * innerRadiusRatio;
    double innerRadiusY = outerRadiusY * innerRadiusRatio;
    double angleStep = Math.PI / numConvexPoints; // 外側と内側の頂点間の角度
    double startAngle = -Math.PI / 2.0;

    for (int i = 0; i < numConvexPoints * 2; ++i) {
      double angle = startAngle + i * angleStep;
      double radiusX;
      double radiusY;
      // 偶数インデックスは外側の頂点、奇数インデックスは内側の頂点
      if (i % 2 == 0) {
        radiusX = outerRadiusX;
        radiusY = outerRadiusY;
      } else {
        radiusX = innerRadiusX;
        radiusY = innerRadiusY;
      }
      double x = centerX + radiusX * Math.cos(angle);
      double y = centerY + radiusY * Math.sin(angle);
      getPoints().addAll(x, y);
    }
    getStyleClass().add(styleClass);
    setMouseTransparent(true);
  }
}
