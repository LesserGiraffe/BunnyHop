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
import java.util.List;
import net.seapanda.bunnyhop.compiler.VarDeclCodeGenerator.ParamList;
import net.seapanda.bunnyhop.node.model.TextNode;
import net.seapanda.bunnyhop.node.model.syntaxsymbol.SyntaxSymbol;

/**
 * 関数定義のコード生成を行うクラス.
 *
 * @author K.Koike
 */
class FuncDefCodeGenerator {

  private final CommonCodeGenerator common;
  private final StatCodeGenerator statCodeGen;
  private final VarDeclCodeGenerator varDeclCodeGen;

  /** コンストラクタ. */
  FuncDefCodeGenerator(
      CommonCodeGenerator common,
      StatCodeGenerator statCodeGen,
      VarDeclCodeGenerator varDeclCodeGen) {

    this.common = common;
    this.statCodeGen = statCodeGen;
    this.varDeclCodeGen = varDeclCodeGen;
  }

  /**
   * 関数定義のコードを作成する.
   *
   * @param compiledNodeList コンパイル対象のノードリスト
   * @param code 生成したコードの格納先
   * @param nestLevel ソースコードのネストレベル
   * @param option コンパイルオプション
   */
  void genFuncDefs(
      Collection<? extends SyntaxSymbol> compiledNodeList,
      StringBuilder code,
      int nestLevel,
      CompileOption option) {

    for (SyntaxSymbol symbol : compiledNodeList) {
      if (SymbolNames.UserDefFunc.LIST.contains(symbol.getSymbolName())) {
        genFuncDef(symbol, code, nestLevel, option);
      }
    }
  }

  /**
   * 関数定義のコードを作成する.
   *
   * @param funcDefNode 関数定義のノード
   * @param code 生成したコードの格納先
   * @param nestLevel ソースコードのネストレベル
   * @param option コンパイルオプション
   */
  void genFuncDef(
      SyntaxSymbol funcDefNode,
      StringBuilder code,
      int nestLevel,
      CompileOption option) {

    String funcName = common.genFuncName(funcDefNode);
    code.append(common.indent(nestLevel))
        .append(Keywords.Js._function_)
        .append(funcName)
        .append("(");
    if (option.withComments) {
      TextNode funcNameNode = (TextNode) funcDefNode.findDescendantOf(
          "*", "*", SymbolNames.UserDefFunc.FUNC_NAME, "*");
      code.append(" /*").append(funcNameNode.getText()).append("*/");
    }
    SyntaxSymbol param = funcDefNode.findDescendantOf(
        "*", "*", SymbolNames.UserDefFunc.PARAM_DECL, "*");
    SyntaxSymbol outParam = funcDefNode.findDescendantOf(
        "*", "*", SymbolNames.UserDefFunc.OUT_PARAM_DECL, "*");
    var commonParams = List.of(ScriptIdentifiers.Vars.THREAD_CONTEXT);
    final ParamList params =
        varDeclCodeGen.genParamList(commonParams, param, outParam, code, nestLevel + 1, option);
    code.append(") {" + Keywords.newLine);
    varDeclCodeGen.genVarAccessors(param, code, nestLevel + 1, option);
    // 変数アクセサを変数スタックに積む必要がないなら, 出力変数のアクセサは作る必要がない.
    if (option.addVarAccessorToVarStack) {
      varDeclCodeGen.genOutParamAccessors(outParam, code, nestLevel + 1, option);
    }

    common.genPushToCallStack(code, nestLevel + 1, option);
    common.genPushToVarStack(params.inParams(), params.outParams(), code, nestLevel + 1, option);
    genFuncDefInner(funcDefNode, code, nestLevel + 1, option);
    common.genPopFromVarStack(code, nestLevel + 1, option);
    common.genPopFromCallStack(code, nestLevel + 1, option);    
    code.append(common.indent(nestLevel))
        .append("}" + Keywords.newLine + Keywords.newLine);
  }

  /**
   * 関数定義のコードの内部関数部分を作成する.
   *
   * @param funcDefNode 関数定義のノード
   * @param code 生成したコードの格納先
   * @param nestLevel ソースコードのネストレベル
   * @param option コンパイルオプション
   */
  private void genFuncDefInner(
      SyntaxSymbol funcDefNode,
      StringBuilder code,
      int nestLevel,
      CompileOption option) {
    // _end : {
    code.append(common.indent(nestLevel))
        .append(ScriptIdentifiers.Label.end)
        .append(" : {" + Keywords.newLine);
    
    SyntaxSymbol stat =
        funcDefNode.findDescendantOf("*", "*", SymbolNames.Stat.STAT_LIST, "*");
    statCodeGen.genStatement(stat, code, nestLevel + 1, option);
    code.append(common.indent(nestLevel))
        .append("}" + Keywords.newLine);
  }
}
