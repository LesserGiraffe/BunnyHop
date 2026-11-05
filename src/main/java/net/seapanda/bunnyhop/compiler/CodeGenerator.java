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
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.SequencedCollection;
import java.util.Set;
import java.util.stream.Collectors;
import net.seapanda.bunnyhop.compiler.ScriptIdentifiers.Funcs;
import net.seapanda.bunnyhop.compiler.api.CompilableSyntaxTreeNode;
import net.seapanda.bunnyhop.compiler.api.CompileError;
import net.seapanda.bunnyhop.compiler.api.CompileOption;
import net.seapanda.bunnyhop.compiler.api.SourceSet;


/**
 * {@link BhCompiler} の実装クラス.
 *
 * @author K.Koike
 */
public class CodeGenerator implements BhCompiler {

  @Override
  public Path compile(SourceSet sourceSet, CompileOption option) throws CompileError {
    StringBuilder code = new StringBuilder();
    try (BufferedWriter writer = Files.newBufferedWriter(
        option.outFile,
        StandardCharsets.UTF_8,
        StandardOpenOption.CREATE,
        StandardOpenOption.TRUNCATE_EXISTING,
        StandardOpenOption.WRITE)) {
      var symbols = new SymbolNames(option.extensionPaths);
      CodeGenerators codeGens = CodeGenerators.create(symbols);
      Preprocessor.process(symbols, sourceSet.getInstanceIdToNode());
      generateLibCode(option.libPaths, code);
      generateCode(codeGens, sourceSet, code, option);
      new File(option.outFile.getParent().toAbsolutePath().toString()).mkdirs();
      writer.write(code.toString());
    } catch (Exception e) {
      throw new CompileError("Failed to compile", e);
    }
    return option.outFile;
  }

  private void generateLibCode(SequencedCollection<String> libPaths, StringBuilder code)
      throws IOException {
    for (String libPath : libPaths) {
      code.append(Files.readString(Paths.get(libPath), StandardCharsets.UTF_8))
          .append(newLine);
    }
  }

  /**
   * プログラム全体のコードを生成する.
   *
   * @param codeGens 各種コードジェネレータを保持するオブジェクト
   * @param sourceSet コンパイル対象のノードを提供するオブジェクト
   * @param code 生成したソースコードの格納先
   * @param option コンパイルオプション
   */
  private void generateCode(
      CodeGenerators codeGens, SourceSet sourceSet, StringBuilder code, CompileOption option) {
    var rootNodes = sourceSet.getRootNodes();
    SequencedCollection<CompilableSyntaxTreeNode> globalVars =
        codeGens.varDecl.generateVarDecls(rootNodes, code, 0, option);
    generateSetGlobalVars(codeGens.common, globalVars, code, 0, option);
    codeGens.globalDataDec.generateGlobalDataDecls(rootNodes, code, 0, option);
    generateEntryPointIdsRegistration(codeGens.common, sourceSet, code, 0);
    code.append(newLine);
    codeGens.funcDef.generateFuncDefs(rootNodes, code, 0, option);
    codeGens.eventHandler.generateEventHandlers(rootNodes, code, 0, option);
    generateMainMethod(codeGens, sourceSet.getSelectedEntryPoint(), code, option);
    generateAddEventFuncCall(
        codeGens.common, option.mainRoutineLaunchEvent, option.mainRoutineName, code, 0);
    generateFooterSnippet(codeGens.common, code, 0);
  }

  /**
   * メインエントリポイントのコードを生成する.
   *
   * @param codeGens 各種コードジェネレータを保持するオブジェクト
   * @param mainEntryPoint プログラム開始時に実行する最初のノード.  (nullable)
   * @param code 生成したソースコードの格納先
   * @param option コンパイルオプション
   */
  private void generateMainMethod(
      CodeGenerators codeGens,
      CompilableSyntaxTreeNode mainEntryPoint,
      StringBuilder code,
      CompileOption option) {
    if (mainEntryPoint == null) {
      code.append("function %s() {}\n".formatted(option.mainRoutineName));
      return;
    }
    String lockVar = Prefix.lockVar + option.mainRoutineName;
    CompilableSyntaxTreeNode mainRoutineSymbol = createMainRoutineNode(option);
    codeGens.eventHandler.generateHeaderSnippetOfEventCall(
        code, mainRoutineSymbol, option.mainRoutineName, lockVar, 0, option);
    codeGens.exp.generateExpression(mainEntryPoint, code, 4, option);
    codeGens.stat.generateStatement(mainEntryPoint, code, 4, option);
    codeGens.eventHandler.generateFooterSnippetOfEventCall(code, lockVar, 0, option);
  }

  /** イベントとそれに対応するメソッドを追加する関数を呼ぶコードを生成する. */
  private void generateAddEventFuncCall(
      CommonCodeGenerator common,
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
   * @param common コード生成機能を提供するオブジェクト
   * @param code 生成したコードの格納先
   * @param nestLevel ソースコードのネストレベル
   */
  private void generateFooterSnippet(
      CommonCodeGenerator common, StringBuilder code, int nestLevel) {
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
   * @param common コード生成機能を提供するオブジェクト
   * @param variables グローバル変数の変数定義ノード一覧
   * @param code 生成したソースコードの格納先
   * @param nestLevel ソースコードのネストレベル
   * @param option コンパイルオプション
   */
  private void generateSetGlobalVars(
      CommonCodeGenerator common,
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
   * @param common コード生成機能を提供するオブジェクト
   * @param sourceSet コンパイル対象のノードを提供するオブジェクト
   * @param code 生成したソースコードの格納先
   * @param nestLevel ソースコードのネストレベル
   */
  private void generateEntryPointIdsRegistration(
      CommonCodeGenerator common,
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
  private CompilableSyntaxTreeNode createMainRoutineNode(CompileOption option) {
    return new CompilableSyntaxTreeNode() {

      private String lexeme = option.mainRoutineName;
      private String tag = option.mainRoutineName;

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
        return option.mainRoutineName;
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

  record CodeGenerators(
      CommonCodeGenerator common,
      VarDeclCodeGenerator varDecl,
      ExpCodeGenerator exp,
      StatCodeGenerator stat,
      FuncDefCodeGenerator funcDef,
      EventHandlerCodeGenerator eventHandler,
      GlobalDataDeclCodeGenerator globalDataDec) {

    static CodeGenerators create(SymbolNames symbols) {
      var common = new CommonCodeGenerator(symbols);
      var varDeclCodeGen = new VarDeclCodeGenerator(symbols, common);
      var expCodeGen = new ExpCodeGenerator(symbols, common, varDeclCodeGen);
      var statCodeGen = new StatCodeGenerator(symbols, common, expCodeGen, varDeclCodeGen);
      var funcDefCodeGen = new FuncDefCodeGenerator(symbols, common, statCodeGen, varDeclCodeGen);
      var eventHandlerCodeGen =
              new EventHandlerCodeGenerator(symbols, common, statCodeGen, varDeclCodeGen);
      var globalDataDeclCodeGen = new GlobalDataDeclCodeGenerator(symbols, common, expCodeGen);

      return new CodeGenerators(
          common,
          varDeclCodeGen,
          expCodeGen,
          statCodeGen,
          funcDefCodeGen,
          eventHandlerCodeGen,
          globalDataDeclCodeGen);
    }
  }
}
