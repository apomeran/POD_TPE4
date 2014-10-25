package ar.edu.itba.pod.mmxivii.sube;

import static ar.edu.itba.pod.mmxivii.sube.common.Utils.CARD_REGISTRY_BIND;
import static ar.edu.itba.pod.mmxivii.sube.common.Utils.CARD_SERVICE_REGISTRY_BIND;

import java.rmi.NotBoundException;
import java.rmi.RemoteException;
import java.util.Scanner;
import java.util.concurrent.TimeUnit;

import javax.annotation.Nonnull;

import org.jgroups.JChannel;
import org.jgroups.Receiver;

import ar.edu.itba.pod.mmxivii.sube.common.BaseMain;
import ar.edu.itba.pod.mmxivii.sube.common.CardRegistry;
import ar.edu.itba.pod.mmxivii.sube.common.CardService;
import ar.edu.itba.pod.mmxivii.sube.common.CardServiceRegistry;
import ar.edu.itba.pod.mmxivii.sube.common.Utils;

public class MainCache extends BaseMain {
	private CardServiceRegistry balancer;
	private CardRegistry server;

	private MainCache(@Nonnull String[] args) throws RemoteException,
			NotBoundException {
		super(args, DEFAULT_CLIENT_OPTIONS);
		getRegistry();
		server = Utils.lookupObject(CARD_REGISTRY_BIND);
		balancer = Utils.lookupObject(CARD_SERVICE_REGISTRY_BIND);
		String clusterName = "JGroupsNodeCluster";
		int nodesCount = 1;
		while (nodesCount > 0) {
			try {
				createNode("node_n" + nodesCount, clusterName);
				Thread.sleep(TimeUnit.SECONDS.toMillis(5));
			} catch (Exception e) {
				e.printStackTrace();
			}
			nodesCount--;
		}

	}

	private void createNode(String nodeName, String clusterName)
			throws Exception {
		JChannel node = new JChannel();
		node.setName(nodeName);
		CardService cardService = new CardServiceJGroupsImpl(node, server,
				balancer);
		node.setReceiver((Receiver) cardService);
		node.connect(clusterName);
		// balancer.registerService(myCardService);
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
		System.exit(0);
	}
}
