package ar.edu.itba.pod.mmxivii.sube.entity;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

public class UserData implements Serializable {

	private static final long serialVersionUID = 1L;
	private double balance;
	private List<Operation> operations = new ArrayList<Operation>();

	public UserData() {
	}

	public UserData(double balance) {
		this.balance = balance;
	}

	public double getBalance() {
		return balance;
	}

	public void addBalance(double amount) {
		this.balance = this.balance + amount;
	}

	public void charge(double amount) {
		synchronized (operations) {
			operations.add(new Operation(OperationType.RECHARGE, amount));
			addBalance(amount);
		}
	}

	public void travel(double amount) {
		synchronized (operations) {
			operations.add(new Operation(OperationType.TRAVEL, amount));
			addBalance(-amount);
		}
	}

}
