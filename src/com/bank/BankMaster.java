/*
 * 
 */
package com.bank;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.TreeSet;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.Callable;
import java.util.concurrent.LinkedBlockingQueue;

import javax.jms.JMSException;

import org.actors.BasicChainReplicationServer;
import org.apache.log4j.Logger;
import org.commons.BasicChainReplicationMessage;
import org.commons.Comm;
import org.commons.CommType;
import org.commons.Constants;
import org.commons.IComm;
import org.utilities.CUtils;

// TODO: Auto-generated Javadoc
/**
 * The Class BankMaster.
 */
public class BankMaster implements Constants {
	
	/** The alive msging period. */
	private long											ALIVE_MSGING_PERIOD		= Long.parseLong(System
																							.getProperty(
																									"com.bank.BankServer.aliveMsgPeriod",
																									"" + Long.MAX_VALUE));
	
	/** The Constant GRACE_PERIOD_MULTIPLIER. */
	protected static final long								GRACE_PERIOD_MULTIPLIER	= Math.abs(Long.parseLong(System
																							.getProperty(
																									"com.bank.BankServer.gracePeriodMultiplier",
																									"" + 4)));
	
	/** The log. */
	private final static Logger								LOG						= Logger.getLogger(BankServer.class);
	
	/** The name. */
	private final String									name					= "MASTER";
	
	/** The me. */
	private IComm											me						= null;
	
	/** The bank map. */
	private Map<String, List<BasicChainReplicationServer>>	bankMap					= null;
	
	/** The last checked for pulse map. */
	private Map<String, BasicChainReplicationMessage>		lastCheckedForPulseMap	= Collections
																							.synchronizedMap(new HashMap<String, BasicChainReplicationMessage>());
	
	/** The lst of all servers. */
	private List<BasicChainReplicationServer>				lstOfAllServers			= Collections
																							.synchronizedList(new ArrayList<BasicChainReplicationServer>());
	
	/** The alive msg recved list. */
	private BlockingQueue<BasicChainReplicationMessage>				aliveMsgRecvedList		= new LinkedBlockingQueue<BasicChainReplicationMessage>();
	/*
	 * (non-Javadoc)
	 * 
	 * @see java.lang.Object#toString()
	 */
	@Override
	public String toString() {
		return "BankMaster [name=" + name + "]";
	}
	
