/*
 * 
 */
package com.bank;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.TreeSet;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.Callable;
import java.util.concurrent.LinkedBlockingQueue;

import javax.jms.JMSException;

import org.actors.BasicChainReplicationServer;
import org.apache.log4j.Logger;
import org.commons.BasicChainReplicationMessage;
import org.commons.CommType;
import org.commons.IComm;
import org.commons.ServerState;
import org.commons.customComm;
import org.utilities.CUtils;

// TODO: Auto-generated Javadoc
/**
 * The Class BankServer.
 */
public class XBankServer extends BankServer {
	
	/** The Constant serialVersionUID. */
	private static final long							serialVersionUID	= -8086774242343208394L;
	
	/** The alive msging period. */
	private long										ALIVE_MSGING_PERIOD	= Long.parseLong(System.getProperty("com.bank.BankServer.aliveMsgPeriod", "" + Long.MAX_VALUE));
	
	/** The die after n msg. */
	private long										dieAfterNMsg		= Long.parseLong(System.getProperty("com.bank.BankServer.dieAfterNMsg." + uuid, "" + Long.MAX_VALUE));
	
	/** The log. */
	private final static Logger							LOG					= Logger.getLogger(XBankServer.class);
	
	/** The from master comm. */
	private IComm										fromMasterComm		= null;
	
	/** The to master comm. */
	private IComm										toMasterComm		= null;
	
	/** The master msg recved list. */
	private BlockingQueue<BasicChainReplicationMessage>	masterMsgRecvedList	= new LinkedBlockingQueue<BasicChainReplicationMessage>();
	
	/**
	 * Instantiates a new bank server.
	 *
	 * @param bankName
	 *            the bank name
	 * @param uuid
	 *            the uuid
	 * @param myIPAddrPort
	 *            the my ip addr port
	 * @param bankTopology
	 *            the bank topology
	 * @throws JMSException
	 *             the JMS exception
	 */
	public XBankServer(String bankName, String uuid, String myIPAddrPort, Map<String, List<BasicChainReplicationServer>> bankTopology) throws JMSException {
		super(bankName, uuid, myIPAddrPort, null);
		toMasterComm = new customComm("MASTER", "localhost:61616");
		fromMasterComm = new customComm(this.getUuid() + "_FROM_MASTER_Q", "localhost:61616");
		this.addServer(this);
	}
	
	/*
	 * (non-Javadoc)
	 * 
	 * @see org.actors.BasicChainReplicationServer#start()
	 */
	@Override
	public void start() {
		try {
			long delay = Long.parseLong(System.getProperty("com.bank.BankServer.startupdelay." + this.getUuid(), "0"));
			LOG.info(String.format("[%s] Server start delayed by %dms", this, delay));
			Thread.sleep(delay);
			LOG.info(String.format("[%s] I am up.", this));
			
			Callable<Integer> sendIAmAliveMsgToMasterThread = new Callable<Integer>() {
				@Override
				public Integer call() {
					while (true) {
						try {
							LOG.info(String.format("[%s] Sending ALIVE to BankMaster...", XBankServer.this));
							BasicChainReplicationMessage msg = new BasicChainReplicationMessage(createAliveMsg(), "" + System.currentTimeMillis(), null, null);
							toMasterComm.sendMsg(msg);
							Thread.sleep(ALIVE_MSGING_PERIOD);
						} catch (InterruptedException e) {
						} catch (JMSException e) {
							e.printStackTrace();
						}
					}
				}
				
				// Standard 'alive' message to send to master
				private String[] createAliveMsg() {
					List<BasicChainReplicationMessage> tmpHistLst = new ArrayList<>(hist);
					String uuidOfLastH = "0";
					if (tmpHistLst.size() > 0)
						uuidOfLastH = tmpHistLst.get(tmpHistLst.size() - 1).getUuid();
					return new String[]{"ALIVE", XBankServer.this.getBankName(), XBankServer.this.uuid, XBankServer.this.ipAddrPort, XBankServer.this.getState().toString(),
							uuidOfLastH};
				}
			};
			
			startListeningToMaster();
			XBankServer.super.start();
			CUtils.work(sendIAmAliveMsgToMasterThread);
		} catch (NumberFormatException | InterruptedException e) {
			e.printStackTrace();
		}
	}
	
