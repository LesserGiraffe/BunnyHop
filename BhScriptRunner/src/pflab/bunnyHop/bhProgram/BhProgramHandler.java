package pflab.bunnyHop.bhProgram;

import java.rmi.Remote;
import java.rmi.RemoteException;

/**
 * スクリプトとBunnyHop間でデータを送受信するクラス
 * @author K.Koike
 */
public interface BhProgramHandler extends Remote {
	
	/**
	 * 引数で指定したスクリプトを実行する
	 * @param fileName 実行ファイル名
	 * @return 実行に成功した場合true
	 */
	public boolean runScript(String fileName) throws RemoteException;
	
	/**
	 * BunnyHopとの通信を切断する
	 */
	public void disconnect() throws RemoteException;
	
	/**
	 * BunnyHopとの通信を始める
	 */
	public void connect() throws RemoteException;
	
	/**
	 * スクリプト実行環境に向けてデータを送る
	 * @param data 送信するデータ. nullは駄目.
	 * @return 送信に成功した場合true
	 */
	public boolean sendDataToScript(BhProgramData data) throws RemoteException;
	
	/**
	 * スクリプト実行環境からデータを受信する
	 * @return 受信データ. 受信に失敗した場合もしくは受信データがなかった場合null
	 */
	public BhProgramData recvDataFromScript() throws RemoteException;
}
