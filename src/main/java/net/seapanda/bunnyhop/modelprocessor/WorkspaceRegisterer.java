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

package net.seapanda.bunnyhop.modelprocessor;

import java.util.Objects;
import net.seapanda.bunnyhop.model.node.BhNode;
import net.seapanda.bunnyhop.model.node.ConnectiveNode;
import net.seapanda.bunnyhop.model.node.TextNode;
import net.seapanda.bunnyhop.model.workspace.Workspace;
import net.seapanda.bunnyhop.undo.UserOperation;

/**
 * ノードに対してワークスペースをセットする.
 *
 * @author K.Koike
 */
public class WorkspaceRegisterer implements BhModelProcessor {

  /** undo 用コマンドオブジェクト. */
  private final UserOperation userOpe;
  /** {@link BhNode} にセットするワークスペース. */
  private final Workspace ws;
  
  /**
   * {@code node} 以下のノードに {@code ws} を登録する.
   *
   * @param node これ以下のノードにワークスペースを登録する
   * @param ws 登録するワークスペース
   * @param userOpe undo 用コマンドオブジェクト
   */
  public static void register(BhNode node, Workspace ws, UserOperation userOpe) {
    Objects.requireNonNull(node);
    Objects.requireNonNull(ws);
    var registerer = new WorkspaceRegisterer(ws, userOpe);
    node.accept(registerer);
  }

  /**
   * 引数で指定したノード以下のノードのワークスペースの登録を解除する.
   *
   * @param node このノード以下のノードのワークスペースの登録を解除する
   * @param userOpe undo 用コマンドオブジェクト
   * */
  public static void deregister(BhNode node, UserOperation userOpe) {
    var registerer = new WorkspaceRegisterer(null, userOpe);
    node.accept(registerer);
  }

  private WorkspaceRegisterer(Workspace ws, UserOperation userOpe) {
    this.ws = ws;
    this.userOpe = userOpe;
  }

  @Override
  public void visit(ConnectiveNode node) {
    node.setWorkspace(ws, userOpe);
    node.sendToSections(this);
  }

  @Override
  public void visit(TextNode node) {
    node.setWorkspace(ws, userOpe);
  }
}
