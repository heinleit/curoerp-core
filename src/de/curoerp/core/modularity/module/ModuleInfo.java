package de.curoerp.core.modularity.module;

public class ModuleInfo {
	
	private String name;
	private int version;
	private String[] dependencies = new String[0];
	private TypeInfo[] typeInfos = new TypeInfo[0];
	private String bootClass;
	
	public String getName() {
		return name;
	}
	
	public int getVersion() {
		return version;
	}
	
	public String[] getDependencies() {
		return dependencies;
	}
	
	public TypeInfo[] getTypeInfos() {
		return typeInfos;
	}
	
	public String getBootClass() {
		return bootClass;
	}
	
}