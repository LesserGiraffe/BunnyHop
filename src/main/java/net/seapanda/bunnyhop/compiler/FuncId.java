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
public class FuncId {
  
  private List<String> id;

  /**
   * 識別子を作成する.
   *
   * @param snippets IDを構成する情報
   */
  public static FuncId of(String... snippets) {

    var retVal = new FuncId();
    retVal.id = new ArrayList<String>(Arrays.asList(snippets));
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
