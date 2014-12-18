/*
 * 
 */
package com.test;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import javax.jms.JMSException;

import org.actors.BasicChainReplicationServer;
import org.apache.log4j.Logger;
import org.commons.BasicChainReplicationMessage;
import org.commons.Comm;
import org.commons.CommType;
import org.commons.IComm;

import com.bank.BankServer;

// TODO: Auto-generated Javadoc
/**
 * The Class BankClient.
 */
public class TestMain {
	
	/**
	 * The main method.
	 *
	 * @param args
	 *            the arguments
	 * @throws Exception
	 *             the exception
	 */
	public static void main(String[] args) throws Exception {
		try {
			// Load master configuration
			Properties p = new Properties(System.getProperties());
			p.load(new FileInputStream(new File(args[0])));
			System.setProperties(p);
			
			BasicChainReplicationServer serverArray[][] = {
					{new BankServer("Citi", "Citi#1", "localhost:61616", null),
							new BankServer("Citi", "Citi#2", "localhost:61616", null),
							new BankServer("Citi", "Citi#3", "localhost:61616", null)},
					
					{new BankServer("IOB", "IOB#1", "localhost:61616", null),
							new BankServer("IOB", "IOB#2", "localhost:61616", null)}};
			
			final List<BasicChainReplicationServer> SERVER_LIST_0 = new ArrayList<BasicChainReplicationServer>(
					Arrays.asList(serverArray[0]));
			final List<BasicChainReplicationServer> SERVER_LIST_1 = new ArrayList<BasicChainReplicationServer>(
					Arrays.asList(serverArray[1]));
			final List<BasicChainReplicationServer> SERVER_LIST_U = new ArrayList<BasicChainReplicationServer>(
					SERVER_LIST_0);
			SERVER_LIST_U.addAll(SERVER_LIST_1);
			Map<String, List<BasicChainReplicationServer>> bt = new HashMap<String, List<BasicChainReplicationServer>>();
			bt.put("Citi", SERVER_LIST_0);
			bt.put("IOB", SERVER_LIST_1);
			
			for (BasicChainReplicationServer s : SERVER_LIST_U) {
				((BankServer) s).setBankTopology(bt);
			}
			
			for (BasicChainReplicationServer s : SERVER_LIST_U) {
				s.start();
			}
			// MAKE REQUESTS
			// Thread.sleep(300);
			// sendAndReceiveMsg("Citi#1", new String[]{"Withdraw", "Citi",
			// "#12345", "100"});
			// Kill server
			// SERVER_LIST_0.remove(1);
			// for (BasicChainReplicationServer s : SERVER_LIST_0) {
			// s.setServerList(SERVER_LIST_0);
			// }
			Thread.sleep(1000);
			sendAndReceiveMsg("Citi#1", new String[]{"Deposit", "Citi", "#12345", "3189"});
			Thread.sleep(1000);
			sendAndReceiveMsg("Citi#1", new String[]{"Balance", "Citi", "#12345"});
			Thread.sleep(300);
			sendAndReceiveMsg("Citi#1", new String[]{"Withdraw", "Citi", "#12345", "100"});
			Thread.sleep(1000);
			sendAndReceiveMsg("Citi#1", new String[]{"Balance"});
			Thread.sleep(1000);
			sendAndReceiveMsg("Citi#1", new String[]{"Transfer", "Citi", "#12345", "3000", "IOB", "#ABCDE"});
			Thread.sleep(1000);
			sendAndReceiveMsg("Citi#1", new String[]{"Balance", "Citi", "#12345"});
			Thread.sleep(1000);
			sendAndReceiveMsg("IOB#1", new String[]{"Balance", "IOB", "#ABCDE"});
			
			Thread.sleep(1000);
			sendAndReceiveMsg("Citi#1", new String[]{"Transfer", "Citi", "#12345", "10", "Citi", "#876"});
			Thread.sleep(1000);
			sendAndReceiveMsg("Citi#1", new String[]{"Balance", "Citi", "#876"});
			Thread.sleep(1000);
			sendAndReceiveMsg("Citi#1", new String[]{"Balance", "Citi", "#12345"});
		} catch (IOException e) {
			throw new RuntimeException("Failed to load config file: ", e);
		} catch (Exception e) {
			e.printStackTrace();
		} finally {
			LOG.info("Done.");
			System.exit(0);
		}
	}
	
	/**
	 * Send and receive msg.
	 *
	 * @param target
	 *            the target
	 * @param req
	 *            the req
	 * @throws JMSException
	 *             the JMS exception
	 * @throws InterruptedException
	 *             the interrupted exception
	 */
	private static void sendAndReceiveMsg(String target, String[] req) throws JMSException, InterruptedException {
		IComm client = new Comm(target, "localhost:61616", CommType.COMM_UNRELIABLE);
		BasicChainReplicationMessage msg = new BasicChainReplicationMessage(req, getNewUID(), "CLIENT_1",
				"localhost:61616");
		LOG.info("SENT	: " + msg + " to " + target);
		client.sendMsg(msg);
		client.close();
		
		client = new Comm("CLIENT_1", "localhost:61616", CommType.COMM_UNRELIABLE);
		LOG.info("RECEIVED : " + client.receiveMsg(0));
		client.close();
	}

	/**
	 * Gets the new uid.
	 *
	 * @return the new uid
	 */
	private static String getNewUID() {
		//return "UUID_" + Math.random();
		return "@@@@@@@@@@@@@@@@@@";
	}
	
	/** The Constant LOG. */
	private static final Logger	LOG	= Logger.getLogger(TestMain.class);
	
}
