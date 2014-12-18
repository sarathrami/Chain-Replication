/*
 * 
 */
package org.utilities;

import java.math.BigInteger;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

// TODO: Auto-generated Javadoc
/**
 * The Class CUtils.
 */
public class CUtils {
	
	/**
	 * Work.
	 *
	 * @param task
	 *            the task
	 * @return the object
	 */
	public static Object work(Callable<?> task) {
		Future<?> futureTask = es.submit(task);
		return futureTask;
	}
	
	/**
	 * Gets the random string.
	 *
	 * @param rndSeed the rnd seed
	 * @return the random string
	 */
	public static String getRandomString(double rndSeed) {
		MessageDigest m;
		try {
			m = MessageDigest.getInstance("MD5");
			byte[] data = ("" + rndSeed).getBytes();
			m.update(data, 0, data.length);
			BigInteger i = new BigInteger(1, m.digest());
			return String.format("%1$032X", i);
		} catch (NoSuchAlgorithmException e) {
		}
		return "" + rndSeed;
	}
	
	/**
	 * Gets the random number.
	 *
	 * @param s the s
	 * @return the random number
	 * @throws Exception the exception
	 */
	public static int getRandomNumber(String s) throws Exception {
		MessageDigest m;
		try {
			m = MessageDigest.getInstance("MD5");
			byte[] data = s.getBytes();
			m.update(data, 0, data.length);
			BigInteger i = new BigInteger(1, m.digest());
			return i.intValue();
		} catch (NoSuchAlgorithmException e) {
		}
		throw new Exception("Cannot generate random number");
	}
	
	/** The Constant es. */
	private final static ExecutorService	es	= Executors.newCachedThreadPool();
	
	/**
	 * Instantiates a new c utils.
	 */
	private CUtils() {
	}
}
