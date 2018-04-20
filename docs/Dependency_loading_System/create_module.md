# Create Module

1. Create a Java-Project which creates a jar-file named **x**.cmod.jar, where **x** is your unique Module-Name

2. Touch a new file named **cmod.yml** in your src-Folder

3. Fill your file with following content:

```yaml
name: Unique Module Name
version: Version, Format: X.X.X... (min. 1 Block (X), only integer, max. 999 per Block)
*typeInfos:
 - type: TestType1
   api: API (Interface of TestType1)
 - type: TestType2
   api: API (Interface of TestType2)
*bootClass: Class, should be a type in typeInfos an implements de.curoerp.core.modularity.BootModule
*dependencies:
 - <Name>[:Version-Limitation]
```
 **\* => Property is optional**

 Dependency version-limitation:

* Expressions:
    * &gt; greater than
    * \&gt;= greater than or same
    * < smaller than
    * < smaller than or same
    * = (default) same
    * ! not
* Use:
    * multiple limitations: comma-separated (and-gate)
    * limitation construction: [expression]Version
    * expression is optional, and the default is 'same'

4. Add CuroERP-Core_VERSION.jar as build-path

5. Add depend-Modules as build-path
