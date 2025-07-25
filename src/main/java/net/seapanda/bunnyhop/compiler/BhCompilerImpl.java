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
import java.util.SequencedCollection;
import net.seapanda.bunnyhop.bhprogram.common.message.BhProgramEvent;
import net.seapanda.bunnyhop.model.node.BhNode;
import net.seapanda.bunnyhop.model.node.syntaxsymbol.InstanceId;
import net.seapanda.bunnyhop.model.node.syntaxsymbol.SyntaxSymbol;

/**
 * BhNode をコンパイルするクラス.
 *
 * @author K.Koike
 */
public class BhCompilerImpl implements BhCompiler {

  private final VarDeclCodeGenerator varDeclCodeGen;
  private final FuncDefCodeGenerator funcDefCodeGen;
  private final ExpCodeGenerator expCodeGen;
  private final StatCodeGenerator statCodeGen;
  private final EventHandlerCodeGenerator eventHandlerCodeGen;
  private final CommonCodeGenerator common;
  private final GlobalDataDeclCodeGenerator globalDataDeclCodeGen;
  private final List<String> commonCodeList;
  private final InstanceId startupRoutineId = InstanceId.of(ScriptIdentifiers.Funcs.BH_MAIN);

  /**
   * コンストラクタ.
   *
   * @param libs ライブラリファイルのパス
   */
  public BhCompilerImpl(Path... libs) throws IOException {
    common = new CommonCodeGenerator();
    varDeclCodeGen = new VarDeclCodeGenerator(common);
    expCodeGen = new ExpCodeGenerator(common, varDeclCodeGen);
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
  public Path compile(BhNode entryPoint, Collection<BhNode> targets, CompileOption option)
      throws CompileError {
    targets = new ArrayList<>(targets);
    if (entryPoint != null && !targets.contains(entryPoint)) {
      targets.add(entryPoint);
    }
    Preprocessor.process(targets);
    StringBuilder code = new StringBuilder();
    genCode(code, entryPoint, targets, option);

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
   * @param entryPoint プログラム開始時に実行する最初のノード
   * @param targets コンパイル対象のノードリスト
   * @param option コンパイルオプション
   */
  private void genCode(
      StringBuilder code,
      BhNode entryPoint,
      Collection<BhNode> targets,
      CompileOption option) {
    String libCode = commonCodeList.stream().reduce("", (a, b) -> a + b);
    code.append(libCode).append(Keywords.newLine);
    SequencedCollection<SyntaxSymbol> globalVars =
        varDeclCodeGen.genVarDecls(targets, code, 0, option);
    genSetGlobalVars(code, globalVars, 0, option);
    globalDataDeclCodeGen.genGlobalDataDecls(targets, code, 0, option);
    code.append(Keywords.newLine);
    funcDefCodeGen.genFuncDefs(targets, code, 0, option);
    eventHandlerCodeGen.genEventHandlers(targets, code, 0, option);
    genMainMethod(code, entryPoint, option);
    genAddEventFuncCall(
        BhProgramEvent.Name.PROGRAM_START, ScriptIdentifiers.Funcs.BH_MAIN, code, 0);
    genFooterSnippet(code, 0, option);
  }

  /**
   * エントリポイントのコードを生成する.
   *
   * @param code 生成したソースコードの格納先
   * @param entryPoint プログラム開始時に実行する最初のノード
   * @param option コンパイルオプション
   */
  private void genMainMethod(StringBuilder code, BhNode entryPoint, CompileOption option) {
    if (entryPoint == null) {
      code.append("function %s() {}".formatted(ScriptIdentifiers.Funcs.BH_MAIN));
      return;
    }
    String lockVar = Keywords.Prefix.lockVar + ScriptIdentifiers.Funcs.BH_MAIN;
    eventHandlerCodeGen.genHeaderSnippetOfEventCall(
        code, startupRoutineId, false, ScriptIdentifiers.Funcs.BH_MAIN, lockVar, 0, option);
    expCodeGen.genExpression(code, entryPoint, 4, option);
    statCodeGen.genStatement(entryPoint, code, 4, option);
    eventHandlerCodeGen.genFooterSnippetOfEventCall(code, lockVar, 0, option);
  }

  /** イベントとそれに対応するメソッドを追加する関数を呼ぶコードを生成する. */
  private void genAddEventFuncCall(
      BhProgramEvent.Name event,
      String funcName,
      StringBuilder code,
      int nestLevel) {
    String addEventCallStat = common.genFuncCall(
        ScriptIdentifiers.Funcs.ADD_EVENT,
        funcName,
        "'%s'".formatted(event));
    code.append(common.indent(nestLevel))
        .append(addEventCallStat + ";")
        .append(Keywords.newLine.repeat(2));
  }

  /**
   * プログラムの末尾に付くコードを生成する.
   *
   * @param code 生成したコードの格納先
   * @param nestLevel ソースコードのネストレベル
   * @param option コンパイルオプション
   */
  private void genFooterSnippet(StringBuilder code, int nestLevel, CompileOption option) {
    // プログラム開始時刻の更新
    code.append(common.indent(nestLevel))
        .append(common.genFuncCall(ScriptIdentifiers.Funcs.START_TIMER))
        .append(";" + Keywords.newLine);

    // グローバル変数初期化スレッドのスレッドコンテキストをデバッガから削除する
    code.append(common.indent(nestLevel))
        .append(common.genFuncCall(ScriptIdentifiers.Funcs.NOTIFY_THREAD_END))
        .append(";" + Keywords.newLine);
  }

  /**
   * グローバル変数のアクセサをデバッガに追加するコードを生成する.
   *
   * @param code 生成したソースコードの格納先
   * @param variables グローバル変数の変数定義ノード一覧
   * @param option コンパイルオプション
   */
  private void genSetGlobalVars(
      StringBuilder code,
      SequencedCollection<SyntaxSymbol> variables,
      int nestLevel,
      CompileOption option) {
    if (!option.addVarAccesorToVarStack) {
      return;
    }
    List<String> varAccessors = variables.stream().map(common::genVarAccesorName).toList();
    String accessorElems = String.join(", ", varAccessors);
    if (!accessorElems.isEmpty()) {
      code.append(common.indent(nestLevel))
          .append(ScriptIdentifiers.Funcs.SET_GLOBAL_VARIABLES)
          .append("([%s]);%s".formatted(accessorElems, Keywords.newLine));
    }
  }

  @Override
  public InstanceId startupRoutineId() {
    return startupRoutineId;
  }
}
