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

import net.seapanda.bunnyhop.model.syntaxsymbol.SyntaxSymbol;

/**
 * 式や文のコード生成に必要な共通の機能を持つクラス.
 *
 * @author K.Koike
 */
public class CommonCodeGenerator {

  /**
   * 変数定義から変数名を生成する.
   *
   * @param varDecl 変数定義ノード
   * @return 変数名
   */
  public String genVarName(SyntaxSymbol varDecl) {
    return Keywords.Prefix.varPrefix + varDecl.getSymbolId();
  }

  /**
   * 変数定義から出力引数に代入するための変数名を生成する.
   *
   * @param varDecl 変数定義ノード
   * @return 変数名
   */
  public String genOutArgName(SyntaxSymbol varDecl) {
    return Keywords.Prefix.outArgPrefix + varDecl.getSymbolId();
  }

  /**
   * 関数定義から関数名を生成する.
   *
   * @param funcDef 関数定義シンボル
   * @return 関数名
   */
  public String genFuncName(SyntaxSymbol funcDef) {
    return Keywords.Prefix.funcPrefix + funcDef.getSymbolId();
  }

  /**
   * 関数呼び出しのコードを作成する.
   *
   * @param funcName 関数名
   * @param argNames 引数名のリスト
   * @return 関数呼び出しのコード
   */
  public String genFuncCallCode(String funcName, String... argNames) {
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
   * 関数呼び出しのコードを作成する. funcName.call(thisObj, args).
   *
   * @param funcName 関数名
   * @param thisObj call の第一引数
   * @return 関数呼び出しのコード
   */
  public String genFuncPrototypeCallCode(String funcName, String thisObj, String... args) {
    String[] argList = new String[args.length + 1];
    argList[0] = thisObj;
    for (int i = 0; i < args.length; ++i) {
      argList[i + 1] = args[i];
    }
    String funcCall = genPropertyAccessCode(funcName, ScriptIdentifiers.JsFuncs.CALL);
    return genFuncCallCode(funcCall, argList);
  }

  /**
   * プロパティアクセス式を作成する.
   *
   * @param root プロパティのルート
   * @param properties root の下に続くプロパティ名のリスト
   * @return プロパティアクセス式
   */
  public String genPropertyAccessCode(String root, String... properties) {
    StringBuilder code = new StringBuilder(root);
    for (String prop : properties) {
      code.append(".").append(prop);
    }
    return code.toString();
  }

  /**
   * コールスタックに関数呼び出しノードのシンボル ID を追加するコードを作成する.
   *
   * @param funcCallNode 関数呼び出しノード
   */
  public String genPushToCallStackCode(SyntaxSymbol funcCallNode) {
    var funcName = genPropertyAccessCode(
        Keywords.Js._this,
        ScriptIdentifiers.Properties.CALL_STACK,
        ScriptIdentifiers.JsFuncs.PUSH);
    return genFuncCallCode(funcName, toJsString(funcCallNode.getSymbolId().toString()));
  }

  /** コールスタックから関数呼び出しノードのシンボル ID を削除するコードを作成する. */
  public String genPopFromCallStackCode() {
    var funcName = genPropertyAccessCode(
        Keywords.Js._this, ScriptIdentifiers.Properties.CALL_STACK, ScriptIdentifiers.JsFuncs.POP);
    return genFuncCallCode(funcName);
  }

  /**
   * {@code varDecl} が出力引数であるかどうかチェックする.
   *
   * @param varDecl 変数宣言ノード
   * @return {@code varDecl} が出力引数である場合 true を返す.
   */
  public boolean isOutputParam(SyntaxSymbol varDecl) {
    return varDecl.findSymbolInAncestors(SymbolNames.UserDefFunc.OUT_PARAM_DECL, 1, true) != null;
  }

  /** 出力引数の値を取得するコードを作成する. */
  public String genGetOutputParamValCode(SyntaxSymbol varDecl) {
    return genPropertyAccessCode(
        genVarName(varDecl), ScriptIdentifiers.Properties.OUT_PARAM_GETTER) + "()";
  }

  /** 出力引数に値を設定するコードを作成する. */
  public String genSetOutputParamValCode(SyntaxSymbol varDecl, String val) {
    return genPropertyAccessCode(
        genVarName(varDecl), ScriptIdentifiers.Properties.OUT_PARAM_SETTER) + "(" + val + ")";
  }

  /**
   * 引数で指定した文字列を Javascript の文字列リテラル表現に変換する.
   */
  public String toJsString(String str) {
    return "'" + str.replaceAll("\\\\", "\\\\\\\\").replaceAll("'", "\\\\'") + "'";
  }

  /** インデント分の空白を返す. */
  public String indent(int depth) {
    switch (depth) {
      case 0: return "";
      case 1: return "  ";
      case 2: return "    ";
      case 3: return "      ";
      case 4: return "        ";
      case 5: return "          ";
      case 6: return "            ";
      case 7: return "              ";
      case 8: return "                ";
      case 9: return "                  ";
      case 10: return "                    ";
      case 11: return "                      ";
      case 12: return "                        ";
      default: {
        StringBuilder ret = new StringBuilder("");
        for (int i = 0; i < depth; ++i) {
          ret.append("  ");
        }
        return ret.toString();
      }
    }
  }
}
