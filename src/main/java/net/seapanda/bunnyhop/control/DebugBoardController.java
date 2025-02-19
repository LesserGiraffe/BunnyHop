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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.SequencedCollection;
import javafx.application.Platform;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.IntegerProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.fxml.FXML;
import javafx.scene.control.ComboBox;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.Tab;
import javafx.scene.layout.StackPane;
import javafx.scene.text.Text;
import net.seapanda.bunnyhop.bhprogram.ThreadContext;
import net.seapanda.bunnyhop.common.BhConstants;
import net.seapanda.bunnyhop.model.node.BhNode;
import net.seapanda.bunnyhop.model.node.syntaxsymbol.InstanceId;
import net.seapanda.bunnyhop.model.workspace.Workspace;
import net.seapanda.bunnyhop.view.node.component.SelectableItem;

/**
 * デバッグ情報を表示する UI コンポーネントのコントローラ.
 * このクラスのメソッドは UI スレッドから呼ぶこと.
 *
 * @author K.Koike
 */
public class DebugBoardController {
  
  @FXML Tab debugTab;
  @FXML ScrollPane debugScrollPane;
  @FXML ComboBox<SelectableItem> threadSelector;
  @FXML StackPane debugStackPane;

  /** スレッド ID とコールスタックのマップ. */
  Map<Long, SequencedCollection<InstanceId>> threadIdToCallStack = new HashMap<>();
  /** {@link InstanceId} とその ID を持つノードがマークされた回数. */
  Map<InstanceId, IntegerProperty> instIdToNumMarked = new HashMap<>();
  /**
   * key: コールスタックの要素 (スレッド ID と {@link InstanceId} のペア) .
   * value: key で指定されるコールスタックの要素がマークされているかどうかのフラグ.
   */
  Map<CallStackElem, BooleanProperty> callStackElemToMarkFlags = new HashMap<>();

  /**
   * スレッドの情報を追加する.
   *
   * @param context 追加するスレッドの情報
   */
  public void addThreadContext(ThreadContext context) {
    if (!Platform.isFxApplicationThread()) {
      Platform.runLater(() -> addThreadContextImpl(context));
      return;
    }
    addThreadContextImpl(context);
  }

  private void addThreadContextImpl(ThreadContext context) {
    if (context.threadId() < 1) {
      return;
    }
    var callStack = new ArrayList<>(context.callStack());
    threadIdToCallStack.put(context.threadId(), callStack);
    for (InstanceId instId : callStack.reversed()) {
      var callStackElem = new CallStackElem(context.threadId(), instId);
      BooleanProperty markFlag = callStackElemToMarkFlags.get(callStackElem);
      if (markFlag != null) {
        markFlag.set(false);
        continue;
      }

      var text = new Text(instId.toString());
      var isSelected = new SimpleBooleanProperty(false);
      text.setOnMouseClicked(event ->
          BhNode.getBhNodeOf(instId).ifPresent(node -> isSelected.set(!isSelected.get())));

      isSelected.addListener((observable, oldVal, newVal) -> {
        changeMarkCount(instId, newVal);
        text.setUnderline(newVal);
      });
      callStackElemToMarkFlags.put(callStackElem, isSelected);

      IntegerProperty numMarked = instIdToNumMarked.get(instId);
      if (numMarked != null) {
        continue;
      }
      numMarked = new SimpleIntegerProperty(0);
      numMarked.addListener(
          (observable, oldVal, newVal) -> switchMarkViewState(instId, newVal.intValue()));
      instIdToNumMarked.put(instId, numMarked);
    }
  }

  private void changeMarkCount(InstanceId id, Boolean newVal) {
    IntegerProperty numMarked = instIdToNumMarked.get(id);
    if (numMarked != null) {
      numMarked.add(newVal ? 1 : -1);
    }
  }

  /** {@code instId} で指定した {@link BhNode} のマーク/非マーク表示を変更する. */
  private void switchMarkViewState(InstanceId instId, int numMarked) {
    BhNode.getBhNodeOf(instId).ifPresent(node -> {
      boolean enable = (numMarked <= 0) ? false : true;
      node.getViewProxy().switchPseudoClassState(BhConstants.Css.PSEUDO_SELECTED, enable);
    });
  }

  /**
   * {@code ws} にあるデバッグ情報のマークを消す.
   *
   * @code ws このワークスペース上にあるデバッグ情報のマークを消す.
   */
  public void removeMarksIn(Workspace ws) {
  }

  /** コールスタックの要素.*/
  private record CallStackElem(long threadId, InstanceId instId) {}
}
