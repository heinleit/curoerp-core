# Debug Modules

For some reasons it is very practically to debug your model without building (FE: finetuning).

## HowTo (eclipse

1. Create a new Java-Project in eclipse IDE named CuroERP-Debugging

2. Add a refernce to CuroERP-Core and to every Module (because of the no-runtime-heaping-mode)

3. Create a unique namespace (FE: *de.curoerp.debugging*)

4. Create a starter-class, with content like this:

``` java
public static void main(String[] args) {
	// logging / console (everything for debugging)
	LoggingService.DefaultLogging = new Logging(LoggingLevel.INFO);
	
	// dependency-container and module-service
	DependencyContainer container = new DependencyContainer();
	ModuleService modules = new ModuleService(container);
	
	// dependencies
	try {
		
		// Module: Pipeline
		ModuleInfo info = new ModuleInfo();
		info.name = "Pipeline";
		info.bootClass = "jar.curoerp.module.pipeline.Pipeline";
		info.typeInfos = new TypeInfo[] {
				new TypeInfo("jar.curoerp.module.pipeline.PipelineInvocationHandler", ""),
				new TypeInfo("jar.curoerp.module.pipeline.Pipeline", "")
		};
		Module pipeline = new Module(new VersionInfo("1"), info);
		
		// Module XYZ etc.
		
		
		// 'Heap' modules in ModuleService (runtime isn't necessary)
		modules.setModules(new Module[] {pipeline});
		
		// resolve dependencies
		modules.boot();
	} catch (RuntimeTroubleException | DependencyLimitationException | ModuleVersionStringInvalidException e) {
		LoggingService.error(e);
		return;
	}

	LoggingService.info("DlS started!");
	

	LoggingService.info("Jump into Boot-Module");
	
	// finally: boot (module: pipeline)
	try {
		modules.runModule("pipeline");
	} catch (RuntimeTroubleException | DependencyNotResolvedException e) {
		LoggingService.error(e);
		return;
	}

}
```

**That's it!**
