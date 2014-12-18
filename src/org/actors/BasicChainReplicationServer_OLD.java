/*
 * 
 */
package org.actors;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;
import java.util.concurrent.Callable;

import javax.jms.JMSException;

import org.apache.log4j.Logger;
import org.commons.BasicChainReplicationIdentifier;
import org.commons.BasicChainReplicationMessage;
import org.commons.Comm;
import org.commons.CommType;
import org.commons.Constants;
import org.commons.IComm;
import org.commons.ServerState;
import org.utilities.CUtils;

// TODO: Auto-generated Javadoc
/**
 * The Class BasicChainReplicationServer_OLD.
 */
@SuppressWarnings("unused")
public abstract class BasicChainReplicationServer_OLD extends BasicChainReplicationIdentifier implements Constants, Comparable<BasicChainReplicationServer_OLD> {
	
	/** The Constant serialVersionUID. */
	private static final long					serialVersionUID	= 7044201853029488887L;
	
	/** The next server. */
	private BasicChainReplicationServer_OLD			nextServer			= null;
	
	/** The prev server. */
	private BasicChainReplicationServer_OLD			prevServer			= null;
	
	/** The server list. */
	private List<BasicChainReplicationServer_OLD>	serverList			= new ArrayList<BasicChainReplicationServer_OLD>();
	
	/** The mycomm. */
	private IComm								mycomm				= null;
	
	/** The Constant LOG. */
	private static final Logger					LOG					= Logger.getLogger(BasicChainReplicationServer_OLD.class);
	
	/** The hist. */
	protected Set<BasicChainReplicationMessage>	hist				= Collections.synchronizedSet(new TreeSet<BasicChainReplicationMessage>());
	/** The sent buffer. */
	protected Set<BasicChainReplicationMessage>	sent				= Collections.synchronizedSet(new TreeSet<BasicChainReplicationMessage>());
	
	/** The state. */
	protected ServerState						state				= ServerState.UNINITIALIZED;
	
	/** The Constant SERVER_RETRY_COUNT. */
	private static final int					SERVER_RETRY_COUNT	= Integer.parseInt(System.getProperty("org.actors.BasicChainReplicationServer_OLD.serverRetryCount", "1"));
	
	/**
	 * Gets the state.
	 *
	 * @return the state
	 */
	public ServerState getState() {
		return state;
	}
	
	/**
	 * Sets the state.
	 *
	 * @param state the new state
	 */
	public void setState(ServerState state) {
		this.state = state;
	}
	
	/**
	 * Instantiates a new basic chain replication server.
	 *
	 * @param uuid
	 *            the uuid
	 * @param myIPAddrPort
	 *            the my ip addr port
	 * @param serverList
	 *            the server list
	 */
	protected BasicChainReplicationServer_OLD(String uuid, String myIPAddrPort, List<BasicChainReplicationServer_OLD> serverList) {
		super();
		this.uuid = uuid;
		this.ipAddrPort = myIPAddrPort;
		if (null != serverList)
			this.serverList = new ArrayList<BasicChainReplicationServer_OLD>(serverList);
	}
	
	/**
	 * Die.
	 */
	public void die() {
		try {
			if (null != mycomm)
				mycomm.close();
			LOG.info(String.format("[%s]\t:\tShutting down...", this));
			System.exit(0);
		} catch (Exception e) {
		}
	}
	
	/**
	 * Gets the comm.
	 *
	 * @return the comm
	 */
	protected IComm getComm() {
		return mycomm;
	}
	
	/*
	 * (non-Javadoc)
	 * 
	 * @see org.actors.BasicChainReplicationIdentifier#getIpAddrPort()
	 */
	@Override
	public String getIpAddrPort() {
		return ipAddrPort;
	}
	
	/**
	 * Gets the next server.
	 *
	 * @return the next server
	 */
	public BasicChainReplicationServer_OLD getNextServer() {
		synchronized (serverList) {
			for (int i = 0; i < serverList.size() - 1; i++)
				if (serverList.get(i).getUuid().equalsIgnoreCase(BasicChainReplicationServer_OLD.this.getUuid())) {
					return serverList.get(i + 1);
				}
		}
		return null;
	}
	
	/**
	 * Gets the prev server.
	 *
	 * @return the prev server
	 */
	public BasicChainReplicationServer_OLD getPrevServer() {
		synchronized (serverList) {
			for (int i = 1; i < serverList.size(); i++)
				if (serverList.get(i).getUuid().equalsIgnoreCase(BasicChainReplicationServer_OLD.this.getUuid())) {
					return serverList.get(i - 1);
				}
		}
		return null;
	}
	
