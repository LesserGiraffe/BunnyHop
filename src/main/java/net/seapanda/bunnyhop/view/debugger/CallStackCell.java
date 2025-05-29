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

package net.seapanda.bunnyhop.view.debugger;

import javafx.css.PseudoClass;
import javafx.scene.control.ListCell;
import net.seapanda.bunnyhop.bhprogram.debugger.CallStackItem;
import net.seapanda.bunnyhop.common.BhConstants;
import net.seapanda.bunnyhop.model.node.BhNode;
import net.seapanda.bunnyhop.undo.UserOperation;
import net.seapanda.bunnyhop.utility.function.TriConsumer;

/**
 * デバッガのコールスタックに表示される要素のビュー.
 * <p>
 * ListView のモデルとビューの結びつきは動的に変わる.
 * </p>
 *
 * @author K.Koike
 */
public class CallStackCell extends ListCell<CallStackItem> {

  private CallStackItem model;
  private boolean empty = true;
  /** {@link #model} に対応する {@link BhNode} の選択状態が変わったときのイベントハンドラ. */
  private final TriConsumer<? super BhNode, ? super Boolean, ? super UserOperation>
      onNodeSelectionStateChanged = (node, val, userOpe) -> decorateText(val);

  /** コンストラクタ. */
  public CallStackCell() {
    getStyleClass().add(BhConstants.Css.CALL_STACK_ITEM);
    setOnMousePressed(event -> clearSelectionIfEmpty());
  }

  private void clearSelectionIfEmpty() {
    if (empty || model == null) {
      getListView().getSelectionModel().clearSelection();
    }
  }

  @Override
  protected void updateItem(CallStackItem item, boolean empty) {
    super.updateItem(item, empty);
    setText(getText(item, empty));
    setEventHandlers(item, empty);
    if (item != null) {
      decorateText(item.getNode().map(BhNode::isSelected).orElse(false));
    }
    model = item;
    this.empty = empty;
  }

  private String getText(CallStackItem item, boolean empty) {
    if (empty || item == null) {
      return null;
    }
    if (item.getId() < 0) {
      return "      %s".formatted(item.getName());
    }
    return "[%s]    %s".formatted(item.getId(), item.getName());
  }

  private void setEventHandlers(CallStackItem item, boolean empty) {
    if ((empty || model != item) && model != null) {
      model.getNode().ifPresent(node ->
          node.getEventManager().removeOnSelectionStateChanged(onNodeSelectionStateChanged));
    }
    if (!empty && model != item && item != null) {
      item.getNode().ifPresent(node ->
          node.getEventManager().addOnSelectionStateChanged(onNodeSelectionStateChanged));
    }
  }

  private void decorateText(boolean val) {
    PseudoClass cls = PseudoClass.getPseudoClass(BhConstants.Css.PSEUDO_TEXT_DECORATE);
    if (val) {
      pseudoClassStateChanged(cls, true);
    } else {
      pseudoClassStateChanged(cls, false);
    }
  }
}
