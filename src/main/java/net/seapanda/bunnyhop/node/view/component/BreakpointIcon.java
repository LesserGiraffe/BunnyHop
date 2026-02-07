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
import net.seapanda.bunnyhop.common.configuration.BhConstants;

/**
 * ブレークポイントを描画するクラス.
 *
 * @author K.Koike
 */
public class BreakpointIcon extends Group {

  /**
   * ブレークポイントマークの半径.
   *
   * @param radius 再生マークの半径.
   * @param styleClass CSS で指定するクラス
   * @param visible 作成直後の可視性
   */
  public BreakpointIcon(double radius, String styleClass, boolean visible) {
    var circle = new Circle(radius, radius, radius);
    circle.getStyleClass().add(BhConstants.Css.Class.CIRCLE);

    getChildren().addAll(circle);
    setMouseTransparent(true);
    getStyleClass().add(styleClass);
    setVisible(visible);
  }
}
