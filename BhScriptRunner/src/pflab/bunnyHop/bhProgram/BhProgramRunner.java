package pflab.bunnyHop.bhProgram;

import pflab.bunnyHop.bhProgram.socket.LocalServerSocketFactory;
import java.io.IOException;
import java.io.Serializable;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.rmi.Remote;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.RMIServerSocketFactory;
import java.rmi.server.RMISocketFactory;
import java.rmi.server.UnicastRemoteObject;
import pflab.bunnyHop.bhProgram.socket.LocalClientSocketFactory;

/**
 * @author K.Koike
 */
public class BhProgramRunner {

	public static void main(String[] args) {
		
		final boolean local;
		if (args.length >= 1) {
			local = args[0].equals("true");
		}
		else {
			local = false;
		}
		
		try {
			BhProgramHandlerImpl programHandler = new BhProgramHandlerImpl();
			programHandler.init();
			Remote remote =
				UnicastRemoteObject.exportObject(
					programHandler,
					0,
					new LocalClientSocketFactory(0),
					new LocalServerSocketFactory(0));
			LocalServerSocketFactory socketFactory = new LocalServerSocketFactory(1);
			Registry registry =
				LocateRegistry.createRegistry(
					0,
					RMISocketFactory.getDefaultSocketFactory(),
					socketFactory);
			registry.rebind(BhProgramHandler.class.getSimpleName(), remote);
			System.out.println(socketFactory.getLocalPort()+"@");	//don't remove
		}
		catch(IOException e){
		}
	}

}
