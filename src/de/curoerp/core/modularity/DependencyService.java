package de.curoerp.core.modularity;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map.Entry;
import java.util.logging.Logger;

import de.curoerp.core.logging.LoggingService;
import de.curoerp.core.modularity.dependency.DependencyContainer;
import de.curoerp.core.modularity.exception.DependencyNotResolvedException;
import de.curoerp.core.modularity.exception.ModuleApiClassNotFoundException;
import de.curoerp.core.modularity.exception.ModuleCanNotBootedException;
import de.curoerp.core.modularity.exception.ModuleControllerClassException;
import de.curoerp.core.modularity.exception.ModuleControllerDoesntImplementApiException;
import de.curoerp.core.modularity.exception.ModuleDependencyUnresolvableException;
import de.curoerp.core.modularity.module.TypeInfo;

/**
 * Dependency Resolver
 * 
 * @category Dependency loading System
 * @author Hendrik Heinle
 * @since 16.04.2018
 */
public class DependencyService {

	private DependencyContainer container;

	public DependencyService(DependencyContainer container) {
		this.container = container;
	}

	/**
	 * This function resolve every TypeInfo
	 * 
	 * @param typeInfos TypeInfo[]
	 * 
	 * @throws ModuleDependencyUnresolvableException => one or more dependencies are unresolved
	 * @throws ModuleControllerClassException => the type-class isn't correct (0 or 1 constructor / not found)
	 * @throws ModuleApiClassNotFoundException =>  the api-interface isn't corrent
	 * @throws ModuleControllerDoesntImplementApiException => type-class doesn't implement api-interface!
	 * @throws ModuleCanNotBootedException => Error while construction, something went horrible wrong, Fuck! -> Cancel&check System!!!
	 * @throws DependencyNotResolvedException 
	 */
	public void resolveTypes(TypeInfo[] typeInfos) throws ModuleDependencyUnresolvableException, ModuleControllerClassException, ModuleControllerDoesntImplementApiException, ModuleApiClassNotFoundException, ModuleCanNotBootedException, DependencyNotResolvedException {

		HashMap<String, Class<?>> queue = new HashMap<>();

		LoggingService.info("resolve types");

		LoggingService.info("dry-run");
		// check types
		for (TypeInfo type : typeInfos) {
			// find type
			Class<?> typeClass = this.resolveType(type.getType());
			LoggingService.info("Class '" + type.getType() + "' found");

			// check type
			this.checkResolvement(typeClass, type.getApi(), Arrays.stream(typeInfos).filter(type_ -> type_ != type).toArray(c -> new TypeInfo[c]));
			LoggingService.info("# validated");

			// put type in resolvable Map
			queue.put(typeClass.getName(), typeClass);
			LoggingService.info("# putted");
		}
		LoggingService.info("dry-run successful");

		// ### Now we can say that there is no lack of dependence anymore. 

		LoggingService.info("final run");
		// Create Types and resolve dependencies finally - try-try-try... => never cancel!
		while(queue.size() > 0) {

			// Copy HashMap for Modifications (possible at types without api)
			ArrayList<Entry<String, Class<?>>> entries = new ArrayList<>(queue.entrySet());

			for (Entry<String, Class<?>> entry : entries) {
				Object obj = createInstance(entry.getValue());

				if(obj == null) {
					continue;
				}

				// Nullpointer is no option, it's an other problem!
				this.container.addResolvedDependency(entry.getValue(), obj);
				queue.remove(entry.getKey());
			}

		}

	}

	/*
	 * Instantiation
	 */

	private Object createInstance(Class<?> type) throws ModuleCanNotBootedException {
		Object obj = null;

		try {
			switch (type.getConstructors().length) {
			// no constructor
			case 0:
				obj = type.newInstance();
				break;

				// only one constructor
			case 1:
				Constructor<?> constructor = type.getConstructors()[0];

				ArrayList<Object> params = new ArrayList<>();
				for (Class<?> cls : constructor.getParameterTypes()) {
					// find direct or in Superclases&Co.
					try {
						params.add(this.container.findSingleInstanceOf(cls));
						continue;
					} catch (DependencyNotResolvedException e) {
						LoggingService.debug(e);
					}

					throw new ModuleDependencyUnresolvableException(cls.getName());
				}

				obj = constructor.newInstance(params.toArray(new Object[params.size()]));
				break;
			default:
				throw new IllegalArgumentException();
			}
		} catch (InstantiationException | IllegalAccessException | IllegalArgumentException | InvocationTargetException e) {
			LoggingService.debug(e);
			// fatal error => exit Runtime
			throw new ModuleCanNotBootedException(new String[] {
					type.getName()
			});
		}
		catch(ModuleDependencyUnresolvableException e) {
			// probably internal dependencies
		}

		return obj;
	}

