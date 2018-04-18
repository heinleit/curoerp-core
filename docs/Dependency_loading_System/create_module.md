# Create Module

1. Create a Java-Project which creates a jar-file named **x**.cmod.jar, where **x** is your unique Module-Name

2. Touch a new file named **cmod.yml** in your src-Folder

3. Fill your file with following content:

```yaml
name: Unique Module Name
version: Integer-Version, for example: 0001 or 1
*typeInfos:
 - type: TestType1
   api: API (Interface of TestType1)
 - type: TestType2
   api: API (Interface of TestType2)
*bootClass: Class, should be a type in typeInfos an implements de.curoerp.core.modularity.BootModule
*dependencies:
 - Module1
 - Module2..
```
 **\* => Property is optional**

4. Add CuroERP-Core_VERSION.jar as build-path

5. Add depend-Modules as build-path
