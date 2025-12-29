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

import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import net.seapanda.bunnyhop.linter.model.CompileErrorChecker;
import net.seapanda.bunnyhop.node.model.service.DerivativeCache;
import net.seapanda.bunnyhop.service.undo.UndoRedoAgent;
import net.seapanda.bunnyhop.service.undo.UserOperation;

/**
 * {@link TransactionNotificationService} の実装クラス.
 *
 * @author K.Koike
 */
public class TransactionNotificationServiceImpl implements TransactionNotificationService {

  private final ReentrantReadWriteLock lock = new ReentrantReadWriteLock();
  private final DerivativeCache cache;
  private final CompileErrorChecker reporter;
  private final UndoRedoAgent undoRedoAgent;
  private final Set<ExclusionId> currentExclusionIds = new HashSet<>();
  /** 現在使用中の {@link TransactionContext} オブジェクト. */
  private TransactionContext context;

  /** コンストラクタ. */
  public TransactionNotificationServiceImpl(
      DerivativeCache cache,
      CompileErrorChecker reporter,
      UndoRedoAgent undoRedoAgent) {
    this.cache = cache;
    this.reporter = reporter;
    this.undoRedoAgent = undoRedoAgent;
  }

  @Override
  public Optional<TransactionContext> begin(ExclusionId... ids) {
    synchronized (this) {
      var newIds = List.of(ids);
      boolean isIdConflicted = currentExclusionIds.stream().anyMatch(newIds::contains);
      if (isIdConflicted) {
        return Optional.empty();
      }
      currentExclusionIds.addAll(newIds);
    }
    return Optional.of(begin());
  }

  @Override
  public TransactionContext begin() {
    lock.writeLock().lock();
    if (lock.getWriteHoldCount() == 1) {
      context = new TransactionContext(new UserOperation());
    }
    return context;
  }

  @Override
  public void end(ExclusionId... ids) {
    synchronized (this) {
      List.of(ids).forEach(currentExclusionIds::remove);
    }
    end();
  }

  @Override
  public void end() {
    if (lock.getWriteHoldCount() == 1) {
      cache.clearAll();
      reporter.check(context.userOpe());
      undoRedoAgent.pushUndoCommand(context.userOpe());
    }
    lock.writeLock().unlock();
  }
}
