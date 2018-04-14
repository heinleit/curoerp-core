package de.curoerp.core.modularity;

import java.io.File;
import java.io.FilenameFilter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;

import de.curoerp.core.exception.RuntimeTroubleException;

public class ModuleService {

	private Module[] modules = new Module[0];
	private HashMap<String, Object> resolvedInterfaces;
	private ArrayList<Module> resolvedModules;
	
	public ModuleService() {
		//
	}
	
	public void findModules(File directory) {
		if(!directory.exists() || !directory.isDirectory()) {
			throw new RuntimeTroubleException(new ModuleBasePathNotExistsException());
		}

		Module[] modules = (Module[]) Arrays.stream(directory.listFiles(new FilenameFilter() {
			@Override
			public boolean accept(File dir, String name) {
				return name.endsWith(".cmod.jar");
			}
		})).map(file -> new Module(file)).toArray(length -> new Module[length]);

		try {
			for (Module module : modules) {
				module.loadInfo();
			}
		} catch(ModuleCanNotBeLoadedException e) {
			throw new RuntimeTroubleException(e);
		}

		this.modules = modules;
	}
	
	public void boot() {
		// Fetch&Load Jars in Runtime
		for (Module module : this.modules) {
			try {
				module.fetchJar();
			} catch (ModuleFileAlreadyLoadedException | ModuleCanNotBeLoadedException e) {
				throw new RuntimeTroubleException(e);
			}
		}
		
		System.out.println("Jars successful loaded!");
		
		// Resolve dependencies
		this.resolvedInterfaces = new HashMap<>();
		this.resolvedModules = new ArrayList<>();
		
		// Gibt es noch unaufgelöste Module?
		while(this.resolvedModules.size() < this.modules.length) {
			
			// Alle unaufgelösten Module durchgehen
			for (Module module : (Module[]) Arrays.stream(this.modules)
					.filter(module -> !this.resolvedModules.contains(module))
					.toArray(c -> new Module[c])) {
				
				try {
					this.resolveModule(module);
					System.out.println("Modul " + module.getInfo().getName() + " erfolgreich aufgelöst");
					this.resolvedModules.add(module);
					break;
				} catch (ModuleDependencyUnresolvableException e) {
					System.err.println("Modul " + module.getInfo().getName() + " konnte nicht aufgelöst werden!");
				}
			}
		}
		
	}
	
	private void resolveModule(Module module) throws ModuleDependencyUnresolvableException {
		// dependencies resolved?
		for (String dependency : module.getInfo().getDependencies()) {
			if(!this.resolvedModules.stream().anyMatch(dModule -> dModule.getInfo().getName().equalsIgnoreCase(dependency))) {
				throw new ModuleDependencyUnresolvableException(dependency);
			}
		}
		
		// construct => controllers
		
	}

}
