package pt.blackboard;



/**
 * 
 * 
 *
 */
public interface IBlackboard {
	
	/**
	 * 
	 * @return
	 */
	public String getName ();
	
	/**
	 * 
	 * @return
	 */
	public int getSize();
	
	/**
	 * Attempts to match a tuple in the space to the supplied template. There 
	 * is no timeout so the operation will block until a match occurs. The 'take' 
	 * operation is destructive in that a matched tuple is removed from the space.
	 * 
	 * @param tuple
	 * @return
	 */
	public Tuple get(MatchTuple tuple);
	
	/**
	 * Attempts to match a tuple in the space to the supplied template. The operation 
	 * will block for up to timeOut milliseconds. The 'take' operation is destructive in 
	 * that a matched tuple is removed from the space.
	 * 
	 * @param tuple
	 * @param timeOut
	 * @return
	 */
	public Tuple get(MatchTuple tuple, Long timeOut);
	
	/**
	 * Attempts to match a tuple in the space to the supplied template. The operation 
	 * will block indefinitely. Similar to the 'get' operations except that 'gets' removes 
	 * the tuple and read does not.
	 * 
	 * @param tuple
	 * @return
	 */
	public Tuple read(MatchTuple tuple);

	/**
	 * Attempts to match a tuple in the space to the supplied template. The operation will block 
	 * for up to timeOut milliseconds. Similar to the 'get' operations except that 'gets' removes 
	 * the tuple and read does not.
	 * 
	 * @param tuple
	 * @param timeOut
	 * @return
	 */
	public Tuple read(MatchTuple tuple, Long timeOut);

	
	/**
	 * Puts the tuple into the space with no timeout.  
	 * 
	 * @param tuple
	 */
	public void put(Tuple tuple);

	/**
	 * Puts the tuple into the space and the tuple will be purged 
	 * after it has been in the space for timeOut milliseconds.
	 * 
	 * @param tuple
	 * @param timeout
	 */
	public void put(Tuple tuple, int timeout);
	
}
