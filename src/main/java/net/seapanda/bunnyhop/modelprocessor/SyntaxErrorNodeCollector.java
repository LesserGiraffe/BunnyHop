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

package net.seapanda.bunnyhop.modelprocessor;

import java.util.ArrayList;
import java.util.List;
import net.seapanda.bunnyhop.model.node.BhNode;
import net.seapanda.bunnyhop.model.node.ConnectiveNode;
import net.seapanda.bunnyhop.model.node.TextNode;
import net.seapanda.bunnyhop.model.node.derivative.Derivative;

/**
 * 構文エラーノードを探して集めるクラス.
 *
 * @author K.Koike
 * */
public class SyntaxErrorNodeCollector implements BhModelProcessor {

  private final List<BhNode> errorNodeList = new ArrayList<>();

  /**
   * <pre>
   * 以下の2種類の構文エラーノードを管理対象に入れる.
   *   ・{@code node} 以下にある構文エラーノード
   *   ・{@code node} 以下にあるオリジナルノードが持つ派生ノードで構文エラーを起こしているノード
   * </pre>
   */
  public static List<BhNode> collect(BhNode node) {
    var collector = new SyntaxErrorNodeCollector();
    node.accept(collector);
    return collector.errorNodeList;
  }

  private SyntaxErrorNodeCollector() {}

  @Override
  public void visit(ConnectiveNode node) {
    node.sendToSections(this);
    for (Derivative derivative : node.getDerivatives()) {
      if (derivative.hasSyntaxError()) {
        errorNodeList.add(derivative);
      }
    }
    if (node.hasSyntaxError()) {
      errorNodeList.add(node);
    }
  }

  @Override
  public void visit(TextNode node) {
    for (Derivative derivative : node.getDerivatives()) {
      if (derivative.hasSyntaxError()) {
        errorNodeList.add(derivative);
      }
    }
    if (node.hasSyntaxError()) {
      errorNodeList.add(node);
    }
  }
}
