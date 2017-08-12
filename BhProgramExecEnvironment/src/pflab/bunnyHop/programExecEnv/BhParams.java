package pflab.bunnyHop.programExecEnv;

/**
 *
 * @author K.Koike
 */
public class BhParams {
	/**
	 * ファイルパス関連のパラメータ
	 */
	public static class Path {
		public static String compiled = "compiled";
	}
	
	public static int maxQueueSize = 2048;
	public static int popSendDataTimeout = 3;	//!< BunnyHopへの送信データキューの読み出しタイムアウト(sec)
	public static int pushSendDataTimeout = 3;	//!< BunnyHopへの送信データキューの書き込みタイムアウト(sec)
	public static int pushRecvDataTimeout = 3;	//!< BunnyHopからの受信データキューの書き込みタイムアウト (sec)
	
	public static class BhProgram {
		public static String inoutModuleName = "inout";
		public static String rmiTcpPortSuffix = "@RmiTcpPort";	//BhProgram実行環境との通信に使うRMIオブジェクトを探す際のTCPポート
	}
}
