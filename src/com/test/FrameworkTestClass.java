/*
 * 
 */
package com.test;

import java.io.File;
import java.io.FileInputStream;
import java.util.Properties;

import javax.jms.Connection;
import javax.jms.DeliveryMode;
import javax.jms.Destination;
import javax.jms.ExceptionListener;
import javax.jms.JMSException;
import javax.jms.Message;
import javax.jms.MessageConsumer;
import javax.jms.MessageProducer;
import javax.jms.Session;
import javax.jms.TextMessage;

import org.apache.activemq.ActiveMQConnectionFactory;

// TODO: Auto-generated Javadoc
/**
 * Hello world!.
 */
public class FrameworkTestClass {
	
	/**
	 * The Class HelloWorldConsumer.
	 */
	public static class HelloWorldConsumer implements Runnable, ExceptionListener {
		
		/* (non-Javadoc)
		 * @see javax.jms.ExceptionListener#onException(javax.jms.JMSException)
		 */
		@Override
		public synchronized void onException(JMSException ex) {
			System.out.println("JMS Exception occured.  Shutting down client.");
		}
		
		/* (non-Javadoc)
		 * @see java.lang.Runnable#run()
		 */
		@Override
		public void run() {
			try {
				
				// Create a ConnectionFactory
				ActiveMQConnectionFactory connectionFactory = new ActiveMQConnectionFactory(CONNECTION_STRING);
				
				// Create a Connection
				Connection connection = connectionFactory.createConnection();
				connection.start();
				
				connection.setExceptionListener(this);
				
				// Create a Session
				Session session = connection.createSession(false, Session.AUTO_ACKNOWLEDGE);
				
				// Create the destination (Topic or Queue)
				Destination destination = session.createQueue(QUEUE);
				
				// Create a MessageConsumer from the Session to the Topic or
				// Queue
				MessageConsumer consumer = session.createConsumer(destination);
				
				// Wait for a message
				Message message = consumer.receive(0);
				
				if (message instanceof TextMessage) {
					TextMessage textMessage = (TextMessage) message;
					String text = textMessage.getText();
					System.out.println("Received: " + text);
				} else {
					System.out.println("Received: " + message);
				}
				
				consumer.close();
				session.close();
				connection.close();
			} catch (Exception e) {
				System.out.println("Caught: " + e);
				e.printStackTrace();
			}
		}
	}
	
	/**
	 * The Class HelloWorldProducer.
	 */
	public static class HelloWorldProducer implements Runnable {
		
		/* (non-Javadoc)
		 * @see java.lang.Runnable#run()
		 */
		@Override
		public void run() {
			try {
				// Create a ConnectionFactory
				ActiveMQConnectionFactory connectionFactory = new ActiveMQConnectionFactory(CONNECTION_STRING);
				
				// Create a Connection
				Connection connection = connectionFactory.createConnection();
				connection.start();
				
				// Create a Session
				Session session = connection.createSession(false, Session.AUTO_ACKNOWLEDGE);
				
				// Create the destination (Topic or Queue)
				Destination destination = session.createQueue(QUEUE);
				
				// Create a MessageProducer from the Session to the Topic or
				// Queue
				MessageProducer producer = session.createProducer(destination);
				producer.setDeliveryMode(DeliveryMode.NON_PERSISTENT);
				
				// Create a messages
				String text = "Hello world! From: " + Thread.currentThread().getName() + " : " + this.hashCode();
				TextMessage message = session.createTextMessage(text);
				
				// Tell the producer to send the message
				System.out.println("Sent message: " + message.hashCode() + " : " + Thread.currentThread().getName());
				producer.send(message);
				
				// Clean up
				session.close();
				connection.close();
			} catch (Exception e) {
				System.out.println("Caught: " + e);
				e.printStackTrace();
			}
		}
	}
	
	/**
	 * The main method.
	 *
	 * @param args the arguments
	 * @throws Exception the exception
	 */
	public static void main(String[] args) throws Exception {
		thread(new HelloWorldProducer(), false);
		thread(new HelloWorldProducer(), false);
		thread(new HelloWorldConsumer(), false);
		Thread.sleep(1000);
		thread(new HelloWorldConsumer(), false);
		thread(new HelloWorldProducer(), false);
		thread(new HelloWorldConsumer(), false);
		thread(new HelloWorldProducer(), false);
	}
	
	/**
	 * Thread.
	 *
	 * @param runnable the runnable
	 * @param daemon the daemon
	 */
	public static void thread(Runnable runnable, boolean daemon) {
		Thread brokerThread = new Thread(runnable);
		brokerThread.setDaemon(daemon);
		brokerThread.start();
	}
	
	/** The Constant CONNECTION_STRING. */
	private static final String	CONNECTION_STRING	= "failover://(tcp://localhost:61616)?initialReconnectDelay=2000&maxReconnectAttempts=90";
	
	/** The Constant QUEUE. */
	private static final String	QUEUE				= "TEST.Q";
	
	{
		try {
			Properties p = new Properties(System.getProperties());
			p.load(new FileInputStream(new File("log4j.properties")));
			System.setProperties(p);
		} catch (Exception e) {
			throw new RuntimeException("Failed to load config file: ", e);
		}
	}
}
