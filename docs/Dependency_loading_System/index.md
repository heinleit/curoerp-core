# Dependency loading System

Lightweight and only for this Project developed

## Real Modularity?

Yes bitch!

## Create Module

1. Create a Java-Project which creates a jar-file named **x**.cmod.jar, where **x** is your unique Module-Name

2. Touch a new file named **cmod.yml** in your src-Folder

3. Fill your file with following content:

```yaml
name: Unique Module Name
version: Integer-Version, for example: 0001 or 1
(*)typeInfos:
 - type: TestType1
   api: API (Interface of TestType1)
 - type: TestType2
   api: API (Interface of TestType2)
(*)bootClass: Class, should be a type in typeInfos an implements de.curoerp.core.modularity.BootModule
(*)dependencies:
 - Module1
 - Module2..
```

4. Add CuroERP-Core_VERSION.jar as build-path

5. Add depend-Modules as build-path

Let's go!

## Load Module

Put your jar-file in your modules-path, defined in cli (`-s`).

## Boot Module

1. Be sure in **cmod.yml**-File is an bootClass defined, which is resolved by DlS!

2. Start CuroERP-Core with cli-parameter `-b` and your module-name.
