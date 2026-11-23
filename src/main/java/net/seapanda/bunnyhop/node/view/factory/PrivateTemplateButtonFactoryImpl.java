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

package net.seapanda.bunnyhop.node.view.factory;

import java.nio.file.Path;
import javafx.scene.control.Button;
import net.seapanda.bunnyhop.node.control.PrivateTemplateButtonController;
import net.seapanda.bunnyhop.node.model.BhNode;
import net.seapanda.bunnyhop.node.view.component.PrivateTemplateButton;
import net.seapanda.bunnyhop.node.view.style.BhNodeViewStyle;
import net.seapanda.bunnyhop.node.view.style.BhNodeViewStyleFactory;
import net.seapanda.bunnyhop.nodeselection.control.BhNodeSelectionViewManager;
import net.seapanda.bunnyhop.nodeselection.view.BhNodeSelectionViewFactory;
import net.seapanda.bunnyhop.service.accesscontrol.TransactionManager;
import net.seapanda.bunnyhop.ui.view.ViewConstructionException;

/**
 * プライベートテンプレートボタンを作成する機能を提供するクラス.
 *
 * @author K.Koike
 */
public class PrivateTemplateButtonFactoryImpl implements PrivateTemplateButtonFactory {
  
  private final Path buttonFilePath;
  private final BhNodeViewStyleFactory viewStyleFactory;
  private final BhNodeSelectionViewFactory nodeSelViewFactory;
  private final TransactionManager txnManager;
  private final BhNodeSelectionViewManager nodeSelViewManager;

  /** コンストラクタ. */
  public PrivateTemplateButtonFactoryImpl(
      Path buttonFilePath,
      BhNodeViewStyleFactory viewStyleFactory,
      BhNodeSelectionViewFactory nodeSelViewFactory,
      TransactionManager txnManager,
      BhNodeSelectionViewManager nodeSelViewManager) {
    this.buttonFilePath = buttonFilePath;
    this.viewStyleFactory = viewStyleFactory;
    this.nodeSelViewFactory = nodeSelViewFactory;
    this.txnManager = txnManager;
    this.nodeSelViewManager = nodeSelViewManager;
  }

  @Override
  public Button create(BhNode node) throws ViewConstructionException {
    BhNodeViewStyle style = viewStyleFactory.canCreateStyleOf(node.getStyleId())
        ? viewStyleFactory.createStyleOf(node.getStyleId())
        : new BhNodeViewStyle();
    var button = new PrivateTemplateButton(buttonFilePath, style.commonPart.privateTemplate);
    new PrivateTemplateButtonController(
        node, button, nodeSelViewFactory, txnManager, nodeSelViewManager);
    return button;
  }
}
