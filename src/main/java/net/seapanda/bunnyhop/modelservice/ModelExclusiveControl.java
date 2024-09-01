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

package net.seapanda.bunnyhop.modelservice;

import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

/**
 * モデル操作のための Lock 機構を提供するクラス.
 *
 * @author Koike
 */
public class ModelExclusiveControl {
  
  /** シングルトンインスタンス. */
  public static final ModelExclusiveControl INSTANCE = new ModelExclusiveControl();
  private ReadWriteLock lock = new ReentrantReadWriteLock();

  private ModelExclusiveControl() {}

  /** Model 修正のためのロックをかける. */
  public void lockForModification() {
    lock.writeLock().lock();
  }

  /** Model 修正のためのロックを解放する. */
  public void unlockForModification() {
    lock.writeLock().unlock();
  }

  /** Model 読み取りのためのロックをかける. */
  public void lockForRead() {
    lock.readLock().lock();
  }

  /** Model 読み取りのためのロックを解放する. */
  public void unlockForRead() {
    lock.readLock().unlock();
  }
}
