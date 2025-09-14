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

package net.seapanda.bunnyhop.control;

import javafx.fxml.FXML;
import javafx.scene.control.TextArea;
import net.seapanda.bunnyhop.common.BhConstants;

/**
 * ユーザへのメッセージを表示する UI 部分のコントローラ.
 *
 * @author K.Koike
 */
public class MessageViewController {

  @FXML TextArea mainMsgArea;

  @FXML
  public void initialize() {
    setEvenHandlers();
  }

  private void setEvenHandlers() {
    mainMsgArea.textProperty().addListener((observable, oldVal, newVal) -> {
      if (newVal.length() > BhConstants.Message.MAX_MAIN_MSG_AREA_CHARS) {
        int numDeleteChars = newVal.length() - BhConstants.Message.MAX_MAIN_MSG_AREA_CHARS;
        mainMsgArea.deleteText(0, numDeleteChars);
      }
      mainMsgArea.setScrollTop(Double.MAX_VALUE);
    });

    mainMsgArea.scrollTopProperty().addListener((observable, oldVal, newVal) -> {
      if (oldVal.doubleValue() == Double.MAX_VALUE && newVal.doubleValue() == 0.0) {
        mainMsgArea.setScrollTop(Double.MAX_VALUE);
      }
    });
  }

  /** アプリケーションのメッセージを表示する {@link TextArea} を取得する. */
  public TextArea getMsgArea() {
    return mainMsgArea;
  }
}
