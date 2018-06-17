package net.seapanda.bunnyhop.view.node;

import net.seapanda.bunnyhop.common.BhParams;
import net.seapanda.bunnyhop.common.tools.MsgPrinter;
import net.seapanda.bunnyhop.model.node.TextNode;

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

		boolean inner = (parent == null) ? true : parent.inner;
		//ボディサイズ決定
		if (!inner) {
			viewStyle.paddingTop = 0.0;
			viewStyle.height = 0.0;
			viewStyle.paddingBottom = 0.0;
			viewStyle.paddingLeft = 0.0;
			viewStyle.width = 0.0;
			viewStyle.paddingRight = 0.0;
		}

		boolean drawBody = inner && viewStyle.drawBody;
		getAppearanceManager().updatePolygonShape(drawBody);
		if (parent != null)
			parent.updateStyle();
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
