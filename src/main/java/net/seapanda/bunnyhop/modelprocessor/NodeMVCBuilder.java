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
package net.seapanda.bunnyhop.modelprocessor;

import java.util.Deque;
import java.util.LinkedList;

import net.seapanda.bunnyhop.control.node.BhNodeControllerInSelectionView;
import net.seapanda.bunnyhop.control.node.ComboBoxNodeController;
import net.seapanda.bunnyhop.control.node.ConnectiveNodeController;
import net.seapanda.bunnyhop.control.node.LabelNodeController;
import net.seapanda.bunnyhop.control.node.NoContentNodeController;
import net.seapanda.bunnyhop.control.node.TextInputNodeController;
import net.seapanda.bunnyhop.control.node.VoidNodeController;
import net.seapanda.bunnyhop.message.BhMsg;
import net.seapanda.bunnyhop.message.MsgData;
import net.seapanda.bunnyhop.model.node.BhNode;
import net.seapanda.bunnyhop.model.node.TextNode;
import net.seapanda.bunnyhop.model.node.VoidNode;
import net.seapanda.bunnyhop.model.node.connective.ConnectiveNode;
import net.seapanda.bunnyhop.model.node.connective.Connector;
import net.seapanda.bunnyhop.view.node.BhNodeView;
import net.seapanda.bunnyhop.view.node.ComboBoxNodeView;
import net.seapanda.bunnyhop.view.node.ConnectiveNodeView;
import net.seapanda.bunnyhop.view.node.LabelNodeView;
import net.seapanda.bunnyhop.view.node.NoContentNodeView;
import net.seapanda.bunnyhop.view.node.TextAreaNodeView;
import net.seapanda.bunnyhop.view.node.TextFieldNodeView;
import net.seapanda.bunnyhop.view.node.VoidNodeView;
import net.seapanda.bunnyhop.view.node.part.BhNodeViewStyle;

/**
 * ノードのMVC関係を構築するクラス
 * @author K.Koike
 * */
public class NodeMVCBuilder implements BhModelProcessor {

	private BhNodeView topNodeView;	//!< MVCを構築したBhNodeツリーのトップノードのビュー
	private final Deque<ConnectiveNodeView> parentStack = new LinkedList<>();	//!< 子ノードの追加先のビュー
	private MVCConnector mvcConnector;
	private final boolean isTemplate;

	/**
	 * 引数で指定したノード以下のノードに対し, MVC関係を構築する. (ワークスペースに追加するノード用)
	 * @return 引数で指定したノードに対応する BhNodeView.
	 * */
	public static BhNodeView build(BhNode node) {
		var builder = new NodeMVCBuilder(ControllerType.Default);
		node.accept(builder);
		return builder.topNodeView;
	}

	/**
	 * 引数で指定したノード以下のノードに対し, MVC関係を構築する. (ノード選択ビューに追加するノード用)
	 * @return 引数で指定したノードに対応する BhNodeView.
	 * */
	public static BhNodeView buildTemplate(BhNode node) {
		var builder = new NodeMVCBuilder(ControllerType.Template);
		node.accept(builder);
		return builder.topNodeView;
	}

	/**
	 * コンストラクタ
	 * @param type Controller の種類 (ワークスペースのノード用かノードセレクタ用)
	 * */
	private NodeMVCBuilder(ControllerType type) {

		if (type == ControllerType.Default) {
			mvcConnector = new DefaultConnector();
		}
		else if (type == ControllerType.Template) {
			mvcConnector = new TemplateConnector();
		}
		isTemplate = type == ControllerType.Template;
	}


	private void addChildView(BhNode node, BhNodeView view) {

		if (node.getParentConnector() != null) {
			parentStack.peekLast().addToGroup(view);
		}
		view.getAppearanceManager().updateAppearance(null);
	}

	/**
	 * node のビューとコントロールを作成しMVCとして結びつける
	 * @param node ビューとコントロールを結びつけるノード
	 * */
	@Override
	public void visit(ConnectiveNode node) {

		BhNodeViewStyle viewStyle = BhNodeViewStyle.getNodeViewStyleFromNodeID(node.getID());
		ConnectiveNodeView connectiveNodeView = new ConnectiveNodeView(node, viewStyle);
		connectiveNodeView.init();
		node.setScriptScope(connectiveNodeView);
		mvcConnector.connect(node, connectiveNodeView);
		if (topNodeView == null)
			topNodeView = connectiveNodeView;

		parentStack.addLast(connectiveNodeView);
		node.sendToSections(this);
		parentStack.removeLast();
		addChildView(node, connectiveNodeView);
	}

	/**
	 * node のビューとコントロールを作成しMVCとして結びつける
	 * @param node ビューとコントロールを結びつけるノード
	 * */
	@Override
	public void visit(VoidNode node) {

		BhNodeViewStyle viewStyle = BhNodeViewStyle.getNodeViewStyleFromNodeID(node.getID());
		VoidNodeView voidNodeView = new VoidNodeView(node, viewStyle);
		voidNodeView.init();
		if (topNodeView == null)
			topNodeView = voidNodeView;

		node.setScriptScope(voidNodeView);
		mvcConnector.connect(node, voidNodeView);
		addChildView(node, voidNodeView);
	}

