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

package net.seapanda.bunnyhop.bhprogram;

import java.nio.file.Path;
import java.util.Collection;
import java.util.Optional;
import javafx.scene.control.Alert;
import net.seapanda.bunnyhop.bhprogram.runtime.LocalBhRuntimeController;
import net.seapanda.bunnyhop.common.TextDefs;
import net.seapanda.bunnyhop.compiler.BhCompiler;
import net.seapanda.bunnyhop.compiler.CompileError;
import net.seapanda.bunnyhop.compiler.CompileOption;
import net.seapanda.bunnyhop.model.node.BhNode;
import net.seapanda.bunnyhop.service.LogManager;
import net.seapanda.bunnyhop.service.MessageService;

/**
 * BhProgram の起動とその実行環境の制御用オブジェクトを取得する機能を提供するクラス.
 *
 * @author K.Koike
 */
public class LocalBhProgramLauncherImpl implements LocalBhProgramLauncher {
  
  private final BhCompiler compiler;
  private final LocalBhRuntimeController runtimeCtrl;
  private final MessageService msgService;

  /**
   * コンストラクタ.
   *
   * @param compiler BhProgram の実行ファイルを作成するのに使うコンパイラ
   * @param controller BhProgram の実行環境を操作するオブジェクト
   * @param msgService アプリケーションユーザにメッセージを出力するためのオブジェクト.
   */
  public LocalBhProgramLauncherImpl(
      BhCompiler compiler,
      LocalBhRuntimeController controller,
      MessageService msgService) {
    this.compiler = compiler;
    this.runtimeCtrl = controller;
    this.msgService = msgService;
  }

  @Override
  public synchronized boolean launch(ExecutableNodeSet nodeSet) {
    return compile(nodeSet).map(this::startProgram).orElse(false);
  }

  /**
   * ノードをコンパイルする.
   *
   * @param nodeSet コンパイル対象のノードのリスト
   * @return ノードをコンパイルしてできたソースファイルのパス
   */
  private Optional<Path> compile(ExecutableNodeSet nodeSet) {
    CompileOption option = new CompileOption.Builder().build();
    Collection<BhNode> nodesToCompile = nodeSet.getRootNodeList();
    try {
      Path outFile = compiler.compile(nodeSet.getEntryPoint(), nodesToCompile, option);
      msgService.info(TextDefs.Compile.succeeded.get());
      return Optional.of(outFile);
    } catch (CompileError e) {
      msgService.alert(
          Alert.AlertType.ERROR,
          TextDefs.Compile.InformFailedToWrite.title.get(),
          null,
          e.toString());
      LogManager.logger().error(e.toString());
      return Optional.empty();
    }
  }

  /** プログラムを実行する. */
  private boolean startProgram(Path filePath) {
    return runtimeCtrl.start(filePath);
  }

  @Override
  public LocalBhRuntimeController getBhRuntimeCtrl() {
    return runtimeCtrl;
  }
}
