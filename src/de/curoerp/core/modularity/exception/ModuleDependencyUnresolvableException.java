package de.curoerp.core.modularity.exception;

public class ModuleDependencyUnresolvableException extends Exception {
	private static final long serialVersionUID = 1L;
	private String dependencyName;
	
	public ModuleDependencyUnresolvableException(String dependencyName) {
		this.dependencyName = dependencyName;
	}
	
	public String getDependencyName() {
		return this.dependencyName;
	}
	
	@Override
	public String getMessage() {
		return "Dependency/-ies '" + this.dependencyName + "' could not resolved:";
	}
}
