/*
 * 
 */
package org.commons;

import javax.jms.Session;

// TODO: Auto-generated Javadoc
/**
 * The Enum CommType.
 */
public enum CommType {
	
	/** The comm unreliable. */
	COMM_UNRELIABLE(Session.AUTO_ACKNOWLEDGE), 
 /** The comm reliable. */
 COMM_RELIABLE(Session.CLIENT_ACKNOWLEDGE);
	
	/** The value. */
	private int	value	= 0;
	
	/**
	 * Instantiates a new comm type.
	 *
	 * @param value the value
	 */
	CommType(int value) {
		this.value = value;
	}
	
	/**
	 * Gets the value.
	 *
	 * @return the value
	 */
	public int getValue() {
		return value;
	}
}
