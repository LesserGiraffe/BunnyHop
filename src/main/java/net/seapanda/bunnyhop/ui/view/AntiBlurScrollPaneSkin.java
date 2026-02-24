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

package net.seapanda.bunnyhop.ui.view;

import java.lang.reflect.Field;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.skin.ScrollPaneSkin;
import javafx.scene.layout.StackPane;

/**
 * {@link ScrollPane} の文字の滲みを防止するためのスキン.
 *
 * @author K.Koike
 */
public class AntiBlurScrollPaneSkin extends ScrollPaneSkin {

  /** コンストラクタ. */
  public AntiBlurScrollPaneSkin(ScrollPane scrollpane) {
    super(scrollpane);
    try {
      Field viewRectField = ScrollPaneSkin.class.getDeclaredField("viewRect");
      viewRectField.setAccessible(true);
      StackPane viewRect = (StackPane) viewRectField.get(this);
      viewRect.setCache(false);
    } catch (Exception ignored) { /* Do nothing. */ }
  }
}
