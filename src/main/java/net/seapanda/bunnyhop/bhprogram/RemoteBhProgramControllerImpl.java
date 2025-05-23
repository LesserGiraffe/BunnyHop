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
import net.seapanda.bunnyhop.bhprogram.common.message.BhProgramNotification;
import net.seapanda.bunnyhop.bhprogram.runtime.BhRuntimeStatus;
import net.seapanda.bunnyhop.bhprogram.runtime.RemoteBhRuntimeController;
import net.seapanda.bunnyhop.common.TextDefs;
import net.seapanda.bunnyhop.compiler.BhCompiler;
import net.seapanda.bunnyhop.compiler.CompileError;
import net.seapanda.bunnyhop.compiler.CompileOption;
import net.seapanda.bunnyhop.model.node.BhNode;
import net.seapanda.bunnyhop.service.LogManager;
import net.seapanda.bunnyhop.service.MessageService;

/**
 * BhProgram に対する以下の操作を実装したクラス.
 *
 * <pre>
 * - BhProgram の実行
 * - BhProgram の終了
 * - BhProgram とのデータ通信の有効化
 * - BhProgram とのデータ通信の無効化
 * - BhProgram へのデータ送信
 * </pre>
 *
 * @author K.Koike
 */
public class RemoteBhProgramControllerImpl implements RemoteBhProgramController {
  
  private final BhCompiler compiler;
  private final RemoteBhRuntimeController runtimeCtrl;
  private final MessageService msgService;

  /**
   * コンストラクタ.
   *
   * @param compiler BhProgram の実行ファイルを作成するのに使うコンパイラ
   * @param controller BhProgram の実行環境を操作するオブジェクト
   * @param msgService アプリケーションユーザにメッセージを出力するためのオブジェクト.
   */
  public RemoteBhProgramControllerImpl(
      BhCompiler compiler,
      RemoteBhRuntimeController controller,
      MessageService msgService) {
    this.compiler = compiler;
    this.runtimeCtrl = controller;
    this.msgService = msgService;
  }

  @Override
  public synchronized boolean execute(
      ExecutableNodeSet nodeSet, String hostname, String uname, String password) {
    return compile(nodeSet)
        .map(srcPath -> startProgram(srcPath, hostname, uname, password))
        .orElse(false);
  }

  /**
   * ノードをコンパイルする.
   *
   * @param nodeSet コンパイル対象のノードのリスト
   * @param entryPoint プログラム開始時に実行されるノード
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
  private boolean startProgram(Path filePath, String hostname, String uname, String password) {
    return runtimeCtrl.start(filePath, hostname, uname, password);
  }

  @Override
  public synchronized boolean terminate(String hostname, String uname, String password) {
    return runtimeCtrl.terminate(hostname, uname, password);
  }

  @Override
  public synchronized boolean enableCommunication(String hostname, String uname, String password) {
    return runtimeCtrl.connect(hostname, uname, password);
  }

  @Override
  public synchronized boolean disableCommunication() {
    return runtimeCtrl.disconnect();
  }

  @Override
  public synchronized BhRuntimeStatus send(BhProgramNotification notif) {
    return runtimeCtrl.send(notif);
  }
}
