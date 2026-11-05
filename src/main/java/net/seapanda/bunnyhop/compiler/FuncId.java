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

package net.seapanda.bunnyhop.compiler;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * 関数の識別子.
 *
 * @author K.Koike
 */
class FuncId {
  
  private List<String> id;

  /**
   * 識別子を作成する.
   *
   * @param snippets ID を構成する文字列群
   */
  public static FuncId of(String... snippets) {
    var retVal = new FuncId();
    retVal.id = new ArrayList<>(Arrays.asList(snippets));
    return retVal;
  }

  /**
   * 識別子を作成する.
   *
   * @param snippet ID を構成する文字列
   * @param snippets ID を構成する文字列のリスト
   */
  public static FuncId of(String snippet, List<String> snippets) {
    var list = new ArrayList<String>();
    list.add(snippet);
    list.addAll(snippets);
    var retVal = new FuncId();
    retVal.id = list;
    return retVal;
  }

  /**
   * 識別子を作成する.
   *
   * @param snippets ID を構成する文字列のリスト
   */
  public static FuncId of(List<String> snippets) {
    var retVal = new FuncId();
    retVal.id = new ArrayList<>(snippets);
    return retVal;
  }

  private FuncId() {}

  @Override
  public boolean equals(Object obj) {
    if (obj == null) {
      return false;
    }
    return (getClass() == obj.getClass()) && (id.equals(((FuncId) obj).id));
  }

  @Override
  public int hashCode() {
    return id.hashCode();
  }
}
