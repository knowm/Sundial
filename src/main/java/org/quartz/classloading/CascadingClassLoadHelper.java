package org.quartz.classloading;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.lang.reflect.Modifier;
import java.net.URL;
import java.net.URLDecoder;
import java.util.*;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import org.knowm.sundial.Job;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A <code>ClassLoadHelper</code> uses all of the <code>ClassLoadHelper</code> types that are found
 * in this package in its attempts to load a class, when one scheme is found to work, it is promoted
 * to the scheme that will be used first the next time a class is loaded (in order to improve
 * performance).
 *
 * <p>This approach is used because of the wide variance in class loader behavior between the
 * various environments in which Quartz runs (e.g. disparate application servers, stand-alone,
 * mobile devices, etc.). Because of this disparity, Quartz ran into difficulty with a one
 * class-load style fits-all design. Thus, this class loader finds the approach that works, then
 * 'remembers' it.
 *
 * @see org.quartz.classloading.ClassLoadHelper
 * @see org.quartz.classloading.LoadingLoaderClassLoadHelper
 * @see org.quartz.classloading.SimpleClassLoadHelper
 * @see org.quartz.classloading.ThreadContextClassLoadHelper
 * @see org.quartz.classloading.InitThreadContextClassLoadHelper
 * @author jhouse
 * @author pl47ypus
 */
public class CascadingClassLoadHelper implements ClassLoadHelper {

  /*
   * ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~ Data members.
   * ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
   */
  private final Logger logger = LoggerFactory.getLogger(CascadingClassLoadHelper.class);

  private LinkedList<ClassLoadHelper> loadHelpers;

  private ClassLoadHelper bestCandidate;

  /*
   * ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~ Interface.
   * ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
   */

  /**
   * Called to give the ClassLoadHelper a chance to initialize itself, including the opportunity to
   * "steal" the class loader off of the calling thread, which is the thread that is initializing
   * Quartz.
   */
  @Override
  public void initialize() {

    loadHelpers = new LinkedList<ClassLoadHelper>();

    loadHelpers.add(new LoadingLoaderClassLoadHelper());
    loadHelpers.add(new SimpleClassLoadHelper());
    loadHelpers.add(new ThreadContextClassLoadHelper());
    loadHelpers.add(new InitThreadContextClassLoadHelper());

    for (ClassLoadHelper loadHelper : loadHelpers) {
      loadHelper.initialize();
    }
  }

  /** Return the class with the given name. */
  @Override
  public Class loadClass(String name) throws ClassNotFoundException {

    if (bestCandidate != null) {
      try {
        return bestCandidate.loadClass(name);
      } catch (Throwable t) {
        bestCandidate = null;
      }
    }

    Throwable throwable = null;
    Class clazz = null;
    ClassLoadHelper loadHelper = null;

    Iterator<ClassLoadHelper> iter = loadHelpers.iterator();
    while (iter.hasNext()) {
      loadHelper = iter.next();

      try {
        clazz = loadHelper.loadClass(name);
        break;
      } catch (Throwable t) {
        throwable = t;
      }
    }

    if (clazz == null) {
      if (throwable instanceof ClassNotFoundException) {
        throw (ClassNotFoundException) throwable;
      } else {
        throw new ClassNotFoundException(
            String.format("Unable to load class %s by any known loaders.", name), throwable);
      }
    }

    bestCandidate = loadHelper;

    return clazz;
  }

  /**
   * Finds a resource with a given name. This method returns null if no resource with this name is
   * found.
   *
   * @param name name of the desired resource
   * @return a java.net.URL object
   */
  @Override
  public URL getResource(String name) {

    URL result = null;

    if (bestCandidate != null) {
      result = bestCandidate.getResource(name);
      if (result == null) {
        bestCandidate = null;
      }
    }

    ClassLoadHelper loadHelper = null;

    Iterator<ClassLoadHelper> iter = loadHelpers.iterator();
    while (iter.hasNext()) {
      loadHelper = iter.next();

      result = loadHelper.getResource(name);
      if (result != null) {
        break;
      }
    }

    bestCandidate = loadHelper;
    return result;
  }

  /**
   * Finds all resources with a given name. This method returns empty list if no resource with this
   * name is found.
   *
   * @param name name of the desired resource
   * @return a java.net.URL list
   */
  @Override
  public List<URL> getResources(String name) {

    List<URL> result = null;

    if (bestCandidate != null) {
      result = bestCandidate.getResources(name);
      if (result == null || result.isEmpty()) {
        bestCandidate = null;
      }
    }

    ClassLoadHelper loadHelper = null;

    Iterator<ClassLoadHelper> iter = loadHelpers.iterator();
    while (iter.hasNext()) {
      loadHelper = iter.next();

      result = loadHelper.getResources(name);
      if (result != null && !result.isEmpty()) {
        break;
      }
    }

    bestCandidate = loadHelper;
    return result;
  }

