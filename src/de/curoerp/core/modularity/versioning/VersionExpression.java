package de.curoerp.core.modularity.versioning;

public enum VersionExpression {
	BEFORE("<"),
	BEFORE_AND_SAME("<="),
	AFTER(">"),
	AFTER_AND_SAME(">="),
	SAME("="),
	NOT("!"),
	DEFAULT("");
	
	public final String pattern;
	private VersionExpression(String pattern) {
		this.pattern = pattern;
	}
}
