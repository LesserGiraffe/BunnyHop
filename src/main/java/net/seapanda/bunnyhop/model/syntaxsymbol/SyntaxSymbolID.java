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

package net.seapanda.bunnyhop.model.syntaxsymbol;

import java.io.Serializable;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicLong;

import net.seapanda.bunnyhop.common.constant.VersionInfo;

/**
 * SyntaxSymbolのID
 * @author K.Koike
 * */
public class SyntaxSymbolID implements Serializable {

  private static final long serialVersionUID = VersionInfo.SERIAL_VERSION_UID;
  static private AtomicLong sequentialID = new AtomicLong(0);
  private final String id;

  static SyntaxSymbolID newID() {
    long id = sequentialID.addAndGet(1);
    return new SyntaxSymbolID(Long.toHexString(id));
  }

  private SyntaxSymbolID(String id) {
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
    return (getClass() == obj.getClass()) && (id.equals(((SyntaxSymbolID)obj).id));
  }

  @Override
  public int hashCode() {
    int hash = 71;
    hash = 311 * hash + Objects.hashCode(this.id);
    return hash;
  }
}