	/*
	 * Check
	 */

	private void checkResolvement(Class<?> typeClass, String api, TypeInfo[] internal) throws 
	ModuleDependencyUnresolvableException, 
	ModuleControllerClassException, 
	ModuleControllerDoesntImplementApiException,
	ModuleApiClassNotFoundException {
		LoggingService.info("# check resolvements");
		// 1st: check API class
		this.checkApi(api, typeClass);
		LoggingService.info("## API '" + api + "' found/ignored");

		// 2nd: check constructors (max = 1)
		if(typeClass.getConstructors().length > 1) {
			LoggingService.debug("contructor (" + typeClass.getName() + ") has more than 1 constructor, throw ModuleControllerClassException");
			throw new ModuleControllerClassException(typeClass.getName());
		}

		// 3nd: find dependencies
		Class<?>[] dependencies = this.findDependencies(typeClass);
		LoggingService.info("## dependencies found: " + String.join(", ", Arrays.stream(dependencies).map(d -> d.getSimpleName()).toArray(c -> new String[c])));

		// 4rd: any dependency unresolved?
		Class<?>[] unresolved = this.findUnresolvedDependencies(dependencies, internal);
		LoggingService.info("## unresolved dependencies: " + String.join(", ", Arrays.stream(unresolved).map(d -> d.getSimpleName()).toArray(c -> new String[c])));


		if(unresolved.length > 0) {
			throw new ModuleDependencyUnresolvableException(String.join(", ", Arrays.stream(unresolved).map(c -> c.getName()).toArray(c -> new String[c])));
		}

		// everything fine!
	}

	private Class<?> resolveType(String fqn) throws ModuleControllerClassException {
		Class<?> type = null;

		// search class
		try {
			type = Class.forName(fqn);
		} catch (ClassNotFoundException e) {
			throw new ModuleControllerClassException(fqn);
		}

		// check constructor.length 1 or 0
		if(type.getConstructors().length > 1) {
			throw new ModuleControllerClassException(fqn);
		}

		return type;
	}

	private void checkApi(String fqn, Class<?> type) throws 
	ModuleControllerDoesntImplementApiException, 
	ModuleApiClassNotFoundException, 
	ModuleControllerClassException  {
		if(fqn.trim().length() > 0) {
			// search api-class
			try {
				final Class<?> apiClass = Class.forName(fqn);

				// Controller really implements api?
				if(!Arrays.asList(type.getInterfaces()).stream().anyMatch(impl -> impl.isAssignableFrom(apiClass))) {
					throw new ModuleControllerDoesntImplementApiException();
				}
				
				// api already used 
				try {
					if(this.container.findInstancesOf(apiClass).length > 0) {
						LoggingService.error("api '" + apiClass.getName() + "' allready used.");
						throw new ModuleApiClassNotFoundException();
					}
				} catch(DependencyNotResolvedException e1) {
					LoggingService.debug("no instance of '" + apiClass.getName() + "' found. That's normal.");
				}
				
			} catch (ClassNotFoundException e) {
				throw new ModuleApiClassNotFoundException();
			}
		}
	}

	private Class<?>[] findDependencies(Class<?> type) {
		ArrayList<Class<?>> unresolved = new ArrayList<>();

		// fetch dependencies
		for (Constructor<?> constructor : type.getConstructors()) {
			for (Class<?> parameter : constructor.getParameterTypes()) {
				if(!unresolved.contains(parameter)) {
					unresolved.add(parameter);
				}
			}
		}

		return unresolved.toArray(new Class<?>[unresolved.size()]);
	}

	private Class<?>[] findUnresolvedDependencies(Class<?>[] dependencies, TypeInfo[] internal) {
		// unresolved dependencies
		ArrayList<Class<?>> unresolved = new ArrayList<>(Arrays.asList(dependencies));

		// any dependencies?
		if(unresolved.size() > 0) {
			// dependencies resolved?

			// check internal resolvements
			for (TypeInfo other : internal) {
				// any api?
				unresolved.remove(unresolved.stream().filter(c -> c.getName().equals(other.getApi())).findFirst().orElse(null));

				// any type?
				unresolved.remove(unresolved.stream().filter(c -> c.getName().equals(other.getType())).findFirst().orElse(null));
			}

			// check external resolvements
			for (Class<?> dependency : unresolved.toArray(new Class<?>[unresolved.size()])) {
				try {
					if(this.container.findSingleInstanceOf(dependency) != null) {
						unresolved.remove(dependency);	
					}
				} catch (DependencyNotResolvedException e) {
					LoggingService.debug(e);
				}
			}

		}
		return unresolved.toArray(new Class<?>[unresolved.size()]);
	}

}
