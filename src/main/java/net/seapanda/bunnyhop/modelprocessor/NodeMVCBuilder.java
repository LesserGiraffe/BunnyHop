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
import java.util.Optional;

import net.seapanda.bunnyhop.common.tools.MsgPrinter;
import net.seapanda.bunnyhop.control.node.BhNodeControllerInSelectionView;
import net.seapanda.bunnyhop.control.node.ComboBoxNodeController;
import net.seapanda.bunnyhop.control.node.ConnectiveNodeController;
import net.seapanda.bunnyhop.control.node.LabelNodeController;
import net.seapanda.bunnyhop.control.node.NoContentNodeController;
import net.seapanda.bunnyhop.control.node.TextInputNodeController;
import net.seapanda.bunnyhop.message.BhMsg;
import net.seapanda.bunnyhop.message.MsgData;
import net.seapanda.bunnyhop.model.node.BhNode;
import net.seapanda.bunnyhop.model.node.TextNode;
import net.seapanda.bunnyhop.model.node.connective.ConnectiveNode;
import net.seapanda.bunnyhop.model.node.connective.Connector;
import net.seapanda.bunnyhop.view.ViewInitializationException;
import net.seapanda.bunnyhop.view.node.BhNodeView;
import net.seapanda.bunnyhop.view.node.ComboBoxNodeView;
import net.seapanda.bunnyhop.view.node.ConnectiveNodeView;
import net.seapanda.bunnyhop.view.node.LabelNodeView;
import net.seapanda.bunnyhop.view.node.NoContentNodeView;
import net.seapanda.bunnyhop.view.node.TextAreaNodeView;
import net.seapanda.bunnyhop.view.node.TextFieldNodeView;
import net.seapanda.bunnyhop.view.node.part.BhNodeViewStyle;

/**
 * ノードのMVC関係を構築するクラス
 * @author K.Koike
 * */
public class NodeMVCBuilder implements BhModelProcessor {

	private BhNodeView topNodeView;	//!< MVCを構築したBhNodeツリーのトップノードのビュー
	private final Deque<ConnectiveNodeView> parentStack = new LinkedList<>();	//!< 子ノードの追加先のビュー
	private MVCConnector mvcConnector;

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
	}


	private void addChildView(BhNode node, BhNodeView view) {

		if (node.getParentConnector() != null) {
			parentStack.peekLast().addToGroup(view);
		}
	}

	/**
	 * node のビューとコントロールを作成しMVCとして結びつける
	 * @param node ビューとコントロールを結びつけるノード
	 * */
	@Override
	public void visit(ConnectiveNode node) {

		BhNodeViewStyle viewStyle = BhNodeViewStyle.getNodeViewStyleFromNodeID(node.getID());
		ConnectiveNodeView connectiveNodeView = null;
		try {
			connectiveNodeView = new ConnectiveNodeView(node, viewStyle);
		}
		catch (ViewInitializationException e) {
			MsgPrinter.INSTANCE.errMsgForDebug(getClass().getSimpleName() + "\n" + e);
			return;
		}

		mvcConnector.connect(node, connectiveNodeView);
		if (topNodeView == null)
			topNodeView = connectiveNodeView;

		parentStack.addLast(connectiveNodeView);
		node.sendToSections(this);
		parentStack.removeLast();
		addChildView(node, connectiveNodeView);
	}

	@Override
	public void visit(TextNode node) {

		BhNodeViewStyle viewStyle = BhNodeViewStyle.getNodeViewStyleFromNodeID(node.getID());
		BhNodeView nodeView = null;

		try {
			Optional<BhNodeView> nodeViewOpt = createViewForTextNode(node, viewStyle);
			if (!nodeViewOpt.isPresent())
				return;

			nodeView = nodeViewOpt.get();
		}
		catch (ViewInitializationException e) {
			MsgPrinter.INSTANCE.errMsgForDebug(getClass().getSimpleName() + "\n" + e);
			return;
		}

		if (topNodeView == null)
			topNodeView = nodeView;

		addChildView(node, nodeView);
	}

	/**
	 * 引数で指定したテキストノードに応じたノードビューを作成する.
	 * @param node このノードに対応するノードビューを作成する
	 * @param viewStyle ノードビューに適用するスタイル
	 * @return {@code node} に対応するノードビュー
	 */
	private Optional<BhNodeView> createViewForTextNode(TextNode node, BhNodeViewStyle viewStyle)
		throws ViewInitializationException {

		switch (node.getType()) {
			case TEXT_FIELD:
				var textNodeView = new TextFieldNodeView(node, viewStyle);
				mvcConnector.connect(node, textNodeView);
				return Optional.of(textNodeView);

			case COMBO_BOX:
				var comboBoxNodeView = new ComboBoxNodeView(node, viewStyle);
				mvcConnector.connect(node, comboBoxNodeView);
				return Optional.of(comboBoxNodeView);

			case LABEL:
				var labelNodeView = new LabelNodeView(node, viewStyle);
				mvcConnector.connect(node, labelNodeView);
				return Optional.of(labelNodeView);

			case TEXT_AREA:
				var textAreaNodeView = new TextAreaNodeView(node, viewStyle);
				mvcConnector.connect(node, textAreaNodeView);
				return Optional.of(textAreaNodeView);

			case NO_CONTENT:
				var noContentNodeView = new NoContentNodeView(node, viewStyle);
				mvcConnector.connect(node, noContentNodeView);
				return Optional.of(noContentNodeView);

			case NO_VIEW:
				node.setMsgProcessor((BhMsg msg, MsgData data) -> null);
				return Optional.empty();

			default:
				throw new AssertionError(NodeMVCBuilder.class.getSimpleName() + " invalid text node type " + node.getType());
		}
	}

	@Override
	public void visit(Connector connector) {
		connector.setScriptScope();
		connector.sendToConnectedNode(this);
	}

	private interface MVCConnector {

		public void connect(ConnectiveNode node, ConnectiveNodeView view);
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
			var control = new BhNodeControllerInSelectionView(node, view, rootView);
			node.setMsgProcessor(control);
		}

		@Override
		public void connect(TextNode node, TextFieldNodeView view) {
			if (rootView == null)
				rootView = view;
			var control = new BhNodeControllerInSelectionView(node, view, rootView);
			node.setMsgProcessor(control);
		}

		@Override
		public void connect(TextNode node, LabelNodeView view) {
			if (rootView == null)
				rootView = view;
			var control = new BhNodeControllerInSelectionView(node, view, rootView);
			node.setMsgProcessor(control);
		}

		@Override
		public void connect(TextNode node, ComboBoxNodeView view) {
			if (rootView == null)
				rootView = view;
			var control = new BhNodeControllerInSelectionView(node, view, rootView);
			node.setMsgProcessor(control);
		}

		@Override
		public void connect(TextNode node, TextAreaNodeView view) {
			if (rootView == null)
				rootView = view;
			var control = new BhNodeControllerInSelectionView(node, view, rootView);
			node.setMsgProcessor(control);
		}

		@Override
		public void connect(TextNode node, NoContentNodeView view) {
			if (rootView == null)
				rootView = view;
			var control = new BhNodeControllerInSelectionView(node, view, rootView);
			node.setMsgProcessor(control);
		}
	}

	public static enum ControllerType {
		Default,	//!< ワークスペース上で操作されるBhNode 用のMVCコネクタ
		Template,	//!< テンプレートリスト上にあるBhNode 用のMVCコネクタ
	}
}




















