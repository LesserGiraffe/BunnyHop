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

import net.seapanda.bunnyhop.model.ModelAccessNotificationService;
import net.seapanda.bunnyhop.model.node.BhNode;
import net.seapanda.bunnyhop.model.node.TextNode;
import net.seapanda.bunnyhop.view.node.BhNodeView;
import net.seapanda.bunnyhop.view.node.TextInputNodeView;

/**
 * {@code TextFieldNodeView} のコントローラ.
 *
 * @author K.Koike
 */
public class TextInputNodeController implements BhNodeController {

  private final TextNode model;
  private final TextInputNodeView view;
  private final ModelAccessNotificationService notifService;

  /** コンストラクタ. */
  public TextInputNodeController(BhNodeController controller) {
    if (controller.getModel() instanceof TextNode model) {
      this.model = model;
    } else {
      throw new IllegalStateException(
          "The model is not %s".formatted(TextNode.class.getSimpleName()));
    }

    if (controller.getView() instanceof TextInputNodeView view) {
      this.view = view;
    } else {
      throw new IllegalStateException(
          "The view is not %s".formatted(TextInputNodeView.class.getSimpleName()));
    }
    notifService = controller.getNotificationService();
    setEventHandlers();
  }

  /** TextInputNodeView の文字列変更時のハンドラを登録する. */
  private void setEventHandlers() {
    view.setTextFormatter(model::formatText);
    view.setTextChangeListener(model::isTextAcceptable);
    view.addFocusListener(
        (observable, oldValue, newValue) -> onFocusChanged(!newValue));

    String initText = model.getText();
    view.setText(initText + " ");  //初期文字列が空文字だったときのため
    view.setText(initText);
    model.getCallbackRegistry().getOnTextChanged().add(event -> view.setText(event.newText()));
  }

  /** {@code TextInputNodeView} のフォーカスが外れた時のイベントハンドラ. */
  private void onFocusChanged(Boolean isInputFinished) {
    try {
      notifService.begin();
      if (!isInputFinished) {
        return;
      }
      String currentGuiText = view.getText();
      boolean isValidFormat = model.isTextAcceptable(currentGuiText);
      if (isValidFormat) {  //正しいフォーマットの文字列が入力されていた場合
        model.setText(currentGuiText);  //model の文字列をTextField のものに変更する
        model.assignContentsToDerivatives();
      } else {
        view.setText(model.getText());  //view の文字列を変更前の文字列に戻す
      }
    } finally {
      notifService.end();
    }
  }

  @Override
  public BhNode getModel() {
    return model;
  }

  @Override
  public BhNodeView getView() {
    return view;
  }

  @Override
  public ModelAccessNotificationService getNotificationService() {
    return notifService;
  }
}
