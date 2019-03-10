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

import java.util.Objects;

/**
 * SyntaxSymbolのID
 * @author K.Koike
 * */
public class SyntaxSymbolID {

	static private long sequentialID = 0;
	private final String id;

	static SyntaxSymbolID newID() {
		++sequentialID;
		return new SyntaxSymbolID(Long.toHexString(sequentialID));
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
		return (getClass() == obj.getClass()) && (id.equals(((BhNodeID)obj).id));
	}

	@Override
	public int hashCode() {
		int hash = 71;
		hash = 311 * hash + Objects.hashCode(this.id);
		return hash;
	}

}