	/**
	 * The main method.
	 *
	 * @param args
	 *            the arguments
	 * @throws JMSException
	 *             the JMS exception
	 */
	public static void main(String args[]) throws JMSException {
		
		try {
			// Load master configuration
			Properties p = new Properties(System.getProperties());
			
			p.load(new FileInputStream(new File(args[0])));
			System.setProperties(p);
			
			XBankServer me = null;
			if (null == me) {
				me = new XBankServer(args[1], args[2], args[3], null);
			}
			
			// me.setBankTopology(null);
			me.setServerList(new ArrayList<BasicChainReplicationServer>(Collections.singletonList(me)));
			
			me.startListeningToMaster();
			me.setState(ServerState.UNINITIALIZED);
			me.start();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	/**
	 * Start listening to master.
	 */
	private void startListeningToMaster() {
		
		Callable<Integer> recvMasterMsgsThread = new Callable<Integer>() {
			@Override
			public Integer call() {
				while (true) {
					try {
						BasicChainReplicationMessage msg = (BasicChainReplicationMessage) fromMasterComm.receiveMsg(0);
						// Seppuku time?
						if (msg.getLocalMessageID() > dieAfterNMsg)
							die(); // Seppuku !
						masterMsgRecvedList.add(msg);
					} catch (Exception e) {
						e.printStackTrace();
					}
				}
			}
		};
		
		Callable<Integer> processMasterMsgsThread = new Callable<Integer>() {
			@Override
			public Integer call() {
				while (true) {
					try {
						LOG.info(String.format("[%s] Listening for msg from Master...", XBankServer.this));
						BasicChainReplicationMessage msg = masterMsgRecvedList.take();
						LOG.info(String.format("[%s] Received %s from Master...", XBankServer.this, msg));
						
						String[] request = msg.getRequest();
						XBankServer tgtSrvr = new XBankServer(request[1], request[2], request[3], null);
						
						if ("ACTIVATE".equalsIgnoreCase(request[0])) {
							XBankServer.this.setState(ServerState.RUNNING);
						} else if ("DIE".equalsIgnoreCase(request[0])) {
							LOG.info(String.format("[%s] Server[%s] is dead.", XBankServer.this, request[2]));
							if (tgtSrvr.equals(XBankServer.this.getPrevServer())) {
								LOG.info(String.format("[%s] Prev[%s] is dead, setting state to [%s]...", XBankServer.this, request[1], ServerState.UNINITIALIZED.toString()));
								XBankServer.this.setState(ServerState.UNINITIALIZED);
							}
							
							XBankServer.this.remServer(tgtSrvr);
							
							if (tgtSrvr.getUuid().equalsIgnoreCase(XBankServer.this.getUuid()))
								XBankServer.this.die();
						} else if ("HT".equalsIgnoreCase(request[0])) {
							XBankServer tgt2Srvr = new XBankServer(request[4], request[5], request[6], null);
							List<BasicChainReplicationServer> bsl = XBankServer.this.getBankTopology().get(request[4]) == null ? Collections
									.synchronizedList(new ArrayList<BasicChainReplicationServer>()) : XBankServer.this.getBankTopology().get(request[4]);
							bsl.clear();
							bsl.add(tgtSrvr);
							bsl.add(tgt2Srvr);
							XBankServer.this.getBankTopology().put(request[4], bsl);
						} else if ("SYNC".equalsIgnoreCase(request[0])) {
							int retryCount = Integer.parseInt(System.getProperty("com.bank.BankClient.retry", "1"));
							IComm newcomm = new customComm(tgtSrvr.getUuid(), tgtSrvr.getIpAddrPort(), CommType.COMM_RELIABLE);
							boolean successful_in_relay = true;
							
							LOG.info(String.format("[%s] Attempting SYNC of {H} to new tail[%s]\n\n", XBankServer.this, tgtSrvr.getUuid()));
							for (BasicChainReplicationMessage hist_msg : new TreeSet<>(XBankServer.this.hist)) {
								successful_in_relay = false;
								while (!successful_in_relay && retryCount-- > 0) {
									try {
										int fromMsg = 0;
										if(request.length >=5)
											Integer.parseInt(request[4]);
										if (Integer.parseInt(hist_msg.getUuid()) <= fromMsg) {
											LOG.info(String.format("[%s] Skipping sending {H}[%s] to new tail[%s]\n\n", XBankServer.this, hist_msg, tgtSrvr.getUuid()));
											successful_in_relay = true;
											break;
										}
										hist_msg.setDestinationQueueName("SYNC");
										LOG.info(String.format("[%s] Sending {H}[%s] to new tail[%s]\n\n", XBankServer.this, hist_msg, tgtSrvr.getUuid()));
										newcomm.sendMsg(hist_msg);
										// newcomm.waitForAck(hist_msg,
										// RETRANSMISSION_DELAY);
										successful_in_relay = true;
										
										if (Boolean.parseBoolean(System.getProperty("com.bank.BankServer.tail.old.dieDuringSync", "false"))){
											LOG.info(String.format("[%s] Dying during SYNC", XBankServer.this));
											XBankServer.this.die();
										}
										
									} catch (JMSException e) {
										// e.printStackTrace();
										LOG.error(String.format("[%s] %s", XBankServer.this, e.getMessage()));
										Thread.yield();
									}
								}
							}
							
							if (successful_in_relay) {
								LOG.info(String.format("[%s] Sending SYNC of [%s] done to MASTER", XBankServer.this, tgtSrvr.getUuid()));
								BasicChainReplicationMessage synced_msg = new BasicChainReplicationMessage(new String[]{"SYNCED", tgtSrvr.getBankName(), tgtSrvr.getUuid(),
										tgtSrvr.getIpAddrPort(), XBankServer.this.getBankName(), XBankServer.this.getUuid(), XBankServer.this.getIpAddrPort()}, ""
										+ System.currentTimeMillis(), "RECONFIG", tgtSrvr.getIpAddrPort());
								toMasterComm.sendMsg(synced_msg);
							} else {
								LOG.info(String.format("[%s] SYNC of [%s] failed.", XBankServer.this, tgtSrvr.getUuid()));
							}
						} else if ("PREV".equalsIgnoreCase(request[0])) {
							if (null == XBankServer.this.getPrevServer()) {// Should
																			// never
																			// happen
								XBankServer.this.getServerList().add(0, tgtSrvr);
							}
							
							XBankServer.this.getPrevServer().setUuid(tgtSrvr.getUuid());
							XBankServer.this.getPrevServer().setIpAddrPort(tgtSrvr.getIpAddrPort());
							XBankServer.this.getPrevServer().setState(ServerState.RUNNING);
							
							LOG.info(String.format("[%s] Prev is [%s]. List=%s", XBankServer.this, tgtSrvr.getUuid(), XBankServer.this.getServerList()));
						} else if ("NEXT".equalsIgnoreCase(request[0])) {
							if (null == XBankServer.this.getNextServer()) {
								XBankServer.this.addServer(tgtSrvr);
							} else {
								XBankServer.this.getNextServer().setUuid(tgtSrvr.getUuid());
								XBankServer.this.getNextServer().setIpAddrPort(tgtSrvr.getIpAddrPort());
								XBankServer.this.getNextServer().setState(ServerState.RUNNING);
							}
							LOG.info(String.format("[%s] Next is [%s]. List=%s", XBankServer.this, tgtSrvr.getUuid(), XBankServer.this.getServerList()));
						}
						
					} catch (Exception e) {
						e.printStackTrace();
						LOG.error("BAD_REQUEST." + e.getMessage());
					}
				}
			}
		};
		
		CUtils.work(recvMasterMsgsThread);
		CUtils.work(processMasterMsgsThread);
	}
	
	/*
	 * (non-Javadoc)
	 * 
	 * @see com.bank.BankServer#postProcessMessage(org.commons.
	 * BasicChainReplicationMessage)
	 */
	@Override
	protected BasicChainReplicationMessage postProcessMessage(final BasicChainReplicationMessage receivedMsg) {
		String request[] = receivedMsg.getRequest();
		
		boolean transferDone = false;
		while (!transferDone) {
			try {
				// INTER BANK TRANSFER ONLY. LOCAL TRANSFER NEED NOT BE
				// POST-PROCESSED
				if (isTail() && (request[0].equalsIgnoreCase("Transfer") && !request[4].startsWith(this.getBankName()))) {
					// ---------------------------------------------------------------------------------------------------------------------
					LOG.info(String.format("[%s] Getting Head/Tail of Bank[%s]", this, request[4]));
					do {
						BasicChainReplicationMessage msg = new BasicChainReplicationMessage(new String[]{"HT", request[4], this.getUuid(), this.getIpAddrPort()}, ""
								+ System.currentTimeMillis(), null, null);
						try {
							toMasterComm.sendMsg(msg);
						} catch (JMSException e) {
							e.printStackTrace();
						}
						
						try {
							Thread.sleep(500);
						} catch (InterruptedException e) {
						}
					} while (null == this.getBankTopology() || null == this.getBankTopology().get(request[4]) || this.getBankTopology().get(request[4]).size() < 2);// Wait
																																									// reply
					// GOT HEAD & TAIL FROM
					// MASTER-----------------------------------------------------------------------------------
					return super.postProcessMessage(receivedMsg);
				}
				
				transferDone = true;
			} catch (Exception e) {
				
			}
		}
		
		return super.postProcessMessage(receivedMsg);
	}
}
