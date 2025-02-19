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

package net.seapanda.bunnyhop.export;

import java.io.File;
import net.seapanda.bunnyhop.model.workspace.WorkspaceSet;

/**
  * ワークスペースセットの情報をプロジェクトとして出力する機能を規定したインタフェース.
  *
  * @author K.KOike
  */
public interface ProjectExporter {

  /**
   * {@code saveFile} に {@code wss} の情報を出力する.
   *
   * @param saveFile このファイルにプロジェクト情報を書き出す
   * @param wss このワークスペースセットの情報を {@code saveFile} に書き出す
   * @return プロジェクトを書き出せた場合 true
   */
  boolean export(File saveFile, WorkspaceSet wss);
}
