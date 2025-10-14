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

package net.seapanda.bunnyhop.debugger.model.breakpoint;

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;
import net.seapanda.bunnyhop.node.model.BhNode;
import net.seapanda.bunnyhop.service.undo.UserOperation;
import net.seapanda.bunnyhop.utility.event.ConsumerInvoker;
import net.seapanda.bunnyhop.utility.event.SimpleConsumerInvoker;

/**
 * ブレークポイントが指定されているノード一覧を提供するクラス.
 *
 * @author K.Koike
 */
public class BreakpointRegistry {

  /** ブレークポイントが指定されているノード一覧. */
  private final Set<BhNode> breakpointNodes = new HashSet<>();
  private final CallbackRegistry cbRegistry = new CallbackRegistry();

  /**
   * このレジストリにブレークポイントとなるノードを登録する.
   *
   * <p>{@code node} が登録済みの場合何もしない.
   *
   * @param node 登録する {@link BhNode}
   * @param userOpe undo 用コマンドオブジェクト
   */
  public void addBreakpointNode(BhNode node, UserOperation userOpe) {
    boolean success = breakpointNodes.add(node);
    if (success) {
      userOpe.pushCmd(ope -> removeBreakpointNode(node, ope));
      cbRegistry.onBreakpointAdded.invoke(new BreakpointAddedEvent(this, node, userOpe));
    }
  }

  /**
   * このレジストリからブレークポイントとなるノードを削除する.
   *
   * <p>{@code node} が削除図にも場合何もしない.
   *
   * @param node 削除する {@link BhNode}
   * @param userOpe undo 用コマンドオブジェクト
   */
  public void removeBreakpointNode(BhNode node, UserOperation userOpe) {
    boolean success = breakpointNodes.remove(node);
    if (success) {
      userOpe.pushCmd(ope -> addBreakpointNode(node, ope));
      cbRegistry.onBreakpointRemoved.invoke(new BreakpointRemovedEvent(this, node, userOpe));
    }
  }

  /** このレジストリに登録されたブレークポイントとなるノードを全て取得する. */
  public Collection<BhNode> getBreakpointNodes() {
    return new HashSet<>(breakpointNodes);
  }

  /**
   * このレジストリに対するイベントハンドラの追加と削除を行うオブジェクトを返す.
   *
   * @return このレジストリに対するイベントハンドラの追加と削除を行うオブジェクト
   */
  public CallbackRegistry getCallbackRegistry() {
    return cbRegistry;
  }

  /** {@link BreakpointRegistry} も対するイベントハンドラの登録および削除操作を規定したインタフェース. */
  public class CallbackRegistry {

    /** {@link BreakpointRegistry} にブレークポイントとなるノードが追加されたときのイベントハンドラを管理するオブジェクト. */
    private final ConsumerInvoker<BreakpointAddedEvent> onBreakpointAdded =
        new SimpleConsumerInvoker<>();

    /** {@link BreakpointRegistry} からブレークポイントとなるノードが削除されたときのイベントハンドラを管理するオブジェクト. */
    private final ConsumerInvoker<BreakpointRemovedEvent> onBreakpointRemoved =
        new SimpleConsumerInvoker<>();

    /** {@link BreakpointRegistry} にブレークポイントとなるノードが追加されたときのイベントハンドラのレジストリを取得する. */
    public ConsumerInvoker<BreakpointAddedEvent>.Registry getOnBreakpointAdded() {
      return onBreakpointAdded.getRegistry();
    }

    /** {@link BreakpointRegistry} からブレークポイントとなるノードが削除されたときのイベントハンドラのレジストリを取得する. */
    public ConsumerInvoker<BreakpointRemovedEvent>.Registry getOnBreakpointRemoved() {
      return onBreakpointRemoved.getRegistry();
    }
  }

  /**
   * {@link BreakpointRegistry} にブレークポイントとなるノードが追加されたときの情報を格納したレコード.
   *
   * @param registry ブレークポイントとなるノードが追加されたレジストリ
   * @param breakpoint ブレークポイントとして追加されたノード
   * @param userOpe undo 用コマンドオブジェクト
   */
  public record BreakpointAddedEvent(
      BreakpointRegistry registry, BhNode breakpoint, UserOperation userOpe) {}

  /**
   * {@link BreakpointRegistry} からブレークポイントとなるノードが削除されたときの情報を格納したレコード.
   *
   * @param registry ブレークポイントとなるノードが削除されたレジストリ
   * @param breakpoint {@code registry} から削除されたノード
   * @param userOpe undo 用コマンドオブジェクト
   */
  public record BreakpointRemovedEvent(
      BreakpointRegistry registry, BhNode breakpoint, UserOperation userOpe) {}
}
