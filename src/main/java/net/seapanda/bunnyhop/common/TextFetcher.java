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

package net.seapanda.bunnyhop.common;

import net.seapanda.bunnyhop.utility.textdb.JsonTextDatabase;
import net.seapanda.bunnyhop.utility.textdb.TextDatabase;
import net.seapanda.bunnyhop.utility.textdb.TextId;

/**
 * {@link TextDatabase} からテキストを取得するクラス.
 * .fxml ファイルから {@link TextDatabase} のテキストを取得するのに使用することを想定している.
 *
 * @author K.Koike
 */
public class TextFetcher {

  private static volatile TextDatabase db = new JsonTextDatabase("{}");

  /** テキストの取得元となるオブジェクトを設定する. */
  public static void setTextDatabase(TextDatabase db) {
    if (db == null) {
      return;
    }
    TextFetcher.db = db;
  }

  private TextId textId = TextId.NONE;
  private String[] path = new String[] {};

  public void setTextId(String... id) {
    path = id;
    textId = TextId.of(id);
  }

  // このクラスを fxml から使用するために必要
  public String[] getTextId() {
    return path;
  }

  // このクラスを fxml から使用するために必要
  public void setText(String text) { }

  public String getText() {
    return db.get(textId);
  }
}
