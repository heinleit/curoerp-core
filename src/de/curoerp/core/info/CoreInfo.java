package de.curoerp.core.info;

public class CoreInfo implements ICoreInfo {
	
	private String _applicationName;
	
	public CoreInfo(String applicationName) {
		this._applicationName = applicationName;
	}

	@Override
	public String getApplicationName() {
		return this._applicationName;
	}

}
