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
import net.seapanda.bunnyhop.node.view.ComboBoxNodeView;
import net.seapanda.bunnyhop.node.view.ConnectiveNodeView;
import net.seapanda.bunnyhop.node.view.LabelNodeView;
import net.seapanda.bunnyhop.node.view.NoContentNodeView;
import net.seapanda.bunnyhop.node.view.TextAreaNodeView;
import net.seapanda.bunnyhop.node.view.TextFieldNodeView;

/**
 * 登録されたコールバック関数を呼び出す Visitor クラス.
 *
 * @author K.Koike
 */
public class CallbackInvoker implements NodeViewWalker {

  private final Consumer<? super BhNodeView> callback;
  private final Consumer<? super BhNodeViewGroup> callbackForGroup;
  /** 外部ノードのみ巡る場合 true. */
  private final boolean visitOnlyOuter;
  /** 子要素を走査してからコールバック関数を呼ぶ場合 true. */
  private final boolean depthFirst;
  /** グループのみを巡る場合 true. */
  private final boolean visitOnlyGroup;

  /** コンストラクタ. */
  private CallbackInvoker(
      Consumer<? super BhNodeView> callback,
      Consumer<? super BhNodeViewGroup> callbackForGroup,
      boolean visitOnlyOuter,
      boolean visitOnlyGroup,
      boolean depthFirst) {
    this.callback = callback;
    this.callbackForGroup = callbackForGroup;
    this.visitOnlyOuter = visitOnlyOuter;
    this.visitOnlyGroup = visitOnlyGroup;
    this.depthFirst = depthFirst;
  }

  /**
   * コールバック関数を呼び出す.
   *
   * @param callback 呼び出すコールバック関数
   * @param nodeView これ以下のノードビューに対して, callback を呼び出す
   */
  public static void invoke(Consumer<? super BhNodeView> callback, BhNodeView nodeView) {
    nodeView.accept(new CallbackInvoker(callback, g -> {}, false, false, false));
  }

  /**
   * コールバック関数を呼び出す.
   *
   * @param callback 呼び出すコールバック関数
   * @param nodeView これ以下のノードビューに対して, callback を呼び出す
   * @param depthFirst 子要素を走査してから {@code callback} を呼ぶ場合 true.
   */
  public static void invoke(
      Consumer<? super BhNodeView> callback,
      BhNodeView nodeView,
      boolean depthFirst) {
    nodeView.accept(new CallbackInvoker(callback, g -> {}, false, false, depthFirst));
  }

  /**
   * コールバック関数を呼び出す.
   *
   * @param callbackForNode ノードビューに対して呼び出すコールバック関数
   * @param callbackForGroup ノードビューグループ呼び出すコールバック関数
   * @param nodeView これ以下のノードビューに対して, callback を呼び出す
   */
  public static void invoke(
      Consumer<? super BhNodeView> callbackForNode,
      Consumer<? super BhNodeViewGroup> callbackForGroup,
      BhNodeView nodeView) {
    nodeView.accept(
        new CallbackInvoker(callbackForNode, callbackForGroup, false, false, false));
  }

  /**
   * コールバック関数を呼び出す.
   *
   * @param callbackForNode ノードビューに対して呼び出すコールバック関数
   * @param callbackForGroup ノードビューグループ呼び出すコールバック関数
   * @param nodeView これ以下のノードビューに対して, callback を呼び出す
   * @param depthFirst 子要素を走査してから {@code callback} を呼ぶ場合 true.
   */
  public static void invoke(
      Consumer<? super BhNodeView> callbackForNode,
      Consumer<? super BhNodeViewGroup> callbackForGroup,
      BhNodeView nodeView,
      boolean depthFirst) {
    nodeView.accept(
        new CallbackInvoker(callbackForNode, callbackForGroup, false, false, depthFirst));
  }

  /**
   * 外部ノードのみを経由しつつコールバック関数を呼び出す.
   *
   * @param callback 呼び出すコールバック関数
   * @param nodeView このノードから外部ノードのみを経由しながらコールバック関数を呼び出す.
   */
  public static void invokeForOuters(
      Consumer<? super BhNodeView> callback, BhNodeView nodeView) {
    nodeView.accept(new CallbackInvoker(callback, g -> {}, true, false, false));
  }

  /**
   * {@code nodeView} の子 {@link BhNodeViewGroup} を対象としてコールバック関数を呼び出す.
   *
   * @param callback 呼び出すコールバック関数
   * @param nodeView このノードから他のノードを辿らずに辿れる {@link BhNodeViewGroup} を経由しながら
   *                 コールバック関数を呼び出す.
   */
  public static void invokeForGroups(
      Consumer<? super BhNodeViewGroup> callback, BhNodeView nodeView) {
    nodeView.accept(new CallbackInvoker(n -> {}, callback, false, true, false));
  }

  @Override
  public void visit(BhNodeViewGroup group) {
    if (!depthFirst) {
      callbackForGroup.accept(group);
    }
    if (!visitOnlyGroup) {
      group.sendToChildNode(this);
    }
    group.sendToSubGroupList(this);
    if (depthFirst) {
      callbackForGroup.accept(group);
    }
  }

  @Override
  public void visit(ConnectiveNodeView view) {
    if (!depthFirst && !visitOnlyGroup) {
      callback.accept(view);
    }
    if (!visitOnlyOuter) {
      view.sendToInnerGroup(this);
    }
    view.sendToOuterGroup(this);
    if (depthFirst && !visitOnlyGroup) {
      callback.accept(view);
    }
  }

  @Override
  public void visit(TextFieldNodeView view) {
    callback.accept(view);
  }

  @Override
  public void visit(TextAreaNodeView view) {
    callback.accept(view);
  }

  @Override
  public void visit(LabelNodeView view) {
    callback.accept(view);
  }

  @Override
  public void visit(ComboBoxNodeView view) {
    callback.accept(view);
  }

  @Override
  public void visit(NoContentNodeView view) {
    callback.accept(view);
  }
}
