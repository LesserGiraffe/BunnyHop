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

package net.seapanda.bunnyhop.search;

import java.util.Objects;
import org.apache.commons.lang3.IntegerRange;

/**
 * 部分文字列を表すクラス.
 *
 * @author K.Koike
 */
public class Substring {

  /** 部分文字列を含む文字列. */
  private final String source;
  /** 部分文字列の元の文字列における始まりの位置. */
  private final int start;
  /** 部分文字列の長さ. */
  private final int length;
  /** 部分文字列. */
  private String substring;

  /**
   * コンストラクタ.
   *
   * @param source 部分文字列を含む文字列
   * @param start 部分文字列の元の文字列における始まりの位置
   * @param length 部分文字列の長さ (>= 1)
   */
  public Substring(String source, int start, int length) {
    if (start < 0) {
      throw new IllegalArgumentException("'start' must be 0 or greater.");
    }
    if (length <= 0) {
      throw new IllegalArgumentException("'length' must be 1 or greater.");
    }
    this.source = source;
    this.start = start;
    this.length = length;
  }

  /**
   * コンストラクタ.
   *
   * @param source 部分文字列を含む文字列
   * @param start 部分文字列の元の文字列における始まりの位置
   * @param substring 部分文字列  (空文字列不可)
   */
  public Substring(String source, int start, String substring) {
    Objects.requireNonNull(source);
    Objects.requireNonNull(substring);
    if (start < 0) {
      throw new IllegalArgumentException("'start' must be 0 or greater.");
    }
    if (substring.isEmpty()) {
      throw new IllegalArgumentException("'substring' must not be empty.");
    }
    this.source = source;
    this.start = start;
    this.length = substring.length();
    this.substring = substring;
  }

  /** 部分文字列を含む文字列を返す. */
  public String getSource() {
    return source;
  }

  /** 部分文字列の元の文字列における始まりの位置を返す. */
  public int getStart() {
    return start;
  }

  /** 部分文字列の元の文字列における終わりの位置を返す. */
  public int getEnd() {
    return start + length - 1;
  }

  /** 部分文字列の長さを返す. */
  public int getLength() {
    return length;
  }

  /**
   * 部分文字列の元の文字列の中での範囲を返す.
   *
   * @return 部分文字列の元の文字列の中での範囲.
   */
  public IntegerRange getRange() {
    return IntegerRange.of(start, start + length - 1);
  }

  @Override
  public String toString() {
    if (substring == null) {
      substring = source.substring(start, start + length);
    }
    return substring;
  }
}
