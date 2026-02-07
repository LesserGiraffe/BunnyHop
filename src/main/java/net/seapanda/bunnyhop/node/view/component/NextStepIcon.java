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
import javafx.scene.shape.Polygon;
import net.seapanda.bunnyhop.common.configuration.BhConstants;

/**
 * 現在実行中か次に実行するノードであることを示すアイコンを描画するクラス.
 *
 * @author K.Koike
 */
public class NextStepIcon extends Group {

  /**
   * コンストラクタ.
   *
   * @param radius アイコンの半径
   * @param styleClass CSS で指定するクラス
   * @param visible 作成直後の可視性
   */
  public NextStepIcon(double radius, String styleClass, boolean visible) {
    // 黄色い円を作成
    var circle = new Circle(radius, radius, radius);
    circle.getStyleClass().add(BhConstants.Css.Class.CIRCLE);

    // 緑の右向き矢印を作成
    Polygon arrow = createRightArrow(radius);
    arrow.getStyleClass().add(BhConstants.Css.Class.ARROW);

    // 子要素として追加
    getChildren().addAll(circle, arrow);
    setMouseTransparent(true);
    getStyleClass().add(styleClass);
    setVisible(visible);
  }

  /** 右向き矢印のPolygonを作成する. */
  private Polygon createRightArrow(double radius) {
    // 矢印のサイズを円の半径に基づいて計算
    double arrowWidth = radius * 1.4;  // 矢印の全体幅
    double arrowHeight = radius * 1.2; // 矢印の全体高さ
    double shaftHeight = radius * 0.5; // 矢印の軸の高さ
    double headWidth = radius * 0.6;   // 矢印の頭部の幅

    // 矢印の開始位置（左端）
    double startX = radius - arrowWidth / 2;
    double startY = radius;

    Polygon arrow = new Polygon();
    arrow.getPoints().addAll(
        // 矢印の軸 (左側の矩形部分)
        startX, startY - shaftHeight / 2,                           // 左上
        startX + arrowWidth - headWidth, startY - shaftHeight / 2,  // 軸右上

        // 矢印の頭部 (三角形部分)
        startX + arrowWidth - headWidth, startY - arrowHeight / 2,  // 頭部左上
        startX + arrowWidth, startY,                                // 頭部先端（右端中央）
        startX + arrowWidth - headWidth, startY + arrowHeight / 2,  // 頭部左下

        // 矢印の軸 (下側)
        startX + arrowWidth - headWidth, startY + shaftHeight / 2,  // 軸右下
        startX, startY + shaftHeight / 2                            // 左下
    );
    return arrow;
  }
}
