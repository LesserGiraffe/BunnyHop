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

package net.seapanda.bunnyhop.node.view;

import static net.seapanda.bunnyhop.node.view.BhNodeViewBase.Shapes;

import java.util.Deque;
import java.util.LinkedList;
import javafx.application.Platform;
import javafx.event.Event;
import javafx.scene.input.MouseEvent;
import net.seapanda.bunnyhop.utility.Utility;
import net.seapanda.bunnyhop.utility.event.ConsumerInvoker;
import net.seapanda.bunnyhop.utility.event.SimpleConsumerInvoker;

/**
 * ノードビューに対してイベントハンドラを追加または削除する機能を提供するクラス.
 *
 * @author K.Koike
 */
abstract class CallbackRegistryBase implements BhNodeView.CallbackRegistry {

  private final BhNodeViewBase view;

  /** 関連するノードビュー上でマウスボタンが押下されたときのイベントハンドラを管理するオブジェクト. */
  final ConsumerInvoker<BhNodeView.MouseEventInfo> onMousePressedInvoker =
      new SimpleConsumerInvoker<>();

  /** 関連するノードビューがドラッグされたときのイベントハンドラを管理するオブジェクト. */
  final ConsumerInvoker<BhNodeView.MouseEventInfo> onMouseDraggedInvoker =
      new SimpleConsumerInvoker<>();

  /** 関連するノードビュー上でマウスのドラッグが検出されたときのイベントハンドラを管理するオブジェクト. */
  final ConsumerInvoker<BhNodeView.MouseEventInfo> onMouseDragDetectedInvoker =
      new SimpleConsumerInvoker<>();

  /** 関連するノードビュー上でマウスボタンが離されたときのイベントハンドラを管理するオブジェクト. */
  final ConsumerInvoker<BhNodeView.MouseEventInfo> onMouseReleasedInvoker =
      new SimpleConsumerInvoker<>();

  /** 関連するノードビューの位置が変わったときのイベントハンドラを管理するオブジェクト. */
  final ConsumerInvoker<BhNodeView.MoveEvent> onMovedInvoker =
      new SimpleConsumerInvoker<>();

  /** 関連するノードビューのサイズが変わったときのイベントハンドラを管理するオブジェクト. */
  final ConsumerInvoker<BhNodeView.SizeChangedEvent> onSizeChangedInvoker =
      new SimpleConsumerInvoker<>();

  /** 関連するノードビューの GUI ツリー上の親要素が変わったときのイベントハンドラを管理するオブジェクト. */
  final ConsumerInvoker<BhNodeView.ParentViewChangedEvent> onParentViewChangedInvoker =
      new SimpleConsumerInvoker<>();

  /** 関連するノードビューの親 {@link BhNodeViewGroup} が変わったときのイベントハンドラを管理するオブジェクト. */
  final ConsumerInvoker<BhNodeView.ParentGroupChangedEvent> onParentGroupChangedInvoker =
      new SimpleConsumerInvoker<>();

  /** {@link #dispatch} で送られたイベントを区別するためのフラグ. */
  private final Deque<BhNodeView.MouseEventInfo> eventStack = new LinkedList<>();

  /** コンストラクタ. */
  CallbackRegistryBase(BhNodeViewBase view) {
    this.view = view;
    Shapes shapes = view.getShapes();
    shapes.nodeShape().setOnMousePressed(event -> {
      onMousePressedInvoker.invoke(
          new BhNodeView.MouseEventInfo(view, event, eventStack.peekLast()));
      consume(event);
    });
    shapes.nodeShape().setOnMouseDragged(event -> {
      onMouseDraggedInvoker.invoke(
          new BhNodeView.MouseEventInfo(view, event, eventStack.peekLast()));
      consume(event);
    });
    shapes.nodeShape().setOnDragDetected(event -> {
      onMouseDragDetectedInvoker.invoke(
          new BhNodeView.MouseEventInfo(view, event, eventStack.peekLast()));
      consume(event);
    });
    shapes.nodeShape().setOnMouseReleased(event -> {
      onMouseReleasedInvoker.invoke(
          new BhNodeView.MouseEventInfo(view, event, eventStack.peekLast()));
      consume(event);
    });
  }

  @Override
  public ConsumerInvoker<BhNodeView.MouseEventInfo>.Registry getOnMousePressed() {
    return onMousePressedInvoker.getRegistry();
  }

  @Override
  public ConsumerInvoker<BhNodeView.MouseEventInfo>.Registry getOnMouseDragged() {
    return onMouseDraggedInvoker.getRegistry();
  }

  @Override
  public ConsumerInvoker<BhNodeView.MouseEventInfo>.Registry getOnMouseDragDetected() {
    return onMouseDragDetectedInvoker.getRegistry();
  }

  @Override
  public ConsumerInvoker<BhNodeView.MouseEventInfo>.Registry getOnMouseReleased() {
    return onMouseReleasedInvoker.getRegistry();
  }

  @Override
  public ConsumerInvoker<BhNodeView.MoveEvent>.Registry getOnMoved() {
    return onMovedInvoker.getRegistry();
  }

  @Override
  public ConsumerInvoker<BhNodeView.SizeChangedEvent>.Registry getOnSizeChanged() {
    return onSizeChangedInvoker.getRegistry();
  }

  @Override
  public ConsumerInvoker<BhNodeView.ParentViewChangedEvent>.Registry getOnParentViewChanged() {
    return onParentViewChangedInvoker.getRegistry();
  }

  @Override
  public ConsumerInvoker<BhNodeView.ParentGroupChangedEvent>.Registry getOnParentGroupChanged() {
    return onParentGroupChangedInvoker.getRegistry();
  }

  @Override
  public void dispatch(Event event) {
    view.getShapes().nodeShape().fireEvent(event);
  }

  @Override
  public void forward(BhNodeView.MouseEventInfo info) {
    if (info == null) {
      return;
    }
    eventStack.addLast(info);
    view.getShapes().nodeShape().fireEvent(info.event);
    eventStack.removeLast();
  }

  /** ノードビューの位置が変わったときのイベントハンドラを呼び出す. */
  void onMoved() {
    if (!Platform.isFxApplicationThread()) {
      throw new IllegalStateException(
          Utility.getCurrentMethodName() + " - a handler invoked in an inappropriate thread");
    }
    onMovedInvoker.invoke(new BhNodeView.MoveEvent(view));
  }

  /** ノードビューのサイズが変更されたときのイベントハンドラを呼び出す. */
  void onSizeChanged() {
    if (!Platform.isFxApplicationThread()) {
      throw new IllegalStateException(
          Utility.getCurrentMethodName() + " - a handler invoked in an inappropriate thread");
    }
    onSizeChangedInvoker.invoke(new BhNodeView.SizeChangedEvent(view));
  }

  /** {@code event} のターゲットがノード本体を描画するポリゴンであった場合 {@code event} を consume する. */
  private void consume(MouseEvent event) {
    if (event.getTarget() == view.getShapes().nodeShape()) {
      event.consume();
    }
  }
}
