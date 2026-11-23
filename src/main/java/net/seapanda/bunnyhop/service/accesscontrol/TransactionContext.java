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
import java.util.Set;
import net.seapanda.bunnyhop.service.undo.UserOperation;
import net.seapanda.bunnyhop.utility.event.ConsumerInvoker;

/**
 * トランザクションに紐づく情報.
 *
 * @author K.Koike
 */
public class TransactionContext {

  private final UserOperation userOpe;

  final ConsumerInvoker<TransactionContext> onTransactionEnding;

  final TransactionManager txnManager;

  private final Set<ExclusiveTask> exclusiveTasks;

  /**
   * コンストラクタ.
   *
   * @param txnManager このコンテキストを管理している {@link TransactionManager} オブジェクト
   * @param userOpe undo/redo 用コマンドオブジェクト
   * @param onTransactionEnding トランザクション終了時に呼び出す関数のレジストリ
   * @param tasks このコンテキストの初期状態における排他実行すべき処理
   */
  TransactionContext(
      TransactionManager txnManager,
      UserOperation userOpe,
      ConsumerInvoker<TransactionContext> onTransactionEnding,
      Set<ExclusiveTask> tasks) {
    this.txnManager = txnManager;
    this.userOpe = userOpe;
    this.onTransactionEnding = onTransactionEnding;
    this.exclusiveTasks = new HashSet<>(tasks);
  }

  /**
   * コンストラクタ.
   *
   * @param txnManager このコンテキストを管理している {@link TransactionManager} オブジェクト
   * @param userOpe undo/redo 用コマンドオブジェクト
   * @param onTransactionEnding トランザクション終了時に呼び出す関数のレジストリ
   */
  TransactionContext(
      TransactionManager txnManager,
      UserOperation userOpe,
      ConsumerInvoker<TransactionContext> onTransactionEnding) {
    this(txnManager, userOpe, onTransactionEnding, new HashSet<>());
  }

  /** undo/redo 用コマンドオブジェクトを取得する. */
  public UserOperation userOpe() {
    return userOpe;
  }

  /** このコンテキストを管理している {@link TransactionManager} を取得する. */
  public TransactionManager manager() {
    return txnManager;
  }

  /** このコンテキストに対して指定された排他実行すべきタスクの一覧を取得する. */
  public Set<ExclusiveTask> exclusionTasks() {
    return new HashSet<>(exclusiveTasks);
  }

  /**
   * このコンテキストに対して, 排他実行すべきタスクを指定する.
   *
   * <p>本メソッドは, 同じスレッド内で排他的に行うタスクを指定するために存在する. <br>
   * 例えば, ノードの付け替えはノードの D&D と undo (どちらも UI スレッドで行われる) で発生し得るが,
   * これらを同時に行ってはならない.
   *
   * <p>{@code tasks} で指定された要素の内, 1 つでも指定済みであった場合, 待たずにコントロールを返す.
   * このとき, タスクの指定は行わない.
   * {@link TransactionManager} から {@link TransactionContext} が取得できた時点で,
   * トランザクションを実行する権利は {@link TransactionContext} を取得したスレッドにある. <br>
   * その状態で, タスクを指定できなかったという事は, 同じスレッドが既にタスクを指定しているという事である. <br>
   * よって, 待っても指定が解除されることはないので待たない.
   *
   * @param tasks 排他実行すべきタスク
   * @return ロックが取得できた場合 true.
   */
  public boolean lockTasks(ExclusiveTask... tasks) {
    var taskList = List.of(tasks);
    boolean isTaskLocked = exclusiveTasks.stream().anyMatch(taskList::contains);
    if (isTaskLocked) {
      return false;
    }
    exclusiveTasks.addAll(taskList);
    return true;
  }

  /**
   * このコンテキストに指定された排他実行すべきタスクを取り除く.
   *
   * @param tasks 取り除くタスク
   */
  public void releaseTasks(ExclusiveTask... tasks) {
    List.of(tasks).forEach(exclusiveTasks::remove);
  }

  /**
   * トランザクションが終了する直前に実行する処理のレジストリを取得する.
   *
   * <p>このレジストリに登録した処理の中で, {@link TransactionManager#begin} を呼び出した場合,
   * このコンテキストのトランザクションは, {@link TransactionManager#end} が呼び出されるまで延長される.
   */
  public ConsumerInvoker<TransactionContext>.Registry onTransactionEnding() {
    return onTransactionEnding.getRegistry();
  }
}