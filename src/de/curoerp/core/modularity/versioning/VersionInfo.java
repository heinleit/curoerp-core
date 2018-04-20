package de.curoerp.core.modularity.versioning;

import de.curoerp.core.modularity.exception.ModuleVersionStringInvalidException;

public class VersionInfo {
	
	private long versionNumber;
	
	public VersionInfo(String version) throws ModuleVersionStringInvalidException {
		this.versionNumber = VersionService.parse(version);
	}
	
	/**
	 * get version-number
	 * 
	 * @return {@link Long}
	 */
	public Long getVersionNumeric() {
		return this.versionNumber;
	}

	/**
	 * get character-version name (reparsed)
	 * 
	 * @return {@link String}
	 */
	public String getVersionName() {
		return VersionService.parse(this.versionNumber);
	}
	
	/**
	 * check second version and expression match
	 * 
	 * [this] expression [second]
	 * 
	 * @param version {@link VersionInfo}
	 * @param expression {@link VersionExpression}
	 * @return {@link Boolean}
	 */
	public boolean match(VersionInfo version, VersionExpression expression) {
		return VersionService.match(this, version, expression);
	}
	
	
	/**
	 * every VersionExpressionSet match this version
	 * 
	 * @param sets {@link VersionExpressionSet}[]
	 * @return {@link Boolean}
	 */
	public boolean allMatch(VersionExpressionSet[] sets) {
		for (VersionExpressionSet set : sets) {
			if(!match(set.version, set.expression)) {
				return false;
			}
		}
		return true;
	}

	/**
	 * one VersionExpressionSet match this version
	 * 
	 * @param sets {@link VersionExpressionSet}[]
	 * @return {@link Boolean}
	 */
	public boolean anyMatch(VersionExpressionSet[] sets) {
		for (VersionExpressionSet set : sets) {
			if(match(set.version, set.expression)) {
				return true;
			}
		}
		return false;
	}
	
	
	/*
	 * overwriting
	 */
	
	@Override
	public String toString() {
		return this.getVersionName();
	}
	
}
