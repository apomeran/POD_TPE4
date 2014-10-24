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

import ar.edu.itba.pod.mmxivii.sube.SyncRequest.SyncType;
import ar.edu.itba.pod.mmxivii.sube.common.CardRegistry;
import ar.edu.itba.pod.mmxivii.sube.common.CardService;
import ar.edu.itba.pod.mmxivii.sube.common.CardServiceRegistry;
import ar.edu.itba.pod.mmxivii.sube.common.Utils;
import ar.edu.itba.pod.mmxivii.sube.entity.OperationType;
import ar.edu.itba.pod.mmxivii.sube.entity.UserData;
import ar.edu.itba.pod.mmxivii.sube.service.CardServiceImpl;

public class CardServiceChannelImpl extends ReceiverAdapter implements
		CardService {

	private final JChannel channel;
	private CardRegistry cardRegistry;
	private CardServiceRegistry cardServiceRegistry;
	private Map<UID, UserData> cachedUserData = new HashMap<UID, UserData>();
	private boolean synchronized_node = false;

	public CardServiceChannelImpl(JChannel channel, CardRegistry cardRegistry,
			CardServiceRegistry cardServiceRegistry) {
		this.channel = channel;
		this.cardRegistry = cardRegistry;
		this.cardServiceRegistry = cardServiceRegistry;
	}

	public void receive(Message msg) {
		if (!msg.getSrc().equals(channel.getAddress())) {
			if (msg.getObject() instanceof CacheRequest) {
				CacheRequest r = (CacheRequest) msg.getObject();
				applyCacheOperation(r);
			} else {
				if (msg.getObject() instanceof SyncRequest) {
					SyncRequest s = (SyncRequest) msg.getObject();
					applySyncOperation(s, msg.getSrc());
				}
			}
		}
	}

	@Override
	public void viewAccepted(View v) {
		if (v.getMembers().size() == 1) {
			if (synchronized_node == false) {
				try {
					cardServiceRegistry.registerService(new CardServiceImpl(
							cardRegistry, this));
					System.out.println("DADO DE ALTA FRENTE AL BALANCER");
				} catch (RemoteException e) {
				}
				synchronized_node = true;
			}
		} else {
			int i = 0;
			Address syncAddress = null;
			while (v.getMembers().get(i) == channel.getAddress()) {
				i++;
			}
			syncAddress = v.getMembers().get(i);
			if (syncAddress != null)
				try {
					channel.send(new Message().setObject(new SyncRequest(
							cachedUserData, SyncType.REQUEST)));
				} catch (Exception e) {
					e.printStackTrace();
				}
		}
	}

	private void applySyncOperation(SyncRequest s, Address address) {
		switch (s.getOperationType()) {
		case REQUEST:
			if (synchronized_node) {
				try {
					channel.send(address, new Message()
							.setObject(new SyncRequest(cachedUserData,
									SyncType.RESPONSE)));
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
			break;
		case RESPONSE:
			if (!synchronized_node) {
				cachedUserData.putAll(s.getCachedUserData());
				synchronized_node = true;
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
		uData = new UserData(cardRegistry.getCardBalance(id));

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
			if (uData.getBalance() >= amount)
				uData.addBalance(-amount);
			else
				return CardRegistry.OPERATION_NOT_PERMITTED_BY_BALANCE;
		} else {
			uData = new UserData(cardRegistry.getCardBalance(id));
			CacheRequest c = new CacheRequest(OperationType.TRAVEL, id,
					uData.getBalance());
			try {
				channel.send(new Message().setObject(c));
				cachedUserData.put(id, uData);
				return uData.getBalance();
			} catch (Exception e) {
			}
		}

		return -1; // SHOULD NOT HAPPEN-
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
			if (uData.getBalance() + amount < cardRegistry.MAX_BALANCE)
				uData.addBalance(amount);
			else
				return CardRegistry.OPERATION_NOT_PERMITTED_BY_BALANCE;
		} else {
			uData = new UserData(cardRegistry.getCardBalance(id));
			CacheRequest c = new CacheRequest(OperationType.RECHARGE, id,
					uData.getBalance());
			try {
				channel.send(new Message().setObject(c));
				cachedUserData.put(id, uData);
				return uData.getBalance();
			} catch (Exception e) {
			}
		}

		return -1; // SHOULD NOT HAPPEN-
	}

}
