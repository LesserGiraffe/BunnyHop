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

package net.seapanda.bunnyhop.debugger.view.factory;

import java.io.IOException;
import java.nio.file.Path;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.fxml.FXMLLoader;
import javafx.scene.layout.VBox;
import net.seapanda.bunnyhop.common.text.TextDefs;
import net.seapanda.bunnyhop.debugger.control.CallStackController;
import net.seapanda.bunnyhop.debugger.control.VariableInspectionController;
import net.seapanda.bunnyhop.debugger.model.Debugger;
import net.seapanda.bunnyhop.debugger.model.thread.ThreadContext;
import net.seapanda.bunnyhop.debugger.model.variable.VariableInfo;
import net.seapanda.bunnyhop.node.view.effect.VisualEffectManager;
import net.seapanda.bunnyhop.ui.control.SearchBox;
import net.seapanda.bunnyhop.ui.view.ViewConstructionException;
import net.seapanda.bunnyhop.workspace.model.WorkspaceSet;

/**
 * デバッガのビューを作成する機能を提供するクラス.
 *
 * @author K.Koike
 */
public class DebugViewFactoryImpl implements DebugViewFactory {
  
  private final Path callStackViewFilePath;
  private final Path varInspectionViewFilePath;
  private final SearchBox searchBox;
  private final Debugger debugger;
  private final WorkspaceSet wss;
  private final VisualEffectManager effectManager;
  private final BooleanProperty callStackCtrlJumpFlag = new SimpleBooleanProperty(true);
  private final BooleanProperty localVarInspCtrlJumpFlag = new SimpleBooleanProperty(false);
  private final BooleanProperty globalVarInspCtrlJumpFlag = new SimpleBooleanProperty(false);

  /** コンストラクタ. */
  public DebugViewFactoryImpl(
      Path callStackViewFilePath,
      Path varInspectionViewFilePath,
      SearchBox searchBox,
      Debugger debugger,
      WorkspaceSet wss,
      VisualEffectManager visualEffectManager) {
    this.callStackViewFilePath = callStackViewFilePath;
    this.varInspectionViewFilePath = varInspectionViewFilePath;
    this.searchBox = searchBox;
    this.debugger = debugger;
    this.wss = wss;
    this.effectManager = visualEffectManager;
  }

  @Override
  public CallStackController createCallStackView(ThreadContext context)
      throws ViewConstructionException {
    try {
      var root = new VBox();
      var ctrl = new CallStackController(
          context, searchBox, debugger, wss, callStackCtrlJumpFlag, effectManager);
      FXMLLoader loader = new FXMLLoader(callStackViewFilePath.toUri().toURL());
      loader.setRoot(root);
      loader.setController(ctrl);
      loader.load();
      return ctrl;
    } catch (IOException e) {
      throw new ViewConstructionException(String.format(
          "Failed to initialize call stack view (%s).\n%s",
          callStackViewFilePath.toAbsolutePath(),
          e));
    }
  }

  @Override
  public VariableInspectionController createVariableInspectionView(
      VariableInfo varInfo, boolean isLocal)
      throws ViewConstructionException {
    try {
      String viewName = isLocal
          ? TextDefs.Debugger.VarInspection.localVars.get()
          : TextDefs.Debugger.VarInspection.globalVars.get();
      BooleanProperty jumpFlag = isLocal ? localVarInspCtrlJumpFlag : globalVarInspCtrlJumpFlag;
      var root = new VBox();
      var ctrl = new VariableInspectionController(
          varInfo, viewName, searchBox, debugger, wss, effectManager, jumpFlag);
      FXMLLoader loader = new FXMLLoader(varInspectionViewFilePath.toUri().toURL());
      loader.setRoot(root);
      loader.setController(ctrl);
      loader.load();
      return ctrl;
    } catch (IOException e) {
      throw new ViewConstructionException(String.format(
          "Failed to initialize call stack view (%s).\n%s",
          varInspectionViewFilePath.toAbsolutePath(),
          e));
    }
  }
}
