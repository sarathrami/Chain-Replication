/*
 * 
 */
package org.commons;

import java.io.Serializable;

import javax.jms.JMSException;

// TODO: Auto-generated Javadoc
/**
 * The Interface IComm.
 */
public interface IComm {
	
	/**
	 * Close.
	 *
	 * @throws JMSException
	 *             the JMS exception
	 */
	void close() throws JMSException;
	
	/**
	 * Receive msg.
	 *
	 * @param timeout
	 *            the timeout
	 * @return the serializable
	 * @throws JMSException
	 *             the JMS exception
	 */
	Serializable receiveMsg(long timeout) throws JMSException;
	
	/**
	 * Send ack.
	 *
	 * @param msg
	 *            the msg
	 */
	void sendAck(BasicChainReplicationMessage msg);
	
	/**
	 * Send msg.
	 *
	 * @param msg
	 *            the msg
	 * @throws JMSException
	 *             the JMS exception
	 */
	void sendMsg(Serializable msg) throws JMSException;
	
	/**
	 * Wait for ack.
	 *
	 * @param msg
	 *            the msg
	 * @param timeout
	 *            the timeout
	 * @throws JMSException
	 *             the JMS exception
	 */
	void waitForAck(BasicChainReplicationMessage msg, long timeout) throws JMSException;
}
