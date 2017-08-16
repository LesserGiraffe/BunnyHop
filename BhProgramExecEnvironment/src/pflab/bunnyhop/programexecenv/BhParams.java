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
package pflab.bunnyhop.programexecenv;

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
