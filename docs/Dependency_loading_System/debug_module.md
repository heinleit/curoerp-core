# Debug Modules

For some reasons it is very practically to debug your model without building (FE: finetuning).

## HowTo (eclipse)

1. Create a new Java-Project in eclipse IDE named CuroERP-Debugging

2. Add a refernce to CuroERP-Core and to every Module (in debugmode the jar-heaping will skipped)

3. Create a unique namespace (FE: *de.curoerp.debugging*)

4. Create a starter-class, with content like this:

``` java
public static void main(String[] args) {
	
	Runtime r = new Runtime("lowercase module name", new File("some/path/here"), LoggingLevel.DEBUG, true);
    r.init();
}
```

Be sure, that your debugging-project depend on core and your modules!

**That's it!**