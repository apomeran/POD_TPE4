package ar.edu.itba.pod.mmxivii.sube.client;

import static ar.edu.itba.pod.mmxivii.sube.common.Utils.CARD_CLIENT_BIND;
import static ar.edu.itba.pod.mmxivii.sube.common.Utils.CARD_REGISTRY_BIND;
import static com.google.common.base.Preconditions.checkArgument;

import java.rmi.NotBoundException;
import java.rmi.RemoteException;
import java.rmi.server.UID;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import javax.annotation.Nonnull;

import ar.edu.itba.pod.mmxivii.sube.common.BaseMain;
import ar.edu.itba.pod.mmxivii.sube.common.Card;
import ar.edu.itba.pod.mmxivii.sube.common.CardClient;
import ar.edu.itba.pod.mmxivii.sube.common.CardRegistry;
import ar.edu.itba.pod.mmxivii.sube.common.Utils;

public class MainClient_2 extends BaseMain {

	public static void main(@Nonnull String[] args) throws Exception {
		final MainClient_2 main = new MainClient_2(args);
		main.run();
	}

	private CardClient cardClient = null;
	private CardRegistry server = null;

	private MainClient_2(@Nonnull String[] args) throws NotBoundException {
		super(args, DEFAULT_CLIENT_OPTIONS);
		getRegistry();
		cardClient = Utils.lookupObject(CARD_CLIENT_BIND);
		server = Utils.lookupObject(CARD_REGISTRY_BIND);
	}

	private void run() throws RemoteException {
		System.out.println("consumerRecharger INFINIT TEST");
		try {
			consumerRecharger();
		} catch (InterruptedException e) {
			System.out.println("Se pudrio todo");
			e.printStackTrace();
		}
		// basicFunctionalityTest();
		// errorTest();
		// tonsOfData();
	}

	private void consumerRecharger() throws RemoteException,
			InterruptedException {
		String cardName = randomCardId();
		Card card = cardClient.newCard(cardName, "");
		UID cardId = card.getId();
		float currentBalance = 90;
		final float travelCost = 10;
		double finalBalance = 0;
		int i = 1;
		while (true) {
			finalBalance = cardClient.recharge(cardId, "initialize",
					currentBalance);
			System.out.println("Recargado, saldo actual " + finalBalance);

			cardClient.travel(cardId, "travel", travelCost);
			cardClient.travel(cardId, "travel", travelCost);
			cardClient.travel(cardId, "travel", travelCost);
			cardClient.travel(cardId, "travel", travelCost);
			cardClient.travel(cardId, "travel", travelCost);
			cardClient.travel(cardId, "travel", travelCost);
			cardClient.travel(cardId, "travel", travelCost);
			cardClient.travel(cardId, "travel", travelCost);
			finalBalance = cardClient.travel(cardId, "travel", travelCost);
			System.out.println("Ciclo " + i + " Saldo Actual " + finalBalance);
			i++;
			Thread.sleep(3000);
		}
	}

	private void basicFunctionalityTest() throws RemoteException {
		String cardName = randomCardId();
		Card card = cardClient.newCard(cardName, "");
		UID cardId = card.getId();
		float currentBalance = 50;
		final float travelCost = 5;
		double rechargeStatus = cardClient.recharge(cardId, "initialize",
				currentBalance);
		checkArgument(rechargeStatus > 0);
		for (int i = 0; i < 5; i++) {
			double reportedBalance = cardClient.getCardBalance(cardId);
			checkArgument((int) reportedBalance == (int) currentBalance);
			cardClient.travel(cardId, "bus" + i, travelCost);
			currentBalance -= travelCost;
		}
		try {
			Thread.sleep(5000);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
		double finalBalance = server.getCardBalance(cardId);
		checkArgument((int) finalBalance == (int) currentBalance);
		System.out
				.println("Cache numbers are ok and the data is in the server.");
	}

	private void errorTest() throws RemoteException {
		String cardName = randomCardId();
		Card card = cardClient.newCard(cardName, "");
		UID cardId = card.getId();
		cardClient.recharge(cardId, "initialize", 50);
		try {
			Thread.sleep(1000);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
		double status = cardClient.travel(cardId, "bus", 51);
		checkArgument((int) status == -4);
		System.out.println("insufficient money working");
		status = server.getCardBalance(new UID());
		checkArgument((int) status == -1);
	}

	private void tonsOfData() throws RemoteException {
		new Thread(new ManyTravelingMFS(100)).start();
		new Thread(new ManyTravelingMFS(100)).start();
		new Thread(new ManyTravelingMFS(100)).start();
	}

	private String randomCardId() {
		return UUID.randomUUID().toString().replaceAll("-", "");
	}

	private final class ManyTravelingMFS implements Runnable {

		private final int userAmount;

		public ManyTravelingMFS(int userAmount) {
			this.userAmount = userAmount;
		}

		@Override
		public void run() {
			try {
				List<UID> cardIds = generateCards();
				System.out.println("Users Registered");
				int j = 0;
				float amount = 10;
				double status = -5;
				while (j < 50) {
					int operationsAmount = randomInt(100, 200);
					UID cardId = cardIds.get(randomInt(0, cardIds.size()));
					System.out.println(operationsAmount
							+ "will be made to card: " + cardId);
					for (int i = 0; i < operationsAmount; i++) {
						status = cardClient.travel(cardId, "Bus", amount);
						if (status < 0) {
							status = cardClient.recharge(cardId, "Recharge",
									amount * randomInt(5, 10));
						}
					}
					checkArgument((int) status != -5);
					System.out.println("the expected result of card: " + cardId
							+ " is: " + status);
					Thread.sleep(500);
					j++;
				}
			} catch (Exception e) {
				e.printStackTrace();
				System.err.println("Se pudrio todo!");
			}
		}

		private int randomInt(int min, int max) {
			return (int) (Math.random() * (max - min) + min);
		}

		private List<UID> generateCards() throws RemoteException {
			List<UID> result = new ArrayList<UID>(userAmount);
			System.out.println("Registering users....");
			for (int i = 0; i < userAmount; i++) {
				String cardName = randomCardId();
				Card card = cardClient.newCard(cardName, "");
				result.add(card.getId());
			}
			return result;
		}
	}
}
