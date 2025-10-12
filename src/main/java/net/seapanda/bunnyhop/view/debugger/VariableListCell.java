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

import java.util.Collections;
import java.util.Map;
import java.util.Set;
import java.util.WeakHashMap;
import java.util.function.Consumer;
import javafx.css.PseudoClass;
import javafx.scene.control.TreeCell;
import net.seapanda.bunnyhop.bhprogram.debugger.VariableListItem;
import net.seapanda.bunnyhop.common.BhConstants;
import net.seapanda.bunnyhop.model.node.BhNode;

/**
 * デバッガの変数一覧に表示される要素のビュー.
 *
 * @author K.Koike
 */
public class VariableListCell extends TreeCell<VariableListItem> {

  private VariableListItem model;
  private final Consumer<? super VariableListItem.ValueChangedEvent>
      onVarValueChanged = event -> setText(model.toString());
  private final Map<VariableListItem, Set<VariableListCell>> itemToCells;
  private final Map<BhNode, Set<VariableListCell>> nodeToCells;

  /** コンストラクタ. */
  public VariableListCell(
      Map<VariableListItem, Set<VariableListCell>> itemToCells,
      Map<BhNode, Set<VariableListCell>> nodeToCells) {
    this.itemToCells = itemToCells;
    this.nodeToCells = nodeToCells;
    getStyleClass().add(BhConstants.Css.VARIABLE_LIST_ITEM);
  }

  @Override
  protected void updateItem(VariableListItem item, boolean empty) {
    super.updateItem(item, empty);
    setText(getText(item, empty));
    mapItemToCell(item, empty);
    mapCellToNode(item, empty);
    if (item != null) {
      decorateText(item.variable.getNode().map(BhNode::isSelected).orElse(false));
    }
    model = item;
  }

  private String getText(VariableListItem item, boolean empty) {
    if (empty || item == null) {
      return null;
    }
    return item.toString();
  }

  /** {@code item} とこのセルを {@link #itemToCells} の中で対応付ける. */
  private void mapItemToCell(VariableListItem item, boolean empty) {
    if ((empty || model != item) && model != null) {
      if (itemToCells.containsKey(item)) {
        itemToCells.get(item).remove(this);
      }
    }
    if (!empty && model != item && item != null) {
      itemToCells
          .computeIfAbsent(
              item,
              key -> Collections.<VariableListCell>newSetFromMap(new WeakHashMap<>()))
          .add(this);
    }
  }

  /** {@code item} に対応する {@link BhNode} とこのセルを {@link #nodeToCells} の中で対応付ける. */
  private void mapCellToNode(VariableListItem item, boolean empty) {
    if ((empty || model != item) && model != null) {
      model.variable.getNode().ifPresent(node -> {
        if (nodeToCells.containsKey(node)) {
          nodeToCells.get(node).remove(this);
        }
      });
    }
    if (!empty && model != item && item != null) {
      item.variable.getNode().ifPresent(node -> {
        nodeToCells
            .computeIfAbsent(
                node,
                key -> Collections.<VariableListCell>newSetFromMap(new WeakHashMap<>()))
            .add(this);
      });
    }
  }

  /** このセルが表示する値を更新する. */
  public void updateValue() {
    if (model != null) {
      setText(model.toString());
    }
  }

  /** このセルに描画される文字を装飾する. */
  public void decorateText(boolean val) {
    PseudoClass cls = PseudoClass.getPseudoClass(BhConstants.Css.PSEUDO_TEXT_DECORATE);
    pseudoClassStateChanged(cls, val);
  }
}
