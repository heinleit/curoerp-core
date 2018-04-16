package de.curoerp.core.modularity.language;

import de.curoerp.core.modularity.DependencyType;
import de.curoerp.core.modularity.SpecialDependency;

@SpecialDependency(type=DependencyType.LocaleService)
public interface ILocaleService {
	
	public String get(String key);
	
}
