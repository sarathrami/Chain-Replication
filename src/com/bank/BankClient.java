/*
 * 
 */
package com.bank;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.jms.JMSException;

import org.actors.BasicChainReplicationClient;
import org.actors.BasicChainReplicationServer;
import org.apache.log4j.Logger;
import org.commons.BasicChainReplicationMessage;
import org.commons.Comm;
import org.commons.CommType;
import org.commons.Constants;
import org.commons.IComm;
import org.commons.customComm;
import org.utilities.CUtils;

// TODO: Auto-generated Javadoc
/**
 * The Class BankClient.
 */
public class BankClient extends BasicChainReplicationClient implements Constants {
	
	/** The counter. */
	private static long										counter				= 1000000l;
	/** The bank topology. */
	private Map<String, List<BasicChainReplicationServer>>	bankTopology		= new HashMap<String, List<BasicChainReplicationServer>>();
	
	/** The bank name. */
	private String											bankName			= null;
	
	/** The account number. */
	private String											accountNumber		= null;
	
	/** The me. */
	private IComm											me					= null;
	
	/** The from master comm. */
	private IComm											fromMasterComm		= null;
	
	/** The to master comm. */
	private IComm											toMasterComm		= null;
	
	/** The duplicate transfer. */
	private static String									duplicateTransfer	= System.getProperty("com.bank.BankClient.duptransfer", "");														// TODO:
																																			// Remove
																																			// test
																																			// code
	/**
	 * Instantiates a new bank client.
	 *
	 * @param bankName
	 *            the bank name
	 * @param accountNumber
	 *            the account number
	 * @param bankTopology
	 *            the bank topology
	 */
	public BankClient(String bankName, String accountNumber, Map<String, List<BasicChainReplicationServer>> bankTopology) {
		super();
		this.bankTopology = bankTopology;
		this.bankName = bankName;
		this.accountNumber = accountNumber;
		String clientQName = "CLIENT_" + this.accountNumber;
		try {
			me = new Comm(clientQName, "localhost:61616", CommType.COMM_UNRELIABLE);
			
			this.toMasterComm = new customComm("MASTER", "localhost:61616");
			this.fromMasterComm = new customComm(this.accountNumber + "_FROM_MASTER_Q", "localhost:61616");
		} catch (JMSException e) {
			e.printStackTrace();
		}
	}
	
