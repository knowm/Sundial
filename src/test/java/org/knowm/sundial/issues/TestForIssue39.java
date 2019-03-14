package org.knowm.sundial.issues;

import java.io.UnsupportedEncodingException;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLDecoder;

public class TestForIssue39 {
  public static void main(String[] args) throws MalformedURLException {
    String testString =
        "jar:file:/usr/local/tomcat/eDHTReporter/webapps/eDHTReporter%20%23%2320180201-01/WEB-INF/lib/eDHRServerScheduling.jar!/de/guhsoft/edhtreporter/server/tools/jobs/";
    URL url = new URL(testString);
    System.out.println("url.getPath() = " + url.getPath());

    String resPath = "";
    //    String resPath = resource.toURI().replace("%20", " ");
    try {
      resPath = URLDecoder.decode(url.getPath(), "UTF-8");
    } catch (UnsupportedEncodingException e) {
      e.printStackTrace();
    }
    System.out.println("resPath = " + resPath);
  }
}
