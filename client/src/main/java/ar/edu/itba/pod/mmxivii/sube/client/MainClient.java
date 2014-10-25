package ar.edu.itba.pod.mmxivii.sube.client;

import static ar.edu.itba.pod.mmxivii.sube.common.Utils.CARD_CLIENT_BIND;

import java.rmi.NotBoundException;
import java.rmi.RemoteException;
import java.util.UUID;

import javax.annotation.Nonnull;

import ar.edu.itba.pod.mmxivii.sube.common.BaseMain;
import ar.edu.itba.pod.mmxivii.sube.common.Card;
import ar.edu.itba.pod.mmxivii.sube.common.CardClient;
import ar.edu.itba.pod.mmxivii.sube.common.Utils;

public class MainClient extends BaseMain {
	private CardClient cardClient = null;

	private MainClient(@Nonnull String[] args) throws NotBoundException {
		super(args, DEFAULT_CLIENT_OPTIONS);
		getRegistry();
		cardClient = Utils.lookupObject(CARD_CLIENT_BIND);
	}

	public static void main(@Nonnull String[] args) throws Exception {
		final MainClient main = new MainClient(args);
		main.run();
	}

	private void run() throws RemoteException {
		System.out.println("Main.run");
		final Card card = cardClient.newCard("alumno", "tarjeta");
		final double primero = cardClient
				.recharge(card.getId(), "primero", 100);
		System.out.println("primero = " + primero);
		final double bondi = cardClient.travel(card.getId(), "bondi", 3);
		System.out.println("bondi = " + bondi);
		// cardClient.newCard()
	}

	public Card cardGenerator() {
		try {
			return cardClient.newCard(UUID.randomUUID().toString(), UUID
					.randomUUID().toString());
		} catch (RemoteException e) {
			e.printStackTrace();
		}
		return null;
	}
}
