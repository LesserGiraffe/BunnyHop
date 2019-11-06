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
import java.util.Deque;
import java.util.LinkedList;

/**
 * BhProgam 実行時に発生した例外を表すクラス
 * */
public class BhProgramException extends RuntimeException implements Serializable {

	private final BhNodeInstanceID id;
	private final Deque<BhNodeInstanceID> callStack;

	/**
	 * コンストラクタ
	 * @param id 例外を起こしたノードのID
	 * @param callStack 例外が発生した時のコールスタック
	 * @param msg 例外メッセージ
	 * */
	public BhProgramException(
		BhNodeInstanceID id, Deque<BhNodeInstanceID> callStack, String msg) {
		super(msg);
		this.id = id;
		this.callStack = new LinkedList<>(callStack);
	}

	public BhProgramException(String msg) {
		super(msg);
		this.id = BhNodeInstanceID.NONE;
		this.callStack = new LinkedList<BhNodeInstanceID>();
	}

	/**
	 * 例外が起こったノードのIDを取得する
	 * */
	public BhNodeInstanceID getBhNodeInstanceID() {
		return id;
	}

	/**
	 * 例外が発生した時のコールスタックを取得する
	 * */
	public final Deque<BhNodeInstanceID> getCallStack() {
		return new LinkedList<>(callStack);
	}
}
