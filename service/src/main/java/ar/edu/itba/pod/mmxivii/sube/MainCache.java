package ar.edu.itba.pod.mmxivii.sube;

import static ar.edu.itba.pod.mmxivii.sube.common.Utils.CARD_REGISTRY_BIND;
import static ar.edu.itba.pod.mmxivii.sube.common.Utils.CARD_SERVICE_REGISTRY_BIND;

import java.lang.reflect.InvocationTargetException;
import java.rmi.NotBoundException;
import java.rmi.RemoteException;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
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
	private CardRegistry server;
	private CardServiceRegistry balancer;
	private static final String CLUSTER_NAME = "cluster";
	private static List<JChannel> channels = new LinkedList<JChannel>();
	private static int cacheAmount;

	private MainCache(@Nonnull String[] args) throws RemoteException,
			NotBoundException {
		super(args, DEFAULT_CLIENT_OPTIONS);
		System.setProperty("java.net.preferIPv4Stack", "true");
		getRegistry();
		server = Utils.lookupObject(CARD_REGISTRY_BIND);
		balancer = Utils.lookupObject(CARD_SERVICE_REGISTRY_BIND);

		int nodesCount = 2;
		cacheAmount = 0;
		while (cacheAmount < nodesCount) {
			try {
				createNode("node_n" + cacheAmount, CLUSTER_NAME);
				Thread.sleep(TimeUnit.SECONDS.toMillis(3));
			} catch (InvocationTargetException e) {

			} catch (Exception e) {
				e.printStackTrace();
			}
			cacheAmount++;
		}

	}

	private void createNode(String nodeName, String clusterName)
			throws Exception {
		JChannel channel = new JChannel();
		channel.setName(nodeName);
		boolean firstNode = false;
		if (cacheAmount == 0)
			firstNode = true;
		CardServiceReceiver cardService = new CardServiceReceiver(channel,
				server, balancer, firstNode);
		channel.setReceiver((Receiver) cardService);
		channel.connect(clusterName);
		Thread.sleep(1200);
		channels.add(channel);
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
			if(line.equals("add")) {
				try {
					createNode("node_n" + cacheAmount, CLUSTER_NAME);
					Thread.sleep(TimeUnit.SECONDS.toMillis(3));
				} catch (InvocationTargetException e) {

				} catch (Exception e) {
					e.printStackTrace();
				}
				cacheAmount++;
			} else if(line.equals("remove")) {
				int randomChannel = (int)Math.random() * channels.size();
				JChannel c = channels.get(randomChannel);
				c.close();
				System.out.println("Closed channel number " + randomChannel);
				balancer.unRegisterService((CardService)balancer.getServices().toArray()[randomChannel]);
				channels.remove(randomChannel);
			}
		} while (!"x".equals(line));
		scan.close();
		System.out.println("Service exit.");
		System.exit(0);
	}
}
