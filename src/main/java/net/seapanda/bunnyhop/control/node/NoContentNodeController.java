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
import net.seapanda.bunnyhop.view.node.NoContentNodeView;

/**
 * {@code NoContentNodeView} のコントローラ.
 *
 * @author K.Koike
 */
public class NoContentNodeController extends BhNodeController {

  private final TextNode model;

  /** コンストラクタ. */
  public NoContentNodeController(TextNode model, NoContentNodeView view) {
    super(model, view);
    this.model = model;
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
      case MATCH_VIEW_CONTENT_TO_MODEL:
        break;

      default:
        return super.processMsg(msg, data);
    }
    return null;
  }
}
