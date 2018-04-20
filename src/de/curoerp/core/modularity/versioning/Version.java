package de.curoerp.core.modularity.versioning;

import de.curoerp.core.modularity.exception.ModuleVersionStringInvalidException;

public class Version {
	
	private String version;
	private long numericVersion;
	
	public Version(String version) {
		this.version = version;
	}
	
	/*
	 * Getter
	 */
	public long getVersion() {
		return this.numericVersion;
	}
	
	
	/*
	 * Parsing
	 */
	public void parse() throws ModuleVersionStringInvalidException {
		this.numericVersion = parse(this.version);
	}
	
	private static long parse(String version) throws ModuleVersionStringInvalidException {
		String[] sSubVersions = version.split("\\.");
		long numericVersion = 0;
		
		for (int i = 0; i < sSubVersions.length; i++) {
			String sSubVersion = sSubVersions[i];
			int iSubVersion = -1;
			
			try {
				iSubVersion = Integer.parseInt(sSubVersion);
			} catch(NumberFormatException e) { }
			
			if(iSubVersion < 0) {
				throw new ModuleVersionStringInvalidException("version-part '" + sSubVersion + "' smaller than 0 or not parsable");
			}
			
			if(iSubVersion > 999) {
				throw new ModuleVersionStringInvalidException("version-part '" + sSubVersion + "' to large (max. 999)");
			}
			
			long multiplicator = (long)Math.pow(10000, (sSubVersions.length - i) - 1) * 1000;
			numericVersion += multiplicator * iSubVersion;
		}
		
		return numericVersion;
	}
	
	
	/*
	 * Version Check
	 */
	public boolean match(long numericVersion, VersionExpressions expression) {
		switch (expression) {
		case AFTER:
			return this.numericVersion > numericVersion;
		case AFTER_AND_SAME:
			return this.numericVersion >= numericVersion;
		case BEFORE:
			return this.numericVersion < numericVersion;
		case BEFORE_AND_SAME:
			return this.numericVersion <= numericVersion;
		case NOT:
			return this.numericVersion != numericVersion;
		case DEFAULT:
		case SAME:
			return this.numericVersion == numericVersion;
		}
		return false;
	}
	
	public boolean match(String version, VersionExpressions expression) throws ModuleVersionStringInvalidException {
		return this.match(Version.parse(version), expression);
	}
	
	public boolean allMatch(Long[] versions, VersionExpressions[] expressions) {
		for(int i = 0; i < versions.length; i++) {
			if(!this.match(versions[i], expressions[i])) 
				return false;
		}
		return true;
	}
	
	public boolean allMatch(long[] versions, VersionExpressions[] expressions) {
		for(int i = 0; i < versions.length; i++) {
			if(!this.match(versions[i], expressions[i])) 
				return false;
		}
		return true;
	}
	
	public boolean allMatch(String[] versions, VersionExpressions[] expressions) throws ModuleVersionStringInvalidException {
		// map in numeric-versions > no lambda, because its exceptional
		long[] numericVersions = new long[versions.length];
		for(int i = 0; i < versions.length; i++) {
			numericVersions[i] = Version.parse(versions[i]);
		}
		
		return allMatch(numericVersions, expressions);
	}
}
