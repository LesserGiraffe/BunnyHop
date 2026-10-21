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

import static net.seapanda.bunnyhop.compiler.Keywords.Js;
import static net.seapanda.bunnyhop.compiler.Keywords.newLine;
import static net.seapanda.bunnyhop.compiler.ScriptIdentifiers.Properties.GET;
import static net.seapanda.bunnyhop.compiler.ScriptIdentifiers.Properties.ID;
import static net.seapanda.bunnyhop.compiler.ScriptIdentifiers.Properties.SET;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.SequencedCollection;
import java.util.stream.Collectors;

/**
 * 変数宣言のコードを生成するクラス.
 *
 * @author K.Koike
 */
class VarDeclCodeGenerator {

  private final CommonCodeGenerator common;

  VarDeclCodeGenerator(CommonCodeGenerator common) {
    this.common = common;
  }

  /**
   * {@code nodes} のノードが変数宣言ノードであった場合, それから辿れる変数宣言ノードのコードを作成する.
   *
   * @param nodes コンパイル対象のノードリスト
   * @param code 生成したコードの格納先
   * @param nestLevel ソースコードのネストレベル
   * @param option コンパイルオプション
   * @return コードを作成した変数宣言ノードのリスト
   */
  SequencedCollection<CompilableSyntaxTreeNode> generateVarDecls(
      Collection<CompilableSyntaxTreeNode> nodes,
      StringBuilder code,
      int nestLevel,
      CompileOption option) {
    var varDecls = new ArrayList<CompilableSyntaxTreeNode>();
    for (CompilableSyntaxTreeNode node : nodes) {
      varDecls.addAll(generateVarDecls(node, code, nestLevel, option));
    }
    return varDecls;
  }

  /**
   * {@code node} が変数宣言ノードであった場合, それから辿れる変数宣言ノードのコードを作成する.
   *
   * @param node コンパイル対象のノード
   * @param code 生成したコードの格納先
   * @param nestLevel ソースコードのネストレベル
   * @param option コンパイルオプション
   * @return コードを作成した変数宣言ノードのリスト
   */
  SequencedCollection<CompilableSyntaxTreeNode> generateVarDecls(
      CompilableSyntaxTreeNode node,
      StringBuilder code,
      int nestLevel,
      CompileOption option) {
    List<CompilableSyntaxTreeNode> varDecls = collectVarDecls(node);
    List<VarDeclInfo> varDeclInfoList = varDecls.stream().map(this::toVarDeclInfo).toList();
    generateVarDecls(varDeclInfoList, code, nestLevel, option);
    return varDecls;
  }

  /**
   * 変数宣言リストのコードを生成する.
   *
   * @param code 生成したコードの格納先
   * @param varDeclInfoList 変数宣言に必要な情報のリスト
   * @param nestLevel ソースコードのネストレベル
   * @param option コンパイルオプション
   */
  private void generateVarDecls(
      List<VarDeclInfo> varDeclInfoList,
      StringBuilder code,
      int nestLevel,
      CompileOption option) {
    varDeclInfoList.forEach(varDeclInfo -> {
      generateVarDecl(code, varDeclInfo, nestLevel, option);
      generateVarAccessor(code, varDeclInfo, nestLevel, option);
    });
  }

