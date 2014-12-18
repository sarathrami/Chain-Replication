/*
 * 
 */
package com.test;

import java.io.ObjectOutputStream;
import java.io.OutputStream;
import java.net.Socket;
// TODO: Auto-generated Javadoc

/**
 * The Class SimpleClient.
 */
public class SimpleClient {
	
	/**
	 * The main method.
	 *
	 * @param args the arguments
	 */
	public static void main(String args[]) {
		try {
			while (true) {
				Socket s = new Socket("localhost", 2002);
				OutputStream os = s.getOutputStream();
				ObjectOutputStream oos = new ObjectOutputStream(os);
				testobject to = new testobject(1, "object from client");
				oos.writeObject(to);
				oos.close();
				os.close();
				s.close();
			}
		} catch (Exception e) {
			System.out.println(e);
		}
	}
}
