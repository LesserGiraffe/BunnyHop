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

import java.util.Optional;
import net.seapanda.bunnyhop.model.node.BhNode;
import net.seapanda.bunnyhop.undo.UserOperation;
import net.seapanda.bunnyhop.utility.function.ConsumerInvoker;

/**
 * BhProgram のコールスタックの各要素 (コールスタックアイテム) を表すクラス.
 *
 * @author K.Koike
 */
public class CallStackItem {
  
  private final int idx;
  private final long threadId;
  private final String name;
  private final BhNode node;
  private final boolean isNotCalled;

  /** このコールスタックアイテムが選択されているかどうかのフラグ. */
  private boolean isSelected = false;
  /** このコールスタックアイテムに登録されたイベントハンドラを管理するオブジェクト. */
  private final CallbackRegistry cbRegistry = new CallbackRegistry();

  /**
   * コンストラクタ.
   *
   * @param idx このコールスタックアイテムのインデックス
   * @param threadId このコールスタックアイテムに対応する関数呼び出しを行ったスレッドの ID
   * @param name このコールスタックアイテムの名前
   * @param node このコールスタックアイテムに対応するノード (nullable)
   * @param isNotCalled このコールスタックアイテムに対応する関数呼び出しをまだ行っていない場合 true
   */
  public CallStackItem(int idx, long threadId, String name, BhNode node, boolean isNotCalled) {
    this.idx = idx;
    this.threadId = threadId;
    this.name = name;
    this.node = node;
    this.isNotCalled = isNotCalled;
  }

  /**
   * コンストラクタ.
   *
   * @param idx このコールスタックアイテムのインデックス
   * @param threadId このコールスタックアイテムに対応する関数呼び出しを行ったスレッドの ID
   * @param name このコールスタックアイテムの名前
   * @param isNotCalled このコールスタックアイテムに対応する関数呼び出しをまだ行っていない場合 true
   */
  public CallStackItem(int idx, long threadId, String name, boolean isNotCalled) {
    this(idx, threadId, name, null, isNotCalled);
  }

  /** このコールスタックアイテムのインデックスを取得する. */
  public int getIdx() {
    return idx;
  }

  /** このコールスタックアイテムに対応する関数呼び出しを行ったスレッドの ID を取得する. */
  public long getThreadId() {
    return threadId;
  }

  /** このコールスタックアイテムの名前を取得する. */
  public String getName() {
    return name;
  }

  /** このコールスタックアイテムに対応する {@link BhNode} を取得する. */
  public Optional<BhNode> getNode() {
    return Optional.ofNullable(node);
  }

  /** このコールスタックアイテムに対応する関数呼び出しをまだ行っていない場合 true を返す. */
  public boolean isNotCalled() {
    return isNotCalled;
  }

  /**
   * このコールスタックアイテムに対するイベントハンドラの追加と削除を行うオブジェクトを返す.
   *
   * @return このコールスタックアイテムに対するイベントハンドラの追加と削除を行うオブジェクト
   */
  public CallbackRegistry getCallbackRegistry() {
    return cbRegistry;
  }

  /**
   * このコールスタックアイテムが選択されているかどうかを調べる.
   *
   * @return このコールスタックアイテムが選択されている場合 true を返す
   */
  public boolean isSelected() {
    return isSelected;
  }

  /** このコールスタックアイテムを選択状態にする. */
  public void select(UserOperation userOpe) {
    if (!isSelected) {
      isSelected = true;
      cbRegistry.onSelectionStateChangedInvoker.invoke(new SelectionEvent(this, true, userOpe));
      userOpe.pushCmdOfSelectCallStackItem(this);
    }
  }

  /** このコールスタックアイテムを非選択状態にする. */
  public void deselect(UserOperation userOpe) {
    if (isSelected) {
      isSelected = false;
      cbRegistry.onSelectionStateChangedInvoker.invoke(new SelectionEvent(this, false, userOpe));
      userOpe.pushCmdOfDeselectCallStackItem(this);
    }
  }

  /** {@link CallStackItem} に対してイベントハンドラを追加または削除する機能を提供するクラス. */
  public class CallbackRegistry {
    
    /** 関連する {@link CallStackItem} が選択されたときのイベントハンドラを管理するオブジェクト. */
    private final ConsumerInvoker<SelectionEvent> onSelectionStateChangedInvoker =
        new ConsumerInvoker<>();

    /** 関連する {@link CallStackItem} の選択状態が変更されたときのイベントハンドラのレジストリを取得する. */
    public ConsumerInvoker<SelectionEvent>.Registry getOnSelectionStateChanged() {
      return onSelectionStateChangedInvoker.getRegistry();
    }
  }

  /**
   * {@link CallStackItem} の選択状態が変更されたときの情報を格納したレコード.
   *
   * @param item 選択状態が変更された {@link CallStackItem}
   * @param isSelected {@code item} が選択された場合 true
   * @param userOpe undo 用コマンドオブジェクト
   */
  public record SelectionEvent(CallStackItem item, boolean isSelected, UserOperation userOpe) {}
}
