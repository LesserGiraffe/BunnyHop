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

import net.seapanda.bunnyhop.common.BhParams;
import net.seapanda.bunnyhop.common.tools.MsgPrinter;
import net.seapanda.bunnyhop.model.node.TextNode;
import net.seapanda.bunnyhop.view.bodyshape.BodyShape.BODY_SHAPE;

/**
 * 内部に何も表示しないノードビュー
 * */
public class NoContentNodeView extends BhNodeView {

	private final TextNode model;	//!< このビューに対応するモデル

	/**
	 * コンストラクタ
	 * @param model ビューに対応するモデル
	 * @param viewStyle ビューのスタイル
	 * */
	public NoContentNodeView(TextNode model, BhNodeViewStyle viewStyle) {

		super(viewStyle, model);
		this.model = model;
	}

	/**
	 * 初期化する
	 */
	public void init() {

		initialize();
		getAppearanceManager().addCssClass(BhParams.CSS.CLASS_NO_CONTENT_NODE);
		setFuncs(this::updateStyleFunc, null);
		setMouseTransparent(true);

		parent.addListener((obs, oldVal, newVal) -> {

			boolean inner = (newVal == null) ? true : newVal.inner;
			//ボディサイズ決定
			if (!inner) {
				viewStyle.paddingTop = 0.0;
				viewStyle.height = 0.0;
				viewStyle.paddingBottom = 0.0;
				viewStyle.paddingLeft = 0.0;
				viewStyle.width = 0.0;
				viewStyle.paddingRight = 0.0;
				getAppearanceManager().setBodyShape(BODY_SHAPE.BODY_SHAPE_NONE);
			}
		});
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
	 * ノードの大きさや見た目を変える関数
	 * */
	private void updateStyleFunc(BhNodeViewGroup child) {

		getAppearanceManager().updatePolygonShape();
		if (parent.get() != null)
			parent.get().updateStyle();
	}

	/**
	 * モデルの構造を表示する
	 * @param depth 表示インデント数
	 * */
	@Override
	public void show(int depth) {
		MsgPrinter.INSTANCE.msgForDebug(indent(depth) + "<NoContentNodeView" + ">   " + this.hashCode());
	}
}
