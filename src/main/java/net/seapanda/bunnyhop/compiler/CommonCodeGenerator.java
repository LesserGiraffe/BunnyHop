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
import static net.seapanda.bunnyhop.compiler.Keywords.Prefix;
import static net.seapanda.bunnyhop.compiler.Keywords.newLine;
import static net.seapanda.bunnyhop.compiler.SymbolNames.UserDefFunc;

import java.util.ArrayList;
import java.util.SequencedCollection;
import net.seapanda.bunnyhop.compiler.api.CompilableSyntaxTreeNode;
import net.seapanda.bunnyhop.compiler.api.CompileOption;

/**
 * 式や文のコード生成に必要な共通の機能を持つクラス.
 *
 * @author K.Koike
 */
class CommonCodeGenerator {

  /**
   * {@code varDecl} から変数名を生成する.
   *
   * @param varDecl この {@link CompilableSyntaxTreeNode} に対応する変数名を作成する
   * @return 変数名
   */
  String generateVarName(CompilableSyntaxTreeNode varDecl) {
    return Prefix.var + varDecl.getTag();
  }

  /**
   * {@code varDecl} から, 変数アクセサを格納する変数の名前を生成する.
   *
   * @param varDecl この {@link CompilableSyntaxTreeNode} に対応する変数名を作成する
   * @return 変数名
   */
  String generateVarAccessorName(CompilableSyntaxTreeNode varDecl) {
    return Prefix.outArg + varDecl.getTag();
  }

  /**
   * {@code funcDef} から関数名を生成する.
   *
   * @param funcDef この {@link CompilableSyntaxTreeNode} に対応する関数名を作成する
   * @return 関数名
   */
  String generateFuncName(CompilableSyntaxTreeNode funcDef) {
    return Prefix.func + funcDef.getTag();
  }

  /**
   * 関数呼び出しのコードを生成する.
   *
   * @param funcName 関数名
   * @param argNames 引数名のリスト
   * @return 関数呼び出しのコード
   */
  String generateFuncCall(String funcName, String... argNames) {
    StringBuilder code = new StringBuilder();
    code.append(funcName)
        .append("(");
    for (int i  = 0; i < argNames.length - 1; ++i) {
      code.append(argNames[i])
          .append(",");
    }
    if (argNames.length == 0) {
      code.append(")");
    } else {
      code.append(argNames[argNames.length - 1])
          .append(")");
    }
    return code.toString();
  }

  /**
   * プロパティアクセス式を作成する.
   *
   * @param root プロパティのルート
   * @param properties root の下に続くプロパティ名のリスト
   * @return プロパティアクセス式
   */
  String generatePropertyAccess(String root, String... properties) {
    StringBuilder code = new StringBuilder(root);
    for (String prop : properties) {
      code.append(".").append(prop);
    }
    return code.toString();
  }

  /**
   * {@code varDecl} が出力引数であるかどうかチェックする.
   *
   * @param varDecl 変数宣言ノード
   * @return {@code varDecl} が出力引数である場合 true を返す.
   */
  boolean isOutputParam(CompilableSyntaxTreeNode varDecl) {
    return varDecl.findAncestor(1, true, UserDefFunc.OUT_PARAM_DECL) != null;
  }

  /** 出力引数の値を取得するコードを生成する. */
  String generateGetOutputParamVal(CompilableSyntaxTreeNode varDecl) {
    return generatePropertyAccess(
        generateVarName(varDecl), ScriptIdentifiers.Properties.GET) + "()";
  }

  /** 出力引数に値を設定するコードを生成する. */
  String generateSetOutputParamVal(CompilableSyntaxTreeNode varDecl, String val) {
    return generatePropertyAccess(
        generateVarName(varDecl), ScriptIdentifiers.Properties.SET) + "(" + val + ")";
  }

  /**
   * コールスタックに関数呼び出しノードのインスタンス ID の追加を行うコードを生成する.
   *
   * @param code 生成したコードの格納先
   * @param nestLevel ソースコードのネストレベル
   * @param option コンパイルオプション
   */
  void generatePushToCallStack(
      StringBuilder code,
      int nestLevel,
      CompileOption option) {
    if (option.addNodeInstIdToCallStack) {
      code.append(indent(nestLevel))
          .append(generateGetCallStack())
          .append(";")
          .append(newLine);
      
      code.append(indent(nestLevel))
          .append(generatePushToCallStack())
          .append(";")
          .append(newLine);
    }
  }

