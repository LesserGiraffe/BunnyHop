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
import javafx.scene.layout.Pane;
import net.seapanda.bunnyhop.bhprogram.debugger.Debugger;
import net.seapanda.bunnyhop.common.BhConstants;
import net.seapanda.bunnyhop.control.debugger.BreakpointListController;
import net.seapanda.bunnyhop.control.debugger.DebugViewController;
import net.seapanda.bunnyhop.model.ModelAccessNotificationService;
import net.seapanda.bunnyhop.model.workspace.WorkspaceSet;
import net.seapanda.bunnyhop.view.factory.DebugViewFactory;

/**
 * ユーザへ各種情報を通知する UI 部分のコントローラ.
 *
 * @author K.Koike
 */
public class NotificationViewController {
  
  private TextArea mainMsgArea;
  @FXML private Pane notificationViewBase;
  @FXML private DebugViewController debugViewController;
  @FXML private BreakpointListController breakpointListController;
  @FXML private SearchBoxController searchBoxController;

  /** 初期化する. */
  public void initialize(
      WorkspaceSet wss,
      Debugger debugger,
      DebugViewFactory factory,
      ModelAccessNotificationService norifService) {
    debugViewController.initialize(debugger, factory);
    breakpointListController.initialize(wss, debugger, norifService, searchBoxController);
    setMessageAreaEvenHandlers();
  }

  /** メッセージエリアのイベントハンドラを登録する. */
  private void setMessageAreaEvenHandlers() {
    TextArea msgArea = getMsgArea();
    msgArea.textProperty().addListener((observable, oldVal, newVal) -> {
      if (newVal.length() > BhConstants.Message.MAX_MAIN_MSG_AREA_CHARS) {
        int numDeleteChars = newVal.length() - BhConstants.Message.MAX_MAIN_MSG_AREA_CHARS;
        msgArea.deleteText(0, numDeleteChars);
      }
      msgArea.setScrollTop(Double.MAX_VALUE);
    });

    msgArea.scrollTopProperty().addListener((observable, oldVal, newVal) -> {
      if (oldVal.doubleValue() == Double.MAX_VALUE && newVal.doubleValue() == 0.0) {
        msgArea.setScrollTop(Double.MAX_VALUE);
      }
    });
  }  

  /** アプリケーションのメッセージを表示する TextArea を取得する. */
  public TextArea getMsgArea() {
    if (mainMsgArea == null) {
      mainMsgArea = (TextArea) notificationViewBase.lookup("#" + BhConstants.UiId.MAIN_MSG_AREA);
    }
    return mainMsgArea;
  }

  /** 検索ボックスのコントローラオブジェクトを取得する. */
  public SearchBoxController getSearchBoxController() {
    return searchBoxController;
  }
}
