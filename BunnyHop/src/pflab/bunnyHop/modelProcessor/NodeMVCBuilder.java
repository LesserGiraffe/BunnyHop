package pflab.bunnyHop.modelProcessor;

import java.util.Deque;
import java.util.LinkedList;
import pflab.bunnyHop.common.BhParams;
import pflab.bunnyHop.control.BhNodeControllerInSelectionView;
import pflab.bunnyHop.control.ComboBoxNodeController;

import pflab.bunnyHop.control.ConnectiveNodeController;
import pflab.bunnyHop.control.LabelNodeController;
import pflab.bunnyHop.control.TextFieldNodeController;
import pflab.bunnyHop.control.VoidNodeController;
import pflab.bunnyHop.message.BhMsg;
import pflab.bunnyHop.message.MsgData;
import pflab.bunnyHop.message.MsgTransporter;
import pflab.bunnyHop.model.BhNode;
import pflab.bunnyHop.model.TextNode;
import pflab.bunnyHop.model.VoidNode;
import pflab.bunnyHop.model.connective.ConnectiveNode;
import pflab.bunnyHop.model.connective.Connector;
import pflab.bunnyHop.view.BhNodeView;
import pflab.bunnyHop.view.BhNodeViewStyle;
import pflab.bunnyHop.view.ConnectiveNodeView;
import pflab.bunnyHop.view.TextFieldNodeView;
import pflab.bunnyHop.view.VoidNodeView;
import pflab.bunnyHop.undo.UserOperationCommand;
import pflab.bunnyHop.view.ComboBoxNodeView;
import pflab.bunnyHop.view.LabelNodeView;

/**
 * ノードのMVC関係を構築するクラス
 * @author K.Koike
 * */
public class NodeMVCBuilder implements BhModelProcessor {

	private BhNodeView topNodeView;	//!< MVCを構築したBhNodeツリーのトップノードのビュー
	private Deque<ConnectiveNodeView> parentStack = new LinkedList<>();	//!< 子ノードの追加先のビュー
	private MVCConnector mvcConnector;
	private final UserOperationCommand userOpeCmd;	//!< undo/redo用コマンドオブジェクト

	/**
	 * コンストラクタ
	 * @param type Controller の種類 (ワークスペースのノード用かノードセレクタ用)
	 * @param userOpeCmd undo/redo用コマンドオブジェクト
	 * */
	public NodeMVCBuilder(ControllerType type, UserOperationCommand userOpeCmd) {

		if (type == ControllerType.Default) {
			mvcConnector = new DefaultConnector();
		}
		else if (type == ControllerType.Template) {
			mvcConnector = new TemplateConnector();
		}
		this.userOpeCmd = userOpeCmd;
	};

	
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
		if (node.type.equals(BhParams.BhModelDef.attrValueTextField)) {
			TextFieldNodeView textNodeView = new TextFieldNodeView(node, viewStyle);
			textNodeView.init();
			node.setScriptScope(textNodeView);
			mvcConnector.connect(node, textNodeView);
			nodeView = textNodeView;
		}
		else if (node.type.equals(BhParams.BhModelDef.attrValueComboBox)) {
			ComboBoxNodeView comboBoxNodeView = new ComboBoxNodeView(node, viewStyle);
			comboBoxNodeView.init();
			node.setScriptScope(comboBoxNodeView);
			mvcConnector.connect(node, comboBoxNodeView);
			nodeView = comboBoxNodeView;
		}
		else if (node.type.equals(BhParams.BhModelDef.attrValueLabel)) {
			LabelNodeView labelNodeView = new LabelNodeView(node, viewStyle);
			labelNodeView.init();
			node.setScriptScope(labelNodeView);
			mvcConnector.connect(node, labelNodeView);
			nodeView = labelNodeView;
		
		}
		if (topNodeView == null)
			topNodeView = nodeView;
		
		if(node.getOriginalNode() != null)
			MsgTransporter.instance().sendMessage(BhMsg.IMITATE_TEXT, new MsgData(node.getOriginalNode().getText()), node);
		
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
	private  class DefaultConnector implements MVCConnector {
		
		@Override
		public void connect(ConnectiveNode node, ConnectiveNodeView view) {
			ConnectiveNodeController controller = new ConnectiveNodeController(node, view);
			MsgTransporter.instance().setSenderAndReceiver(node, controller, userOpeCmd);
		}

		@Override
		public void connect(VoidNode node, VoidNodeView view) {
			VoidNodeController controller = new VoidNodeController(node, view);
			MsgTransporter.instance().setSenderAndReceiver(node, controller, userOpeCmd);
		}

		@Override
		public void connect(TextNode node, TextFieldNodeView view) {
			TextFieldNodeController controller = new TextFieldNodeController(node, view);
			MsgTransporter.instance().setSenderAndReceiver(node, controller, userOpeCmd);
		}

		@Override
		public void connect(TextNode node, LabelNodeView view) {
			LabelNodeController controller = new LabelNodeController(node, view);
			MsgTransporter.instance().setSenderAndReceiver(node, controller, userOpeCmd);
		}
		
		@Override
		public void connect(TextNode node, ComboBoxNodeView view) {
			ComboBoxNodeController controller = new ComboBoxNodeController(node, view);
			MsgTransporter.instance().setSenderAndReceiver(node, controller, userOpeCmd);
		}
	}

 	/**
	 * テンプレートノードリストに追加されるノードのModel と View をつなぐ機能を提供するクラス
	 */
	private  class TemplateConnector implements MVCConnector {

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




















