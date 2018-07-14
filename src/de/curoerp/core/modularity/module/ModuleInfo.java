package de.curoerp.core.modularity.module;

public class ModuleInfo {
	
	public String name;
	public String version;
	public String[] dependencies = new String[0];
	public String[] libraries = new String[0];
	public TypeInfo[] typeInfos = new TypeInfo[0];
	public String bootClass;
	
}
