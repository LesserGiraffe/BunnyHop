/*
 * Copyright 2017 K.Koike
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
      File file = Paths.get(Util.INSTANCE.execPath, BhConstants.Path.MESSAGE_DIR).toFile();
      URL[] urls = {file.toURI().toURL()};
      ClassLoader loader = new URLClassLoader(urls);
      rb = ResourceBundle.getBundle(
          BhConstants.Path.DEFAULT_MESSAGE_FILE_NAME, Locale.getDefault(), loader);
    } catch (Exception e) {
      e.printStackTrace();
    }
  }

  public String toMessage(Object... args) {
    return String.format(rb.getString(name()), args);
  }
}
