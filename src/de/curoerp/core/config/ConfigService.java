package de.curoerp.core.config;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;

import org.yaml.snakeyaml.Yaml;
import org.yaml.snakeyaml.constructor.Constructor;

import de.curoerp.core.info.ICoreInfo;
import de.curoerp.core.logging.LoggingService;


public class ConfigService implements IConfigService {
	
	private File _configDir;
	
	public ConfigService(ICoreInfo coreInfo) {
		this._configDir = coreInfo.getConfigDir();
	}
	
	private File getConfigFile(String name) {
		return new File(this._configDir + "/" + name + ".yml");
	}

	@Override
	public <T> ConfigInfo<T> loadConfig(String name, Class<T> type) {
		ConfigInfo<T> info = new ConfigInfo<T>();
		try {
			info.instance = new Yaml(new Constructor(type)).loadAs(new FileReader(this.getConfigFile(name)), type);
		} catch (FileNotFoundException e) {
			LoggingService.warn(e);
		}
		
		return info;
	}
	
}
