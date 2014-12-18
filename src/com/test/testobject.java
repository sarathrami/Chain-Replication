/*
 * 
 */
package com.test;

import java.io.Serializable;
// TODO: Auto-generated Javadoc

/**
 * The Class testobject.
 */
class testobject implements Serializable {
	
	/** The value. */
	int		value;
	
	/** The id. */
	String	id;
	
	/**
	 * Instantiates a new testobject.
	 *
	 * @param v the v
	 * @param s the s
	 */
	public testobject(int v, String s) {
		this.value = v;
		this.id = s;
	}
}
