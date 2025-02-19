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
import net.seapanda.bunnyhop.undo.UserOperation;

/**
 * プロジェクトをワークスペースセットに追加する機能を規定したインタフェース.
 *
 * @author K.KOike
 */
public interface ProjectImporter {

  /**
   * {@code saveFile} からプロジェクトを読みだして, {@code wss} に追加する.
   *
   * @param saveFile このファイルからプロジェクト情報を読みだす
   * @param wss このワークスペースセットに読みだしたプロジェクトを追加する
   * @param useOpe undo 用コマンドオブジェクト
   * @return プロジェクトをワークスペースセットに追加できた場合 true
   */
  boolean imports(File saveFile, WorkspaceSet wss, UserOperation useOpe);
}
