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

package net.seapanda.bunnyhop.workspace.model.factory;

import java.nio.file.Path;
import net.seapanda.bunnyhop.node.view.effect.VisualEffectManager;
import net.seapanda.bunnyhop.nodeselection.view.BhNodeSelectionViewProxy;
import net.seapanda.bunnyhop.service.accesscontrol.TransactionNotificationService;
import net.seapanda.bunnyhop.service.message.MessageService;
import net.seapanda.bunnyhop.ui.view.ViewConstructionException;
import net.seapanda.bunnyhop.utility.math.Vec2D;
import net.seapanda.bunnyhop.workspace.control.WorkspaceController;
import net.seapanda.bunnyhop.workspace.model.Workspace;
import net.seapanda.bunnyhop.workspace.view.FxmlWorkspaceView;
import net.seapanda.bunnyhop.workspace.view.NodeShifterView;
import net.seapanda.bunnyhop.workspace.view.WorkspaceView;

/**
 * {@link Workspace} を作成する機能を提供するクラス.
 *
 * @author K.Koike
 */
public class WorkspaceFactoryImpl implements WorkspaceFactory {
  
  private final Path nodeShifterViewPath;
  private final TransactionNotificationService notifService;
  private final BhNodeSelectionViewProxy nodeSelectionViewProxy;
  private final Path workspaceViewFilePath;
  private final MessageService msgService;
  private final VisualEffectManager effectManager;

  /** コンストラクタ. */
  public WorkspaceFactoryImpl(
      Path workspaceViewFilePath,
      Path nodeShifterViewPath,
      TransactionNotificationService notifService,
      BhNodeSelectionViewProxy proxy,
      MessageService msgService,
      VisualEffectManager visualEffectManager) {
    this.workspaceViewFilePath = workspaceViewFilePath;
    this.nodeShifterViewPath = nodeShifterViewPath;
    this.notifService = notifService;
    this.nodeSelectionViewProxy = proxy;
    this.msgService = msgService;
    this.effectManager = visualEffectManager;
  }

  @Override
  public Workspace create(String name) {
    return new Workspace(name);
  }

  @Override
  public WorkspaceView setMvc(Workspace ws, Vec2D size) throws ViewConstructionException {
    WorkspaceView view = new FxmlWorkspaceView(ws, size, workspaceViewFilePath, notifService);
    new WorkspaceController(
        ws,
        view,
        new NodeShifterView(nodeShifterViewPath),
        notifService,
        nodeSelectionViewProxy,
        msgService,
        effectManager);
    return view;
  }
}
