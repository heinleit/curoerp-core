package de.curoerp.core.modularity;

import java.io.File;
import java.io.FilenameFilter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.MissingResourceException;

import de.curoerp.core.exception.RuntimeTroubleException;
import de.curoerp.core.logging.LoggingService;
import de.curoerp.core.modularity.exception.DependencyNotResolvedException;
import de.curoerp.core.modularity.exception.ModuleApiClassNotFoundException;
import de.curoerp.core.modularity.exception.ModuleBasePathNotExistsException;
import de.curoerp.core.modularity.exception.ModuleCanNotBeLoadedException;
import de.curoerp.core.modularity.exception.ModuleCanNotBootedException;
import de.curoerp.core.modularity.exception.ModuleControllerClassException;
import de.curoerp.core.modularity.exception.ModuleControllerDoesntImplementApiException;
import de.curoerp.core.modularity.exception.ModuleDependencyUnresolvableException;
import de.curoerp.core.modularity.exception.ModuleFileAlreadyLoadedException;
import de.curoerp.core.modularity.exception.ModuleServiceAllreadyBootedException;
import de.curoerp.core.modularity.language.ILocaleService;
import de.curoerp.core.modularity.language.LocaleService;
import de.curoerp.core.modularity.module.DependencyInfo;
import de.curoerp.core.modularity.module.IBootModule;
import de.curoerp.core.modularity.module.IModule;
import de.curoerp.core.modularity.module.Module;
import de.curoerp.core.modularity.versioning.VersionExpressions;

/**
 * Central Module Service for internal..
 * @category Dependency loading System 
 * 
 * @author Hendrik Heinle
 * @since 15.04.2018
 */
public class ModuleService {

	private Module[] modules = new Module[0];
	private DependencyService resolver;
	private boolean booted = false;

	public ModuleService(DependencyService resolver) {
		this.resolver = resolver;
	}

	/**
	 * Boot a bootable Module
	 * 
	 * @param module Module
	 * 
	 * @throws DependencyNotResolvedException
	 */
	public void runModule(Module module) throws DependencyNotResolvedException {
		if(module.getBootClass() == null) {
			throw new RuntimeTroubleException(new Exception("Module " + module.getName() + " dont know any boot-class!"));
		}

		try {
			IBootModule obj = (IBootModule) this.resolver.findInstanceOf(module.getBootClass());
			obj.boot();
		} catch(ClassCastException e) {
			throw new RuntimeTroubleException(e);
		}
	}

	/**
	 * Boot a bootable Module by Name
	 * 
	 * @param name String
	 * 
	 * @throws DependencyNotResolvedException
	 */
	public void runModule(String name) throws DependencyNotResolvedException {
		Module module = Arrays.stream(this.modules).filter(m -> m.getName().equals(name)).findFirst().orElse(null);

		if(module == null) {
			throw new RuntimeTroubleException(new Exception("Module '" + name + "' not loaded!"));
		}
		
		this.runModule(module);
	}

