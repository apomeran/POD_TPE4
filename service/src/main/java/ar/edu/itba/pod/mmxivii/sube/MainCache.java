package ar.edu.itba.pod.mmxivii.sube;

import static ar.edu.itba.pod.mmxivii.sube.common.Utils.CARD_REGISTRY_BIND;
import static ar.edu.itba.pod.mmxivii.sube.common.Utils.CARD_SERVICE_REGISTRY_BIND;

import java.rmi.NotBoundException;
import java.rmi.RemoteException;
import java.util.Scanner;
import java.util.concurrent.TimeUnit;

import javax.annotation.Nonnull;

import org.jgroups.JChannel;

import ar.edu.itba.pod.mmxivii.sube.common.BaseMain;
import ar.edu.itba.pod.mmxivii.sube.common.CardRegistry;
import ar.edu.itba.pod.mmxivii.sube.common.CardServiceRegistry;
import ar.edu.itba.pod.mmxivii.sube.common.Utils;
import ar.edu.itba.pod.mmxivii.sube.service.CardServiceImpl;

public class MainCache extends BaseMain {
	private CardServiceRegistry cardServiceRegistry;
	private CardServiceImpl bypassCardService;
	private CardRegistry cardRegistry;

	private MainCache(@Nonnull String[] args) throws RemoteException,
			NotBoundException {
		super(args, DEFAULT_CLIENT_OPTIONS);
		getRegistry();
		cardRegistry = Utils.lookupObject(CARD_REGISTRY_BIND);
		cardServiceRegistry = Utils.lookupObject(CARD_SERVICE_REGISTRY_BIND);
		int nodesCount = 1;
		for (int n = 0; n < nodesCount; n++) {
			try {
				JChannel node = new JChannel();
				node.setName("nodo_" + n);
				CardServiceChannelImpl myCardService = new CardServiceChannelImpl(
						node, cardRegistry, cardServiceRegistry);
				bypassCardService = new CardServiceImpl(cardRegistry,
						myCardService);
				node.setReceiver(myCardService);
				node.connect("cluster");
				cardServiceRegistry.registerService(bypassCardService); // WE
																		// SHOULD
																		// USE
																		// MY
																		// CARD
																		// SERVICE
																		// INSTEAD
																		// BUT
																		// ITS
																		// NOT
																		// UNICAST
				Thread.sleep(TimeUnit.SECONDS.toMillis(5));
			} catch (Exception e) {
			}
		}

	}

	public static void main(@Nonnull String[] args) throws Exception {
		final MainCache main = new MainCache(args);
		main.run();
	}

	private void run() throws RemoteException {
		System.out.println("Starting Service!");
		final Scanner scan = new Scanner(System.in);
		String line;
		do {
			line = scan.next();
			System.out.println("Service running");
		} while (!"x".equals(line));
		scan.close();
		System.out.println("Service exit.");
		scan.close();
		System.exit(0);

	}
}
