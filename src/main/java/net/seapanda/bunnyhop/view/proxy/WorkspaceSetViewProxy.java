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

package net.seapanda.bunnyhop.view.proxy;

import net.seapanda.bunnyhop.model.workspace.Workspace;
import net.seapanda.bunnyhop.model.workspace.WorkspaceSet;

/**
 * {@link WorkspaceSet} のビューを操作するプロキシクラスのインタフェース.
 *
 * @author K.Koike
 */
public interface WorkspaceSetViewProxy {
  
  /** {@link WorkspaceSet} のビューに対し, ワークスペースが追加されたことを通知する. */
  public default void notifyWorkspaceAdded(Workspace ws) {}

  /** {@link WorkspaceSet} のビューに対し, ワークスペースが削除されたことを通知する. */
  public default void notifyWorkspaceRemoved(Workspace ws) {}
}
