package net.seapanda.bunnyhop.common.constant;

import java.io.File;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.Paths;
import java.util.Locale;
import java.util.ResourceBundle;

import net.seapanda.bunnyhop.common.tools.Util;

public enum Message {

  msg0,
  ;

  private static ResourceBundle rb;
  static {
    try {
      File file = Paths.get(Util.INSTANCE.EXEC_PATH, BhParams.Path.MESSAGE_DIR).toFile();
        URL[] urls = {file.toURI().toURL()};
        ClassLoader loader = new URLClassLoader(urls);
        rb = ResourceBundle.getBundle(BhParams.Path.DEFAULT_MESSAGE_FILE_NAME , Locale.getDefault(), loader);
      }
    catch (Exception e) {
      e.printStackTrace();
    }
  }

  public String toMessage(Object... args) {
    return String.format(rb.getString(name()), args);
  }
}
