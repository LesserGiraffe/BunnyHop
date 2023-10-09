/**
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
package net.seapanda.bunnyhop.modelprocessor;

import net.seapanda.bunnyhop.message.MsgService;
import net.seapanda.bunnyhop.model.node.BhNode;
import net.seapanda.bunnyhop.model.node.TextNode;

/**
 * テキストイミテーションノードにオリジナルの模倣をさせるクラス
 * @author K.Koike
 */
public class TextImitationPrompter implements BhModelProcessor {

  /**
   * 引数で指定したノード以下のイミテーションテキストノードにオリジナルノードのテキストを真似させる.
   * */
  public static void prompt(BhNode node) {
    node.accept(new TextImitationPrompter());
  }

  private TextImitationPrompter() {}

  @Override
  public void visit(TextNode node) {

    if(node.isImitationNode()) {
      TextNode original = node.getOriginal();
      String viewText = MsgService.INSTANCE.getViewText(original);
      MsgService.INSTANCE.imitateText(node, original.getText(), viewText);
    }
  }
}
