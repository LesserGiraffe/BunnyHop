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

package net.seapanda.bunnyhop.view.factory;

import java.nio.file.Path;
import javafx.scene.control.Button;
import net.seapanda.bunnyhop.control.PrivateTemplateButtonController;
import net.seapanda.bunnyhop.model.ModelAccessNotificationService;
import net.seapanda.bunnyhop.model.node.BhNode;
import net.seapanda.bunnyhop.view.ViewConstructionException;
import net.seapanda.bunnyhop.view.node.component.PrivateTemplateButton;
import net.seapanda.bunnyhop.view.node.style.BhNodeViewStyle;
import net.seapanda.bunnyhop.view.node.style.BhNodeViewStyleFactory;
import net.seapanda.bunnyhop.view.nodeselection.BhNodeSelectionViewProxy;

/**
 * プライベートテンプレートボタンを作成する機能を提供するクラス.
 *
 * @author K.Koike
 */
public class PrivateTemplateButtonFactoryImpl implements PrivateTemplateButtonFactory {
  
  private final Path buttonFilePath;
  private final BhNodeViewStyleFactory viewStyleFactory;
  private final ModelAccessNotificationService service;
  private final BhNodeSelectionViewProxy proxy;

  /** コンストラクタ. */
  public PrivateTemplateButtonFactoryImpl(
      Path buttonFilePath,
      BhNodeViewStyleFactory viewStyleFactory,
      ModelAccessNotificationService service,
      BhNodeSelectionViewProxy proxy) {
    this.buttonFilePath = buttonFilePath;
    this.viewStyleFactory = viewStyleFactory;
    this.service = service;
    this.proxy = proxy;
  }

  @Override
  public Button createButtonOf(BhNode node) throws ViewConstructionException {
    BhNodeViewStyle style = viewStyleFactory.canCreateStyleOf(node.getStyleId())
        ? viewStyleFactory.createStyleOf(node.getStyleId())
        : new BhNodeViewStyle();
    var button = new PrivateTemplateButton(buttonFilePath, style.privatTemplate);
    new PrivateTemplateButtonController(node, button, service, proxy);
    return button;
  }
}
