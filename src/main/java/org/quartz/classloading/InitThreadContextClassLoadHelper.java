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

import java.io.InputStream;
import java.net.URL;

/**
 * A <code>ClassLoadHelper</code> that uses either the context class loader of the thread that initialized Quartz.
 * 
 * @see org.quartz.classloading.ClassLoadHelper
 * @see org.quartz.classloading.ThreadContextClassLoadHelper
 * @see org.quartz.classloading.SimpleClassLoadHelper
 * @see org.quartz.classloading.CascadingClassLoadHelper
 * @see org.quartz.classloading.LoadingLoaderClassLoadHelper
 * @author jhouse
 * @author pl47ypus
 */
class InitThreadContextClassLoadHelper implements ClassLoadHelper {

  /*
   * ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~ Data members.
   * ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
   */

  private ClassLoader initClassLoader;

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

    initClassLoader = Thread.currentThread().getContextClassLoader();
  }

  /**
   * Return the class with the given name.
   */
  @Override
  public Class loadClass(String name) throws ClassNotFoundException {

    return initClassLoader.loadClass(name);
  }

  /**
   * Finds a resource with a given name. This method returns null if no resource with this name is found.
   * 
   * @param name name of the desired resource
   * @return a java.net.URL object
   */
  @Override
  public URL getResource(String name) {

    return initClassLoader.getResource(name);
  }

  /**
   * Finds a resource with a given name. This method returns null if no resource with this name is found.
   * 
   * @param name name of the desired resource
   * @return a java.io.InputStream object
   */
  @Override
  public InputStream getResourceAsStream(String name) {

    return initClassLoader.getResourceAsStream(name);
  }

  /**
   * Enable sharing of the class-loader with 3rd party.
   * 
   * @return the class-loader user be the helper.
   */
  @Override
  public ClassLoader getClassLoader() {

    return this.initClassLoader;
  }
}
