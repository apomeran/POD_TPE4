package ar.edu.itba.pod.mmxivii.sube;

import org.jgroups.Address;

public class pushServerUpdateMessage {

	private Address selectedNode;
	
	public static enum SyncType {
		PICK, UPDATED
	}

	private SyncType type;

	public SyncType getOperationType() {
		return type;
	}

	public Address getLeaderAddress() {
		return selectedNode;
	};
}
