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

package net.seapanda.bunnyhop.model.factory;

import net.seapanda.bunnyhop.model.workspace.Workspace;
import net.seapanda.bunnyhop.utility.math.Vec2D;
import net.seapanda.bunnyhop.view.ViewConstructionException;
import net.seapanda.bunnyhop.view.workspace.WorkspaceView;

/**
 * {@link Workspace} を作成するメソッドを規定したインタフェース.
 *
 * @author K.Koike
 */
public interface WorkspaceFactory {

  /**
   * ワークスペースを作成する.
   *
   * @param name ワークスペース名
   * @return 作成したワークスペース
   */
  Workspace create(String name);

  /**
   * {@code node} 以下のノードに対し MVC 構造を作成する.
   *
   * @param ws このワークスペースの MVC 構造を作成する.
   * @param size ワークスペースのサイズ
   * @return 引数で指定したワークスペースに対応する {@link WorkspaceView}.
   * @throws ViewConstructionException ワークスペースビューの作成に失敗した場合
   */
  WorkspaceView setMvc(Workspace ws, Vec2D size) throws ViewConstructionException;
}
