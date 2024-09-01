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
import net.seapanda.bunnyhop.model.node.TextNode;
import net.seapanda.bunnyhop.model.node.connective.ConnectiveNode;
import net.seapanda.bunnyhop.model.node.imitation.Imitatable;

/**
 * テンプレートノードのイミテーションノードを集めるクラス.
 *
 * @author K.Koike
 */
public class TemplateImitationCollector implements BhModelProcessor {

  /** テンプレートイミテーションノードのリスト. */
  private Collection<Imitatable> templateImitationNodes = new ArrayList<>();

  /**
   * 引数で指定したノード以下にあるオリジナルノードが持つテンプレートイミテーションノードを集める.
   *
   * @param node このノード以下のオリジナルノードが持つテンプレートイミテーションノードを集める
   * @return {@code node} 以下のオリジナルノードが保持していたテンプレートイミテーションノードのリスト
   */
  public static Collection<Imitatable> collect(BhNode node) {
    var deleter = new TemplateImitationCollector();
    node.accept(deleter);
    return deleter.templateImitationNodes;
  }

  private TemplateImitationCollector() {}

  @Override
  public void visit(ConnectiveNode node) {
    node.getImitationList().forEach(imit -> {
      if (MsgService.INSTANCE.isTemplateNode(imit)) {
        templateImitationNodes.add(imit);
      }
    });
    node.sendToSections(this);
  }

  @Override
  public void visit(TextNode node) {
    node.getImitationList().forEach(imit -> {
      if (MsgService.INSTANCE.isTemplateNode(imit)) {
        templateImitationNodes.add(imit);
      }
    });
  }
}
