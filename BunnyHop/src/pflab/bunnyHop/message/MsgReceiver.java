package pflab.bunnyHop.message;

/**
 * MsgTransporterからメッセージをもらうクラスのインタフェース
 * @author K.Koike
 */
public interface MsgReceiver {

	/**
	 * メッセージを受信する
	 * @param msg 受信したメッセージ
	 * @param data 受信したデータ
	 * @return 受信したメッセージに対する返信データ
	 * */
	MsgData receiveMsg(BhMsg msg, MsgData data);
}