	/**
	 * load modules in directory
	 * NEVER AFTER BOOT!
	 * 
	 * @param directory {@link File}
	 */
	public void loadModules(File directory) {
		if(this.booted) {
			throw new RuntimeTroubleException(new ModuleServiceAllreadyBootedException());
		}
		
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

	/**
	 * Boot the Module-System
	 * 
	 * @throws RuntimeTroubleException => something went wrong :/ Please check code or Modules
	 */
	public void boot() {

		// Fetch&Load Jars in Runtime
		this.hang();
		LoggingService.info("module-jars successfully heaped in runtime: " + String.join(", ", Arrays.stream(this.modules).map(m -> m.getName()).toArray(c -> new String[c])));

		// Check module-dependencies
		this.check();
		LoggingService.info("all module-dependencies found");

		// Resolve dependencies
		this.resolve();
		LoggingService.info("all modules resolved");

		// finish
		this.booted = true;
		LoggingService.info("boot progress successful finished"); 
	}

	/**
	 * hang every module-jar in runtime
	 * 
	 * @throws RuntimeTroubleException => something went wrong :/ Please check code or Modules
	 */
	private void hang() {
		for (Module module : this.modules) {
			try {
				module.fetchJar();
			} catch (ModuleFileAlreadyLoadedException | ModuleCanNotBeLoadedException e) {
				throw new RuntimeTroubleException(e);
			}
		}
	}


	/**
	 * check every dependency in all modules
	 * 
	 * @throws RuntimeTroubleException => something went wrong :/ Please check code or Modules
	 */
	private void check() {
		for (Module module : this.modules) {
			String dependency = this.findUnresolvedDependency(module);
			if(dependency != null) {
				throw new RuntimeTroubleException(new ModuleDependencyUnresolvableException(dependency));
			}
		}
	}

	/**
	 * find first unresolved dependency for module
	 * 
	 * @param module Module
	 * @return [String=first unresolved dependency]|[null=no unresolved dependencies]
	 */
	public String findUnresolvedDependency(Module module) {
		LoggingService.info("\t# check dependencies for module '" + module.getDisplayName() + "'");
		
		for (DependencyInfo dependencyInfo : module.getDependencies()) {
			LoggingService.info("\t## check dependency (" + dependencyInfo.name + ")");
			
			Module dependencyModule = Arrays.stream(this.modules).filter(m -> m.getName().equals(dependencyInfo.name)).findFirst().orElse(null);
			if(dependencyModule == null) {
				LoggingService.info("\t### module not found (" + dependencyInfo.name + ")");
				return dependencyInfo.name;
			}
			
			boolean allMatch = dependencyModule.getVersion().allMatch(
					Arrays.stream(dependencyInfo.versions).map(v -> v.value.getVersion()).toArray(c -> new Long[c]),
					Arrays.stream(dependencyInfo.versions).map(v -> v.expression).toArray(c -> new VersionExpressions[c]));
			
			LoggingService.info("\t### matching: " + allMatch);
			
			if(!allMatch) {
				LoggingService.info("\t### module version not match for " + dependencyModule.getDisplayName() + "");
				return dependencyInfo.name;
			}
		}
		return null;
	}

	/**
	 * Resolve every dependency
	 * 
	 * @throws RuntimeTroubleException => something went wrong :/ Please check code or Modules
	 */
	private void resolve() {
		ArrayList<Module> unresolved = new ArrayList<Module>(Arrays.asList(this.modules));
		ArrayList<String> unsucceeded = new ArrayList<>();


		while(unresolved.size() > 0) {
			int unresolvedStart = unresolved.size();

			for (Module module : unresolved.toArray(new Module[unresolved.size()])) {
				LoggingService.breaker("try resolve " + module.getDisplayName());

				try {
					this.resolver.setSpecialDependencies(this.buildSpecialDependencyMap(module));
					this.resolver.resolveTypes(module.getTypes());
					this.resolver.cleanSpecialDependencies();
					
					unresolved.remove(module);
					LoggingService.info("..resolved");
					
				} catch (ModuleDependencyUnresolvableException e) {
					this.resolver.cleanSpecialDependencies();
					unsucceeded.add(module.getDisplayName());
					LoggingService.warn("..Module " + module.getDisplayName() + " can not resolved!");
					LoggingService.warn(e);
					
				} catch (ModuleControllerClassException | ModuleApiClassNotFoundException
						| ModuleControllerDoesntImplementApiException | ModuleCanNotBootedException e) {
					
					this.resolver.cleanSpecialDependencies();
					throw new RuntimeTroubleException(e);
				
				}
				
			}

			if(unresolvedStart == unresolved.size()) {
				break;
			}
		}

		// are there unresolvable Modules?
		if(unresolved.size() > 0) {
			throw new RuntimeTroubleException(new ModuleCanNotBootedException( 
					unresolved.stream()
					.map(module -> module.getName())
					.toArray(c -> new String[c])));
		}
		
		LoggingService.breaker("all modules resolved");
	}
	
	
	private HashMap<DependencyType, Object> buildSpecialDependencyMap(Module currentModule) {
		HashMap<DependencyType, Object> map = new HashMap<>();
		
		// map builder
		map.put(DependencyType.CurrentModule, (IModule)currentModule);
		
		try {
			map.put(DependencyType.LocaleService, (ILocaleService) new LocaleService(currentModule));
		} catch(MissingResourceException e) {
			LoggingService.warn(e);
		}
		
		return map;
	}

}
