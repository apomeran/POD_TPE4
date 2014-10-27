package ar.edu.itba.pod.mmxivii.sube;

import java.io.Serializable;
import java.rmi.RemoteException;
import java.rmi.server.UID;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.jgroups.Address;
import org.jgroups.JChannel;
import org.jgroups.Message;
import org.jgroups.ReceiverAdapter;
import org.jgroups.View;

import ar.edu.itba.pod.mmxivii.sube.pushDataMessage.SyncType;
import ar.edu.itba.pod.mmxivii.sube.common.CardRegistry;
import ar.edu.itba.pod.mmxivii.sube.common.CardService;
import ar.edu.itba.pod.mmxivii.sube.common.CardServiceRegistry;
import ar.edu.itba.pod.mmxivii.sube.common.Utils;
import ar.edu.itba.pod.mmxivii.sube.entity.Operation;
import ar.edu.itba.pod.mmxivii.sube.entity.OperationType;
import ar.edu.itba.pod.mmxivii.sube.entity.UserData;

public class CardServiceReceiver extends ReceiverAdapter implements
		CardService, Serializable {
	private Map<UID, UserData> cachedUserData;
	private Map<UID, UserData> myCachedUserData;
	private CardRegistry server;
	private CardServiceRegistry balancer;
	private final JChannel channel;
	private boolean initialUpdate = false;
	private boolean registered = false;
	private boolean hasClusterUpdatedServer;
	private ExecutorService executor = Executors.newFixedThreadPool(4);

	public CardServiceReceiver(JChannel channel, CardRegistry server,
			CardServiceRegistry balancer, boolean isFirstNode) {
		this.channel = channel;
		this.server = server;
		this.balancer = balancer;
		this.cachedUserData = new HashMap<UID, UserData>();
		this.myCachedUserData = new HashMap<UID, UserData>();
		if (isFirstNode) {
			if (registered == false) {
				try {
					CardServiceImpl cardService = new CardServiceImpl(this);
					balancer.registerService(cardService);
					registered = true;
					initialUpdate = true;
					System.out.println("Registered First Node");
				} catch (RemoteException e) {
				}

			}
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
	public void suspect(Address mbr) {
		try {
			for (CardService serv : balancer.getServices()) {
				CardServiceReceiver service = (CardServiceReceiver) serv;
				if (service.getCurrentAddress().equals(mbr)) {
					balancer.getServices().remove(service);
					return;
				}
			}
		} catch (RemoteException e) {
			e.printStackTrace();
		}
	}

	@Override
	public void viewAccepted(View v) {
		if (v.getMembers().size() > 1)
			sendInformationToNewNode(v);
	}

	private void sendInformationToNewNode(View v) {
		Address syncAddress = getNodeAddress(v);
		if (syncAddress != null)
			try {
				Message syncMessage = new Message()
						.setObject(new pushDataMessage(cachedUserData,
								SyncType.PUSH));
				channel.send(syncMessage);
			} catch (Exception e) {
				e.printStackTrace();
			}
	}

	private Address getNodeAddress(View v) {
		int i = 0;
		Address syncAddress = null;
		while (i < v.getMembers().size()
				&& v.getMembers().get(i) != channel.getAddress()) {
			i++;
		}
		syncAddress = v.getMembers().get(i - 1);
		return syncAddress;
	}

	private void applyUpdateServerOperation(pushServerUpdateMessage p,
			Address src) {
		switch (p.getOperationType()) {
		case PICK:
			// pickedLeaderAddres = p.getLeaderAddress(); // TODO
			break;
		case UPDATED:
			hasClusterUpdatedServer = true; // TODO
			break;
		}

	}

	private void applySyncOperation(pushDataMessage s, Address address) {
		switch (s.getOperationType()) {
		case PUSH:
			if (!initialUpdate) {
				cachedUserData.putAll(s.getCachedUserData());
				try {
					CardServiceImpl cardService = new CardServiceImpl(this);
					if (registered == false)
						balancer.registerService(cardService);
					registered = true;
					initialUpdate = true;
				} catch (Exception e) {
					registered = false;
					initialUpdate = false;
					e.printStackTrace();
				}
			}
			break;
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
		executor.execute( new Runnable() {			
			public void run() {
				downloadDataToServer();
			};
		});

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
		myCachedUserData.put(uid, new UserData(amount));
	}

	private void downloadDataToServer() {
		System.out.println("Updating Server");
		for (UID uid : myCachedUserData.keySet()) {
			for (Operation operation : myCachedUserData.get(uid)
					.getOperations()) {
				try {
					if (!operation.isAlreadyUpdatedInServer()) {
						server.addCardOperation(uid, operation.getType()
								.toString(), operation.getAmount());
						operation.setUpdated();
					}
				} catch (RemoteException e) {
					e.printStackTrace();
				}
			}
		}
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
			// System.out.println("Server Reply CardBalance: "
			// + uData.getBalance());
			return uData.getBalance();
		} catch (Exception e) {
		}
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
			// System.out.println("Cached Travel Reply");

			return (uData.getBalance() >= amount) ? uData.addBalance(-amount)
					: CardRegistry.OPERATION_NOT_PERMITTED_BY_BALANCE;

		} else {
			uData = new UserData(server.getCardBalance(id));
		}
		CacheRequest c = new CacheRequest(OperationType.TRAVEL, id,
				uData.getBalance());
		try {
			channel.send(new Message().setObject(c));
			cachedUserData.put(id, uData);
		} catch (Exception e) {
			e.printStackTrace();
		}
		// System.out.println("Server Travel Reply");
		return uData.getBalance();
	}

	@Override
	public double recharge(UID id, String description, double amount)
			throws RemoteException {
		// NEED TO CHECK IF AMOUNT ACCOMPLISHES $ARS FORMAT
		Utils.assertAmount(amount);
		//
		UserData uData = cachedUserData.get(id);
		if (uData != null) {
			System.out.println("Cached Recharged Reply Nº" + id);
			return uData.getBalance() + amount <= server.MAX_BALANCE ? uData
					.addBalance(amount)
					: CardRegistry.OPERATION_NOT_PERMITTED_BY_BALANCE;
		} else {
			uData = new UserData(server.getCardBalance(id));
		}
		CacheRequest c = new CacheRequest(OperationType.RECHARGE, id,
				uData.getBalance());
		uData.addBalance(amount);
		try {
			channel.send(new Message().setObject(c));
			cachedUserData.put(id, uData);

		} catch (Exception e) {
			e.printStackTrace();
		}
		// System.out.println("Server Recharge, UserData Nº" + id);

		return uData.getBalance();
	}

}
