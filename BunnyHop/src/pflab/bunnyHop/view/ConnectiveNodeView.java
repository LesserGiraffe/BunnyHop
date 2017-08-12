package pflab.bunnyHop.view;

import pflab.bunnyHop.root.MsgPrinter;
import java.io.IOException;
import java.nio.file.Path;
import java.util.function.Consumer;
import javafx.fxml.FXMLLoader;
import javafx.scene.control.Button;
import pflab.bunnyHop.model.connective.ConnectiveNode;
import pflab.bunnyHop.view.BhNodeViewStyle.CNCTR_POS;
import pflab.bunnyHop.common.*;
import pflab.bunnyHop.configFileReader.FXMLCollector;

/**
 * ConnectiveNode に対応するビュークラス
 * @author K.Koike
 * */
public class ConnectiveNodeView extends BhNodeView implements ImitationCreator{

	private final BhNodeViewGroup innerGroup = new BhNodeViewGroup(this, true); //!< ノード内部に描画されるノードのGroup
	private final BhNodeViewGroup outerGroup = new BhNodeViewGroup(this, false); //!< ノード外部に描画されるノードのGroup
	private Button imitCreateImitBtn;	//!< イミテーション作成ボタン
	private ConnectiveNode model;
	
	/**
	 * コンストラクタ
	 * @param model ビューが表すモデル
	 * @param viewStyle ノードの見た目を決めるパラメータオブジェクト
	 * */
	public ConnectiveNodeView(ConnectiveNode model, BhNodeViewStyle viewStyle) {
		super(viewStyle, model);
		this.model = model;
	}
	
	/**
	 * GUI部品の追加とコールバック関数の登録を行う
	 */
	@Override
	public void init() {
		
		innerGroup.buildSubGroup(viewStyle.connective.inner);
		outerGroup.buildSubGroup(viewStyle.connective.outer);
		getChildren().add(innerGroup);
		getChildren().add(outerGroup);
		
		setFuncs(this::updateStyleFunc, this::updateAbsPosFunc);
		if (model.getImitationInfo().canCreateImitManually) {
			Path filePath = FXMLCollector.instance.getFilePath(BhParams.Path.imitButtonFXML);
			try {
				FXMLLoader loader = new FXMLLoader(filePath.toUri().toURL());
				imitCreateImitBtn = (Button)loader.load();
				setBtnStyle(viewStyle.imitation);
				getChildren().add(imitCreateImitBtn);
			} catch (IOException | ClassCastException e) {
				MsgPrinter.instance.ErrMsgForDebug("failed to initialize " + ConnectiveNodeView.class.getSimpleName() + "\n" + e.toString());
			}
		}
		getAppearanceManager().addCssClass(BhParams.CSS.classConnectiveNode);
	}

	/**
	 * このビューのモデルであるBhNodeを取得する
	 * @return このビューのモデルであるBhNode
	 */
	@Override
	public ConnectiveNode getModel() {
		return model;
	}
	
	/**
	 * ノード内部に描画されるノードをリストの最後に追加する
	 * @param view ノード内部に描画されるノード
	 * */
	public void addToGroup(BhNodeView view) {
		
		// innerGroup に追加できなかったらouterGroupに入れる
		if (!innerGroup.addNodeView(view))
			outerGroup.addNodeView(view);
	}

	/**
	 * このノードの絶対位置を更新する関数
	 * @param posX 本体部分左上のX位置
	 * @param posY 本体部分左上のY位置
	 * */
	private void updateAbsPosFunc(double posX, double posY) {

		getPositionManager().defaultUpdateAbsPos(posX, posY);
		//内部ノード絶対位置更新
		Point2D relativePos = innerGroup.getRelativePosFromParent();
		innerGroup.updateAbsPos(posX + relativePos.x, posY + relativePos.y);
		
		//外部ノード絶対位置更新
		Point2D bodySize = getRegionManager().getBodySize(false);
		if (viewStyle.connectorPos == CNCTR_POS.LEFT)	//外部ノードが右に繋がる
			outerGroup.updateAbsPos(posX + bodySize.x, posY);
		else										   //外部ノードが下に繋がる
			outerGroup.updateAbsPos(posX, posY + bodySize.y);
	}

	/**
	 * ノードの形状を更新する
	 * @param child 形状が変わった子ノード
	 * */
	private void updateStyleFunc(BhNodeViewGroup child) {
		
		Point2D sizeBefore = getRegionManager().getBodyAndOuterSize(true);
		if (child == null) {
			Point2D outerSize = outerGroup.getSize();	
			viewStyle.connective.outerWidth = outerSize.x;
			viewStyle.connective.outerHeight = outerSize.y;
			Point2D innerSize = innerGroup.getSize();	
			viewStyle.width = innerSize.x;
			viewStyle.height = innerSize.y;
			getAppearanceManager().updatePolygonShape(viewStyle.drawBody);			
		}
		else {
			Point2D childSize = child.getSize();
			if (child == outerGroup) {
				viewStyle.connective.outerWidth = childSize.x;
				viewStyle.connective.outerHeight = childSize.y;
			}
			else if (child == innerGroup){
				viewStyle.width = childSize.x;
				viewStyle.height = childSize.y;
				getAppearanceManager().updatePolygonShape(viewStyle.drawBody);
			}
		}
		
		updateChildRelativePos();
		
		Point2D sizeAfter = getRegionManager().getBodyAndOuterSize(true);
		if (parent == null ||
			(sizeBefore.equals(sizeAfter) && child != null)) {
			Point2D pos = getPositionManager().getPosOnWorkspace();	//workspace からの相対位置を計算
			getPositionManager().updateAbsPos(pos.x, pos.y);
			return;
		}
		
		parent.updateStyle();
	}

	/**
	 * ノードの親からの相対位置を指定する
	 * @param innerSizeList 内部描画ノードの大きさが格納された配列
	 * */
	private void updateChildRelativePos() {
		
		innerGroup.setRelativePosFromParent(viewStyle.leftMargin, viewStyle.topMargin);
		Point2D bodySize = getRegionManager().getBodySize(false);
		if (viewStyle.connectorPos == CNCTR_POS.LEFT)	//外部ノードが右に繋がる
			outerGroup.setRelativePosFromParent(bodySize.x, 0.0);
		else										   //外部ノードが下に繋がる
			outerGroup.setRelativePosFromParent(0.0, bodySize.y);
	}
	
	/**
	 * BhNodeView を引数にとる関数オブジェクトを実行する<br>
	 * 子ノードがある場合は、子ノードに関数オブジェクトを渡す
	 * @param visitorFunc BhNodeView を引数にとり処理するオブジェクト
	 * */
	@Override
	public void accept(Consumer<BhNodeView> visitorFunc) {
		visitorFunc.accept(this);
		innerGroup.accept(visitorFunc);
		outerGroup.accept(visitorFunc);
	}

	/**
	 * モデルの構造を表示する
	 * @param depth 表示インデント数
	 * */
	@Override
	public void show(int depth) {

		try {
			MsgPrinter.instance.MsgForDebug(indent(depth) + "<ConnectiveNodeView" + ">   " + this.hashCode());
			innerGroup.show(depth + 1);
			outerGroup.show(depth + 1);
		}
		catch (Exception e) {
			MsgPrinter.instance.MsgForDebug("connectiveNodeView show exception " + e);
		}
	}
	
	@Override
	public Button imitCreateButton() {
		return imitCreateImitBtn;
	}
}











