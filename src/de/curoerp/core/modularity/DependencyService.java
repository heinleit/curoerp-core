package de.curoerp.core.modularity;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Optional;
import java.util.Map.Entry;

import de.curoerp.core.logging.LoggingService;
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

	private HashMap<String, Object> availableDependencies;
	private HashMap<DependencyType, Object> specialDependencies = new HashMap<>();

	public DependencyService() {
		this.availableDependencies = new HashMap<>();
	}

	/*
	 * Librarian
	 */

	/**
	 * find instance of type (String)
	 * 
	 * @param fqcn {@link String} full qualified class name
	 * @return T instance of fqcn
	 * 
	 * @throws DependencyNotResolvedException
	 */
	public Object findInstanceOf(String fqcn) throws DependencyNotResolvedException {
		if(!this.availableDependencies.containsKey(fqcn)) {
			try {
				return findInstanceOf(Class.forName(fqcn));
			} catch (ClassNotFoundException e) {
				throw new DependencyNotResolvedException();
			}
		}

		return this.availableDependencies.get(fqcn);
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
		SpecialDependency annotation = cls.getAnnotation(SpecialDependency.class);
		if(annotation != null) {
			if(this.specialDependencies.containsKey(annotation.type())) {
				return (T) this.specialDependencies.get(annotation.type());
			}
		}

		if(!this.availableDependencies.containsKey(cls.getName())) {
			Optional<String> key = this.availableDependencies.entrySet().stream()
					.filter(e -> cls.isAssignableFrom(e.getValue().getClass()))
					.map(e -> e.getKey())
					.findFirst();
			if(key.isPresent()) {
				return (T) this.availableDependencies.get(key.get());
			}

			throw new DependencyNotResolvedException();
		}

		return (T) this.availableDependencies.get(cls.getName());
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
	 */
	public void resolveTypes(TypeInfo[] typeInfos) throws ModuleDependencyUnresolvableException, ModuleControllerClassException, ModuleControllerDoesntImplementApiException, ModuleApiClassNotFoundException, ModuleCanNotBootedException {

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
				this.availableDependencies.put(entry.getValue().getName(), obj);
				queue.remove(entry.getKey());
			}

		}

	}

	/*
	 * Instantiation
	 */

	public void setSpecialDependencies(HashMap<DependencyType, Object> map) {
		this.cleanSpecialDependencies();
		for (Entry<DependencyType, Object> entry : map.entrySet()) {
			this.specialDependencies.put(entry.getKey(), entry.getValue());
		}
	}

	public void cleanSpecialDependencies() {
		this.specialDependencies = new HashMap<>();
	}

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
						params.add(this.findInstanceOf(cls));
						continue;
					} catch (DependencyNotResolvedException e) { }

					throw new ModuleDependencyUnresolvableException(cls.getName());
				}

				obj = constructor.newInstance(params.toArray(new Object[params.size()]));
				break;
			}
		} catch (InstantiationException | IllegalAccessException | IllegalArgumentException | InvocationTargetException e) {
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

	private void checkResolvement(Class<?> typeClass, String api, TypeInfo[] internal) throws ModuleDependencyUnresolvableException, ModuleControllerClassException, ModuleControllerDoesntImplementApiException, ModuleApiClassNotFoundException {
		LoggingService.info("# check resolvements");
		// 1st: check API class
		this.checkApi(api, typeClass);
		LoggingService.info("## API '" + api + "' found/ignored");

		// 2nd: find dependencies
		Class<?>[] dependencies = this.findDependencies(typeClass);
		LoggingService.info("## dependencies found: " + String.join(", ", Arrays.stream(dependencies).map(d -> d.getSimpleName()).toArray(c -> new String[c])));

		// 3rd: any dependency unresolved?
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
			throw new ModuleControllerClassException();
		}

		// check constructor.length 1 or 0
		if(type.getConstructors().length > 1) {
			throw new ModuleControllerClassException();
		}

		return type;
	}

	private void checkApi(String fqn, Class<?> type) throws ModuleControllerDoesntImplementApiException, ModuleApiClassNotFoundException, ModuleControllerClassException  {
		if(fqn.trim().length() > 0) {
			// search api-class
			try {
				final Class<?> apiClass = Class.forName(fqn);

				// Controller really implements api?
				if(!Arrays.asList(type.getInterfaces()).stream().anyMatch(impl -> impl.isAssignableFrom(apiClass))) {
					throw new ModuleControllerDoesntImplementApiException();
				}

				//TODO Check, if already resolved
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

	private Class<?>[] findUnresolvedDependencies(Class<?>[] dependencies, TypeInfo[] internal) throws ModuleDependencyUnresolvableException {
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
					if(findInstanceOf(dependency) != null) {
						unresolved.remove(dependency);	
					}
				} catch (DependencyNotResolvedException e) {
				}
			}

		}
		return unresolved.toArray(new Class<?>[unresolved.size()]);
	}

}
