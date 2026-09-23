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

import static javafx.css.PseudoClass.getPseudoClass;

import java.util.function.Consumer;
import java.util.regex.Pattern;
import javafx.css.PseudoClass;
import javafx.scene.control.ListCell;
import javafx.scene.input.MouseEvent;
import net.seapanda.bunnyhop.common.configuration.BhConstants;
import net.seapanda.bunnyhop.common.text.TextDefs;
import net.seapanda.bunnyhop.debugger.model.callstack.CallStackItem;
import net.seapanda.bunnyhop.ui.skin.HighlightableListCellSkin;

/**
 * デバッガのコールスタックに表示される要素のビュー.
 *
 * @author K.Koike
 */
public class CallStackCell extends ListCell<CallStackItem> {

  private CallStackItem model;
  private boolean empty = true;
  private final HighlightableListCellSkin<CallStackItem> skin;
  private Consumer<? super ItemChangeEvent> onItemChanged = event -> {};

  /** コンストラクタ. */
  public CallStackCell() {
    getStyleClass().add(BhConstants.Css.Class.CALL_STACK_ITEM);
    skin = new HighlightableListCellSkin<>(this);
    setSkin(skin);
    addEventFilter(MouseEvent.MOUSE_PRESSED, this::onCellClicked);
  }

  private void onCellClicked(MouseEvent event) {
    changeSelectionState(event);
    event.consume();
    getListView().requestFocus();
  }

  private void changeSelectionState(MouseEvent event) {
    if (!event.isPrimaryButtonDown()) {
      return;
    }
    var selModel = getListView().getSelectionModel();
    if (empty
        || model == null
        || (model == selModel.getSelectedItem() && event.isShiftDown())) {
      selModel.clearSelection();
    } else {
      selModel.select(getIndex());
    }
  }

  @Override
  protected void updateItem(CallStackItem item, boolean empty) {
    super.updateItem(item, empty);
    setText(getText(item, empty));
    applyPseudoClass(item);
    onItemChanged.accept(new ItemChangeEvent(this, model, item, empty));
    model = item;
    this.empty = empty;
  }

  /** セルに {@code item} の状態に応じた疑似クラスを適用する. */
  private void applyPseudoClass(CallStackItem item) {
    if (item == null) {
      pseudoClassStateChanged(getPseudoClass(BhConstants.Css.Pseudo.NEXT), false);
      pseudoClassStateChanged(getPseudoClass(BhConstants.Css.Pseudo.ERROR), false);
      return;
    }
    pseudoClassStateChanged(getPseudoClass(BhConstants.Css.Pseudo.NEXT), item.isNext);
    pseudoClassStateChanged(getPseudoClass(BhConstants.Css.Pseudo.ERROR), item.isError);
  }

  private static String getText(CallStackItem item, boolean empty) {
    if (empty || item == null) {
      return null;
    }
    if (item.isNext) {
      return "[%s]    %s".formatted(TextDefs.Debugger.CallStack.next.get(), item.name);
    }
    if (item.isError) {
      return "[%s]    %s".formatted(TextDefs.Debugger.CallStack.error.get(), item.name);
    }
    if (item.idx < 0) {
      return "      %s".formatted(item.name);
    }
    return "[%s]    %s".formatted(item.idx, item.name);
  }

  /** {@link CallStackCell} オブジェクトが {@code item} と紐づく場合に UI に表示される文字列を返す. */
  public static String getText(CallStackItem item) {
    String text = getText(item, false);
    return text == null ? "" : text;
  }

  /** このセルに描画される文字を装飾する. */
  public void decorateText(boolean val) {
    PseudoClass cls = getPseudoClass(BhConstants.Css.Pseudo.TEXT_DECORATE);
    pseudoClassStateChanged(cls, val);
  }

  /**
   * このセルのテキストの強調表示を有効化する.
   *
   * @param pattern 強調表示する文字列の正規表現
   * @param styleClass 強調表示部分に適用するスタイルクラス
   */
  public void enableHighlighting(Pattern pattern, String styleClass) {
    skin.enableHighlighting(pattern, styleClass);
  }

  /** このセルのテキストの強調表示を無効化する. */
  public void disableHighlighting() {
    skin.disableHighlighting();
  }

  /** このセルに割り当てられたアイテムが変わったときのイベントハンドラを設定する. */
  public void setOnItemChanged(Consumer<? super ItemChangeEvent> handler) {
    if (handler == null) {
      handler = event -> {};
    }
    onItemChanged = handler;
  }

  /** このセルに割り当てられたアイテムが変わったときのイベント. */
  public record ItemChangeEvent(
      CallStackCell cell, CallStackItem oldVal, CallStackItem newVal, boolean empty) {}
}