  /** コールスタックに関数呼び出しノードのインスタンス ID を追加するコードを生成する. */
  private String generatePushToCallStack() {
    return "%s[%s.%s] = %s[%s]".formatted(
        ScriptIdentifiers.Vars.CALL_STACK,
        ScriptIdentifiers.Vars.CALL_STACK,
        ScriptIdentifiers.JsProperties.LENGTH,
        ScriptIdentifiers.Vars.THREAD_CONTEXT,
        ScriptIdentifiers.Vars.IDX_NEXT_NODE_INST_ID);
  }

  /** スレッドコンテキストからコールスタックを取り出して, 専用の変数に格納するコードを生成する. */
  private String generateGetCallStack() {
    // let _callStack = _threadContext[_idxCallStack]
    return "%s%s = %s[%s]".formatted(
        Js._const_,
        ScriptIdentifiers.Vars.CALL_STACK,
        ScriptIdentifiers.Vars.THREAD_CONTEXT,
        ScriptIdentifiers.Vars.IDX_CALL_STACK);
  }

  /**
   * コールスタックから関数呼び出しノードのインスタンス ID を削除するコードを生成する.
   *
   * @param code 生成したコードの格納先
   * @param nestLevel ソースコードのネストレベル
   * @param option コンパイルオプション
   */
  void generatePopFromCallStack(
      StringBuilder code,
      int nestLevel,
      CompileOption option) {
    if (option.addNodeInstIdToCallStack) {      
      code.append(indent(nestLevel))
          .append(generatePopFromCallStack())
          .append(";")
          .append(newLine);
    }
  }

  /** コールスタックから関数呼び出しノードのインスタンス ID を削除するコードを生成する. */
  private String generatePopFromCallStack() {
    return "%s.%s()".formatted(
        ScriptIdentifiers.Vars.CALL_STACK,
        ScriptIdentifiers.JsFuncs.POP);
  }

  /**
   * {@link CompilableSyntaxTreeNode} のインスタンス ID を専用の変数に代入するコードを生成する.
   *
   * @param node この {@link CompilableSyntaxTreeNode} のインスタンス ID を格納するコードを生成する.
   * @param code 生成したコードの格納先
   * @param nestLevel ソースコードのネストレベル
   * @param option コンパイルオプション
   * @return ノードのインスタンス ID を格納した変数名. コードを作成しなかった場合は空の文字列.
   */
  public String generateAssignNodeInstId(
      CompilableSyntaxTreeNode node,
      StringBuilder code,
      int nestLevel,
      CompileOption option) {
    if (option.addNodeInstIdToContext || option.addConditionalWait) {
      String instIdVar = Prefix.instIdVar + node.getTag();
      code.append(indent(nestLevel))
          .append("%s%s = '%s';".formatted(Js._let_, instIdVar, node.getInstanceId()))
          .append(newLine);
      return instIdVar;
    }
    return "";
  }

  /**
   * 次に処理するノードのインスタンス ID をスレッドコンテキストに設定するコードを生成する.
   *
   * @param instIdExp 評価結果がノードのインスタンス ID になる式の文字列
   * @param code 生成したコードの格納先
   * @param nestLevel ソースコードのネストレベル
   * @param option コンパイルオプション
   */
  public void generateSetInstIdToThreadContext(
      String instIdExp,
      StringBuilder code,
      int nestLevel,
      CompileOption option) {
    if (option.addNodeInstIdToContext) {
      var assignStat = "%s[%s] = %s;".formatted(
          ScriptIdentifiers.Vars.THREAD_CONTEXT,
          ScriptIdentifiers.Vars.IDX_NEXT_NODE_INST_ID,
          instIdExp);
      code.append(indent(nestLevel))
          .append(assignStat)
          .append(newLine);
    }
  }

