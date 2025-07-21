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
import net.seapanda.bunnyhop.bhprogram.debugger.Debugger;

/**
 * デバッガのスレッド制御コンポーネントのコントローラ.
 *
 * @author K.Koike
 */
public class StepExecutionViewController {
  
  @FXML private Button resumeBtn;
  @FXML private Button suspendBtn;
  @FXML private Button reloadBtn;
  @FXML private Button stepOverBtn;
  @FXML private Button stepIntoBtn;
  @FXML private Button stepOutBtn;

  /** 初期化する. */
  public void initialize(Debugger debugger, ThreadSelectorController threadSelCtrl) {
    resumeBtn.setOnAction(event -> {
      if (threadSelCtrl.isAllSelected()) {
        debugger.resumeAll();
      } else {
        threadSelCtrl.getSelected().ifPresent(debugger::resume);
      }
    });
    suspendBtn.setOnAction(event -> {
      if (threadSelCtrl.isAllSelected()) {
        debugger.suspendAll();
      } else {
        threadSelCtrl.getSelected().ifPresent(debugger::suspend);
      }
    });
    stepOverBtn.setOnAction(event -> threadSelCtrl.getSelected().ifPresent(debugger::stepOver));
    stepIntoBtn.setOnAction(event -> threadSelCtrl.getSelected().ifPresent(debugger::stepInto));
    stepOutBtn.setOnAction(event -> threadSelCtrl.getSelected().ifPresent(debugger::stepOut));
  }  
}
