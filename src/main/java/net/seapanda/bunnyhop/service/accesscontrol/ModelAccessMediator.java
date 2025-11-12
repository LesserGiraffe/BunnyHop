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

package net.seapanda.bunnyhop.service.accesscontrol;

import java.util.concurrent.locks.ReentrantReadWriteLock;
import net.seapanda.bunnyhop.linter.model.CompileErrorChecker;
import net.seapanda.bunnyhop.node.service.DerivativeCache;
import net.seapanda.bunnyhop.service.undo.UndoRedoAgent;
import net.seapanda.bunnyhop.service.undo.UserOperation;

/**
 * モデル (ノード, ワークスペース等) へアクセスする際に実行すべき処理を呼び出すクラス.
 *
 * @author K.Koike
 */
public class ModelAccessMediator implements ModelAccessNotificationService {

  private final ReentrantReadWriteLock lock = new ReentrantReadWriteLock();
  private final DerivativeCache cache;
  private final CompileErrorChecker reporter;
  private final UndoRedoAgent undoRedoAgent;
  /** 現在使用中の {@link Context} オブジェクト. */
  private Context context;
  /** {@link #beginWrite} が連続して呼ばれた回数. */
  private int nestingLevel = 0;


  /** コンストラクタ. */
  public ModelAccessMediator(
      DerivativeCache cache,
      CompileErrorChecker reporter,
      UndoRedoAgent undoRedoAgent) {
    this.cache = cache;
    this.reporter = reporter;
    this.undoRedoAgent = undoRedoAgent;
  }

  @Override
  public Context beginWrite() {
    lock.writeLock().lock();
    ++nestingLevel;
    if (nestingLevel != 1) {
      return context;
    }
    context = new Context(new UserOperation());
    return context;
  }
 
  @Override
  public void endWrite() {
    if (nestingLevel == 0) {
      return;
    }
    if (nestingLevel == 1) {
      cache.clearAll();
      reporter.check(context.userOpe());
      undoRedoAgent.pushUndoCommand(context.userOpe());
      lock.writeLock().unlock();
    }
    --nestingLevel;
  }

  @Override
  public void beginRead() {
    lock.readLock().lock();
  }

  @Override
  public void endRead() {
    lock.readLock().unlock();
  }
}
