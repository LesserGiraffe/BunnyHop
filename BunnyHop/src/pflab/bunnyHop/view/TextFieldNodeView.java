package pflab.bunnyHop.view;

import java.util.function.Function;
import java.util.stream.Stream;
import java.io.IOException;
import java.nio.file.Path;
import javafx.scene.control.TextField;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.css.PseudoClass;
import javafx.fxml.FXMLLoader;
import javafx.scene.control.Button;
import pflab.bunnyHop.root.MsgPrinter;
import pflab.bunnyHop.common.BhParams;
import pflab.bunnyHop.common.Point2D;
import pflab.bunnyHop.common.Util;
import pflab.bunnyHop.model.TextNode;
import pflab.bunnyHop.configFileReader.FXMLCollector;

/**
 * テキストフィールドを入力フォームに持つビュー
 * @author K.Koike
 */
public class TextFieldNodeView extends BhNodeView implements ImitationCreator {

	private TextField textField = new TextField();
	private final TextNode model;
	private Button imitCreateImitBtn;	//!< イミテーション作成ボタン

	public TextFieldNodeView(TextNode model, BhNodeViewStyle viewStyle) {
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
				textField = (TextField)loader.load();
			} catch (IOException | ClassCastException e) {
				MsgPrinter.instance.ErrMsgForDebug("failed to initialize " + TextFieldNodeView.class.getSimpleName() + "\n" + e.toString());
			}
		}
		getChildren().add(textField);

		if (model.getImitationInfo().canCreateImitManually) {
			imitCreateImitBtn = loadButton(BhParams.Path.imitButtonFXML, viewStyle.imitation);
			if (imitCreateImitBtn != null)
				getChildren().add(imitCreateImitBtn);

		}		
		initStyle(viewStyle);
		setFuncs(this::updateStyleFunc, null);
	}
	
	private void initStyle(BhNodeViewStyle viewStyle) {
		
		textField.setTranslateX(viewStyle.leftMargin);
		textField.setTranslateY(viewStyle.topMargin);
		textField.getStyleClass().add(viewStyle.textField.cssClass);		
		textField.heightProperty().addListener(observable -> getAppearanceManager().updateStyle(null));
		textField.widthProperty().addListener(observable -> getAppearanceManager().updateStyle(null));
		textField.fontProperty().addListener(observable -> {
			String text = textField.getText();
			setText(text + " "); 
			setText(text);});
		getAppearanceManager().addCssClass(BhParams.CSS.classTextFieldNode);
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
	 * テキスト変更時のイベントハンドラを登録する
	 * @param checkFormatFunc 入力された文字列の形式が正しいかどうか判断する関数 (テキスト変更時のイベントハンドラから呼び出す)
	 * */
	public void setTextChangeListener(Function<String, Boolean> checkFormatFunc) {

		String wsMargine = Stream.iterate(" ", ws -> ws).limit((long)viewStyle.textField.whiteSpaceMargine).reduce("", String::concat);
		String minWidthWS = Stream.iterate(" ", ws -> ws).limit((long)viewStyle.textField.minWhiteSpace).reduce("", String::concat);

		// テキストの長さに応じてTextField の長さが変わるように
		textField.textProperty().addListener((ObservableValue<? extends String> observable, String oldValue, String newValue) -> {				
			double newWidth = Util.calcStrWidth(newValue + wsMargine, textField.getFont());
			double minWidth = Util.calcStrWidth(minWidthWS, textField.getFont());
			newWidth = Math.max(newWidth, minWidth);	//最低限の長さは確保する
			newWidth += textField.getPadding().getRight() + textField.getPadding().getLeft();
			textField.setPrefWidth(newWidth);
			
			boolean acceptable = checkFormatFunc.apply(newValue);
			if (acceptable)
				textField.pseudoClassStateChanged(PseudoClass.getPseudoClass(BhParams.CSS.pseudoError), false);
			else
				textField.pseudoClassStateChanged(PseudoClass.getPseudoClass(BhParams.CSS.pseudoError), true);
		});
	}

	/**
	 * テキストフィールドのカーソルon/off時のイベントハンドラを登録する
	 * @param changeFocusFunc テキストフィールドのカーソルon/off時のイベントハンドラ
	 * */
	public void setObservableListener(ChangeListener<? super Boolean> changeFocusFunc) {
		textField.focusedProperty().addListener(changeFocusFunc);
	}

	/**
	 * モデルの構造を表示する
	 * @param depth 表示インデント数
	 * */
	@Override
	public void show(int depth) {
		MsgPrinter.instance.MsgForDebug(indent(depth) + "<TextNodeView" + ">   " + this.hashCode());
		MsgPrinter.instance.MsgForDebug(indent(depth + 1) + "<content" + ">   " + textField.getText());
	}

	/**
	 * ノードの大きさや見た目を変える関数
	 * */
	private void updateStyleFunc(BhNodeViewGroup child) {

		viewStyle.width = textField.getWidth();
		viewStyle.height = textField.getHeight();
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
		return textField.getText();
	}

	public void setText(String text) {
		textField.setText(text);
	}

	/**
	 * テキストフィールドが編集可能かどうかをセットする
	 * @param editable テキストフィールドが編集可能なときtrue
	 * */
	public void setEditable(boolean editable) {
		textField.setEditable(editable);
	}
	
	/**
	 * テキストフィールドが編集可能かどうかチェックする
	 * @return テキストフィールドが編集可能な場合 true
	 * */
	public boolean getEditable() {
		return textField.editableProperty().getValue();
	}
	
	@Override
	public Button imitCreateButton() {
		return imitCreateImitBtn;
	}
}
