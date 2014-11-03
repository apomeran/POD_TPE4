package ar.edu.itba.pod.mmxivii.sube;

import java.io.Serializable;
import java.rmi.RemoteException;
import java.rmi.server.UID;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.jgroups.Address;
import org.jgroups.JChannel;
import org.jgroups.Message;
import org.jgroups.ReceiverAdapter;
import org.jgroups.View;

import ar.edu.itba.pod.mmxivii.sube.common.CardRegistry;
import ar.edu.itba.pod.mmxivii.sube.common.CardService;
import ar.edu.itba.pod.mmxivii.sube.common.CardServiceRegistry;
import ar.edu.itba.pod.mmxivii.sube.common.Utils;
import ar.edu.itba.pod.mmxivii.sube.entity.Operation;
import ar.edu.itba.pod.mmxivii.sube.entity.OperationType;
import ar.edu.itba.pod.mmxivii.sube.entity.UserData;

public class CardServiceReceiver extends ReceiverAdapter implements
		CardService, Serializable {
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	private Map<UID, UserData> cachedUserData;
	private Map<UID, UserData> myUsersCachedUserData;
	private CardRegistry server;
	private CardServiceRegistry balancer;
	private final JChannel channel;
	private boolean initialUpdate;
	private boolean registered;
	private ExecutorService executor = Executors.newSingleThreadExecutor();
	private String nodeName;

	public CardServiceReceiver(JChannel channel, CardRegistry server,
			CardServiceRegistry balancer, boolean isFirstNode, String name) {
		this.channel = channel;
		this.server = server;
		this.balancer = balancer;
		this.nodeName = name;
		this.cachedUserData = new ConcurrentHashMap<UID, UserData>();
		this.myUsersCachedUserData = new ConcurrentHashMap<UID, UserData>();
		if (isFirstNode) {
			if (registered == false) {
				try {
					CardServiceImpl cardService = new CardServiceImpl(
							channel.getAddress(), this);
					balancer.registerService(cardService);
					registered = true;
					initialUpdate = true;
					System.out.println("Registered First Node." + nodeName);
					System.out.println("Size of Nodes "
							+ balancer.getServices().size());
				} catch (RemoteException e) {
				}

			}
		} else {
			registered = false;
			initialUpdate = false;
		}

	}

	public void receive(Message msg) {
		if (!msg.getSrc().equals(channel.getAddress())) {
			if (msg.getObject() instanceof CacheRequest) {
				CacheRequest r = (CacheRequest) msg.getObject();
				applyCacheOperation(r);
			} else {
				if (msg.getObject() instanceof pushDataMessage) {
					pushDataMessage s = (pushDataMessage) msg.getObject();
					applySyncOperation(s, msg.getSrc());
				} else {
					if (msg.getObject() instanceof pushServerUpdateMessage) {
						pushServerUpdateMessage p = (pushServerUpdateMessage) msg
								.getObject();
						applyUpdateServerOperation(p, msg.getSrc());
					}
				}
			}
		}
	}

	public Address getCurrentAddress() {
		return channel.getAddress();
	}

	@Override
	public void viewAccepted(View v) {
		if (v.getMembers().size() > 1) {
			sendInformationToNewNode(v);
		}
	}

	private void sendInformationToNewNode(View v) {
		try {

			// I DO THIS NEW MAP UID-USERDATA BECAUSE OPERATION IS NOT
			// SERIALIZABLE AND THROWS
			// EXCEPTION WHEN SENDING TO OTHER NODES
			// IM JUST CLEARING OPERATIONS
			Map<UID, UserData> newInfoToNewNode = new HashMap<UID, UserData>();
			for (UID uid : newInfoToNewNode.keySet()) {
				newInfoToNewNode.put(uid, new UserData(newInfoToNewNode
						.get(uid).getBalance()));
			}
			Message syncMessage = new Message().setObject(new pushDataMessage(
					newInfoToNewNode));
			System.out.println("Sent Info to all from " + nodeName);
			Thread.sleep((long) (Math.random() * 300));
			channel.send(syncMessage);
			// int i = 0;
			// while (i < v.getMembers().size()) {
			// Address nodeAddress = v.getMembers().get(i);
			// if (nodeAddress != channel.getAddress()) {
			// channel.send(nodeAddress, syncMessage);
			// System.out.println("Sent Info to Node " + i + " FROM "
			// + nodeName);
			// }
			// i++;
			// }
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	private void applyUpdateServerOperation(pushServerUpdateMessage p,
			Address src) {

	}

	private void applySyncOperation(pushDataMessage s, Address address) {
		synchronized (this) {
			System.out.println("Soy " + nodeName + " y me llego");
			if (!initialUpdate) {
				cachedUserData = (s.getCachedUserData());
				try {

					if (registered == false) {
						CardServiceImpl cardService = new CardServiceImpl(
								channel.getAddress(), this);
						System.out.println("About to register myself");
						balancer.registerService(cardService);
						Thread.sleep(1500);
						System.out.println("Registered myself");
						System.out.println("Size of Nodes "
								+ balancer.getServices().size());
						registered = true;
						initialUpdate = true;
					}

				} catch (Exception e) {
					registered = false;
					initialUpdate = false;
					e.printStackTrace();
				}
			}
			System.out.println("Fin del me llego");
		}
	}

	private void applyCacheOperation(CacheRequest r) {
		switch (r.getOperationType()) {
		case TRAVEL:
			applyTravel(r.getUid(), r.getBalance());
			break;
		case BALANCE:
			newUserData(r.getUid(), r.getBalance());
			break;
		case RECHARGE:
			applyRecharge(r.getUid(), r.getBalance());
			break;
		}

	}

	private void applyRecharge(UID uid, double amount) {
		// NEED TO CHECK IF AMOUNT ACCOMPLISHES $ARS FORMAT
		Utils.assertAmount(amount);
		//
		System.out.println("Propagated Recharge");
		UserData u = cachedUserData.get(uid);
		if (u == null) {
			newUserData(uid, amount);
		}
		cachedUserData.get(uid).addBalance(amount);
	}

	private void newUserData(UID uid, double amount) {
		// NEED TO CHECK IF AMOUNT ACCOMPLISHES $ARS FORMAT
		Utils.assertAmount(amount);
		//
		System.out.println("Holding information for " + cachedUserData.size()
				+ " users in each Node");

		cachedUserData.put(uid, new UserData(amount));
		myUsersCachedUserData.put(uid, new UserData(amount));
	}

	private void downloadDataToServer() {
		System.out.println("Updating Server");
		for (UID uid : myUsersCachedUserData.keySet()) {
			for (Operation operation : myUsersCachedUserData.get(uid)
					.getOperations()) {
				try {
					if (!operation.isAlreadyUpdatedInServer()) {
						System.out.println("Operation " + uid + " Type:"
								+ operation.getType().toString() + " Amount: $"
								+ operation.getAmount());
						server.addCardOperation(uid, operation.getType()
								.toString(), operation.getAmount());
						operation.setUpdated();
					}
				} catch (RemoteException e) {
					e.printStackTrace();
				}
			}
		}
		System.out.println("********* END UPDATE SERVER ***********");
	}

	private void applyTravel(UID uid, double amount) {
		// NEED TO CHECK IF AMOUNT ACCOMPLISHES $ARS FORMAT
		Utils.assertAmount(amount);
		//
		System.out.println("Propagated Travel");
		UserData u = cachedUserData.get(uid);
		if (u == null) {
			newUserData(uid, amount);
		}
		cachedUserData.get(uid).addBalance(-amount);
	}

	@Override
	public double getCardBalance(UID id) throws RemoteException {
		UserData uData = cachedUserData.get(id);
		if (uData != null) {
			// System.out.println("Cached Reply CardBalance: "
			// + uData.getBalance());

			return uData.getBalance();
		}
		uData = new UserData(server.getCardBalance(id));

		CacheRequest c = new CacheRequest(OperationType.BALANCE, id,
				uData.getBalance());
		try {
			channel.send(new Message().setObject(c));
			cachedUserData.put(id, uData);
			myUsersCachedUserData.put(id, uData);

			// System.out.println("Server Reply CardBalance: "
			// + uData.getBalance());
			return uData.getBalance();
		} catch (Exception e) {
		}
		downloadDataToServer();
		return -1; // SHOULD NOT HAPPEN-
	}

	@Override
	public double travel(UID id, String description, double amount)
			throws RemoteException {
		// NEED TO CHECK IF AMOUNT ACCOMPLISHES $ARS FORMAT
		Utils.assertAmount(amount);
		//
		UserData uData = cachedUserData.get(id);
		if (uData != null) {
			System.out.println("Cached Travel");
			if (uData.getBalance() < amount)
				return CardRegistry.OPERATION_NOT_PERMITTED_BY_BALANCE;

		} else {
			System.out.println("Asking Server Travel");
			uData = new UserData(server.getCardBalance(id));
		}
		CacheRequest c = new CacheRequest(OperationType.TRAVEL, id,
				uData.getBalance());
		uData.travel(amount);
		try {
			channel.send(new Message().setObject(c));
			cachedUserData.put(id, uData);
			myUsersCachedUserData.put(id, uData);

		} catch (Exception e) {
			e.printStackTrace();
		}
		System.out.println("Travel, UserData Nº" + id);
		System.out.println("**********************");
		downloadDataToServer();
		return uData.getBalance();
	}

	@Override
	public double recharge(UID id, String description, double amount)
			throws RemoteException {
		// NEED TO CHECK IF AMOUNT ACCOMPLISHES $ARS FORMAT
		Utils.assertAmount(amount);
		UserData uData = cachedUserData.get(id);
		if (uData != null) {
			System.out.println("Cached Recharge");
			if (uData.getBalance() + amount > server.MAX_BALANCE)
				return CardRegistry.OPERATION_NOT_PERMITTED_BY_BALANCE;
		} else {
			System.out.println("Asking Server Recharge");
			uData = new UserData(server.getCardBalance(id));
		}
		CacheRequest c = new CacheRequest(OperationType.RECHARGE, id,
				uData.getBalance());
		uData.charge(amount);
		try {
			channel.send(new Message().setObject(c));
			cachedUserData.put(id, uData);
			myUsersCachedUserData.put(id, uData);

		} catch (Exception e) {
			e.printStackTrace();
		}
		downloadDataToServer();
		System.out.println("Recharge, UserData Nº" + id);
		System.out.println("**********************");
		return uData.getBalance();
	}

}
