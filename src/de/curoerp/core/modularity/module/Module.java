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
import de.curoerp.core.modularity.exception.ModuleVersionStringInvalidException;
import de.curoerp.core.modularity.versioning.VersionInfo;

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
	private VersionInfo version;
	
	/**
	 * Construct Module by Jar-File
	 * 
	 * @param Jar-{@link File}
	 */
	public Module(File file) {
		this.file = file;
	}
	
	
	
	/*
	 * #########################################################
	 * #                                                       #
	 * # Getter                                                #
	 * #                                                       #
	 * #########################################################
	 */
	
	/**
	 * Get System-Name
	 * 
	 * @return {@link String}
	 */
	public String getSystemName() {
		if(!this.isLoaded) return null;
		return Module.parseSystemName(this.info.name);
	}
	
	/**
	 * parse module name for system
	 * 
	 * @param name {@link String}
	 * @return {@link String}
	 */
	public static String parseSystemName(String name) {
		return name.toLowerCase().trim().replace(' ', '_');
	}
	
	/**
	 * Get Display Name (Name and Version)
	 * 
	 * @return {@link String}
	 */
	public String getDisplayName() {
		if(!this.isLoaded) return null;
		return this.info.name + " (" + this.version + ")";
	}
	
	/**
	 * Get Module-Version
	 * 
	 * @return {@link Integer}
	 */
	public VersionInfo getVersion() {
		if(!this.isLoaded) return null;
		return this.version;
	}
	
	/**
	 * Get Dependencies
	 * 
	 * @return {@link String}[]
	 */
	public String[] getDependencies() {
		if(!this.isLoaded) return null;
		return this.info.dependencies;
	}
	
	/**
	 * Get Dependency-Resolve Types
	 * 
	 * @return {@link TypeInfo}[]
	 */
	public TypeInfo[] getTypes() {
		if(!this.isLoaded) return null;
		return this.info.typeInfos;
	}
	
	/**
	 * Get Boot-Class for booting after heaping and dependency resolvement
	 * 
	 * @return {@link String}
	 */
	public String getBootClass() {
		if(!this.isLoaded) return null;
		return this.info.bootClass;
	}

	
	
	/*
	 * #########################################################
	 * #                                                       #
	 * # Information & Heaping                                 #
	 * #                                                       #
	 * #########################################################
	 */
	
	/**
	 * Fetch module-information from jar-file/cmod.yml
	 * 
	 * @throws ModuleCanNotBeLoadedException
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
			
			// VersionInfo
			this.version = new VersionInfo(info.version);
			
			// ModuleInfo
			this.info = info;
		} catch (IOException | ModuleVersionStringInvalidException e) {
			throw new ModuleCanNotBeLoadedException(e.getMessage());
		}
	}
	
	/**
	 * heap jar-file in actual runtime
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
			throw new ModuleCanNotBeLoadedException(e.getMessage());
		}
		finally {
			if(method != null) {
				method.setAccessible(false);
			}
		}
		
		this.isLoaded = true;
	}
	
}
