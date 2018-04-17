package de.curoerp.core.logging;


import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.core.config.Configurator;

public class LoggingService {

	private Logger logger;

	public LoggingService() {
		Configurator.setLevel("CuroLogger", Level.INFO);
		this.logger = LogManager.getLogger("CuroLogger");//"curoerp-core");
	}

	/*
	 * Logging => direct and static access
	 */
	public static void error(String log) {
		boot().logger.error(log);
	}

	public static void error(Exception exc) {
		boot().logger.error(exc.getMessage());
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
		boot().logger.info(exc.getMessage());
	}
	
	public static void breaker(String title) {
		LoggingService.info("########## " + title.trim().toUpperCase() + " ##########");
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
