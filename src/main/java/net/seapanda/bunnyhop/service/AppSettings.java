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

package net.seapanda.bunnyhop.service;

import net.seapanda.bunnyhop.bhprogram.runtime.BhRuntimeType;

/**
 * アプリケーションの現在の設定を取得する機能を提供するクラスのインタフェース.
 *
 * @author K.Koike
 */
public interface AppSettings {

  /** ブレークポイントの設定が有効かどうかを返す. */
  boolean isBreakpointSettingEnabled();

  /** 現在制御対象になっている BhRuntime の種類を返す. */
  BhRuntimeType getBhRuntimeType();
}
