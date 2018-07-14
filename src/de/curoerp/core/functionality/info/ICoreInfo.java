package de.curoerp.core.functionality.info;

import java.io.File;

public interface ICoreInfo {
	
	public String getApplicationName();
	public File getBaseDir();
	public File getModuleDir();
	public File getConfigDir();
	public File getLibaryDir();
	
}
