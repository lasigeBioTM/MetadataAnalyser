package pt.ma.exception;

/**
 * 
 * 
 *
 */
public class InactiveJobException extends Exception {

	/**
	 * 
	 */
	private static final long serialVersionUID = -4138443916656183918L;

	/**
	 * 
	 */
	public InactiveJobException() {
		
	}
	
	/**
	 * 
	 * @param message
	 */
	public InactiveJobException(String message) {
		super(message);
	}
	
	/**
	 * 
	 * @param cause
	 */
	public InactiveJobException(Throwable cause) {
		super(cause);
	}
	
	/**
	 * 
	 * @param message
	 * @param cause
	 */
	public InactiveJobException(
			String message, 
			Throwable cause) {
		super(message, cause);
	}

	/**
	 * 
	 * @param message
	 * @param cause
	 * @param enableSupression
	 * @param writableStackTrace
	 */
	public InactiveJobException(
			String message, 
			Throwable cause, 
			boolean enableSupression, 
			boolean writableStackTrace) {
		super(message, cause, enableSupression, writableStackTrace);
	}
	
}
