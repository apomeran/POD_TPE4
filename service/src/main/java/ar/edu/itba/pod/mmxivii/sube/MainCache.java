package ar.edu.itba.pod.mmxivii.sube;

import static ar.edu.itba.pod.mmxivii.sube.common.Utils.CARD_REGISTRY_BIND;

import java.lang.reflect.InvocationTargetException;
import java.rmi.NotBoundException;
import java.rmi.RemoteException;
import java.util.Scanner;
import java.util.concurrent.TimeUnit;

import javax.annotation.Nonnull;

import org.jgroups.JChannel;
import org.jgroups.Receiver;

import ar.edu.itba.pod.mmxivii.sube.common.BaseMain;
import ar.edu.itba.pod.mmxivii.sube.common.CardRegistry;
import ar.edu.itba.pod.mmxivii.sube.common.Utils;

public class MainCache extends BaseMain {
	private CardRegistry server;

	private MainCache(@Nonnull String[] args) throws RemoteException,
			NotBoundException {
		super(args, DEFAULT_CLIENT_OPTIONS);
		getRegistry();
		server = Utils.lookupObject(CARD_REGISTRY_BIND);
		String clusterName = "cluster";
		int nodesCount = 2;
		int i = 0;
		while (i < nodesCount) {
			try {
				createNode("node_n" + i, clusterName, i);
				Thread.sleep(TimeUnit.SECONDS.toMillis(3));
			} catch (InvocationTargetException e) {

			} catch (Exception e) {
				e.printStackTrace();
			}
			i++;
		}

	}

	private void createNode(String nodeName, String clusterName, int nodeCount)
			throws Exception {
		JChannel channel = new JChannel();
		channel.setName(nodeName);
		boolean firstNode = false;
		if (nodeCount == 0)
			firstNode = true;
		CardServiceReceiver cardService = new CardServiceReceiver(channel,
				server, firstNode);
		channel.setReceiver((Receiver) cardService);
		channel.connect(clusterName);
		Thread.sleep(3200);
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