  /**
   * 条件付きで一時停止するコードを生成する.
   *
   * <p>本メソッドで生成されるコードは, ブレークポイントなどで一時停止するための機能を実現する.
   *
   * @param instIdExp 評価結果がノードのインスタンス ID になる式の文字列
   * @param code 生成したコードの格納先
   * @param nestLevel ソースコードのネストレベル
   * @param option コンパイルオプション
   */
  void generateConditionalWait(
      String instIdExp,
      StringBuilder code,
      int nestLevel,
      CompileOption option) {
    // _condWait(inst-id-exp);
    if (option.addConditionalWait) {
      code.append(indent(nestLevel))
          .append(generateFuncCall(ScriptIdentifiers.Funcs.COND_WAIT, instIdExp))
          .append(";")
          .append(newLine);
    }
  }

  /**
   * 変数フレームを変数スタックに追加するコードを生成する.
   *
   * @param code 生成したコードの格納先
   * @param nestLevel ソースコードのネストレベル
   * @param option コンパイルオプション
   */
  void generatePushToVarStack(
      StringBuilder code,
      int nestLevel,
      CompileOption option) {
    generatePushToVarStack(new ArrayList<>(), new ArrayList<>(), code, nestLevel, option);
  }

  /**
   * 変数アクセサを変数フレームに格納して, それを変数スタックに追加するコードを生成する.
   *
   * @param varDecls 変数宣言ノードのリスト
   * @param outParams 出力パラメータのリスト
   * @param code 生成したコードの格納先
   * @param nestLevel ソースコードのネストレベル
   * @param option コンパイルオプション
   */
  void generatePushToVarStack(
      SequencedCollection<CompilableSyntaxTreeNode> varDecls,
      SequencedCollection<CompilableSyntaxTreeNode> outParams,
      StringBuilder code,
      int nestLevel,
      CompileOption option) {
    if (!option.addVarAccessorToVarStack) {
      return;
    }
    var varAccessors = new ArrayList<String>();
    for (CompilableSyntaxTreeNode varDecl : varDecls) {
      varAccessors.add("%s".formatted(generateVarAccessorName(varDecl)));
    }
    for (CompilableSyntaxTreeNode outParam : outParams) {
      varAccessors.add("%s".formatted(generateVarAccessorName(outParam)));
    }
    // let _varFrame = [_vo123, ... ];
    code.append(indent(nestLevel))
        .append(Js._const_)
        .append(ScriptIdentifiers.Vars.VAR_FRAME)
        .append(" = [")
        .append(String.join(", ", varAccessors))
        .append("];")
        .append(newLine);

    // let _varStack = _threadContext[_idxVarStack];
    code.append(indent(nestLevel))
        .append(generateGetVarStack())
        .append(";")
        .append(newLine);

    // _varStack[_varStack.length] = _varFrame;
    code.append(indent(nestLevel))
        .append(generatePushToVarStack())
        .append(";")
        .append(newLine);
  }

  /** 変数スタックに変数フレームを追加するコードを生成する. */
  private String generatePushToVarStack() {
    return "%s[%s.%s] = %s".formatted(
        ScriptIdentifiers.Vars.VAR_STACK,
        ScriptIdentifiers.Vars.VAR_STACK,
        ScriptIdentifiers.JsProperties.LENGTH,
        ScriptIdentifiers.Vars.VAR_FRAME);
  }  

  /** スレッドコンテキストから変数スタックを取り出して, 専用の変数に格納するコードを生成する. */
  private String generateGetVarStack() {
    // let _varStack = _threadContext[_varStackIdx];
    return "%s%s = %s[%s]".formatted(
        Js._const_,
        ScriptIdentifiers.Vars.VAR_STACK,
        ScriptIdentifiers.Vars.THREAD_CONTEXT,
        ScriptIdentifiers.Vars.IDX_VAR_STACK);
  }

  /**
   * 変数スタックから要素を取り除くコードを生成する.
   *
   * @param code 生成したコードの格納先
   * @param nestLevel ソースコードのネストレベル
   * @param option コンパイルオプション
   */
  void generatePopFromVarStack(
      StringBuilder code,
      int nestLevel,
      CompileOption option) {
    if (!option.addVarAccessorToVarStack) {
      return;
    }
    code.append(indent(nestLevel))
        .append(generatePopFromToVarStack())
        .append(";")
        .append(newLine);
  }

  /** 変数スタックから要素を取り除くコードを生成する. */
  private String generatePopFromToVarStack() {
    return "%s.%s()".formatted(
        ScriptIdentifiers.Vars.VAR_STACK,
        ScriptIdentifiers.JsFuncs.POP);
  }

