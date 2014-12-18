/*
 * 
 */
package org.commons;

import java.io.Serializable;

// TODO: Auto-generated Javadoc
/**
 * The Class BasicChainReplicationIdentifier.
 */
public class BasicChainReplicationIdentifier implements Serializable {
	
	/** The Constant serialVersionUID. */
	private static final long	serialVersionUID	= 6006670536713391576L;
	
	/** The uuid. */
	protected String			uuid				= null;
	
	/** The ip addr port. */
	protected String			ipAddrPort			= null;
	
	
	/**
	 * Instantiates a new identifier.
	 */
	public BasicChainReplicationIdentifier() {
		super();
	}
	
	/**
	 * Gets the ip addr port.
	 *
	 * @return the ip addr port
	 */
	public String getIpAddrPort() {
		return ipAddrPort;
	}
	
	/**
	 * Gets the uuid.
	 *
	 * @return the uuid
	 */
	public String getUuid() {
		return uuid;
	}
	
	/**
	 * Sets the ip addr port.
	 *
	 * @param ipAddrPort
	 *            the new ip addr port
	 */
	public void setIpAddrPort(String ipAddrPort) {
		this.ipAddrPort = ipAddrPort;
	}
	
	/**
	 * Sets the uuid.
	 *
	 * @param uuid
	 *            the new uuid
	 */
	public void setUuid(String uuid) {
		this.uuid = uuid;
	}
	
}
