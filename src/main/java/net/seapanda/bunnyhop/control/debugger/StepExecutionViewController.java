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
import javafx.scene.input.MouseButton;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.VBox;
import net.seapanda.bunnyhop.bhprogram.debugger.Debugger;
import net.seapanda.bunnyhop.bhprogram.debugger.ThreadSelection;

/**
 * デバッガのスレッド制御コンポーネントのコントローラ.
 *
 * @author K.Koike
 */
public class StepExecutionViewController {

  @FXML private VBox stepExecutionViewBase;
  @FXML private Button resumeBtn;
  @FXML private Button suspendBtn;
  @FXML private Button reloadBtn;
  @FXML private Button stepOverBtn;
  @FXML private Button stepIntoBtn;
  @FXML private Button stepOutBtn;

  private final Debugger debugger;

  /** コンストラクタ. */
  public StepExecutionViewController(Debugger debugger) {
    this.debugger = debugger;
  }

  /** このコントローラの UI 要素を初期化する. */
  @FXML
  public void initialize() {
    setEventHandlers();
    setStepButtonsEnable(isParticularThreadSelected(debugger.getCurrentThread()));
  }

  /** イベントハンドラをセットする. */
  private void setEventHandlers() {
    debugger.getCallbackRegistry().getOnCurrentThreadChanged().add(
        event -> setStepButtonsEnable(isParticularThreadSelected(event.newVal())));
    resumeBtn.setOnAction(event -> debugger.resume());
    suspendBtn.setOnAction(event -> debugger.suspend());
    reloadBtn.setOnAction(event -> debugger.requestThreadContexts());
    stepOverBtn.setOnAction(event -> debugger.stepOver());
    stepIntoBtn.setOnAction(event -> debugger.stepInto());
    stepOutBtn.setOnAction(event -> debugger.stepOut());
    stepExecutionViewBase.addEventFilter(MouseEvent.MOUSE_PRESSED, this::consumeIfNotAcceptable);
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

  /** 受付不能なマウスイベントを consume する. */
  private void consumeIfNotAcceptable(MouseEvent event) {
    if (event.getButton() != MouseButton.PRIMARY) {
      event.consume();
    }
  }
}
