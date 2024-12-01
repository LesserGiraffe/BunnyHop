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

package net.seapanda.bunnyhop.utility;

import java.io.File;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * アプリケーション全体で使用する様々な処理をまとめたクラス.
 *
 * @author K.Koike
 */
public class Utility {

  /** 実行時jarパス. */
  public static final String execPath;
  public static final String javaPath;
  public static final String ps = System.getProperty("path.separator");
  public static final String fs = System.getProperty("file.separator");
  private static final String osName = System.getProperty("os.name").toLowerCase();

  static {
    boolean isModulePath = true;
    String pathStr = System.getProperty("jdk.module.path");
    if (pathStr == null) {
      isModulePath = false;
      pathStr = System.getProperty("java.class.path");
    }

    String[] paths = pathStr.split(ps);
    pathStr = paths[paths.length - 1];
    File jarFile = new File(pathStr);
    Path jarPath = Paths.get(jarFile.getAbsolutePath());
    String root = (jarPath.getRoot() == null) ? "" : jarPath.getRoot().toString();
    if (isModulePath) {
      execPath = root + jarPath.subpath(0, jarPath.getNameCount()).toString();
    } else {
      execPath = root + jarPath.subpath(0, jarPath.getNameCount() - 2).toString();
    }
    javaPath = System.getProperty("java.home") + fs + "bin" + fs + "java";
  }

  /** このメソッドを呼び出したメソッド名を (クラス名.メソッド名) として返す. */
  public static String getCurrentMethodName() {
    return getMethodName(2);
  }

  /**
   * コールスタックからメソッド名を (クラス名.メソッド名) として返す.
   *
   * @param callee <pre>コールスタックの要素の指定に使用する.
   *               0 : このメソッド
   *               1 : このメソッドを呼び出したメソッド
   *                    ....
   *               n : n - 1 で取得できるメソッドを呼び出したメソッド.
   *               </pre>
   * @retrun {@code callee} で指定したメソッド名.  コールスタックから情報を取得できなかった場合は空の文字列.
   */
  public static String getMethodName(int callee) {
    StackTraceElement[] elems = Thread.currentThread().getStackTrace();
    if (elems.length >= 2 + callee) {
      return elems[1 + callee].getClassName() + "." + elems[1 + callee].getMethodName();
    }
    return "";
  }

  /** OS を識別するためのクラス. */
  public static class Platform {
    public boolean isWindows() {
      return osName.startsWith("windows");
    }

    public boolean isLinux() {
      return osName.startsWith("linux");
    }
  }
}
