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

package net.seapanda.bunnyhop.control.node;

import net.seapanda.bunnyhop.message.BhMsg;
import net.seapanda.bunnyhop.message.MsgData;
import net.seapanda.bunnyhop.model.node.TextNode;
import net.seapanda.bunnyhop.modelservice.ModelExclusiveControl;
import net.seapanda.bunnyhop.view.node.TextAreaNodeView;
import net.seapanda.bunnyhop.view.node.TextFieldNodeView;
import net.seapanda.bunnyhop.view.node.TextInputNodeView;

/**
 * {@code TextFieldNodeView} のコントローラ.
 *
 * @author K.Koike
 */
public class TextInputNodeController extends BhNodeController {

  private final TextNode model;
  private final TextInputNodeView view;

  /** コンストラクタ. */
  public TextInputNodeController(TextNode model, TextFieldNodeView view) {
    super(model, view);
    this.model = model;
    this.view = view;
    if (model.isImitationNode()) {
      view.setEditable(false);
    }
    setTextChangeHandlers(model, view);
  }

  /** コンストラクタ. */
  public TextInputNodeController(TextNode model, TextAreaNodeView view) {
    super(model, view);
    this.model = model;
    this.view = view;
    if (model.isImitationNode()) {
      view.setEditable(false);
    }
    setTextChangeHandlers(model, view);
  }

  /**
   * TextNodeView に対して文字列変更時のハンドラを登録する.
   *
   * @param model TextNodeView に対応する model
   * @param view イベントハンドラを登録するview
   */
  public static void setTextChangeHandlers(TextNode model, TextInputNodeView view) {
    view.setTextFormatHandler(model::formatText);
    view.setTextChangeListener(model::isTextAcceptable);
    view.addFocusListener(
        (observable, oldValue, newValue) -> onFocusChanged(model, view, !newValue));

    String initText = model.getText();
    view.setText(initText + " ");  //初期文字列が空文字だったときのため
    view.setText(initText);
  }

  /** {@code TextInputNodeView} のフォーカスが外れた時のイベントハンドラ. */
  private static void onFocusChanged(
      TextNode model, TextInputNodeView view, Boolean isInputFinished) {
    //テキストフィールドにフォーカスが移ったとき
    if (!isInputFinished) {
      return;
    }
    ModelExclusiveControl.INSTANCE.lockForModification();
    try {
      String currentGuiText = view.getText();
      boolean isValidFormat = model.isTextAcceptable(view.getText());
      if (isValidFormat) {  //正しいフォーマットの文字列が入力されていた場合
        model.setText(currentGuiText);  //model の文字列をTextField のものに変更する
        model.getImitNodesToImitateContents();
      } else {
        view.setText(model.getText());  //view の文字列を変更前の文字列に戻す
      }
    } finally {
      ModelExclusiveControl.INSTANCE.unlockForModification();
    }
  }

  /**
   * 受信したメッセージを処理する.
   *
   * @param msg メッセージの種類
   * @param data メッセージの種類に応じて処理するデータ
   * @return メッセージを処理した結果返すデータ
   */
  @Override
  public MsgData processMsg(BhMsg msg, MsgData data) {
    switch (msg) {
      case IMITATE_TEXT:
        setText(model, view, data.strPair.v1, data.strPair.v2);
        break;

      case GET_VIEW_TEXT:
        return new MsgData(view.getText());

      default:
        return super.processMsg(msg, data);
    }
    return null;
  }

  /**
   * テキストノードとそのビューにテキストをセットする.
   *
   * @param model テキストをセットするノード
   * @param view テキストをセットするビュー
   * @param modelText {@code model} にセットする文字列
   * @param viewText {@code view} にセットする文字列
   */
  public static void setText(
      TextNode model, TextInputNodeView view, String modelText, String viewText) {
    model.setText(modelText);
    boolean editable = view.getEditable();
    view.setEditable(true);
    view.setText(viewText);
    view.setEditable(editable);
  }
}
