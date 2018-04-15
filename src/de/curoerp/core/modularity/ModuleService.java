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
import java.util.stream.Collector;

import com.fasterxml.jackson.dataformat.yaml.snakeyaml.constructor.ConstructorException;

import de.curoerp.core.exception.RuntimeTroubleException;
import de.curoerp.core.logging.LoggingService;
import de.curoerp.core.modularity.exception.ModuleApiClassNotFoundException;
import de.curoerp.core.modularity.exception.ModuleBasePathNotExistsException;
import de.curoerp.core.modularity.exception.ModuleCanNotBeLoadedException;
import de.curoerp.core.modularity.exception.ModuleCanNotBootedException;
import de.curoerp.core.modularity.exception.ModuleControllerClassException;
import de.curoerp.core.modularity.exception.ModuleControllerDoesntImplementApiException;
import de.curoerp.core.modularity.exception.ModuleDependencyUnresolvableException;
import de.curoerp.core.modularity.exception.ModuleFileAlreadyLoadedException;
import de.curoerp.core.modularity.info.TypeInfo;

public class ModuleService {

	private Module[] modules = new Module[0];
	private HashMap<String, Object> resolvedDependencies;

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
		LoggingService.info("jars successfully loaded in the runtime");

		// Check dependencies
		for (Module module : this.modules) {
			for (String dependency : module.getInfo().getDependencies()) {
				if(!Arrays.stream(this.modules).anyMatch(dModule -> dModule.getInfo().getName().equalsIgnoreCase(dependency))) {
					throw new RuntimeTroubleException(new ModuleDependencyUnresolvableException(dependency));
				}
			}
		}
		LoggingService.info("all dependencies found");


		// Resolve dependencies
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

		LoggingService.info("boot progress successful finished"); 
	}

	private void resolveModule(Module module) throws ModuleDependencyUnresolvableException, ModuleControllerClassException, ModuleApiClassNotFoundException, ModuleControllerDoesntImplementApiException, ModuleCanNotBootedException {

		HashMap<String, Class<?>> queue = new HashMap<>();

		// check resolvement in types
		for (TypeInfo type : module.getInfo().getTypeInfos()) {
			Class<?> typeClass = null;
			Class<?> apiClass = null;
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
					final Class<?> fApiClass = apiClass = Class.forName(type.getApi());

					// Controller realy implements api?
					if(!Arrays.asList(typeClass.getInterfaces()).stream().anyMatch(impl -> impl.isAssignableFrom(fApiClass))) {
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
					unresolved.remove(other.getApi());
					unresolved.remove(other.getType());
				}

				// 7th: check external resolvements
				for (Class<?> dependency : unresolved.toArray(new Class<?>[unresolved.size()])) {

					if(this.resolvedDependencies.containsKey(dependency.getName())) {
						unresolved.remove(dependency);
					}
					else if(this.resolvedDependencies.entrySet().stream().map(obj -> obj.getValue().getClass()).anyMatch(cls -> dependency.isAssignableFrom(cls))) {
						unresolved.remove(dependency);
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
			/*Bist du behindert?! if(apiClass != null) {
				queue.put(apiClass.getName(), apiClass);
			}*/
		}

		// ### Now we can say that there is no lack of dependence anymore. 

		// Create Types and resolve dependencies finally - try-try-try... => never cancel!
		while(queue.size() > 0) {

			for (Entry<String, Class<?>> entry : queue.entrySet()) {
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
							// find class direct
							if(this.resolvedDependencies.containsKey(cls.getName())) {
								params.add(this.resolvedDependencies.get(cls.getName()));
								continue;
							}
							
							// find in Superclases&Co.
							Optional<String> key = this.resolvedDependencies.entrySet().stream().filter(e -> cls.isAssignableFrom(e.getValue().getClass())).map(e -> e.getKey()).findFirst();
							if(key.isPresent()) {
								params.add(this.resolvedDependencies.get(key.get()));
								continue;
							}
							
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
