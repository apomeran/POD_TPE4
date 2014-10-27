package ar.edu.itba.pod.mmxivii.sube.entity;

import java.util.Date;

public class Operation implements Comparable<Operation> {

	private OperationType type;
	private double amount;
	private boolean updatedToServer = false;
	private Date timestamp;

	public Operation(OperationType opType, double amount) {
		this.type = opType;
		this.amount = amount;
		this.timestamp = new Date();

	}

	@Override
	public int compareTo(Operation otherOperation) {
		return timestamp.compareTo(otherOperation.timestamp);
	}

	public OperationType getType() {
		return type;
	}

	public void setType(OperationType type) {
		this.type = type;
	}

	public double getAmount() {
		return amount;
	}

	public void setAmount(double amount) {
		this.amount = amount;
	}

	public Date getTimestamp() {
		return timestamp;
	}

	public void setTimestamp(Date timestamp) {
		this.timestamp = timestamp;
	}

	public void setUpdated() {
		this.updatedToServer = true;
	}

	public boolean isAlreadyUpdatedInServer() {
		return this.updatedToServer;
	}

}
