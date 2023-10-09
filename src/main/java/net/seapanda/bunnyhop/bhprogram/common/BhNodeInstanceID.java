/**
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
package net.seapanda.bunnyhop.bhprogram.common;

import java.io.Serializable;
import java.util.Objects;

/**
 * BunnyHop と BhProgram の実行環境間でノードのインスタンスを特定するための識別子
 * */
public class BhNodeInstanceID implements Serializable {

  private final String id;
  public static final BhNodeInstanceID NONE = new BhNodeInstanceID("NONE");  //!< IDが無いことを表す null オブジェクト

  public BhNodeInstanceID(String id) {
    this.id = id;
  }

  @Override
  public String toString() {
    return id;
  }

  @Override
  public boolean equals(Object obj) {
    if (obj == null)
      return false;
    return (getClass() == obj.getClass()) && (id.equals(((BhNodeInstanceID)obj).id));
  }

  @Override
  public int hashCode() {

    int hash = 29;
    hash = 173 * hash + Objects.hashCode(this.id);
    return hash;
  }
}

