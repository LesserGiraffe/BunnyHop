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

package net.seapanda.bunnyhop.export;

/**
 * セーブデータのロード中に発生する可能性のある警告一覧.
 *
 * @author K.Koike
 */
public enum ImportWarning {
  /** 定義されていない BhNode の ID を見つけた. */
  UNKNOWN_BH_NODE_ID,

  /** BhNode のバージョンが現在サポートされているバージョンと違った. */
  INCOMPATIBLE_BH_NODE_VERSION,

  /** ノードを接続するコネクタが見つからなかった. */
  CONNECTOR_NOT_FOUND,

  /**
   * オリジナルノードが保持すべき派生ノードが見つからなかった.
   * オリジナルノードと派生ノードの型が異なる場合も見つからなかったと見なす.
   */
  DERIVATIVE_NOT_FOUND,

  /** 既存の BhNode のインスタンス ID とロードした BhNode のインスタンス ID が重複した. */
  DUPLICATE_INSTANCE_ID,

  /** 破損フラグの立っているノードをロードした. */
  CORRUPTED_NODE,
}
