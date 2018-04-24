package de.curoerp.core.functionality;

import de.curoerp.core.exception.RuntimeTroubleException;
import de.curoerp.core.functionality.config.ConfigService;
import de.curoerp.core.functionality.config.IConfigService;
import de.curoerp.core.modularity.IDependencyService;

public class FunctionalityLoader {
	
	private IDependencyService _dependencyService;

	public FunctionalityLoader(IDependencyService dependencyService) {
		this._dependencyService = dependencyService;
	}

	public void initialize() {
		try {
			this._dependencyService.resolveTypes(ConfigService.class, IConfigService.class);
		} catch (Exception e) {
			throw new RuntimeTroubleException(e);
		}
	}
	
}
