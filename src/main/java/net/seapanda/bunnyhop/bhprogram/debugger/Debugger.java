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

import net.seapanda.bunnyhop.bhprogram.debugger.variable.VariableInfo;
import net.seapanda.bunnyhop.model.node.BhNode;
import net.seapanda.bunnyhop.utility.function.ConsumerInvoker;

/**
 * BhProgram のデバッガが持つ機能を規定したインタフェース.
 *
 * @author K.Koike
 */
public interface Debugger {

  /** デバッガに {@link ThreadContext} を追加する. */
  void add(ThreadContext context);

  /** デバッガに変数情報を追加する. */
  void add(VariableInfo variableInfo);

  /** デバッガが持つ全ての情報をクリアする. */
  void clear();

  /**
   * このデバッガに対するイベントハンドラの追加と削除を行うオブジェクトを返す.
   *
   * @return このデバッガに対するイベントハンドラの追加と削除を行うオブジェクト
   */
  CallbackRegistry getCallbackRegistry();

  /**
   * 「現在のスレッド」を停止可能位置で一時停止するようにする.
   *
   * @return 成功した場合 true.  失敗したか「現在のスレッド」が選択されていない場合 false.
   */
  boolean suspend();

  /**
   * 「現在のスレッド」が一時停止中であった場合, 動作を再開させる.
   *
   * @return 成功した場合 true.  失敗したか「現在のスレッド」が選択されていない場合 false.
   */
  boolean resume();

  /**
   * 「現在のスレッド」が一時停止中であった場合, 次に停止可能な位置まで処理を進める.
   *
   * <p>次の処理が関数呼び出しであった場合, その中では止まらず, 関数呼び出し終了後の次に停止可能な位置で止まる.
   *
   * @return 成功した場合 true.  失敗したか「現在のスレッド」として 1 つのスレッドが選択されていない場合 false.
   */
  boolean stepOver();

  /**
   * 「現在のスレッド」が一時停止中であった場合, 次に停止可能な位置まで処理を進める.
   *
   * <p>次の処理が関数呼び出しであった場合, その中に停止可能な位置があれば止まる.
   *
   * @return 成功した場合 true.  失敗したか「現在のスレッド」として 1 つのスレッドが選択されていない場合 false.
   */
  boolean stepInto();

  /**
   * 「現在のスレッド」が一時停止中であった場合,
   * 現在実行している関数の呼び出し元の関数の中で次に停止可能な位置まで処理を進める.
   *
   * @return 成功した場合 true.  失敗したか「現在のスレッド」として 1 つのスレッドが選択されていない場合 false.
   */
  boolean stepOut();

  /**
   * BhRuntime にスレッドコンテキストの送信を要求する.
   *
   * @return 成功した場合 true.
   */
  boolean requestThreadContexts();

  /**
   * BhRuntime に「現在のスタックフレーム」の変数の送信を要求する.
   *
   * @return 成功した場合 true.  失敗したか「現在のスタックフレーム」が選択されていない場合 false.
   */
  boolean requestLocalVars();

  /**
   * BhRuntime にグローバル変数の情報の送信を要求する.
   *
   * @return 成功した場合 true.
   */
  boolean requestGlobalVars();

  /**
   * BhRuntime に「現在のスタックフレーム」のリスト変数の値の送信を要求する.
   *
   * @param node この {@link BhNode} に対応するリストの値を取得する
   * @param startIdx 値を取得する範囲のスタートインデックス
   * @param length 値を取得する範囲の要素数
   * @return 成功した場合 true.   失敗したか「現在のスタックフレーム」が選択されていない場合 false.
   */
  boolean requestLocalListVals(BhNode node, long startIdx, long length);

  /**
   * BhRuntime にグローバルリスト変数の値の送信を要求する.
   *
   * @param node この {@link BhNode} に対応するリストの値を取得する
   * @return 成功した場合 true.
   */
  boolean requestGlobalListVals(BhNode node, long startIdx, long length);


