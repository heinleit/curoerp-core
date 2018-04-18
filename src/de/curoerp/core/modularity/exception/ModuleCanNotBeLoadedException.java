package de.curoerp.core.modularity.exception;

public class ModuleCanNotBeLoadedException extends Exception {
	private static final long serialVersionUID = 1L;
	
	public ModuleCanNotBeLoadedException(String msg) {
		super(msg);
	}
	
}