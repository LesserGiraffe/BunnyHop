package pflab.bunnyHop.control;

import pflab.bunnyHop.modelProcessor.NodeMVCBuilder;
import pflab.bunnyHop.common.Single;
import pflab.bunnyHop.message.BhMsg;
import pflab.bunnyHop.message.MsgData;
import pflab.bunnyHop.message.MsgTransporter;
import pflab.bunnyHop.model.BhNode;
import pflab.bunnyHop.model.TextNode;
import pflab.bunnyHop.model.Workspace;
import pflab.bunnyHop.root.BunnyHop;
import pflab.bunnyHop.view.BhNodeView;
import pflab.bunnyHop.view.TextFieldNodeView;
import pflab.bunnyHop.undo.UserOperationCommand;
import pflab.bunnyHop.common.Point2D;
import pflab.bunnyHop.modelHandler.BhNodeHandler;
import pflab.bunnyHop.view.ComboBoxNodeView;
import pflab.bunnyHop.view.LabelNodeView;

/**
 * ノード選択リストにあるBhNodeのコントローラ
 * @author K.Koike
 * */
public class BhNodeControllerInSelectionView {

	private final BhNode model;
	private final BhNodeView view;	//!< テンプレートリストのビュー
	private final BhNodeView rootView;	//!< 上のview ルートとなるview

	/**
	 * コンストラクタ
	 * @param model 管理するモデル
	 * @param view 管理するビュー
	 * */
	public BhNodeControllerInSelectionView(BhNode model, BhNodeView view, BhNodeView rootView) {

		this.model = model;
		this.view = view;
		this.rootView = rootView;

		setMouseEventHandler();

		if (view instanceof TextFieldNodeView) {
			TextFieldNodeController.setTextChangeHandler((TextNode)model, (TextFieldNodeView)view);
		}
		else if (view instanceof ComboBoxNodeView) {
			ComboBoxNodeController.setItemChangeHandler((TextNode)model, (ComboBoxNodeView)view);
		}
		else if (view instanceof LabelNodeView) {
			LabelNodeController.setInitStr((TextNode)model, (LabelNodeView)view);
		}
	}

	/**
	 * View が走査されたときのイベントハンドラをセットする
	 * */
	private void setMouseEventHandler() {

		Single<BhNodeView> currentView = new Single<>();	//現在、テンプレートのBhNodeView 上に発生したてマウスイベントを送っているワークスペース上の view

		//マウスボタンを押したとき
		view.getEventManager().setOnMousePressedHandler(mouseEvent -> {
			
			Workspace currentWS = BunnyHop.instance().getCurrentWorkspace();
			if (currentWS == null)
				return;
			
			UserOperationCommand userOpeCmd = new UserOperationCommand();
			NodeMVCBuilder builder = new NodeMVCBuilder(NodeMVCBuilder.ControllerType.Default, userOpeCmd);
			BhNode newNode = model.findRootNode().copy(userOpeCmd);
			newNode.accept(builder);	//MVC構築
			currentView.content = builder.getTopNodeView();
			Point2D posOnRootView = BhNodeView.getRelativePos(rootView, view);	//クリックされたテンプレートノードのルートノード上でのクリック位置
			posOnRootView.x += mouseEvent.getX();
			posOnRootView.y += mouseEvent.getY();
			MsgData posOnWS = MsgTransporter.instance().sendMessage(BhMsg.SCENE_TO_WORKSPACE, new MsgData(mouseEvent.getSceneX(), mouseEvent.getSceneY()) ,currentWS);
			BhNodeHandler.instance.addRootNode(
				currentWS,
				newNode,
				posOnWS.doublePair._1 - posOnRootView.x ,
				posOnWS.doublePair._2 - posOnRootView.y,
				userOpeCmd);
			MsgTransporter.instance().sendMessage(BhMsg.SET_USER_OPE_CMD, new MsgData(userOpeCmd), newNode);	//undo用コマンドセット
			currentView.content.getEventManager().propagateEvent(mouseEvent);
			BunnyHop.instance().hideTemplatePanel();
			mouseEvent.consume();
		});

		//ドラッグ中
		view.getEventManager().setOnMouseDraggedHandler(mouseEvent -> {
			
			if (currentView.content == null)
				return;
			currentView.content.getEventManager().propagateEvent(mouseEvent);
		});

		//ドラッグを検出(先にsetOnMouseDraggedが呼ばれ、ある程度ドラッグしたときにこれが呼ばれる)
		view.getEventManager().setOnDragDetectedHandler(mouseEvent -> {
			
			if (currentView.content == null)
				return;
			currentView.content.getEventManager().propagateEvent(mouseEvent);
		});

		//マウスボタンを離したとき
		view.getEventManager().setOnMouseReleasedHandler(mouseEvent -> {
			
			if (currentView.content == null)
				return;
			currentView.content.getEventManager().propagateEvent(mouseEvent);
			currentView.content = null;
		});
	}
}