	@Override
	public void visit(TextNode node) {

		BhNodeViewStyle viewStyle = BhNodeViewStyle.getNodeViewStyleFromNodeID(node.getID());
		BhNodeView nodeView = null;
		switch (node.getType()) {
			case TEXT_FIELD:
				var textNodeView = new TextFieldNodeView(node, viewStyle);
				textNodeView.init(isTemplate);
				node.setScriptScope(textNodeView);
				mvcConnector.connect(node, textNodeView);
				nodeView = textNodeView;
				break;

			case COMBO_BOX:
				var comboBoxNodeView = new ComboBoxNodeView(node, viewStyle);
				comboBoxNodeView.init(isTemplate);
				node.setScriptScope(comboBoxNodeView);
				mvcConnector.connect(node, comboBoxNodeView);
				nodeView = comboBoxNodeView;
				break;

			case LABEL:
				var labelNodeView = new LabelNodeView(node, viewStyle);
				labelNodeView.init();
				node.setScriptScope(labelNodeView);
				mvcConnector.connect(node, labelNodeView);
				nodeView = labelNodeView;
				break;

			case TEXT_AREA:
				var textAreaNodeView = new TextAreaNodeView(node, viewStyle);
				textAreaNodeView.init(isTemplate);
				node.setScriptScope(textAreaNodeView);
				mvcConnector.connect(node, textAreaNodeView);
				nodeView = textAreaNodeView;
				break;

			case NO_CONTENT:
				var noContentNodeView = new NoContentNodeView(node, viewStyle);
				noContentNodeView.init();
				node.setScriptScope(noContentNodeView);
				mvcConnector.connect(node, noContentNodeView);
				nodeView = noContentNodeView;
				break;

			case NO_VIEW:
				node.setMsgProcessor((BhMsg msg, MsgData data) -> null);
				return;

			default:
				throw new AssertionError(NodeMVCBuilder.class.getSimpleName() + " invalid text node type " + node.getType());
		}
		if (topNodeView == null)
			topNodeView = nodeView;

		addChildView(node, nodeView);
	}

	@Override
	public void visit(Connector connector) {
		connector.setScriptScope();
		connector.sendToConnectedNode(this);
	}

	private interface MVCConnector {

		public void connect(ConnectiveNode node, ConnectiveNodeView view);
		public void connect(VoidNode node, VoidNodeView view);
		public void connect(TextNode node, TextFieldNodeView view);
		public void connect(TextNode node, LabelNodeView view);
		public void connect(TextNode node, ComboBoxNodeView view);
		public void connect(TextNode node, TextAreaNodeView view);
		public void connect(TextNode node, NoContentNodeView view);
	}

	/**
	 * ワークスペースに追加されるノードのModel と View をつなぐ機能を提供するクラス
	 */
	private static class DefaultConnector implements MVCConnector {

		@Override
		public void connect(ConnectiveNode node, ConnectiveNodeView view) {
			var controller = new ConnectiveNodeController(node, view);
			node.setMsgProcessor(controller);
		}

		@Override
		public void connect(VoidNode node, VoidNodeView view) {
			var controller = new VoidNodeController(node, view);
			node.setMsgProcessor(controller);
		}

		@Override
		public void connect(TextNode node, TextFieldNodeView view) {
			var controller = new TextInputNodeController(node, view);
			node.setMsgProcessor(controller);
		}

		@Override
		public void connect(TextNode node, LabelNodeView view) {
			var controller = new LabelNodeController(node, view);
			node.setMsgProcessor(controller);
		}

		@Override
		public void connect(TextNode node, ComboBoxNodeView view) {
			var controller = new ComboBoxNodeController(node, view);
			node.setMsgProcessor(controller);
		}

		@Override
		public void connect(TextNode node, TextAreaNodeView  view) {
			var controller = new TextInputNodeController(node, view);
			node.setMsgProcessor(controller);
		}

		@Override
		public void connect(TextNode node, NoContentNodeView view) {
			var controller = new NoContentNodeController(node, view);
			node.setMsgProcessor(controller);
		}
	}

 	/**
	 * テンプレートノードリストに追加されるノードのModel と View をつなぐ機能を提供するクラス
	 */
	private static class TemplateConnector implements MVCConnector {

		BhNodeView rootView = null;	//トップノードのビュー

		@Override
		public void connect(ConnectiveNode node, ConnectiveNodeView view) {
			if (rootView == null)
				rootView = view;
			new BhNodeControllerInSelectionView(node, view, rootView);
		}

		@Override
		public void connect(VoidNode node, VoidNodeView view) {
			if (rootView == null)
				rootView = view;
			new BhNodeControllerInSelectionView(node, view, rootView);
		}

		@Override
		public void connect(TextNode node, TextFieldNodeView view) {
			if (rootView == null)
				rootView = view;
			new BhNodeControllerInSelectionView(node, view, rootView);
		}

		@Override
		public void connect(TextNode node, LabelNodeView view) {
			if (rootView == null)
				rootView = view;
			new BhNodeControllerInSelectionView(node, view, rootView);
		}

		@Override
		public void connect(TextNode node, ComboBoxNodeView view) {
			if (rootView == null)
				rootView = view;
			new BhNodeControllerInSelectionView(node, view, rootView);
		}

		@Override
		public void connect(TextNode node, TextAreaNodeView view) {
			if (rootView == null)
				rootView = view;
			new BhNodeControllerInSelectionView(node, view, rootView);
		}

		@Override
		public void connect(TextNode node, NoContentNodeView view) {
			if (rootView == null)
				rootView = view;
			new BhNodeControllerInSelectionView(node, view, rootView);
		}
	}

	public static enum ControllerType {
		Default,	//!< ワークスペース上で操作されるBhNode 用のMVCコネクタ
		Template,	//!< テンプレートリスト上にあるBhNode 用のMVCコネクタ
	}
}




















