/*
 * 
 */
package com.bank;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.jms.JMSException;

import org.actors.BasicChainReplicationServer;
import org.apache.log4j.Logger;
import org.commons.BasicChainReplicationMessage;
import org.commons.Comm;
import org.commons.CommType;
import org.commons.IComm;
import org.commons.customComm;

// TODO: Auto-generated Javadoc
/**
 * The Class BankServer.
 */
public class BankServer extends BasicChainReplicationServer {
	
	/** The die after n msg. */
	private long											dieAfterNMsg		= Long.parseLong(System
																						.getProperty("com.bank.BankServer.dieAfterNMsg." + uuid, "" + Long.MAX_VALUE));
	/** The Constant serialVersionUID. */
	private static final long								serialVersionUID	= -4761408322395608661L;
	
	/** The log. */
	private final static Logger								LOG					= Logger.getLogger(BankServer.class);
	
	/** The bank topology. */
	private Map<String, List<BasicChainReplicationServer>>	bankTopology		= new HashMap<String, List<BasicChainReplicationServer>>();
	
	/** The bank name. */
	private String											bankName			= null;
	
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
	 */
	public BankServer(String bankName, String uuid, String myIPAddrPort, Map<String, List<BasicChainReplicationServer>> bankTopology) {
		super(uuid, myIPAddrPort, null);
		if (null != bankTopology) {
			this.bankTopology = bankTopology;
			super.setServerList(bankTopology.get(bankName));
		}
		this.bankName = bankName;
	}
	
	/**
	 * Gets the bank name.
	 *
	 * @return the bank name
	 */
	public String getBankName() {
		return bankName;
	}
	
