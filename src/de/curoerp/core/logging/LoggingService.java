package de.curoerp.core.logging;

public class LoggingService {
	
	public static Logging DefaultLogging;

	/*
	 * Logging => direct and static access
	 */
	public static void error(String log) {
		DefaultLogging.log(LoggingLevel.ERROR, log);
	}

	public static void error(Exception exc) {
		LoggingService.logException(LoggingLevel.ERROR, exc);
	}

	public static void warn(String log) {
		DefaultLogging.log(LoggingLevel.WARN, log);
	}

	public static void warn(Exception exc) {
		LoggingService.logException(LoggingLevel.WARN, exc);
	}

	public static void info(String log) {
		DefaultLogging.log(LoggingLevel.INFO, log);
	}

	public static void info(Exception exc) {
		LoggingService.logException(LoggingLevel.INFO, exc);
	}

	public static void breaker(String title) {
		LoggingService.info("########## " + title.trim().toUpperCase() + " ##########");
	}

	private static void logException(LoggingLevel level, Exception exc) {
		DefaultLogging.log(level, exc.getClass().getName() + ": " + exc.getMessage());
		for (StackTraceElement stack : exc.getStackTrace()) {
			DefaultLogging.log(level, "\t" + stack.getClassName() + "." + stack.getMethodName() + "(" + stack.getFileName() + ":" + stack.getLineNumber() + ")");
		}
	}

}
