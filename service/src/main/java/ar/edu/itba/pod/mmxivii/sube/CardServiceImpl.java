package ar.edu.itba.pod.mmxivii.sube;

import java.rmi.RemoteException;
import java.rmi.server.UID;
import java.rmi.server.UnicastRemoteObject;

import ar.edu.itba.pod.mmxivii.sube.common.CardService;

@SuppressWarnings("serial")
public class CardServiceImpl extends UnicastRemoteObject implements
		CardService {

	private CardService slave;

	public CardServiceImpl(CardService slave) throws RemoteException{
			super();
			this.slave = slave;			
	}

	@Override
	public double getCardBalance(UID id) throws RemoteException {
		return slave.getCardBalance(id);
	}

	@Override
	public double travel(UID id, String description, double amount)
			throws RemoteException {
		return slave.travel(id, description, amount);
	}

	@Override
	public double recharge(UID id, String description, double amount)
			throws RemoteException {
		return slave.recharge(id, description, amount);
	}

}
