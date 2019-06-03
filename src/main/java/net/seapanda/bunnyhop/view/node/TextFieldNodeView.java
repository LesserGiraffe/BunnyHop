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
package net.seapanda.bunnyhop.view.node;

import java.io.IOException;
import java.nio.file.Path;
import java.util.Optional;
import java.util.function.Function;

import javafx.css.PseudoClass;
import javafx.fxml.FXMLLoader;
import javafx.scene.control.Button;
import javafx.scene.control.TextField;
import javafx.scene.control.TextInputControl;
import javafx.scene.input.MouseEvent;
import javafx.scene.text.Text;
import net.seapanda.bunnyhop.common.BhParams;
import net.seapanda.bunnyhop.common.Vec2D;
import net.seapanda.bunnyhop.common.tools.MsgPrinter;
import net.seapanda.bunnyhop.configfilereader.FXMLCollector;
import net.seapanda.bunnyhop.model.node.TextNode;
import net.seapanda.bunnyhop.view.ViewHelper;
import net.seapanda.bunnyhop.view.node.part.BhNodeViewStyle;
import net.seapanda.bunnyhop.view.node.part.ImitationCreator;
import net.seapanda.bunnyhop.viewprocessor.NodeViewProcessor;

/**
 * テキストフィールドを入力フォームに持つビュー
 * @author K.Koike
 */
public class TextFieldNodeView extends TextInputNodeView implements ImitationCreator {

	private TextField textField = new TextField();
	private final TextNode model;
	private Button imitCreateImitBtn;	//!< イミテーション作成ボタン

	public TextFieldNodeView(TextNode model, BhNodeViewStyle viewStyle) {
		super(model, viewStyle);
		this.model = model;
	}

	/**
	 * GUI部品の読み込みと初期化を行う
	 * @param isTemplate ノード選択パネルに表示されるノードであった場合true
	 */
	public boolean init(boolean isTemplate) {

		initialize();
		boolean success = loadComponent();
		getTreeManager().addChild(textField);

		textField.addEventFilter(MouseEvent.ANY, event -> {
			getEventManager().propagateEvent(event);
			if (isTemplate)
				event.consume();
		});

		if (model.canCreateImitManually) {
			Optional<Button> btnOpt = loadButton(BhParams.Path.IMIT_BUTTON_FXML, viewStyle.imitation);
			success &= btnOpt.isPresent();
			imitCreateImitBtn = btnOpt.orElse(new Button());
			getTreeManager().addChild(imitCreateImitBtn);
		}
		initStyle(viewStyle);
		setFuncs(this::updateShape, null);
		return success;
	}

	private void initStyle(BhNodeViewStyle viewStyle) {

		textField.setTranslateX(viewStyle.paddingLeft);
		textField.setTranslateY(viewStyle.paddingTop);
		textField.getStyleClass().add(viewStyle.textField.cssClass);
		textField.heightProperty().addListener(observable -> getAppearanceManager().updateAppearance(null));
		textField.widthProperty().addListener(observable -> getAppearanceManager().updateAppearance(null));
		textField.setMaxWidth(USE_PREF_SIZE);
		textField.setMinWidth(USE_PREF_SIZE);
		getAppearanceManager().addCssClass(BhParams.CSS.CLASS_TEXT_FIELD_NODE);
	}

	/**
	 * GUI部品をロードする
	 * @return ロードに成功した場合 true. 失敗した場合 false.
	 * */
	private boolean loadComponent() {

		String inputControlFileName = BhNodeViewStyle.nodeID_inputControlFileName.get(model.getID());
		if (inputControlFileName != null) {
			Path filePath = FXMLCollector.INSTANCE.getFilePath(inputControlFileName);
			try {
				FXMLLoader loader = new FXMLLoader(filePath.toUri().toURL());
				textField = (TextField)loader.load();
			} catch (IOException | ClassCastException e) {
				MsgPrinter.INSTANCE.errMsgForDebug(
					"failed to initialize " + TextFieldNodeView.class.getSimpleName() + "\n" + e.toString());
				return false;
			}
		}
		return true;
	}

	@Override
	public TextNode getModel() {
		return model;
	}

	@Override
	public void setTextChangeListener(Function<String, Boolean> checkFormatFunc) {

		textField.boundsInLocalProperty().addListener(
			(observable, oldVal, newVal) -> updateTextFieldLook(checkFormatFunc));

		// テキストの長さに応じてTextField の長さが変わるように
		textField.textProperty().addListener(
			(observable, oldVal, newVal) ->	updateTextFieldLook(checkFormatFunc));
	}

	/**
	 * テキストフィールドの見た目を変える
	 * @param checkFormatFunc テキストのフォーマットをチェックする関数
	 * */
	private void updateTextFieldLook(Function<String, Boolean> checkFormatFunc) {

		Text textPart = (Text)textField.lookup(".text");
		if (textPart != null){

			// 正確な文字部分の境界を取得するため, GUI部品内部のTextの境界は使わない.
			double newWidth = ViewHelper.INSTANCE.calcStrWidth(textPart.getText(), textPart.getFont());
			newWidth = Math.max(newWidth, viewStyle.textField.minWidth);
			//幅を (文字幅 + パディング) にするとキャレットの移動時に文字が左右に移動するので定数 2 を足す.
			//この定数はフォントやパディングが違っても機能する.
			newWidth += textField.getPadding().getLeft() + textField.getPadding().getRight() + 2;
			textField.setPrefWidth(newWidth);
			boolean acceptable = checkFormatFunc.apply(textPart.getText());
			if (acceptable)
				textField.pseudoClassStateChanged(PseudoClass.getPseudoClass(BhParams.CSS.PSEUDO_BHNODE), false);
			else
				textField.pseudoClassStateChanged(PseudoClass.getPseudoClass(BhParams.CSS.PSEUDO_BHNODE), true);
		}
	}

	@Override
	public void show(int depth) {
		MsgPrinter.INSTANCE.msgForDebug(indent(depth) + "<" + this.getClass().getSimpleName() + ">   " + this.hashCode());
		MsgPrinter.INSTANCE.msgForDebug(indent(depth + 1) + "<content" + ">   " + textField.getText());
	}

	/**
	 * ノードの大きさや見た目を変える
	 * */
	private void updateShape(BhNodeViewGroup child) {

		viewStyle.width = textField.getWidth();
		viewStyle.height = textField.getHeight();
		getAppearanceManager().updatePolygonShape();
		if (parent.get() != null) {
			parent.get().rearrangeChild();
		}
		else {
			Vec2D pos = getPositionManager().getPosOnWorkspace();	//workspace からの相対位置を計算
			getPositionManager().updateAbsPos(pos.x, pos.y);
		}
	}

	@Override
	protected TextInputControl getTextInputControl() {
		return textField;
	}

	@Override
	public Button imitCreateButton() {
		return imitCreateImitBtn;
	}

	@Override
	public void accept(NodeViewProcessor visitor) {
		visitor.visit(this);
	}
}












