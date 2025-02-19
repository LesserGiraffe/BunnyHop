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

package net.seapanda.bunnyhop.service;

import net.seapanda.bunnyhop.model.ModelAccessNotificationService;
import net.seapanda.bunnyhop.model.ModelAccessNotificationService.Context;
import net.seapanda.bunnyhop.undo.UndoRedoAgent;
import net.seapanda.bunnyhop.undo.UserOperation;

/**
 * モデル (ノード, ワークスペース等) へアクセスする際に実行すべき処理を呼び出すクラス.
 *
 * @author K.Koike
 */
public class ModelAccessMediator implements ModelAccessNotificationService {
   
  private final ModelExclusiveControl exclusiveCtrl;
  private final DerivativeCache cache;
  private final CompileErrorReporter reporter;
  private final UndoRedoAgent undoRedoAgent;
  /** 現在使用中の {@link Context} オブジェクト. */
  private Context context;
  /** {@link #begin} が連続して呼ばれた回数. */
  private int nestingLevel = 0;

  /** コンストラクタ. */
  public ModelAccessMediator(
      ModelExclusiveControl exclusiveCtrl,
      DerivativeCache cache,
      CompileErrorReporter reporter,
      UndoRedoAgent undoRedoAgent) {
    this.exclusiveCtrl = exclusiveCtrl;
    this.cache = cache;
    this.reporter = reporter;
    this.undoRedoAgent = undoRedoAgent;
  }

  @Override
  public synchronized Context begin() {
    exclusiveCtrl.lockForModification();
    ++nestingLevel;
    if (nestingLevel != 1) {
      return context;
    }
    context = new Context(new UserOperation());
    return context;
  }
 
  @Override
  public synchronized void end() {
    if (nestingLevel == 0) {
      return;
    }
    if (nestingLevel == 1) {
      cache.clearAll();
      reporter.report(context.userOpe());
      undoRedoAgent.pushUndoCommand(context.userOpe());
      exclusiveCtrl.unlockForModification();
    }
    --nestingLevel;
  }
}
