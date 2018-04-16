package de.curoerp.core.modularity;

import java.io.File;
import java.io.FilenameFilter;
import java.util.ArrayList;
import java.util.Arrays;

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
		if(module.getInfo().getBootClass() == null) {
			throw new RuntimeTroubleException(new Exception("Module " + module.getInfo().getName() + " dont know any boot-class!"));
		}

		try {
			BootModule obj = (BootModule) this.resolver.findInstanceOf(module.getInfo().getBootClass());
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
		Module module = Arrays.stream(this.modules).filter(m -> m.getInfo().getName().equals(name)).findFirst().orElse(null);

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
		LoggingService.info("jars successfully loaded in the runtime");

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
		for (String dependency : module.getInfo().getDependencies()) {
			if(!Arrays.stream(this.modules).anyMatch(dModule -> dModule.getInfo().getName().equalsIgnoreCase(dependency))) {
				return dependency;
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
				LoggingService.info("try resolve module " + module.getInfo().getName() + "..");

				try {
					this.resolver.resolveTypes(module.getInfo().getTypeInfos());
					unresolved.remove(module);
					LoggingService.info("..resolved");
				} catch (ModuleDependencyUnresolvableException e) {
					unsucceeded.add(module.getInfo().getName());
					LoggingService.warn("..Module " + module.getInfo().getName() + " can not resolved!");
				} catch (ModuleControllerClassException | ModuleApiClassNotFoundException
						| ModuleControllerDoesntImplementApiException | ModuleCanNotBootedException e) {
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
					.map(module -> module.getInfo().getName())
					.toArray(c -> new String[c])));
		}
	}

}
