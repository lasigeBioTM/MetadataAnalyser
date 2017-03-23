package pt.blackboard.protocol;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import pt.blackboard.protocol.enums.ComponentList;
import pt.ma.component.log.LogType;
import pt.ma.metadata.MetaClass;

/**
 * 
 * 
 *
 */
public class LogIngoing extends MessageProtocol {

	/**
	 * 
	 */
	private String body;
	
	/**
	 * 
	 */
	private Throwable throwable;
	
	/**
	 * 
	 */
	private LogType logType;

	/**
	 * 
	 * @param uniqueID
	 * @param body
	 * @param logType
	 * @param target
	 */
	public LogIngoing( 
			String body, 
			LogType logType,
			ComponentList target) {
		super(null, target);
		//
		this.body = body;
		this.logType = logType;
		
	}

	/**
	 * 
	 * @return
	 */
	public String getBody() {
		return body;
	}

	/**
	 * 
	 * @return
	 */
	public Throwable getThrowable() {
		return throwable;
	}

	/**
	 * 
	 * @return
	 */
	public LogType getLogType() {
		return logType;
	}

	/**
	 * 
	 * @param throwable
	 */
	public void setThrowable(Throwable throwable) {
		this.throwable = throwable;
	}

	
}
