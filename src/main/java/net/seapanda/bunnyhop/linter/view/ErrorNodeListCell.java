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

package net.seapanda.bunnyhop.linter.view;

import java.util.Collections;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.WeakHashMap;
import javafx.css.PseudoClass;
import javafx.scene.control.TreeCell;
import net.seapanda.bunnyhop.common.configuration.BhConstants;
import net.seapanda.bunnyhop.linter.model.ErrorNodeListItem;
import net.seapanda.bunnyhop.node.model.BhNode;

/**
 * デバッガの変数一覧に表示される要素のビュー.
 *
 * @author K.Koike
 */
public class ErrorNodeListCell extends TreeCell<ErrorNodeListItem> {

  private ErrorNodeListItem model;
  private final Map<BhNode, Set<ErrorNodeListCell>> nodeToCells;

  /** コンストラクタ. */
  public ErrorNodeListCell(Map<BhNode, Set<ErrorNodeListCell>> nodeToCells) {
    this.nodeToCells = nodeToCells;
    getStyleClass().add(BhConstants.Css.Class.ERROR_NODE_LIST_ITEM);
  }

  @Override
  protected void updateItem(ErrorNodeListItem item, boolean empty) {
    super.updateItem(item, empty);
    setText(getText(item, empty));
    mapCellToNode(item, empty);
    if (item != null && item.node() != null) {
      decorateText(item.node().isSelected());
    }
    model = item;
  }

  private static String getText(ErrorNodeListItem item, boolean empty) {
    if (empty || item == null) {
      return null;
    }
    return item.toString();
  }

  /** {@link ErrorNodeListCell} オブジェクトが {@code item} と紐づく場合に UI に表示される文字列を返す. */
  public static String getText(ErrorNodeListItem item) {
    String text = getText(item, false);
    return text == null ? "" : text;
  }

  /** {@code item} に対応する {@link BhNode} とこのセルを {@link #nodeToCells} の中で対応付ける. */
  private void mapCellToNode(ErrorNodeListItem item, boolean empty) {
    Optional.ofNullable(model)
        .filter(model -> empty || model != item)
        .map(ErrorNodeListItem::node)
        .filter(nodeToCells::containsKey)
        .ifPresent(node -> nodeToCells.get(node).remove(this));

    Optional.ofNullable(item)
        .filter(itm -> !empty)
        .filter(itm -> model != itm)
        .map(ErrorNodeListItem::node)
        .ifPresent(node -> {
          nodeToCells
              .computeIfAbsent(
                  node, key -> Collections.<ErrorNodeListCell>newSetFromMap(new WeakHashMap<>()))
              .add(this);
        });
  }

  /** このセルが表示する値を更新する. */
  public void updateValue() {
    if (model != null) {
      setText(getText(model, false));
    }
  }

  /** このセルに描画される文字を装飾する. */
  public void decorateText(boolean val) {
    PseudoClass cls = PseudoClass.getPseudoClass(BhConstants.Css.Pseudo.TEXT_DECORATE);
    pseudoClassStateChanged(cls, val);
  }
}