	/**
	 * Gets the server list.
	 *
	 * @return the server list
	 */
	public List<BasicChainReplicationServer_OLD> getServerList() {
		return serverList;
	}
	
	/*
	 * (non-Javadoc)
	 * 
	 * @see org.actors.BasicChainReplicationIdentifier#getUuid()
	 */
	@Override
	public String getUuid() {
		return uuid;
	}
	
	/**
	 * Checks if is head.
	 *
	 * @return true, if is head
	 */
	protected boolean isHead() {
		return null == getPrevServer();
	}
	
	/**
	 * Checks if is tail.
	 *
	 * @return true, if is tail
	 */
	protected boolean isTail() {
		return null == getNextServer();
	}
	
	/**
	 * Update history.
	 *
	 * @param receivedMsg
	 *            the received msg
	 * @return true, if successful
	 */
	protected abstract boolean updateHistory(BasicChainReplicationMessage receivedMsg);
	/**
	 * Post process message.
	 *
	 * @param receivedMsg
	 *            the received msg
	 * @return the basic chain replication message
	 */
	protected abstract BasicChainReplicationMessage postProcessMessage(BasicChainReplicationMessage receivedMsg);
	
	/**
	 * Pre process message.
	 *
	 * @param receivedMsg
	 *            the received msg
	 * @return the basic chain replication message
	 */
	protected abstract BasicChainReplicationMessage preProcessMessage(BasicChainReplicationMessage receivedMsg);
	
	/**
	 * Sets the server list.
	 *
	 * @param serverList
	 *            the new server list
	 */
	public void setServerList(List<BasicChainReplicationServer_OLD> serverList) {
		synchronized (this.serverList) {
			if (null != serverList)
				this.serverList = Collections.synchronizedList(new ArrayList<BasicChainReplicationServer_OLD>(serverList));
		}
	}
	
