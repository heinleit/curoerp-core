package de.curoerp.core.modularity.module;

public class TypeInfo {
	
	public TypeInfo() {
		// placeholder for jackson-yaml
	}

	public TypeInfo(String type, String api) {
		this.type = type;
		this.api = api;
	}

	//Fields
	
	public String type;
	public String api = "";
}
