package ar.edu.itba.pod.mmxivii.sube;

import java.io.Serializable;
import java.rmi.server.UID;
import java.util.HashMap;
import java.util.Map;

import ar.edu.itba.pod.mmxivii.sube.entity.UserData;

public class pushDataMessage implements Serializable {
	private Map<UID, UserData> cachedUserData = new HashMap<UID, UserData>();

	public pushDataMessage(Map<UID, UserData> cachedUserData) {
		this.cachedUserData = cachedUserData;
	}

	private static final long serialVersionUID = 1L;

	public Map<UID, UserData> getCachedUserData() {
		return cachedUserData;
	}

}