  /**
   * デバッガの「現在のスレッド」となるスレッドを選択する.
   *
   * <p>「現在のスレッド」が変わると, 「現在のスタックフレーム」は未選択状態になる.
   *
   * @param selection このオブジェクトのスレッドを「現在のスレッド」として選択する
   */
  void selectCurrentThread(ThreadSelection selection);

  /**
   * デバッガの「現在のスレッド」を取得する.
   *
   * @return 「現在のスレッド」として選択されているスレッドの情報を格納したオブジェクト
   */
  ThreadSelection getCurrentThread();

  /**
   * デバッガの「現在のスタックフレーム」となるスタックフレームを選択する.
   *
   * @param selection スタックフレームの選択状態を表すオブジェクト
   */
  void selectCurrentStackFrame(StackFrameSelection selection);

  /** 
   * デバッガの「現在のスタックフレーム」を取得する.
   *
   * @return 「現在のスタックフレーム」として選択されているスタックフレームの情報を格納したオブジェクト
   */
  StackFrameSelection getCurrentStackFrame();

  /**
   * ブレークポイントの登録および削除を行うためのオブジェクトを取得する.
   *
   * @return ブレークポイントの登録および削除を行うためのオブジェクト
   */
  BreakpointRegistry getBreakpointRegistry();

  /** デバッガに対するイベントハンドラの登録および削除操作を規定したインタフェース. */
  interface CallbackRegistry {
    
    /** {@link ThreadContext} が追加されたときのイベントハンドラのレジストリを取得する. */
    ConsumerInvoker<ThreadContextAddedEvent>.Registry getOnThreadContextAdded();

    /** {@link VariableInfo} が追加されたときのイベントハンドラのレジストリを取得する. */
    ConsumerInvoker<VariableInfoAddedEvent>.Registry getOnVariableInfoAdded();

    /** デバッガの設定をクリアしたときのイベントハンドラのレジストリを取得する. */
    ConsumerInvoker<ClearEvent>.Registry getOnCleared();

    /** 「現在のスレッド」が変わったときのイベントハンドラのレジストリを取得する. */
    ConsumerInvoker<CurrentThreadChangedEvent>.Registry getOnCurrentThreadChanged();

    /** 「現在のスタックフレーム」が変わったときのイベントハンドラのレジストリを取得する. */
    ConsumerInvoker<CurrentStackFrameChangedEvent>.Registry getOnCurrentStackFrameChanged();
  }

  /**
   * デバッガが {@link ThreadContext} を取得したときの情報を格納したレコード.
   *
   * @param debugger {@code context} を取得したデバッガ
   * @param context {@code debugger} が取得した {@link ThreadContext}
   */
  record ThreadContextAddedEvent(Debugger debugger, ThreadContext context) {}

  /**
   * デバッガが {@link VariableInfo} を取得したときの情報を格納したレコード.
   *
   * @param debugger {@code info} を取得したデバッガ
   * @param info {@code debugger} が取得した {@link VariableInfo}
   */
  record VariableInfoAddedEvent(Debugger debugger, VariableInfo info) {}

  /**
   * デバッガの持つ情報がクリアされたときの情報を格納したレコード.
   *
   * @param debugger 保持する情報をクリアされたデバッガ
   */
  record ClearEvent(Debugger debugger) {}

  /**
   * 「現在のスレッド」が変わったときのイベント.
   *
   * @param debugger 「現在のスレッド」が変わったデバッガ
   * @param oldVal 変更前のスレッドの選択状態
   * @param newVal 変更後のスレッドの選択状態
   */
  record CurrentThreadChangedEvent(
      Debugger debugger, ThreadSelection oldVal, ThreadSelection newVal) {}

  /**
   * 「現在のスタックフレーム」が変わったときのイベント.
   *
   * @param debugger 「現在のスタックフレーム」が変わったデバッガ
   * @param currentThread {@code debugger} の「現在のスレッド」
   * @param oldVal 変更前のスタックフレームの選択状態
   * @param newVal 変更後のスタックフレームの選択状態
   */
  record CurrentStackFrameChangedEvent(
      Debugger debugger, ThreadSelection currentThread, StackFrameSelection oldVal, StackFrameSelection newVal) {}
}
