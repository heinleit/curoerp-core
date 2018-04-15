package de.curoerp.core.modularity;

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
import de.curoerp.core.modularity.info.ModuleInfo;

public class Module {
	
	private File file;
	private boolean isLoaded = false;
	private ModuleInfo info;
	
	public Module(File file) {
		this.file = file;
	}
	
	public ModuleInfo getInfo() {
		return this.info;
	}
	
	public void loadInfo() throws ModuleCanNotBeLoadedException {
		ObjectMapper mapper = new ObjectMapper(new YAMLFactory());
		try {
			JarFile jarFile = new JarFile(this.file);
			InputStream stream = jarFile.getInputStream(jarFile.getEntry("cmod.yml"));
			ModuleInfo info = mapper.readValue(stream, ModuleInfo.class);
			
			//cleanup
			stream.close();
			jarFile.close();
			
			this.info = info;
		} catch (IOException e) {
			throw new ModuleCanNotBeLoadedException();
		}
	}
	
	/**
	 * This 
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
			method.invoke(classLoader, new URL("file://" + this.file));
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
