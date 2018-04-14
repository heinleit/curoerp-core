package de.curoerp.core.modularity.info;

public class ModuleInfo {
	
	private String name;
	private int version;
	private String[] dependencies;
	private ControllerInfo[] controllerInfos;
	private String[] locales;
	
	public String getName() {
		return name;
	}
	
	public int getVersion() {
		return version;
	}
	
	public String[] getDependencies() {
		return dependencies;
	}
	
	public ControllerInfo[] getControllerInfos() {
		return controllerInfos;
	}
	
	public String[] getLocales() {
		return locales;
	}
	
}
