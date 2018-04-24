package de.curoerp.core.modularity.dependency;

import java.util.HashMap;

import de.curoerp.core.modularity.DependencyType;
import de.curoerp.core.modularity.exception.DependencyNotResolvedException;

public interface IDependencyContainer {
	public Object findSingleInstanceOf(Class<?> cls) throws DependencyNotResolvedException;
	public Object findSingleInstanceOf(String fqcn) throws DependencyNotResolvedException;
	public Object[] findInstancesOf(Class<?> cls) throws DependencyNotResolvedException;
	public Object[]findInstancesOf(String fqcn) throws DependencyNotResolvedException;
	public void addResolvedDependency(Class<?> cls, Object instance) throws DependencyNotResolvedException;
	public void setSessionDependencies(HashMap<DependencyType, Object> map);
	public void cleanSessionDependencies();
}