  /**
   * 仮引数リストを作成する.
   *
   * @param commonParams 全てのメソッドが共通で持つパラメータのリスト
   * @param paramNode 仮引数のノード
   * @param outParamNode 出力引数ノード
   * @param code 生成したコードの格納先
   * @param nestLevel ソースコードのネストレベル
   * @param option コンパイルオプション
   * @return パラメータノードを格納したレコード
   */
  ParamList generateParamList(
      List<String> commonParams,
      CompilableSyntaxTreeNode paramNode,
      CompilableSyntaxTreeNode outParamNode,
      StringBuilder code,
      int nestLevel,
      CompileOption option) {
    List<VarDeclInfo> paramList = commonParams.stream()
        .map(param -> new VarDeclInfo(param, "", "", "", ""))
        .collect(Collectors.toCollection(ArrayList::new));

    List<CompilableSyntaxTreeNode> params = collectVarDecls(paramNode);
    List<CompilableSyntaxTreeNode> outParams = collectVarDecls(outParamNode);
    List<VarDeclInfo> varDeclInfoList = params.stream().map(this::toVarDeclInfo).toList();
    List<VarDeclInfo> outVarDeclInfoList = outParams.stream().map(this::toVarDeclInfo).toList();    
    paramList.addAll(varDeclInfoList);
    paramList.addAll(outVarDeclInfoList);

    if (!paramList.isEmpty()) {
      code.append(newLine);
    }
    for (int i = 0; i < paramList.size(); ++i) {
      VarDeclInfo param = paramList.get(i);
      code.append(common.indent(nestLevel))
          .append(param.varName);
      boolean isLastParam = i == (paramList.size() - 1);
      if (!isLastParam) {
        code.append(",");
      }
      if (option.withComments && !param.comment.isEmpty()) {
        code.append(" /*").append(param.comment).append("*/");
      }
      if (!isLastParam) {
        code.append(newLine);
      }
    }
    return new ParamList(params, outParams);
  }

  /**
   * 変数宣言ノードから {@link VarDeclInfo} オブジェクトを作成する.
   *
   * @param varDecl 変数宣言ノード
   * @return {@link VarDeclInfo} オブジェクト
   */
  private VarDeclInfo toVarDeclInfo(CompilableSyntaxTreeNode varDecl) {
    String comment =
        SymbolNames.VarDecl.VAR_NAME_CNCTR_LIST.stream()
        .map(cnctrName -> varDecl.findDescendantOf("*", cnctrName, "*"))
        .filter(Objects::nonNull)
        .findFirst()
        .map(CompilableSyntaxTreeNode::getLexeme)
        .orElse("");
    String varName = common.generateVarName(varDecl);
    String outArgName = common.generateVarAccessorName(varDecl);
    String initVal = SymbolNames.VarDecl.INIT_VAL_MAP.get(varDecl.getSymbolName());
    return new VarDeclInfo(varName, outArgName, initVal, varDecl.getInstanceId(), comment);
  }

  /** {@code node} から辿れる変数宣言ノードを集める. */
  private List<CompilableSyntaxTreeNode> collectVarDecls(CompilableSyntaxTreeNode node) {
    var varDecls = new ArrayList<CompilableSyntaxTreeNode>();
    while (node != null && SymbolNames.VarDecl.LIST.contains(node.getSymbolName())) {
      varDecls.add(node);
      node = node.findDescendantOf("*", SymbolNames.VarDecl.NEXT_VAR_DECL, "*");
    }
    return varDecls;
  }

  /** {@link VarDeclInfo} オブジェクトから変数宣言コードを作成する. */
  private void generateVarDecl(
      StringBuilder code,
      VarDeclInfo varDeclInfo,
      int nestLevel,
      CompileOption option) {
    code.append(common.indent(nestLevel))
        .append(Js._let_)
        .append(varDeclInfo.varName)
        .append(" = ")
        .append(varDeclInfo.initVal)
        .append(";");

    if (option.withComments) {
      code.append(" /*")
          .append(varDeclInfo.comment)
          .append("*/");
    }
    code.append(newLine);
  }

  /** 変数アクセサを定義するコードを生成する. */
  private void generateVarAccessor(
      StringBuilder code,
      VarDeclInfo varDeclInfo,
      int nestLevel,
      CompileOption option) {
    code.append(common.indent(nestLevel))
        .append(Js._const_)
        .append(varDeclInfo.outArgName)
        .append(" = {")
        .append("%s: (v) => %s = v, ".formatted(SET, varDeclInfo.varName))
        .append("%s: () => %s".formatted(GET, varDeclInfo.varName));

    if (option.addVarAccessorToVarStack) {
      code.append(", ")
          .append("%s: '%s'".formatted(ID, varDeclInfo.instId()));
    }
    code.append("};").append(newLine);
  }

