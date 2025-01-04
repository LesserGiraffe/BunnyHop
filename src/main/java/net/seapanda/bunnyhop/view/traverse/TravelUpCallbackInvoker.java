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

import java.util.function.Consumer;
import net.seapanda.bunnyhop.view.node.BhNodeView;
import net.seapanda.bunnyhop.view.node.BhNodeViewGroup;

/**
 * ノードツリーをルートのほうへ登りながらコールバック関数を呼ぶ.
 *
 * @author K.Koike
 */
public class TravelUpCallbackInvoker {

  /** ノードビューに対して呼び出すコールバック関数. */
  private final Consumer<BhNodeView> callbackForNode;
  /** ノードグループに対して呼び出すコールバック関数. */
  private final Consumer<BhNodeViewGroup> callbackForGroup;
  /** 親要素の走査後にコールバック関数を呼び出すかどうか. */
  private final boolean depthFirst;

  /**
   * コールバック関数を呼び出す.
   *
   * @param callbackForNode ノードビューに対して呼び出すコールバック関数
   * @param callbackForGroup ノードビューグループ呼び出すコールバック関数
   * @param nodeView これ以上のノードビューに対して callback を呼び出す
   */
  public static void invoke(
      Consumer<BhNodeView> callbackForNode,
      Consumer<BhNodeViewGroup> callbackForGroup,
      BhNodeView nodeView) {
    new TravelUpCallbackInvoker(callbackForNode, callbackForGroup, nodeView, false)
        .visit(nodeView);
  }

  /**
   * コールバック関数を呼び出す.
   *
   * @param callbackForNode ノードビューに対して呼び出すコールバック関数
   * @param callbackForGroup ノードビューグループ呼び出すコールバック関数
   * @param nodeView これ以上のノードビューに対して callback を呼び出す
   * @param depthFirst 親要素を走査してから {@code callback} を呼ぶ場合 true.
   */
  public static void invoke(
      Consumer<BhNodeView> callbackForNode,
      Consumer<BhNodeViewGroup> callbackForGroup,
      BhNodeView nodeView,
      boolean depthFirst) {
    new TravelUpCallbackInvoker(callbackForNode, callbackForGroup, nodeView, depthFirst)
        .visit(nodeView);
  }

  /**
   * コールバック関数を呼び出す.
   *
   * @param callback ノードビューに対して呼び出すコールバック関数
   * @param nodeView これ以上のノードビューに対して callback を呼び出す
   */
  public static void invoke(
      Consumer<BhNodeView> callback, BhNodeView nodeView) {
    new TravelUpCallbackInvoker(callback, g -> {}, nodeView, false)
        .visit(nodeView);
  }

  /**
   * コールバック関数を呼び出す.
   *
   * @param callback ノードビューに対して呼び出すコールバック関数
   * @param nodeView これ以上のノードビューに対して callback を呼び出す
   * @param depthFirst 親要素を走査してから {@code callback} を呼ぶ場合 true.
   */
  public static void invoke(
      Consumer<BhNodeView> callback, BhNodeView nodeView, boolean depthFirst) {
    new TravelUpCallbackInvoker(callback, g -> {}, nodeView, depthFirst)
        .visit(nodeView);
  }

  private TravelUpCallbackInvoker(
      Consumer<BhNodeView> callbackForNode,
      Consumer<BhNodeViewGroup> callbackForGroup,
      BhNodeView nodeView,
      boolean depthFirst) {

    this.callbackForNode = callbackForNode;
    this.callbackForGroup = callbackForGroup;
    this.depthFirst = depthFirst;
  }

  private void visit(BhNodeView view) {
    if (!depthFirst) {
      callbackForNode.accept(view);
    }
    BhNodeViewGroup parentGroup = view.getTreeManager().getParentGroup();
    if (parentGroup != null) {
      visit(parentGroup);
    }
    if (depthFirst) {
      callbackForNode.accept(view);
    }
  }

  private void visit(BhNodeViewGroup group) {
    if (!depthFirst) {
      callbackForGroup.accept(group);
    }
    BhNodeViewGroup parentGroup = group.getParentGroup();
    if (parentGroup != null) {
      visit(parentGroup);
    } else {
      visit(group.getParentView());
    }
    if (depthFirst) {
      callbackForGroup.accept(group);
    }
  }
}