	/**
	 * The main method.
	 *
	 * @param args
	 *            the arguments
	 * @throws Exception
	 *             the exception
	 */
	public void start(String[] args) throws Exception {
		try {
			// MAKE REQUESTS
			/*
			 * Thread.sleep(300);
			 * sendAndReceiveMsg("Citi#1", "Citi#1", new String[]{"Withdraw",
			 * "Citi", "#12345", "100"});
			 * // Kill server
			 * SERVER_LIST_0.remove(1);
			 * for (BasicChainReplicationServer s : SERVER_LIST_0) {
			 * s.setServerList(SERVER_LIST_0);
			 * }
			 */
			while (Integer.parseInt(System.getProperty("com.bank.BankClient.mode", "-1")) == 1) {
				List<BasicChainReplicationServer> myServerChain = bankTopology.get(bankName);
				Collections.sort(myServerChain);
				LOG.info(String.format("[%s] Received server list [%s]", this, myServerChain.toString()));
				
				String transferredToBankName = new ArrayList<String>(bankTopology.keySet()).get((int) (bankTopology.keySet().size() * Math.random()));
				// String headServer = ((BankServer)
				// myServerChain.get(0)).getUuid();
				// String tailServer = ((BankServer)
				// myServerChain.get(myServerChain.size() - 1)).getUuid();
				// ---------------------------------------------------------------------------------------------------------------------
				LOG.info(String.format("[%s] Getting Head/Tail of Bank[%s]", this, bankName));
				String response = "BAD_REQUEST";
				BasicChainReplicationMessage recvMsg = null;
				do {
					BasicChainReplicationMessage msg = new BasicChainReplicationMessage(new String[]{"HT", bankName, this.accountNumber, "localhost:61616"}, ""
							+ System.currentTimeMillis(), null, null);
					toMasterComm.sendMsg(msg);
					recvMsg = (BasicChainReplicationMessage) fromMasterComm.receiveMsg(0);
					response = recvMsg.getRequest()[0];
				} while ("BAD_REQUEST".equalsIgnoreCase(response));
				
				String headServer = recvMsg.getRequest()[2];
				String tailServer = recvMsg.getRequest()[5];
				LOG.info(String.format("[%s] Received Head/Tail of Bank[%s]: [%s]-->[%s]", this, bankName, headServer, tailServer));
				// GOT HEAD & TAIL FROM
				// MASTER-----------------------------------------------------------------------------------
				
				if (Math.random() <= Double.parseDouble(System.getProperty("com.bank.BankClient.probability.withdraw", "0.3"))) {
					Thread.sleep(1000);
					sendAndReceiveMsg(headServer, tailServer, new String[]{"Withdraw", bankName, accountNumber, "10"});
				}
				
				if (Math.random() <= Double.parseDouble(System.getProperty("com.bank.BankClient.probability.transfer", "0.3"))) {
					Thread.sleep(3000);
					sendAndReceiveMsg(headServer, tailServer, new String[]{"Transfer", bankName, accountNumber, "150", transferredToBankName, "#ABCDE"});
				}
				
				if (Math.random() <= Double.parseDouble(System.getProperty("com.bank.BankClient.probability.deposit", "0.3"))) {
					Thread.sleep(1000);
					sendAndReceiveMsg(headServer, tailServer, new String[]{"Deposit", bankName, accountNumber, "100"});
				}
				
				if (Math.random() <= Double.parseDouble(System.getProperty("com.bank.BankClient.probability.balance", "0.3"))) {
					Thread.sleep(1000);
					sendAndReceiveMsg(tailServer, tailServer, new String[]{"Balance", bankName, accountNumber});
				}
			}
			
			/*
			 * sendAndReceiveMsg("Citi#1", "Citi#2", new String[]{"Withdraw",
			 * "Citi", "#12345", "100"});
			 * Thread.sleep(3000);
			 * sendAndReceiveMsg("Citi#1", "Citi#2", new String[]{"Transfer",
			 * "Citi", "#12345", "3000", "IOB", "#ABCDE"});
			 * Thread.sleep(1000);
			 * sendAndReceiveMsg("Citi#2", "Citi#2", new String[]{"Balance",
			 * "Citi", "#12345"});
			 * Thread.sleep(1000);
			 * sendAndReceiveMsg("IOB#2", "IOB#2", new String[]{"Balance",
			 * "IOB", "#ABCDE"});
			 */
			
			if (Integer.parseInt(System.getProperty("com.bank.BankClient.mode", "-1")) == 0) {
				List<BasicChainReplicationServer> myServerChain = bankTopology.get(bankName);
				Collections.sort(myServerChain);
				LOG.info(String.format("[%s] Received server list [%s]", this, myServerChain.toString()));
				
				String actionSetString = System.getProperty("com.bank.BankClient.actions");
				
				if (null == actionSetString)
					throw new RuntimeException("Action set not defined.");
				
				for (String action : actionSetString.split(";")) {
					
					// ---------------------------------------------------------------------------------------------------------------------
					LOG.info(String.format("[%s] Getting Head/Tail of Bank[%s]", this, bankName));
					String response = "BAD_REQUEST";
					BasicChainReplicationMessage recvMsg = null;
					do {
						BasicChainReplicationMessage msg = new BasicChainReplicationMessage(new String[]{"HT", bankName, this.accountNumber, "localhost:61616"}, ""
								+ System.currentTimeMillis(), null, null);
						toMasterComm.sendMsg(msg);
						recvMsg = (BasicChainReplicationMessage) fromMasterComm.receiveMsg(0);
						response = recvMsg.getRequest()[0];
					} while ("BAD_REQUEST".equalsIgnoreCase(response));
					
					String headServer = recvMsg.getRequest()[2];
					String tailServer = recvMsg.getRequest()[5];
					LOG.info(String.format("[%s] Received Head/Tail of Bank[%s]: [%s]-->[%s]", this, bankName, headServer, tailServer));
					// GOT HEAD & TAIL FROM
					// MASTER-----------------------------------------------------------------------------------
					
					String[] actionSet = action.split(" ");
					if (actionSet[0].equalsIgnoreCase("Balance")) {
						sendAndReceiveMsg(headServer, tailServer, new String[]{actionSet[0], bankName, accountNumber});
					} else if (actionSet[0].equalsIgnoreCase("Deposit") || actionSet[0].equalsIgnoreCase("Withdraw")) {
						if (actionSet.length != 2)
							throw new RuntimeException("Action set not properly defined.");
						sendAndReceiveMsg(headServer, tailServer, new String[]{actionSet[0], bankName, accountNumber, actionSet[1]});
					} else if (actionSet[0].equalsIgnoreCase("Transfer")) {
						if (actionSet.length != 4)
							throw new RuntimeException("Action set not properly defined.");
						sendAndReceiveMsg(headServer, tailServer, new String[]{actionSet[0], bankName, accountNumber, actionSet[1], actionSet[2], actionSet[3]});
					} else {
						throw new RuntimeException("Unknown action defined.");
					}
					Thread.sleep(1000);
				}
			}
			
		} catch (Exception e) {
			throw new RuntimeException("Failed to load config file: ", e);
		} finally {
			LOG.info("Done.");
		}
	}
	
