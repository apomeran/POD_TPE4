package ar.edu.itba.pod.mmxivii.sube.entity;

import java.io.Serializable;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;

public class UserData implements Serializable {

	private static final long serialVersionUID = 1L;
	private double balance;
	private SortedSet<Operation> operations = new TreeSet<Operation>();

	public UserData() {
	}
	
	public Set<Operation> getOperations(){
		return operations;
	}
	public UserData(double balance) {
		this.balance = balance;
	}

	public double getBalance() {
		return balance;
	}

	public double addBalance(double amount) {
		this.balance = this.balance + amount;
		return balance;
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
