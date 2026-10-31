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

package net.seapanda.bunnyhop.bhruntime.launch;

import static net.seapanda.bunnyhop.common.configuration.BhConstants.Path.Dir;
import static net.seapanda.bunnyhop.common.configuration.BhConstants.Path.File;
import static net.seapanda.bunnyhop.runtime.api.message.BhProgramEvent.Name.PROGRAM_START;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Optional;
import javafx.scene.control.Alert;
import net.seapanda.bunnyhop.bhruntime.ctrl.remote.RemoteBhRuntimeController;
import net.seapanda.bunnyhop.common.text.TextDefs;
import net.seapanda.bunnyhop.compile.CompileConfig;
import net.seapanda.bunnyhop.compiler.api.BhCompilerFactory;
import net.seapanda.bunnyhop.compiler.api.CompileError;
import net.seapanda.bunnyhop.compiler.api.CompileOption;
import net.seapanda.bunnyhop.compiler.api.SourceSet;
import net.seapanda.bunnyhop.service.LogManager;
import net.seapanda.bunnyhop.service.message.MessageService;
import net.seapanda.bunnyhop.utility.Utility;

/**
 * BhProgram の起動とその実行環境の制御用オブジェクトを取得する機能を提供するクラス.
 *
 * @author K.Koike
 */
public class RemoteBhProgramLauncherImpl implements RemoteBhProgramLauncher {

  private final CompileConfig compileConfig;
  private final RemoteBhRuntimeController runtimeCtrl;
  private final MessageService msgService;

  /**
   * コンストラクタ.
   *
   * @param compileConfig コンパイルに関する設定を格納したオブジェクト
   * @param controller BhProgram の実行環境を操作するオブジェクト
   * @param msgService アプリケーションユーザにメッセージを出力するためのオブジェクト.
   */
  public RemoteBhProgramLauncherImpl(
      CompileConfig compileConfig,
      RemoteBhRuntimeController controller,
      MessageService msgService) {
    this.compileConfig = compileConfig;
    this.runtimeCtrl = controller;
    this.msgService = msgService;
  }

  @Override
  public synchronized boolean launch(
      SourceSet sourceSet, String hostname, String uname, String password) {
    return compile(sourceSet)
        .map(srcPath -> startProgram(srcPath, hostname, uname, password))
        .orElse(false);
  }

  /**
   * ノードをコンパイルする.
   *
   * @param sourceSet コンパイル対象となるノード一覧を提供するオブジェクト
   * @return ノードをコンパイルしてできたソースファイルのパス
   */
  private Optional<Path> compile(SourceSet sourceSet) {
    CompileOption option = CompileOption.builder()
        .libPaths(compileConfig.getLibPaths())
        .outFile(Paths.get(Utility.execPath, Dir.COMPILED, File.APP_FILE_NAME_JS))
        .mainRoutineLaunchEvent(PROGRAM_START.toString())
        .mainRoutineName(compileConfig.getMainRoutineName())
        .build();
    try {
      Path outFile = BhCompilerFactory.create().compile(sourceSet, option);
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
  public RemoteBhRuntimeController getBhRuntimeCtrl() {
    return runtimeCtrl;
  }
}
