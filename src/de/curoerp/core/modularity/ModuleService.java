package de.curoerp.core.modularity;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.MissingResourceException;

import de.curoerp.core.exception.RuntimeTroubleException;
import de.curoerp.core.functionality.info.ICoreInfo;
import de.curoerp.core.logging.LoggingService;
import de.curoerp.core.modularity.dependency.DependencyInfo;
import de.curoerp.core.modularity.dependency.DependencyLimitation;
import de.curoerp.core.modularity.dependency.IDependencyContainer;
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
import de.curoerp.core.modularity.module.IBootModule;
import de.curoerp.core.modularity.module.IModule;
import de.curoerp.core.modularity.module.Module;
import de.curoerp.core.modularity.module.ModuleInfo;

/**
 * Central Module Service for internal..
 * @category Dependency loading System 
 * 
 * @author Hendrik Heinle
 * @since 15.04.2018
 */
public class ModuleService {

	private Module[] modules = new Module[0];
	private IDependencyService resolver;
	private boolean booted = false;
	private IDependencyContainer container;
	private ICoreInfo coreInfo;

	public ModuleService(IDependencyService resolver, IDependencyContainer container, ICoreInfo coreInfo) {
		this.resolver = resolver;
		this.container = container;
		this.coreInfo = coreInfo;
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
			throw new RuntimeTroubleException(new Exception("Module " + module.getDisplayName() + " dont know any boot-class!"));
		}

		try {
			IBootModule obj = (IBootModule) this.container.findSingleInstanceOf(module.getBootClass());
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
		Module module = Arrays.stream(this.modules).filter(m -> m.getSystemName().equals(name)).findFirst().orElse(null);

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

		Module[] modules = (Module[]) Arrays.stream(directory.listFiles((d, n) -> n.endsWith(".cmod.jar")))
				.map(file -> new Module(file)).toArray(length -> new Module[length]);

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
	 * load modules in jvm
	 * only for debugging, very slow and dangerous!
	 * 
	 * NEVER AFTER BOOT!
	 */
	public void loadModules(ModuleInfo[] infos) {
		if(this.booted) {
			throw new RuntimeTroubleException(new ModuleServiceAllreadyBootedException());
		}
		
		ArrayList<Module> modules = new ArrayList<>();
		for (ModuleInfo info : infos) {
			try {
				modules.add(new Module(info));
			} catch (Exception e) {
				LoggingService.error(e);
			}
		}

		this.modules = modules.toArray(new Module[modules.size()]);
	}
	
	/**
	 * for debugging: Load Modules by Object-Array
	 * 
	 * not really type-safe!
	 */
	public void setModules(Module[] modules) {
		this.modules = modules;
	}

	/**
	 * Boot the Module-System
	 * 
	 * @throws RuntimeTroubleException => something went wrong :/ Please check code or Modules
	 */
	public void boot() {

		// Prefetch Libraries
		this.libraries();
		LoggingService.info("libraries fetched: " + String.join(", ", this.libaries.stream().toArray(c -> new String[c])));

		// Fetch&Load Jars in Runtime
		this.hang();
		LoggingService.info("module-jars successfully heaped in runtime: " + String.join(", ", Arrays.stream(this.modules).map(m -> m.getDisplayName()).toArray(c -> new String[c])));

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
	
	private ArrayList<String> libaries = new ArrayList<>();
	
	private void libraries() {
		for (Module module : modules) {
			for (String lib : module.getLibraries()) {
				if(libaries.contains(lib)) continue;
				try {
					URLClassLoader classLoader = (URLClassLoader)ClassLoader.getSystemClassLoader();
					Method method = URLClassLoader.class.getDeclaredMethod("addURL", URL.class);
					method.setAccessible(true);
					method.invoke(classLoader, new File(this.coreInfo.getLibaryDir() + "/" + lib).toURI().toURL());
				} catch (IOException | NoSuchMethodException 
						| SecurityException | IllegalAccessException
						| IllegalArgumentException | InvocationTargetException e) {
					throw new RuntimeTroubleException(e);
				}
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
		for (DependencyInfo dependency : module.getDependencies()) {
			Module dependModule = Arrays.stream(this.modules)
					.filter(dModule -> dModule.getSystemName().equalsIgnoreCase(Module.parseSystemName(dependency.name)))
					.findFirst()
					.orElse(null);
			if(dependModule == null) {
				return dependency.name;
			}

			DependencyLimitation notMatchLimitation = dependency.limitations == null ? null : Arrays.stream(dependency.limitations)
					.filter(limitation -> !limitation.version.match(dependModule.getVersion(), limitation.expression))
					.findFirst()
					.orElse(null);
			if(notMatchLimitation != null) {
				return dependency.name + " (" + notMatchLimitation.expression + " " + notMatchLimitation.version.getVersionName() + ")";
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


		while(unresolved.size() > 0) {
			int unresolvedStart = unresolved.size();

			for (Module module : unresolved.toArray(new Module[unresolved.size()])) {
				LoggingService.breaker("try resolve " + module.getDisplayName());

				try {
					this.container.setSessionDependencies(this.buildSpecialDependencyMap(module));
					this.resolver.resolveTypes(module.getTypes());
					this.container.cleanSessionDependencies();

					unresolved.remove(module);
					LoggingService.info("..resolved");

				} catch (ModuleDependencyUnresolvableException e) {
					this.container.cleanSessionDependencies();
					LoggingService.warn("..Module " + module.getDisplayName() + " can not resolved!");
					LoggingService.warn(e);

				} catch (ModuleControllerClassException | ModuleApiClassNotFoundException
						| ModuleControllerDoesntImplementApiException | ModuleCanNotBootedException | DependencyNotResolvedException e) {

					this.container.cleanSessionDependencies();
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
					.map(module -> module.getDisplayName())
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
