/**
 * 
 */
package org.commons;

import java.io.Serializable;
import java.util.Arrays;

// TODO: Auto-generated Javadoc
/**
 * The Class BasicChainReplicationMessage.
 *
 * @author rami
 */
public class BasicChainReplicationMessage extends BasicChainReplicationIdentifier
		implements
			Serializable,
			Comparable<BasicChainReplicationMessage> {
	
	/** The Constant serialVersionUID. */
	private static final long	serialVersionUID		= 1585635233138251805L;
	
	/** The local message id counter. */
	private static long			localMessageIDCounter	= 1;
	
	/** The local message id. */
	private long				localMessageID			= 0;
	
	/** The request_response. */
	private String[]			request_response		= null;
	
	/** The destination queue name. */
	private String				destinationQueueName	= null;
	
	/**
	 * Instantiates a new basic chain replication message.
	 */
	@SuppressWarnings("unused")
	private BasicChainReplicationMessage() {
	}
	
	/**
	 * Instantiates a new basic chain replication message.
	 *
	 * @param msg the msg
	 */
	public BasicChainReplicationMessage(BasicChainReplicationMessage msg) {
		super();
		this.request_response = new String[msg.getRequest().length];
		System.arraycopy(msg.getRequest(), 0, this.request_response, 0, this.request_response.length);
		this.uuid = msg.getUuid();
		this.ipAddrPort = msg.getIpAddrPort();
		this.localMessageID = msg.getLocalMessageID();
		this.destinationQueueName = msg.getDestinationQueueName();
	}
	
	/**
	 * Instantiates a new basic chain replication message.
	 *
	 * @param request
	 *            the request
	 * @param uuid
	 *            the uuid
	 * @param destinationQueueName
	 *            the destination queue name
	 * @param ipAddrPort
	 *            the ip addr port
	 */
	public BasicChainReplicationMessage(String[] request, String uuid, String destinationQueueName, String ipAddrPort) {
		super();
		this.request_response = request;
		this.uuid = uuid;
		this.ipAddrPort = ipAddrPort;
		this.localMessageID = BasicChainReplicationMessage.localMessageIDCounter++;
		this.destinationQueueName = destinationQueueName;
	}
	
	/*
	 * (non-Javadoc)
	 * 
	 * @see java.lang.Comparable#compareTo(java.lang.Object)
	 */
	@Override
	public int compareTo(BasicChainReplicationMessage o) {
		//return (int) (this.getLocalMessageID() - o.getLocalMessageID());
		return this.getUuid().compareTo(o.getUuid());
	}
	
	/**
	 * Gets the destination queue name.
	 *
	 * @return the destination queue name
	 */
	public String getDestinationQueueName() {
		return destinationQueueName;
	}
	
	/**
	 * Gets the local message id.
	 *
	 * @return the local message id
	 */
	public long getLocalMessageID() {
		return localMessageID;
	}
	
	/**
	 * Gets the request.
	 *
	 * @return the request
	 */
	public String[] getRequest() {
		return request_response;
	}
	
	/**
	 * Sets the destination queue name.
	 *
	 * @param destinationQueueName
	 *            the new destination queue name
	 */
	public void setDestinationQueueName(String destinationQueueName) {
		this.destinationQueueName = destinationQueueName;
	}
	
	/**
	 * Sets the request.
	 *
	 * @param request
	 *            the new request
	 */
	public void setRequest(String[] request) {
		this.request_response = request;
	}
	
	/*
	 * (non-Javadoc)
	 * 
	 * @see java.lang.Object#toString()
	 */
	@Override
	public String toString() {
		return "[localMessageID=" + localMessageID + ", request_response=" + Arrays.toString(request_response)
				+ ", destinationQueueName=" + destinationQueueName + ", uuid=" + uuid + ", ipAddrPort=" + ipAddrPort
				+ "]";
	}
	
	/* (non-Javadoc)
	 * @see java.lang.Object#equals(java.lang.Object)
	 */
	@Override
	public boolean equals(Object obj) {
		if (obj instanceof BasicChainReplicationMessage) {
			BasicChainReplicationMessage cobj = (BasicChainReplicationMessage) obj;
			/*return Arrays.asList(request_response).toString()
					.equalsIgnoreCase(Arrays.asList(cobj.getRequest()).toString())
					&& this.getUuid().equalsIgnoreCase(cobj.getUuid());*/
			return this.getUuid().equalsIgnoreCase(cobj.getUuid());
		} else {
			return super.equals(obj);
		}
	}
}
