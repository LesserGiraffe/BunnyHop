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

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.SequencedCollection;
import java.util.stream.Collectors;
import net.seapanda.bunnyhop.node.model.TextNode;
import net.seapanda.bunnyhop.node.model.syntaxsymbol.InstanceId;
import net.seapanda.bunnyhop.node.model.syntaxsymbol.SyntaxSymbol;

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
   * {@code nodeListToCompile} から辿れる変数宣言ノードの変数宣言コードを作成する.
   *
   * @param nodeListToCompile コンパイル対象のノードリスト
   * @param code 生成したコードの格納先
   * @param nestLevel ソースコードのネストレベル
   * @param option コンパイルオプション
   * @return コードを作成した変数宣言ノードのリスト
   */
  SequencedCollection<SyntaxSymbol> genVarDecls(
      Collection<? extends SyntaxSymbol> nodeListToCompile,
      StringBuilder code,
      int nestLevel,
      CompileOption option) {

    var varDecls = new ArrayList<SyntaxSymbol>();
    for (SyntaxSymbol node : nodeListToCompile) {
      varDecls.addAll(genVarDecls(node, code, nestLevel, option));
    }
    return varDecls;
  }

  /**
   * {@code varDecl} から辿れる変数宣言ノードの変数宣言コードを作成する.
   *
   * @param varDecl 変数宣言ノード
   * @param code 生成したコードの格納先
   * @param nestLevel ソースコードのネストレベル
   * @param option コンパイルオプション
   * @return コードを作成した変数宣言ノードのリスト
   */
  SequencedCollection<SyntaxSymbol> genVarDecls(
      SyntaxSymbol varDecl,
      StringBuilder code,
      int nestLevel,
      CompileOption option) {

    List<SyntaxSymbol> varDelcs = collectVarDecls(varDecl);
    List<VarDeclInfo> varDeclInfoList = varDelcs.stream().map(this::toVarDeclInfo).toList();
    genVarDecls(code, varDeclInfoList, nestLevel, option);
    return varDelcs;
  }

  /**
   * 変数宣言リストのコードを生成する.
   *
   * @param code 生成したコードの格納先
   * @param varDeclInfoList 変数宣言に必要な情報のリスト
   * @param nestLevel ソースコードのネストレベル
   * @param option コンパイルオプション
   */
  private void genVarDecls(
      StringBuilder code,
      List<VarDeclInfo> varDeclInfoList,
      int nestLevel,
      CompileOption option) {

    varDeclInfoList.forEach(varDeclInfo -> {
      genVarDecl(code, varDeclInfo, nestLevel, option);
      genVarAccessor(code, varDeclInfo, nestLevel, option);
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
  ParamList genParamList(
      List<String> commonParams,
      SyntaxSymbol paramNode,
      SyntaxSymbol outParamNode,
      StringBuilder code,
      int nestLevel,
      CompileOption option) {

    List<VarDeclInfo> paramList = commonParams.stream()
        .map(param -> new VarDeclInfo(param, "", "", InstanceId.NONE, ""))
        .collect(Collectors.toCollection(ArrayList::new));

    List<SyntaxSymbol> params = collectVarDecls(paramNode);
    List<SyntaxSymbol> outParams = collectVarDecls(outParamNode);
    List<VarDeclInfo> varDeclInfoList = params.stream().map(this::toVarDeclInfo).toList();
    List<VarDeclInfo> outVarDeclInfoList = outParams.stream().map(this::toVarDeclInfo).toList();    
    paramList.addAll(varDeclInfoList);
    paramList.addAll(outVarDeclInfoList);

    if (paramList.size() >= 1) {
      code.append(Keywords.newLine);
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
        code.append(Keywords.newLine);
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
  private VarDeclInfo toVarDeclInfo(SyntaxSymbol varDecl) {
    String comment =
        SymbolNames.VarDecl.VAR_NAME_CNCTR_LIST.stream()
        .map(cnctrName -> (TextNode) varDecl.findDescendantOf("*", cnctrName, "*"))
        .filter(Objects::nonNull)
        .findFirst()
        .map(TextNode::getText).orElse("");
    String varName = common.genVarName(varDecl);
    String outArgName = common.genVarAccessorName(varDecl);
    String initVal = SymbolNames.VarDecl.INIT_VAL_MAP.get(varDecl.getSymbolName());
    return new VarDeclInfo(varName, outArgName, initVal, varDecl.getInstanceId(), comment);
  }

  /** {@code root} から辿れる変数宣言ノードを集める. */
  private List<SyntaxSymbol> collectVarDecls(SyntaxSymbol root) {
    var varDecls = new ArrayList<SyntaxSymbol>();
    while (root != null && SymbolNames.VarDecl.LIST.contains(root.getSymbolName())) {
      varDecls.add(root);
      root = root.findDescendantOf("*", SymbolNames.VarDecl.NEXT_VAR_DECL, "*");
    }
    return varDecls;
  }

  /** {@link VarDeclInfo} オブジェクトから変数宣言コードを作成する. */
  private void genVarDecl(
      StringBuilder code,
      VarDeclInfo varDeclInfo,
      int nestLevel,
      CompileOption option) {

    code.append(common.indent(nestLevel))
        .append(Keywords.Js._let_)
        .append(varDeclInfo.varName)
        .append(" = ")
        .append(varDeclInfo.initVal)
        .append(";");

    if (option.withComments) {
      code.append(" /*")
          .append(varDeclInfo.comment)
          .append("*/");
    }
    code.append(Keywords.newLine);
  }

  /** 変数アクセサを定義するコードを生成する. */
  private void genVarAccessor(
      StringBuilder code,
      VarDeclInfo varDeclInfo,
      int nestLevel,
      CompileOption option) {

    code.append(common.indent(nestLevel))
        .append(Keywords.Js._const_)
        .append(varDeclInfo.outArgName)
        .append(" = {")
        .append(ScriptIdentifiers.Properties.SET + ": (v) => ")
        .append(varDeclInfo.varName)
        .append(" = v, ")
        .append(ScriptIdentifiers.Properties.GET + ": () => ")
        .append(varDeclInfo.varName);

    if (option.addVarAccessorToVarStack) {
      code.append(", ")
          .append(ScriptIdentifiers.Properties.ID + ": ")
          .append("'%s'".formatted(varDeclInfo.instId()));
    }
    code.append("};" + Keywords.newLine);
  }

  /** 出力変数用のアクセサを定義するコードを生成する. */
  private void genOutParamAccessor(
      StringBuilder code,
      VarDeclInfo varDeclInfo,
      int nestLevel,
      CompileOption option) {

    code.append(common.indent(nestLevel))
        .append(Keywords.Js._const_)
        .append(varDeclInfo.outArgName)
        .append(" = {")
        .append(ScriptIdentifiers.Properties.SET + ": ")
        .append("%s.%s, ".formatted(varDeclInfo.varName, ScriptIdentifiers.Properties.SET))
        .append(ScriptIdentifiers.Properties.GET + ": ")
        .append("%s.%s".formatted(varDeclInfo.varName, ScriptIdentifiers.Properties.GET));

    if (option.addVarAccessorToVarStack) {
      code.append(", ")
          .append(ScriptIdentifiers.Properties.ID + ": ")
          .append("'%s'".formatted(varDeclInfo.instId()));
    }
    code.append("};" + Keywords.newLine);
  }

  /**
   * 変数宣言に必要な情報.
   *
   * @param varName 変数名
   * @param outArgName 出力引数に代入する変数名
   * @param initVal 初期値
   * @param instId 変数宣言ノードの {@link InstanceId}
   * @param comment コメント (デバッグ用)
   */
  private static record VarDeclInfo(
      String varName,
      String outArgName,
      String initVal,
      InstanceId instId,
      String comment) {}
  
  /**
   * 関数のパラメータのリスト.
   *
   * @param inParams 仮引数のリスト
   * @param outParams 出力引数のリスト
   */
  static record ParamList(
      SequencedCollection<SyntaxSymbol> inParams,
      SequencedCollection<SyntaxSymbol> outParams) {}

  /**
   * 変数宣言文を作成する.
   *
   * @param code 生成したコードの格納先
   * @param varName 変数名
   * @param initExp 初期値. null の場合は初期値をセットしない.
   * @param nestLevel ネストレベル
   */
  public void genVarDeclStat(
      StringBuilder code,
      String varName,
      String initExp,
      int nestLevel) {

    code.append(common.indent(nestLevel))
        .append(Keywords.Js._let_)
        .append(varName);

    if (initExp != null) {
      code.append(" = (")
          .append(initExp)
          .append(");" + Keywords.newLine);
    } else {
      code.append(";" + Keywords.newLine);
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
  void genVarAccessors(
      SyntaxSymbol varDecl,
      StringBuilder code,
      int nestLevel,
      CompileOption option) {

    collectVarDecls(varDecl).stream()
        .map(this::toVarDeclInfo)
        .forEach(varDeclInfo -> genVarAccessor(code, varDeclInfo, nestLevel, option));
  }

  /**
   * 出力変数用のアクセサを定義するコードを生成する.
   *
   * @param outParam 出力変数宣言ノード
   * @param code 生成したコードの格納先
   * @param nestLevel ソースコードのネストレベル
   * @param option コンパイルオプション
   */
  void genOutParamAccessors(
      SyntaxSymbol outParam,
      StringBuilder code,
      int nestLevel,
      CompileOption option) {

    collectVarDecls(outParam).stream()
        .map(this::toVarDeclInfo)
        .forEach(varDeclInfo -> genOutParamAccessor(code, varDeclInfo, nestLevel, option));
  }

  /**
   * 変数を定義するコードとそのアクセサオブジェクトを定義するコードを作成する.
   *
   * @return [0] -> {@code varDecl} から作成した変数の名前 <br>
   *         [1] -> アクセサオブジェクトを格納した変数の名前
   */
  List<String> genVarDeclAndAccessor(
      SyntaxSymbol varDecl,
      String initVal,
      StringBuilder code,
      int nestLevel,
      CompileOption option) {

    String varName = common.genVarName(varDecl);
    code.append(common.indent(nestLevel))
        .append(Keywords.Js._let_)
        .append(varName)
        .append(" = ")
        .append(initVal)
        .append(";")
        .append(Keywords.newLine);

    String accessorName = common.genVarAccessorName(varDecl);
    genVarAccessor(
        code,
        new VarDeclInfo(varName, accessorName, "", varDecl.getInstanceId(), ""),
        nestLevel,
        option);
    return List.of(varName, accessorName);
  }
}
