package com.github.nethad.clustermeister.integration.sc07;

import java.lang.annotation.Annotation;
import java.net.URL;
import java.net.URLClassLoader;

import org.jppf.classloader.AbstractJPPFClassLoader;
import org.jppf.classloader.DelegationModel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class JPPFClassloader extends ClassLoader {
	
	private final Logger logger = LoggerFactory.getLogger(JPPFClassloader.class);
	
	private AbstractJPPFClassLoader classLoader;

	public JPPFClassloader() {
		if (this.getClass().getClassLoader() instanceof AbstractJPPFClassLoader) {
			classLoader = (AbstractJPPFClassLoader) this.getClass().getClassLoader();
			System.out.println("JPPFClassloader successfully initialized.");
	        
	        URL[] urls = classLoader.getURLs();
	        System.out.println("Classloader urls length = "+urls.length);
	        for(URL url: urls){
	        	System.out.println(url.getFile());
	        }
	        
	        urls = ((URLClassLoader)ClassLoader.getSystemClassLoader()).getURLs();
	        System.out.println("System classloader urls length = "+urls.length);
	        for(URL url: urls){
	        	System.out.println(url.getFile());
	        }
		} else {
			System.out.println("JPPFClassloader could not be initialized. :(");
		}
	}
	
	public AbstractJPPFClassLoader getClassLoader() {
		return classLoader;
	}
	
	@Override
	protected synchronized Class<?> loadClass(String name, boolean resolve) {
		System.out.println(String.format("JPPFClassloader.loadClass(%s,%s)", name, resolve));
		try {
			Class<?> clazz;
			clazz = classLoader.loadClass(name);
			Annotation[] annotations = clazz.getAnnotations();
			System.out.println(String.format("%s has %d annotations.", clazz.getName(), annotations.length));
			return clazz;
		} catch (ClassNotFoundException e) {
			debugOutput();
		} finally {
			
		}
		return null;
	}

	private void debugOutput() {
		System.out.println("java system property class path:");
		
		System.out.println(System.getProperty("java.class.path"));
		
		URL[] urLs = classLoader.getURLs();
		DelegationModel delegationModel = AbstractJPPFClassLoader.getDelegationModel();
		ClassLoader scl = ClassLoader.getSystemClassLoader();
		URLClassLoader uscl = ((URLClassLoader)scl);
		URL[] urLs2 = uscl.getURLs();
		
		System.out.println("class loader url lenght = "+urLs.length);
		System.out.println("class loader urls:");
		
		for (URL url : urLs) {
			System.out.println(url.getFile());
		}
		
		System.out.println("Delegation model: "+delegationModel);
		
		System.out.println("system class loader urls:");
		
		for (URL url : urLs2) {
			System.out.println(url.getFile());
		}
		
		
	}

}
