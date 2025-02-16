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

package net.seapanda.bunnyhop.control.node;

import net.seapanda.bunnyhop.model.node.ConnectiveNode;
import net.seapanda.bunnyhop.view.node.ConnectiveNodeView;

/**
 * {@link ConnectiveNodeView} のコントローラ. のコントローラ.
 *
 * @author K.Koike
 */
public class ConnectiveNodeController extends BhNodeController {

  /**
   * コンストラクタ.
   *
   * @param model 管理するモデル
   * @param view 管理するビュー
   */
  public ConnectiveNodeController(ConnectiveNode model, ConnectiveNodeView view) {
    super(model, view);
    model.setViewProxy(new BhNodeViewProxyImpl(view, false));
  }
}
