package de.curoerp.core.functionality.config;

public interface IConfigService {
	
	public <T> ConfigInfo<T> loadConfig(String name, Class<T> type);
	
}