	/**
	 * Start.
	 */
	public void start() {
		Callable<Integer> startListeningAndRelayThread = new Callable<Integer>() {
			@Override
			public Integer call() {
				while (true) {
					
					try {
						LOG.info(String.format("[%s]\t:\tListening...", BasicChainReplicationServer_OLD.this));
						// Close previous comm
						if (null != mycomm)
							mycomm.close();
						// Check if is head
						if (isHead())
							mycomm = new Comm(uuid, ipAddrPort, CommType.COMM_UNRELIABLE);
						else
							mycomm = new Comm(uuid, ipAddrPort);
						
						// receive from client/ prev
						Serializable receivedObjMsg = mycomm.receiveMsg(0);
						
						if (null == receivedObjMsg || !(receivedObjMsg instanceof BasicChainReplicationMessage)) {
							LOG.info(String.format("[%s]\t:\tDiscarding Invalid message [%s]", BasicChainReplicationServer_OLD.this, receivedObjMsg));
							continue;
						}
						
						final BasicChainReplicationMessage receivedMsg = (BasicChainReplicationMessage) receivedObjMsg;
						final String msg = receivedMsg.toString();
						// Process the message
						LOG.info(String.format("[%s]\t:\tProcessing [%s]...", BasicChainReplicationServer_OLD.this, msg));
						final BasicChainReplicationMessage toTransmitMsg = BasicChainReplicationServer_OLD.this.preProcessMessage(receivedMsg);
						
						// Callable<Integer> startRelayAndAckThread = new
						// Callable<Integer>() {
						// @Override
						// public Integer call() {
						// Send to the next guy
						boolean successful_in_relay = false;
						int serverRetryCount = SERVER_RETRY_COUNT;
						while (!successful_in_relay && serverRetryCount-- > 0) {
							IComm nxtcomm = null;
							try {
								// synchronized (serverList) {
								// Calculate next guy to send msg to
								// and
								if (isTail()) {
									// Reply to BasicChainReplicationClient
									nxtcomm = new Comm(toTransmitMsg.getDestinationQueueName(), toTransmitMsg.getIpAddrPort(), CommType.COMM_UNRELIABLE);
								} else {
									nextServer = getNextServer();
									nxtcomm = new Comm(nextServer.getUuid(), nextServer.getIpAddrPort());
								}
								LOG.info(String.format("[%s]\t:\tSending [%s] to [%s]", BasicChainReplicationServer_OLD.this, toTransmitMsg.toString(), nxtcomm));
								
								LOG.info(String.format("[%s]\t:\tPost-Processing [%s]...", BasicChainReplicationServer_OLD.this, msg));
								final BasicChainReplicationMessage sentAndPostProcessedMsg = postProcessMessage(toTransmitMsg);
								
								if (isTail()) {
									if (updateHistory(receivedMsg)) {
										LOG.info(String.format("[%s]\t:\tUpdating {H} for [%s]", BasicChainReplicationServer_OLD.this, receivedMsg));
										hist.add(receivedMsg);
										sent.remove(receivedMsg);
									}
									LOG.info(String.format("[%s]\t:\tSending [%s] to [%s]!", BasicChainReplicationServer_OLD.this, sentAndPostProcessedMsg, nxtcomm));
									nxtcomm.sendMsg(sentAndPostProcessedMsg);
									LOG.info(String.format("[%s]\t:\tSending Ack for [%s]!", BasicChainReplicationServer_OLD.this, receivedMsg));
									mycomm.sendAck(receivedMsg);
								} else {
									nxtcomm.sendMsg(sentAndPostProcessedMsg);
								}
								
								sent.add(receivedMsg);
								LOG.info(String.format("[%s]\t:\tSent [%s] to [%s]", BasicChainReplicationServer_OLD.this, sentAndPostProcessedMsg, nextServer));
								if (!isTail()) {
									LOG.info(String.format("[%s]\t:\tWaiting for Ack for [%s] from [%s]...", BasicChainReplicationServer_OLD.this, sentAndPostProcessedMsg, nxtcomm));
									nxtcomm.waitForAck(sentAndPostProcessedMsg, RETRANSMISSION_DELAY);
									LOG.info(String.format("[%s]\t:\tReceived Ack for [%s] from [%s]", BasicChainReplicationServer_OLD.this, sentAndPostProcessedMsg, nextServer));
								}
								
								successful_in_relay = true;
								Thread.yield();
								// }
							} catch (JMSException e) {
								LOG.error(String.format("[%s]\t:\tJMSException [%s]. Retrying...", BasicChainReplicationServer_OLD.this, e.getMessage()));
							} catch (Exception e) {
								e.printStackTrace();
							} finally {
								try {
									nxtcomm.close();
									Thread.yield();
								} catch (JMSException e) {
									e.printStackTrace();
									System.exit(-1);
								}
							}
						}
						
						if (!successful_in_relay)
							continue;
						
						// Tails has already added to Hist by this point
						if (!isTail()) {
							if (updateHistory(receivedMsg)) {
								LOG.info(String.format("[%s]\t:\tUpdating {H} for [%s]", BasicChainReplicationServer_OLD.this, receivedMsg));
								hist.add(receivedMsg);
								sent.remove(receivedMsg);
							}
							if (!isHead()) {
								LOG.info(String.format("[%s]\t:\tSending Ack for [%s]!", BasicChainReplicationServer_OLD.this, receivedMsg));
								mycomm.sendAck(receivedMsg);
							}
						}
						// return 0;
						// }
						// };
						//
						// CUtils.work(startRelayAndAckThread);
					} catch (Exception e) {
						e.printStackTrace();
						// return -1;
					} finally {
						/*
						 * try {
						 * mycomm.close();
						 * } catch (JMSException e) {
						 * e.printStackTrace();
						 * }
						 */
					}
				}
			}
		};
		CUtils.work(startListeningAndRelayThread);
	}
	
	/**
	 * Adds the server.
	 *
	 * @param srv
	 *            the srv
	 */
	public void addServer(BasicChainReplicationServer_OLD srv) {
		synchronized (serverList) {
			serverList.add(srv);
		}
	}
	
	/**
	 * Rem server.
	 *
	 * @param srv
	 *            the srv
	 */
	public void remServer(BasicChainReplicationServer_OLD srv) {
		synchronized (serverList) {
			serverList.remove(srv);
		}
	}
	
	/**
	 * Reset.
	 */
	public void reset() {
		hist.clear();
		sent.clear();
		synchronized (serverList) {
			serverList.clear();
			serverList.add(this);
		}
	}
	/*
	 * (non-Javadoc)
	 * 
	 * @see java.lang.Object#toString()
	 */
	@Override
	public String toString() {
		return "BasicChainReplicationServer_OLD(@" + ipAddrPort + " - " + uuid + ")";
	}
	
	/*
	 * (non-Javadoc)
	 * 
	 * @see java.lang.Comparable#compareTo(java.lang.Object)
	 */
	@Override
	public int compareTo(BasicChainReplicationServer_OLD comparedWith) {
		return this.getUuid().compareTo(comparedWith.getUuid());
	}
	
	/* (non-Javadoc)
	 * @see java.lang.Object#equals(java.lang.Object)
	 */
	@Override
	public boolean equals(Object obj) {
		if (null != obj && obj instanceof BasicChainReplicationServer_OLD)
			return 0 == this.compareTo((BasicChainReplicationServer_OLD) obj);
		return false;
	}
}
