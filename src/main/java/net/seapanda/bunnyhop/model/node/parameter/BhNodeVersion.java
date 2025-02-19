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

package net.seapanda.bunnyhop.model.node.parameter;

import java.io.Serializable;
import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * BhNode のバージョン.
 *
 * @author K.Koike
 */
public class BhNodeVersion implements Serializable {

  /** BhNode のバージョンが存在しないことを表すオブジェクト. */
  public static final BhNodeVersion NONE = new BhNodeVersion("");
  public final String version;
  public final String prefix;
  public final String major;
  public final String minor;

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
    Matcher matcher = Pattern.compile("([a-zA-Z0-9]+)\\-(\\d+)\\.(\\d+)").matcher(version);
    if (version == null || !matcher.find()) {
      throw new IllegalArgumentException("Invalid BhNode version format (" + version + ")");
    }
    prefix = matcher.group(1);
    major = matcher.group(2);
    minor = matcher.group(3);
  }

  /** デフォルトコンストラクタ. (デシリアライズ用) */
  public BhNodeVersion() {
    version = NONE.version;
    prefix = NONE.prefix;
    major = NONE.major;
    minor = NONE.minor;
  }

  /** 接頭語部分を比較する. */
  public boolean compPrefix(BhNodeVersion other) {
    if (other == null) {
      return false;
    }
    return other.prefix.equals(prefix);
  }

  /** メジャー番号を比較する. */
  public boolean compMajor(BhNodeVersion other) {
    if (other == null) {
      return false;
    }
    return other.major.equals(major);
  }

  /** マイナー番号を比較する. */
  public boolean compMinor(BhNodeVersion other) {
    if (other == null) {
      return false;
    }
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
