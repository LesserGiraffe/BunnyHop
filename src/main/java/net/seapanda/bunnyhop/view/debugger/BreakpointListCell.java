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

import java.util.function.Consumer;
import javafx.css.PseudoClass;
import javafx.scene.control.ListCell;
import net.seapanda.bunnyhop.common.BhConstants;
import net.seapanda.bunnyhop.model.node.BhNode;

/**
 * ブレークポイント一覧に表示される要素のビュー.
 *
 * @author K.Koike
 */
public class BreakpointListCell extends ListCell<BhNode> {

  private BhNode model;
  private boolean empty = true;
  /** {@link #model} に対応する {@link BhNode} の選択状態が変わったときのイベントハンドラ. */
  private final Consumer<? super BhNode.SelectionEvent>
      onNodeSelStateChanged = event -> decorateText(event.isSelected());

  /** コンストラクタ. */
  public BreakpointListCell() {
    getStyleClass().add(BhConstants.Css.BREAKPOINT_LIST_ITEM);
    setOnMousePressed(event -> clearSelectionIfEmpty());
  }

  private void clearSelectionIfEmpty() {
    if (empty || model == null) {
      getListView().getSelectionModel().clearSelection();
    }
  }

  @Override
  protected void updateItem(BhNode item, boolean empty) {
    super.updateItem(item, empty);
    setText(getText(item, empty));
    setEventHandlers(item, empty);
    if (item != null) {
      decorateText(item.isSelected());
    }
    model = item;
    this.empty = empty;
  }

  private String getText(BhNode item, boolean empty) {
    if (empty || item == null) {
      return null;
    }
    return item.getAlias();
  }

  private void setEventHandlers(BhNode item, boolean empty) {
    if ((empty || model != item) && model != null) {
      model.getCallbackRegistry().getOnSelectionStateChanged().remove(onNodeSelStateChanged);
    }
    if (!empty && model != item && item != null) {
      item.getCallbackRegistry().getOnSelectionStateChanged().add(onNodeSelStateChanged);
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
