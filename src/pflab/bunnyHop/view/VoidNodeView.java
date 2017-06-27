package pflab.bunnyHop.view;

import pflab.bunnyHop.root.MsgPrinter;
import pflab.bunnyHop.common.BhParams;
import pflab.bunnyHop.model.VoidNode;

/**
 * VoidNode に対応するビュー
 * @author K.Koike
 */
public class VoidNodeView extends BhNodeView {

	private final VoidNode model;	//!< このビューに対応するモデル
	
	/**
	 * コンストラクタ
	 * @param model ビューに対応するモデル
	 * @param viewStyle ビューのスタイル
	 * */
	public VoidNodeView(VoidNode model, BhNodeViewStyle viewStyle) {

		super(viewStyle, model);
		this.model = model;
	}

	@Override
	public void init() {
		getAppearanceManager().addCssClass(BhParams.CSS.classVoidNode);
		setFuncs(this::updateStyleFunc, null);
	}
	
	/**
	 * このビューのモデルであるBhNodeを取得する
	 * @return このビューのモデルであるBhNode
	 */
	@Override
	public VoidNode getModel() {
		return model;
	}
	
	/**
	 * ノードの大きさや見た目を変える関数
	 * */
	private void updateStyleFunc(BhNodeViewGroup child) {
		
		boolean inner = (parent == null) ? true : parent.inner;
		//ボディサイズ決定
		if (!inner) {
			viewStyle.topMargin = 0.0;
			viewStyle.height = 0.0;
			viewStyle.bottomMargin = 0.0;
			viewStyle.leftMargin = 0.0;
			viewStyle.width = 0.0;
			viewStyle.rightMargin = 0.0;
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

		try {
			MsgPrinter.instance.MsgForDebug(indent(depth) + "<VoidNodeView" + ">   " + this.hashCode());
		}
		catch (Exception e){}
	}
}


