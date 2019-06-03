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
package net.seapanda.bunnyhop.viewprocessor;

import java.util.function.Consumer;

import net.seapanda.bunnyhop.view.node.BhNodeView;
import net.seapanda.bunnyhop.view.node.ComboBoxNodeView;
import net.seapanda.bunnyhop.view.node.ConnectiveNodeView;
import net.seapanda.bunnyhop.view.node.LabelNodeView;
import net.seapanda.bunnyhop.view.node.NoContentNodeView;
import net.seapanda.bunnyhop.view.node.TextAreaNodeView;
import net.seapanda.bunnyhop.view.node.TextFieldNodeView;
import net.seapanda.bunnyhop.view.node.VoidNodeView;

/**
 * 登録されたコールバック関数を呼び出す Visitor クラス
 * @author K.Koike
 * */
public class CallbackInvoker implements NodeViewProcessor {

	private final boolean visitOnlyOuter;	//!< 外部ノードのみ巡る場合 true
	private final Consumer<BhNodeView> callback;

	private CallbackInvoker(boolean visitOnlyOuter, Consumer<BhNodeView> callback) {
		this.visitOnlyOuter = visitOnlyOuter;
		this.callback = callback;
	}

	/**
	 * コールバック関数を呼び出す.
	 * @param callback 呼び出すコールバック関数
	 * @param nodeView これ以下のノードビューに対して, callback を呼び出す
	 * */
	public static void invoke(Consumer<BhNodeView> callback, BhNodeView nodeView) {
		nodeView.accept(new CallbackInvoker(false, callback));
	}

	/**
	 * 外部ノードのみを経由しつつコールバック関数を呼び出す.
	 * @param callback 呼び出すコールバック関数
	 * @param nodeView このノードから外部ノードのみを経由しながらコールバック関数を呼び出す.
	 * */
	public static void invokeForOuters(Consumer<BhNodeView> callback, BhNodeView nodeView) {
		nodeView.accept(new CallbackInvoker(true, callback));
	}

	@Override
	public void visit(ConnectiveNodeView view) {

		callback.accept(view);
		if (!visitOnlyOuter)
			view.sendToInnerGroup(this);

		view.sendToOuterGroup(this);
	}

	@Override
	public void visit(TextFieldNodeView view) {
		callback.accept(view);
	}

	@Override
	public void visit(TextAreaNodeView view) {
		callback.accept(view);
	}

	@Override
	public void visit(LabelNodeView view) {
		callback.accept(view);
	}

	@Override
	public void visit(ComboBoxNodeView view) {
		callback.accept(view);
	}

	@Override
	public void visit(NoContentNodeView view) {
		callback.accept(view);
	}

	@Override
	public void visit(VoidNodeView view) {
		callback.accept(view);
	}
}
