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

import java.io.Closeable;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.nio.file.StandardOpenOption;

/**
 * ログ出力クラス.
 *
 * @author K.Koike
 */
public class LogManager implements Closeable {

  private OutputStream logOutputStream;
  /** ログファイルを作成するディレクトリのパス. */
  private final Path logDirPath;
  /** ログファイルの共通部分の名前. */
  private final String logFileName;
  /** ログファイルの最大サイズ.  このサイズを超えると新しいログファイルを作成する. */
  private final int maxLogFileSize;
  /** ログファイルの最大個数. この個数を超えるとログローテーションを行う. */
  private final int maxLogFiles;

  /**
   * コンストラクタ.
   *
   * @param logDirPath ログファイルを作成するディレクトリのパス.
   * @param logFileName ログファイルの共通部分の名前.
   * @param maxLogFileSize 最大ログファイルサイズ
   * @param maxLogFiles ログファイルの最大個数. この個数を超えるとログローテーションを行う.
   */
  public LogManager(Path logDirPath, String logFileName, int maxLogFileSize, int maxLogFiles)
      throws IOException {
    this.logDirPath = logDirPath;
    this.logFileName = logFileName;
    this.maxLogFileSize = maxLogFileSize;
    this.maxLogFiles = maxLogFiles;
    initialize();
  }

  private void initialize() throws IOException {
    if (!Files.isDirectory(logDirPath)) {
      Files.createDirectory(logDirPath);
    }
    Path logFilePath = genLogFilePath(0);
    if (!Files.exists(logFilePath)) {
      Files.createFile(logFilePath);
    }
    //ログローテーション
    if (Files.size(logFilePath) > maxLogFileSize) {
      renameLogFiles();
    }
    logOutputStream = Files.newOutputStream(
        logFilePath,
        StandardOpenOption.CREATE, StandardOpenOption.APPEND, StandardOpenOption.WRITE);
  }

  /**
   * ログファイルにメッセージを書き込む.
   *
   * @param msg ログファイルに書き込むメッセージ
   */
  public void writeToFile(String msg) {
    try {
      if (logOutputStream != null) {
        logOutputStream.write(msg.getBytes(StandardCharsets.UTF_8));
        logOutputStream.flush();
      }
    } catch (IOException | SecurityException e) { /* do nothing */ }
  }

  /**
   * ログローテンションのため, ログファイルをリネームする.
   *
   * @return リネームに成功した場合true
   */
  private void renameLogFiles() throws IOException {
    Path oldestLogFilePath = genLogFilePath(maxLogFiles - 1);
    if (Files.exists(oldestLogFilePath)) {
      Files.delete(oldestLogFilePath);
    }
    for (int fileNo = maxLogFiles - 2; fileNo >= 0; --fileNo) {
      Path oldLogFilePath = genLogFilePath(fileNo);
      Path newLogFilePath = genLogFilePath(fileNo + 1);
      if (Files.exists(oldLogFilePath)) {
        Files.move(oldLogFilePath, newLogFilePath, StandardCopyOption.ATOMIC_MOVE);
      }
    }
  }

  private Path genLogFilePath(int fileNo) {
    String numStr = ("0000" + fileNo);
    numStr = numStr.substring(numStr.length() - 4, numStr.length());
    String fileName = "%s%s.log".formatted(logFileName, numStr, ".log");
    return logDirPath.resolve(fileName);
  }

  /** 終了処理をする. */
  @Override
  public void close() {
    try {
      if (logOutputStream != null) {
        logOutputStream.close();
      }
    } catch (IOException e) { /* do nothing */ }
  }
}
