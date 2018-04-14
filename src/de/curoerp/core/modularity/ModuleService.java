package de.curoerp.core.modularity;

import java.io.File;
import java.io.FilenameFilter;
import java.util.Arrays;

import de.curoerp.core.exception.RuntimeTroubleException;

public class ModuleService {
	
	public static Module[] findModuleFiles(File directory) {
		if(!directory.exists() || !directory.isDirectory()) {
			throw new RuntimeTroubleException(new ModuleBasePathNotExistsException());
		}
		
		return (Module[]) Arrays.stream(directory.listFiles(new FilenameFilter() {
		    @Override
		    public boolean accept(File dir, String name) {
		        return name.endsWith(".cmod.jar");
		    }
		})).map(file -> new Module(file)).toArray(length -> new Module[length]);
	}
	
	public static void startModule(Module module) {
		try {
			module.loadFile();
		} catch (ModuleFileAlreadyLoadedException | ModuleCanNotBeLoadedException e) {
			throw new RuntimeTroubleException(e);
		}
	}
	
}
