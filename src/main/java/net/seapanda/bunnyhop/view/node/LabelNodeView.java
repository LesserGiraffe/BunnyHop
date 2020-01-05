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

import javafx.scene.control.Label;
import net.seapanda.bunnyhop.common.Vec2D;
import net.seapanda.bunnyhop.common.constant.BhParams;
import net.seapanda.bunnyhop.common.tools.MsgPrinter;
import net.seapanda.bunnyhop.model.node.TextNode;
import net.seapanda.bunnyhop.view.ViewInitializationException;
import net.seapanda.bunnyhop.view.node.part.BhNodeViewStyle;
import net.seapanda.bunnyhop.view.node.part.BhNodeViewStyle.CNCTR_POS;
import net.seapanda.bunnyhop.view.node.part.ComponentLoader;
import net.seapanda.bunnyhop.view.node.part.ImitationCreationButton;
import net.seapanda.bunnyhop.view.node.part.PrivateTemplateCreationButton;
import net.seapanda.bunnyhop.viewprocessor.NodeViewProcessor;

/**
 * ラベルを入力フォームに持つビュー
 * @author K.Koike
 */
public final class LabelNodeView extends BhNodeView {

	private Label label = new Label();
	private final TextNode model;

	/**
	 * コンストラクタ
	 * @param model このノードビューに対応するノード
	 * @param viewStyle このノードビューのスタイル
	 * @throws ViewInitializationException ノードビューの初期化に失敗
	 */
	public LabelNodeView(TextNode model, BhNodeViewStyle viewStyle)
		throws ViewInitializationException {

		super(viewStyle, model);
		this.model = model;
		init();
	}

	private void init() throws ViewInitializationException {

		var labelOpt = ComponentLoader.<Label>loadComponent(model.getID());
		label = labelOpt.orElseThrow(() -> new ViewInitializationException(
			getClass().getSimpleName() + "  failed To load the Label of this view."));
		getTreeManager().addChild(label);

		if (model.canCreateImitManually) {
			var imitButtonOpt = ImitationCreationButton.create(model, viewStyle.imitation);
			var imitButton = imitButtonOpt.orElseThrow(() -> new ViewInitializationException(
				getClass().getSimpleName() + "  failed To load the Imitation Creation Button of this view."));
			getTreeManager().addChild(imitButton);
		}

		if (model.hasPrivateTemplateNodes()) {
			var privateTemplateBtnOpt = PrivateTemplateCreationButton.create(model, viewStyle.privatTemplate);
			var privateTemplateBtn = privateTemplateBtnOpt.orElseThrow(() -> new ViewInitializationException(
				getClass().getSimpleName() + "  failed To load the Private Template Button of this view."));
			getTreeManager().addChild(privateTemplateBtn);
		}

		initStyle();
	}

	private void initStyle() {

		label.autosize();
		label.setMouseTransparent(true);
		label.setTranslateX(viewStyle.paddingLeft);
		label.setTranslateY(viewStyle.paddingTop);
		label.getStyleClass().add(viewStyle.label.cssClass);
		label.heightProperty().addListener(newValue -> notifySizeChange());
		label.widthProperty().addListener(newValue -> notifySizeChange());
		getAppearanceManager().addCssClass(BhParams.CSS.CLASS_LABEL_NODE);
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
		MsgPrinter.INSTANCE.msgForDebug(indent(depth) + "<LabelView>   " + this.hashCode());
		MsgPrinter.INSTANCE.msgForDebug(indent(depth + 1) + "<content>   " + label.getText());
	}

	@Override
	protected void arrangeAndResize() {
		getAppearanceManager().updatePolygonShape();
	}

	@Override
	protected Vec2D getBodySize(boolean includeCnctr) {

		Vec2D cnctrSize = viewStyle.getConnectorSize();

		double bodyWidth = viewStyle.paddingLeft + label.getWidth() + viewStyle.paddingRight;
		if (includeCnctr && (viewStyle.connectorPos == CNCTR_POS.LEFT))
			bodyWidth += cnctrSize.x;

		double bodyHeight = viewStyle.paddingTop + label.getHeight() + viewStyle.paddingBottom;
		if (includeCnctr && (viewStyle.connectorPos == CNCTR_POS.TOP))
			bodyHeight += cnctrSize.y;

		return new Vec2D(bodyWidth, bodyHeight);
	}

	@Override
	protected Vec2D getNodeSizeIncludingOuter(boolean includeCnctr) {
		return getBodySize(includeCnctr);
	}

	public String getText() {
		return label.getText();
	}

	public void setText(String text) {
		label.setText(text);
	}

	@Override
	public void accept(NodeViewProcessor visitor) {
		visitor.visit(this);
	}
}














