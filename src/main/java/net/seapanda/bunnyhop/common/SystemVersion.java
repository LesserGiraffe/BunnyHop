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

import net.seapanda.bunnyhop.utility.version.Version;

/**
  * システムのバージョン.
  *
  * @author K.Koike
  */
public class SystemVersion extends Version {

  /** システムのバージョンが存在しないことを表すオブジェクト. */
  public static final SystemVersion NONE = new SystemVersion();

  /**
   * {@link SystemVersion} オブジェクトを作成する.
   *
   * @param id 識別子名
   * @return {@link SystemVersion} オブジェクト
   */
  public static SystemVersion of(String id) {
    return new SystemVersion(id == null ? "" : id);
  }

  /**
   * コンストラクタ.
   *
   * @param version 識別子名
   */
  private SystemVersion(String version) {
    super(version);
  }

  /** デフォルトコンストラクタ. (デシリアライズ用) */
  public SystemVersion() {
    super("");
  }
}
