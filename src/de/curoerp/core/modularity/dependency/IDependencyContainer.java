package de.curoerp.core.modularity.dependency;

import de.curoerp.core.modularity.exception.DependencyNotResolvedException;

public interface IDependencyContainer {
	public Object findSingleInstanceOf(Class<?> cls) throws DependencyNotResolvedException;
	public Object findSingleInstanceOf(String fqcn) throws DependencyNotResolvedException;
	public Object[] findInstancesOf(Class<?> cls) throws DependencyNotResolvedException;
	public Object[]findInstancesOf(String fqcn) throws DependencyNotResolvedException;
}
