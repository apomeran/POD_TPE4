package ar.edu.itba.pod.mmxivii.sube;

import org.jgroups.JChannel;
import org.jgroups.ReceiverAdapter;

import ar.edu.itba.pod.mmxivii.sube.common.CardRegistry;

public class Synchronizer extends ReceiverAdapter {

	private CardRegistry server;
	private JChannel node;

	public Synchronizer(JChannel synchronizationNode, CardRegistry server) {
		this.server = server;
		this.node = synchronizationNode;
	}

}
