package de.curoerp.core;

import java.io.File;

import de.curoerp.core.exception.RuntimeTroubleException;
import de.curoerp.core.logging.LoggingService;
import de.curoerp.core.modularity.ModuleService;

public class Runtime {

	public static void main(String[] args) {
		
		try {
			ModuleService service = new ModuleService();
			service.loadModules(new File("/home/hheinle/curoerp/modules/"));
			service.boot();
			
		} catch (RuntimeTroubleException e) {
			LoggingService.error(e);
		}
		
	}

}
