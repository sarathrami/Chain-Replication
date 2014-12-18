/*
 * 
 */
package com.bank;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.lang.ProcessBuilder.Redirect;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.Callable;

import org.actors.BasicChainReplicationServer;
import org.apache.log4j.Logger;
import org.utilities.CUtils;

// TODO: Auto-generated Javadoc
/**
 * The Class Spawner.
 */
public class Spawner {
	
	// DEBUG PURPOSE ONLY
	/** The debug list. */
	static List<String>	debugList	= Arrays.asList(new String[]{"Citi_1s"});
	/**
	 * The main method.
	 *
	 * @param args
	 *            the arguments
	 */
	public static void main(final String[] args) {
		try {
			// Load master configuration
			Properties p = new Properties(System.getProperties());
			p.load(new FileInputStream(new File(args[0])));
			System.setProperties(p);
			
			// //////////////////////////////////////////////////
			// Start the BankMaster
			// BankMaster master = new BankMaster(null);
			final String[] masterCommand = new String[]{"/usr/lib/jvm/java-7-openjdk-i386/bin/java", "-Dfile.encoding=UTF-8", "-classpath",
					"/home/rami/workspace/ChainReplication/bin:/home/rami/workspace/ChainReplication/apache-activemq-5.9.1/activemq-all-5.9.1.jar", "com.bank.XBankMaster", args[0]};
			List<String> mcommand = Arrays.asList(masterCommand);
			ProcessBuilder mbuilder = new ProcessBuilder(mcommand);
			mbuilder.redirectOutput(Redirect.INHERIT);
			mbuilder.redirectError(Redirect.INHERIT);
			final Process mprocess = mbuilder.start();
			
			// //////////////////////////////////////////////////
			final String[] bankCommand = new String[]{"/usr/lib/jvm/java-7-openjdk-i386/bin/java", "-Dfile.encoding=UTF-8", "-classpath",
					"/home/rami/workspace/ChainReplication/bin:/home/rami/workspace/ChainReplication/apache-activemq-5.9.1/activemq-all-5.9.1.jar", "com.bank.XBankServer", "%s",
					"%s", "%s", "%s"};
			// //////////////////////////////////////////////////
			// Start the servers
			final Map<String, List<BasicChainReplicationServer>> bt = Collections.synchronizedMap(new HashMap<String, List<BasicChainReplicationServer>>());
			String serverMap = System.getProperty("com.bank.BankServer.map", "");
			for (String serverDesc : serverMap.split("/")) {
				final String[] serverDescs = serverDesc.split(",");
				// System.out.println(Arrays.asList(serverDescs));
				List<BasicChainReplicationServer> list = null == bt.get(serverDescs[0]) ? Collections.synchronizedList(new ArrayList<BasicChainReplicationServer>()) : bt
						.get(serverDescs[0]);
				BankServer newBank = new BankServer(serverDescs[0], serverDescs[1], serverDescs[2], null);
				list.add(newBank);
				Collections.sort(list);
				bt.put(serverDescs[0], list);
				
				//Thread.sleep(70);
				LOG.info(String.format("[%s] Starting server [%s/%s/%s]", "Spawner", serverDescs[0], serverDescs[1], serverDescs[2]));
				bankCommand[5] = args[0];
				bankCommand[6] = serverDescs[0];
				bankCommand[7] = serverDescs[1];
				bankCommand[8] = serverDescs[2];
				List<String> bcommand = Arrays.asList(bankCommand);
				ProcessBuilder bbuilder = new ProcessBuilder(bcommand);
				bbuilder.redirectOutput(Redirect.INHERIT);
				bbuilder.redirectError(Redirect.INHERIT);
				
				if (!debugList.contains(serverDescs[1])) {// TODO: REMOVE - ONLY
															// FOR DEBUG
					final Process bprocess = bbuilder.start();
				}
			}
			
			int n = Integer.parseInt(System.getProperty("com.bank.BankClient.count", "1"));
			String[] bankNamesOfClients = System.getProperty("com.bank.BankClient.map", new ArrayList<String>(bt.keySet()).get((int) (bt.keySet().size() * Math.random()))).split(
					",");
			for (int i = 0; i < n; i++) {
				final String clientBankName = bankNamesOfClients.length > i ? bankNamesOfClients[i] : new ArrayList<String>(bt.keySet()).get((int) (bt.keySet().size() * Math
						.random()));
				final BankClient tgtBank = new BankClient(clientBankName, CUtils.getRandomString(Math.random()), bt);
				Callable<?> task = new Callable<String>() {
					@Override
					public String call() throws Exception {
						Thread.sleep(Long.parseLong(System.getProperty("com.bank.BankClient.startupdelay", "3000")));
						tgtBank.start(args);
						return null;
					}
				};
				CUtils.work(task);
			}
			
			while (true); // Wait until eternity. Other clients might be still
							// running.
		} catch (IOException e) {
			throw new RuntimeException("Failed to load config file: ", e);
		} catch (Exception e) {
			throw new RuntimeException("Failed to load config file: ", e);
		} finally {
			LOG.info("Done.");
		}
	}
	/** The Constant LOG. */
	private static final Logger	LOG	= Logger.getLogger(Spawner.class);
}
