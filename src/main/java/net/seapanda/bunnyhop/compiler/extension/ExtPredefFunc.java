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

package net.seapanda.bunnyhop.compiler.extension;

import java.util.ArrayList;

/**
 * 定義済み関数を表すコンパイラの拡張要素.
 *
 * @author K.Koike
 */
public class ExtPredefFunc implements Extension {

  /** 拡張関数に対応するノードのシンボル名. */
  public final String symbolName;

  /** 拡張関数の選択に使われるオプション値. */
  public final ArrayList<String> options;

  /** 拡張関数が戻り値を返すかどうか. */
  public final boolean hasRetVal;

  /** 拡張関数の名前. */
  public final String funcName;

  /** コンストラクタ. (デシリアライズ用) */
  ExtPredefFunc() {
    this.symbolName = "";
    this.options = new ArrayList<>();
    this.hasRetVal = true;
    this.funcName = "";
  }
}
