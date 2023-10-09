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
package net.seapanda.bunnyhop.view.node.part;

import net.seapanda.bunnyhop.common.constant.BhParams;

/**
 * @author K.Koike
 * GUI 要素の種類 
 */
public enum ComponentType {

  TEXT_FIELD(BhParams.NodeStyleDef.VAL_TEXT_FIELD),
  COMBO_BOX(BhParams.NodeStyleDef.VAL_COMBO_BOX),
  LABEL(BhParams.NodeStyleDef.VAL_LABEL),
  TEXT_AREA(BhParams.NodeStyleDef.VAL_TEXT_AREA),
  NONE(BhParams.NodeStyleDef.VAL_NONE);

  private final String typeName;

  private ComponentType(String typeName) {
    this.typeName = typeName;
  }

  /**
   * タイプ名から列挙子を得る
   */
  public static ComponentType toType(String name) {

    for (var type : ComponentType.values())
      if (type.getName().equals(name))
        return type;

    throw new IllegalArgumentException("Unknown component type  " + name);
  }

  public String getName() {
    return typeName;
  }

  @Override
  public String toString() {
    return typeName;
  }
}
