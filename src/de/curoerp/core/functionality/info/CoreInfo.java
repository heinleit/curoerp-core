package de.curoerp.core.functionality.info;

import java.io.File;

public class CoreInfo implements ICoreInfo {
	
	private String _applicationName;
	private File _baseDir;
	
	public CoreInfo(String applicationName, File file) {
		this._applicationName = applicationName;
		this._baseDir = file;
	}

	@Override
	public String getApplicationName() {
		return this._applicationName;
	}

	@Override
	public File getBaseDir() {
		return this._baseDir;
	}
	
	private File subDir(String dir) {
		File f = new File(this._baseDir + "/" + dir + "/");
		if(!f.exists() || !f.isDirectory()) {
			f.mkdirs();
		}
		return f;
	}

	@Override
	public File getModuleDir() {
		return subDir("module");
	}

	@Override
	public File getConfigDir() {
		return subDir("config");
	}

}
