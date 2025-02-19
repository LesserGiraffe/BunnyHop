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

package net.seapanda.bunnyhop.service;

import java.util.Optional;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonType;

/**
 * アプリケーションユーザ向けにメッセージを出力する機能を規定したインタフェース.
 *
 * @author K.Koike
 */
public interface MessageService {
 
  /** メッセージを出力する. */
  void info(String msg);

  /** エラーメッセージを出力する. */
  void error(String msg);
  
  /**
   * アラーウィンドウでメッセージを出力する.
   *
   * @param type アラートの種類
   * @param title アラートウィンドウのタイトル (nullable)
   * @param header アラートウィンドウのヘッダ (nullable)
   * @param content アラートウィンドウの本文 (nullable)
   * @param buttonTypes 表示するボタン (nullable)
   * @return メッセージに対して選択されたボタン
   */
  Optional<ButtonType> alert(
      Alert.AlertType type,
      String title,
      String header,
      String content,
      ButtonType ... buttonTypes);
}
