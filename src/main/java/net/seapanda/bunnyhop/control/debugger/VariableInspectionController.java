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

package net.seapanda.bunnyhop.control.debugger;

import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.Label;
import javafx.scene.control.TreeView;
import net.seapanda.bunnyhop.bhprogram.debugger.Debugger;
import net.seapanda.bunnyhop.bhprogram.debugger.variable.VariableInfo;
import net.seapanda.bunnyhop.control.SearchBox;
import net.seapanda.bunnyhop.model.ModelAccessNotificationService;

/**
 * 変数情報を表示するビューのコントローラ.
 *
 * @author K.Koike
 */
public class VariableInspectionController {

  @FXML private Label viViewName;
  @FXML private TreeView<String> variableTreeView;
  @FXML private Button viSearchButton;
  @FXML private CheckBox viJumpCheckBox;

  private final VariableInfo varInfo;
  private final ModelAccessNotificationService notifService;
  private final SearchBox searchBox;
  private final Debugger debugger;

  /**
   * コンストラクタ.
   *
   * @param varInfo このコントローラが管理するビューに表示する変数情報を格納したオブジェクト
   * @param viewName ビューの名前
   * @param notifService モデルへのアクセスの通知先となるオブジェクト
   * @param searchBox 検索クエリを受け取る UI コンポーネントのインタフェース
   */
  public VariableInspectionController(
      VariableInfo varInfo,
      String viewName,
      ModelAccessNotificationService notifService,
      SearchBox searchBox,
      Debugger debugger) {
    viewName = (viewName == null) ? "" : viewName;
    this.viViewName.setText(viewName);
    this.varInfo = varInfo;
    this.notifService = notifService;
    this.searchBox = searchBox;
    this.debugger = debugger;
  }
}
