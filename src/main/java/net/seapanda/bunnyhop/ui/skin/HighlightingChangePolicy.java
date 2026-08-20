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

package net.seapanda.bunnyhop.ui.skin;

/**
 * UI の文字列が変更されたときに, 強調表示をどう扱うかを規定した列挙型.
 *
 * @author K.Koike
 */
public enum HighlightingChangePolicy {
  /** 新しい文字列に合わせて強調表示を再計算する. */
  REFRESH,
  /** 強調表示を無効にする. */
  DISABLE,
  /** 古い文字列の強調部分をそのまま表示する. */
  KEEP
}
