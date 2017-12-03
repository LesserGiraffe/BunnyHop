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
package pflab.bunnyhop.modelprocessor;

import java.util.Deque;
import java.util.LinkedList;
import pflab.bunnyhop.common.BhParams;
import pflab.bunnyhop.control.BhNodeControllerInSelectionView;
import pflab.bunnyhop.control.ComboBoxNodeController;

import pflab.bunnyhop.control.ConnectiveNodeController;
import pflab.bunnyhop.control.LabelNodeController;
import pflab.bunnyhop.control.TextFieldNodeController;
import pflab.bunnyhop.control.VoidNodeController;
import pflab.bunnyhop.message.BhMsg;
import pflab.bunnyhop.message.MsgData;
import pflab.bunnyhop.message.MsgTransporter;
import pflab.bunnyhop.model.BhNode;
import pflab.bunnyhop.model.TextNode;
import pflab.bunnyhop.model.VoidNode;
import pflab.bunnyhop.model.connective.ConnectiveNode;
import pflab.bunnyhop.model.connective.Connector;
import pflab.bunnyhop.view.BhNodeView;
import pflab.bunnyhop.view.BhNodeViewStyle;
import pflab.bunnyhop.view.ConnectiveNodeView;
import pflab.bunnyhop.view.TextFieldNodeView;
import pflab.bunnyhop.view.VoidNodeView;
import pflab.bunnyhop.view.ComboBoxNodeView;
import pflab.bunnyhop.view.LabelNodeView;

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
	 * コンストラクタ
	 * @param type Controller の種類 (ワークスペースのノード用かノードセレクタ用)
	 * */
	public NodeMVCBuilder(ControllerType type) {

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
		view.getAppearanceManager().updateStyle(null);
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
		node.introduceSectionsTo(this);
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
		switch (node.type) {
			case BhParams.BhModelDef.ATTR_NAME_TEXT_FIELD:
				TextFieldNodeView textNodeView = new TextFieldNodeView(node, viewStyle);
				textNodeView.init(isTemplate);
				node.setScriptScope(textNodeView);
				mvcConnector.connect(node, textNodeView);
				nodeView = textNodeView;
				break;
			case BhParams.BhModelDef.ATTR_NAME_COMBO_BOX:
				ComboBoxNodeView comboBoxNodeView = new ComboBoxNodeView(node, viewStyle);
				comboBoxNodeView.init(isTemplate);
				node.setScriptScope(comboBoxNodeView);
				mvcConnector.connect(node, comboBoxNodeView);
				nodeView = comboBoxNodeView;
				break;
			case BhParams.BhModelDef.ATTR_NAME_LABEL:
				LabelNodeView labelNodeView = new LabelNodeView(node, viewStyle);
				labelNodeView.init();
				node.setScriptScope(labelNodeView);
				mvcConnector.connect(node, labelNodeView);
				nodeView = labelNodeView;
				break;
			default:
				break;
		}
		if (topNodeView == null)
			topNodeView = nodeView;
		
		if(node.getOriginalNode() != null)
			MsgTransporter.instance.sendMessage(BhMsg.IMITATE_TEXT, new MsgData(node.getOriginalNode().getText()), node);
		
		addChildView(node, nodeView);
	}
	
	@Override
	public void visit(Connector connector) {
		connector.setScriptScope();
		connector.introduceConnectedNodeTo(this);
	}

	/**
	 * 構築したMVCのトップノードのBhNodeView を返す
	 * @return 構築したMVCのトップノードのBhNodeView
	 * */
	public BhNodeView getTopNodeView() {
		return topNodeView;
	}

	private interface MVCConnector {

		public void connect(ConnectiveNode node, ConnectiveNodeView view);
		public void connect(VoidNode node, VoidNodeView view);
		public void connect(TextNode node, TextFieldNodeView view);
		public void connect(TextNode node, LabelNodeView view);
		public void connect(TextNode node, ComboBoxNodeView view);
	}

	/**
	 * ワークスペースに追加されるノードのModel と View をつなぐ機能を提供するクラス
	 */
	private static class DefaultConnector implements MVCConnector {
		
		@Override
		public void connect(ConnectiveNode node, ConnectiveNodeView view) {
			ConnectiveNodeController controller = new ConnectiveNodeController(node, view);
			node.setMsgProcessor(controller);
		}

		@Override
		public void connect(VoidNode node, VoidNodeView view) {
			VoidNodeController controller = new VoidNodeController(node, view);
			node.setMsgProcessor(controller);
		}

		@Override
		public void connect(TextNode node, TextFieldNodeView view) {
			TextFieldNodeController controller = new TextFieldNodeController(node, view);
			node.setMsgProcessor(controller);
		}

		@Override
		public void connect(TextNode node, LabelNodeView view) {
			LabelNodeController controller = new LabelNodeController(node, view);
			node.setMsgProcessor(controller);
		}
		
		@Override
		public void connect(TextNode node, ComboBoxNodeView view) {
			ComboBoxNodeController controller = new ComboBoxNodeController(node, view);
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
	}

	public static enum ControllerType {
		Default,	//!< ワークスペース上で操作されるBhNode 用のMVCコネクタ
		Template,	//!< テンプレートリスト上にあるBhNode 用のMVCコネクタ
	}
}




