	/**
	 * Gets the bank topology.
	 *
	 * @return the bank topology
	 */
	public Map<String, List<BasicChainReplicationServer>> getBankTopology() {
		return bankTopology;
	}
	
	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.actors.BasicChainReplicationServer#postProcessMessage(org.commons
	 * .BasicChainReplicationMessage)
	 */
	@Override
	protected BasicChainReplicationMessage postProcessMessage(final BasicChainReplicationMessage receivedMsg) {
		
		String request[] = receivedMsg.getRequest();
		
		// INTER BANK TRANSFER ONLY. LOCAL TRANSFER NEED NOT BE
		// POST-PROCESSED
		if (isTail() && (request[0].equalsIgnoreCase("Transfer") && !request[4].startsWith(this.bankName))) {
			
			IComm tgtCommObject = null;
			IComm myCommObject = null;
			try {
				BasicChainReplicationServer tgtServer = bankTopology.get(request[4]).get(0);
				String tmpQName = this.getUuid() + ".TMP";
				myCommObject = new Comm(tmpQName, this.getIpAddrPort(), CommType.COMM_UNRELIABLE);
				tgtCommObject = new customComm(tgtServer.getUuid(), tgtServer.getIpAddrPort(), CommType.COMM_UNRELIABLE);
				
				BasicChainReplicationMessage newToSendMsg = new BasicChainReplicationMessage(request, "UUID_" + Math.random(), tmpQName, this.getIpAddrPort());
				
				LOG.info(String.format("[%s]\t:\tTransfering [%s] to [%s]", this, newToSendMsg, tgtCommObject));
				newToSendMsg.setDestinationQueueName(tmpQName);
				newToSendMsg.setIpAddrPort(this.getIpAddrPort());
				tgtCommObject.sendMsg(newToSendMsg);
				
				if (Boolean.parseBoolean(System.getProperty("com.bank.BankServer.dieAfterTransferSend." + this.getUuid(), "false"))) {
					LOG.info(String.format("[%s]\t:\tDying after sending transfer request %s", this, newToSendMsg));
					this.die();
				}
				
				LOG.info(String.format("[%s]\t:\tWaiting for Ack for [%s] from [%s] to [%s]...", this, newToSendMsg, tgtCommObject, myCommObject));
				BasicChainReplicationMessage newReceivedMsg = (BasicChainReplicationMessage) myCommObject.receiveMsg(RETRANSMISSION_DELAY);
				if (null == newReceivedMsg || !newReceivedMsg.getUuid().equalsIgnoreCase(newToSendMsg.getUuid()))
					throw new JMSException("Failed to transfer to target bank. Retrying.");
				LOG.info(String.format("[%s]\t:\tTransfer [%s] to [%s] complete !", this, newToSendMsg, tgtCommObject));
			} catch (Exception e) {
				e.printStackTrace();
				try {
					Thread.sleep(RETRANSMISSION_DELAY);
				} catch (InterruptedException e1) {
				}
			} finally {
				try {
					if (null != tgtCommObject)
						tgtCommObject.close();
					if (null != myCommObject)
						myCommObject.close();
				} catch (JMSException e) {
					e.printStackTrace();
				}
			}
		}
		
		// Modify response as per spec
		if (isTail()) {/*
						 * if (request[0].equalsIgnoreCase("TRANSFER") ||
						 * request[0].equalsIgnoreCase("DEPOSIT")
						 * || request[0].equalsIgnoreCase("WITHDRAW")
						 * //|| request[0].equalsIgnoreCase("BALANCE")
						 * ) {
						 * 
						 * ///////////////////////////////////////////////////
						 * // REQ[0] = REQ/RES
						 * // REQ[1] = BNK#1
						 * // REQ[2] = A/C#1
						 * // REQ[3] = AMT
						 * // REQ[4] = BNK#2
						 * // REQ[5] = A/C#2
						 * ///////////////////////////////////////////////////
						 * // if(request[0].equalsIgnoreCase("TRANSFER"))
						 * // preProcessMessage(receivedMsg).getRequest()[2] =
						 * preProcessMessage(receivedMsg).getRequest()[5];
						 * preProcessMessage(receivedMsg).getRequest()[0] =
						 * "Balance";
						 * receivedMsg.setRequest(new String[]{"Processed",
						 * request[2],
						 * preProcessMessage(receivedMsg).getRequest()[3]});
						 * }
						 */
		}
		
		return receivedMsg;
	}
	
	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.actors.BasicChainReplicationServer#preProcessMessage(org.commons.
	 * BasicChainReplicationMessage)
	 */
	@Override
	protected BasicChainReplicationMessage preProcessMessage(BasicChainReplicationMessage receivedMsg) {
		try {
			
			// Seppuku time?
			if ((receivedMsg.getLocalMessageID() > dieAfterNMsg)
					|| (Boolean.parseBoolean(System.getProperty("com.bank.BankServer.tail.new.dieDuringSync", "false")) && receivedMsg.getDestinationQueueName().equalsIgnoreCase(
							"SYNC"))){
				LOG.info(String.format("[%s] Dying during SYNC", this));
				die(); // Seppuku !
			}
				
			long balance = 0l;
			for (BasicChainReplicationMessage h : hist) {
				if (h.getUuid().equalsIgnoreCase(receivedMsg.getUuid())) {
					if (h.equals(receivedMsg) || receivedMsg.getRequest()[0].equalsIgnoreCase("Duplicate Request!")) {
						LOG.info(String.format("[%s]\t\t:\tDuplicate [%s] found in {H}", BankServer.this, receivedMsg));
						receivedMsg.setRequest(new String[]{"Duplicate Request!", receivedMsg.getRequest()[1], receivedMsg.getRequest()[2]});
					} else {
						LOG.info(String.format("[%s]\t\t:\tInconsistent[%s] with {H}", BankServer.this, receivedMsg));
						receivedMsg.setRequest(new String[]{"Inconsistent with {H}", receivedMsg.getRequest()[1], receivedMsg.getRequest()[2], "<BALANCE_MASKED>"}); // Required
						// here
						// only
						// for
						// the
						// tail
					}
					return receivedMsg;
				}
				
				// REQ[0] = REQ/RES
				// REQ[1] = BNK#1
				// REQ[2] = A/C#1
				// REQ[3] = AMT
				// REQ[4] = BNK#2
				// REQ[5] = A/C#2
				try {
					if ((this.bankName.startsWith(h.getRequest()[1]) || this.bankName.startsWith(h.getRequest()[4]))
							&& (h.getRequest()[0].equalsIgnoreCase("Deposit") || h.getRequest()[0].equalsIgnoreCase("Withdraw") || h.getRequest()[0].equalsIgnoreCase("Transfer"))
							&& (h.getRequest()[2].equalsIgnoreCase(receivedMsg.getRequest()[2]) || h.getRequest()[5].equalsIgnoreCase(receivedMsg.getRequest()[2]))) {
						if (h.getRequest()[0].equalsIgnoreCase("Transfer")) {
							if (h.getRequest()[2].equalsIgnoreCase(receivedMsg.getRequest()[2])) {
								balance -= Long.parseLong(h.getRequest()[3]);
							} else if (h.getRequest()[5].equalsIgnoreCase(receivedMsg.getRequest()[2])) {
								balance += Long.parseLong(h.getRequest()[3]);
							}
						} else {
							balance += Long.parseLong(h.getRequest()[3]);
						}
						
					}
				} catch (Exception e) {
				}
			}
			
			String request[] = receivedMsg.getRequest();
			
			if (request[0].equalsIgnoreCase("Balance")) {
				receivedMsg.setRequest(new String[]{request[0], request[1], request[2], "" + balance});
			}
			
			if (request[0].equalsIgnoreCase("Withdraw")) {
				long amount = Math.abs(Long.parseLong(request[3]));
				if (balance >= amount) {
					request[3] = "" + -amount;
					receivedMsg.setRequest(request);
				} else {
					receivedMsg.setRequest(new String[]{"Insufficient funds", request[1], request[2], "" + balance});
				}
			}
			
			if (request[0].equalsIgnoreCase("Deposit")) {
				long amount = Math.abs(Long.parseLong(request[3]));
				request[3] = "" + amount;
				receivedMsg.setRequest(request);
			}
			
			// [Transfer, Citi, F51005D24ECFFE7C70C8BF09D12C5099, 10, TFCU,
			// ABCDE]
			if (request[0].equalsIgnoreCase("Transfer") && (request[4].startsWith(this.bankName) || request[1].startsWith(this.bankName))) {
				long amount = Math.abs(Long.parseLong(request[3]));
				if (request[1].startsWith(this.bankName)) {
					if (balance >= amount) {
						request[3] = "" + amount;
						receivedMsg.setRequest(request);
					} else {
						receivedMsg.setRequest(new String[]{"Insufficient funds", request[1], request[2], "" + balance});
					}
				}
				
				if (request[4].startsWith(this.bankName) && Boolean.parseBoolean(System.getProperty("com.bank.BankServer.dieAfterTransferRecv." + this.getUuid(), "false"))) {
					LOG.info(String.format("[%s]\t:\tDying after receiving transfer request %s", this, receivedMsg));
					this.die();
				}
			}
			
		} catch (Exception e) {
			// System.out.println(e.getMessage());
			receivedMsg.setRequest(new String[]{"Bad Request !"});
		}
		
		return receivedMsg;
	}
	
	/**
	 * Sets the bank name.
	 *
	 * @param bankName
	 *            the new bank name
	 */
	public void setBankName(String bankName) {
		this.bankName = bankName;
	}
	
	/**
	 * Sets the bank topology.
	 *
	 * @param bankTopology
	 *            the bank topology
	 */
	public void setBankTopology(Map<String, List<BasicChainReplicationServer>> bankTopology) {
		this.bankTopology = bankTopology;
		if (null != this.bankTopology)
			super.setServerList(this.bankTopology.get(this.bankName));
	}
	
	/* (non-Javadoc)
	 * @see org.actors.BasicChainReplicationServer#toString()
	 */
	@Override
	public String toString() {
		return "BankServer [bankName=" + bankName + ", uuid=" + uuid + ", state=" + state + ", ipAddrPort=" + ipAddrPort + "]";
	}
	
	/*
	 * (non-Javadoc)
	 * 
	 * @see org.actors.BasicChainReplicationServer#updateHistory(org.commons.
	 * BasicChainReplicationMessage)
	 */
	@Override
	protected boolean updateHistory(BasicChainReplicationMessage receivedMsg) {
		String op = receivedMsg.getRequest()[0];
		if (op.equalsIgnoreCase("Transfer") || op.equalsIgnoreCase("Withdraw") || op.equalsIgnoreCase("Deposit"))
			return true;
		return false;
	}
}
