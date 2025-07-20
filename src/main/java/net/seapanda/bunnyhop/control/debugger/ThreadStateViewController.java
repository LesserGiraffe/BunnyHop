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

import javafx.css.PseudoClass;
import javafx.fxml.FXML;
import javafx.geometry.Point2D;
import javafx.scene.control.Tooltip;
import javafx.scene.input.MouseEvent;
import javafx.scene.text.Text;
import net.seapanda.bunnyhop.bhprogram.common.BhThreadState;
import net.seapanda.bunnyhop.bhprogram.debugger.DebugUtil;
import net.seapanda.bunnyhop.bhprogram.debugger.ThreadContext;
import net.seapanda.bunnyhop.common.BhConstants;
import net.seapanda.bunnyhop.common.Rem;
import net.seapanda.bunnyhop.common.TextDefs;
import net.seapanda.bunnyhop.view.ViewUtil;

/**
 * スレッドの状態を表示するコンポーネントのコントローラ.
 *
 * @author K.Koike
 */
public class ThreadStateViewController {

  @FXML private Text threadStateText;  

  /** エラーメッセージを表示するツールチップ. */
  private final Tooltip errMsgTooltip = new Tooltip();

  /** 初期化する. */
  public void initialize() {
    errMsgTooltip.setId(BhConstants.UiId.BH_RUNTIME_ERR_MSG);
    errMsgTooltip.setAutoHide(false);
    errMsgTooltip.addEventFilter(MouseEvent.MOUSE_PRESSED, event -> errMsgTooltip.hide());
  }

  /**
   * {@code context} に対応するスレッドの状態を表示する.
   *
   * @param context 表示するスレッドの状態を格納したオブジェクト
   */
  synchronized void showThreadState(ThreadContext context) {
    if (context == null) {
      return;
    }
    ViewUtil.runSafe(() -> {
      PseudoClass pseudo = PseudoClass.getPseudoClass(BhConstants.Css.PSEUDO_ERROR);
      threadStateText.setText(getThreadStateText(context.state()));
      if (context.state() != BhThreadState.ERROR) {
        threadStateText.pseudoClassStateChanged(pseudo, false);
        threadStateText.setOnMousePressed(null);
      } else if (context.state() == BhThreadState.ERROR) {
        String errMsg = DebugUtil.getErrMsg(context.exception());
        errMsgTooltip.setText(errMsg);
        threadStateText.pseudoClassStateChanged(pseudo, true);
        threadStateText.setOnMousePressed(event -> toggleErrTooltipVisibility());
      }
      errMsgTooltip.hide();
    });
  }

  /** スレッドの状態を非表示にする. */
  synchronized void hideThreadState() {
    ViewUtil.runSafe(() -> {
      threadStateText.setText("");
      errMsgTooltip.setText("");
      errMsgTooltip.hide();
    });
  }

  public synchronized void reset() {
    hideThreadState();
  }

  private static String getThreadStateText(BhThreadState state) {
    String stateStr = switch (state) {
      case RUNNING -> TextDefs.Debugger.ThreadStatus.running.get();
      case SUSPENDED -> TextDefs.Debugger.ThreadStatus.suspended.get();
      case ERROR -> TextDefs.Debugger.ThreadStatus.error.get();
      case FINISHED -> TextDefs.Debugger.ThreadStatus.finished.get();
    };
    return "%s: %s".formatted(TextDefs.Debugger.ThreadStatus.status.get(), stateStr);
  }
  
  /** エラーメッセージの可視性を切り替える. */
  private void toggleErrTooltipVisibility() {
    if (errMsgTooltip.isShowing()) {
      errMsgTooltip.hide(); 
    } else {
      Point2D p = threadStateText.localToScreen(0.5 * Rem.VAL, 1.5 * Rem.VAL);
      errMsgTooltip.show(threadStateText, p.getX(), p.getY());
    }
  }
}
