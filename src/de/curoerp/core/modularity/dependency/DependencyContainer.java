package de.curoerp.core.modularity.dependency;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map.Entry;

import de.curoerp.core.modularity.DependencyType;
import de.curoerp.core.modularity.SpecialDependency;
import de.curoerp.core.modularity.annotations.CuroNoDependency;
import de.curoerp.core.modularity.exception.DependencyNotResolvedException;

public class DependencyContainer {

	private ArrayList<Dependency> resolvedDependencies;
	private HashMap<DependencyType, Object> instanceDependencies = new HashMap<>();

	public DependencyContainer() {
		this.resolvedDependencies = new ArrayList<>();
	}

	/*
	 * Getter, 'Finder'
	 */
	/**
	 * find single instance of type (String)
	 * 
	 * @param fqcn {@link String} full qualified class name
	 * @return T instance of fqcn
	 * 
	 * @throws DependencyNotResolvedException
	 */
	public Object findSingleInstanceOf(String fqcn) throws DependencyNotResolvedException {
		if(!this.resolvedDependencies.stream().anyMatch(d -> d.classPath.equals(fqcn))) {
			try {
				return findSingleInstanceOf(Class.forName(fqcn));
			} catch (ClassNotFoundException e) {
				throw new DependencyNotResolvedException("class '" + fqcn + "' can not found in current runtime (module not loaded?)");
			}
		}

		return this.resolvedDependencies.stream().filter(d -> d.classPath.equals(fqcn)).toArray(c -> new Object[c]);
	}

	/**
	 * find single instance of type (Class<T>)
	 * 
	 * @param cls Class<T>
	 * @return T instance of cls
	 * 
	 * @throws DependencyNotResolvedException
	 */
	public Object findSingleInstanceOf(Class<?> cls) throws DependencyNotResolvedException {
		Object[] ts = findInstanceOf(cls);
		if(ts.length > 1) {
			throw new DependencyNotResolvedException("more than 1 Dependency of '" + cls.getName() + "'");
		}
		return ts[0];
	}

	/**
	 * find all instances of type (Class<T>)
	 * 
	 * @param cls Class<T>
	 * @return T instance of cls
	 * 
	 * @throws DependencyNotResolvedException
	 */
	public Object[] findInstanceOf(Class<?> cls) throws DependencyNotResolvedException {
		Object obj = this.processAnnotations(cls);
		if(obj != null) {
			return new Object[] {obj};
		}

		if(!this.resolvedDependencies.stream().anyMatch(d -> d.classPath.equals(cls.getName()))) {
			Dependency[] dependencies = this.resolvedDependencies.stream()
					.filter(e -> cls.isAssignableFrom(e.instance.getClass()))
					.toArray(c -> new Dependency[c]);
			if(dependencies.length > 0) {
				return dependencies;
			}

			throw new DependencyNotResolvedException("dependency '" + cls.getName() + "' not resolved");
		}

		return this.resolvedDependencies.stream().filter(d -> d.classPath.equals(cls.getName())).toArray(c -> new Object[c]);
	}

	/**
	 * process all dependency-annotations (View Github#11)
	 * 
	 * @param cls Class<?>
	 * @return Object (your instance of Class<T>)
	 * @throws DependencyNotResolvedException
	 */
	private Object processAnnotations(Class<?> cls) throws DependencyNotResolvedException {
		// is no dependency?
		CuroNoDependency aNoDependency = cls.getAnnotation(CuroNoDependency.class);
		if(aNoDependency != null) {
			throw new DependencyNotResolvedException("dependency '" + cls.getName() + "' is marked as not resolvable (@CuroNoDependency)");
		}

		// special dependency?
		SpecialDependency aSpecialDependency = cls.getAnnotation(SpecialDependency.class);
		if(aSpecialDependency != null) {
			if(this.instanceDependencies.containsKey(aSpecialDependency.type())) {
				return this.instanceDependencies.get(aSpecialDependency.type());
			}
		}

		return null;
	}
	
	/*
	 * Adder
	 */
	
	public void addResolvedDependency(Class<?> cls, Object instance) throws DependencyNotResolvedException {
		if(cls == null || instance == null) {
			throw new DependencyNotResolvedException("null is not an option");
		}
		if(!cls.isInstance(instance)) {
			throw new DependencyNotResolvedException("Object is not a instance of class '" + cls.getName() + "'");
		}
		
		Dependency dependency = new Dependency();
		dependency.classPath = cls.getName();
		dependency.instance = instance;
		this.resolvedDependencies.add(dependency);
	}

	
	/*
	 * Instance-Specific Dependencies
	 */

	/**
	 * clean & set instance-specific dependencies
	 * 
	 * @param map HashMap<DependencyType, Object>
	 */
	public void setSessionDependencies(HashMap<DependencyType, Object> map) {
		this.cleanSessionDependencies();
		for (Entry<DependencyType, Object> entry : map.entrySet()) {
			this.instanceDependencies.put(entry.getKey(), entry.getValue());
		}
	}

	/**
	 * Clean instance-specific dependencies
	 */
	public void cleanSessionDependencies() {
		this.instanceDependencies = new HashMap<>();
	}

}
