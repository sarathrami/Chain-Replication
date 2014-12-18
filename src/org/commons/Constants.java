/*
 * 
 */
package org.commons;

// TODO: Auto-generated Javadoc
/**
 * The Interface Constants.
 */
public interface Constants {
	
	/** The Constant IP_SEPARATOR. */
	static final String	IP_SEPARATOR			= "#";
	
	/** The Constant CONNECTION_STRING. */
	/*static final String	CONNECTION_STRING		= "failover://(%s://%s)?wireFormat.maxInactivityDuration=30000&initialReconnectDelay=7&maxReconnectAttempts=" + Integer.MAX_VALUE
														+ "&jms.prefetchPolicy.queuePrefetch=1";*/
	static final String	CONNECTION_STRING		= "%s://%s?wireFormat.maxInactivityDuration=0&jms.prefetchPolicy.queuePrefetch=1";
	
	/** The Constant QUEUE. */
	static final String	QUEUE					= "Q.%s.%s";
	
	/** The Constant RETRANSMISSION_DELAY. */
	static final int	RETRANSMISSION_DELAY	= Integer.parseInt(System.getProperty("org.commons.BasicChainReplicationServer.retransmissionDelay", "10000"));
}