	/**
	 * Instantiates a new bank master.
	 *
	 * @param bt
	 *            the bt
	 */
	public BankMaster(Map<String, List<BasicChainReplicationServer>> bt) {
		super();
		
		this.bankMap = bt;
		
		String masterQName = name;
		try {
			me = new Comm(masterQName, "localhost:61616", CommType.COMM_RELIABLE);
		} catch (JMSException e) {
			e.printStackTrace();
		}
		
		for (Entry<String, List<BasicChainReplicationServer>> bme : bankMap.entrySet()) {
			List<BasicChainReplicationServer> lstOfBanks = bme.getValue();
			lstOfAllServers.addAll(lstOfBanks);
		}
		
		//Initialize lastCheckedForPulseMap
		long now = System.currentTimeMillis();
		for (BasicChainReplicationServer svr : lstOfAllServers) {
			lastCheckedForPulseMap.put(svr.getUuid(), new BasicChainReplicationMessage(new String[]{"ALIVE", "" + now,
					((BankServer) svr).getBankName(), svr.getUuid()}, svr.getUuid(), null, svr.getIpAddrPort()));
		}
		
		/*//INIT _ ALL _ MAPS AND LISTS
		lstOfAllServers.clear();
		for (Entry<String, List<BasicChainReplicationServer>> bme : bankMap.entrySet()) {
			bme.getValue().clear();
		}*/
		
		Callable<Integer> recvIAmAliveMsgThread = new Callable<Integer>() {
			@Override
			public Integer call() {
				while (true) {
					try {
						LOG.info(String.format("[%s] %s", BankMaster.this, lstOfAllServers.toString()));
						LOG.info(String.format("[%s] Listening...", BankMaster.this));
						BasicChainReplicationMessage msg = (BasicChainReplicationMessage) me.receiveMsg(0);
						LOG.info(String.format("[%s] Received %s", BankMaster.this, msg));
						msg.getRequest()[1] = "" + System.currentTimeMillis();
						
						synchronized (lastCheckedForPulseMap) {
							lastCheckedForPulseMap.put(msg.getRequest()[3], msg);
						}
						
						aliveMsgRecvedList.add(msg);
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
						BasicChainReplicationMessage msg = aliveMsgRecvedList.take();
						BasicChainReplicationServer tgtSrvr = new BankServer(msg.getRequest()[2], msg.getRequest()[3],
								msg.getRequest()[4], null);
						
						if (!lstOfAllServers.contains(tgtSrvr)){
							lstOfAllServers.add(tgtSrvr);
							// TODO: Inform all about new server
							for (Entry<String, List<BasicChainReplicationServer>> entry : BankMaster.this.bankMap
									.entrySet()) {
								List<BasicChainReplicationServer> lstBankServers = entry.getValue();
								
								// Add to master's map for corresponding Bank
								if (((BankServer) tgtSrvr).getBankName().equalsIgnoreCase(entry.getKey())
										&& !lstBankServers.contains(tgtSrvr)) {
									LOG.info(String.format("[%s] Adding server %s to %s's list", BankMaster.this,
											tgtSrvr.getUuid(), entry.getKey()));
									lstBankServers.add(tgtSrvr);
								}
								
								if (null != lstBankServers && !lstBankServers.isEmpty()) {
									for (int i = 0; i < lstBankServers.size(); i++) {///////////////////////////////&&&&&&&&
										BasicChainReplicationServer server = lstBankServers.get(i);
										String serverQName = server.getUuid() + "_FROM_MASTER_Q";
										try {
											LOG.info(String.format("[%s] Informing server %s of %s's addition",
													BankMaster.this, server.getUuid(), msg.getRequest()[3]));
											Comm comm = new Comm(serverQName, server.getIpAddrPort(), CommType.COMM_RELIABLE);
											// RECV: [ALIVE, 1415628455062,
											// Citi, Citi_3]
											// SEND: [ADD/REM,
											// BANK_NAME,BANK_UUID,
											// BANK_IP_PORT]
											BasicChainReplicationMessage msg2 = new BasicChainReplicationMessage(
													new String[]{"ADD", msg.getRequest()[2], msg.getRequest()[3],
															msg.getRequest()[4]}, "" + System.currentTimeMillis(),
													"RECONFIG", "localhost:61616");
											comm.sendMsg(msg2);
										} catch (JMSException e) {
											e.printStackTrace();
										}
									}
								}
							}
						}
						
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
				
				while (true) {
					try {
						long now = System.currentTimeMillis();
						synchronized (lastCheckedForPulseMap) {
							Set<String> iterHashSet = new TreeSet<String>(lastCheckedForPulseMap.keySet());
							for (String serverKey : iterHashSet) {
								
								String[] aliveRequest = lastCheckedForPulseMap.get(serverKey).getRequest();
								
								LOG.info(String.format("[%s] Server[%s] was last alive %d secs ago...",
										BankMaster.this, serverKey, (now - Long.parseLong(aliveRequest[1])) / 1000));
								// Check for dead servers
								if (Long.parseLong(aliveRequest[1]) < now - GRACE_PERIOD) {
									
									LOG.info(String.format("[%s] Inactive server %s found...", BankMaster.this,
											serverKey));
									
									// Remove time entry for optimization
									lastCheckedForPulseMap.remove(serverKey);
									
									// TODO: Tell all about death
									for (Entry<String, List<BasicChainReplicationServer>> entry : BankMaster.this.bankMap
											.entrySet()) {
										List<BasicChainReplicationServer> lstBankServers = entry.getValue();
										if (null != lstBankServers && !lstBankServers.isEmpty()) {
											for (int i = 0; i < lstBankServers.size(); i++) {
												BasicChainReplicationServer server = lstBankServers.get(i);
												String serverQName = server.getUuid() + "_FROM_MASTER_Q";
												try {
													
													// Remove from master's map
													if (((BankServer) server).getBankName().equalsIgnoreCase(
															entry.getKey())
															&& server.getUuid().equalsIgnoreCase(serverKey)) {
														lstBankServers.remove(server);
														lstOfAllServers.remove(server);
													}
													
													LOG.info(String.format("[%s] Informing server %s of %s's death",
															BankMaster.this, server.getUuid(), serverKey));
													Comm comm = new Comm(serverQName, server.getIpAddrPort(),
															CommType.COMM_RELIABLE);
													// RECV: [ALIVE,
													// 1415628455062,
													// Citi, Citi_3]
													// SEND: [ADD/REM,
													// BANK_NAME,BANK_UUID,
													// BANK_IP_PORT]
													BasicChainReplicationMessage msg = new BasicChainReplicationMessage(
															new String[]{"REM", aliveRequest[2], serverKey,
																	"localhost:61616"},
															"" + System.currentTimeMillis(), "RECONFIG",
															"localhost:61616");
													comm.sendMsg(msg);
												} catch (JMSException e) {
													e.printStackTrace();
												}
											}
										}
									}
								}
							}
						}
						Thread.yield();
						Thread.sleep(GRACE_PERIOD);
						LOG.info(String.format("[%s] Reaper checking for inactive servers...", BankMaster.this));
					} catch (Exception e) {
						e.printStackTrace();
					}
				}
			}
		};
		
		CUtils.work(processingIAmAliveMsgThread);
		CUtils.work(recvIAmAliveMsgThread);
		CUtils.work(reaperThread);
	}
}
