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

import net.seapanda.bunnyhop.common.BhConstants;

/** {@link BhNode} のタイプ. */
public enum BhNodeType {
  
  CONNECTIVE(BhConstants.BhModelDef.ATTR_VAL_CONNECTIVE),
  TEXT(BhConstants.BhModelDef.ATTR_VAL_TEXT);
  private final String typeName;

  private BhNodeType(String typeName) {
    this.typeName = typeName;
  }

  /** タイプ名から列挙子を得る. */
  public static BhNodeType toType(String typeName) {
    for (var type : BhNodeType.values()) {
      if (type.getName().equals(typeName)) {
        return type;
      }
    }
    throw new IllegalArgumentException("unknown BhNode type name " + typeName);
  }

  public String getName() {
    return typeName;
  }

  @Override
  public String toString() {
    return typeName;
  }
}
