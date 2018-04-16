package de.curoerp.core.modularity;

import java.io.File;
import java.io.FilenameFilter;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map.Entry;
import java.util.Optional;

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
import de.curoerp.core.modularity.info.TypeInfo;

/**
 * Central Module Service for internal..
 * @category Dependency loading System 
 * 
 * @author Hendrik Heinle
 * @since 15.04.2018
 */
public class ModuleService {

	private Module[] modules = new Module[0];
	private HashMap<String, Object> resolvedDependencies;

	public ModuleService() {
		//
	}

	
	public void bootModule(String name) throws DependencyNotResolvedException {
		// find module
		Module module = Arrays.stream(this.modules).filter(m -> m.getInfo().getName().equals(name)).findFirst().orElse(null);
		
		if(module == null) {
			throw new RuntimeTroubleException(new Exception("Module '" + name + "' not loaded!"));
		}
		
		if(module.getInfo().getBootClass() == null) {
			throw new RuntimeTroubleException(new Exception("Module " + module.getInfo().getName() + " dont know any boot-class!"));
		}
		
		try {
			BootModule obj = (BootModule) this.findInstanceOf(module.getInfo().getBootClass());
			obj.boot();
		} catch(ClassCastException e) {
			throw new RuntimeTroubleException(e);
		}
	}

	/**
	 * find instance of type (String)
	 * 
	 * @param fqcn {@link String} full qualified class name
	 * @return T instance of fqcn
	 * 
	 * @throws DependencyNotResolvedException
	 */
	public Object findInstanceOf(String fqcn) throws DependencyNotResolvedException {
		if(!this.resolvedDependencies.containsKey(fqcn)) {
			try {
				return findInstanceOf(Class.forName(fqcn));
			} catch (ClassNotFoundException e) {
				throw new DependencyNotResolvedException();
			}
		}

		return this.resolvedDependencies.get(fqcn);
	}

	/**
	 * find instance of type (Class<T>)
	 * 
	 * @param cls Class<T>
	 * @return T instance of cls
	 * 
	 * @throws DependencyNotResolvedException
	 */
	@SuppressWarnings("unchecked")
	public <T> T findInstanceOf(Class<T> cls) throws DependencyNotResolvedException {
		if(!this.resolvedDependencies.containsKey(cls.getName())) {
			Optional<String> key = this.resolvedDependencies.entrySet().stream()
					.filter(e -> cls.isAssignableFrom(e.getValue().getClass()))
					.map(e -> e.getKey())
					.findFirst();
			if(key.isPresent()) {
				return (T) this.resolvedDependencies.get(key.get());
			}

			throw new DependencyNotResolvedException();
		}

		return (T) this.resolvedDependencies.get(cls.getName());
	}

