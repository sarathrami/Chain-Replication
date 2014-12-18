/*
 * 
 */
package com.bank;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.TreeSet;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.Callable;
import java.util.concurrent.LinkedBlockingQueue;

import javax.jms.JMSException;

import org.actors.BasicChainReplicationServer;
import org.apache.log4j.Logger;
import org.commons.BasicChainReplicationMessage;
import org.commons.Constants;
import org.commons.IComm;
import org.commons.ServerState;
import org.commons.customComm;
import org.utilities.CUtils;

// TODO: Auto-generated Javadoc
/**
 * The Class BankMaster.
 */
public class XBankMaster implements Constants {
	
	/** The alive msging period. */
	private static long												ALIVE_MSGING_PERIOD		= Long.parseLong(System.getProperty("com.bank.BankServer.aliveMsgPeriod", ""
																									+ Long.MAX_VALUE));
	
	/** The grace period multiplier. */
	private static long												GRACE_PERIOD_MULTIPLIER	= Math.abs(Long.parseLong(System.getProperty(
																									"com.bank.BankServer.gracePeriodMultiplier", "" + 1)));
	
	/** The log. */
	private final static Logger										LOG						= Logger.getLogger(XBankMaster.class);
	
	/** The name. */
	private final static String										name					= "MASTER";
	
	/** The me. */
	private static IComm											me						= null;
	
	/** The bank map. */
	private static Map<String, List<BasicChainReplicationServer>>	bankMap					= Collections.synchronizedMap(new HashMap<String, List<BasicChainReplicationServer>>());
	
	/** The last checked for pulse map. */
	/** The last checked for pulse map. */
	private static Map<String, BasicChainReplicationMessage>		lastCheckedForPulseMap	= Collections.synchronizedMap(new HashMap<String, BasicChainReplicationMessage>());
	
	/** The recvd msg list. */
	private static BlockingQueue<BasicChainReplicationMessage>		recvdMsgList			= new LinkedBlockingQueue<BasicChainReplicationMessage>();
	/*
	 * (non-Javadoc)
	 * 
	 * @see java.lang.Object#toString()
	 */
	@Override
	public String toString() {
		return "BankMaster [name=" + name + "]";
	}
	
	// SENDER FROM MASTER FN()
	/**
	 * Send from master msg to tgt srvr.
	 *
	 * @param tgtSrvr the tgt srvr
	 * @param req the req
	 * @throws JMSException the JMS exception
	 */
	private static void sendFromMasterMsgToTgtSrvr(XBankServer tgtSrvr, String[] req) throws JMSException {
		if("SYNC".equalsIgnoreCase(req[0])){
			boolean a = true;
		}
		IComm comm = new customComm(tgtSrvr.getUuid() + "_FROM_MASTER_Q", tgtSrvr.getIpAddrPort());
		BasicChainReplicationMessage msg = new BasicChainReplicationMessage(req, "" + System.currentTimeMillis(), "RECONFIG", tgtSrvr.getIpAddrPort());
		try {
			comm.sendMsg(msg);
		} catch (JMSException e) {
			LOG.error(e.getMessage());
		}
		LOG.info(String.format("[%s] Sent to server [%s] MSG=[%s]", name, tgtSrvr.getUuid(), msg));
	}
	
