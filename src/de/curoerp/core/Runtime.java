package de.curoerp.core;

import java.io.File;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.ParseException;

import de.curoerp.core.exception.RuntimeTroubleException;
import de.curoerp.core.logging.LoggingService;
import de.curoerp.core.modularity.DependencyService;
import de.curoerp.core.modularity.ModuleService;
import de.curoerp.core.modularity.exception.DependencyNotResolvedException;

public class Runtime {

	public static void main(String[] args) {
		CLIService cli = new CLIService(args);
		CommandLine cmd = null;
		DependencyService dependency = new DependencyService();
		ModuleService modules = new ModuleService(dependency);
		
		// parse cli
		try {
			cmd = cli.getCli();
			if(!CLIService.check(cli.getCli(), "b", "s")) {
				throw new ParseException("");
			}
		} catch (ParseException e1) {
			cli.displayHelp();
			return;
		}
		
		LoggingService.info("Start DlS");
		
		// dependencies
		try {
			modules.loadModules(new File(cmd.getOptionValue("s")));
			modules.boot();
		} catch (RuntimeTroubleException e) {
			LoggingService.error(e);
			return;
		}

		LoggingService.info("DlS started!");
		

		LoggingService.info("Jump into Boot-Module");
		
		// finally: boot
		try {
			modules.runModule(cmd.getOptionValue("b"));
		} catch (RuntimeTroubleException | DependencyNotResolvedException e) {
			LoggingService.error(e);
			return;
		}
	}

}
