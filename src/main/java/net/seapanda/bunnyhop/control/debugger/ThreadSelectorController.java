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

import java.util.List;
import javafx.fxml.FXML;
import javafx.scene.control.ComboBox;
import javafx.scene.control.ListCell;
import javafx.scene.input.MouseButton;
import javafx.scene.input.MouseEvent;
import net.seapanda.bunnyhop.bhprogram.debugger.Debugger;
import net.seapanda.bunnyhop.bhprogram.debugger.ThreadSelection;
import net.seapanda.bunnyhop.common.TextDefs;
import net.seapanda.bunnyhop.view.ViewUtil;

/**
 * デバッガのスレッド選択コンポーネントのコントローラ.
 *
 * @author K.Koike
 */
public class ThreadSelectorController {

  @FXML private ComboBox<Long> threadComboBox;

  /** 初期化する. */
  public void initialize(Debugger debugger) {
    threadComboBox.setButtonCell(new ThreadSelectorListCell());
    threadComboBox.setCellFactory(items -> new ThreadSelectorListCell());
    threadComboBox.valueProperty().addListener(
        (observable, oldVal, newVal) -> selectCurrentThread(debugger, newVal));
    Debugger.CallbackRegistry cbRegistry = debugger.getCallbackRegistry();
    cbRegistry.getOnCleared().add(event -> reset());
    cbRegistry.getOnCurrentThreadChanged().add(this::onCurrentThreadChanged);
    threadComboBox.addEventFilter(MouseEvent.MOUSE_RELEASED, this::consumeIfNotAcceptable);
    reset();
  }

  /** {@code newVal} に応じて {@code debugger} の現在のスレッドを変更する. */
  private static void selectCurrentThread(Debugger debugger, Long newVal) {
    long threadId = (newVal == null) ? ThreadSelection.NONE.getThreadId() : newVal;
    if (threadId == debugger.getCurrentThread().getThreadId()) {
      return;
    }
    if (newVal == null) {
      debugger.selectCurrentThread(ThreadSelection.NONE);
    } else if (ThreadSelection.of(newVal).equals(ThreadSelection.ALL)) {
      debugger.selectCurrentThread(ThreadSelection.ALL);
    } else {
      debugger.selectCurrentThread(ThreadSelection.of(newVal));
    }
  }

  /** デバッガの現在のスレッドが変わったときの処理. */
  private void onCurrentThreadChanged(Debugger.CurrentThreadChangedEvent event) {
    Long threadId =
        event.newVal().equals(ThreadSelection.NONE) ? null : event.newVal().getThreadId();
    ViewUtil.runSafe(() -> threadComboBox.setValue(threadId));
  }

  /** このコントローラの管理するコンポーネントを初期状態に戻す. */
  private synchronized void reset() {
    ViewUtil.runSafe(() -> {
      threadComboBox.getItems().setAll(List.of(ThreadSelection.ALL.getThreadId()));
      threadComboBox.getSelectionModel().selectFirst();
    });
  }

  /**
   * スレッドの選択肢に {@code threadId} で指定したスレッドを追加する.
   *
   * @param threadId 選択肢に追加するスレッドの ID
   */
  synchronized void addToSelection(long threadId) {
    if (threadId < 1) {
      return;
    }
    boolean isNewThreadId = !threadComboBox.getItems().contains(threadId);
    if (isNewThreadId) {
      threadComboBox.getItems().addLast(threadId);
    }
  }

  /**
   * スレッドの選択肢から {@code threadId} で指定したスレッドを削除する.
   *
   * @param threadId 選択肢から削除するスレッドの ID
   */
  synchronized void removeFromSelection(long threadId) {
    if (threadId < 1) {
      return;
    }
    ViewUtil.runSafe(() -> {
      if (!threadComboBox.getItems().contains(threadId)) {
        threadComboBox.getItems().remove(threadId);
      }
    });
  }

  /** 受付不能なマウスイベントを consume する. */
  private void consumeIfNotAcceptable(MouseEvent event) {
    if (event.getButton() != MouseButton.PRIMARY) {
      event.consume();
    }
  }

  /** スレッド選択コンボボックスのアイテムの View. */
  private class ThreadSelectorListCell extends ListCell<Long> {

    @Override
    protected void updateItem(Long item, boolean empty) {
      super.updateItem(item, empty);
      if (!empty && item != null) {
        if (ThreadSelection.of(item).equals(ThreadSelection.ALL)) {
          setText("%s".formatted(TextDefs.Debugger.allThreads.get()));
        } else {
          setText("%s %s".formatted(TextDefs.Debugger.thread.get(), item));
        }
      } else {
        setText(null);
      }
    }
  }
}
