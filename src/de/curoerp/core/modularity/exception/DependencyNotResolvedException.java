package de.curoerp.core.modularity.exception;

public class DependencyNotResolvedException extends Exception {
	private static final long serialVersionUID = -6058327143940032950L;
	
	public DependencyNotResolvedException(String msg) {
		super(msg);
	}

}
