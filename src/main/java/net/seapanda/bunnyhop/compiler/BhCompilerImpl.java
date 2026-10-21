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


import static net.seapanda.bunnyhop.compiler.Keywords.Prefix;
import static net.seapanda.bunnyhop.compiler.Keywords.newLine;

import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.SequencedCollection;
import java.util.Set;
import java.util.stream.Collectors;
import net.seapanda.bunnyhop.compiler.ScriptIdentifiers.Funcs;


/**
 * {@link BhCompiler} の実装クラス.
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
  private final String mainRoutineId = Funcs.BH_MAIN;

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
  public Path compile(SourceSet sourceSet, CompileOption option)
      throws CompileError {
    var targets = new ArrayList<>(sourceSet.getRootNodes());
    CompilableSyntaxTreeNode mainEntryPoint = sourceSet.getSelectedEntryPoint();
    if (mainEntryPoint != null && !targets.contains(mainEntryPoint)) {
      targets.add(mainEntryPoint);
    }
    Preprocessor.process(sourceSet.getInstanceIdToNode());
    StringBuilder code = new StringBuilder();
    generateCode(sourceSet, code, option);

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
   * @param sourceSet コンパイル対象のノードを提供するオブジェクト
   * @param code 生成したソースコードの格納先
   * @param option コンパイルオプション
   */
  private void generateCode(
      SourceSet sourceSet,
      StringBuilder code,
      CompileOption option) {
    String libCode = commonCodeList.stream().reduce("", (a, b) -> a + b);
    code.append(libCode).append(newLine);
    var rootNodes = sourceSet.getRootNodes();
    SequencedCollection<CompilableSyntaxTreeNode> globalVars =
        varDeclCodeGen.generateVarDecls(rootNodes, code, 0, option);
    generateSetGlobalVars(globalVars, code, 0, option);
    globalDataDeclCodeGen.generateGlobalDataDecls(rootNodes, code, 0, option);
    generateEntryPointIdsRegistration(sourceSet, code, 0);
    code.append(newLine);
    funcDefCodeGen.generateFuncDefs(rootNodes, code, 0, option);
    eventHandlerCodeGen.generateEventHandlers(rootNodes, code, 0, option);
    generateMainMethod(sourceSet.getSelectedEntryPoint(), code, option);
    generateAddEventFuncCall(option.mainRoutineLaunchEvent, Funcs.BH_MAIN, code, 0);
    generateFooterSnippet(code, 0);

  }

  /**
   * メインエントリポイントのコードを生成する.
   *
   * @param mainEntryPoint プログラム開始時に実行する最初のノード.  (nullable)
   * @param code 生成したソースコードの格納先
   * @param option コンパイルオプション
   */
  private void generateMainMethod(
      CompilableSyntaxTreeNode mainEntryPoint, StringBuilder code, CompileOption option) {
    if (mainEntryPoint == null) {
      code.append("function %s() {}\n".formatted(Funcs.BH_MAIN));
      return;
    }
    String lockVar = Prefix.lockVar + Funcs.BH_MAIN;
    CompilableSyntaxTreeNode mainRoutineSymbol = createMainRoutineNode();
    eventHandlerCodeGen.generateHeaderSnippetOfEventCall(
        code, mainRoutineSymbol, Funcs.BH_MAIN, lockVar, 0, option);
    expCodeGen.generateExpression(mainEntryPoint, code, 4, option);
    statCodeGen.generateStatement(mainEntryPoint, code, 4, option);
    eventHandlerCodeGen.generateFooterSnippetOfEventCall(code, lockVar, 0, option);
  }

  /** イベントとそれに対応するメソッドを追加する関数を呼ぶコードを生成する. */
  private void generateAddEventFuncCall(
      String eventName,
      String funcName,
      StringBuilder code,
      int nestLevel) {
    String addEventCallStat = common.generateFuncCall(
        Funcs.ADD_EVENT,
        funcName,
        "'%s'".formatted(eventName));
    code.append(common.indent(nestLevel))
        .append(addEventCallStat)
        .append(";")
        .repeat(newLine, 2);
  }

  /**
   * プログラムの末尾に付くコードを生成する.
   *
   * @param code 生成したコードの格納先
   * @param nestLevel ソースコードのネストレベル
   */
  private void generateFooterSnippet(StringBuilder code, int nestLevel) {
    // プログラム開始時刻の更新
    code.append(common.indent(nestLevel))
        .append(common.generateFuncCall(Funcs.START_TIMER))
        .append(";")
        .append(newLine);

    // グローバル変数初期化スレッドのスレッドコンテキストをデバッガから削除する
    code.append(common.indent(nestLevel))
        .append(common.generateFuncCall(Funcs.NOTIFY_THREAD_END))
        .append(";")
        .append(newLine);
  }

  /**
   * グローバル変数のアクセサをデバッガに追加するコードを生成する.
   *
   * @param variables グローバル変数の変数定義ノード一覧
   * @param code 生成したソースコードの格納先
   * @param nestLevel ソースコードのネストレベル
   * @param option コンパイルオプション
   */
  private void generateSetGlobalVars(
      SequencedCollection<CompilableSyntaxTreeNode> variables,
      StringBuilder code,
      int nestLevel,
      CompileOption option) {
    if (!option.addVarAccessorToVarStack) {
      return;
    }
    List<String> varAccessors = variables.stream().map(common::generateVarAccessorName).toList();
    String accessorElems = String.join(", ", varAccessors);
    if (!accessorElems.isEmpty()) {
      code.append(common.indent(nestLevel))
          .append(Funcs.SET_GLOBAL_VARIABLES)
          .append("([%s]);%s".formatted(accessorElems, newLine));
    }
  }

  /**
   * イベントハンドラ一覧をデバッガに追加するコードを生成する.
   *
   * <p>この処理はデバッグオプションに関係なく行う.
   *
   * @param sourceSet コンパイル対象のノードを提供するオブジェクト
   * @param code 生成したソースコードの格納先
   * @param nestLevel ソースコードのネストレベル
   */
  private void generateEntryPointIdsRegistration(
      SourceSet sourceSet,
      StringBuilder code,
      int nestLevel) {
    Set<CompilableSyntaxTreeNode> entryPoints = sourceSet.getRootNodes().stream()
        .filter(CompilableSyntaxTreeNode::isBuiltInEntryPoint)
        .collect(Collectors.toCollection(HashSet::new));
    Optional.ofNullable(sourceSet.getSelectedEntryPoint()).ifPresent(entryPoints::add);

    String entryPointIds = entryPoints.stream()
        .map(node -> "'" + node.getInstanceId() + "'")
        .reduce((a, b) -> a + ", " + b)
        .orElse("");

    code.append(common.indent(nestLevel))
        .append(Funcs.SET_ENTRY_POINT_IDS)
        .append("([%s]);%s".formatted(entryPointIds, newLine));
  }

  /** メインルーチンに対応する {@link CompilableSyntaxTreeNode} を作成する. */
  private CompilableSyntaxTreeNode createMainRoutineNode() {
    return new CompilableSyntaxTreeNode() {

      private String lexeme = mainRoutineId;
      private String tag = mainRoutineId;

      @Override
      public List<? extends CompilableSyntaxTreeNode> findDescendants(
          int generation, boolean toBottom, String... symbolNames) {
        return new ArrayList<>();
      }

      @Override
      public CompilableSyntaxTreeNode findDescendantOf(String... symbolNamePath) {
        return null;
      }

      @Override
      public CompilableSyntaxTreeNode findAncestor(
          int generation, boolean toTop, String symbolName) {
        return null;
      }

      @Override
      public CompilableSyntaxTreeNode findAncestorOf(String... symbolNamePath) {
        return null;
      }

      @Override
      public String getSymbolName() {
        return "";
      }

      @Override
      public String getLexeme() {
        return lexeme;
      }

      @Override
      public void setLexeme(String val) {
        lexeme = val;
      }

      @Override
      public String getInstanceId() {
        return mainRoutineId;
      }

      @Override
      public String getTag() {
        return tag;
      }

      @Override
      public void setTag(String val) {
        tag = val;
      }

      @Override
      public CompilableSyntaxTreeNode getOriginal() {
        return null;
      }

      @Override
      public boolean isBuiltInEntryPoint() {
        return false;
      }

      @Override
      public boolean canSetBreakpoint() {
        return false;
      }
    };
  }

  @Override
  public String mainRoutineId() {
    return mainRoutineId;
  }
}
