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

package net.seapanda.bunnyhop.view.workspace;

import javafx.scene.layout.Pane;

/**
 * ワークスペースビュー内の描画物を保持するビュー.
 *
 * @author K.Koike
 */
public class WorkspaceViewPane extends Pane {

  /** このペインを保持しているワークスペースビュー. */
  private WorkspaceView container;

  /**
   * このビューが所属するワークスペースビューを設定する.
   */
  public void setContainer(WorkspaceView container) {
    this.container = container;
  }

  /**
   * このビューが所属するワークスペースビューを取得する.
   */
  public WorkspaceView getContainer() {
    return container;
  }
}
