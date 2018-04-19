package de.curoerp.core.modularity.exception;

public class ModuleControllerClassException extends Exception {
	private static final long serialVersionUID = 1L;

	public ModuleControllerClassException(String fqn) {
		super(fqn);
	}
}
