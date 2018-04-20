package de.curoerp.core.modularity.module;

import java.util.ArrayList;
import java.util.Arrays;

import de.curoerp.core.logging.LoggingService;
import de.curoerp.core.modularity.exception.ModuleVersionStringInvalidException;
import de.curoerp.core.modularity.versioning.Version;
import de.curoerp.core.modularity.versioning.VersionExpressions;

public class DependencyService {
	
	public static DependencyInfo[] parseDependencies(String[] sDependencies) throws ModuleVersionStringInvalidException {			
		ArrayList<DependencyInfo> dependencies = new ArrayList<>();
		for (String sDependency : sDependencies) {
			LoggingService.info("DependencyService: parse dependency '" + sDependency + "'");
			
			DependencyInfo dependency = new DependencyInfo();
			//dependency.name = sDependency;
			String[] aDependency = sDependency.split(":");
			
			if(aDependency.length == 1) {
				dependency.versions = new DependencyVersion[0];
				LoggingService.info("DependencyService: dependency '" + sDependency + "' has no limitations");
			}
			else {
				String[] limitations = aDependency[1].split(",");
				ArrayList<DependencyVersion> versions = new ArrayList<>();

				for (String limit : limitations) {
					if(limit.trim().length() == 0) {
						continue;
					}
					
					DependencyVersion version = DependencyService.getDependency(limit);
					versions.add(version);
					
					if(version == null) {
						throw new ModuleVersionStringInvalidException("an dependency-version can not resolved (" + limit + ")");
					}
					
					LoggingService.info("DependencyService: dependency '" + sDependency + "' limit: " + limit + ", version: " + version.value.getVersion() + ", expression: " + version.expression.toString() + " (" + version.expression.pattern + ")");
					
				}
				
				dependency.versions = versions.toArray(new DependencyVersion[versions.size()]);
			}
			
			dependency.name = aDependency[0];
			dependencies.add(dependency);
		}
		return dependencies.toArray(new DependencyInfo[dependencies.size()]);
	}

	public static DependencyVersion getDependency(String limit) {
		return Arrays.stream(VersionExpressions.values())
		.filter(expression -> limit.startsWith(expression.pattern))
		.map(expression -> DependencyService.parseVersion(limit.substring(expression.pattern.length()), expression))
		.filter(dependencyVersion -> dependencyVersion != null)
		.findFirst().orElse(null);

	}
	
	public static DependencyVersion parseVersion(String sVersion, VersionExpressions expression) {
		
		try {
			Version version = new Version(sVersion);
			version.parse();

			DependencyVersion dependency = new DependencyVersion();
			dependency.expression = expression;
			dependency.value = version;
			
			return dependency;
		} catch(ModuleVersionStringInvalidException e) {
			LoggingService.error(e);
		}
		
		return null;
	}
}