	/**
	 * Send and receive msg.
	 *
	 * @param target
	 *            the target
	 * @param src
	 *            the src
	 * @param req
	 *            the req
	 * @throws JMSException
	 *             the JMS exception
	 * @throws InterruptedException
	 *             the interrupted exception
	 */
	private void sendAndReceiveMsg(String target, String src, final String[] req) throws JMSException, InterruptedException {
		int phase = 0;
		String infix = req[1] + req[2];
		BasicChainReplicationMessage opMsg = null;
		BasicChainReplicationMessage msg = null;
		
		int retryCount = Integer.parseInt(System.getProperty("com.bank.BankClient.retry", "1"));
		String lastSentUID = getUID();
		
		if ("TRANSFER".equalsIgnoreCase(req[0]))
			if (!duplicateTransfer.isEmpty() && duplicateTransfer.equalsIgnoreCase("true"))// TODO: REMOVE TEST CODE
				lastSentUID = duplicateTransfer;
			else
				duplicateTransfer = lastSentUID;
		
		IComm to = new customComm(target, "localhost:61616", CommType.COMM_UNRELIABLE);
		LOG.info("[" + this.accountNumber + "] Received Head=" + target + " Tail=" + src);
		while (retryCount-- > 0) {
			try {
				if (phase == 0) {
					msg = new BasicChainReplicationMessage(req, lastSentUID, "CLIENT_" + this.accountNumber, "localhost:61616");
					LOG.info("[" + this.accountNumber + "] SENT     : " + msg);
					to.sendMsg(msg);
					opMsg = (BasicChainReplicationMessage) me.receiveMsg(10 * RETRANSMISSION_DELAY);
					if (null == opMsg) {
						LOG.error("[" + this.accountNumber + "] Failed to get response from server. Retrying request.");
						continue;
					}
					// #1 LOG.info("RECEIVED : " + client.receiveMsg(0));
				}
				
				Thread.sleep(800);
				phase = 1;
				lastSentUID = getUID();
				if (phase == 1) {
					// Get Balance
					msg = new BasicChainReplicationMessage(req, lastSentUID, "CLIENT_" + this.accountNumber, "localhost:61616");
					msg.setRequest(new String[]{"Balance", req[1], req[2]});
					// #2 LOG.info(">>SENT     : " + msg);
					to.sendMsg(msg);
					String response[] = ((BasicChainReplicationMessage) me.receiveMsg(10 * RETRANSMISSION_DELAY)).getRequest();
					LOG.info("["
							+ this.accountNumber
							+ "] RECEIVED : "
							+ Arrays.asList(new String[]{
									opMsg.getRequest()[0].equalsIgnoreCase("Deposit") || opMsg.getRequest()[0].equalsIgnoreCase("Transfer")
											|| opMsg.getRequest()[0].equalsIgnoreCase("Withdraw") || opMsg.getRequest()[0].equalsIgnoreCase("Balance") ? "Processed" : opMsg
											.getRequest()[0], response[2], response[3]}));
				}
				
				Thread.yield();
				return;
			} catch (Exception e) {
				LOG.error("[" + this.accountNumber + "] Failed to get response from server. Retrying request.");
			}
		}
		LOG.error("[" + this.accountNumber + "] Failed to get response from server. Dropping request.");
	}
	
	/**
	 * Gets the uid.
	 *
	 * @param infix
	 *            the infix
	 * @return the uid
	 */
	private static String getUID(String infix) {
		String suffix = CUtils.getRandomString(Math.random() * Integer.MAX_VALUE);
		// suffix = "CONSTANT_SUFFIX"; // Check for inconsistency, duplicates
		return "UUID." + infix + "." + suffix;
	}
	
	/**
	 * Gets the uid.
	 *
	 * @return the uid
	 */
	private synchronized static String getUID() {
		return "" + counter++;
	}
	
	/** The Constant LOG. */
	private static final Logger	LOG	= Logger.getLogger(BankClient.class);
}
