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

package net.seapanda.bunnyhop.bhprogram.debugger;

import net.seapanda.bunnyhop.utility.function.ConsumerInvoker;

/**
 * BhProgram のデバッガが持つ機能を規定したインタフェース.
 *
 * @author K.Koike
 */
public interface Debugger {

  /** {@link context} の情報を出力する. */
  void output(ThreadContext context);

  /** デバッガが持つ全ての情報をクリアする. */
  void clear();

  /**
   * このデバッガに対するイベントハンドラの追加と削除を行うオブジェクトを返す.
   *
   * @return このデバッガに対するイベントハンドラの追加と削除を行うオブジェクト
   */
  CallbackRegistry getCallbackRegistry();

  /**
   * {@code threadId} で指定した BhProgram のスレッドを停止可能位置で一時停止するようにする.
   */
  void suspend(long threadId);

  /** 動作中の全ての BhProgram のスレッドを停止可能位置で一時停止するようにする. */
  void suspendAll();

  /** {@code threadId} で指定した BhProgram のスレッドが一時停止中であった場合, 動作を再開させる. */
  void resume(long threadId);

  /** 一時停止中の全ての BhProgram のスレッドの動作を再開させる. */
  void resumeAll();

  /**
   * {@code threadId} で指定した BhProgram のスレッドが一時停止中であった場合, 次に停止可能な位置まで処理を進める.
   *
   * <p>次の処理が関数呼び出しであった場合, その中では止まらず, 関数呼び出し終了後の次に停止可能な位置で止まる.
   */
  void stepOver(long threadId);

  /**
   * {@code threadId} で指定した BhProgram のスレッドが一時停止中であった場合, 次に停止可能な位置まで処理を進める.
   *
   * <p>次の処理が関数呼び出しであった場合, その中に停止可能な位置があれば止まる.
   */
  void stepInto(long threadId);

  /**
   * {@code threadId} で指定した BhProgram のスレッドが一時停止中であった場合,
   * 現在実行している関数の呼び出し元の関数の中で次に停止可能な位置まで処理を進める.
   */
  void stepOut(long threadId);


  /** デバッガに対するイベントハンドラの登録および削除操作を規定したインタフェース. */
  public interface CallbackRegistry {
    
    /** {@link ThreadContext} を取得したときのイベントハンドラのレジストリを取得する. */
    ConsumerInvoker<ThreadContextReceivedEvent>.Registry getOnThreadContextReceived();

    /** {@link ThreadContext} を取得したときのイベントハンドラのレジストリを取得する. */
    ConsumerInvoker<ClearEvent>.Registry getOnCleared();
  }

  /**
   * デバッガが {@link ThreadContext} を取得したときの情報を格納したレコード.
   *
   * @param debugger {@code context} を取得したデバッガ
   * @param context {@code debugger} が取得した {@link ThreadContext}
   */
  public record ThreadContextReceivedEvent(Debugger debugger, ThreadContext context) {}

  /**
   * デバッガの持つ情報がクリアされたときの情報を格納したレコード.
   *
   * @param debugger 保持する情報をクリアされたデバッガ
   */
  public record ClearEvent(Debugger debugger) {}
}
