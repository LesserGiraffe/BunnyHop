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

package net.seapanda.bunnyhop.view.node.component;

import java.nio.file.Path;
import javafx.fxml.FXMLLoader;
import javafx.scene.control.Button;
import net.seapanda.bunnyhop.view.ViewConstructionException;
import net.seapanda.bunnyhop.view.node.style.BhNodeViewStyle;

/**
 * ノード固有のテンプレートノードを作成するボタン.
 *
 * @author K.Koike
 */
public final class PrivateTemplateButton extends Button {

  /**
   * コンストラクタ.
   *
   * @param filePath ボタンが定義された fxml ファイルのパス.
   * @param style ボタンのスタイル.
   * @throws ViewConstructionException ボタンの作成に失敗した場合
   */
  public PrivateTemplateButton(Path filePath, BhNodeViewStyle.Button style)
      throws ViewConstructionException {
    try {      
      FXMLLoader loader = new FXMLLoader(filePath.toUri().toURL());
      loader.setController(this);
      loader.setRoot(this);
      loader.load();
      this.getStyleClass().add(style.cssClass);
    } catch (Exception e) {
      throw new ViewConstructionException(
          "Failed to create Private Template Creation Button.\n" + e);
    }
  }
}
