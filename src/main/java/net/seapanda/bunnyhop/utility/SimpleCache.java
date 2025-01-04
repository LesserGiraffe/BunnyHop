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

package net.seapanda.bunnyhop.utility;

/**
 * ダーティフラグと共に値を保持するクラス.
 *
 * @author K.Koike
 */
public class SimpleCache<T> {
  
  private T val;
  private boolean isDirty = false;

  /**
   * コンストラクタ.
   *
   * @param val このオブジェクトが保持する値  (キャッシュ値)
   * @param isDirty ダーティフラグ.
   *                キャッシュ値が無効な場合 true.  無効な場合 false.
   */
  public SimpleCache(T val, boolean isDirty) {
    this.val = val;
    this.isDirty = isDirty;
  }

  /**
   * コンストラクタ.
   *
   * @param val このオブジェクトが保持する値;
   */
  public SimpleCache(T val) {
    this.val = val;
    this.isDirty = true;
  }

  /**
   * ダーティフラグを返す.
   *
   * @return キャッシュ値が無効な場合 true.  無効な場合 false.
   */
  public boolean isDirty() {
    return isDirty;
  }

  /** ダーティフラグを設定する. */
  public void setDirty(boolean isDirty) {
    this.isDirty = isDirty;
  }

  /** このオブジェクトが保持する値を返す. */
  public T getVal() {
    return val;
  }

  /**
   * このオブジェクトが保持する値 (キャッシュ値を) を更新する.
   * ダーティフラグが false になる.
   *
   * @param val この値でキャッシュ値を更新する.
   */
  public void update(T val) {
    this.val = val;
    isDirty = false;
  }

  /**
   * このオブジェクトが保持する値 (キャッシュ値を) を更新する.
   * ダーティフラグは変化しない.
   *
   * @param val この値でキャッシュ値を更新する.
   */
  public void set(T val) {
    this.val = val;
  }
}
