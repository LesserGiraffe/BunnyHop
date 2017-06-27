package pflab.bunnyHop.remote;

import java.rmi.Remote;
import java.rmi.RemoteException;

/**
 * @author K.Koike
 */
public interface RemoteCmdProcessor extends Remote {
	public boolean sendFile(String fileName, byte[] data) throws RemoteException;
}
