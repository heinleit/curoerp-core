package de.curoerp.core.exception;

import java.util.Arrays;

public class RuntimeTroubleException extends RuntimeException {
	private static final long serialVersionUID = 1L;
	private Exception exc = null;
	
	public RuntimeTroubleException(Exception exception) {
		this.exc = exception;
	}
	
	public String getExceptionName() {
		return this.exc.getClass().getName();
	}
	
	@Override
	public String getMessage() {
		StringBuilder message = new StringBuilder();
		
		message.append(this.getExceptionName());
		message.append(System.lineSeparator());
		
		message.append(this.exc.getMessage());
		message.append(System.lineSeparator());
		
		Arrays.stream(this.exc.getStackTrace()).forEach(item -> {
			message.append(item.toString());
			message.append(System.lineSeparator());
		});
		
		return message.toString();
	}
	
	@Override
	public void printStackTrace() {
		this.exc.printStackTrace();
	}
	
}