	/**
	 * The main method.
	 *
	 * @param args the arguments
	 * @throws FileNotFoundException the file not found exception
	 * @throws IOException Signals that an I/O exception has occurred.
	 */
	public static void main(String args[]) throws FileNotFoundException, IOException {
		
		// Load master configuration
		Properties p = new Properties(System.getProperties());
		p.load(new FileInputStream(new File(args[0])));
		System.setProperties(p);
		ALIVE_MSGING_PERIOD = Long.parseLong(System.getProperty("com.bank.BankServer.aliveMsgPeriod", "" + Long.MAX_VALUE));
		GRACE_PERIOD_MULTIPLIER = Math.abs(Long.parseLong(System.getProperty("com.bank.BankServer.gracePeriodMultiplier", "" + 1)));
		
		String masterQName = name;
		try {
			me = new customComm(masterQName, "localhost:61616");
		} catch (JMSException e) {
			e.printStackTrace();
		}
		
		Callable<Integer> recvMsgThread = new Callable<Integer>() {
			@Override
			public Integer call() {
				while (true) {
					try {
						LOG.info(String.format("[%s] Listening...", name));
						BasicChainReplicationMessage msg = (BasicChainReplicationMessage) me.receiveMsg(0);
						LOG.info(String.format("[%s] Received %s", name, msg));
						
						synchronized (lastCheckedForPulseMap) {
							if ("ALIVE".equalsIgnoreCase(msg.getRequest()[0])) {
								lastCheckedForPulseMap.put(msg.getRequest()[2], msg);
							}
						}
						recvdMsgList.add(msg);
					} catch (Exception e) {
						e.printStackTrace();
					}
				}
			}
		};
		
		Callable<Integer> processingIAmAliveMsgThread = new Callable<Integer>() {
			@Override
			public Integer call() {
				while (true) {
					try {
						BasicChainReplicationMessage msg = recvdMsgList.take();
						LOG.info(String.format("[%s] Received %s", name, msg));
						String[] request = msg.getRequest();
						XBankServer tgtSrvr = new XBankServer(request[1], request[2], request[3], null);
						tgtSrvr.setState(ServerState.RUNNABLE);
						
						List<BasicChainReplicationServer> listOfThisBanksServers = bankMap.get(tgtSrvr.getBankName());
						listOfThisBanksServers = null == listOfThisBanksServers
								? Collections.synchronizedList(new ArrayList<BasicChainReplicationServer>())
								: listOfThisBanksServers;
						tgtSrvr.setServerList(listOfThisBanksServers);
						
						if ("HT".equalsIgnoreCase(request[0])) {
							String[] response = new String[]{"BAD_REQUEST"};
							IComm comm = new customComm(tgtSrvr.getUuid() + "_FROM_MASTER_Q", tgtSrvr.getIpAddrPort());
							try {
								BankServer head = (BankServer) listOfThisBanksServers.get(0);
								BankServer tail = (BankServer) listOfThisBanksServers.get(listOfThisBanksServers.size() - 1);
								response = new String[]{"HT", head.getBankName(), head.getUuid(), head.getIpAddrPort(), 
										tail.getBankName(), tail.getUuid(), tail.getIpAddrPort()};
							} catch (Exception e) {
							}
							BasicChainReplicationMessage head_tail_msg = new BasicChainReplicationMessage(response, "" + System.currentTimeMillis(), "RECONFIG",
									tgtSrvr.getIpAddrPort());
							comm.sendMsg(head_tail_msg);
						} else if ("ALIVE".equalsIgnoreCase(request[0])) {
							
							if (null == listOfThisBanksServers || !listOfThisBanksServers.contains(tgtSrvr)) {
								
								// Tail already exists
								if (!listOfThisBanksServers.isEmpty()) {
									XBankServer oldTail = (XBankServer) listOfThisBanksServers.get(listOfThisBanksServers.size() - 1);
									LOG.info(String.format("[%s] Informing server %s to SYNC %s", name, oldTail.getUuid(), tgtSrvr.getUuid()));
									sendFromMasterMsgToTgtSrvr(oldTail, new String[]{"SYNC", tgtSrvr.getBankName(), tgtSrvr.getUuid(), tgtSrvr.getIpAddrPort(), request[5]});
								} else {
									// Brand new chain. Activate server
									LOG.info(String.format("[%s] Activating new server %s", name, tgtSrvr.getUuid()));
									sendFromMasterMsgToTgtSrvr(tgtSrvr, new String[]{"ACTIVATE", tgtSrvr.getBankName(), tgtSrvr.getUuid(), tgtSrvr.getIpAddrPort()});
									listOfThisBanksServers.add(tgtSrvr);
									bankMap.put(tgtSrvr.getBankName(), listOfThisBanksServers);
								}
							} else if (ServerState.UNINITIALIZED.toString().equalsIgnoreCase(request[4])) {
								// Server recently back from the dead before
								// being reaped
								LOG.info(String.format("[%s] Re-syncing server %s", name, tgtSrvr.getUuid()));
								XBankServer prev = (XBankServer) tgtSrvr.getPrevServer();
								if (null != prev)
									sendFromMasterMsgToTgtSrvr(prev, new String[]{"SYNC", tgtSrvr.getBankName(), tgtSrvr.getUuid(), tgtSrvr.getIpAddrPort(), request[5]});
								else
									sendFromMasterMsgToTgtSrvr(tgtSrvr, new String[]{"ACTIVATE", tgtSrvr.getBankName(), tgtSrvr.getUuid(), tgtSrvr.getIpAddrPort()});
							}
							
						} else if ("SYNCED".equalsIgnoreCase(request[0])) {
							XBankServer syncedBy = new XBankServer(request[4], request[5], request[6], null);
							syncedBy.setServerList(listOfThisBanksServers);
							
							if(tgtSrvr.getPrevServer() != null && !tgtSrvr.getPrevServer().equals(syncedBy)){
								LOG.info("### "+listOfThisBanksServers);
								LOG.info(String.format("[%s] [%s] SYNCED by [%s].Needs new SYNC.", name, tgtSrvr.getUuid(),syncedBy.getUuid()));
								continue; //Someone beat tgtServer to become new tail. Current sync is stale.
							}
								
							// Activate new synced server, add to map
							LOG.info(String.format("[%s] Activating synced server %s", name, tgtSrvr.getUuid()));
							sendFromMasterMsgToTgtSrvr(tgtSrvr, new String[]{"ACTIVATE", tgtSrvr.getBankName(), tgtSrvr.getUuid(), tgtSrvr.getIpAddrPort()});
							if (!listOfThisBanksServers.contains(tgtSrvr)) {
								// New tail, tell old tail to add new tail, tell
								// new tail old tail is prev
								XBankServer oldTail = (XBankServer) listOfThisBanksServers.get(listOfThisBanksServers.size() - 1);
								LOG.info(String.format("[%s] Informing server [%s] that NEXT=[%s]", name, oldTail.getUuid(), tgtSrvr.getUuid()));
								sendFromMasterMsgToTgtSrvr(oldTail, new String[]{"NEXT", tgtSrvr.getBankName(), tgtSrvr.getUuid(), tgtSrvr.getIpAddrPort()});
								sendFromMasterMsgToTgtSrvr(oldTail, new String[]{"SYNC", tgtSrvr.getBankName(), tgtSrvr.getUuid(), tgtSrvr.getIpAddrPort()});
								
								LOG.info(String.format("[%s] Informing server [%s] that PREV=[%s]", name, tgtSrvr.getUuid(), oldTail.getUuid()));
								sendFromMasterMsgToTgtSrvr(tgtSrvr, new String[]{"PREV", oldTail.getBankName(), oldTail.getUuid(), oldTail.getIpAddrPort()});
								
								listOfThisBanksServers.add(tgtSrvr);
								bankMap.put(tgtSrvr.getBankName(), listOfThisBanksServers);
							}
						}
						
						LOG.info(String.format("[%s] Current srvr list of Bank[%s]=%s", name, request[1], listOfThisBanksServers));
					} catch (Exception e) {
						e.printStackTrace();
					}
				}
			}
		};
		
		Callable<Integer> reaperThread = new Callable<Integer>() {
			@Override
			public Integer call() {
				
				final long GRACE_PERIOD = ALIVE_MSGING_PERIOD * GRACE_PERIOD_MULTIPLIER;
				
				// Thread.currentThread().setDaemon(true);
				while (true) {
					try {
						LOG.info(String.format("[%s] Reaper checking for inactive servers...", name));
						long now = System.currentTimeMillis();
						synchronized (lastCheckedForPulseMap) {
							Set<String> iterHashSet = new TreeSet<String>(lastCheckedForPulseMap.keySet());
							for (String serverKey : iterHashSet) {
								
								BasicChainReplicationMessage lastAliveMessage = lastCheckedForPulseMap.get(serverKey);
								String[] aliveRequest = lastAliveMessage.getRequest();
								
								LOG.info(String.format("[%s] Server[%s] was last alive %d secs ago...", name, serverKey, (now - Long.parseLong(lastAliveMessage.getUuid())) / 1000));
								// Check for dead servers
								if (now - Long.parseLong(lastAliveMessage.getUuid()) > GRACE_PERIOD) {
									XBankServer tgtSrvr = new XBankServer(aliveRequest[1], aliveRequest[2], aliveRequest[3], null);
									tgtSrvr.setState(ServerState.RUNNABLE);
									
									List<BasicChainReplicationServer> listOfThisBanksServers = bankMap.get(tgtSrvr.getBankName());
									listOfThisBanksServers = null == listOfThisBanksServers
											? Collections.synchronizedList(new ArrayList<BasicChainReplicationServer>())
											: listOfThisBanksServers;
									tgtSrvr.setServerList(listOfThisBanksServers);
									
									LOG.info(String.format("[%s] Inactive server %s found...", name, serverKey));
									
									String[] die_msg = new String[]{"DIE", tgtSrvr.getBankName(), tgtSrvr.getUuid(), tgtSrvr.getIpAddrPort()};
									try {
										sendFromMasterMsgToTgtSrvr(tgtSrvr, die_msg);
									} catch (Exception e) {
									}
									// TODO: Tell N+ ans N- abt it.
									XBankServer prevSvr = (XBankServer) tgtSrvr.getPrevServer();
									XBankServer nxtSrvr = (XBankServer) tgtSrvr.getNextServer();
									
									if (null != prevSvr) {
										sendFromMasterMsgToTgtSrvr(prevSvr, die_msg);
									}
									
									if (null != nxtSrvr) {
										sendFromMasterMsgToTgtSrvr(nxtSrvr, die_msg);
									}
									
									if (null != nxtSrvr && null != prevSvr) {
										sendFromMasterMsgToTgtSrvr(prevSvr, new String[]{"SYNC", nxtSrvr.getBankName(), nxtSrvr.getUuid(), nxtSrvr.getIpAddrPort(), lastCheckedForPulseMap.get(nxtSrvr.getUuid()).getRequest()[5]});
									}
									
									// Remove time entry for optimization
									lastCheckedForPulseMap.remove(serverKey);
									
									// Remove from self list
									if (null != listOfThisBanksServers && !listOfThisBanksServers.isEmpty()) {
										listOfThisBanksServers.remove(tgtSrvr);
									}
								}
							}
						}
						Thread.yield();
						LOG.info(String.format("[%s] Reaper going to sleep...", name));
						Thread.sleep(GRACE_PERIOD);
					} catch (Exception e) {
						// e.printStackTrace();
						LOG.error(String.format("[%s] Exception [%s] occurred.", name, e.getMessage()));
					}
				}
			}
		};
		
		CUtils.work(reaperThread);
		CUtils.work(recvMsgThread);
		CUtils.work(processingIAmAliveMsgThread);
	}
}
