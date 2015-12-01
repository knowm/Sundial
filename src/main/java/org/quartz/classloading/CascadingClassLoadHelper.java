/**
 * Copyright 2015 Knowm Inc. (http://knowm.org) and contributors.
 * Copyright 2013-2015 Xeiam LLC (http://xeiam.com) and contributors.
 * Copyright 2001-2011 Terracotta Inc. (http://terracotta.org).
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.quartz.classloading;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Modifier;
import java.net.URL;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.Set;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

import org.knowm.sundial.Job;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A <code>ClassLoadHelper</code> uses all of the <code>ClassLoadHelper</code> types that are found in this package in its attempts to load a class,
 * when one scheme is found to work, it is promoted to the scheme that will be used first the next time a class is loaded (in order to improve
 * performance).
 * <p>
 * This approach is used because of the wide variance in class loader behavior between the various environments in which Quartz runs (e.g. disparate
 * application servers, stand-alone, mobile devices, etc.). Because of this disparity, Quartz ran into difficulty with a one class-load style fits-all
 * design. Thus, this class loader finds the approach that works, then 'remembers' it.
 * </p>
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
   * Called to give the ClassLoadHelper a chance to initialize itself, including the opportunity to "steal" the class loader off of the calling
   * thread, which is the thread that is initializing Quartz.
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

  /**
   * Return the class with the given name.
   */
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
        throw new ClassNotFoundException(String.format("Unable to load class %s by any known loaders.", name), throwable);
      }
    }

    bestCandidate = loadHelper;

    return clazz;
  }

  /**
   * Finds a resource with a given name. This method returns null if no resource with this name is found.
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
   * Finds a resource with a given name. This method returns null if no resource with this name is found.
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
   * Given a package name, search the classpath for all classes that extend sundial.job
   *
   * @param pkgname
   * @return
   */
  public Set<Class<? extends Job>> getJobClasses(String pkgname) {

    Set<Class<? extends Job>> classes = new HashSet<Class<? extends Job>>();

    String relPath = pkgname.replace('.', '/');

    // Get a File object for the package
    URL resource = getResource(relPath);
    if (resource == null) {
      throw new RuntimeException("Unexpected problem: No resource for " + relPath);
    }
    logger.info("Package: '" + pkgname + "' becomes Resource: '" + resource.toString() + "'");

    if (resource.toString().startsWith("jar:")) {
      processJarfile(resource, pkgname, classes);
    } else {
      processDirectory(new File(resource.getPath()), pkgname, classes);
    }

    return classes;

  }

  private void processDirectory(File directory, String pkgname, Set<Class<? extends Job>> classes) {

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
        processDirectory(subdir, pkgname + '.' + fileName, classes);
      }
    }
  }

  private void processJarfile(URL resource, String pkgname, Set<Class<? extends Job>> classes) {

    String relPath = pkgname.replace('.', '/');
    String resPath = resource.getPath().replace("%20", " ");
    String jarPath = resPath.replaceFirst("[.]jar[!].*", ".jar").replaceFirst("file:", "");
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
      if (entryName.endsWith(".class") && entryName.startsWith(relPath) && entryName.length() > (relPath.length() + "/".length())) {
        className = entryName.replace('/', '.').replace('\\', '.').replace(".class", "");
      }
      logger.debug("JarEntry '" + entryName + "'  =>  class '" + className + "'");
      if (className != null) {
        filterJobClassWithExceptionCatch(className, classes);
      }
    }
  }

  private void filterJobClassWithExceptionCatch(String className, Set<Class<? extends Job>> classes) {
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
      throw new RuntimeException("Unexpected ClassNotFoundException loading class '" + className + "'");
    }
  }

  /**
   * Enable sharing of the "best" class-loader with 3rd party.
   *
   * @return the class-loader user be the helper.
   */
  @Override
  public ClassLoader getClassLoader() {

    return (this.bestCandidate == null) ? Thread.currentThread().getContextClassLoader() : this.bestCandidate.getClassLoader();
  }
}
