package de.curoerp.core.logging;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;

public class Logging {

	private int loggingLevelIndex;

	public Logging(LoggingLevel level) {
		this.setLoggingLevel(level);
	}
	
	public void setLoggingLevel(LoggingLevel level) {
		this.loggingLevelIndex = Arrays.asList(LoggingLevel.values()).indexOf(level);
	}
	
	private final int MAX_LEVEL_LENGTH = Arrays.stream(LoggingLevel.values()).map(l -> l.toString().length()).max((o1, o2) -> o1 - o2).get();

	public void log(LoggingLevel level, String msg) {
		if(Arrays.asList(LoggingLevel.values()).indexOf(level) > this.loggingLevelIndex) {
			//not for logging!
			return;
		}

		String[] msgs = msg.split("\n");

		LocalDateTime currentTime = LocalDateTime.now();
		
		for (String string : msgs) {
			System.out.println(currentTime.format(DateTimeFormatter.ofPattern("YYYY-MM-dd HH:mm:ss")) + " " + 
					String.format("%1$" + (MAX_LEVEL_LENGTH + 2) + "s", "[" + level.toString() + "]" ) + ": " +
					string);
		}
	}
}
