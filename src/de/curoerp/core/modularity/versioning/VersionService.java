package de.curoerp.core.modularity.versioning;

import de.curoerp.core.modularity.exception.ModuleVersionStringInvalidException;

public class VersionService {

	/**
	 * Parse Version-String in numeric version
	 * Format #/##/###.#/##/###.#/##/###...
	 * 
	 * Example: 1.6.8 => 1006008
	 * 			900.0.999 => 900000999
	 * 
	 * min 0, max 9 per position
	 * 
	 * @param Version {@link String}
	 * @return {@link Long} Version
	 * 
	 * @throws ModuleVersionStringInvalidException
	 */
	public static long parse(String version) throws ModuleVersionStringInvalidException {
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

			long multiplicator = (long)Math.pow(1000, (sSubVersions.length - i) - 1);
			numericVersion += multiplicator * iSubVersion;
		}

		return numericVersion;
	}

	/**
	 * reverse-engineering: version-number to version-string
	 * 
	 * @param version {@link Long}
	 * @return {@link String}
	 */
	public static String parse(Long version) {
		int positions = (int) Math.ceil((double)String.valueOf(version).length()/3D);
		StringBuilder versionBuilder = new StringBuilder();
		for(int i = 0; i < positions; i++) {
			// find number
			long multiplicator = (long)Math.pow(1000, (positions - i) - 1);
			long v = version/multiplicator;
			
			// append number and dot
			versionBuilder.append(versionBuilder.length() > 0 ? "." : "");
			versionBuilder.append(v);
			
			// correcting version for under-versions
			version -= v*multiplicator;
		}

		return versionBuilder.toString();
	}

	/**
	 * check expression-match between first-second-values
	 * 
	 * @param firstInfo {@link VersionInfo}
	 * @param secondInfo {@link VersionInfo}
	 * @param expression {@link VersionExpression}
	 * @return {@link Boolean}
	 */
	public static boolean match(VersionInfo firstInfo, VersionInfo secondInfo, VersionExpression expression) {
		switch (expression) {
		case AFTER:
			return firstInfo.getVersionNumeric() > secondInfo.getVersionNumeric();
		case AFTER_AND_SAME:
			return firstInfo.getVersionNumeric() >= secondInfo.getVersionNumeric();
		case BEFORE:
			return firstInfo.getVersionNumeric() < secondInfo.getVersionNumeric();
		case BEFORE_AND_SAME:
			return firstInfo.getVersionNumeric() <= secondInfo.getVersionNumeric();
		case NOT:
			return firstInfo.getVersionNumeric() != secondInfo.getVersionNumeric();
		case DEFAULT:
		case SAME:
			return firstInfo.getVersionNumeric() == secondInfo.getVersionNumeric();
		}
		return false;
	}
}
