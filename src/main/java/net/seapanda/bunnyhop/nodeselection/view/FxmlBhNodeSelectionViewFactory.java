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

package net.seapanda.bunnyhop.nodeselection.view;

import java.nio.file.Path;
import net.seapanda.bunnyhop.ui.view.ViewConstructionException;
import net.seapanda.bunnyhop.workspace.model.Workspace;

/**
 * FXML ファイルから {@link BhNodeSelectionView} を作成する機能を提供するクラス.
 *
 * @author K.Koike
 */
public class FxmlBhNodeSelectionViewFactory implements BhNodeSelectionViewFactory {

  private final Path fxmlFilePath;

  /**
   * コンストラクタ.
   *
   * @param fxmlFilePath {@link BhNodeSelectionView} の構造が定義された FXML ファイルのパス.
   */
  public FxmlBhNodeSelectionViewFactory(Path fxmlFilePath) {
    this.fxmlFilePath = fxmlFilePath;
  }

  public BhNodeSelectionView create(String name, String cssClass) throws ViewConstructionException {
    var ws = new Workspace(name);
    return new FxmlBhNodeSelectionView(fxmlFilePath, name, ws, cssClass);
  }
}
