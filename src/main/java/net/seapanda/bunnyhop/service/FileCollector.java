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

package net.seapanda.bunnyhop.service;

import static java.nio.file.FileVisitOption.FOLLOW_LINKS;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;

/**
 * ファイル名とそのパスを保持するクラス.
 *
 * @author K.Koike
 */
public class FileCollector {

  private final Map<String, Path> fileNameToFilePath = new HashMap<>();

  /**
   * {@code dirPath} から {@code extension} で指定した拡張子のファイルを集めて保持する.
   *
   * @param dirPath このディレクトリ以下からファイルを探す
   * @param extension 集めるファイルの拡張子 (例: txt, xml, .xml)
   * @throws IOException ディレクトリの走査にしっぱ視した場合
   */
  public FileCollector(Path dirPath, String extension) throws IOException {
    // 読み込むファイルパスリスト
    if (!extension.startsWith(".")) {
      extension = "." + extension;
    }
    String ext = extension.startsWith(".") ? extension : "." + extension;
    Files.walk(dirPath, FOLLOW_LINKS)
        .filter(path -> path.getFileName().toString().endsWith(ext))
        .forEach(filePath -> fileNameToFilePath.put(filePath.getFileName().toString(), filePath));
  }

  /**
   * FXMLファイル名からそのファイルのフルパスを取得する.
   *
   * @param fileName フルパスを知りたいFXMLファイル名
   * @return fileName で指定したファイルのパスオブジェクト. パスが見つからない場合はnullを返す
   */
  public Path getFilePath(String fileName) {
    return fileNameToFilePath.getOrDefault(fileName, null);
  }
}
