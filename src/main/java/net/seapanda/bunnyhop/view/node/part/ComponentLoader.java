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

package net.seapanda.bunnyhop.view.node.part;

import java.io.IOException;
import java.nio.file.Path;
import java.util.Optional;
import javafx.fxml.FXMLLoader;
import javafx.scene.control.Button;
import javafx.scene.control.Control;
import net.seapanda.bunnyhop.service.FxmlCollector;
import net.seapanda.bunnyhop.service.MsgPrinter;

/**
 * GUI コンポーネントをロードするクラス.
 *
 * @author K.Koike
 */
public class ComponentLoader {

  /**
   * FXML ファイルからボタンをロードする.
   *
   * @param fileName ボタンをロードするFXMLファイル名
   * @param root このオブジェクトに対してボタンをロードする
   * @param buttonStyle ボタンに適用するスタイル
   */
  public static void loadButton(String fileName, Button root, BhNodeViewStyle.Button buttonStyle)
      throws IOException, ClassCastException {
    Path filePath = FxmlCollector.INSTANCE.getFilePath(fileName);
    FXMLLoader loader = new FXMLLoader(filePath.toUri().toURL());
    loader.setController(root);
    loader.setRoot(root);
    loader.load();
    setBtnStyle(buttonStyle, root);
  }

  /**
   * ノードに付くボタンのスタイルを指定する.
   *
   * @param style ノードに付くボタンのスタイル情報が格納されたオブジェクト
   */
  private static void setBtnStyle(BhNodeViewStyle.Button style, Button button) {
    button.setTranslateX(style.buttonPosX);
    button.setTranslateY(style.buttonPosY);
    button.getStyleClass().add(style.cssClass);
  }

  /**
   * GUI コンポーネントをロードする.
   *
   * @param contentFilePath GUI コンポーネントが定義されたファイルのパス.
   * @return ロードした GUI コンポーネント. ロードに失敗した場合は, {@code Optional.empty()}
   */
  public static <T extends Control> Optional<T> loadComponent(String contentFilePath) {
    if (contentFilePath == null) {
      return Optional.empty();
    }
    Path filePath = FxmlCollector.INSTANCE.getFilePath(contentFilePath);
    if (filePath == null) {
      return Optional.empty();
    }
    try {
      FXMLLoader loader = new FXMLLoader(filePath.toUri().toURL());
      return Optional.of(loader.<T>load());
    } catch (IOException | ClassCastException e) {
      MsgPrinter.INSTANCE.errMsgForDebug(
          "Failed to load component in %s\n%s.".formatted(contentFilePath, e));
      return Optional.empty();
    }
  }
}
