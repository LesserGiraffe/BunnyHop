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
import javafx.scene.control.TextArea;
import javafx.scene.control.TextInputControl;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.Region;
import javafx.scene.text.Text;
import net.seapanda.bunnyhop.common.BhParams;
import net.seapanda.bunnyhop.common.Vec2D;
import net.seapanda.bunnyhop.common.tools.MsgPrinter;
import net.seapanda.bunnyhop.configfilereader.FXMLCollector;
import net.seapanda.bunnyhop.model.node.TextNode;
import net.seapanda.bunnyhop.view.ViewHelper;

/**
 * テキストエリアを入力フォームに持つビュー
 * @author K.Koike
 */
public class TextAreaNodeView  extends TextInputNodeView implements ImitationCreator {

	private TextArea textArea = new TextArea();
	private final TextNode model;
	private Button imitCreateImitBtn;	//!< イミテーション作成ボタン

	public TextAreaNodeView(TextNode model, BhNodeViewStyle viewStyle) {
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
		getChildren().add(textArea);

		textArea.addEventFilter(MouseEvent.ANY, event -> {
			getEventManager().propagateEvent(event);
			if (isTemplate)
				event.consume();
		});

		if (model.getImitationInfo().canCreateImitManually) {
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

		textArea.setTranslateX(viewStyle.paddingLeft);
		textArea.setTranslateY(viewStyle.paddingTop);
		textArea.getStyleClass().add(viewStyle.textArea.cssClass);
		textArea.setWrapText(false);
		textArea.heightProperty().addListener((observable, oldVal , newVal) ->
			getAppearanceManager().updateAppearance(null));
		textArea.widthProperty().addListener((observable, oldVal , newVal) ->
			getAppearanceManager().updateAppearance(null));
		textArea.setWrapText(false);
		textArea.setMaxSize(USE_PREF_SIZE, USE_PREF_SIZE);
		textArea.setMinSize(USE_PREF_SIZE, USE_PREF_SIZE);
		getAppearanceManager().addCssClass(BhParams.CSS.CLASS_TEXT_AREA_NODE);
	}

	/**
	 * GUI部品をロードする
	 * @return ロードに成功した場合 true. 失敗した場合 false.
	 * */
	private boolean loadComponent() {

		String inputControlFileName = BhNodeViewStyle.nodeID_inputControlFileName.get(model.getID());
		if (inputControlFileName == null) {
			Path filePath = FXMLCollector.INSTANCE.getFilePath(inputControlFileName);
			try {
				FXMLLoader loader = new FXMLLoader(filePath.toUri().toURL());
				textArea = (TextArea)loader.load();
			} catch (IOException | ClassCastException e) {
				MsgPrinter.INSTANCE.errMsgForDebug(
					"failed to initialize " + TextAreaNodeView.class.getSimpleName() + "\n" + e.toString());
				return false;
			}
		}
		return true;
	}

	@Override
	public TextNode getModel() {
		return model;
	}

	/**
	 * テキスト変更時のイベントハンドラを登録する
	 * @param checkFormatFunc 入力された文字列の形式が正しいかどうか判断する関数 (テキスト変更時のイベントハンドラから呼び出す)
	 * */
	public void setTextChangeListener(Function<String, Boolean> checkFormatFunc) {

		textArea.boundsInLocalProperty().addListener(
			(observable, oldVal, newVal) -> updateTextAreaLook(checkFormatFunc));

		// テキストの長さに応じてTextArea のサイズが変わるように
		textArea.textProperty().addListener(
			(observable, oldVal, newVal) ->	updateTextAreaLook(checkFormatFunc));
	}

	/**
	 * テキストエリアの見た目を変える
	 * @param checkFormatFunc テキストのフォーマットをチェックする関数
	 * @param text このテキストに基づいてテキストエリアの見た目を変える
	 * */
	private void updateTextAreaLook(Function<String, Boolean> checkFormatFunc) {

		Text textPart = (Text)textArea.lookup(".text");
		Region content = (Region)textArea.lookup(".content");
		if (textPart != null && content != null){

			// 正確な文字部分の境界を取得するため, GUI部品内部のTextの境界は使わない.
			Vec2D textBounds = ViewHelper.INSTANCE.calcStrBounds(
				textPart.getText(),
				textPart.getFont(),
				textPart.getBoundsType(),
				textPart.getLineSpacing());

			double newWidth = Math.max(textBounds.x, viewStyle.textArea.minWidth);
			//幅を (文字幅 + パディング) にするとwrapの設定によらず文字列が折り返してしまういことがあるので定数3を足す
			//この定数はフォントやパディングが違っても機能する.
			newWidth += content.getPadding().getLeft() + content.getPadding().getRight() + 3;
			double newHeight = Math.max(textBounds.y, viewStyle.textArea.minHeight);
			newHeight += content.getPadding().getTop() + content.getPadding().getBottom() + 2;
			//textArea.setMaxSize(newWidth, newHeight);
			textArea.setPrefSize(newWidth, newHeight);

			boolean acceptable = checkFormatFunc.apply(textPart.getText());
			if (acceptable)
				textArea.pseudoClassStateChanged(PseudoClass.getPseudoClass(BhParams.CSS.PSEUDO_BHNODE), false);
			else
				textArea.pseudoClassStateChanged(PseudoClass.getPseudoClass(BhParams.CSS.PSEUDO_BHNODE), true);
		}
	}

	@Override
	public void show(int depth) {
		MsgPrinter.INSTANCE.msgForDebug(indent(depth) + "<" + this.getClass().getSimpleName() + ">   " + this.hashCode());
		MsgPrinter.INSTANCE.msgForDebug(indent(depth + 1) + "<content" + ">   " + textArea.getText());
	}

	/**
	 * ノードの大きさや見た目を変える
	 * */
	private void updateShape(BhNodeViewGroup child) {

		viewStyle.width = textArea.getWidth();
		viewStyle.height = textArea.getHeight();
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
		return textArea;
	}

	@Override
	public Button imitCreateButton() {
		return imitCreateImitBtn;
	}
}











