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

import java.io.IOException;
import java.nio.file.Path;
import java.util.SequencedCollection;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.layout.VBox;
import net.seapanda.bunnyhop.bhprogram.debugger.CallStackItem;
import net.seapanda.bunnyhop.control.SearchBox;
import net.seapanda.bunnyhop.control.debugger.CallStackController;
import net.seapanda.bunnyhop.model.ModelAccessNotificationService;
import net.seapanda.bunnyhop.view.ViewConstructionException;

/**
 * デバッガのビューを作成する機能を提供するクラス.
 *
 * @author K.Koike
 */
public class DebugViewFactoryImpl implements DebugViewFactory {
  
  private final Path callStackViewFilePath;
  private final ModelAccessNotificationService service;
  private final SearchBox searchBox;
  
  /** コンストラクタ. */
  public DebugViewFactoryImpl(
      Path callStackViewFilePath,
      ModelAccessNotificationService service,
      SearchBox searchBox) {
    this.callStackViewFilePath = callStackViewFilePath;
    this.service = service;
    this.searchBox = searchBox;
  }

  @Override
  public Node createCallStackView(SequencedCollection<CallStackItem> items)
      throws ViewConstructionException {
    try {
      var root = new VBox();
      var ctrl = new CallStackController(items, service, searchBox);
      FXMLLoader loader = new FXMLLoader(callStackViewFilePath.toUri().toURL());
      loader.setRoot(root);
      loader.setController(ctrl);
      loader.load();
      return root;
    } catch (IOException e) {
      throw new ViewConstructionException(String.format(
          "Failed to initialize call stack view (%s).\n%s",
          callStackViewFilePath.toAbsolutePath(),
          e));
    }
  }
}
