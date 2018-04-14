package de.curoerp.core.modularity;

public class ModuleDependencyUnresolvableException extends Exception {
	private static final long serialVersionUID = 1L;
	private String dependencyName;
	
	public ModuleDependencyUnresolvableException(String dependencyName) {
		this.dependencyName = dependencyName;
	}
	
	public String getDependencyName() {
		return this.dependencyName;
	}
}
