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
import java.util.List;
import net.seapanda.bunnyhop.compiler.SymbolNames.UserDefFunc;
import net.seapanda.bunnyhop.compiler.VarDeclCodeGenerator.ParamList;

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
   * @param nodes コンパイル対象のノードリスト
   * @param code 生成したコードの格納先
   * @param nestLevel ソースコードのネストレベル
   * @param option コンパイルオプション
   */
  void generateFuncDefs(
      Collection<CompilableSyntaxTreeNode> nodes,
      StringBuilder code,
      int nestLevel,
      CompileOption option) {
    for (CompilableSyntaxTreeNode symbol : nodes) {
      if (UserDefFunc.LIST.contains(symbol.getSymbolName())) {
        generateFuncDef(symbol, code, nestLevel, option);
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
  void generateFuncDef(
      CompilableSyntaxTreeNode funcDefNode,
      StringBuilder code,
      int nestLevel,
      CompileOption option) {
    String funcName = common.generateFuncName(funcDefNode);
    code.append(common.indent(nestLevel))
        .append(Keywords.Js._function_)
        .append(funcName)
        .append("(");
    if (option.withComments) {
      CompilableSyntaxTreeNode funcNameNode =
          funcDefNode.findDescendantOf("*", "*", UserDefFunc.FUNC_NAME, "*");
      code.append(" /*").append(funcNameNode.getLexeme()).append("*/");
    }
    CompilableSyntaxTreeNode param =
        funcDefNode.findDescendantOf("*", "*", UserDefFunc.PARAM_DECL, "*");
    CompilableSyntaxTreeNode outParam =
        funcDefNode.findDescendantOf("*", "*", UserDefFunc.OUT_PARAM_DECL, "*");
    var commonParams = List.of(ScriptIdentifiers.Vars.THREAD_CONTEXT);
    final ParamList params = varDeclCodeGen.generateParamList(
        commonParams, param, outParam, code, nestLevel + 1, option);
    code.append(") {")
        .append(newLine);
    varDeclCodeGen.generateVarAccessors(param, code, nestLevel + 1, option);
    // 変数アクセサを変数スタックに積む必要がないなら, 出力変数のアクセサは作る必要がない.
    if (option.addVarAccessorToVarStack) {
      varDeclCodeGen.generateOutParamAccessors(outParam, code, nestLevel + 1, option);
    }

    common.generatePushToCallStack(code, nestLevel + 1, option);
    common.generatePushToVarStack(
        params.inParams(), params.outParams(), code, nestLevel + 1, option);
    generateFuncDefInner(funcDefNode, code, nestLevel + 1, option);
    common.generatePopFromVarStack(code, nestLevel + 1, option);
    common.generatePopFromCallStack(code, nestLevel + 1, option);
    code.append(common.indent(nestLevel))
        .append("}")
        .repeat(newLine, 2);
  }

  /**
   * 関数定義のコードの内部関数部分を作成する.
   *
   * @param funcDefNode 関数定義のノード
   * @param code 生成したコードの格納先
   * @param nestLevel ソースコードのネストレベル
   * @param option コンパイルオプション
   */
  private void generateFuncDefInner(
      CompilableSyntaxTreeNode funcDefNode,
      StringBuilder code,
      int nestLevel,
      CompileOption option) {
    // _end : {
    code.append(common.indent(nestLevel))
        .append(ScriptIdentifiers.Label.end)
        .append(" : {")
        .append(newLine);
    
    CompilableSyntaxTreeNode stat =
        funcDefNode.findDescendantOf("*", "*", SymbolNames.Stat.STAT_LIST, "*");
    statCodeGen.generateStatement(stat, code, nestLevel + 1, option);
    code.append(common.indent(nestLevel))
        .append("}")
        .append(newLine);
  }
}
