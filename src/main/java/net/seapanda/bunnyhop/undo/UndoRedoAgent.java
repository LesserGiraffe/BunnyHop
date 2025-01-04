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

package net.seapanda.bunnyhop.undo;

import java.util.Deque;
import java.util.LinkedList;
import net.seapanda.bunnyhop.common.BhConstants;
import net.seapanda.bunnyhop.service.BhService;

/**
 * undo/redo 時に {@link UserOperation} クラスを操作するクラス.
 *
 * @author K.Koike
 */
public class UndoRedoAgent {

  /** Undo できるコマンドのスタック. */
  private final Deque<UserOperation> undoStack = new LinkedList<>();
  /** Redo できるコマンドのスタック. */
  private final Deque<UserOperation> redoStack = new LinkedList<>();

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
    if (BhService.appRoot() != null) {
      BhService.appRoot().getWorkspaceSet().setDirty();
    }
    redoStack.clear();
  }

  /** Undo 操作を行う. */
  public void undo() {
    if (undoStack.isEmpty()) {
      return;
    }
    UserOperation invCmd = undoStack.removeLast().doInverseOperation();
    redoStack.addLast(invCmd);
  }

  /** Redo 操作を行う. */
  public void redo() {
    if (redoStack.isEmpty()) {
      return;
    }
    UserOperation invCmd = redoStack.removeLast().doInverseOperation();
    undoStack.addLast(invCmd);
  }

  /** undo/redo 対象の操作を全て消す. */
  public void deleteCommands() {
    undoStack.clear();
    redoStack.clear();
  }
}
