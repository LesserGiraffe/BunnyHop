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

package net.seapanda.bunnyhop.node.model.parameter;

import net.seapanda.bunnyhop.utility.version.Version;

/**
 * BhNode のバージョン.
 *
 * @author K.Koike
 */
public class BhNodeVersion extends Version {

  /** BhNode のバージョンが存在しないことを表すオブジェクト. */
  public static final BhNodeVersion NONE = new BhNodeVersion();

  /**
   * {@link BhNodeVersion} オブジェクトを作成する.
   *
   * @param id 識別子名
   * @return {@link BhNodeVersion} オブジェクト
   */
  public static BhNodeVersion of(String id) {
    return new BhNodeVersion(id);
  }

  /**
   * コンストラクタ.
   *
   * @param version 識別子名
   */
  private BhNodeVersion(String version) {
    super(version);
  }

  /** デフォルトコンストラクタ. (デシリアライズ用) */
  public BhNodeVersion() {
    super();
  }
}
