package ar.edu.itba.pod.mmxivii.sube.server;

import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;

public class RmiRunner {
	public static void main(String[] args) throws RemoteException {
		final int port;
		if (args.length != 1)
			port = 7242;
		else
			port = Integer.valueOf(args[0]);
		System.out.println("Listening on port" + port);
		LocateRegistry.createRegistry(port);
		while (true) {
			;
		}

	}
}
