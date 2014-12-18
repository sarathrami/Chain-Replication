/*
 * 
 */
package com.test;

import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.net.ServerSocket;
import java.net.Socket;

// TODO: Auto-generated Javadoc
/**
 * The Class SimpleServer.
 */
public class SimpleServer {
	
	/**
	 * The main method.
	 *
	 * @param args the arguments
	 * @throws IOException Signals that an I/O exception has occurred.
	 */
	public static void main(String args[]) throws IOException {
		int port = 2002;
		ServerSocket ss = new ServerSocket(port);
		try {
			while (true) {
				Socket s = ss.accept();
				InputStream is = s.getInputStream();
				ObjectInputStream ois = new ObjectInputStream(is);
				testobject to = (testobject) ois.readObject();
				if (to != null) {
					System.out.println(to.id);
				}
				is.close();
				s.close();
			}
		} catch (Exception e) {
			System.out.println(e);
		} finally {
			ss.close();
		}
	}
}
