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
import java.util.LinkedHashSet;
import java.util.Set;
import net.seapanda.bunnyhop.node.model.BhNode;
import net.seapanda.bunnyhop.service.undo.UserOperation;
import net.seapanda.bunnyhop.utility.event.ConsumerInvoker;
import net.seapanda.bunnyhop.utility.event.SimpleConsumerInvoker;
import net.seapanda.bunnyhop.workspace.model.WorkspaceSet;

/**
 * ブレークポイントが指定されているノードの一覧を提供するクラス.
 *
 * @author K.Koike
 */
public class BreakpointCache {

  /** ブレークポイントが指定されているノード一覧. */
  private final Set<BhNode> nodes = new LinkedHashSet<>();
  private final CallbackRegistry cbRegistry = new CallbackRegistry();

  /** コンストラクタ. */
  public BreakpointCache(WorkspaceSet wss) {
    WorkspaceSet.CallbackRegistry cbRegistry = wss.getCallbackRegistry();
    cbRegistry.getOnNodeAdded().add(event -> updateBreakpoints(event.node(), event.userOpe()));
    cbRegistry.getOnNodeRemoved().add(event -> removeNode(event.node(), event.userOpe()));
    cbRegistry.getOnNodeBreakpointSetEvent().add(
        event -> updateBreakpoints(event.node(), event.userOpe()));
  }

  private void updateBreakpoints(BhNode node, UserOperation userOpe) {
    if (node.isBreakpointSet()) {
      addNode(node, userOpe);
    } else {
      removeNode(node, userOpe);
    }
  }

  /** {@link #nodes} に {@code node} を追加する. */
  private void addNode(BhNode node, UserOperation userOpe) {
    boolean success = nodes.add(node);
    if (success) {
      cbRegistry.onNodeAdded.invoke(new NodeAddedEvent(this, node, userOpe));
    }
  }

  /** {@link #nodes} から {@code node} を削除する. */
  private void removeNode(BhNode node, UserOperation userOpe) {
    boolean success = nodes.remove(node);
    if (success) {
      cbRegistry.onNodeRemoved.invoke(new NodeRemovedEvent(this, node, userOpe));
    }
  }

  /** このオブジェクトが保持するブレークポイントが指定されたノードを全て取得する. */
  public Collection<BhNode> getBreakpoints() {
    return new HashSet<>(nodes);
  }

  /**
   * このレジストリに対するイベントハンドラの追加と削除を行うオブジェクトを返す.
   *
   * @return このレジストリに対するイベントハンドラの追加と削除を行うオブジェクト
   */
  public CallbackRegistry getCallbackRegistry() {
    return cbRegistry;
  }

  /** {@link BreakpointCache} に対するイベントハンドラの登録および削除操作を規定したインタフェース. */
  public class CallbackRegistry {

    /** {@link BreakpointCache} にノードが追加されたときのイベントハンドラを管理するオブジェクト. */
    private final ConsumerInvoker<NodeAddedEvent> onNodeAdded = new SimpleConsumerInvoker<>();

    /** {@link BreakpointCache} からノードが削除されたときのイベントハンドラを管理するオブジェクト. */
    private final ConsumerInvoker<NodeRemovedEvent> onNodeRemoved = new SimpleConsumerInvoker<>();

    /** {@link BreakpointCache} にノードが追加されたときのイベントハンドラのレジストリを取得する. */
    public ConsumerInvoker<NodeAddedEvent>.Registry getOnNodeAdded() {
      return onNodeAdded.getRegistry();
    }

    /** {@link BreakpointCache} からノードが削除されたときのイベントハンドラのレジストリを取得する. */
    public ConsumerInvoker<NodeRemovedEvent>.Registry getOnNodeRemoved() {
      return onNodeRemoved.getRegistry();
    }
  }

  /**
   * {@link BreakpointCache} にノードが追加されたときの情報を格納したレコード.
   *
   * @param cache ノードが追加された {@link BreakpointCache}
   * @param added 追加されたノード
   * @param userOpe undo 用コマンドオブジェクト
   */
  public record NodeAddedEvent(BreakpointCache cache, BhNode added, UserOperation userOpe) {}

  /**
   * {@link BreakpointCache} からノードが削除されたときの情報を格納したレコード.
   *
   * @param cache ノードが削除された {@link BreakpointCache}
   * @param removed 削除されたノード
   * @param userOpe undo 用コマンドオブジェクト
   */
  public record NodeRemovedEvent(BreakpointCache cache, BhNode removed, UserOperation userOpe) {}
}
