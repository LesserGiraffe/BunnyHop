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
import java.util.Collection;
import net.seapanda.bunnyhop.message.MsgService;
import net.seapanda.bunnyhop.model.node.BhNode;
import net.seapanda.bunnyhop.model.node.ConnectiveNode;
import net.seapanda.bunnyhop.model.node.TextNode;
import net.seapanda.bunnyhop.model.node.derivative.Derivative;

/**
 * テンプレートノードの派生ノードを集めるクラス.
 *
 * @author K.Koike
 */
public class TemplateDerivativeCollector implements BhModelProcessor {

  /** テンプレート派生ノードのリスト. */
  private Collection<Derivative> templateDerivatives = new ArrayList<>();

  /**
   * {@code node} で指定したノード以下にあるオリジナルノードが持つテンプレート派生ノードを集める.
   *
   * @param node このノード以下のオリジナルノードが持つテンプレート派生ノードを集める
   * @return {@code node} 以下のオリジナルノードが保持していたテンプレート派生ノードのリスト
   */
  public static Collection<Derivative> collect(BhNode node) {
    var deleter = new TemplateDerivativeCollector();
    node.accept(deleter);
    return deleter.templateDerivatives;
  }

  private TemplateDerivativeCollector() {}

  @Override
  public void visit(ConnectiveNode node) {
    node.getDerivatives().forEach(derv -> {
      if (MsgService.INSTANCE.isTemplateNode(derv)) {
        templateDerivatives.add(derv);
      }
    });
    node.sendToSections(this);
  }

  @Override
  public void visit(TextNode node) {
    node.getDerivatives().forEach(derv -> {
      if (MsgService.INSTANCE.isTemplateNode(derv)) {
        templateDerivatives.add(derv);
      }
    });
  }
}
