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


import java.util.function.Consumer;
import org.mozilla.javascript.Script;

/**
 * アプリケーション外部で定義されたスクリプトを保持するクラスのインタフェース.
 *
 * @author K.Koike
 */
public interface BhScriptRepository {

  /**
   * JavaScriptのファイルパスからコンパイル済みスクリプトを取得する.
   *
   * @param fileName 取得したいスクリプトのファイル名. (nullable)
   * @return {@code fileName} で指定した名前のスクリプト.
   *         fileName が null の場合とコンパイル済みスクリプトが見つからない場合は null.
   */
  Script getScript(String fileName);

  /**
   * 引数で指定したスクリプト名に対応するスクリプトが存在するかどうかチェックする.
   *
   * @param scriptNames スクリプトが存在するかどうか調べるスクリプト名
   * @return 引数で指定したスクリプト名に対応するスクリプトが全て見つかった場合 true
   */
  boolean allExist(String... scriptNames);

  /**
   * 引数で指定したスクリプト名に対応するスクリプトが存在するかどうかチェックする.
   *
   * @param onScriptNotFound {@code scriptNames} の中のスクリプトが見つからなかったときに呼ばれる関数オブジェクト.
   *                         関数オブジェクトの第一引数は見つからなかったスクリプトの名前.
   * @param scriptNames スクリプトが存在するかどうか調べるスクリプト名
   * @return 引数で指定したスクリプト名に対応するスクリプトが全て見つかった場合 true
   */
  boolean allExistWithHandler(Consumer<String> onScriptNotFound, String... scriptNames);

  /**
   * 引数で指定したスクリプト名に対応するスクリプトが存在するかどうかチェックする.
   * ただし, スクリプト名が null か空文字だった場合, そのスクリプトの存在は調べない.
   *
   * @param scriptNames スクリプトが存在するかどうか調べるスクリプト名
   * @return 引数で指定したスクリプト名に対応するスクリプトが全て見つかった場合 true
   */
  boolean allExistIgnoringEmpty(String... scriptNames);

  /**
   * 引数で指定したスクリプト名に対応するスクリプトが存在するかどうかチェックする.
   * ただし, スクリプト名が null か空文字だった場合, そのスクリプトの存在は調べない.
   *
   * @param onScriptNotFound {@code scriptNames} の中のスクリプトが見つからなかったときに呼ばれる関数オブジェクト.
   *                         関数オブジェクトの第一引数は見つからなかったスクリプトの名前.
   * @param scriptNames スクリプトが存在するかどうか調べるスクリプト名
   * @return 引数で指定したスクリプト名に対応するスクリプトが全て見つかった場合 true
   */
  boolean allExistIgnoringEmptyWithHandler(
      Consumer<String> onScriptNotFound, String... scriptNames);
}
