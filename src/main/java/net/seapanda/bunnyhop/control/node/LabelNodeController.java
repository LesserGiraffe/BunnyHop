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

import net.seapanda.bunnyhop.model.node.TextNode;
import net.seapanda.bunnyhop.view.node.LabelNodeView;
import net.seapanda.bunnyhop.view.proxy.TextNodeViewProxy;

/**
 * {@link LabelNodeView} のコントローラ.
 *
 * @author K.Koike
 */
public class LabelNodeController extends BhNodeController {

  private final TextNode model;
  private final LabelNodeView view;

  /** コンストラクタ. */
  public LabelNodeController(TextNode model, LabelNodeView view) {
    super(model, view);
    this.model = model;
    this.view = view;
    model.setViewProxy(new TextNodeViewProxyImpl(view));
    setInitStr(model, view);
  }

  /**
   * view に初期文字列をセットする.
   *
   * @param model セット初期文字列を持つTextNode
   * @param view 初期文字列をセットするLabelNodeView
   */
  public static void setInitStr(TextNode model, LabelNodeView view) {
    String initText = model.getText();
    view.setText(initText + " ");  //初期文字列が空文字だったときのため
    view.setText(initText);
  }

  /** {@code model} の持つ文字列に合わせて {@code view} の内容を変更する. */
  public static void matchViewToModel(TextNode model, LabelNodeView view) {
    view.setText(model.getText());
  }

  private class TextNodeViewProxyImpl extends BhNodeViewProxyImpl implements TextNodeViewProxy {

    public TextNodeViewProxyImpl(LabelNodeView view) {
      super(view, false);
    }

    @Override
    public void matchViewContentToModel() {
      view.setText(model.getText());
    }
  }
}
