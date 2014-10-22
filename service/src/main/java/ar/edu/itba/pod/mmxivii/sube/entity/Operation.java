package ar.edu.itba.pod.mmxivii.sube.entity;

import java.io.Serializable;
import java.util.Date;

public class Operation implements Comparable<Operation>, Serializable {

	/**
	 * 
	 */
	private static final long serialVersionUID = 7742645122840050032L;

	private OperationType type;
	private double amount;
	private Date timestamp;

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

}
