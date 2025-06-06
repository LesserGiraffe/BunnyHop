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
import javafx.scene.control.ListView;
import net.seapanda.bunnyhop.bhprogram.debugger.Debugger;
import net.seapanda.bunnyhop.model.workspace.WorkspaceSet;

/**
 * ブレークポイントを表示する UI コンポーネントのコントローラ.
 *
 * @author K.Koike
 */
public class BreakpointBoardController {

  @FXML private ListView<String> bpListView;
  @FXML private WorkspaceSelectorController bpWsSelectorController;

  /** 初期化する. */
  public void initialize(WorkspaceSet wss, Debugger debugger) {
    bpWsSelectorController.initialize(wss);
  }  
}
