package pflab.bunnyHop.root;

import javafx.application.Platform;
import javafx.scene.control.TextArea;

/**
 * メッセージ出力クラス
 * @author K.Koike
 * */
public class MsgPrinter {

	public static final MsgPrinter instance = new MsgPrinter();	//!< シングルトンインスタンス
	private TextArea mainMsgArea;
	
	/**
	 * デバッグ用メッセージ出力メソッド
	 * */
	public void ErrMsgForDebug(String msg) {
		System.out.println("ERR : " + msg);
	}

	/**
	 * デバッグ用メッセージ出力メソッド
	 * */
	public void MsgForDebug(String msg) {
		System.out.println("DBG : " + msg);
	}
	
	/**
	 * Bhユーザ向けにメッセージを出力する
	 */
	synchronized public void MsgForUser(String msg) {
		Platform.runLater(() -> mainMsgArea.appendText(msg));
	}
	
	/**
	 * メインのメッセージ出力エリアを登録する
	 * @param mainMsgArea 登録するメインのメッセージ出力エリア
	 */
	public void setMainMsgArea(TextArea mainMsgArea) {
		this.mainMsgArea = mainMsgArea;
	}
}
