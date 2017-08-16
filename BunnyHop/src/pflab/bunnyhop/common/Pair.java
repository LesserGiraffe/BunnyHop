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
package pflab.bunnyhop.common;

/**
 * タプル
 * @author K.Koike
 * */
public class Pair<T1, T2> {

	public Pair(T1 _1, T2 _2) {
		this._1 = _1;
		this._2 = _2;
	}

	public Pair(){}

	public T1 _1;
	public T2 _2;

	@Override
	public String toString() {
		return "1:" + _1.toString() + "  " + "2:" + _2.toString();
	}
}
