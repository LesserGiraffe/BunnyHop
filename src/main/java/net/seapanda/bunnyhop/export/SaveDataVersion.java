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

import net.seapanda.bunnyhop.utility.version.Version;

/**
 * セーブデータのバージョン.
 *
 * @author K.Koike
 */
public class SaveDataVersion extends Version {

  /** セーブデータのバージョンが存在しないことを表すオブジェクト. */
  public static final SaveDataVersion NONE = new SaveDataVersion("");

  /**
   * {@link SaveDataVersion} オブジェクトを作成する.
   *
   * @param id 識別子名
   * @return {@link SaveDataVersion} オブジェクト
   */
  public static SaveDataVersion of(String id) {
    return new SaveDataVersion(id == null ? "" : id);
  }

  /**
   * コンストラクタ.
   *
   * @param version 識別子名
   */
  private SaveDataVersion(String version) {
    super(version);
  }

  /** デフォルトコンストラクタ. (デシリアライズ用) */
  public SaveDataVersion() {
    super("");
  }
}
