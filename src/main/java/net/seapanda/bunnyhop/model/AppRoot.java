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

package net.seapanda.bunnyhop.model;

import net.seapanda.bunnyhop.model.nodeselection.BhNodeCategoryList;
import net.seapanda.bunnyhop.model.workspace.WorkspaceSet;
import net.seapanda.bunnyhop.view.proxy.BhNodeSelectionViewProxy;

/**
 * アプリケーションを構成するモデルのルートとなるモデル.
 *
 * @author K.Koike
 */
public class AppRoot {
  
  private final WorkspaceSet wss;
  private final BhNodeCategoryList bhNodeCategoryList;
  private final BhNodeSelectionViewProxy bhNodeSelectionViewProxy;
  
  /** コンストラクタ. */
  public AppRoot(
      WorkspaceSet wss,
      BhNodeCategoryList bhNodeCategoryList,
      BhNodeSelectionViewProxy bhNodeSelectionViewProxy) {
    this.wss = wss;
    this.bhNodeCategoryList = bhNodeCategoryList;
    this.bhNodeSelectionViewProxy = bhNodeSelectionViewProxy;
  }

  public WorkspaceSet getWorkspaceSet() {
    return wss;
  }

  public BhNodeCategoryList getBhNodeCategoryList() {
    return bhNodeCategoryList;
  }

  public BhNodeSelectionViewProxy getNodeSelectionViewProxy() {
    return bhNodeSelectionViewProxy;
  }
}
