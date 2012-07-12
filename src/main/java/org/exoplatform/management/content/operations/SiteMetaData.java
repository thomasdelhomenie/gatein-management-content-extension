package org.exoplatform.management.content.operations;

import java.util.HashMap;
import java.util.Map;

public class SiteMetaData {

  public static final String SITE_PATH = "site-path";
  public static final String SITE_WORKSPACE = "site-workspace";
  public static final String SITE_NAME = "site-name";

  Map<String, String> options = new HashMap<String, String>();
  Map<String, String> jcrExportedFiles = new HashMap<String, String>();

  public Map<String, String> getJcrExportedFiles() {
    return this.jcrExportedFiles;
  }

  public Map<String, String> getOptions() {
    return this.options;
  }

  public void setJcrExportedFiles(Map<String, String> jcrExportedFiles) {
    this.jcrExportedFiles = jcrExportedFiles;
  }

  public void setOptions(Map<String, String> options) {
    this.options = options;
  }

}
