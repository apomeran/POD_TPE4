package ar.edu.itba.pod.mmxivii.sube;

import java.rmi.RemoteException;
import java.rmi.server.UID;

import org.jgroups.JChannel;
import org.jgroups.ReceiverAdapter;

import ar.edu.itba.pod.mmxivii.sube.common.CardRegistry;
import ar.edu.itba.pod.mmxivii.sube.common.CardService;
import ar.edu.itba.pod.mmxivii.sube.common.CardServiceRegistry;

public class CardServiceChannelImpl extends ReceiverAdapter implements CardService {

	public CardServiceChannelImpl(JChannel node, CardRegistry cardRegistry,
			CardServiceRegistry cardServiceRegistry) {
		// TODO Auto-generated constructor stub
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
