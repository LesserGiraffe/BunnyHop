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

package net.seapanda.bunnyhop.node.control;

import net.seapanda.bunnyhop.node.model.BhNode;
import net.seapanda.bunnyhop.node.model.TextNode;
import net.seapanda.bunnyhop.node.view.BhNodeView;
import net.seapanda.bunnyhop.node.view.TextInputNodeView;
import net.seapanda.bunnyhop.service.accesscontrol.TransactionNotificationService;

/**
 * {@code TextFieldNodeView} のコントローラ.
 *
 * @author K.Koike
 */
public class TextInputNodeController implements BhNodeController {

  private final TextNode model;
  private final TextInputNodeView view;
  private final TransactionNotificationService notifService;

  /** コンストラクタ. */
  public TextInputNodeController(BhNodeController controller) {
    if (controller.getModel() instanceof TextNode node) {
      model = node;
    } else {
      throw new IllegalStateException(
          "The model is not %s".formatted(TextNode.class.getSimpleName()));
    }

    if (controller.getView() instanceof TextInputNodeView nodeView) {
      view = nodeView;
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
    view.setFormatChecker(model::isTextAcceptable);
    view.addOnFocusChanged((observable, oldValue, newValue) -> onFocusChanged(newValue));

    String initText = model.getText();
    view.setText(initText + " ");  //初期文字列が空文字だったときのため
    view.setText(initText);
    model.getCallbackRegistry().getOnTextChanged().add(event -> view.setText(event.newText()));
  }

  private void onFocusChanged(Boolean focused) {
    try {
      notifService.begin();
      if (focused) {
        return;
      }
      model.setText(view.getText());
      model.assignContentsToDerivatives();
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
  public TransactionNotificationService getNotificationService() {
    return notifService;
  }
}
