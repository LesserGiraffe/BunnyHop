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

package net.seapanda.bunnyhop.debugger.view;

import java.util.Collections;
import java.util.Map;
import java.util.Set;
import java.util.WeakHashMap;
import javafx.css.PseudoClass;
import javafx.scene.control.ListCell;
import net.seapanda.bunnyhop.common.configuration.BhConstants;
import net.seapanda.bunnyhop.debugger.model.callstack.CallStackItem;
import net.seapanda.bunnyhop.node.model.BhNode;

/**
 * デバッガのコールスタックに表示される要素のビュー.
 *
 * @author K.Koike
 */
public class CallStackCell extends ListCell<CallStackItem> {

  private CallStackItem model;
  private boolean empty = true;
  private final Map<BhNode, Set<CallStackCell>> nodeToCells;

  /** コンストラクタ. */
  public CallStackCell(Map<BhNode, Set<CallStackCell>> nodeToCells) {
    this.nodeToCells = nodeToCells;
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
    mapNodeToCell(item, empty);
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
    if (item.getIdx() < 0) {
      return "      %s".formatted(item.getName());
    }
    return "[%s]    %s".formatted(item.getIdx(), item.getName());
  }

  /** {@code item} が持つ {@link BhNode} とこのセルを {@link #nodeToCells} の中で対応付ける. */
  private void mapNodeToCell(CallStackItem item, boolean empty) {
    if ((empty || model != item) && model != null) {
      model.getNode().ifPresent(node -> {
        if (nodeToCells.containsKey(node)) {
          nodeToCells.get(node).remove(this);
        }
      });
    }
    if (!empty && model != item && item != null) {
      item.getNode().ifPresent(node -> {
        nodeToCells
            .computeIfAbsent(
                node,
                key -> Collections.<CallStackCell>newSetFromMap(new WeakHashMap<>()))
            .add(this);
      });
    }
  }

  /** このセルに描画される文字を装飾する. */
  public void decorateText(boolean val) {
    PseudoClass cls = PseudoClass.getPseudoClass(BhConstants.Css.PSEUDO_TEXT_DECORATE);
    pseudoClassStateChanged(cls, val);
  }
}
