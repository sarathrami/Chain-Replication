/*
 * 
 */
package org.commons;

import java.io.Serializable;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;

import javax.jms.Connection;
import javax.jms.DeliveryMode;
import javax.jms.Destination;
import javax.jms.JMSException;
import javax.jms.MessageConsumer;
import javax.jms.MessageProducer;
import javax.jms.ObjectMessage;
import javax.jms.Queue;
import javax.jms.QueueBrowser;
import javax.jms.Session;

import org.apache.activemq.ActiveMQConnection;
import org.apache.activemq.ActiveMQConnectionFactory;
import org.apache.activemq.RedeliveryPolicy;
import org.apache.activemq.command.ActiveMQObjectMessage;

// TODO: Auto-generated Javadoc
/**
 * Hello world!.
 */
public class Comm implements Constants, IComm {
	
	/** The Constant PROTOCOL. */
	private static final String					PROTOCOL					= "tcp";
	
	/** The Constant ACKNOWLEDGE_TYPE. */
	private static final int					DEFAULT_ACKNOWLEDGE_TYPE	= CommType.COMM_RELIABLE.getValue();
	
	/** The connection string. */
	private String								connectionString			= "UKNOWN_CONNECTION_STRING";
	
	/** The queue. */
	private String								queue						= "UNKNOWN_QUEUE";
	
	/** The connection. */
	private Connection							connection					= null;
	
	/** The session. */
	private Session								session						= null;
	
	/** The destination. */
	private Destination							destination					= null;
	
	/** The consumer. */
	private MessageConsumer						consumer					= null;
	
	/** The producer. */
	private MessageProducer						producer					= null;
	
	/** The pay load to vehicle map. */
	private Map<Serializable, ObjectMessage>	payLoadToVehicleMap			= Collections.synchronizedMap(new HashMap<Serializable, ObjectMessage>());
	
	/** The expiration. */
	private long								expiration					= RETRANSMISSION_DELAY;
	/**
	 * Instantiates a new comm.
	 *
	 * @param uuid
	 *            the uuid
	 * @param iPAddrPort
	 *            the i p addr port
	 * @throws JMSException
	 *             the JMS exception
	 */
	public Comm(String uuid, String iPAddrPort) throws JMSException {
		super();
		this.queue = String.format(QUEUE, iPAddrPort, uuid);
		this.connectionString = String.format(CONNECTION_STRING, PROTOCOL, iPAddrPort);
		this.setup(DEFAULT_ACKNOWLEDGE_TYPE);
	}
	
	/**
	 * Instantiates a new comm.
	 *
	 * @param uuid            the uuid
	 * @param iPAddrPort            the i p addr port
	 * @param expiryTime the expiry time
	 * @throws JMSException             the JMS exception
	 */
	public Comm(String uuid, String iPAddrPort, long expiryTime) throws JMSException {
		super();
		this.expiration = expiryTime;
		this.queue = String.format(QUEUE, iPAddrPort, uuid);
		this.connectionString = String.format(CONNECTION_STRING, PROTOCOL, iPAddrPort);
		this.setup(DEFAULT_ACKNOWLEDGE_TYPE);
	}
	
	/**
	 * Instantiates a new comm.
	 *
	 * @param qName
	 *            the q name
	 * @param iPAddrPort
	 *            the i p addr port
	 * @param commType
	 *            the ack type
	 * @throws JMSException
	 *             the JMS exception
	 */
	public Comm(String qName, String iPAddrPort, CommType commType) throws JMSException {
		super();
		this.queue = String.format(QUEUE, iPAddrPort, qName);
		this.connectionString = String.format(CONNECTION_STRING, PROTOCOL, iPAddrPort);
		this.setup(commType.getValue());
	}
	
	/*
	 * (non-Javadoc)
	 * 
	 * @see org.commons.IComm#close()
	 */
	@Override
	public void close() throws JMSException {
		// if (null != destination)
		// ((ActiveMQQueue)destination).close();
		if (null != producer)
			producer.close();
		if (null != consumer)
			consumer.close();
		if (null != session)
			session.close();
		if (null != connection)
			connection.close();
	}
	
