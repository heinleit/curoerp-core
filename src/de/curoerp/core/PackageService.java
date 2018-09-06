package de.curoerp.core;

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import de.curoerp.core.logging.LoggingService;

public class PackageService {

	private Package root;

	public PackageService() {
		this.load();
	}

	public void load() {
		this.root = new Package(true, "root");

		for (java.lang.Package pack : Arrays.stream(java.lang.Package.getPackages()).collect(Collectors.toList())) {
			Package last = this.root;
			for (String name : pack.getName().split("\\.")) {
				last = last.addAndGet(name);
			}
		}
	}

	public File[] findFiles(String pattern) {
		return this.searchRecursive(this.root, pattern, new String[0]).stream().toArray(c -> new File[c]);
	}


	private List<File> searchRecursive(Package pack, String pattern, String[] path) {
		ArrayList<File> buffer = new ArrayList<>(); 
		String[] partPath = pack.root ? new String[0] :  Arrays.copyOfRange(path, 1, path.length);
		
		// only in root for cp .
		if(partPath.length > 0 || pack.isRoot()) {
			
			// search here
			ClassLoader loader = PackageService.class.getClassLoader();
			InputStream in = loader.getResourceAsStream(partPath.length == 0 ? "." : String.join(".", partPath));
			if(in != null) {
				BufferedReader rdr = new BufferedReader(new InputStreamReader(in));
				String line;
				try {
					while ((line = rdr.readLine()) != null) {
						String fqFileName = (partPath.length == 0 ? "" : "/") + String.join("/", partPath) + "/" + line;
						
						// only files
						File file = new File(PackageService.class.getResource(fqFileName).toURI());
						if(!file.isFile()) continue;
						
						// pattern match
						if(!file.getName().matches(pattern)) continue;
						
						buffer.add(file);
					}
					rdr.close();
				} catch (Exception e) {
					LoggingService.error(e);
				}
			}
		}
		

		// search in children
		for (Package child : pack.getChildren()) {
			buffer.addAll(this.searchRecursive(child, pattern, merge(path, new String[] { pack.getName() })));
		}

		return buffer;
	}

	private <T> T[] merge(T[] f, T[] s) {
		T[] result = Arrays.copyOf(f, f.length + s.length);
		System.arraycopy(s, 0, result, f.length, s.length);
		return result;
	}


	public class Package {
		private ArrayList<Package> children;
		private boolean root;
		private String name;

		public Package(boolean root, String name) {
			this.children = new ArrayList<>();
			this.root = root;
			this.name = name;
		}

		public boolean isRoot() {
			return this.root;
		}

		public String getName() {
			return this.name;
		}

		public Package[] getChildren() {
			return this.children.toArray(new Package[this.children.size()]);
		}

		public Package addAndGet(String name) {
			Package p = this.children.stream().filter(c -> c.getName().equals(name)).findFirst().orElse(null);
			if(p == null) {
				p = new Package(false, name);
				this.children.add(p);
			}
			return p;
		}
	}
}
