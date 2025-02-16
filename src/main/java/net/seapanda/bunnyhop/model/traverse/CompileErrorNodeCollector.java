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

import java.util.ArrayList;
import java.util.List;
import net.seapanda.bunnyhop.model.node.BhNode;
import net.seapanda.bunnyhop.model.node.ConnectiveNode;
import net.seapanda.bunnyhop.model.node.TextNode;
import net.seapanda.bunnyhop.model.node.derivative.Derivative;

/**
 * コンパイルエラーノードを探して集めるクラス.
 *
 * @author K.Koike
 * */
public class CompileErrorNodeCollector implements BhNodeWalker {

  private final List<BhNode> errorNodeList = new ArrayList<>();

  /**
   * <pre>
   * 以下の2種類のコンパイルエラーノードを管理対象に入れる.
   *   ・{@code node} 以下にあるコンパイルエラーノード
   *   ・{@code node} 以下にあるオリジナルノードが持つ派生ノードでコンパイルエラーを起こしているノード
   * </pre>
   */
  public static List<BhNode> collect(BhNode node) {
    var collector = new CompileErrorNodeCollector();
    node.accept(collector);
    return collector.errorNodeList;
  }

  private CompileErrorNodeCollector() {}

  /** {@code node} の派生ノードがエラーを持つかチェックする. */
  private void checkDerivatives(Derivative node) {
    for (Derivative derivative : node.getDerivatives()) {
      if (derivative.hasCompileError()) {
        errorNodeList.add(derivative);
      }
      checkDerivatives(derivative);
    }
  }

  @Override
  public void visit(ConnectiveNode node) {
    node.sendToSections(this);
    checkDerivatives(node);
    if (node.hasCompileError()) {
      errorNodeList.add(node);
    }
  }

  @Override
  public void visit(TextNode node) {
    checkDerivatives(node);
    if (node.hasCompileError()) {
      errorNodeList.add(node);
    }
  }
}
