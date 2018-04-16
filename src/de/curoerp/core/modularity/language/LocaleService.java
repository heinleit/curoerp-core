package de.curoerp.core.modularity.language;

import java.util.ResourceBundle;

import de.curoerp.core.logging.LoggingService;
import de.curoerp.core.modularity.module.Module;

public class LocaleService implements ILocaleService {

	private String localeName;
	private ResourceBundle bundle;

	public LocaleService(Module module) {
		this.localeName = module.getInfo().getName().trim().replace(' ', '_');
		
		this.ini();
	}
	
	private void ini() {
    	this.bundle = ResourceBundle.getBundle("resources." + this.localeName);
	}

	@Override
	public String get(String key) {
		try {
			return this.bundle.getString(key);	
		} catch(Exception e) {
			LoggingService.error(e);
		}
		return key;
	}

}
