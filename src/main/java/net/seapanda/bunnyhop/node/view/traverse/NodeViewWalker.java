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

package net.seapanda.bunnyhop.node.view.traverse;

import net.seapanda.bunnyhop.node.view.BhNodeViewGroup;
import net.seapanda.bunnyhop.node.view.ComboBoxNodeView;
import net.seapanda.bunnyhop.node.view.ConnectiveNodeView;
import net.seapanda.bunnyhop.node.view.LabelNodeView;
import net.seapanda.bunnyhop.node.view.NoContentNodeView;
import net.seapanda.bunnyhop.node.view.TextAreaNodeView;
import net.seapanda.bunnyhop.node.view.TextFieldNodeView;

/**
 * {@code BhNodeView} を走査する Visitor クラスのインタフェース.
 *
 * @author K.Koike
 */
public interface NodeViewWalker {

  /** {@link BhNodeViewGroup} に対する処理を行う. */
  default void visit(BhNodeViewGroup group) {
    group.sendToChildNode(this);
    group.sendToSubGroupList(this);
  }

  /** {@link ConnectiveNodeView} に対する処理を行う. */
  default void visit(ConnectiveNodeView view) {
    view.sendToInnerGroup(this);
    view.sendToOuterGroup(this);
  }

  /** {@link TextFieldNodeView} に対する処理を行う. */
  default void visit(TextFieldNodeView view) {}

  /** {@link TextAreaNodeView} に対する処理を行う. */
  default void visit(TextAreaNodeView view) {}

  /** {@link LabelNodeView}  に対する処理を行う. */
  default void visit(LabelNodeView view) {}

  /** {@link ComboBoxNodeView} に対する処理を行う. */
  default void visit(ComboBoxNodeView view) {}

  /** {@link NoContentNodeView} に対する処理を行う. */
  default void visit(NoContentNodeView view) {}
}
