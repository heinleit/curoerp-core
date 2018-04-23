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
	
	private String type;
	private String api = "";

	public String getType() {
		return this.type;
	}

	public String getApi() {
		return this.api;
	}
}
