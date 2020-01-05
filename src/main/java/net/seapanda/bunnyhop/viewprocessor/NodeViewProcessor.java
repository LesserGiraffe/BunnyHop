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

import net.seapanda.bunnyhop.view.node.BhNodeViewGroup;
import net.seapanda.bunnyhop.view.node.ComboBoxNodeView;
import net.seapanda.bunnyhop.view.node.ConnectiveNodeView;
import net.seapanda.bunnyhop.view.node.LabelNodeView;
import net.seapanda.bunnyhop.view.node.NoContentNodeView;
import net.seapanda.bunnyhop.view.node.TextAreaNodeView;
import net.seapanda.bunnyhop.view.node.TextFieldNodeView;
import net.seapanda.bunnyhop.view.node.VoidNodeView;

/**
 * BhNode の View を巡る Visitor クラスのインタフェース
 * @author K.Koike
 * */
public interface NodeViewProcessor {

	/**
	 * BhNodeViewGroup を処理する.
	 * @param group 処理する BhNodeViewGroup
	 */
	default public void visit(BhNodeViewGroup group) {
		group.sendToChildNode(this);
		group.sendToSubGroupList(this);
	}

	/**
	 * ConnectiveNodeView を処理する.
	 * @param view 処理する ConnectiveNodeView
	 * */
	default public void visit(ConnectiveNodeView view) {
		view.sendToInnerGroup(this);
		view.sendToOuterGroup(this);
	}

	/**
	 * TextFieldNodeView を処理する
	 * @param view 処理する TextFieldNodeView
	 * */
	default public void visit(TextFieldNodeView view) {}

	/**
	 * TextAreaNodeView を処理する
	 * @param view 処理する TextAreaNodeView
	 * */
	default public void visit(TextAreaNodeView view) {}

	/**
	 * LabelNodeView を処理する
	 * @param view 処理する LabelNodeView
	 * */
	default public void visit(LabelNodeView view) {}

	/**
	 * ComboBoxNodeView を処理する
	 * @param view 処理する ComboBoxNodeView
	 * */
	default public void visit(ComboBoxNodeView view) {}

	/**
	 * NoContentNodeView を処理する
	 * @param view 処理する NoContentNodeView
	 * */
	default public void visit(NoContentNodeView view) {}

	/**
	 * VoidNodeView を処理する
	 * @param view 処理する VoidNodeView
	 * */
	default public void visit(VoidNodeView view) {}
}

























