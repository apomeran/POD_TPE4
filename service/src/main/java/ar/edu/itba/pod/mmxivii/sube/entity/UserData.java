package ar.edu.itba.pod.mmxivii.sube.entity;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

public class UserData implements Serializable {

	/**
	 * 
	 */
	private static final long serialVersionUID = -8256962059169870528L;

	private double balance;
	private List<Operation> operations = new ArrayList<Operation>();

}