  /**
   * 変数アクセサを専用のスタックフレームに追加するコードを生成する.
   *
   * @param varDecls 変数宣言ノードのリスト
   * @param code 生成したコードの格納先
   * @param nestLevel ソースコードのネストレベル
   * @param option コンパイルオプション
   */
  void generatePushToVarFrame(
      SequencedCollection<CompilableSyntaxTreeNode> varDecls,
      StringBuilder code,
      int nestLevel,
      CompileOption option) {
    if (!option.addVarAccessorToVarStack || varDecls.isEmpty()) {
      return;
    }
    code.append(indent(nestLevel))
        .append(generatePushToVarFrame(varDecls))
        .append(";")
        .append(newLine);
  }

  /** 変数アクセサを専用のスタックフレームに追加するコードを生成する. */
  private String generatePushToVarFrame(SequencedCollection<CompilableSyntaxTreeNode> varDecls) {
    if (varDecls.size() == 1) {
      return "%s[%s.%s] = %s".formatted(
          ScriptIdentifiers.Vars.VAR_FRAME,
          ScriptIdentifiers.Vars.VAR_FRAME,
          ScriptIdentifiers.JsProperties.LENGTH,
          generateVarAccessorName(varDecls.getFirst()));
    }
    var varAccessors = new ArrayList<String>();
    for (CompilableSyntaxTreeNode varDecl : varDecls) {
      varAccessors.add("%s".formatted(generateVarAccessorName(varDecl)));
    }
    return "%s.%s(%s)".formatted(
        ScriptIdentifiers.Vars.VAR_FRAME,
        ScriptIdentifiers.JsFuncs.PUSH,
        String.join(", ", varAccessors));
  }

  /**
   * 変数アクセサを専用のスタックフレームから削除するコードを生成する.
   *
   * @param numVars 取り除く変数アクセサの個数
   * @param code 生成したコードの格納先
   * @param nestLevel ソースコードのネストレベル
   * @param option コンパイルオプション
   */
  void generatePopFromVarFrame(
      int numVars,
      StringBuilder code,
      int nestLevel,
      CompileOption option) {
    // _varFrame.splice(_varFrame.length - n, n);
    if (!option.addVarAccessorToVarStack || numVars == 0) {
      return;
    }
    code.append(indent(nestLevel))
        .append(generatePopFromVarFrame(numVars))
        .append(";")
        .append(newLine);
  }

  /** 変数アクセサを専用のスタックフレームから削除するコードを生成する. */
  private String generatePopFromVarFrame(int numVars) {
    if (numVars == 1) {
      return "%s.%s()".formatted(ScriptIdentifiers.Vars.VAR_FRAME, ScriptIdentifiers.JsFuncs.POP);
    }
    return "%s.%s(%s.%s - %s, %s)".formatted(
        ScriptIdentifiers.Vars.VAR_FRAME,
        ScriptIdentifiers.JsFuncs.SPLICE,
        ScriptIdentifiers.Vars.VAR_FRAME,
        ScriptIdentifiers.JsProperties.LENGTH,
        numVars,
        numVars);
  }

  /**
   * 引数で指定した文字列を JavaScript の文字列リテラル表現に変換する.
   */
  String toJsString(String str) {
    return "'" + str
        .replaceAll("\\\\", "\\\\\\\\")
        .replaceAll("'", "\\\\'")
        .replaceAll("\n", "\\\\n")
        + "'";
  }

  /** {@code numStr} で表される数値を Javasctipt の文字列リテラル表現に変換する. */
  String toJsNumber(String numStr) {
    return switch (numStr) {
      case "Infinity" -> "Number.POSITIVE_INFINITY";
      case "-Infinity" -> "Number.NEGATIVE_INFINITY";
      case "NaN" -> "Number.NaN";
      default -> numStr;
    };
  }

  /** インデント分の空白を返す. */
  String indent(int depth) {
    return switch (depth) {
      case 0 -> "";
      case 1 -> "  ";
      case 2 -> "    ";
      case 3 -> "      ";
      case 4 -> "        ";
      case 5 -> "          ";
      case 6 -> "            ";
      case 7 -> "              ";
      case 8 -> "                ";
      case 9 -> "                  ";
      case 10 -> "                    ";
      case 11 -> "                      ";
      case 12 -> "                        ";
      default -> " ".repeat(Math.max(0, depth));
    };
  }
}
