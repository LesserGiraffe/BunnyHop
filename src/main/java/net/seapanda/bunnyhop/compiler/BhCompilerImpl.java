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

package net.seapanda.bunnyhop.compiler;

import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import net.seapanda.bunnyhop.bhprogram.common.message.BhProgramEvent;
import net.seapanda.bunnyhop.model.node.BhNode;

/**
 * BhNode をコンパイルするクラス.
 *
 * @author K.Koike
 */
public class BhCompilerImpl implements BhCompiler {

  private final VarDeclCodeGenerator varDeclCodeGen;
  private final FuncDefCodeGenerator funcDefCodeGen;
  private final StatCodeGenerator statCodeGen;
  private final EventHandlerCodeGenerator eventHandlerCodeGen;
  private final CommonCodeGenerator common;
  private final GlobalDataDeclCodeGenerator globalDataDeclCodeGen;
  private final List<String> commonCodeList;

  /**
   * コンストラクタ.
   *
   * @param libs ライブラリファイルのパス
   */
  public BhCompilerImpl(Path... libs) throws IOException {
    common = new CommonCodeGenerator();
    varDeclCodeGen = new VarDeclCodeGenerator(common);
    ExpCodeGenerator expCodeGen = new ExpCodeGenerator(common, varDeclCodeGen);
    statCodeGen = new StatCodeGenerator(common, expCodeGen, varDeclCodeGen);
    funcDefCodeGen = new FuncDefCodeGenerator(common, statCodeGen, varDeclCodeGen);
    eventHandlerCodeGen = new EventHandlerCodeGenerator(common, statCodeGen, varDeclCodeGen);
    globalDataDeclCodeGen = new GlobalDataDeclCodeGenerator(common, expCodeGen);
    commonCodeList = readLibCode(libs);
  }

  private List<String> readLibCode(Path... libPaths) throws IOException {
    var codeList = new ArrayList<String>();
    for (Path libPath : libPaths) {
      byte[] content = Files.readAllBytes(libPath);
      codeList.add(new String(content, StandardCharsets.UTF_8));
    }
    return codeList;
  }

  @Override
  public Path compile(
      BhNode entryPoint, Collection<BhNode> nodesToCompile, CompileOption option)
      throws CompileError {
    Preprocessor.process(nodesToCompile);
    StringBuilder code = new StringBuilder();
    genCode(code, entryPoint, nodesToCompile, option);

    try {
      new File(option.outFile.getParent().toAbsolutePath().toString()).mkdirs();
    } catch (Exception e) {
      throw new CompileError("%s\n%s".formatted(e, option.outFile.toString()));
    }
    try (BufferedWriter writer = Files.newBufferedWriter(
        option.outFile,
        StandardCharsets.UTF_8,
        StandardOpenOption.CREATE,
        StandardOpenOption.TRUNCATE_EXISTING,
        StandardOpenOption.WRITE)) {
      writer.write(code.toString());
    } catch (Exception e) {
      throw new CompileError("%s\n%s".formatted(e, option.outFile.toString()));
    }
    return option.outFile;
  }

  /**
   * プログラム全体のコードを生成する.
   *
   * @param code 生成したソースコードの格納先
   * @param execNode 実行するノード
   * @param nodeListToCompile コンパイル対象のノードリスト (execNode を含む)
   * @param option コンパイルオプション
   */
  private void genCode(
      StringBuilder code,
      BhNode execNode,
      Collection<BhNode> nodeListToCompile,
      CompileOption option) {
    String libCode = commonCodeList.stream().reduce("", (a, b) -> a + b);
    code.append(libCode);
    varDeclCodeGen.genVarDecls(nodeListToCompile, code, 1, option);
    globalDataDeclCodeGen.genGlobalDataDecls(nodeListToCompile, code, 1, option);
    code.append(Keywords.newLine);
    funcDefCodeGen.genFuncDefs(nodeListToCompile, code, 1, option);
    eventHandlerCodeGen.genEventHandlers(nodeListToCompile, code, 1, option);
    String lockVar = Keywords.Prefix.lockVar + ScriptIdentifiers.Funcs.BH_MAIN;
    eventHandlerCodeGen.genHeaderSnippetOfEventCall(
        code, ScriptIdentifiers.Funcs.BH_MAIN, lockVar, 1);
    statCodeGen.genStatement(execNode, code, 5, option);
    eventHandlerCodeGen.genFooterSnippetOfEventCall(code, lockVar, 1);
    String addEventCallStat = common.genFuncCallCode(
        ScriptIdentifiers.Funcs.ADD_EVENT,
        ScriptIdentifiers.Funcs.BH_MAIN,
        "'%s'".formatted(BhProgramEvent.Name.PROGRAM_START));
    addEventCallStat += ";" + Keywords.newLine;
    code.append(common.indent(1)).append(addEventCallStat).append(Keywords.newLine);
    genCodeForProgramStart(code, 1, option);
  }

  /**
   * プログラム開始前の初期化用コードを生成する.
   *
   * @param code 生成したコードの格納先
   * @param nestLevel ソースコードのネストレベル
   * @param option コンパイルオプション
   */
  private void genCodeForProgramStart(StringBuilder code, int nestLevel, CompileOption option) {
    // プログラム開始時刻の更新
    code.append(common.indent(nestLevel))
        .append(ScriptIdentifiers.Vars.PROGRAM_STARTING_TIME)
        .append(" = ")
        .append(common.genFuncCallCode(ScriptIdentifiers.Funcs.CURRENT_TIME_MILLS))
        .append(";" + Keywords.newLine);
  }
}
