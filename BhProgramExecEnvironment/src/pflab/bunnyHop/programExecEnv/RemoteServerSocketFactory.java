package pflab.bunnyHop.programExecEnv;

import java.io.IOException;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.rmi.server.RMIServerSocketFactory;

/**
 * ローカルのアクセスのみ許すソケットを作成するファクトリ
 * @author K.Koike
 */
public class RemoteServerSocketFactory implements RMIServerSocketFactory {
	
	private int localPort;
	private int id;	//!< 同一性確認のためのID
	
	/**
	 * コンストラクタ
	 * @param id オブジェクトの同一性確認のためのID
	 */
	public RemoteServerSocketFactory(int id) {
		this.id = id;
	}
	
	@Override
	public ServerSocket createServerSocket(int port) throws IOException {
		ServerSocket serverSocket = null;
		try {
			serverSocket = new ServerSocket(port);
		}
		catch(IOException e) {
			throw new IOException();
		}
		localPort = serverSocket.getLocalPort();
		return serverSocket;
	}
	
	@Override
	public int hashCode() {
		return id;
    }

	@Override
	public boolean equals(Object obj) {
		return (getClass() == obj.getClass()) && (id == ((RemoteServerSocketFactory)obj).id);
    }
	
	public int getLocalPort() {
		return localPort;
	}
}

