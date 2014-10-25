package ar.edu.itba.pod.mmxivii.sube;

import java.rmi.RemoteException;
import java.rmi.server.UID;
import java.util.HashMap;
import java.util.Map;

import org.jgroups.Address;
import org.jgroups.JChannel;
import org.jgroups.Message;
import org.jgroups.ReceiverAdapter;
import org.jgroups.View;
import org.joda.time.LocalDateTime;

import ar.edu.itba.pod.mmxivii.sube.pushDataMessage.SyncType;
import ar.edu.itba.pod.mmxivii.sube.common.CardRegistry;
import ar.edu.itba.pod.mmxivii.sube.common.CardService;
import ar.edu.itba.pod.mmxivii.sube.common.CardServiceRegistry;
import ar.edu.itba.pod.mmxivii.sube.common.Utils;
import ar.edu.itba.pod.mmxivii.sube.entity.OperationType;
import ar.edu.itba.pod.mmxivii.sube.entity.UserData;

public class CardServiceJGroupsImpl extends ReceiverAdapter implements
		CardService, Runnable {

	private final JChannel channel;
	private CardRegistry server;
	private CardServiceRegistry balancer;
	private Map<UID, UserData> cachedUserData;
	private boolean initialUpdate;
	private boolean registered;
	private static final int MAX_TIMEOUT_SERVER_MINUTES = 2;
	private LocalDateTime lastUpdate;
	private boolean hasClusterUpdatedServer;
	private Address pickedLeaderAddres;

	public CardServiceJGroupsImpl(JChannel channel, CardRegistry cardRegistry,
			CardServiceRegistry cardServiceRegistry) {
		this.channel = channel;
		this.server = cardRegistry;
		this.balancer = cardServiceRegistry;
		this.cachedUserData = new HashMap<UID, UserData>(); // INITIALIZES DATA
		this.registered = false;
		this.initialUpdate = false;
		this.hasClusterUpdatedServer = false;
	}

	public Address getCurrentAddress() {
		return channel.getAddress();
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

	private void applyUpdateServerOperation(pushServerUpdateMessage p,
			Address src) {
		switch (p.getOperationType()) {
		case PICK:
			this.pickedLeaderAddres = p.getLeaderAddress(); // TODO
			break;
		case UPDATED:
			this.hasClusterUpdatedServer = true; // TODO
			break;
		}

	}

	@Override
	public void suspect(Address mbr) {
		try {
			for (CardService serv : balancer.getServices()) {
				CardServiceJGroupsImpl service = (CardServiceJGroupsImpl) serv;
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
		if (v.getMembers().size() == 1) {
			if (registered == false) {
				try {
					balancer.registerService(this);
					registered = true;
					initialUpdate = true;
					System.out.println("Registered First Node");
				} catch (RemoteException e) {
				}

			}
		} else {
			sendInformationToNewNode(v);
		}
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
		while (v.getMembers().get(i) != channel.getAddress()
				&& i < v.getMembers().size()) {
			i++;
		}
		syncAddress = v.getMembers().get(i);
		return syncAddress;
	}

	private void applySyncOperation(pushDataMessage s, Address address) {
		switch (s.getOperationType()) {
		case PUSH:
			if (!initialUpdate) {
				cachedUserData.putAll(s.getCachedUserData());
				try {
					balancer.registerService(this);
					registered = true;
					initialUpdate = true;
				} catch (RemoteException e) {
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

	}

	private void applyRecharge(UID uid, double amount) {
		// NEED TO CHECK IF AMOUNT ACCOMPLISHES $ARS FORMAT
		Utils.assertAmount(amount);
		//
		cachedUserData.get(uid).addBalance(amount);
	}

	private void newUserData(UID uid, double amount) {
		// NEED TO CHECK IF AMOUNT ACCOMPLISHES $ARS FORMAT
		Utils.assertAmount(amount);
		//
		cachedUserData.put(uid, new UserData(amount));
	}

	private void applyTravel(UID uid, double amount) {
		// NEED TO CHECK IF AMOUNT ACCOMPLISHES $ARS FORMAT
		Utils.assertAmount(amount);
		//
		cachedUserData.get(uid).addBalance(-amount);
	}

	@Override
	public double getCardBalance(UID id) throws RemoteException {
		UserData uData = cachedUserData.get(id);
		if (uData != null) {
			return uData.getBalance();
		}
		uData = new UserData(server.getCardBalance(id));

		CacheRequest c = new CacheRequest(OperationType.BALANCE, id,
				uData.getBalance());
		try {
			channel.send(new Message().setObject(c));
			cachedUserData.put(id, uData);
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
			return (uData.getBalance() >= amount) ? uData.addBalance(-amount)
					: CardRegistry.OPERATION_NOT_PERMITTED_BY_BALANCE;

		} else {
			uData = new UserData(server.getCardBalance(id));
			CacheRequest c = new CacheRequest(OperationType.TRAVEL, id,
					uData.getBalance());
			try {
				channel.send(new Message().setObject(c));
				cachedUserData.put(id, uData);
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
		return uData.getBalance();

	}

	@SuppressWarnings("static-access")
	@Override
	public double recharge(UID id, String description, double amount)
			throws RemoteException {
		// NEED TO CHECK IF AMOUNT ACCOMPLISHES $ARS FORMAT
		Utils.assertAmount(amount);
		//
		UserData uData = cachedUserData.get(id);
		if (uData != null) {
			return uData.getBalance() + amount < server.MAX_BALANCE ? uData
					.addBalance(amount)
					: CardRegistry.OPERATION_NOT_PERMITTED_BY_BALANCE;
		} else {
			uData = new UserData(server.getCardBalance(id));
			CacheRequest c = new CacheRequest(OperationType.RECHARGE, id,
					uData.getBalance());
			try {
				channel.send(new Message().setObject(c));
				cachedUserData.put(id, uData);

			} catch (Exception e) {
				e.printStackTrace();
			}
		}
		return uData.getBalance();
	}

	@Override
	public void run() {
		syncronizationWithServer();
	}

	private void syncronizationWithServer() {
		while (true) {
			if (!hasClusterUpdatedServer)
				if (LocalDateTime.now()
						.minusSeconds(lastUpdate.getSecondOfMinute())
						.getSecondOfMinute() > MAX_TIMEOUT_SERVER_MINUTES) {
					updateServer();
				}

		}
	}

	private void updateServer() {
		lastUpdate = LocalDateTime.now();

	}

}
