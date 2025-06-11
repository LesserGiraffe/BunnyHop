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

/**
 * ブレークポイントが指定されたときの設定.
 *
 * @author K.Koike
 */
public enum BreakpointSetting {
  /** ブレークポイントをセットする. */
  SET(BhConstants.BhModelDef.ATTR_VAL_SET),
  /** ブレークポイントをセットしない. */
  IGNORE(BhConstants.BhModelDef.ATTR_VAL_IGNORE),
  /** 親ノードを指定する. */
  SPECIFY_PARENT(BhConstants.BhModelDef.ATTR_VAL_SPECIFY_PARENT);

  private final String setting;

  private BreakpointSetting(String setting) {
    this.setting = setting;
  }

  /** タイプ名から列挙子を得る. */
  public static BreakpointSetting of(String setting) {
    for (var val : BreakpointSetting.values()) {
      if (val.getName().equals(setting)) {
        return val;
      }
    }
    throw new IllegalArgumentException("unknown BreakpointSetting " + setting);
  }

  public String getName() {
    return setting;
  }

  @Override
  public String toString() {
    return setting;
  }
}
