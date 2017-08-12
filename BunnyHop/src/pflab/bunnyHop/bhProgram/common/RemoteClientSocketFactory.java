package pflab.bunnyHop.bhProgram.common;

import java.io.IOException;
import java.io.Serializable;
import java.net.Socket;
import java.rmi.server.RMIClientSocketFactory;

/**
 * リモート通信用のソケットを作成するファクトリ
 * @author K.Koike
 */
public class RemoteClientSocketFactory implements RMIClientSocketFactory, Serializable {
	
	private int id;	//!< 同一性確認のためのID
	
	/**
	 * コンストラクタ
	 * @param id オブジェクトの同一性確認のためのID
	 */
	public RemoteClientSocketFactory(int id) {
		this.id = id;
	}
	
	@Override
	public Socket createSocket(String host, int port) throws IOException {
		Socket socket = null;
		try {
			socket = new Socket(host, port);
		}
		catch(IOException e) {
			throw new IOException();
		}
		return socket;
	}
	
	@Override
	public int hashCode() {
		return id;
    }

	@Override
	public boolean equals(Object obj) {
		return (getClass() == obj.getClass()) && (id == ((RemoteClientSocketFactory)obj).id);
    }
}

