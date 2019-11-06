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
package net.seapanda.bunnyhop.model.node;

import net.seapanda.bunnyhop.common.constant.BhParams;

/**
 * @author K.Koike
 * BhNode に関連する BhNodeView の種類
 */
public enum BhNodeViewType {

	CONNECTIVE(BhParams.BhModelDef.ATTR_VALUE_CONNECTIVE),
	TEXT_FIELD(BhParams.BhModelDef.ATTR_VALUE_TEXT_FIELD),
	COMBO_BOX(BhParams.BhModelDef.ATTR_VALUE_COMBO_BOX),
	LABEL(BhParams.BhModelDef.ATTR_VALUE_LABEL),
	TEXT_AREA(BhParams.BhModelDef.ATTR_VALUE_TEXT_AREA),
	NO_VIEW(BhParams.BhModelDef.ATTR_VALUE_NO_VIEW),
	NO_CONTENT(BhParams.BhModelDef.ATTR_VALUE_NO_CONTENT),
	VOID(BhParams.BhModelDef.ATTR_VALUE_VOID);

	private final String typeName;

	private BhNodeViewType(String typeName) {
		this.typeName = typeName;
	}

	/**
	 * タイプ名から列挙子を得る
	 */
	public static BhNodeViewType toType(String typeName) {

		for (var type : BhNodeViewType.values())
			if (type.getName().equals(typeName))
				return type;

		throw new IllegalArgumentException(
			BhNodeViewType.class.getSimpleName() + "  unknown BhNode type name  " + typeName);
	}

	public String getName() {
		return typeName;
	}
}