  /**
   * Finds a resource with a given name. This method returns null if no resource with this name is
   * found.
   *
   * @param name name of the desired resource
   * @return a java.io.InputStream object
   */
  @Override
  public InputStream getResourceAsStream(String name) {

    InputStream result = null;

    if (bestCandidate != null) {
      result = bestCandidate.getResourceAsStream(name);
      if (result == null) {
        bestCandidate = null;
      }
    }

    ClassLoadHelper loadHelper = null;

    Iterator<ClassLoadHelper> iter = loadHelpers.iterator();
    while (iter.hasNext()) {
      loadHelper = iter.next();

      result = loadHelper.getResourceAsStream(name);
      if (result != null) {
        break;
      }
    }

    bestCandidate = loadHelper;
    return result;
  }

  /**
   * Given a package name(s), search the classpath for all classes that extend sundial.job .
   *
   * <p>A comma(,) or colon(:) can be used to specify multiple packages to scan for Jobs.
   *
   * @param pkgname
   * @return
   */
  public Set<Class<? extends Job>> getJobClasses(String pkgname) {

    Set<Class<? extends Job>> classes = new HashSet<Class<? extends Job>>();

    String[] packages = pkgname.split("[\\:,]");
    if (packages.length > 1) {
      for (String pkg : packages) {
        classes.addAll(getJobClasses(pkg));
      }
    } else {
      String relPath = pkgname.replace('.', '/').replace("%20", " ");

      // Get a File object for the package
      List<URL> resources = getResources(relPath);
      if (resources.isEmpty()) {
        throw new RuntimeException("Unexpected problem: No resource for " + relPath);
      }
      for (URL resource : resources) {
        String resPath = "";
        try {
          resPath = URLDecoder.decode(resource.getPath(), "UTF-8");
        } catch (UnsupportedEncodingException e) {
          e.printStackTrace();
        }

        logger.info("Package: '" + pkgname + "' becomes Resource: '" + resPath + "'");

        if (resource.toString().startsWith("jar:")) {
          processJarfile(resPath, pkgname, classes);
        } else {
          processDirectory(resPath, pkgname, classes);
        }
      }
    }

    return classes;
  }

  private void processDirectory(String path, String pkgname, Set<Class<? extends Job>> classes) {

    File directory = new File(path);
    logger.debug("Reading Directory '" + directory + "'");
    // Get the list of the files contained in the package
    String[] files = directory.list();
    for (int i = 0; i < files.length; i++) {
      String fileName = files[i];
      String className = null;
      // we are only interested in .class files
      if (fileName.endsWith(".class")) {
        // removes the .class extension
        className = pkgname + '.' + fileName.substring(0, fileName.length() - 6);
      }
      logger.debug("FileName '" + fileName + "'  =>  class '" + className + "'");
      if (className != null) {
        filterJobClassWithExceptionCatch(className, classes);
      }
      File subdir = new File(directory, fileName);
      if (subdir.isDirectory()) {
        processDirectory(subdir.getPath(), pkgname + '.' + fileName, classes);
      }
    }
  }

  private void processJarfile(String path, String pkgname, Set<Class<? extends Job>> classes) {

    String relPath = pkgname.replace('.', '/');

    String jarPath = path.replaceFirst("[.]jar[!].*", ".jar").replaceFirst("file:", "");
    logger.debug("Reading JAR file: '" + jarPath + "'");
    JarFile jarFile;
    try {
      jarFile = new JarFile(jarPath);
    } catch (IOException e) {
      throw new RuntimeException("Unexpected IOException reading JAR File '" + jarPath + "'", e);
    }
    Enumeration<JarEntry> entries = jarFile.entries();
    while (entries.hasMoreElements()) {
      JarEntry entry = entries.nextElement();
      String entryName = entry.getName();
      String className = null;
      if (entryName.endsWith(".class")
          && entryName.startsWith(relPath)
          && entryName.length() > (relPath.length() + "/".length())) {
        className = entryName.replace('/', '.').replace('\\', '.').replace(".class", "");
      }
      logger.debug("JarEntry '" + entryName + "'  =>  class '" + className + "'");
      if (className != null) {
        filterJobClassWithExceptionCatch(className, classes);
      }
    }
  }

  private void filterJobClassWithExceptionCatch(
      String className, Set<Class<? extends Job>> classes) {
    try {

      Class clazz = loadClass(className);
      if (Modifier.isAbstract(clazz.getModifiers())) {
        return;
      }
      if (Modifier.isInterface(clazz.getModifiers())) {
        return;
      }
      if (Job.class.isAssignableFrom(clazz)) {
        classes.add(clazz);
      }
    } catch (ClassNotFoundException e) {
      throw new RuntimeException(
          "Unexpected ClassNotFoundException loading class '" + className + "'");
    }
  }

  /**
   * Enable sharing of the "best" class-loader with 3rd party.
   *
   * @return the class-loader user be the helper.
   */
  @Override
  public ClassLoader getClassLoader() {

    return (this.bestCandidate == null)
        ? Thread.currentThread().getContextClassLoader()
        : this.bestCandidate.getClassLoader();
  }
}
