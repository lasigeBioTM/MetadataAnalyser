package pt.blackboard;

import js.co.uk.tuplespace.tuple.SimpleTuple;

/**
 * 
 * 
 *
 */
public class Tuple extends SimpleTuple {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	
	/**
	 * 
	 */
	public Tuple (TupleKey key, TupleValue value) {
		super(key, value);
		
	}

	/**
	 * 
	 */
	public Tuple (TupleKey key, String value) {
		super(key, value);
		
	}


	/**
	 * 
	 */
	public Tuple (String key, String value) {
		super(key, value);
		
	}

}
