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
import net.seapanda.bunnyhop.utility.event.ConsumerInvoker;
import net.seapanda.bunnyhop.utility.event.SimpleConsumerInvoker;

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
      context = new TransactionContext(this, new UserOperation(), new SimpleConsumerInvoker<>());
    }
    return context;
  }

  @Override
  public void end(ExclusionId... ids) {
    synchronized (this) {
      currentExclusionIds.removeAll(Set.of(ids));
      if (lock.getWriteHoldCount() == 1) {
        invokeOnTransactionEnding();
      }
    }
    endImpl();
  }

  @Override
  public void end() {
    if (lock.getWriteHoldCount() == 1) {
      invokeOnTransactionEnding();
    }
    endImpl();
  }

  private void endImpl() {
    try {
      // トランザクション終了直前のイベントハンドラで begin が呼ばれるとトランザクションが延長される.
      // それを考慮して, ここでもロック回数の検査を行う.
      if (lock.getWriteHoldCount() == 1) {
        cache.clearAll();
        reporter.check(context.userOpe());
        undoRedoAgent.pushUndoCommand(context.userOpe());
      }
    } finally {
      lock.writeLock().unlock();
    }
  }

  /** トランザクション終了直前に実行するイベントハンドラを呼び出す. */
  private void invokeOnTransactionEnding() {
    ConsumerInvoker<TransactionContext> onTransactionEnding = context.onTransactionEnding;
    context = new TransactionContext(this, context.userOpe(), new SimpleConsumerInvoker<>());
    onTransactionEnding.invoke(context);
  }

  @Override
  public Set<ExclusionId> getCurrentExclusionIds() {
    synchronized (this) {
      return new HashSet<>(currentExclusionIds);
    }
  }
}
