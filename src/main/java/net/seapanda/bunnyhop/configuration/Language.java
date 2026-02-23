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

package net.seapanda.bunnyhop.configuration;

/**
 * 言語の種類を規定した列挙型.
 *
 * @author K.Koike
 */
public enum Language {
  JAPANESE("日本語", "Japanese"),
  JAPANESE_EASY("にほんご", "Japanese-easy");

  private final String name;
  private final String val;

  Language(String name, String val) {
    this.name = name;
    this.val = val;
  }

  @Override
  public String toString() {
    return name;
  }

  public String getValue() {
    return val;
  }

  /**
   * 値から対応する言語を取得する.
   *
   * @return 対応する Language、見つからない場合は null
   */
  public static Language fromValue(String val) {
    if (JAPANESE.val.equals(val)) {
      return JAPANESE;
    }
    if (JAPANESE_EASY.val.equals(val)) {
      return JAPANESE_EASY;
    }
    return null;
  }
}
