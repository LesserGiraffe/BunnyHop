package pflab.bunnyHop.message;

import java.util.HashMap;
import pflab.bunnyHop.undo.UserOperationCommand;

/**
 * メッセージを送信するクラス
 * @author K.Koike
 * */
public class MsgTransporter {

	private HashMap<MsgSender, MsgReceiver> sender_receiver = new HashMap<>();	//!< node からのメッセージ送信に使うハッシュマップ

	private MsgTransporter() {};
	static private MsgTransporter instance = new MsgTransporter();	//!< シングルトンインスタンス

	/**
	 * シングルトンインスタンスを取得する
	 * */
	public static MsgTransporter instance() {
		return instance;
	}

	/**
	 * メッセージの送り手と受け手を追加する
	 * @param sender メッセージの送り手オブジェクト
	 * @param receiver メッセージの受け手オブジェクト
	 * @param userOpeCmd undo/redo用コマンドオブジェクト
	 * */
	public void setSenderAndReceiver(
		MsgSender sender,
		MsgReceiver receiver,
		UserOperationCommand userOpeCmd) {
		assert sender_receiver.get(sender) == null;
		sender_receiver.put(sender, receiver);
		userOpeCmd.pushCmdOfSetSenderAndReceiver(sender);
	}

	/**
	 * メッセージの送り手と受け手の対応を削除する
	 * @param sender 削除したいメッセージ送信者
	 * @param userOpeCmd undo/redo用コマンドオブジェクト
	 * @return 削除した送り手に対応付けられていたメッセージ受信者
	 * */
	public MsgReceiver deleteSenderAndReceiver(MsgSender sender, UserOperationCommand userOpeCmd) {
		MsgReceiver receiver =  sender_receiver.remove(sender);
		userOpeCmd.pushCmdOfDeleteSenderAndReceiver(sender, receiver);
		return receiver;
	}

	/**
	 * senders に対応するそれぞれの receiver に順番にメッセージを送信する<br>
	 * 2つめ以降のsender には, 1つ前のreceiver の戻り値がデータとして渡される
	 * @param msg 送信メッセージ
	 * @param data 1番目のreceiver に渡されるメッセージ
	 * @param senders メッセージの送り手オブジェクト
	 * @return 最後のメッセージ送信先から返されるデータ
	 * */
	public MsgData sendMessage(BhMsg msg, MsgData data, MsgSender... senders) {

		for (MsgSender sender : senders) {
			data = sender_receiver
				.get(sender)
				.receiveMsg(msg, data);
		}
		return data;
	}

	/**
	 * senders に対応するそれぞれの receiver に順番にメッセージを送信する<br>
	 * 2つめ以降のsender には, 1つ前のreceiver の戻り値がデータとして渡される
	 * @param msg 送信メッセージ
	 * @param senders メッセージの送り手オブジェクト
	 * @return 最後のメッセージ送信先から返されるデータ
	 * */
	public MsgData sendMessage(BhMsg msg, MsgSender... senders) {

		MsgData data = null;
		for (MsgSender sender : senders) {
			data = sender_receiver.get(sender).receiveMsg(msg, data);
		}
		return data;
	}
	
	/**
	 * 送信 - 受信ハッシュに登録されているペアの数を取得する (デバッグ用)
	 * @return 送信 - 受信リストに登録されているペア
	 */
	public int getNumPair() {
		return sender_receiver.size();
	}
	
	/**
	 * 引数で指定した送信オブジェクトに対応する受信オブジェクトを返す (デバッグ用)
	 * @param sender 対応する受信オブジェクトを取得したい送信オブジェクト
	 * @return 引数で指定した送信オブジェクトに対応する受信オブジェクト
	 */
	public MsgReceiver getReceiver(MsgSender sender) {
		return sender_receiver.get(sender);
	}
}
















