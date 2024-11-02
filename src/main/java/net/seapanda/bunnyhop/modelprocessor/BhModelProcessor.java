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

import net.seapanda.bunnyhop.model.node.BhNode;
import net.seapanda.bunnyhop.model.node.ConnectiveNode;
import net.seapanda.bunnyhop.model.node.Connector;
import net.seapanda.bunnyhop.model.node.TextNode;
import net.seapanda.bunnyhop.model.node.section.ConnectorSection;
import net.seapanda.bunnyhop.model.node.section.Subsection;

/**
 * {@link BhNode} を走査する Visitor クラスのインタフェース.
 *
 * @author K.Koike
 */
public interface BhModelProcessor {

  /** {@link ConnectiveNode} に対する処理を行う. */
  public default void visit(ConnectiveNode node) {
    node.sendToSections(this);
  }

  /** {@link TextNode} に対する処理を行う. */
  public default void visit(TextNode node) {}

  /** {@link Subsection} に対する処理を行う. */
  public default void visit(Subsection section) {
    section.sendToSubsections(this);
  }

  /** {@link ConnectorSection} に対する処理を行う. */
  public default void visit(ConnectorSection section) {
    section.sendToConnectors(this);
  }

  /** {@link Connector} に対する処理を行う. */
  public default void visit(Connector connector) {
    connector.sendToConnectedNode(this);
  }
}
