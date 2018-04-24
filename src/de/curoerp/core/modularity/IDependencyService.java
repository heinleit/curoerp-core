package de.curoerp.core.modularity;

import de.curoerp.core.modularity.exception.DependencyNotResolvedException;
import de.curoerp.core.modularity.exception.ModuleApiClassNotFoundException;
import de.curoerp.core.modularity.exception.ModuleCanNotBootedException;
import de.curoerp.core.modularity.exception.ModuleControllerClassException;
import de.curoerp.core.modularity.exception.ModuleControllerDoesntImplementApiException;
import de.curoerp.core.modularity.exception.ModuleDependencyUnresolvableException;
import de.curoerp.core.modularity.module.TypeInfo;

public interface IDependencyService {
	public void resolveTypes(TypeInfo[] typeInfos) 
			throws ModuleDependencyUnresolvableException, ModuleControllerClassException, 
			ModuleControllerDoesntImplementApiException, ModuleApiClassNotFoundException, 
			ModuleCanNotBootedException, DependencyNotResolvedException;
	
	public void resolveType(Class<?> type, Class<?> api) 
			throws DependencyNotResolvedException, ModuleCanNotBootedException, 
			ModuleDependencyUnresolvableException, ModuleControllerClassException, 
			ModuleControllerDoesntImplementApiException, ModuleApiClassNotFoundException;
}
