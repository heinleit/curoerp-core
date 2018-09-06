package de.curoerp.core;

import java.io.File;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.ParseException;

import de.curoerp.core.exception.RuntimeTroubleException;
import de.curoerp.core.functionality.FunctionalityLoader;
import de.curoerp.core.functionality.info.CoreInfo;
import de.curoerp.core.logging.Logging;
import de.curoerp.core.logging.LoggingLevel;
import de.curoerp.core.logging.LoggingService;
import de.curoerp.core.modularity.DependencyService;
import de.curoerp.core.modularity.ModuleService;
import de.curoerp.core.modularity.dependency.DependencyContainer;
import de.curoerp.core.modularity.exception.DependencyNotResolvedException;
import de.curoerp.core.modularity.module.ModuleInfo;

public class Runtime {

	private ModuleService modules;
	private DependencyService resolver;
	private CoreInfo info;
	private String bootModule;
	public Runtime(String bootModule, File baseFile) throws DependencyNotResolvedException {
		this(bootModule, baseFile, LoggingLevel.DEBUG);
	}

	public Runtime(String bootModule, File baseFile, LoggingLevel logging) throws DependencyNotResolvedException {
		this.bootModule = bootModule;
		// start logging-service
		LoggingService.DefaultLogging = new Logging(LoggingLevel.DEBUG);

		// start di container and resolver
		DependencyContainer container = new DependencyContainer();
		this.resolver = new DependencyService(container);

		// info
		LoggingService.info("Build CoreInfo");
		// Info isn't really resolvable, that's why we construct manually
		this.info = new CoreInfo(bootModule, baseFile);
		container.addResolvedDependency(this.info.getClass(), this.info);

		// modules
		this.modules = new ModuleService(this.resolver, container, this.info);

		// some nice shit
		FunctionalityLoader loader = new FunctionalityLoader(this.resolver);
		loader.initialize();
	}

	public void init(ModuleInfo[] infos) {
		try {
			this.modules.loadModules(infos);
			this.boot();
		} catch (RuntimeTroubleException e) {
			LoggingService.error(e);
			return;
		}
	}

	public void init() {
		LoggingService.info("Start DlS");

		// dependencies
		try {
			this.modules.loadModules(this.info.getModuleDir());
			this.boot();
		} catch (RuntimeTroubleException e) {
			LoggingService.error(e);
			return;
		}
	}

	private void boot() throws RuntimeTroubleException {
		this.modules.boot();

		LoggingService.info("DlS started!");

		LoggingService.info("Jump into Boot-Module");

		// finally: boot
		try {
			modules.runModule(this.bootModule);
		} catch (RuntimeTroubleException | DependencyNotResolvedException e) {
			LoggingService.error(e);
			return;
		}
	}

	public static void main(String[] args) throws DependencyNotResolvedException {

		CLIService cli = new CLIService(args);
		CommandLine cmd = null;

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

		// logging-level
		LoggingLevel level = null;
		if(CLIService.check(cmd, "l")) {
			switch (cmd.getOptionValue("l").toLowerCase().trim()) {
			case "error":
			case "1":
				level = LoggingLevel.ERROR;
				break;
			case "warn":
			case "2":
				level = LoggingLevel.WARN;
				break;
			case "info":
			case "3":
				level = LoggingLevel.INFO;
				break;
			default:
				level = LoggingLevel.DEBUG;
				break;
			}
		}

		Runtime r = new Runtime(cmd.getOptionValue("b"), new File(cmd.getOptionValue("s")), level);
		r.init();
	}

}
