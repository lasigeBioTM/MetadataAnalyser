package pt.blackboard;




/**
 * 
 * 
 *
 */
public abstract class DSL {

	/**
	 * 
	 * @return
	 */
	public static Tuple Tuple (TupleKey key, TupleValue value) {
		return new Tuple(key, value);
		
	}

	/**
	 * 
	 * @return
	 */
	public static Tuple Tuple (TupleKey key, String value) {
		return new Tuple(key, value);
		
	}

	/**
	 * 
	 * @return
	 */
	public static Tuple Tuple (String key, String value) {
		return new Tuple(key, value);
		
	}

	/**
	 * 
	 * 
	 */
	public static MatchTuple MatchTuple (TupleKey key) {
		return new MatchTuple(key);
				
	}
	
	/**
	 * 
	 * 
	 */
	public static MatchTuple MatchTuple (TupleKey key, TupleValue value) {
		return new MatchTuple(key, value);
				
	}

	/**
	 * 
	 * 
	 */
	public static MatchTuple MatchTuple (TupleKey key, String value) {
		return new MatchTuple(key, value);
				
	}

	/**
	 * 
	 * 
	 */
	public static MatchTuple MatchTuple (String key, String value) {
		return new MatchTuple(key, value);
				
	}

}
