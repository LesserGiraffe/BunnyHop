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
import java.util.Optional;
import java.util.Set;
import java.util.WeakHashMap;
import javafx.css.PseudoClass;
import javafx.scene.control.ListCell;
import net.seapanda.bunnyhop.common.configuration.BhConstants;
import net.seapanda.bunnyhop.node.model.BhNode;

/**
 * ブレークポイント一覧に表示される要素のビュー.
 *
 * @author K.Koike
 */
public class BreakpointListCell extends ListCell<BhNode> {

  private BhNode model;
  private boolean empty = true;
  private final Map<BhNode, Set<BreakpointListCell>> nodeToCells;

  /** コンストラクタ. */
  public BreakpointListCell(Map<BhNode, Set<BreakpointListCell>> nodeToCells) {
    this.nodeToCells = nodeToCells;
    getStyleClass().add(BhConstants.Css.Class.BREAKPOINT_LIST_ITEM);
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
    mapNodeToCell(item, empty);
    if (item != null) {
      decorateText(item.isSelected());
    }
    model = item;
    this.empty = empty;
  }

  private static String getText(BhNode item, boolean empty) {
    if (empty || item == null) {
      return null;
    }
    return item.getAlias();
  }

  /** {@link BreakpointListCell} オブジェクトが {@code item} と紐づく場合に UI に表示される文字列を返す. */
  public static String getText(BhNode item) {
    String text = getText(item, false);
    return text == null ? "" : text;
  }

  /** {@code node} とこのセルを {@link #nodeToCells} の中で対応付ける. */
  private void mapNodeToCell(BhNode node, boolean empty) {
    Optional.ofNullable(model)
        .filter(model -> empty || model != node)
        .filter(nodeToCells::containsKey)
        .ifPresent(model -> nodeToCells.get(model).remove(this));

    Optional.ofNullable(node)
        .filter(bhNode -> !empty)
        .filter(bhNode -> model != bhNode)
        .ifPresent(bhNode -> {
          nodeToCells
              .computeIfAbsent(
                  node,
                  key -> Collections.<BreakpointListCell>newSetFromMap(new WeakHashMap<>()))
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
