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
import net.seapanda.bunnyhop.node.view.LabelNodeView;
import net.seapanda.bunnyhop.service.accesscontrol.TransactionNotificationService;

/**
 * {@link LabelNodeView} のコントローラ.
 *
 * @author K.Koike
 */
public class LabelNodeController implements BhNodeController {

  private final TextNode model;
  private final LabelNodeView view;
  private final TransactionNotificationService notifService;

  /** コンストラクタ. */
  public LabelNodeController(BhNodeController controller) {
    if (controller.getModel() instanceof TextNode model) {
      this.model = model;
    } else {
      throw new IllegalStateException(
          "The model is not %s".formatted(TextNode.class.getSimpleName()));
    }
    if (controller.getView() instanceof LabelNodeView view) {
      this.view = view;
    } else {
      throw new IllegalStateException(
          "The view is not %s".formatted(LabelNodeView.class.getSimpleName()));
    }
    notifService = controller.getNotificationService();
    model.getCallbackRegistry().getOnTextChanged().add(event -> view.setText(event.newText()));
    setInitStr();
  }

  /**
   * {@link #view} に初期文字列をセットする.
   */
  private void setInitStr() {
    String initText = model.getText();
    view.setText(initText + " ");  //初期文字列が空文字だったときのため
    view.setText(initText);
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
