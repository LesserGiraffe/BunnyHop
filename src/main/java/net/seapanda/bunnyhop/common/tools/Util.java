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

package net.seapanda.bunnyhop.common.tools;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.concurrent.atomic.AtomicLong;

/**
 * ユーティリティクラス.
 *
 * @author K.Koike
 */
public class Util {

  public static final Util INSTANCE = new Util();
  /** 実行時jarパス. */
  public final String execPath;
  public final String javaPath;
  public final String ps = System.getProperty("path.separator");
  public final String fs = System.getProperty("file.separator");
  private final AtomicLong serialId = new AtomicLong();
  private final String osName = System.getProperty("os.name").toLowerCase();
  public final Platform platform = this.new Platform();

  private Util() {
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

  /**
   * ワイルドカード比較機能つき文字列一致検査.
   *
   * @param whole 比較対象の文字列. wildcard指定不可.
   * @param part 比較対象の文字列. wildcard指定可.
   * @return partにwildcard がある場合, wholeがpartを含んでいればtrue.
   *         partにwildcard が無い場合, wholeとpartが一致すればtrue.
   */
  public boolean equals(String whole, String part) {
    if (whole == null || part == null) {
      return false;
    }
    if (!part.contains("*")) {
      return whole.equals(part);
    }
    return whole.contains(part.substring(0, part.indexOf('*')));
  }

  /**
   * シリアルIDを取得する.
   *
   * @return シリアルID
   */
  public String genSerialId() {
    return Long.toHexString(serialId.getAndIncrement()) + "";
  }

  /**
   * 引数で指定したパスのファイルが存在しない場合作成する.
   *
   * @param filePath 作成するファイルのパス
   * @return 作成に失敗した場合false. 作成しなかった場合はtrue
   */
  public boolean createFileIfNotExists(Path filePath) {
    try {
      if (!Files.exists(filePath)) {
        Files.createFile(filePath);
      }
    } catch (IOException e) {
      MsgPrinter.INSTANCE.msgForDebug("create file err " + filePath + "\n" + e);
      return false;
    }
    return true;
  }

  /**
   * 引数で指定したパスのディレクトリが存在しない場合作成する.
   *
   * @param dirPath 作成するファイルのパス
   * @return 作成に失敗した場合false. 作成しなかった場合はtrue
   */
  public boolean createDirectoryIfNotExists(Path dirPath) {
    try {
      if (!Files.isDirectory(dirPath)) {
        Files.createDirectory(dirPath);
      }
    } catch (IOException e) {
      MsgPrinter.INSTANCE.msgForDebug("create dir err " + dirPath + "\n" + e);
      return false;
    }
    return true;
  }

  /** 高速平方根計算. */
  public double fastSqrt(double x) {
    double half = 0.5 * x;
    long lnum = 0x5FE6EB50C7B537AAL - (Double.doubleToLongBits(x) >> 1);
    double dnum = Double.longBitsToDouble(lnum);
    dnum *= 1.5 - half * dnum * dnum;
    return dnum * x;
  }

  /** このメソッドを呼び出したメソッド名を (クラス名.メソッド名) として返す. */
  public String getCurrentMethodName() {
    StackTraceElement[] elems = Thread.currentThread().getStackTrace();
    if (elems.length >= 3) {
      return elems[2].getClassName() + "." + elems[2].getMethodName();
    }
    return "";
  }

  /** OS を識別するためのクラス. */
  public class Platform {
    public boolean isWindows() {
      return osName.startsWith("windows");
    }

    public boolean isLinux() {
      return osName.startsWith("linux");
    }
  }
}
