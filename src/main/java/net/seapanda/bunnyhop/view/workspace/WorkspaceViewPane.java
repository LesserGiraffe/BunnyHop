package net.seapanda.bunnyhop.view.workspace;

import javafx.scene.layout.Pane;

/**
 * @author K.Koike
 * ワークスペースビュー内の描画物を保持するビュー
 * */
public class WorkspaceViewPane extends Pane {

	private WorkspaceView holder;	//!< このペインを保持しているワークスペースビュー

	/**
	 * このビューが所属するワークスペースビューを設定する
	 */
	public void setHolder(WorkspaceView holder) {
		this.holder = holder;
	}

	/**
	 * このビューが所属するワークスペースビューを取得する
	 */
	public WorkspaceView getHolder() {
		return holder;
	}
}
