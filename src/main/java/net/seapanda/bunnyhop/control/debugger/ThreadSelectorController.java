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

import java.util.Optional;
import java.util.function.Consumer;
import javafx.fxml.FXML;
import javafx.scene.control.ComboBox;
import javafx.scene.control.ListCell;
import net.seapanda.bunnyhop.common.TextDefs;
import net.seapanda.bunnyhop.view.ViewUtil;

/**
 * デバッガのスレッド選択コンポーネントのコントローラ.
 *
 * @author K.Koike
 */
public class ThreadSelectorController {
  
  @FXML private ComboBox<Long> threadComboBox;

  /** スレッドが選択されたときのイベントハンドラ. */
  private Consumer<? super ThreadSelectionEvent> onThreadSelected = threadId -> {};
  /** 全スレッドを表す選択肢に対応するスレッド ID. */
  private final Long threadIdForAll = -1L;

  /** 初期化する. */
  public void initialize() {
    threadComboBox.setButtonCell(new ThreadSelectorListCell());
    threadComboBox.setCellFactory(items -> new ThreadSelectorListCell());
    threadComboBox.valueProperty().addListener((observable, oldVal, newVal) -> {
      onThreadSelected.accept(new ThreadSelectionEvent(oldVal, newVal, isAllSelected()));
    });
    reset();
  }

  /**
   * 現在選択中のスレッドを返す. 
   *
   * @return 現在選択中のスレッド. 特定のスレッドが選択されていない場合は Optional.empty.
   */
  synchronized Optional<Long> getSelected() {
    Long selected = threadComboBox.getValue();
    if (threadIdForAll.equals(selected)) {
      return Optional.empty();
    }
    return Optional.ofNullable(selected);
  }

  /** 「すべてのスレッド」が選択されている場合 true を返す. */
  synchronized boolean isAllSelected() {
    return threadIdForAll.equals(threadComboBox.getValue());
  }

  /** このコントローラの管理するコンポーネントを初期状態に戻す. */
  synchronized void reset() {
    ViewUtil.runSafe(() -> {
      threadComboBox.getItems().clear();
      threadComboBox.getItems().add(threadIdForAll);
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
    ViewUtil.runSafe(() -> {
      if (!threadComboBox.getItems().contains(threadId)) {
        threadComboBox.getItems().addLast(threadId);
      }
      Long selected = threadComboBox.getValue();
      if (selected != null && selected < 0L) {
        selectThread(threadId);
      }
    });
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

  /** {@code threadId} で指定したスレッドを選択する. */
  private void selectThread(long threadId) {
    if (threadComboBox.getItems().contains(threadId)) {
      threadComboBox.getSelectionModel().select(threadId);
    }
  }

  /**
   * スレッドが選択されたときのイベントハンドラを登録する.
   *
   * @param handler 登録するイベントハンドラ.
   */
  void setOnThreadSelected(Consumer<? super ThreadSelectionEvent> handler) {
    if (handler != null) {
      onThreadSelected = handler;
    }
  }

  /** スレッド選択コンボボックスのアイテムの View. */
  private class ThreadSelectorListCell extends ListCell<Long> {
    @Override
    protected void updateItem(Long item, boolean empty) {
      super.updateItem(item, empty);
      if (!empty && item != null) {
        if (item == threadIdForAll) {
          setText("%s".formatted(TextDefs.Debugger.allThreads.get()));
        } else {
          setText("%s %s".formatted(TextDefs.Debugger.thread.get(), item));
        }
      } else {
        setText(null);
      }
    }
  }

  /**
   * スレッドが選択されたときの情報を格納したレコード.
   *
   * @param oldThreadId {@code newThread} の前に選択されていたスレッドの ID
   * @param newThreadId 新しく選択されたスレッドの ID
   * @param isAllSelected 「すべてのスレッド」が選択された場合 true
   */
  public record ThreadSelectionEvent(
      Long oldThreadId, Long newThreadId, boolean isAllSelected) {}
}
