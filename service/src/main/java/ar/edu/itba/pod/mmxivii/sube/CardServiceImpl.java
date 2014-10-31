package ar.edu.itba.pod.mmxivii.sube;

import java.rmi.RemoteException;
import java.rmi.server.UID;
import java.rmi.server.UnicastRemoteObject;

import org.jgroups.Address;

import ar.edu.itba.pod.mmxivii.sube.common.CardService;

@SuppressWarnings("serial")
public class CardServiceImpl extends UnicastRemoteObject implements CardService {

	private CardService slave;
	private Address address;

	public CardServiceImpl(Address address, CardService slave)
			throws RemoteException {
		super();
		this.slave = slave;
		this.address = address;
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

	public CardServiceImpl getService() {
		return (CardServiceImpl) slave;
	}

	public Address getCurrentAddress() {
		return address;
	}

}