	/**
	 * Discard messages in queue.
	 *
	 * @return true, if successful
	 */
	@SuppressWarnings("unchecked")
	private boolean discardMessagesInQueue() {
		QueueBrowser browser = null;
		try {
			browser = session.createBrowser((Queue) destination);
			Enumeration<BasicChainReplicationMessage> enumeration = browser.getEnumeration();
			while (enumeration.hasMoreElements()) {
				enumeration.nextElement();
				receiveMsg(RETRANSMISSION_DELAY);
			}
			payLoadToVehicleMap.clear();
		} catch (Exception e) {
			e.printStackTrace();
		} finally {
			try {
				browser.close();
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
		return false;
	}
	
	/**
	 * Queue contains message.
	 *
	 * @param msg
	 *            the msg
	 * @return true, if successful
	 */
	@SuppressWarnings("unchecked")
	private boolean queueContainsMessage(javax.jms.Message msg) {
		QueueBrowser browser = null;
		try {
			browser = session.createBrowser((Queue) destination);
			Enumeration<BasicChainReplicationMessage> enumeration = browser.getEnumeration();
			while (enumeration.hasMoreElements()) {
				javax.jms.Message message = (javax.jms.Message) enumeration.nextElement();
				if (message.getJMSMessageID().equalsIgnoreCase(msg.getJMSMessageID()))
					return true;
			}
			
		} catch (Exception e) {
			e.printStackTrace();
		} finally {
			try {
				browser.close();
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
		return false;
	}
	
	/*
	 * (non-Javadoc)
	 * 
	 * @see org.commons.IComm#receiveMsg(long)
	 */
	@Override
	public synchronized Serializable receiveMsg(long timeout) throws JMSException {
		
		Serializable msg = null;
		ObjectMessage objectMessage = null;
		try {
			// Wait for a message
			objectMessage = (ObjectMessage) consumer.receive(timeout);
			
			if (null != objectMessage) {
				msg = objectMessage.getObject();
				payLoadToVehicleMap.put(msg, objectMessage);
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
		return msg;
	}
	
	/*
	 * (non-Javadoc)
	 * 
	 * @see org.commons.IComm#sendAck(org.commons.BasicChainReplicationMessage)
	 */
	@Override
	public synchronized void sendAck(BasicChainReplicationMessage msg) {
		try {
			if (payLoadToVehicleMap.containsKey(msg)) {
				payLoadToVehicleMap.get(msg).acknowledge();
				payLoadToVehicleMap.remove(msg);
			}
		} catch (JMSException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	/*
	 * (non-Javadoc)
	 * 
	 * @see org.commons.IComm#sendMsg(java.io.Serializable)
	 */
	@Override
	public synchronized void sendMsg(Serializable msg) throws JMSException {
		try {
			// Create a MessageProducer from the Session to the Topic or
			// Queue
			producer.setDeliveryMode(DeliveryMode.NON_PERSISTENT);
			// Send message
			final ActiveMQObjectMessage objectMessage = (ActiveMQObjectMessage) session.createObjectMessage(msg);
			objectMessage.setExpiration(expiration);
			producer.send(objectMessage);
			payLoadToVehicleMap.put(msg, objectMessage);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	/**
	 * Sets the up.
	 *
	 * @param ackType
	 *            the new up
	 * @throws JMSException
	 *             the JMS exception
	 */
	private void setup(int ackType) throws JMSException {
		// Create a ConnectionFactory
		ActiveMQConnectionFactory connectionFactory = new ActiveMQConnectionFactory(this.connectionString);
		connection = connectionFactory.createConnection();
		connection.start();
		// if (Session.CLIENT_ACKNOWLEDGE == ackType) {
		RedeliveryPolicy policy = ((ActiveMQConnection) connection).getRedeliveryPolicy();
		policy.setInitialRedeliveryDelay(100);
		policy.setBackOffMultiplier(1.00317);
		policy.setUseExponentialBackOff(true);
		policy.setMaximumRedeliveries(Integer.MAX_VALUE);
		policy.setUseCollisionAvoidance(true);
		policy.setCollisionAvoidancePercent((short) 13);
		// }
		session = connection.createSession(false, ackType);
		// Create the destination (Topic or Queue)
		destination = session.createQueue(this.queue);
		consumer = session.createConsumer(destination);
		producer = session.createProducer(destination);
	}
	
	/*
	 * (non-Javadoc)
	 * 
	 * @see java.lang.Object#toString()
	 */
	@Override
	public String toString() {
		// return "Comm [connectionString=" + connectionString + ", queue=" +
		// queue + "]";
		return "Comm [queue=" + queue + "]";
	}
	
	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.commons.IComm#waitForAck(org.commons.BasicChainReplicationMessage,
	 * long)
	 */
	@Override
	public synchronized void waitForAck(BasicChainReplicationMessage msg, long timeout) throws JMSException {
		if (payLoadToVehicleMap.containsKey(msg)) {
			long timeoutAt = System.currentTimeMillis() + timeout;
			while (queueContainsMessage(payLoadToVehicleMap.get(msg)) && System.currentTimeMillis() < timeoutAt)
				Thread.yield();
			if (queueContainsMessage(payLoadToVehicleMap.get(msg))) {
				// Self consume message to empty queue, throw exception
				discardMessagesInQueue();
				throw new JMSException("Unresponsive node found.");
			}
			
			payLoadToVehicleMap.remove(msg);
		}
	}
}
