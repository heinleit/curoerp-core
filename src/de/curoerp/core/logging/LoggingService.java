package de.curoerp.core.logging;


import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class LoggingService {

	private Logger logger;

	public LoggingService() {
		this.logger = LogManager.getLogger("CuroLogger");//"curoerp-core");
	}

	/*
	 * Logging => direct and static access
	 */
	public static void error(String log) {
		boot().logger.error(log);
	}

	public static void error(Exception exc) {
		boot().logger.error(exc);
	}

	public static void warn(String log) {
		boot().logger.warn(log);
	}

	public static void warn(Exception exc) {
		boot().logger.warn(exc);
	}

	public static void info(String log) {
		boot().logger.info(log);
	}

	public static void info(Exception exc) {
		boot().logger.info(exc);
	}



	/*
	 * Instance
	 */

	private static LoggingService instance;

	public static LoggingService boot() {
		if(LoggingService.instance == null) {
			LoggingService.instance = new LoggingService();
		}

		return LoggingService.instance;
	}

}
