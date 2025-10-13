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

package net.seapanda.bunnyhop.node.control;

import net.seapanda.bunnyhop.node.model.BhNode;
import net.seapanda.bunnyhop.node.view.BhNodeView;
import net.seapanda.bunnyhop.node.view.ConnectiveNodeView;
import net.seapanda.bunnyhop.service.accesscontrol.ModelAccessNotificationService;

/**
 * {@link ConnectiveNodeView} のコントローラ.
 *
 * @author K.Koike
 */
public class ConnectiveNodeController implements BhNodeController {

  private final BhNodeController wrapped;

  /** コンストラクタ. */
  public ConnectiveNodeController(BhNodeController controller) {
    wrapped = controller;
  }

  @Override
  public BhNode getModel() {
    return wrapped.getModel();
  }

  @Override
  public BhNodeView getView() {
    return wrapped.getView();
  }

  @Override
  public ModelAccessNotificationService getNotificationService() {
    return wrapped.getNotificationService();
  }  
}
