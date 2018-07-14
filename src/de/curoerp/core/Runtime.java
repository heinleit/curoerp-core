package de.curoerp.core;

import java.io.File;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.ParseException;

import de.curoerp.core.exception.RuntimeTroubleException;
import de.curoerp.core.functionality.FunctionalityLoader;
import de.curoerp.core.functionality.info.CoreInfo;
import de.curoerp.core.functionality.info.ICoreInfo;
import de.curoerp.core.logging.Logging;
import de.curoerp.core.logging.LoggingLevel;
import de.curoerp.core.logging.LoggingService;
import de.curoerp.core.modularity.DependencyService;
import de.curoerp.core.modularity.ModuleService;
import de.curoerp.core.modularity.dependency.DependencyContainer;
import de.curoerp.core.modularity.exception.DependencyNotResolvedException;

public class Runtime {

	public static void main(String[] args) throws DependencyNotResolvedException {
		// start logging-service
		LoggingService.DefaultLogging = new Logging(LoggingLevel.DEBUG);

		CLIService cli = new CLIService(args);
		CommandLine cmd = null;
		DependencyContainer container = new DependencyContainer();
		DependencyService resolver = new DependencyService(container);

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
		
		LoggingService.info("Build CoreInfo");
		// Info isn't really resolvable, that's why we construct manually
		ICoreInfo info = new CoreInfo(cmd.getOptionValue("b"), new File(cmd.getOptionValue("s")));
		container.addResolvedDependency(info.getClass(), info);

		ModuleService modules = new ModuleService(resolver, container, info);

		// logging-level
		if(CLIService.check(cmd, "l")) {
			switch (cmd.getOptionValue("l").toLowerCase().trim()) {
			case "error":
			case "1":
				LoggingService.DefaultLogging.setLoggingLevel(LoggingLevel.ERROR);
				break;
			case "warn":
			case "2":
				LoggingService.DefaultLogging.setLoggingLevel(LoggingLevel.WARN);
				break;
			case "info":
			case "3":
				LoggingService.DefaultLogging.setLoggingLevel(LoggingLevel.ERROR);
				break;
			default:
				LoggingService.DefaultLogging.setLoggingLevel(LoggingLevel.DEBUG);
				break;
			}
		}
		
		FunctionalityLoader loader = new FunctionalityLoader(resolver);
		loader.initialize();

		LoggingService.info("Start DlS");

		// dependencies
		try {
			modules.loadModules(info.getModuleDir());
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
