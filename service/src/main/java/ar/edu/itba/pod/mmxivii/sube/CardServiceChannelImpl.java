package ar.edu.itba.pod.mmxivii.sube;

import java.rmi.RemoteException;
import java.rmi.server.UID;
import java.util.HashMap;
import java.util.Map;

import org.jgroups.JChannel;
import org.jgroups.Message;
import org.jgroups.ReceiverAdapter;

import com.google.common.cache.CacheStats;

import ar.edu.itba.pod.mmxivii.sube.common.CardRegistry;
import ar.edu.itba.pod.mmxivii.sube.common.CardService;
import ar.edu.itba.pod.mmxivii.sube.common.CardServiceRegistry;
import ar.edu.itba.pod.mmxivii.sube.entity.OperationType;
import ar.edu.itba.pod.mmxivii.sube.entity.UserData;

public class CardServiceChannelImpl extends ReceiverAdapter implements
		CardService {

	private final JChannel channel;
	private CardRegistry cardRegistry;
	private CardServiceRegistry cardServiceRegistry;
	private Map<UID, UserData> cachedUserData = new HashMap<UID, UserData>();

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
				applyOperation(r);

			} else {
				if (msg.getObject() instanceof SyncRequest) {
				}
			}
		}
	}

	private void applyOperation(CacheRequest r) {
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
		cachedUserData.get(uid).addBalance(amount);
	}

	private void newUserData(UID uid, double amount) {
		cachedUserData.put(uid, new UserData(amount));
	}

	private void applyTravel(UID uid, double amount) {
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

		return 0;
	}

	@SuppressWarnings("static-access")
	@Override
	public double recharge(UID id, String description, double amount)
			throws RemoteException {
		// NEED TO CHECK IF AMOUNT ACCOMPLISHES $ARS FORMAT

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

		return 0;
	}

}
