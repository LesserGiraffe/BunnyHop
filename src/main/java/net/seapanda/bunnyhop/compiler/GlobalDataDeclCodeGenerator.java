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

import java.util.Collection;
import net.seapanda.bunnyhop.model.node.TextNode;
import net.seapanda.bunnyhop.model.node.syntaxsymbol.SyntaxSymbol;

/**
 * グローバルデータを定義するコードを生成するクラス.
 *
 * @author K.Koike
 */
class GlobalDataDeclCodeGenerator {

  private final CommonCodeGenerator common;
  private final ExpCodeGenerator expCodeGen;

  GlobalDataDeclCodeGenerator(CommonCodeGenerator common, ExpCodeGenerator expCodeGen) {
    this.common = common;
    this.expCodeGen = expCodeGen;
  }

  /**
   * グローバルデータを定義するコードを作成する.
   *
   * @param nodeListToCompile コンパイル対象のノードリスト
   * @param code 生成したコードの格納先
   * @param nestLevel ソースコードのネストレベル
   * @param option コンパイルオプション
   */
  void genGlobalDataDecls(
      Collection<? extends SyntaxSymbol> nodeListToCompile,
      StringBuilder code,
      int nestLevel,
      CompileOption option) {

    nodeListToCompile.forEach(node -> {
      if (SymbolNames.GlobalData.LIST.contains(node.getSymbolName())) {
        genGlobalDataDecls(node, code, nestLevel, option);
      }
    });
  }

  /**
   * グローバルデータを定義するコードを作成する.
   *
   * @param globalDataDeclNode グローバルデータ定義ノード
   * @param code 生成したコードの格納先
   * @param nestLevel ソースコードのネストレベル
   * @param option コンパイルオプション
   */
  private void genGlobalDataDecls(
      SyntaxSymbol globalDataDeclNode,
      StringBuilder code,
      int nestLevel,
      CompileOption option) {

    if (!SymbolNames.GlobalData.LIST.contains(globalDataDeclNode.getSymbolName())) {
      return;
    }
    if (option.withComments) {
      SymbolNames.GlobalData.DATA_NAME_CNCTR_LIST.stream()
          .map(cnctrName ->
              (TextNode) globalDataDeclNode.findDescendantOf("*", cnctrName, "*"))
          .filter(node -> node != null)
          .findFirst()
          .map(node -> node.getText())
          .ifPresent(comment -> {
            code.append(common.indent(nestLevel))
                .append(" /*")
                .append(comment)
                .append("*/" + Keywords.newLine);
          });
    }

    String varName =
        expCodeGen.genPreDefFuncCallExp(code, globalDataDeclNode, nestLevel, option, true);
    String thisVarName = common.genVarName(globalDataDeclNode);
    if (!varName.equals(thisVarName)) {
      code.append(common.indent(nestLevel))
          .append(thisVarName)
          .append(" = ")
          .append(varName)
          .append(";").append(Keywords.newLine);
    }

    SyntaxSymbol nextGlobalDataDecl = globalDataDeclNode.findDescendantOf(
        "*", SymbolNames.GlobalData.NEXT_GLOBAL_DATA_DECL, "*");
    if (nextGlobalDataDecl != null) {
      genGlobalDataDecls(nextGlobalDataDecl, code, nestLevel, option);
    }
  }
}
