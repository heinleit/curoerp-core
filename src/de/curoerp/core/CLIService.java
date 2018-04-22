package de.curoerp.core;

import java.util.Arrays;
import java.util.List;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;

public class CLIService {

	private Options options;
	private String[] args;
	private CommandLineParser parser;

	public CLIService(String[] args) {
		this.args = args;
		this.options = new Options();
		this.parser = new DefaultParser();

		this.init();
	}

	private void init() {
		Option o = new Option("s", true, "Modules Space");
		o.setArgName("Directory");
		this.options.addOption(o);

		o = new Option("b", true, "Boot Module");
		o.setArgName("Module");
		this.options.addOption(o);

		o = new Option("l", true, "Logging Level (1-3, ERROR/WARN/INFO)");
		o.setArgName("Level");
		this.options.addOption(o);
	}

	public CommandLine getCli() throws ParseException {
		return parser.parse( options, args);
	}

	public void displayHelp() {
		HelpFormatter formatter = new HelpFormatter();
		formatter.printHelp( "java -jar CuroERP-Core_VERSION.jar", options );
	}

	public static boolean check(CommandLine cli, String...requirements) {
		List<String> options = Arrays.asList(Arrays.stream(cli.getOptions())
				.filter(o -> o.getValue() != null && o.getValue().trim().length() > 0)
				.map(o -> o.getOpt()).toArray(c -> new String[c]));

		for (String requirement : requirements) {
			if(!options.contains(requirement)) {
				return false;
			}
		}
		
		return true;
	}

}
