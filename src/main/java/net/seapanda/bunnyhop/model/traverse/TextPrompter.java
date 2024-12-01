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

package net.seapanda.bunnyhop.model.traverse;

import net.seapanda.bunnyhop.model.node.BhNode;
import net.seapanda.bunnyhop.model.node.TextNode;
import net.seapanda.bunnyhop.service.BhService;

/**
 * {@link TextNode} 型の派生ノードにオリジナルのテキストをセットするクラス.
 *
 * @author K.Koike
 */
public class TextPrompter implements BhNodeWalker {

  /**
   * {@code node} で指定したノード以下の {@link TextNode} 型の派生ノードに, そのオリジナルノードのテキストをセットする.
   */
  public static void prompt(BhNode node) {
    node.accept(new TextPrompter());
  }

  private TextPrompter() {}

  @Override
  public void visit(TextNode node) {
    if (node.isDerivative()) {
      node.setText(node.getOriginal().getText());
      BhService.cmdProxy().matchViewContentToModel(node);
    }
  }
}
