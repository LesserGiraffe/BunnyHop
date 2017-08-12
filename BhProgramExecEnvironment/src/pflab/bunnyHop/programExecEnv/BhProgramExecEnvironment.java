package pflab.bunnyHop.programExecEnv;

import pflab.bunnyHop.bhProgram.common.BhProgramHandler;
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
import pflab.bunnyHop.bhProgram.common.LocalClientSocketFactory;
import pflab.bunnyHop.bhProgram.common.RemoteClientSocketFactory;

/**
 * @author K.Koike
 */
public class BhProgramExecEnvironment {

	public static void main(String[] args) {
		try {
			boolean local = true;
			if (args.length >= 1)
				local = args[0].equals("true");
			
			BhProgramHandlerImpl programHandler = new BhProgramHandlerImpl();
			programHandler.init();
			Remote remote =
				UnicastRemoteObject.exportObject(
					programHandler,
					0,
					local ? new LocalClientSocketFactory(0) : new RemoteClientSocketFactory(0),
					local ? new LocalServerSocketFactory(0) : new RemoteServerSocketFactory(0));
			RMIServerSocketFactory socketFactory = local ? new LocalServerSocketFactory(1) : new RemoteServerSocketFactory(1);
			Registry registry =
				LocateRegistry.createRegistry(
					0,
					RMISocketFactory.getDefaultSocketFactory(),
					socketFactory);
			registry.rebind(BhProgramHandler.class.getSimpleName(), remote);
			if (socketFactory instanceof LocalServerSocketFactory)
				System.out.println("\n" + ((LocalServerSocketFactory)socketFactory).getLocalPort() + BhParams.BhProgram.rmiTcpPortSuffix);	//don't remove
			else
				System.out.println("\n" + ((RemoteServerSocketFactory)socketFactory).getLocalPort() + BhParams.BhProgram.rmiTcpPortSuffix);	//don't remove
		}
		catch(IOException e){
			System.out.println("\n" + "null" + BhParams.BhProgram.rmiTcpPortSuffix);	//don't remove
		}
	}

}
