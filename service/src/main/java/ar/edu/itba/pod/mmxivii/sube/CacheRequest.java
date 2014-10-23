package ar.edu.itba.pod.mmxivii.sube;

import java.io.Serializable;
import java.rmi.server.UID;

import ar.edu.itba.pod.mmxivii.sube.entity.OperationType;

public class CacheRequest implements Serializable {
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	private OperationType operationType;
	private UID uid;
	private double balance;

	public CacheRequest(OperationType operationType, UID uid, double balance) {
		this.operationType = operationType;
		this.uid = uid;
		this.balance = balance;
	}

	public OperationType getOperationType() {
		return operationType;
	}

	public void setOperationType(OperationType operationType) {
		this.operationType = operationType;
	}

	public UID getUid() {
		return uid;
	}

	public void setUid(UID uid) {
		this.uid = uid;
	}

	public double getBalance() {
		return balance;
	}

	public void setBalance(double balance) {
		this.balance = balance;
	}
}
