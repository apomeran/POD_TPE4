package ar.edu.itba.pod.mmxivii.sube.balancer;

import java.rmi.RemoteException;
import java.rmi.server.UID;
import java.rmi.server.UnicastRemoteObject;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import javax.annotation.Nonnull;

import ar.edu.itba.pod.mmxivii.sube.common.CardService;
import ar.edu.itba.pod.mmxivii.sube.common.CardServiceRegistry;

public class CardServiceRegistryImpl extends UnicastRemoteObject implements
		CardServiceRegistry {
	private static final long serialVersionUID = 2473638728674152366L;
	private final List<CardService> serviceList = Collections
			.synchronizedList(new ArrayList<CardService>());
	private final List<UID> registeredUIDs = Collections
			.synchronizedList(new ArrayList<UID>());

	protected CardServiceRegistryImpl() throws RemoteException {
	}

	@SuppressWarnings("unused")
	private int randomInt(int min, int max) {
		return (int) (Math.random() * (max - min) + min);
	}

	@Override
	public void registerService(@Nonnull CardService service)
			throws RemoteException {
		if(!serviceList.contains(service))
		serviceList.add(service);
	}

	@Override
	public void unRegisterService(@Nonnull CardService service)
			throws RemoteException {
		serviceList.remove(service);
	}

	@Override
	public Collection<CardService> getServices() throws RemoteException {
		return serviceList;
	}

	CardService getCardService(UID id) {
		int offset = 0;
		if (!registeredUIDs.contains(id)) {
			registeredUIDs.add(id);
		}
		offset = registeredUIDs.indexOf(id);
		System.out.println(serviceList.size());
		int selectedNode = offset % serviceList.size(); // EQUITY
		return serviceList.get(0);
	}
}
