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

import java.util.function.Consumer;
import java.util.regex.Pattern;
import javafx.css.PseudoClass;
import javafx.scene.control.TreeCell;
import net.seapanda.bunnyhop.common.configuration.BhConstants;
import net.seapanda.bunnyhop.linter.model.ErrorNodeListItem;
import net.seapanda.bunnyhop.ui.skin.HighlightableTreeCellSkin;

/**
 * デバッガのエラーノード一覧に表示される要素のビュー.
 *
 * @author K.Koike
 */
public class ErrorNodeListCell extends TreeCell<ErrorNodeListItem> {

  private ErrorNodeListItem model;
  private final HighlightableTreeCellSkin<ErrorNodeListItem> skin;
  private Consumer<? super ItemChangeEvent> onItemChanged = event -> {};

  /** コンストラクタ. */
  public ErrorNodeListCell() {
    getStyleClass().add(BhConstants.Css.Class.ERROR_NODE_LIST_ITEM);
    skin = new HighlightableTreeCellSkin<>(this);
    setSkin(skin);
  }

  @Override
  protected void updateItem(ErrorNodeListItem item, boolean empty) {
    super.updateItem(item, empty);
    setText(getText(item, empty));
    onItemChanged.accept(new ItemChangeEvent(this, model, item, empty));
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

  /** このセルが表示する値を更新する. */
  public void updateValue() {
    setText(getText(model, isEmpty()));
  }

  /** このセルに描画される文字を装飾する. */
  public void decorateText(boolean val) {
    PseudoClass cls = PseudoClass.getPseudoClass(BhConstants.Css.Pseudo.TEXT_DECORATE);
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
      ErrorNodeListCell cell, ErrorNodeListItem oldVal, ErrorNodeListItem newVal, boolean empty) {}
}

