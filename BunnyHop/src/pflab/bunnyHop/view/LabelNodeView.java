package pflab.bunnyHop.view;

import java.io.IOException;
import java.nio.file.Path;
import javafx.fxml.FXMLLoader;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import pflab.bunnyHop.root.MsgPrinter;
import pflab.bunnyHop.common.BhParams;
import pflab.bunnyHop.common.Point2D;
import pflab.bunnyHop.model.TextNode;
import pflab.bunnyHop.configFileReader.FXMLCollector;

/**
 * テキストフィールドを入力フォームに持つビュー
 * @author K.Koike
 */
public class LabelNodeView extends BhNodeView implements ImitationCreator {

	private Label label = new Label();
	private final TextNode model;
	private Button imitCreateImitBtn;	//!< イミテーション作成ボタン

	public LabelNodeView(TextNode model, BhNodeViewStyle viewStyle) {
		super(viewStyle, model);
		this.model = model;
	}

	/**
	 * GUI部品の読み込みと初期化を行う
	 */
	@Override
	public void init() {
		
		String inputControlFileName = BhNodeViewStyle.nodeID_inputControlFileName.get(model.getID());
		if (inputControlFileName != null) {
			Path filePath = FXMLCollector.instance.getFilePath(inputControlFileName);
			try {
				FXMLLoader loader = new FXMLLoader(filePath.toUri().toURL());
				label = (Label)loader.load();
			} catch (IOException | ClassCastException ex) {
				MsgPrinter.instance.ErrMsgForDebug(ex.toString());
			}
		}
		getChildren().add(label);

		if (model.getImitationInfo().canCreateImitManually) {
			imitCreateImitBtn = loadButton(BhParams.Path.imitButtonFXML, viewStyle.imitation);
			if (imitCreateImitBtn != null)
				getChildren().add(imitCreateImitBtn);
		}		
		initStyle(viewStyle);
		setFuncs(this::updateStyleFunc, null);
	}
	
	private void initStyle(BhNodeViewStyle viewStyle) {

		label.autosize();
		label.setMouseTransparent(true);
		label.setTranslateX(viewStyle.leftMargin);
		label.setTranslateY(viewStyle.topMargin);
		label.getStyleClass().add(viewStyle.label.cssClass);
		label.heightProperty().addListener(newValue -> getAppearanceManager().updateStyle(null));
		label.widthProperty().addListener(newValue -> getAppearanceManager().updateStyle(null));
		getAppearanceManager().addCssClass(BhParams.CSS.classLabelNode);
	}

	/**
	 * このビューのモデルであるBhNodeを取得する
	 * @return このビューのモデルであるBhNode
	 */
	@Override
	public TextNode getModel() {
		return model;
	}
	
	/**
	 * モデルの構造を表示する
	 * @param depth 表示インデント数
	 * */
	@Override
	public void show(int depth) {
		MsgPrinter.instance.MsgForDebug(indent(depth) + "<LabelView" + ">   " + this.hashCode());
		MsgPrinter.instance.MsgForDebug(indent(depth + 1) + "<content" + ">   " + label.getText());
	}

	/**
	 * ノードの大きさや見た目を変える関数
	 * */
	private void updateStyleFunc(BhNodeViewGroup child) {

		viewStyle.width = label.getWidth();
		viewStyle.height = label.getHeight();
		getAppearanceManager().updatePolygonShape(viewStyle.drawBody);
		if (parent != null) {
			parent.updateStyle();
		}
		else {
			Point2D pos = getPositionManager().getPosOnWorkspace();	//workspace からの相対位置を計算
			getPositionManager().updateAbsPos(pos.x, pos.y);
		}
	}

	public String getText() {
		return label.getText();
	}

	public void setText(String text) {
		label.setText(text);
	}
		
	@Override
	public Button imitCreateButton() {
		return imitCreateImitBtn;
	}
}
