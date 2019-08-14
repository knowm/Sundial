package org.quartz.classloading;

import java.io.InputStream;
import java.net.URL;
import java.util.List;

/**
 * An interface for classes wishing to provide the service of loading classes and resources within
 * the scheduler...
 *
 * @author jhouse
 * @author pl47ypus
 */
public interface ClassLoadHelper {

  /**
   * Called to give the ClassLoadHelper a chance to initialize itself, including the opportunity to
   * "steal" the class loader off of the calling thread, which is the thread that is initializing
   * Quartz.
   */
  void initialize();

  /**
   * Return the class with the given name.
   *
   * @param name of the class to load.
   * @return the requested class.
   * @throws ClassNotFoundException if the class can be found in the classpath.
   */
  Class loadClass(String name) throws ClassNotFoundException;

  /**
   * Finds a resource with a given name. This method returns null if no resource with this name is
   * found.
   *
   * @param name name of the desired resource
   * @return a java.net.URL object
   */
  URL getResource(String name);

  /**
   * Finds all resources with a given name. This method returns empty list if no resource with this
   * name is found.
   *
   * @param name name of the desired resource
   * @return a java.net.URL list
   */
  List<URL> getResources(String name);

  /**
   * Finds a resource with a given name. This method returns null if no resource with this name is
   * found.
   *
   * @param name name of the desired resource
   * @return a java.io.InputStream object
   */
  InputStream getResourceAsStream(String name);

  /**
   * Enable sharing of the class-loader with 3rd party (e.g. digester).
   *
   * @return the class-loader user be the helper.
   */
  ClassLoader getClassLoader();
}
