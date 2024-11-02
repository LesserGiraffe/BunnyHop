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

package net.seapanda.bunnyhop.model.node.attribute;

import java.io.Serializable;
import java.util.Objects;
import net.seapanda.bunnyhop.common.constant.VersionInfo;

/**
 * BhNode のバージョン.
 *
 * @author K.Koike
 */
public class BhNodeVersion implements Serializable {

  private static final long serialVersionUID = VersionInfo.SERIAL_VERSION_UID;
  /** BhNode のバージョンが存在しないことを表すオブジェクト. */
  public static final BhNodeVersion NONE = new BhNodeVersion("");
  private final String version;
  private final String prefix;
  private final String major;
  private final String minor;

  /**
   * コンストラクタ.
   *
   * @param version 識別子名 (例 : bh-1.2)
   */
  private BhNodeVersion(String version) {
    this.version = version;
    if (version.isEmpty()) {
      prefix = "";
      major = "";
      minor = "";
      return;
    }
    if (version == null || !version.matches("[a-zA-Z0-9]+\\-\\d+\\.\\d+")) {
      throw new IllegalArgumentException("Invalid BhNode version format (" + version + ")");
    }
    prefix = version.substring(0, version.indexOf("-"));
    major = version.substring(version.indexOf("-") + 1, version.indexOf("."));
    minor = version.substring(version.indexOf(".") + 1, version.length());
  }

  /** 接頭語部分を比較する. */
  public boolean compPrefix(BhNodeVersion other) {
    return other.prefix.equals(prefix);
  }

  /** メジャー番号を比較する. */
  public boolean compMajor(BhNodeVersion other) {
    return other.major.equals(major);
  }

  /** マイナー番号を比較する. */
  public boolean compMinor(BhNodeVersion other) {
    return other.minor.equals(minor);
  }

  /**
   * {@link BhNodeVersion} オブジェクトを作成する.
   *
   * @param id 識別子名
   * @return BhNodeVersion オブジェクト
   */
  public static BhNodeVersion of(String id) {
    return new BhNodeVersion(id == null ? "" : id);
  }

  @Override
  public String toString() {
    return version;
  }

  @Override
  public boolean equals(Object obj) {
    if (obj instanceof BhNodeVersion other) {
      return version.equals(other.version);
    }
    return false;
  }

  @Override
  public int hashCode() {
    return Objects.hashCode(version);
  }
}