  /** 出力変数用のアクセサを定義するコードを生成する. */
  private void generateOutParamAccessor(
      StringBuilder code,
      VarDeclInfo varDeclInfo,
      int nestLevel,
      CompileOption option) {
    code.append(common.indent(nestLevel))
        .append(Js._const_)
        .append(varDeclInfo.outArgName)
        .append(" = {")
        .append("%s: %s.%s, ".formatted(SET, varDeclInfo.varName, SET))
        .append("%s: %s.%s".formatted(GET, varDeclInfo.varName, GET));

    if (option.addVarAccessorToVarStack) {
      code.append(", ")
          .append("%s: '%s'".formatted(ID, varDeclInfo.instId()));
    }
    code.append("};").append(newLine);
  }

  /**
   * 変数宣言に必要な情報.
   *
   * @param varName 変数名
   * @param outArgName 出力引数に代入する変数名
   * @param initVal 初期値
   * @param instId 変数宣言ノードのインスタンス ID
   * @param comment コメント (デバッグ用)
   */
  private record VarDeclInfo(
      String varName,
      String outArgName,
      String initVal,
      String instId,
      String comment) {}
  
  /**
   * 関数のパラメータのリスト.
   *
   * @param inParams 仮引数のリスト
   * @param outParams 出力引数のリスト
   */
  record ParamList(
      SequencedCollection<CompilableSyntaxTreeNode> inParams,
      SequencedCollection<CompilableSyntaxTreeNode> outParams) {}

  /**
   * 変数宣言文を作成する.
   *
   * @param code 生成したコードの格納先
   * @param varName 変数名
   * @param initExp 初期値. null の場合は初期値をセットしない.
   * @param nestLevel ネストレベル
   */
  public void generateVarDeclStat(
      StringBuilder code,
      String varName,
      String initExp,
      int nestLevel) {
    code.append(common.indent(nestLevel))
        .append(Js._let_)
        .append(varName);

    if (initExp != null) {
      code.append(" = (")
          .append(initExp)
          .append(");")
          .append(newLine);
    } else {
      code.append(";")
          .append(newLine);
    }
  }

  /**
   * 変数アクセサを定義するコードを生成する.
   *
   * @param varDecl 変数宣言ノード
   * @param code 生成したコードの格納先
   * @param nestLevel ソースコードのネストレベル
   * @param option コンパイルオプション
   */
  void generateVarAccessors(
      CompilableSyntaxTreeNode varDecl,
      StringBuilder code,
      int nestLevel,
      CompileOption option) {
    collectVarDecls(varDecl).stream()
        .map(this::toVarDeclInfo)
        .forEach(varDeclInfo -> generateVarAccessor(code, varDeclInfo, nestLevel, option));
  }

  /**
   * 出力変数用のアクセサを定義するコードを生成する.
   *
   * @param outParam 出力変数宣言ノード
   * @param code 生成したコードの格納先
   * @param nestLevel ソースコードのネストレベル
   * @param option コンパイルオプション
   */
  void generateOutParamAccessors(
      CompilableSyntaxTreeNode outParam,
      StringBuilder code,
      int nestLevel,
      CompileOption option) {
    collectVarDecls(outParam).stream()
        .map(this::toVarDeclInfo)
        .forEach(varDeclInfo -> generateOutParamAccessor(code, varDeclInfo, nestLevel, option));
  }

  /**
   * 変数を定義するコードとそのアクセサオブジェクトを定義するコードを作成する.
   *
   * @return [0] -> {@code varDecl} から作成した変数の名前 <br>
   *         [1] -> アクセサオブジェクトを格納した変数の名前
   */
  List<String> generateVarDeclAndAccessor(
      CompilableSyntaxTreeNode varDecl,
      String initVal,
      StringBuilder code,
      int nestLevel,
      CompileOption option) {
    String varName = common.generateVarName(varDecl);
    code.append(common.indent(nestLevel))
        .append(Js._let_)
        .append(varName)
        .append(" = ")
        .append(initVal)
        .append(";")
        .append(newLine);

    String accessorName = common.generateVarAccessorName(varDecl);
    generateVarAccessor(
        code,
        new VarDeclInfo(varName, accessorName, "", varDecl.getInstanceId(), ""),
        nestLevel,
        option);
    return List.of(varName, accessorName);
  }
}
