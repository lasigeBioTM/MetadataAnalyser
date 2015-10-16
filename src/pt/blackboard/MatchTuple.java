package pt.blackboard;

import java.util.Collection;

import js.co.uk.tuplespace.tuple.SimpleTuple;
import js.co.uk.tuplespace.tuple.Tuple;

/**
 * 
 * 
 *
 */
public class MatchTuple {

	/**
	 * 
	 */
	SimpleTuple template;

	/**
	 * 
	 * @param key
	 */
	public MatchTuple (TupleKey key) {
		template = new SimpleTuple();
		template.setData(key, "*");
		
	}

	/**
	 * 
	 * @param key
	 */
	public MatchTuple (String key) {
		template = new SimpleTuple();
		template.setData(key,"*");
		
	}

	/**
	 * 
	 * @param key
	 */
	public MatchTuple (TupleKey key, TupleValue value) {
		template = new SimpleTuple();
		template.setData(key, value);
		
	}

	/**
	 * 
	 * @param key
	 */
	public MatchTuple (TupleKey key, String value) {
		template = new SimpleTuple();
		template.setData(key, value);
		
	}

	/**
	 * 
	 * @param key
	 */
	public MatchTuple (String key, String value) {
		template = new SimpleTuple();
		template.setData(key, value);
		
	}

	/**
	 * 
	 * @param data
	 */
	public MatchTuple (Collection<Object> data) {
		template = new SimpleTuple();
		template.setData(data);
		
	}
	
	/**
	 * 
	 * @return
	 */
	public Tuple getTemplate() {
		return template;
		
	}
	
}
