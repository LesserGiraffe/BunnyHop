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

import net.seapanda.bunnyhop.model.ModelAccessNotificationService;
import net.seapanda.bunnyhop.model.node.BhNode;
import net.seapanda.bunnyhop.view.node.BhNodeView;

/**
 * {@code NoContentNodeView} のコントローラ.
 *
 * @author K.Koike
 */
public class NoContentNodeController implements BhNodeController {

  private final BhNodeController wrapped;

  public NoContentNodeController(BhNodeController controller) {
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
