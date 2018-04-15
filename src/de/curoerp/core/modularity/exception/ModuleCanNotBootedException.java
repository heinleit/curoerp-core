package de.curoerp.core.modularity.exception;

public class ModuleCanNotBootedException extends Exception {
	private static final long serialVersionUID = 1L;

	private String[] modules;
	
	public ModuleCanNotBootedException(String[] modules) {
		this.modules = modules;
	}
	
	@Override
	public String getMessage() {
		return "Module can not be loaded: " + String.join(", ", this.modules);
	}
	
}
