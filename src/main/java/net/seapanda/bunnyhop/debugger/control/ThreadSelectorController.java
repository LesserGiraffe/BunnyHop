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

package net.seapanda.bunnyhop.debugger.control;

import static javafx.css.PseudoClass.getPseudoClass;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import javafx.fxml.FXML;
import javafx.scene.control.ComboBox;
import javafx.scene.control.ListCell;
import javafx.scene.input.MouseButton;
import javafx.scene.input.MouseEvent;
import net.seapanda.bunnyhop.bhprogram.common.BhThreadState;
import net.seapanda.bunnyhop.common.configuration.BhConstants;
import net.seapanda.bunnyhop.common.text.TextDefs;
import net.seapanda.bunnyhop.debugger.model.Debugger;
import net.seapanda.bunnyhop.debugger.model.Debugger.ThreadContextAddedEvent;
import net.seapanda.bunnyhop.debugger.model.thread.ThreadContext;
import net.seapanda.bunnyhop.debugger.model.thread.ThreadSelection;

/**
 * デバッガのスレッド選択コンポーネントのコントローラ.
 *
 * @author K.Koike
 */
public class ThreadSelectorController {

  @FXML private ComboBox<Long> threadComboBox;

  /** スレッド ID とスレッドコンテキストのマップ. */
  private final Map<Long, ThreadContext> threadIdToContext = new HashMap<>();
  /** {@link #threadComboBox} が保持する全てのリストセルを格納するセット. */
  private final Set<ThreadSelectorListCell> listCells = new HashSet<>();
  private final Debugger debugger;

  /** コンストラクタ. */
  public ThreadSelectorController(Debugger debugger) {
    this.debugger = debugger;
  }

  /** このコントローラの UI 要素を初期化する. */
  @FXML
  public void initialize() {
    setEventHandlers();
    reset();
  }

  /** イベントハンドラを設定する. */
  private void setEventHandlers() {
    Debugger.CallbackRegistry cbRegistry = debugger.getCallbackRegistry();
    cbRegistry.getOnCleared().add(event -> reset());
    cbRegistry.getOnCurrentThreadChanged().add(this::onCurrentThreadChanged);
    cbRegistry.getOnThreadContextAdded().add(this::onThreadContextAdded);
    threadComboBox.setButtonCell(new ThreadSelectorListCell());
    threadComboBox.setCellFactory(items -> new ThreadSelectorListCell());
    threadComboBox.valueProperty().addListener(
        (observable, oldVal, newVal) -> selectCurrentThread(debugger, newVal));
    threadComboBox.addEventFilter(MouseEvent.MOUSE_RELEASED, this::consumeIfNotAcceptable);
    threadComboBox.setOnShowing(
        event -> listCells.forEach(ThreadSelectorListCell::updateStyleByThreadState));
  }

  /** スレッドコンテキストが追加されたときの処理. */
  private void onThreadContextAdded(ThreadContextAddedEvent event) {
    threadIdToContext.put(event.context().threadId, event.context());
    listCells.forEach(ThreadSelectorListCell::updateStyleByThreadState);
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
    if (threadId != null) {
      addToOptions(threadId);
    }
    threadComboBox.setValue(threadId);
  }

  /** このコントローラの管理するコンポーネントを初期状態に戻す. */
  private void reset() {
    int numItems = threadComboBox.getItems().size();
    if (numItems == 0) {
      threadComboBox.getItems().add(ThreadSelection.ALL.getThreadId());
    } else {
      // ThreadSelection.NONE を経由せずに ThreadSelection.ALL を選択したい
      threadComboBox.getItems().remove(1, numItems);
    }
    threadComboBox.getSelectionModel().selectFirst();
    threadIdToContext.clear();
  }

  /**
   * スレッドの選択肢に {@code threadId} で指定したスレッドを追加する.
   *
   * @param threadId 選択肢に追加するスレッドの ID
   */
  void addToOptions(long threadId) {
    if (threadId < 1) {
      return;
    }
    if (!threadComboBox.getItems().contains(threadId)) {
      threadComboBox.getItems().addLast(threadId);
    }
  }

  /**
   * スレッドの選択肢から {@code threadId} で指定したスレッドを削除する.
   *
   * @param threadId 選択肢から削除するスレッドの ID
   */
  void removeFromOptions(long threadId) {
    if (threadId < 1) {
      return;
    }
    threadComboBox.getItems().remove(threadId);
  }

  /** 受付不能なマウスイベントを consume する. */
  private void consumeIfNotAcceptable(MouseEvent event) {
    if (event.getButton() != MouseButton.PRIMARY) {
      event.consume();
    }
  }

  /** スレッド選択コンボボックスのアイテムの View. */
  private class ThreadSelectorListCell extends ListCell<Long> {

    /** コンストラクタ. */
    private ThreadSelectorListCell() {
      listCells.add(this);
      getStyleClass().add(BhConstants.Css.Class.THREAD_SELECTOR_LIST_ITEM);
    }

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
      updateStyleByThreadState();
    }

    /** スレッドの状態に応じてセルに視覚効果を適用する. */
    private void updateStyleByThreadState() {
      if (isEmpty()) {
        setDefaultStyle();
        return;
      }
      ThreadContext context = threadIdToContext.get(getItem());
      if (context == null) {
        setDefaultStyle();
        return;
      }
      switch (context.state) {
        case BhThreadState.FINISHED -> {
          pseudoClassStateChanged(getPseudoClass(BhConstants.Css.Pseudo.FINISHED), true);
          pseudoClassStateChanged(getPseudoClass(BhConstants.Css.Pseudo.ERROR), false);
          pseudoClassStateChanged(getPseudoClass(BhConstants.Css.Pseudo.SUSPENDED), false);
        }
        case BhThreadState.ERROR -> {
          pseudoClassStateChanged(getPseudoClass(BhConstants.Css.Pseudo.FINISHED), false);
          pseudoClassStateChanged(getPseudoClass(BhConstants.Css.Pseudo.ERROR), true);
          pseudoClassStateChanged(getPseudoClass(BhConstants.Css.Pseudo.SUSPENDED), false);
        }
        case BhThreadState.SUSPENDED -> {
          pseudoClassStateChanged(getPseudoClass(BhConstants.Css.Pseudo.FINISHED), false);
          pseudoClassStateChanged(getPseudoClass(BhConstants.Css.Pseudo.ERROR), false);
          pseudoClassStateChanged(getPseudoClass(BhConstants.Css.Pseudo.SUSPENDED), true);
        }
        default -> setDefaultStyle();
      }
    }

    private void setDefaultStyle() {
      pseudoClassStateChanged(getPseudoClass(BhConstants.Css.Pseudo.FINISHED), false);
      pseudoClassStateChanged(getPseudoClass(BhConstants.Css.Pseudo.ERROR), false);
      pseudoClassStateChanged(getPseudoClass(BhConstants.Css.Pseudo.SUSPENDED), false);
    }
  }
}
