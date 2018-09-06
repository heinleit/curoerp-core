package de.curoerp.core.modularity.module;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.jar.JarFile;

import org.yaml.snakeyaml.Yaml;
import org.yaml.snakeyaml.constructor.Constructor;

public class ModuleInfo {
	
	public String name;
	public String version;
	public String[] dependencies = new String[0];
	public String[] libraries = new String[0];
	public TypeInfo[] typeInfos = new TypeInfo[0];
	public String bootClass;
	

	public final static Yaml YAML_MODULEINFO = new Yaml(new Constructor(ModuleInfo.class));

	public static ModuleInfo get(File cmodFile) throws IOException  {
		InputStream stream = new FileInputStream(cmodFile);
		ModuleInfo info = YAML_MODULEINFO.loadAs(stream, ModuleInfo.class);
		stream.close();
		return info;
	}
	
	public static ModuleInfo get(JarFile jarFile) throws IOException  {
		InputStream stream = jarFile.getInputStream(jarFile.getEntry("cmod.yml"));
		ModuleInfo info = YAML_MODULEINFO.loadAs(stream, ModuleInfo.class);
		stream.close();
		return info;
	}
}
