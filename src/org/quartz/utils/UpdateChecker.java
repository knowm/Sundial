package org.quartz.utils;
/**
 * Copyright 2003-2010 Terracotta, Inc.
 */

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.quartz.core.QuartzScheduler;

import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.net.InetAddress;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLEncoder;
import java.util.Properties;
import java.util.TimerTask;

/**
 * Check for updates and alert users if an update is available
 * 
 * @author Hung Huynh
 */
public class UpdateChecker extends TimerTask {
  private static final Logger    LOG               = LoggerFactory.getLogger(UpdateChecker.class);
  private static final long   MILLIS_PER_SECOND = 1000L;
  private static final String UNKNOWN           = "UNKNOWN";
  private static final String UPDATE_CHECK_URL  = "http://www.terracotta.org/kit/reflector?kitID=quartz&pageID=update.properties";
  private static final long   START_TIME        = System.currentTimeMillis();
  private static final String PRODUCT_NAME      = "Quartz";

  /**
   * Run the update check
   */
  @Override
  public void run() {
    checkForUpdate();
  }

  /**
   * This method ensures that there will be no exception thrown.
   */
  public void checkForUpdate() {
    try {
      if (!Boolean.getBoolean("org.terracotta.quartz.skipUpdateCheck")) {
        doCheck();
      }
    } catch (Throwable t) {
      LOG.debug("Quartz version update check failed: " + t.toString());
    }
  }

  private void doCheck() throws IOException {
    LOG.debug("Checking for available updated version of Quartz...");
    URL updateUrl = buildUpdateCheckUrl();
    Properties updateProps = getUpdateProperties(updateUrl);
    String currentVersion = getQuartzVersion();
    String propVal = updateProps.getProperty("general.notice");
    if (notBlank(propVal)) {
      LOG.info(propVal);
    }
    propVal = updateProps.getProperty(currentVersion + ".notice");
    if (notBlank(propVal)) {
      LOG.info(propVal);
    }
    propVal = updateProps.getProperty(currentVersion + ".updates");
    if (notBlank(propVal)) {
      StringBuilder sb = new StringBuilder();
      String[] newVersions = propVal.split(",");
      for (int i = 0; i < newVersions.length; i++) {
        String newVersion = newVersions[i].trim();
        if (i > 0) {
          sb.append(", ");
        }
        sb.append(newVersion);
        propVal = updateProps.getProperty(newVersion + ".release-notes");
        if (notBlank(propVal)) {
          sb.append(" [");
          sb.append(propVal);
          sb.append("]");
        }
      }
      if (sb.length() > 0) {
        LOG.info("New Quartz update(s) found: " + sb.toString());
      }
    } else {
      // Do nothing at all (ever) on no updates found (DEV-3799)
    }
  }

  private String getQuartzVersion() {
    return String.format("%s.%s.%s", QuartzScheduler.getVersionMajor(), QuartzScheduler.getVersionMinor(),
                         QuartzScheduler.getVersionIteration());
  }

  private Properties getUpdateProperties(URL updateUrl) throws IOException {
    URLConnection connection = updateUrl.openConnection();
    connection.setConnectTimeout(3 * 1000);
    InputStream in = connection.getInputStream();
    try {
      Properties props = new Properties();
      props.load(connection.getInputStream());
      return props;
    } finally {
      if (in != null) {
        in.close();
      }
    }
  }

  private URL buildUpdateCheckUrl() throws MalformedURLException, UnsupportedEncodingException {
    String url = System.getProperty("quartz.update-check.url", UPDATE_CHECK_URL);
    String connector = url.indexOf('?') > 0 ? "&" : "?";
    return new URL(url + connector + buildParamsString());
  }

  private String buildParamsString() throws UnsupportedEncodingException {
    StringBuilder sb = new StringBuilder();
    sb.append("id=");
    sb.append(getClientId());
    sb.append("&os-name=");
    sb.append(urlEncode(getProperty("os.name")));
    sb.append("&jvm-name=");
    sb.append(urlEncode(getProperty("java.vm.name")));
    sb.append("&jvm-version=");
    sb.append(urlEncode(getProperty("java.version")));
    sb.append("&platform=");
    sb.append(urlEncode(getProperty("os.arch")));
    sb.append("&tc-version=");
    sb.append(urlEncode(getQuartzVersion()));
    sb.append("&tc-product=");
    sb.append(urlEncode(PRODUCT_NAME));
    sb.append("&source=");
    sb.append(urlEncode(PRODUCT_NAME));
    sb.append("&uptime-secs=");
    sb.append(getUptimeInSeconds());
    sb.append("&patch=");
    sb.append(urlEncode(UNKNOWN));
    return sb.toString();
  }

  private long getUptimeInSeconds() {
    long uptime = System.currentTimeMillis() - START_TIME;
    return uptime > 0 ? (uptime / MILLIS_PER_SECOND) : 0;
  }

  private int getClientId() {
    try {
      return InetAddress.getLocalHost().hashCode();
    } catch (Throwable t) {
      return 0;
    }
  }

  private String urlEncode(String param) throws UnsupportedEncodingException {
    return URLEncoder.encode(param, "UTF-8");
  }

  private String getProperty(String prop) {
    return System.getProperty(prop, UNKNOWN);
  }

  private boolean notBlank(String s) {
    return s != null && s.trim().length() > 0;
  }
  
  public static void main(String[] args) {
    new UpdateChecker().run();
  }
}

