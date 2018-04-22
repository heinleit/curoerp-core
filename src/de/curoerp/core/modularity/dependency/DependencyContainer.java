package de.curoerp.core.modularity.dependency;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map.Entry;

import de.curoerp.core.modularity.DependencyType;
import de.curoerp.core.modularity.SpecialDependency;
import de.curoerp.core.modularity.annotations.CuroMultiDependency;
import de.curoerp.core.modularity.annotations.CuroNoDependency;
import de.curoerp.core.modularity.exception.DependencyNotResolvedException;

public class DependencyContainer implements IDependencyContainer {

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
		try {
			return findSingleInstanceOf(Class.forName(fqcn));
		} catch (ClassNotFoundException e) {
			throw new DependencyNotResolvedException("class '" + fqcn + "' can not found in current runtime (module not loaded?)");
		}
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
		// is multi-dependency?
		if(this.isMultiDependency(cls)) {
			throw new DependencyNotResolvedException("You are searching for a MDO (@CuroMultiDependency)! Please use findInstancOf instead of findSingleInstanceOf!");
		}
		
		Object[] ts = findInstancesOf(cls);
		if(ts.length > 1) {
			throw new DependencyNotResolvedException("more than 1 dependency of '" + cls.getName() + "'");
		}
		return ts[0];
	}

	/**
	 * find all instances of type (String)
	 * 
	 * @param fqcn {@link String} full qualified class name
	 * @return T instances of fqcn
	 * 
	 * @throws DependencyNotResolvedException
	 */
	@Override
	public Object[] findInstancesOf(String fqcn) throws DependencyNotResolvedException {
		try {
			return findInstancesOf(Class.forName(fqcn));
		} catch (ClassNotFoundException e) {
			throw new DependencyNotResolvedException("class '" + fqcn + "' can not found in current runtime (module not loaded?)");
		}
	}

	/**
	 * find all instances of type (Class<T>)
	 * 
	 * @param cls Class<T>
	 * @return T instance of cls
	 * 
	 * @throws DependencyNotResolvedException
	 */
	public Object[] findInstancesOf(Class<?> cls) throws DependencyNotResolvedException {
		// check special: this
		if(cls.getName().equals(IDependencyContainer.class.getName())) {
			return new Object[] {this};
		}
		
		// check special: annotations
		Object obj = this.processAnnotations(cls);
		if(obj != null) {
			return new Object[] {obj};
		}

		// check special: parent-classes
		if(!this.resolvedDependencies.stream().anyMatch(d -> d.classPath.equals(cls.getName()))) {
			Dependency[] dependencies = this.resolvedDependencies.stream()
					.filter(e -> cls.isAssignableFrom(e.instance.getClass()))
					.toArray(c -> new Dependency[c]);
			if(dependencies.length > 0) {
				return dependencies;
			}

			throw new DependencyNotResolvedException("dependency '" + cls.getName() + "' not resolved");
		}

		// everything is good!
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
		// is not an dependency?
		if(this.isNoDependency(cls)) {
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

	private boolean isNoDependency(Class<?> cls) {
		return cls.getAnnotation(CuroNoDependency.class) != null;
	}
	
	private boolean isMultiDependency(Class<?> cls) {
		return cls.getAnnotation(CuroMultiDependency.class) != null;
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