	/**
	 * load modules in directory
	 * NEVER AFTER BOOT!
	 * 
	 * @param directory {@link File}
	 */
	public void loadModules(File directory) {
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

		// Check dependencies
		this.check();

		// Resolve dependencies
		this.resolve();

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
		LoggingService.info("jars successfully loaded in the runtime");
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
		LoggingService.info("all dependencies found");
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
		this.resolvedDependencies = new HashMap<>();
		ArrayList<Module> unresolved = new ArrayList<Module>(Arrays.asList(this.modules));
		ArrayList<String> unsucceeded = new ArrayList<>();

		while(unresolved.size() > 0) {
			int unresolvedStart = unresolved.size();

			for (Module module : unresolved.toArray(new Module[unresolved.size()])) {
				LoggingService.info("try resolve module " + module.getInfo().getName() + "..");

				try {
					this.resolveModule(module);
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
		LoggingService.info("all modules resolved");
	}

	/**
	 * This function resolve every class under 'typeInfos.type' in given Module.
	 * 
	 * @param module Module
	 * 
	 * @throws ModuleDependencyUnresolvableException => one or more dependencies are unresolved
	 * @throws ModuleControllerClassException => the type-class isn't correct (0 or 1 constructor / not found)
	 * @throws ModuleApiClassNotFoundException =>  the api-interface isn't corrent
	 * @throws ModuleControllerDoesntImplementApiException => type-class doesn't implement api-interface!
	 * @throws ModuleCanNotBootedException => Error while construction, something went horrible wrong, Fuck! -> Cancel&check System!!!
	 */
	private void resolveModule(Module module) throws ModuleDependencyUnresolvableException, ModuleControllerClassException, ModuleApiClassNotFoundException, ModuleControllerDoesntImplementApiException, ModuleCanNotBootedException {

		HashMap<String, Class<?>> queue = new HashMap<>();

		// check resolvement in types
		for (TypeInfo type : module.getInfo().getTypeInfos()) {
			Class<?> typeClass = null;
			ArrayList<Class<?>> unresolved = new ArrayList<>();

			// 1st: check type-class
			try {
				typeClass = Class.forName(type.getType());
			} catch (ClassNotFoundException e) {
				throw new ModuleControllerClassException();
			}

			// 2nd: check constructor.length 1 or 0
			if(typeClass.getConstructors().length > 1) {
				throw new ModuleControllerClassException();
			}

			// 3rd: check api (api is optional)
			if(type.getApi().trim().length() > 0) {

				// search api-class
				try {
					final Class<?> apiClass = Class.forName(type.getApi());

					// Controller realy implements api?
					if(!Arrays.asList(typeClass.getInterfaces()).stream().anyMatch(impl -> impl.isAssignableFrom(apiClass))) {
						throw new ModuleControllerDoesntImplementApiException();
					}
				} catch (ClassNotFoundException e) {
					throw new ModuleApiClassNotFoundException();
				}
			}

			// 4th: fetch dependencies
			for (Constructor<?> constructor : typeClass.getConstructors()) {
				for (Class<?> parameter : constructor.getParameterTypes()) {
					if(!unresolved.contains(parameter)) {
						unresolved.add(parameter);
					}
				}
			}

			// 5th: any dependencies?
			if(unresolved.size() > 0) {
				// dependencies resolved?

				// 6th: check internal resolvements
				for (TypeInfo other : module.getInfo().getTypeInfos()) {
					unresolved.remove(unresolved.stream().filter(c -> c.getName().equals(other.getApi())).findFirst().orElse(null));
					unresolved.remove(unresolved.stream().filter(c -> c.getName().equals(other.getType())).findFirst().orElse(null));
				}

				// 7th: check external resolvements
				for (Class<?> dependency : unresolved.toArray(new Class<?>[unresolved.size()])) {
					try {
						if(findInstanceOf(dependency) != null) {
							unresolved.remove(dependency);	
						}
					} catch (DependencyNotResolvedException e) {
					}
				}

				// 8th: Check, if all Dependencies are resolved
				if(unresolved.size() > 0) {
					throw new ModuleDependencyUnresolvableException(String.join(", ", unresolved.stream().map(c -> c.getName()).toArray(c -> new String[c])));
				}
			}

			// Last, but not least
			// 9th: put type and api in resolvable Map
			queue.put(typeClass.getName(), typeClass);
		}

		// ### Now we can say that there is no lack of dependence anymore. 

		// Create Types and resolve dependencies finally - try-try-try... => never cancel!
		while(queue.size() > 0) {

			// Copy HashMap for Modifications (possible at types without api)
			ArrayList<Entry<String, Class<?>>> entries = new ArrayList<>(queue.entrySet());

			for (Entry<String, Class<?>> entry : entries) {
				Object obj = null;

				// create instance
				try {
					switch (entry.getValue().getConstructors().length) {
					// no constructor
					case 0:
						obj = entry.getValue().newInstance();
						break;

						// only one constructor
					case 1:
						Constructor<?> constructor = entry.getValue().getConstructors()[0];

						ArrayList<Object> params = new ArrayList<>();
						for (Class<?> cls : constructor.getParameterTypes()) {
							// find direct or in Superclases&Co.
							try {
								params.add(findInstanceOf(cls));
								continue;
							} catch (DependencyNotResolvedException e) { }

							throw new ModuleDependencyUnresolvableException(cls.getName());
						}

						obj = constructor.newInstance(params.toArray(new Object[params.size()]));
						break;
					}
				} catch (InstantiationException | IllegalAccessException | IllegalArgumentException | InvocationTargetException e) {
					e.printStackTrace();

					// error => exit Runtime
					throw new ModuleCanNotBootedException(new String[] {
							module.getInfo().getName()
					});
				}
				catch(ModuleDependencyUnresolvableException e) {
					// probably internal dependencies
					continue;
				}

				// Nullpointer is no option, it's an problem!
				this.resolvedDependencies.put(entry.getValue().getName(), obj);
				queue.remove(entry.getKey());
			}

		}

	}

}
