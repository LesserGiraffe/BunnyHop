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
import net.seapanda.bunnyhop.bhprogram.ThreadSelection;
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
  public void initialize(Debugger debugger) {
    resumeBtn.setOnAction(event -> {
      ThreadSelection selection = debugger.getSelectedThread();
      if (selection.equals(ThreadSelection.ALL)) {
        debugger.resumeAll();
      } else if (!selection.equals(ThreadSelection.NONE)) {
        debugger.resume(selection.getThreadId());
      }
    });
    suspendBtn.setOnAction(event -> {
      ThreadSelection selection = debugger.getSelectedThread();
      if (selection.equals(ThreadSelection.ALL)) {
        debugger.suspendAll();
      } else if (!selection.equals(ThreadSelection.NONE)) {
        debugger.suspend(selection.getThreadId());
      }
    });
    reloadBtn.setOnAction(event -> debugger.requireThreadContexts());

    stepOverBtn.setOnAction(event -> {
      ThreadSelection selection = debugger.getSelectedThread();
      if (isParticularThreadSelected(selection)) {
        debugger.stepOver(selection.getThreadId());
      }
    });
    stepIntoBtn.setOnAction(event -> {
      ThreadSelection selection = debugger.getSelectedThread();
      if (isParticularThreadSelected(selection)) {
        debugger.stepInto(selection.getThreadId());
      }
    });
    stepOutBtn.setOnAction(event -> {
      ThreadSelection selection = debugger.getSelectedThread();
      if (isParticularThreadSelected(selection)) {
        debugger.stepOut(selection.getThreadId());
      }
    });
    debugger.getCallbackRegistry().getOnThreadSelectionChanged().add(
        event -> setStepButtonsEnable(isParticularThreadSelected(event.newVal())));
    setStepButtonsEnable(isParticularThreadSelected(debugger.getSelectedThread()));
  }

  /** 特定のスレッドが選択されているかどうかを調べる. */
  private boolean isParticularThreadSelected(ThreadSelection selection) {
    return !selection.equals(ThreadSelection.ALL) && !selection.equals(ThreadSelection.NONE);
  }

  private void setStepButtonsEnable(boolean enable) {
    stepOverBtn.setDisable(!enable);
    stepIntoBtn.setDisable(!enable);
    stepOutBtn.setDisable(!enable);
  }
}
