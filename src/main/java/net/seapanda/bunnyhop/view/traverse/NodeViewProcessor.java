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

package net.seapanda.bunnyhop.view.traverse;

import net.seapanda.bunnyhop.view.node.BhNodeViewGroup;
import net.seapanda.bunnyhop.view.node.ComboBoxNodeView;
import net.seapanda.bunnyhop.view.node.ConnectiveNodeView;
import net.seapanda.bunnyhop.view.node.LabelNodeView;
import net.seapanda.bunnyhop.view.node.NoContentNodeView;
import net.seapanda.bunnyhop.view.node.TextAreaNodeView;
import net.seapanda.bunnyhop.view.node.TextFieldNodeView;

/**
 * {@code BhNodeView} を走査する Visitor クラスのインタフェース.
 *
 * @author K.Koike
 */
public interface NodeViewProcessor {

  /** {@link BhNodeViewGroup} に対する処理を行う. */
  public default void visit(BhNodeViewGroup group) {
    group.sendToChildNode(this);
    group.sendToSubGroupList(this);
  }

  /** {@link ConnectiveNodeView} に対する処理を行う. */
  public default void visit(ConnectiveNodeView view) {
    view.sendToInnerGroup(this);
    view.sendToOuterGroup(this);
  }

  /** {@link TextFieldNodeView} に対する処理を行う. */
  public default void visit(TextFieldNodeView view) {}

  /** {@link TextAreaNodeView} に対する処理を行う. */
  public default void visit(TextAreaNodeView view) {}

  /** {@link LabelNodeView}  に対する処理を行う. */
  public default void visit(LabelNodeView view) {}

  /** {@link ComboBoxNodeView} に対する処理を行う. */
  public default void visit(ComboBoxNodeView view) {}

  /** {@link NoContentNodeView} に対する処理を行う. */
  public default void visit(NoContentNodeView view) {}
}
