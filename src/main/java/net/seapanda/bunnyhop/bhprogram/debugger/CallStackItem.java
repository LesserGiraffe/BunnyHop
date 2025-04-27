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

import java.util.LinkedHashSet;
import java.util.Optional;
import java.util.SequencedSet;
import net.seapanda.bunnyhop.model.node.BhNode;
import net.seapanda.bunnyhop.undo.UserOperation;
import org.apache.commons.lang3.function.TriConsumer;

/**
 * BhProgram のコールスタックの各要素 (コールスタックアイテム) を表すクラス.
 *
 * @author K.Koike
 */
public class CallStackItem {
  
  private final long id;
  private final long threadId;
  private final String name;
  private final BhNode node;

  /** このコールスタックアイテムが選択されているかどうかのフラグ. */
  private boolean isSelected = false;
  /** このコールスタックアイテムに登録されたイベントハンドラを管理するオブジェクト. */
  private EventManager eventManager = new EventManager();

  /**
   * コンストラクタ.
   *
   * @param id このコールスタックアイテムの ID
   * @param threadId このコールスタックアイテムに対応する関数呼び出しを行ったスレッドの ID
   * @param name このコールスタックアイテムの名前
   * @param node このコールスタックアイテムに対応するノード
   */
  public CallStackItem(long id, long threadId, String name, BhNode node) {
    this.id = id;
    this.threadId = threadId;
    this.name = name;
    this.node = node;
  }

  /**
   * コンストラクタ.
   *
   * @param id このコールスタックアイテムの ID
   * @param threadId このコールスタックアイテムに対応する関数呼び出しを行ったスレッドの ID
   * @param name このコールスタックアイテムの名前
   */
  public CallStackItem(long id, long threadId, String name) {
    this(id, threadId, name, null);
  }

  /** このコールスタックアイテムの ID を取得する. */
  public long getId() {
    return id;
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

  /**
   * このコールスタックアイテムに対するイベントハンドラの追加と削除を行うオブジェクトを返す.
   *
   * @return このコールスタックアイテムに対するイベントハンドラの追加と削除を行うオブジェクト
   */
  public EventManager getEventManager() {
    return eventManager;
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
      getEventManager().invokeOnSelectionStateChanged(userOpe);
      userOpe.pushCmdOfSelectCallStackItem(this);
    }
  }

  /** このコールスタックアイテムを非選択状態にする. */
  public void deselect(UserOperation userOpe) {
    if (isSelected) {
      isSelected = false;
      getEventManager().invokeOnSelectionStateChanged(userOpe);
      userOpe.pushCmdOfDeselectCallStackItem(this);
    }
  }

  /** イベントハンドラの管理を行うクラス. */
  public class EventManager {
    
    /** このノードが選択されたときに呼び出すメソッドのリスト. */
    private transient
        SequencedSet<TriConsumer<? super CallStackItem, ? super Boolean, ? super UserOperation>>
        onSelectionStateChangedList = new LinkedHashSet<>();

    /**
     * コールスタックアイテムの選択状態が変更されたときのイベントハンドラを追加する.
     *  <pre>
     *  イベントハンドラの第 1 引数: 選択状態に変更のあった {@link CallStackItem}
     *  イベントハンドラの第 2 引数: 選択状態. 選択されたなら true.
     *  イベントハンドラの第 3 引数: undo 用コマンドオブジェクト
     *  </pre>
     *
     * @param handler 追加するイベントハンドラ
     */
    public void addOnSelectionStateChanged(
        TriConsumer<? super CallStackItem, ? super Boolean, ? super UserOperation> handler) {
      onSelectionStateChangedList.addLast(handler);
    }

    /**
     * コールスタックアイテムの選択状態が変更されたときのイベントハンドラを削除する.
     *
     * @param handler 削除するイベントハンドラ
     */
    public void removeOnSelectionStateChanged(
        TriConsumer<? super CallStackItem, ? super Boolean, ? super UserOperation> handler) {
      onSelectionStateChangedList.remove(handler);
    }

    /** 選択変更時のイベントハンドラを呼び出す. */
    private void invokeOnSelectionStateChanged(UserOperation userOpe) {
      onSelectionStateChangedList.forEach(
          handler -> handler.accept(CallStackItem.this, isSelected, userOpe));
    }
  }  
}
