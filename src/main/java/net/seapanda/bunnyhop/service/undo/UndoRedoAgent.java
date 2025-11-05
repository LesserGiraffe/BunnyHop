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

package net.seapanda.bunnyhop.service.undo;

import java.util.Deque;
import java.util.LinkedList;
import net.seapanda.bunnyhop.common.configuration.BhConstants;
import net.seapanda.bunnyhop.utility.event.ConsumerInvoker;
import net.seapanda.bunnyhop.utility.event.SimpleConsumerInvoker;
import net.seapanda.bunnyhop.workspace.model.WorkspaceSet;

/**
 * Undo / Redo 操作を提供するクラス.
 *
 * @author K.Koike
 */
public class UndoRedoAgent {

  /** Undo できるコマンドのスタック. */
  private final Deque<UserOperation> undoStack = new LinkedList<>();
  /** Redo できるコマンドのスタック. */
  private final Deque<UserOperation> redoStack = new LinkedList<>();
  private final CallbackRegistry cbRegistry = new CallbackRegistry();
  private final WorkspaceSet wss;

  public UndoRedoAgent(WorkspaceSet wss) {
    this.wss = wss;
  }

  /**
   * Undo の対象になるコマンドを追加する.
   *
   * @param cmd Undo の対象になるコマンド
   */
  public void pushUndoCommand(UserOperation cmd) {
    if (cmd == null || cmd.getNumSubOps() <= 0) {
      return;
    }
    undoStack.addLast(cmd);
    if (undoStack.size() > BhConstants.NUM_TIMES_MAX_UNDO) {
      undoStack.removeFirst();
    }
    wss.setDirty(true);
    redoStack.clear();
    cbRegistry.onUndoStackChanged.invoke(new UndoStackChangedEvent(this));
  }

  /** Undo 操作を行う. */
  public void undo() {
    if (undoStack.isEmpty()) {
      return;
    }
    UserOperation invCmd = undoStack.removeLast().doInverseOperation();
    redoStack.addLast(invCmd);
    wss.setDirty(true);
    cbRegistry.onUndoStackChanged.invoke(new UndoStackChangedEvent(this));
  }

  /** Redo 操作を行う. */
  public void redo() {
    if (redoStack.isEmpty()) {
      return;
    }
    UserOperation invCmd = redoStack.removeLast().doInverseOperation();
    undoStack.addLast(invCmd);
    wss.setDirty(true);
    cbRegistry.onUndoStackChanged.invoke(new UndoStackChangedEvent(this));
  }

  /** Undo / Redo 対象の操作を全て消す. */
  public void deleteCommands() {
    undoStack.clear();
    redoStack.clear();
    cbRegistry.onUndoStackChanged.invoke(new UndoStackChangedEvent(this));
  }

  /**
   * この {@link UndoRedoAgent} に対するイベントハンドラの追加と削除を行うオブジェクトを返す.
   *
   * @return この {@link UndoRedoAgent} に対するイベントハンドラの追加と削除を行うオブジェクト.
   */
  public CallbackRegistry getCallbackRegistry() {
    return cbRegistry;
  }

  /**
   * Undo 可能な回数を返す.
   *
   * @return Undo 可能な回数.
   */
  public int getUndoCount() {
    return undoStack.size();
  }

  /**
   * Redo 可能な回数を返す.
   *
   * @return Redo 可能な回数.
   */
  public int getRedoCount() {
    return redoStack.size();
  }

  /** {@link UndoRedoAgent} に対してイベントハンドラを追加または削除する機能を提供するクラス. */
  public class CallbackRegistry {

    /** Undo スタックに変化があったときのイベントハンドラを管理するオブジェクト. */
    private final ConsumerInvoker<UndoStackChangedEvent> onUndoStackChanged =
        new SimpleConsumerInvoker<>();

    /** Undo スタックに変化があったときのイベントハンドラを登録 / 削除するためのオブジェクトを取得する. */
    public ConsumerInvoker<UndoStackChangedEvent>.Registry getOnUndoStackChanged() {
      return onUndoStackChanged.getRegistry();
    }
  }

  /**
   * Undo スタックに変化があったときの情報を格納したイベント.
   *
   * @param agent Undo スタックに変化があった {@link UndoRedoAgent} クラス.
   */
  public record UndoStackChangedEvent(UndoRedoAgent agent) {};
}
