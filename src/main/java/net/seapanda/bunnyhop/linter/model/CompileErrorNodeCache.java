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

package net.seapanda.bunnyhop.linter.model;

import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.Set;
import net.seapanda.bunnyhop.node.model.BhNode;
import net.seapanda.bunnyhop.service.undo.UserOperation;
import net.seapanda.bunnyhop.utility.event.ConsumerInvoker;
import net.seapanda.bunnyhop.utility.event.SimpleConsumerInvoker;
import net.seapanda.bunnyhop.workspace.model.WorkspaceSet;

/**
 * コンパイルエラーを起こしたノードの一覧を提供するクラス.
 *
 * @author K.Koike
 */
public class CompileErrorNodeCache {

  private final Set<BhNode> nodes = new LinkedHashSet<>();
  private final CallbackRegistry cbRegistry = new CallbackRegistry();

  /** コンストラクタ. */
  public CompileErrorNodeCache(WorkspaceSet wss) {
    WorkspaceSet.CallbackRegistry cbRegistry = wss.getCallbackRegistry();
    cbRegistry.getOnNodeAdded().add(
        event -> updateCompileErrorNodes(event.node(), event.userOpe()));
    cbRegistry.getOnNodeRemoved().add(
        event -> removeNode(event.node(), event.userOpe()));
    cbRegistry.getOnNodeCompileErrStateUpdated().add(
        event -> updateCompileErrorNodes(event.node(), event.userOpe()));
  }

  private void updateCompileErrorNodes(BhNode node, UserOperation userOpe) {
    if (node.hasCompileErr()) {
      if (nodes.contains(node)) {
        cbRegistry.onCompileErrorStateUpdated.invoke(
            new CompileErrorStateUpdatedEvent(this, node, userOpe));
      } else {
        addNode(node, userOpe);
      }
    } else {
      removeNode(node, userOpe);
    }
  }


  /** {@link #nodes} に {@code node} を追加する. */
  private void addNode(BhNode node, UserOperation userOpe) {
    if (!node.isInWorkspace()) {
      return;
    }
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


  /** このオブジェクトが保持するコンパイルエラーを持つノードを全て取得する. */
  public Set<BhNode> getCompileErrorNodes() {
    return new HashSet<>(nodes);
  }

  /** コンパイルエラーを起こしたノードが存在しない場合 true を返す. */
  public boolean isEmpty() {
    return nodes.isEmpty();
  }

  /**
   * このレジストリに対するイベントハンドラの追加と削除を行うオブジェクトを返す.
   *
   * @return このレジストリに対するイベントハンドラの追加と削除を行うオブジェクト
   */
  public CallbackRegistry getCallbackRegistry() {
    return cbRegistry;
  }

  /** {@link CompileErrorNodeCache} に対するイベントハンドラの登録および削除操作を規定したインタフェース. */
  public class CallbackRegistry {

    /** ノードのコンパイルエラー状態が更新されたときのイベントハンドラを管理するオブジェクト. */
    private final ConsumerInvoker<CompileErrorStateUpdatedEvent> onCompileErrorStateUpdated =
        new SimpleConsumerInvoker<>();

    /** {@link CompileErrorNodeCache} にノードが追加されたときのイベントハンドラを管理するオブジェクト. */
    private final ConsumerInvoker<NodeAddedEvent> onNodeAdded =
        new SimpleConsumerInvoker<>();

    /** {@link CompileErrorNodeCache} からノードが削除されたときのイベントハンドラを管理するオブジェクト. */
    private final ConsumerInvoker<NodeRemovedEvent> onNodeRemoved =
        new SimpleConsumerInvoker<>();

    /** ノードのコンパイルエラー状態が更新されたときのイベントハンドラを登録 / 削除するためのオブジェクトを取得する. */
    public ConsumerInvoker<CompileErrorStateUpdatedEvent>.Registry getOnCompileErrorStateUpdated() {
      return onCompileErrorStateUpdated.getRegistry();
    }

    /** {@link CompileErrorNodeCache} にノードが追加されたときのイベントハンドラのレジストリを取得する. */
    public ConsumerInvoker<NodeAddedEvent>.Registry getOnNodeAdded() {
      return onNodeAdded.getRegistry();
    }

    /** {@link CompileErrorNodeCache} からノードが削除されたときのイベントハンドラのレジストリを取得する. */
    public ConsumerInvoker<NodeRemovedEvent>.Registry getOnNodeRemoved() {
      return onNodeRemoved.getRegistry();
    }
  }

  /**
   * ノードのコンパイルエラー状態が更新されたときの情報を格納したレコード.
   *
   * @param cache {@code node} を保持している {@link CompileErrorNodeCache}
   * @param updated コンパイルエラーの状態が更新されたノード
   * @param userOpe undo 用コマンドオブジェクト
   */
  public record CompileErrorStateUpdatedEvent(
      CompileErrorNodeCache cache, BhNode updated, UserOperation userOpe) {}

  /**
   * {@link CompileErrorNodeCache} にノードが追加されたときの情報を格納したレコード.
   *
   * @param cache ノードが追加された {@link CompileErrorNodeCache}
   * @param added 追加されたノード
   * @param userOpe undo 用コマンドオブジェクト
   */
  public record NodeAddedEvent(CompileErrorNodeCache cache, BhNode added, UserOperation userOpe) {}

  /**
   * {@link CompileErrorNodeCache} からノードが削除されたときの情報を格納したレコード.
   *
   * @param cache ノードが削除された {@link CompileErrorNodeCache}
   * @param removed 削除されたノード
   * @param userOpe undo 用コマンドオブジェクト
   */
  public record NodeRemovedEvent(
      CompileErrorNodeCache cache, BhNode removed, UserOperation userOpe) {}
}
