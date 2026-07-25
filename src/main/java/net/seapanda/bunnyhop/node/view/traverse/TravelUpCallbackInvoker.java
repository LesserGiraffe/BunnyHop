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

import java.util.function.Consumer;
import net.seapanda.bunnyhop.node.view.BhNodeView;
import net.seapanda.bunnyhop.node.view.BhNodeViewGroup;

/**
 * ノードツリーをルートのほうへ登りながらコールバック関数を呼ぶ.
 *
 * @author K.Koike
 */
public class TravelUpCallbackInvoker {

  /** ノードビューに対して呼び出すコールバック関数. */
  private final Consumer<BhNodeView> callbackForNode;
  /** 親要素の走査後にコールバック関数を呼び出すかどうか. */
  private final boolean rootFirst;

  /**
   * コールバック関数を呼び出す.
   *
   * @param callback ノードビューに対して呼び出すコールバック関数
   * @param nodeView これ以上のノードビューに対して callback を呼び出す
   */
  public static void invoke(Consumer<BhNodeView> callback, BhNodeView nodeView) {
    new TravelUpCallbackInvoker(callback, false)
        .visit(nodeView);
  }

  /**
   * コールバック関数を呼び出す.
   *
   * @param callback ノードビューに対して呼び出すコールバック関数
   * @param nodeView これ以上のノードビューに対して callback を呼び出す
   * @param rootFirst 親要素を走査してから {@code callback} を呼ぶ場合 true.
   */
  public static void invoke(
      Consumer<BhNodeView> callback, BhNodeView nodeView, boolean rootFirst) {
    new TravelUpCallbackInvoker(callback, rootFirst)
        .visit(nodeView);
  }

  private TravelUpCallbackInvoker(
      Consumer<BhNodeView> callbackForNode, boolean rootFirst) {
    this.callbackForNode = callbackForNode;
    this.rootFirst = rootFirst;
  }

  private void visit(BhNodeView view) {
    if (!rootFirst) {
      callbackForNode.accept(view);
    }
    BhNodeViewGroup parentGroup = view.getTreeControl().getParentGroup();
    if (parentGroup != null) {
      visit(parentGroup);
    }
    if (rootFirst) {
      callbackForNode.accept(view);
    }
  }

  private void visit(BhNodeViewGroup group) {
    BhNodeViewGroup parentGroup = group.getParentGroup();
    if (parentGroup != null) {
      visit(parentGroup);
    } else {
      visit(group.getParentView());
    }
  }
}
