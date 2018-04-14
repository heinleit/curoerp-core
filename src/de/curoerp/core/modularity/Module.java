package de.curoerp.core.modularity;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Method;
import java.net.URI;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.jar.JarFile;

public class Module {
	
	private File file;
	private JarFile jarFile;
	private boolean isLoaded = false;
	
	public Module(File file) {
		this.file = file;
	}
	
	public String getModuleName() {
		return this.file.getName();
	}
	
	public void loadInfo() throws ModuleCanNotBeLoadedException {
		try {
			this.jarFile = new JarFile(this.file);
			
		} catch (IOException e) {
			throw new ModuleCanNotBeLoadedException();
		}
	}
	
	/**
	 * This 
	 * @throws ModuleFileAlreadyLoadedException 
	 * @throws ModuleCanNotBeLoadedException 
	 */
	public void loadFile() throws ModuleFileAlreadyLoadedException, ModuleCanNotBeLoadedException {
		if(this.isLoaded) {
			throw new ModuleFileAlreadyLoadedException();
		}
		
		Method method = null;
		try {
			URLClassLoader classLoader = (URLClassLoader)ClassLoader.getSystemClassLoader();
			method = URLClassLoader.class.getDeclaredMethod("addURL", URL.class);
			method.setAccessible(true);
			method.invoke(classLoader, new URI(this.file.toString()).toURL());
		} catch(Exception e) {
			throw new ModuleCanNotBeLoadedException();
		}
		
		if(method != null) {
			method.setAccessible(false);
		}
		
		this.isLoaded = true;
	}
	
}
