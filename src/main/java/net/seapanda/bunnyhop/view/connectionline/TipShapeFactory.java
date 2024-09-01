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

import javafx.scene.paint.Paint;
import javafx.scene.shape.Line;
import javafx.scene.shape.Polyline;
import javafx.scene.shape.Shape;

/**
 * 線の先端のファクトリクラス.
 *
 * @author K.Koike
 */
class TipShapeFactory {

  /**
   * 矢印型の形状を作成する.
   *
   * @param angleOfOpening 矢印の開き具合 (rad)
   * @param lineLength 矢印を構成する線の長さ
   * @param strokeWidth 矢印を構成する線の太さ
   * @param rotAngle 回転角度. 左向きを 0[rad] として時計回り.
   * @param color 矢印の色
   */
  public static Shape createArrow(
      double angleOfOpening,
      double lineLength,
      double strokeWidth,
      double rotAngle,
      Paint color) {

    double startPosX = 0.0;
    double startPosY = 0.0;
    double endPosX0 = lineLength * Math.cos(angleOfOpening + rotAngle);
    double endPosY0 = lineLength * Math.sin(angleOfOpening + rotAngle);
    double endPosX1 = lineLength * Math.cos(-angleOfOpening + rotAngle);
    double endPosY1 = lineLength * Math.sin(-angleOfOpening + rotAngle);

    var arrow = new Polyline(endPosX0, endPosY0, startPosX, startPosY, endPosX1, endPosY1);
    arrow.setStrokeWidth(strokeWidth);
    arrow.setStroke(color);
    return arrow;
  }

  /** 形状のない {@link Shape} を作成する. */
  public static Shape createNone() {
    var line = new Line();
    line.setStrokeWidth(0.0);
    return line;
  }
}
