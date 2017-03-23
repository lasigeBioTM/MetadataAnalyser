package pt.ma.protocol;

/**
 * 
 * 
 *
 */
public class UploadFileResult {

	/**
	 * transaction unique identifier
	 */
	private String poolID;
	
	/**
	 * the actual message to be sent to the client
	 */
	private String message;

	/**
	 * the result type for this message: 
	 * 	0 - a digest message; 
	 * 	1 - a response message; 
	 * 	2 - an error message
	 */
	private int type;

	/**
	 * 
	 * @param poolID
	 * @param message
	 */
	public UploadFileResult(
			String poolID, 
			String message) {
		//
		super();
		//
		this.poolID = poolID;
		this.message = message;
		this.type = 0;
		
	}

	/**
	 * 
	 * @param poolID
	 * @param message
	 */
	public UploadFileResult(
			String poolID, 
			String message,
			int type) {
		//
		super();
		//
		this.poolID = poolID;
		this.message = message;
		this.type = type;
		
	}

	/**
	 * 
	 * @return
	 */
	public String getPoolID() {
		return poolID;
	}

	/**
	 * 
	 * @return
	 */
	public String getMessage() {
		return message;
	}

}
