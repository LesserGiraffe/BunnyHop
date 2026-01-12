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

package net.seapanda.bunnyhop.node.view.bodyshape;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import net.seapanda.bunnyhop.node.view.BhNodeView;
import net.seapanda.bunnyhop.node.view.connectorshape.ConnectorShape;
import net.seapanda.bunnyhop.node.view.style.BhNodeViewStyle;
import net.seapanda.bunnyhop.node.view.style.ConnectorAlignment;
import net.seapanda.bunnyhop.node.view.style.ConnectorPos;
import net.seapanda.bunnyhop.node.view.style.NotchPos;

/** {@link BhNodeView} のボディを描画するクラスの基底クラス. */
public abstract class BodyShape {

  /**
   * ノードを形作る頂点を生成する.
   *
   * @param style ノードビュースタイル
   * @param bodyWidth ボディの幅
   * @param bodyHeight ボディの高さ
   * @param connector 描画するコネクタ
   * @param notch 描画する切り欠き
   * @return ノードを形成する頂点のリスト
   * */
  public abstract Collection<Double> createVertices(
      BhNodeViewStyle style,
      double bodyWidth,
      double bodyHeight,
      ConnectorShape connector,
      ConnectorShape notch);

  /**
   * コネクタ部分の頂点を作成する.
   *
   * @param connector 描画するコネクタオブジェクト
   * @param style ノードビュースタイル
   * @param cnctrWidth コネクタの幅
   * @param cnctrHeight コネクタの高さ
   * @param cnctrShift ノードの左上からのコネクタの位置
   * @param bodyWidth ボディの幅
   * @param bodyHeight ボディの高さ
   * @return コネクタ部分の頂点リスト
   */
  protected List<Double> createConnectorVertices(
      ConnectorShape connector,
      BhNodeViewStyle style,
      double cnctrWidth,
      double cnctrHeight,
      double cnctrShift,
      double bodyWidth,
      double bodyHeight) {
    
    double offsetX = 0.0;  // 頂点に加算するオフセットX
    double offsetY = 0.0;  // 頂点に加算するオフセットY

    if (style.connectorAlignment == ConnectorAlignment.CENTER) {
      offsetX = (bodyWidth - cnctrWidth) / 2.0;
      offsetY = (bodyHeight - cnctrHeight) / 2.0;
    }
    if (style.connectorPos == ConnectorPos.LEFT) {
      offsetX = -cnctrWidth;
      offsetY += cnctrShift;
    } else if (style.connectorPos == ConnectorPos.TOP) {
      offsetX += cnctrShift;
      offsetY = -cnctrHeight;
    }
    return connector.createVertices(offsetX, offsetY, cnctrWidth, cnctrHeight, style.connectorPos);
  }

  /**
   * 切り欠き部分の頂点を作成する.
   *
   * @param notch 描画する切り欠きオブジェクト
   * @param notchPos 切り欠きの位置 (Top, Left)
   * @param notchWidth 切り欠きの幅
   * @param notchHeight 切り欠きの高さ
   * @param bodyWidth ボディの幅
   * @param bodyHeight ボディの高さ
   * @return 切り欠き部分の頂点リスト
   */
  protected List<Double> createNotchVertices(
      ConnectorShape notch,
      NotchPos notchPos,
      double notchWidth,
      double notchHeight,
      double bodyWidth,
      double bodyHeight) {

    double offsetX = 0.0;
    double offsetY = 0.0;
    ConnectorPos cnctrPos = ConnectorPos.LEFT;

    if (notchPos == NotchPos.RIGHT) {
      offsetX = bodyWidth - notchWidth;
      offsetY = (bodyHeight - notchHeight) * 0.5;
      cnctrPos = ConnectorPos.LEFT;
    } else if (notchPos == NotchPos.BOTTOM) {
      offsetX = (bodyWidth - notchWidth) * 0.5;
      offsetY = bodyHeight - notchHeight;
      cnctrPos = ConnectorPos.TOP;
    }

    List<Double> vertices =
        notch.createVertices(offsetX, offsetY, notchWidth, notchHeight, cnctrPos);
    Collections.reverse(vertices);
    for (int i = 0; i < vertices.size(); i += 2) {
      double tmp = vertices.get(i);
      vertices.set(i, vertices.get(i + 1));
      vertices.set(i + 1, tmp);
    }
    return vertices;
  }

  /**
   * ボディ名から対応する BODY_SHAPE を返す.
   *
   * @param bodyShapeName ボディの形を表す文字列
   * @param fileName ボディの形が記述してあるjsonファイルの名前 (nullable)
   * @return shapeStrに対応する CNCTR_SHAPE 列挙子 (オプション)
   */
  public static BodyShapeType getBodyTypeFromName(String bodyShapeName, String fileName) {
    BodyShapeType type = BodyShapeType.getByName(bodyShapeName);
    if (type == null) {
      throw new IllegalArgumentException(
          "Invalid body shape name %s (%s)".formatted(bodyShapeName, fileName));
    }
    return type;
  }
}
