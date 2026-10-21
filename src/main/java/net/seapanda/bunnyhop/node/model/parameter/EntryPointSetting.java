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

package net.seapanda.bunnyhop.node.model.parameter;

import net.seapanda.bunnyhop.common.configuration.BhConstants;

/**
 * ノードがエントリポイントとして指定可能かどうかを規定した列挙型.
 *
 * @author K.Koike
 */
public enum EntryPointSetting {

  /** エントリポイントとして指定可能可能. */
  SELECTABLE(BhConstants.BhModelDef.ATTR_VAL_SELECTABLE),
  /** エントリポイントとして指定不可. */
  NONE(BhConstants.BhModelDef.ATTR_VAL_NONE),
  /** 選択の有無に関わらずエントリポイントとなる. */
  SET(BhConstants.BhModelDef.ATTR_VAL_SET);

  private final String setting;

  EntryPointSetting(String setting) {
    this.setting = setting;
  }

  /** タイプ名から列挙子を得る. */
  public static EntryPointSetting of(String setting) {
    for (var val : EntryPointSetting.values()) {
      if (val.getName().equals(setting)) {
        return val;
      }
    }
    throw new IllegalArgumentException("unknown EntryPointSetting " + setting);
  }

  public String getName() {
    return setting;
  }

  @Override
  public String toString() {
    return setting;
  }
}
