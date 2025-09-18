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

package net.seapanda.bunnyhop.model.node.derivative;

import java.util.HashSet;
import java.util.Set;
import net.seapanda.bunnyhop.model.node.BhNode;
import net.seapanda.bunnyhop.model.node.ConnectiveNode;
import net.seapanda.bunnyhop.model.node.TextNode;
import net.seapanda.bunnyhop.model.node.traverse.BhNodeWalker;

/**
 * 走査したノードが保持する派生ノードを集めるクラス.
 *
 * @author K.Koike
 */
public class DerivativeCollector implements BhNodeWalker {

  private Set<Derivative> derivatives = new HashSet<>();

  /**
   * 引数で指定したノード以下のオリジナルノードが持つ派生ノードを全て返す.
   *
   * @param node このノード以下のオリジナルノードが持つ派生ノードを探す
   * @return 発見した派生ノードのリスト
   */
  public static Set<Derivative> collect(BhNode node) {
    var finder = new DerivativeCollector();
    node.accept(finder);
    return finder.derivatives;
  }

  /** コンストラクタ. */
  private DerivativeCollector() {}

  @Override
  public void visit(ConnectiveNode node) {
    derivatives.addAll(node.getDerivatives());
    node.sendToSections(this);
  }

  @Override
  public void visit(TextNode node) {
    derivatives.addAll(node.getDerivatives());
  }
}
