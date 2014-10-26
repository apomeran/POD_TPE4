package ar.edu.itba.pod.mmxivii.sube;

import java.rmi.RemoteException;
import java.rmi.server.UID;
import java.util.HashMap;
import java.util.Map;

import org.jgroups.Address;
import org.jgroups.JChannel;
import org.jgroups.Message;
import org.jgroups.ReceiverAdapter;

import ar.edu.itba.pod.mmxivii.sube.common.CardRegistry;
import ar.edu.itba.pod.mmxivii.sube.common.CardService;
import ar.edu.itba.pod.mmxivii.sube.common.CardServiceRegistry;
import ar.edu.itba.pod.mmxivii.sube.common.Utils;
import ar.edu.itba.pod.mmxivii.sube.entity.OperationType;
import ar.edu.itba.pod.mmxivii.sube.entity.UserData;

public class CardServiceReceiver extends ReceiverAdapter implements CardService{
	private Map<UID, UserData> cachedUserData;
	private CardRegistry server;
	private CardServiceRegistry balancer;
	private final JChannel channel;
	private boolean initialUpdate;
	private boolean registered;
	private boolean hasClusterUpdatedServer;
	
	public CardServiceReceiver(JChannel channel, CardRegistry server) {
		this.channel = channel;
		this.server = server;
		this.cachedUserData = new HashMap<UID, UserData>();
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
			//pickedLeaderAddres = p.getLeaderAddress(); // TODO
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
			System.out.println("CardBalance ACK UserData Nº" + id);

			return uData.getBalance();
		}
		uData = new UserData(server.getCardBalance(id));

		CacheRequest c = new CacheRequest(OperationType.BALANCE, id,
				uData.getBalance());
		try {
			channel.send(new Message().setObject(c));
			cachedUserData.put(id, uData);
			System.out
					.println("CardBalance ACK via Server, now cached UserData Nº"
							+ id);
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
			System.out.println("Travel ACK UserData Nº" + id);

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
		System.out
				.println("Travel ACK via Server, now cached UserData Nº" + id);
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
			System.out.println("Recharged UserData Nº" + id);
			return uData.getBalance() + amount < server.MAX_BALANCE ? uData
					.addBalance(amount)
					: CardRegistry.OPERATION_NOT_PERMITTED_BY_BALANCE;
		} else {
			uData = new UserData(server.getCardBalance(id));
		}
		CacheRequest c = new CacheRequest(OperationType.RECHARGE, id,
				uData.getBalance());
		try {
			channel.send(new Message().setObject(c));
			cachedUserData.put(id, uData);

		} catch (Exception e) {
			e.printStackTrace();
		}
		System.out.println("Recharged via Server, now cached UserData Nº" + id);

		return uData.getBalance();
	}

}
