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

package net.seapanda.bunnyhop.ui.model;

/**
 * 検索処理の結果を格納するレコード.
 *
 * @param currentIdx 現在注目している検索結果のインデックス. <br>
 *                   負の数のとき, 注目している検索結果が存在しなことを示す.
 * @param numFound 検索結果の個数.
 */
public record SearchQueryResult(int currentIdx, int numFound) {}
