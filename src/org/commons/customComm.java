/*
 * 
 */
package org.commons;

import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.OutputStream;
import java.io.Serializable;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.Callable;
import java.util.concurrent.LinkedBlockingQueue;

import javax.jms.JMSException;

import org.utilities.CUtils;

// TODO: Auto-generated Javadoc
class Sender {
	String	iPAddrPort;
	int		port;
}

/**
 * The Class customComm.
 */
public class customComm implements IComm {
	
	/** The name. */
	private String										name			= null;
	
	/** The port. */
	private int											port			= -1;
	
	/** The ss. */
	private ServerSocket								ss				= null;
	
	/** The recved msg list. */
	private BlockingQueue<BasicChainReplicationMessage>	recvedMsgList	= new LinkedBlockingQueue<BasicChainReplicationMessage>();
	
	/** The i p addr port. */
	private String										iPAddrPort		= null;
	
	/** The com type. */
	private Object										comType;
	
	/**
	 * Instantiates a new custom comm.
	 *
	 * @param uuid the uuid
	 * @param iPAddrPort the i p addr port
	 * @throws JMSException the JMS exception
	 */
	public customComm(String uuid, String iPAddrPort) throws JMSException {
		super();
		try {
			this.name = uuid + "_CUSTOM_COMM";
			// 1024–65535 range
			this.port = 1024 + Math.abs(CUtils.getRandomNumber(this.name)) % (65535 - 1024);
			this.iPAddrPort = iPAddrPort.split(":")[0] + ":" + port;
			this.comType = CommType.COMM_RELIABLE;
		} catch (Exception e) {
			throw new JMSException(e.getMessage());
		}
	}
	
	/**
	 * Instantiates a new custom comm.
	 *
	 * @param uuid the uuid
	 * @param iPAddrPort the i p addr port
	 * @param commType the comm type
	 * @throws JMSException the JMS exception
	 */
	public customComm(String uuid, String iPAddrPort, CommType commType) throws JMSException {
		super();
		try {
			this.name = uuid + "_CUSTOM_COMM";
			// 1024–65535 range
			this.port = 1024 + Math.abs(CUtils.getRandomNumber(this.name)) % (65535 - 1024);
			this.iPAddrPort = iPAddrPort.split(":")[0] + ":" + port;
			this.comType = comType;
		} catch (Exception e) {
			throw new JMSException(e.getMessage());
		}
	}
	
	/* (non-Javadoc)
	 * @see org.commons.IComm#close()
	 */
	@Override
	public void close() throws JMSException {
		// TODO Auto-generated method stub
		
	}
	
	/* (non-Javadoc)
	 * @see org.commons.IComm#receiveMsg(long)
	 */
	@Override
	public Serializable receiveMsg(long timeout) throws JMSException {
		try {
			synchronized (this) {
				if (null == this.ss) {
					
					customComm.this.ss = new ServerSocket(port);
					
					Callable<Integer> sendIAmAliveMsgToMasterThread = new Callable<Integer>() {
						@Override
						public Integer call() {
							while (true) {
								try {
									final Socket s = ss.accept();
									final InputStream is = s.getInputStream();
									final ObjectInputStream ois = new ObjectInputStream(is);
									Object rObject = ois.readObject();
									if (null == rObject || !(rObject instanceof BasicChainReplicationMessage))
										continue;
									BasicChainReplicationMessage to = (BasicChainReplicationMessage) rObject;
									recvedMsgList.add(to);
									ois.close();
									is.close();
								} catch (Exception e) {
									e.printStackTrace();
									System.exit(-1);
								}
							}
						}
					};
					
					CUtils.work(sendIAmAliveMsgToMasterThread);
				}
			}
			return recvedMsgList.take();
		} catch (InterruptedException e) {
		} catch (Exception e) {
			try {
				if (null != ss)
					ss.close();
			} catch (IOException e2) {
				throw new JMSException(e.getMessage());
			}
			throw new JMSException(e.getMessage());
		}
		return null;
	}
	
	/* (non-Javadoc)
	 * @see org.commons.IComm#sendAck(org.commons.BasicChainReplicationMessage)
	 */
	@Override
	public void sendAck(BasicChainReplicationMessage msg) {
		throw new UnsupportedOperationException();
	}
	
	/* (non-Javadoc)
	 * @see org.commons.IComm#sendMsg(java.io.Serializable)
	 */
	@Override
	public void sendMsg(Serializable msg) throws JMSException {
		int n = Integer.parseInt(System.getProperty("com.bank.BankClient.count", "1")) * 7;
		while (true) {
			try {
				Thread.sleep(500);
				Socket s = new Socket(iPAddrPort.split(":")[0], port);
				try {
					OutputStream os = s.getOutputStream();
					ObjectOutputStream oos = new ObjectOutputStream(os);
					oos.writeObject(msg);
					oos.close();
					os.close();
					break;
				} finally {
					s.close();
				}
			} catch (Exception e) {
				if (--n < 0)
					throw new JMSException(e.getMessage());
				else {
					Thread.yield();
					try {
						Thread.sleep(50 * n);
					} catch (InterruptedException e1) {
					}
				}
			}
		}
	}
	
	/* (non-Javadoc)
	 * @see org.commons.IComm#waitForAck(org.commons.BasicChainReplicationMessage, long)
	 */
	@Override
	public void waitForAck(BasicChainReplicationMessage msg, long timeout) throws JMSException {
		throw new UnsupportedOperationException();
	}
	
}
