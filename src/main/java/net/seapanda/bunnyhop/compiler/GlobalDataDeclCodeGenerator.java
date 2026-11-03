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

import static net.seapanda.bunnyhop.compiler.Keywords.newLine;

import java.util.Collection;
import java.util.Objects;
import net.seapanda.bunnyhop.compiler.api.CompilableSyntaxTreeNode;
import net.seapanda.bunnyhop.compiler.api.CompileOption;

/**
 * グローバルデータを定義するコードを生成するクラス.
 *
 * @author K.Koike
 */
class GlobalDataDeclCodeGenerator {

  private final SymbolNames symbols;
  private final CommonCodeGenerator common;
  private final ExpCodeGenerator expCodeGen;

  GlobalDataDeclCodeGenerator(
      SymbolNames symbols, CommonCodeGenerator common, ExpCodeGenerator expCodeGen) {
    this.symbols = symbols;
    this.common = common;
    this.expCodeGen = expCodeGen;
  }

  /**
   * グローバルデータを定義するコードを作成する.
   *
   * @param targets コンパイル対象のノードリスト
   * @param code 生成したコードの格納先
   * @param nestLevel ソースコードのネストレベル
   * @param option コンパイルオプション
   */
  void generateGlobalDataDecls(
      Collection<CompilableSyntaxTreeNode> targets,
      StringBuilder code,
      int nestLevel,
      CompileOption option) {
    targets.forEach(node -> generateGlobalDataDecls(node, code, nestLevel, option));
  }

  /**
   * グローバルデータを定義するコードを作成する.
   *
   * @param globalDataDeclNode グローバルデータ定義ノード
   * @param code 生成したコードの格納先
   * @param nestLevel ソースコードのネストレベル
   * @param option コンパイルオプション
   */
  private void generateGlobalDataDecls(
      CompilableSyntaxTreeNode globalDataDeclNode,
      StringBuilder code,
      int nestLevel,
      CompileOption option) {
    if (!symbols.globalDataDecl.list.contains(globalDataDeclNode.getSymbolName())) {
      return;
    }
    if (option.withComments) {
      symbols.globalDataDecl.dataNameCnctrList.stream()
          .map(cnctrName -> globalDataDeclNode.findDescendantOf("*", cnctrName, "*"))
          .filter(Objects::nonNull)
          .findFirst()
          .map(CompilableSyntaxTreeNode::getLexeme)
          .ifPresent(comment ->
              code.append(common.indent(nestLevel))
                  .append(" /*")
                  .append(comment)
                  .append("*/")
                  .append(newLine));
    }

    String varName =
        expCodeGen.generatePreDefFuncCallExp(globalDataDeclNode, code, nestLevel, option, true);
    String thisVarName = common.generateVarName(globalDataDeclNode);
    if (!varName.equals(thisVarName)) {
      code.append(common.indent(nestLevel))
          .append(thisVarName)
          .append(" = ")
          .append(varName)
          .append(";")
          .append(newLine);
    }

    CompilableSyntaxTreeNode nextGlobalDataDecl =
        globalDataDeclNode.findDescendantOf("*", symbols.globalDataDecl.nextGlobalDataDecl, "*");
    if (nextGlobalDataDecl != null) {
      generateGlobalDataDecls(nextGlobalDataDecl, code, nestLevel, option);
    }
  }
}
