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
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import net.seapanda.bunnyhop.common.constant.BhConstants;

/**
 * FXMLファイルとそのパスを保存するクラス.
 *
 * @author K.Koike
 */
public class FxmlCollector {

  public static final FxmlCollector INSTANCE = new FxmlCollector();
  private static final Map<String, Path> fileNameToFilePath = new HashMap<>();

  private FxmlCollector() {}

  /**
   * FXMLファイルのファイル名とそのパスを集める.
   *
   * @return FXMLファイルのフォルダが見つからなかった場合 falseを返す
   */
  public boolean collectFxmlFiles() {
    Path dirPath =
        Paths.get(Util.INSTANCE.execPath, BhConstants.Path.VIEW_DIR, BhConstants.Path.FXML_DIR);
    List<Path> paths;  //読み込むファイルパスリスト
    try {
      paths = Files.walk(dirPath, FOLLOW_LINKS)
          .filter(path -> path.getFileName().toString().endsWith(".fxml")) //.fxmlファイルだけ収集
          .toList();
    } catch (IOException e) {
      MsgPrinter.INSTANCE.errMsgForDebug("Directory not found.  (%s)".formatted(dirPath));
      return false;
    }
    paths.forEach(filePath -> fileNameToFilePath.put(filePath.getFileName().toString(), filePath));
    return true;
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
