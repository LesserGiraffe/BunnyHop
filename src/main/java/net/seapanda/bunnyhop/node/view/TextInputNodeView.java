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

package net.seapanda.bunnyhop.node.view;

import java.util.SequencedSet;
import java.util.function.BiFunction;
import java.util.function.Function;
import javafx.beans.value.ChangeListener;
import javafx.scene.Node;
import javafx.scene.control.TextFormatter;
import javafx.scene.control.TextFormatter.Change;
import javafx.scene.control.TextInputControl;
import net.seapanda.bunnyhop.node.model.TextNode;
import net.seapanda.bunnyhop.node.model.TextNode.FormatResult;
import net.seapanda.bunnyhop.node.view.style.BhNodeViewStyle;
import net.seapanda.bunnyhop.ui.view.ViewConstructionException;

/**
 * テキスト入力可能な NodeView の基底クラス.
 *
 * @author K.Koike
 */
public abstract class TextInputNodeView extends BhNodeViewBase {

  protected TextInputNodeView(
      TextNode model, BhNodeViewStyle viewStyle, SequencedSet<Node> components)
      throws ViewConstructionException {
    super(viewStyle, model, components);
  }

  /** テキスト入力用GUIコンポーネントを取得する. */
  protected abstract TextInputControl getTextInputControl();

  /**
   * テキスト変更時のイベントハンドラを登録する.
   *
   * @param fnCheckFormat 入力された文字列の形式が正しいかどうか判断する関数 (テキスト変更時のイベントハンドラから呼び出す)
   */
  public abstract void setTextChangeListener(Function<String, Boolean> fnCheckFormat);

  /**
   * テキストフィールドのカーソル on/off 時のイベントハンドラを登録する.
   *
   * @param changeFocusFunc テキストフィールドのカーソルon/off時のイベントハンドラ
   */
  public final void addFocusListener(ChangeListener<? super Boolean> changeFocusFunc) {
    getTextInputControl().focusedProperty().addListener(changeFocusFunc);
  }

  /**
   * テキストを整形する関数を登録する.
   *
   * @param formatter
   *     <pre>
   *     テキストを整形する関数.
   *     formatterFunc の第1引数 -> 整形対象の全文字列
   *     formatterFunc の第2引数 -> 前回整形したテキストから新たに追加された文字列
   *     formatterFunc の戻り値
   *     &nbsp;&nbsp; v1 -> テキスト全体を整形した場合 true. 追加分だけ整形した場合 false.
   *     &nbsp;&nbsp; v2 -> 整形したテキスト
   *     </pre>
   */
  public final void setTextFormatter(
      BiFunction<String, String, FormatResult> formatter) {
    TextInputControl control = getTextInputControl();
    control.setTextFormatter(new TextFormatter<Object>(
        change -> setFormattedText(formatter, control.getLength(), change)));
  }

  /**
   * フォーマットされたテキストを {@code Change} オブジェクトにセットする.
   *
   * @param formatter テキストをフォーマットする関数
   * @param textLen {@code change} オブジェクトに対応するコントロールの現在のテキスト長
   * @param change テキスト入力コンポーネントの変更を表すオブジェクト
   */
  private Change setFormattedText(
      BiFunction<String, String, FormatResult> formatter, int textLen, Change change) {
    FormatResult result = formatter.apply(change.getControlNewText(), change.getText());
    if (result.isWholeFormatted()) {
      change.setRange(0, textLen);
    }
    change.setText(result.text());
    return change;
  }

  /** View に表示されたテキストを取得する. */
  public final String getText() {
    return getTextInputControl().getText();
  }

  /** View に表示するテキストを設定する. */
  public final void setText(String text) {
    getTextInputControl().setText(text);
  }


  /** テキストフィールドが編集可能かどうかをセットする.
   *
   * @param editable テキストフィールドが編集可能なときtrue
   */
  public final void setEditable(boolean editable) {
    getTextInputControl().setEditable(editable);
  }

  /**
   * テキストフィールドが編集可能かどうかチェックする.
   *
   * @return テキストフィールドが編集可能な場合 true
   */
  public final boolean getEditable() {
    return getTextInputControl().editableProperty().getValue();
  }
}
