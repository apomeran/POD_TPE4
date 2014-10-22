package ar.edu.itba.pod.mmxivii.sube;

import java.rmi.RemoteException;
import java.rmi.server.UID;
import java.util.HashMap;
import java.util.Map;

import org.jgroups.JChannel;
import org.jgroups.Message;
import org.jgroups.ReceiverAdapter;

import ar.edu.itba.pod.mmxivii.sube.common.CardRegistry;
import ar.edu.itba.pod.mmxivii.sube.common.CardService;
import ar.edu.itba.pod.mmxivii.sube.common.CardServiceRegistry;
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

			} else {
				if (msg.getObject() instanceof SyncRequest) {
				}
			}
		}
	}

	@Override
	public double getCardBalance(UID id) throws RemoteException {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public double travel(UID id, String description, double amount)
			throws RemoteException {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public double recharge(UID id, String description, double amount)
			throws RemoteException {
		// TODO Auto-generated method stub
		return 0;
	}

}
