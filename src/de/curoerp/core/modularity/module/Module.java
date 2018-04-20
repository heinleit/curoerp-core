package de.curoerp.core.modularity.module;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Method;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.jar.JarFile;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;

import de.curoerp.core.modularity.exception.ModuleCanNotBeLoadedException;
import de.curoerp.core.modularity.exception.ModuleFileAlreadyLoadedException;
import de.curoerp.core.modularity.exception.ModuleNameInvalidException;
import de.curoerp.core.modularity.exception.ModuleVersionStringInvalidException;
import de.curoerp.core.modularity.versioning.Version;

/**
 * Module-Model for..
 * @category Dependency loading System
 * 
 * @author Hendrik Heinle
 * @since 15.04.2018
 */
public class Module implements IModule {

	private File file;
	private boolean isLoaded = false;
	private ModuleInfo info;
	private Version version;
	private DependencyInfo[] dependencies;
	private String name;

	public Module(File file) {
		this.file = file;
	}
	
	public String getBootClass() {
		return this.info.getBootClass();
	}
	
	public TypeInfo[] getTypes() {
		return this.info.getTypeInfos();
	}


	/*
	 * Infos
	 */

	public DependencyInfo[] getDependencies() {
		return this.dependencies;
	}

	public Version getVersion() {
		return this.version;
	}

	public String getName() {
		return this.name;
	}
	
	public String getDisplayName() {
		return this.info.getName() + " (" + this.info.getVersion() + ")";
	}



	/*
	 * Load&Heaping
	 */

	public void loadInfo() throws ModuleCanNotBeLoadedException {
		ObjectMapper mapper = new ObjectMapper(new YAMLFactory());
		try {
			JarFile jarFile = new JarFile(this.file);
			InputStream stream = jarFile.getInputStream(jarFile.getEntry("cmod.yml"));
			ModuleInfo info = mapper.readValue(stream, ModuleInfo.class);

			//cleanup
			stream.close();
			jarFile.close();

			// Module Info
			this.info = info;

			// check name (after define ModuleInfo!)
			String name = info.getName().trim().toLowerCase().replace(' ', '_');
			if(name.contains(":")) {
				throw new ModuleNameInvalidException("module name contains invalid character (:)");
			}
			else if(name.length() == 0) {
				throw new ModuleNameInvalidException("module name must contains 1 or more characters!");
			}
			this.name = name;

			// DependencyInfo (z.B. Module:>10)
			this.dependencies = DependencyService.parseDependencies(info.getDependencies());

			//Version
			this.version = new Version(info.getVersion());
			this.version.parse();
		} catch (IOException | ModuleVersionStringInvalidException | ModuleNameInvalidException e) {
			this.info = null;
			this.version = null;
			this.dependencies = null;
			e.printStackTrace();

			throw new ModuleCanNotBeLoadedException();
		}
	}

	/**
	 * 
	 * @throws ModuleFileAlreadyLoadedException 
	 * @throws ModuleCanNotBeLoadedException 
	 */
	public void fetchJar() throws ModuleFileAlreadyLoadedException, ModuleCanNotBeLoadedException {
		if(this.isLoaded) {
			throw new ModuleFileAlreadyLoadedException();
		}

		Method method = null;
		try {
			URLClassLoader classLoader = (URLClassLoader)ClassLoader.getSystemClassLoader();
			method = URLClassLoader.class.getDeclaredMethod("addURL", URL.class);
			method.setAccessible(true);
			method.invoke(classLoader, this.file.toURI().toURL());
		}
		catch(Exception e) {
			e.printStackTrace();
			throw new ModuleCanNotBeLoadedException();
		}
		finally {
			if(method != null) {
				method.setAccessible(false);
			}
		}

		this.isLoaded = true;
	}

}
