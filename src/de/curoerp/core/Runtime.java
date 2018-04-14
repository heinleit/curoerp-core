package de.curoerp.core;

import java.io.File;

import de.curoerp.core.exception.RuntimeTroubleException;
import de.curoerp.core.modularity.Module;
import de.curoerp.core.modularity.ModuleService;

public class Runtime {

	public static void main(String[] args) {
		
		try {
			for (Module module : ModuleService.findModuleFiles(new File("/home/hheinle/curoerp/modules/e"))) {
				ModuleService.startModule(module);	
			}
		} catch (RuntimeTroubleException e) {
			e.printStackTrace();
		}
		
	}

}
