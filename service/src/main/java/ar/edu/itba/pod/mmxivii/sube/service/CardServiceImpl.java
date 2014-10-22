package ar.edu.itba.pod.mmxivii.sube.service;

import java.rmi.RemoteException;
import java.rmi.server.UID;
import java.rmi.server.UnicastRemoteObject;

import javax.annotation.Nonnull;

import ar.edu.itba.pod.mmxivii.sube.common.CardRegistry;
import ar.edu.itba.pod.mmxivii.sube.common.CardService;

public class CardServiceImpl extends UnicastRemoteObject implements CardService {

	private static final long serialVersionUID = 2919260533266908792L;

	@Nonnull
	private CardRegistry cardRegistry;
	private CardService override; // THIS SHOULD BE REPLACED

	public CardServiceImpl(@Nonnull CardRegistry cardRegistry,
			CardService override) throws RemoteException {
		super(0);
		this.override = override;

	}

	@Override
	public double getCardBalance(@Nonnull UID id) throws RemoteException {
		return override.getCardBalance(id);
	}

	@Override
	public double travel(@Nonnull UID id, @Nonnull String description,
			double amount) throws RemoteException {
		return override.travel(id, description, amount);
	}

	@Override
	public double recharge(@Nonnull UID id, @Nonnull String description,
			double amount) throws RemoteException {
		return override.recharge(id, description, amount);
	}
}
