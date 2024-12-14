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

package net.seapanda.bunnyhop.service;


import java.nio.file.Paths;
import net.seapanda.bunnyhop.command.CmdProxy;
import net.seapanda.bunnyhop.common.BhConstants.Path;
import net.seapanda.bunnyhop.common.BhSettings;
import net.seapanda.bunnyhop.control.workspace.TrashboxController;
import net.seapanda.bunnyhop.model.factory.BhNodeFactory;
import net.seapanda.bunnyhop.model.workspace.Workspace;
import net.seapanda.bunnyhop.model.workspace.WorkspaceSet;
import net.seapanda.bunnyhop.undo.UndoRedoAgent;
import net.seapanda.bunnyhop.utility.TextDatabase;
import net.seapanda.bunnyhop.utility.Utility;
import net.seapanda.bunnyhop.view.nodeselection.BhNodeSelectionService;

/**
 * アプリケーション全体で使用するクラスのオブジェクトをまとめて保持する.
 *
 * @author K.Koike
 */
public class BhService {

  private static volatile WorkspaceSet workspaceSet;
  private static volatile MsgPrinter msgPrinter;
  private static volatile CmdProxy cmdProxy;
  private static volatile TextDatabase textDatabase;
  private static volatile BhScriptManager bhScriptManager;
  private static volatile BhNodeFactory bhNodeFactory;
  private static volatile BhNodePlacer bhNodePlacer;
  private static volatile FileCollector fxmlCollector;
  private static volatile CompileErrorNodeManager compileErrNodeManager;
  private static volatile DerivativeCache derivativeCache;
  private static volatile UndoRedoAgent undoRedoAgent;
  private static volatile BhNodeSelectionService bhNodeSelectionService;
  private static volatile TrashboxController trashboxCtrl;

  /** 保持している全てのオブジェクトの初期化処理を行う. */
  public static boolean initialize(WorkspaceSet wss) {
    try {
      workspaceSet = wss;
      msgPrinter = new MsgPrinter();
      cmdProxy = new CmdProxy(wss);
      textDatabase = new TextDatabase(
          Paths.get(Utility.execPath, Path.LANGUAGE_DIR, BhSettings.language, Path.LANGUAGE_FILE));
      bhScriptManager = new BhScriptManager(
          Paths.get(Utility.execPath, Path.BH_DEF_DIR, Path.FUNCTIONS_DIR),
          Paths.get(Utility.execPath, Path.BH_DEF_DIR, Path.TEMPLATE_LIST_DIR),
          Paths.get(Utility.execPath, Path.REMOTE_DIR)
      );
      bhNodeFactory = new BhNodeFactory(
          Paths.get(Utility.execPath, Path.BH_DEF_DIR, Path.NODE_DEF_DIR),
          Paths.get(Utility.execPath, Path.BH_DEF_DIR, Path.CONNECTOR_DEF_DIR),
          bhScriptManager);
      bhNodePlacer = new BhNodePlacer();
      fxmlCollector = new FileCollector(
          Paths.get(Utility.execPath, Path.VIEW_DIR, Path.FXML_DIR), "fxml");
      compileErrNodeManager = new CompileErrorNodeManager();
      derivativeCache = new DerivativeCache();
      undoRedoAgent = new UndoRedoAgent();
      bhNodeSelectionService = new BhNodeSelectionService();
    } catch (Exception e) {
      if (msgPrinter != null) {
        msgPrinter.errForDebug(e.toString());
      }
      return false;
    }
    return true;
  }

  public static BhNodePlacer bhNodePlacer() {
    return bhNodePlacer;
  
  }

  public static BhScriptManager bhScriptManager() {
    return bhScriptManager;
  }

  public static CompileErrorNodeManager compileErrNodeManager() {
    return compileErrNodeManager;
  }

  public static MsgPrinter msgPrinter() {
    return msgPrinter;
  }

  public static DerivativeCache derivativeCache() {
    return derivativeCache;
  }
  
  public static FileCollector fxmlCollector() {
    return fxmlCollector;
  }

  public static UndoRedoAgent undoRedoAgent() {
    return undoRedoAgent;
  }

  public static CmdProxy cmdProxy() {
    return cmdProxy;
  }

  public static BhNodeFactory bhNodeFactory() {
    return bhNodeFactory;
  }

  public static BhNodeSelectionService bhNodeSelectionService() {
    return bhNodeSelectionService;
  }

  public static TrashboxController trashboxCtrl() {
    return trashboxCtrl;
  }

  public static TextDatabase textDb() {
    return textDatabase;
  }

  /**
   * 現在, 操作対象となっているワークスペースを取得する.
   *
   * @return 現在操作対象となっているワークスペース. 存在しない場合は null.
   */
  public static Workspace getCurrentWorkspace() {
    return workspaceSet.getCurrentWorkspace();
  }

  public static void setTrashboxCtrl(TrashboxController ctrl) {
    trashboxCtrl = ctrl;
  }
}
