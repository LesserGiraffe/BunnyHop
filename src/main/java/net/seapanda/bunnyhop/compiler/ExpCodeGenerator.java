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
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import net.seapanda.bunnyhop.common.tools.Util;
import net.seapanda.bunnyhop.model.node.BhNode;
import net.seapanda.bunnyhop.model.node.TextNode;
import net.seapanda.bunnyhop.model.syntaxsymbol.SyntaxSymbol;

/**
 * 式のコード生成を行うクラス.
 *
 * @author K.Koike
 */
public class ExpCodeGenerator {

  private final CommonCodeGenerator common;
  private final VarDeclCodeGenerator varDeclCodeGen;

  public ExpCodeGenerator(CommonCodeGenerator common, VarDeclCodeGenerator varDeclCodeGen) {
    this.common = common;
    this.varDeclCodeGen = varDeclCodeGen;
  }

  /**
   * 式を作成する.
   *
   * @param code 途中式の格納先
   * @param expNode 式のノード
   * @param nestLevel ソースコードのネストレベル
   * @param option コンパイルオプション
   * @return 式もしくは式の評価結果を格納した変数. expNode に該当する式ノードが見つからなかった場合null.
   */
  public String genExpression(
      StringBuilder code,
      SyntaxSymbol expNode,
      int nestLevel,
      CompileOption option) {

    String expSymbolName = expNode.getSymbolName();
    if (SymbolNames.BinaryExp.LIST.contains(expSymbolName)) {
      return genBinaryExp(code, expNode, nestLevel, option);

    } else if (SymbolNames.UnaryExp.LIST.contains(expSymbolName)) {
      return genUnaryExp(code, expNode, nestLevel, option);

    } else if (SymbolNames.VarDecl.VAR_LIST.contains(expSymbolName)) {
      return genVarExp(expNode);

    } else if (SymbolNames.GlobalData.VAR_LIST.contains(expSymbolName)) {
      var varNode = (BhNode) expNode;
      return common.genVarName(varNode.getOriginal());

    } else if (SymbolNames.Literal.LIST.contains(expSymbolName)) {
      return genLiteral(code, expNode, nestLevel, option);

    } else if (SymbolNames.Literal.LITERAL_EXP_LIST.contains(expSymbolName)) {
      return genExpression(
          code, expNode.findSymbolInDescendants("*", "Literal", "*"), nestLevel, option);

    } else if (SymbolNames.PreDefFunc.PREDEF_FUNC_CALL_EXP_LIST.contains(expSymbolName)) {
      return genPreDefFuncCallExp(code, expNode, nestLevel, option, true);

    } else if (SymbolNames.ConstantValue.LIST.contains(expSymbolName)) {
      return genIdentifierExp(code, expNode, nestLevel, option);
    }
    return null;
  }

  /**
   * 二項演算式を作成する.
   *
   * @param code 途中式の格納先
   * @param binaryExpNode 二項式のノード
   * @param nestLevel ソースコードのネストレベル
   * @param option コンパイルオプション
   * @return 式もしくは式の評価結果を格納した変数
   */
  private String genBinaryExp(
      StringBuilder code,
      SyntaxSymbol binaryExpNode,
      int nestLevel,
      CompileOption option) {

    SyntaxSymbol leftExp =
        binaryExpNode.findSymbolInDescendants("*",  SymbolNames.BinaryExp.LEFT_EXP, "*");
    String leftExpCode = genExpression(code, leftExp, nestLevel, option);

    SyntaxSymbol rightExp =
        binaryExpNode.findSymbolInDescendants("*", SymbolNames.BinaryExp.RIGHT_EXP, "*");
    String rightExpCode = genExpression(code, rightExp, nestLevel, option);

    TextNode operator =
        (TextNode) binaryExpNode.findSymbolInDescendants("*", SymbolNames.BinaryExp.OPERATOR, "*");
    String operatorCode = SymbolNames.BinaryExp.OPERATOR_MAP.get(operator.getText());    
    if (leftExp == null || rightExp == null) {
      return null;
    }

    String tmpVar = common.genVarName(binaryExpNode);
    code.append(common.indent(nestLevel))
        .append(Keywords.Js._const_)
        .append(tmpVar)
        .append(" = ")
        .append(leftExpCode)
        .append(operatorCode)
        .append(rightExpCode)
        .append(";" + Keywords.newLine);

    String tmpVarResult = tmpVar;
    if (option.keepRealNumber) {
      if (SymbolNames.BinaryExp.ARITH_EXCEPTION_EXP.contains(binaryExpNode.getSymbolName())) {
        tmpVarResult = "_" + tmpVar;
        code.append(common.indent(nestLevel))
            .append(Keywords.Js._const_)
            .append(tmpVarResult)
            .append(" = ")
            .append("(")
            .append(common.genFuncCallCode(ScriptIdentifiers.JsFuncs.IS_FINITE, tmpVar))
            .append(") ? ")
            .append(tmpVar)
            .append(" : ")
            .append(leftExpCode)
            .append(";" + Keywords.newLine);
      }
    }
    return tmpVarResult;
  }

  /**
   * 単項演算式を作成する.
   *
   * @param code 途中式の格納先
   * @param unaryExpNode 単項式のノード
   * @param nestLevel ソースコードのネストレベル
   * @param option コンパイルオプション
   * @return 式もしくは式の評価結果を格納した変数
   */
  private String genUnaryExp(
      StringBuilder code,
      SyntaxSymbol unaryExpNode,
      int nestLevel,
      CompileOption option) {

    SyntaxSymbol primaryExp =
        unaryExpNode.findSymbolInDescendants("*",  SymbolNames.UnaryExp.PRIMARY_EXP, "*");
    String primaryExpCode = genExpression(code, primaryExp, nestLevel, option);
    String operatorCode = SymbolNames.UnaryExp.OPERATOR_MAP.get(unaryExpNode.getSymbolName());

    if (primaryExp == null) {
      return null;
    }
    String tmpVar = common.genVarName(unaryExpNode);
    code.append(common.indent(nestLevel))
        .append(Keywords.Js._const_)
        .append(tmpVar)
        .append(" = ")
        .append(operatorCode)
        .append(primaryExpCode)
        .append(";" + Keywords.newLine);
    return tmpVar;
  }

  /**
   * リテラルのコードを作成する.
   *
   * @param code 生成したコードの格納先
   * @param literalNode リテラルのノード
   * @param nestLevel ソースコードのネストレベル
   * @param option コンパイルオプション
   * @return リテラル式
   */
  private String genLiteral(
      StringBuilder code,
      SyntaxSymbol literalNode,
      int nestLevel,
      CompileOption option) {

    if (SymbolNames.Literal.ARRAY_TYPES.contains(literalNode.getSymbolName())) {
      return "([])";  //空リスト
    }
    String inputText = "";
    if (literalNode instanceof TextNode textNode) {
      inputText = textNode.getText();
    }
    switch (literalNode.getSymbolName()) {
      case SymbolNames.Literal.NUM_LITERAL:
      case SymbolNames.Literal.BOOL_LITERAL:
      case SymbolNames.Literal.STR_CHAIN_LINK_VOID:
        return "(" + inputText + ")";

      case SymbolNames.Literal.STR_LITERAL:
        return "(" + common.toJsString(inputText) + ")";

      case SymbolNames.Literal.SOUND_LITERAL_VOID:
      case SymbolNames.Literal.FREQ_SOUND_LITERAL:
      case SymbolNames.Literal.SCALE_SOUND_LITERAL:
        return genSoundLiteralExp(code, literalNode, nestLevel, option);

      case SymbolNames.Literal.COLOR_LITERAL:
        return genColorLiteralExp(code, literalNode, nestLevel, option);

      default:
        throw new AssertionError("invalid literal " + literalNode.getSymbolName());
    }
  }

  /**
   * 定義済み関数の呼び出し式を作成する.
   *
   * @param code 関数呼び出し式の格納先
   * @param funcCallNode 関数呼び出し式のノード
   * @param nestLevel ソースコードのネストレベル
   * @param option コンパイルオプション
   * @param storeRetVal 戻り値を変数に格納するコードを出力する場合true.
   * @return 式もしくは式の評価結果を格納した変数. storeRetVal が false の場合は null.
   */
  public String genPreDefFuncCallExp(
      StringBuilder code,
      SyntaxSymbol funcCallNode,
      int nestLevel,
      CompileOption option,
      boolean storeRetVal) {

    List<String> argList = genPreDefFuncArgs(code, funcCallNode, false, nestLevel, option);
    List<String> outArgList = genPreDefFuncArgs(code, funcCallNode, true, nestLevel, option);
    argList.addAll(outArgList);
    FuncId funcIdentifier = createFuncId(funcCallNode);
    String funcName = SymbolNames.PreDefFunc.PREDEF_FUNC_NAME_MAP.get(funcIdentifier);
    String retValName = null;
    if (storeRetVal) {
      retValName = common.genVarName(funcCallNode);
      code.append(common.indent(nestLevel))
          .append(Keywords.Js._const_)
          .append(retValName)
          .append(";" + Keywords.newLine);
    }
    // コールスタック push
    if (option.isDebug) {
      code.append(common.indent(nestLevel))
          .append(common.genPushToCallStackCode(funcCallNode))
          .append(";" + Keywords.newLine);
    }
    String[] argArray = argList.toArray(new String[argList.size()]);
    String funcCallCode;
    // 恒等写像は最初の引数を結果の変数に代入するだけ
    if (funcName.equals(ScriptIdentifiers.Funcs.IDENTITY)) {
      funcCallCode = argArray[0];
    } else {
      funcCallCode = common.genFuncPrototypeCallCode(funcName, Keywords.Js._this, argArray);
    }
    code.append(common.indent(nestLevel));
    if (storeRetVal) {
      code.append(retValName).append(" = ");
    }
    code.append(funcCallCode).append(";").append(Keywords.newLine);
    // コールスタック pop
    if (option.isDebug) {
      code.append(common.indent(nestLevel))
          .append(common.genPopFromCallStackCode())
          .append(";" + Keywords.newLine);
    }
    return retValName;
  }

  /**
   * 定義済み関数の実引数部分を作成する.
   *
   * @param code 関数呼び出し式の格納先
   * @param funcCallNode 関数呼び出し式のノード
   * @param outArg 出力引数を作成する場合 true. 入力引数を作成する場合 false.
   * @param nestLevel ソースコードのネストレベル
   * @param option コンパイルオプション
   * @return 実引数を格納したリスト
   */
  private List<String> genPreDefFuncArgs(
      StringBuilder code,
      SyntaxSymbol funcCallNode,
      boolean outArg,
      int nestLevel,
      CompileOption option) {

    var argList = new ArrayList<String>();
    int idArg = 0;
    while (true) {
      String argCnctrName = outArg ? SymbolNames.PreDefFunc.OUT_ARG : SymbolNames.PreDefFunc.ARG;
      argCnctrName += idArg;
      SyntaxSymbol argExp = funcCallNode.findSymbolInDescendants("*", argCnctrName, "*");
      if (argExp == null) {
        break;
      }
      if (outArg) {
        argList.add(genOutArg(code, argExp, nestLevel, option));
      } else {
        argList.add(genExpression(code, argExp, nestLevel, option));
      }
      ++idArg;
    }
    return argList;
  }

  /**
   * 関数呼び出しノードから関数IDを作成する.
   *
   * @return 関数ID
   */
  private FuncId createFuncId(SyntaxSymbol funcCallNode) {
    //呼び出しオプションを探す
    int idOption = 0;
    List<String> funcIdentifier = new ArrayList<>(Arrays.asList(funcCallNode.getSymbolName()));
    while (true) {
      String optionCnctrName = SymbolNames.PreDefFunc.OPTION + idOption;
      SyntaxSymbol optionExp = funcCallNode.findSymbolInDescendants("*", optionCnctrName, "*");
      if (optionExp == null) {
        break;
      }
      if (optionExp instanceof TextNode textNode) {
        funcIdentifier.add(textNode.getText());
      }
      ++idOption;
    }
    return FuncId.create(funcIdentifier.toArray(new String[funcIdentifier.size()]));
  }

  /**
   * ユーザー定義関数の呼び出し式を生成する.
   *
   * @param code 生成したコードの格納先
   * @param funcCallNode 関数呼び出しのノード
   * @param nestLevel ソースコードのネストレベル
   * @param option コンパイルオプション
   * @param storeRetVal 戻り値を変数に格納するコードを出力する場合true.
   * @return 式もしくは式の評価結果を格納した変数. storeRetVal が false の場合は null.
   */
  public String genUserDefFuncCallExp(
      StringBuilder code,
      SyntaxSymbol funcCallNode,
      int nestLevel,
      CompileOption option,
      boolean storeRetVal) {

    SyntaxSymbol arg = funcCallNode.findSymbolInDescendants("*", SymbolNames.UserDefFunc.ARG, "*");
    SyntaxSymbol outArg =
        funcCallNode.findSymbolInDescendants("*", SymbolNames.UserDefFunc.OUT_ARG, "*");
    List<String> argList = genArgList(code, arg, false, nestLevel, option);
    List<String> outArgList = genArgList(code, outArg, true, nestLevel, option);

    String retValName = null;
    if (storeRetVal) {
      retValName = common.genVarName(funcCallNode);
      code.append(common.indent(nestLevel))
          .append(Keywords.Js._const_)
          .append(retValName)
          .append(";" + Keywords.newLine);
    }
    // コールスタック push
    if (option.isDebug) {
      code.append(common.indent(nestLevel))
          .append(common.genPushToCallStackCode(funcCallNode))
          .append(";" + Keywords.newLine);
    }
    argList.addAll(outArgList);
    String funcName = common.genFuncName(((BhNode) funcCallNode).getOriginal());
    String[] argArray = argList.toArray(new String[argList.size()]);
    String funcCallCode = common.genFuncPrototypeCallCode(funcName, Keywords.Js._this, argArray);

    code.append(common.indent(nestLevel));
    if (storeRetVal) {
      code.append(retValName + " = ");
    }
    code.append(funcCallCode)
        .append(";" + Keywords.newLine);
    // コールスタック pop
    if (option.isDebug) {
      code.append(common.indent(nestLevel))
          .append(common.genPopFromCallStackCode())
          .append(";" + Keywords.newLine);
    }
    return retValName;
  }

  /**
   * ユーザ定義関数の実引数リストを作成する.
   *
   * @param code 生成したコードの格納先
   * @param argNode 引数ノード
   * @param assignToOutParams 出力引数に代入する実引数リストを作成する場合 true
   * @param nestLevel ソースコードのネストレベル
   * @param option コンパイルオプション
   * @return 実引数を格納したリスト
   */
  private LinkedList<String> genArgList(
      StringBuilder code,
      SyntaxSymbol argNode,
      boolean assignToOutParams,
      int nestLevel,
      CompileOption option) {

    LinkedList<String> argList;
    SyntaxSymbol nextArg =
        argNode.findSymbolInDescendants("*", SymbolNames.UserDefFunc.NEXT_ARG, "*");
    if (nextArg != null && !nextArg.getSymbolName().equals(SymbolNames.UserDefFunc.ARG_VOID)) {
      argList = genArgList(code, nextArg, assignToOutParams, nestLevel, option);
    } else {
      argList = new LinkedList<>();
    }
    SyntaxSymbol argument = argNode.findSymbolInDescendants("*", SymbolNames.UserDefFunc.ARG, "*");
    if (argument != null) {
      if (assignToOutParams) {
        argList.addFirst(genOutArg(code, argument, nestLevel, option));
      } else {
        argList.addFirst(genExpression(code, argument, nestLevel, option));
      }
    }
    return argList;
  }

  /**
   * 出力変数を作成する.
   *
   * @param code 生成したコードの格納先
   * @param varNode 変数ノード
   * @param nestLevel ソースコードのネストレベル
   * @param option コンパイルオプション
   * @return 出力変数
   */
  private String genOutArg(
      StringBuilder code,
      SyntaxSymbol varNode,
      int nestLevel,
      CompileOption option) {

    if (SymbolNames.VarDecl.VAR_LIST.contains(varNode.getSymbolName())) {
      BhNode varDecl = ((BhNode) varNode).getOriginal();
      if (common.isOutputParam(varDecl)) {
        return common.genVarName(varDecl); // out -> out
      } else {
        return common.genOutArgName(varDecl); // in -> out
      }
    } else if (SymbolNames.VarDecl.VAR_VOID_LIST.contains(varNode.getSymbolName())) {
      //出力引数に変数指定がなかった場合
      List<String> vars = varDeclCodeGen.genVarDeclAndOutVarArg(
          varNode,
          SymbolNames.VarDecl.INIT_VAL_MAP.get(varNode.getSymbolName()),
          code,
          nestLevel,
          option);
      return vars.get(1);
    }
    throw new IllegalStateException(Util.INSTANCE.getCurrentMethodName());
  }

  /**
   * 音リテラルのコードを生成する.
   *
   * @param code 生成したコードの格納先
   * @param soundLiteralNode 音リテラルノード
   * @param nestLevel ソースコードのネストレベル
   * @param option コンパイルオプション
   * @return 音リテラルを格納した変数.
   * */
  private String genSoundLiteralExp(
      StringBuilder code,
      SyntaxSymbol soundLiteralNode,
      int nestLevel,
      CompileOption option) {

    if (soundLiteralNode.getSymbolName().equals(SymbolNames.Literal.SOUND_LITERAL_VOID)) {
      String soundVar = common.genVarName(soundLiteralNode);
      String rightExp = common.genFuncCallCode(ScriptIdentifiers.Funcs.CREATE_SOUND, "0", "0");
      code.append(common.indent(nestLevel))
        .append(Keywords.Js._const_).append(soundVar).append(" = ").append(rightExp)
        .append(";").append(Keywords.newLine);
      return soundVar;
    }

    if (soundLiteralNode.getSymbolName().equals(SymbolNames.Literal.FREQ_SOUND_LITERAL)) {
      return genFreqSoundLiteralExp(code, soundLiteralNode, nestLevel, option);
    }
    if (soundLiteralNode.getSymbolName().equals(SymbolNames.Literal.SCALE_SOUND_LITERAL)) {
      return genScaleSoundLiteralExp(code, soundLiteralNode, nestLevel, option);
    }
    throw new AssertionError("invalid sound literal " + soundLiteralNode.getSymbolName());
  }

  /**
   * 周波数指定の音リテラルのコードを生成する.
   *
   * @param code 生成したコードの格納先
   * @param freqSoundLiteralNode 周波数指定の音リテラルノード
   * @param nestLevel ソースコードのネストレベル
   * @param option コンパイルオプション
   * @return 音リテラルを格納した変数.
   * */
  private String genFreqSoundLiteralExp(
      StringBuilder code,
      SyntaxSymbol freqSoundLiteralNode,
      int nestLevel,
      CompileOption option) {

    SyntaxSymbol durationNode =
        freqSoundLiteralNode.findSymbolInDescendants("*", SymbolNames.Literal.Sound.DURATION, "*");
    SyntaxSymbol frequencyNode =
        freqSoundLiteralNode.findSymbolInDescendants("*", SymbolNames.Literal.Sound.FREQUENCY, "*");
    String duration = genExpression(code, durationNode, nestLevel, option);
    String frequency = genExpression(code, frequencyNode, nestLevel, option);
    // 音オブジェクト作成
    String soundVar = common.genVarName(freqSoundLiteralNode);
    String rightExp =
        common.genFuncCallCode(ScriptIdentifiers.Funcs.CREATE_SOUND, frequency, duration);
    code.append(common.indent(nestLevel))
        .append(Keywords.Js._const_)
        .append(soundVar)
        .append(" = ")
        .append(rightExp)
        .append(";" + Keywords.newLine);
    return soundVar;
  }

  /**
   * 音階の音を指定する音リテラルのコードを生成する.
   *
   * @param code 生成したコードの格納先
   * @param scaleSoundLiteralNode 音階の音を指定する音リテラルノード
   * @param nestLevel ソースコードのネストレベル
   * @param option コンパイルオプション
   * @return 音リテラルを格納した変数.
   * */
  private String genScaleSoundLiteralExp(
      StringBuilder code,
      SyntaxSymbol scaleSoundLiteralNode,
      int nestLevel,
      CompileOption option) {
        
    SyntaxSymbol durationNode = scaleSoundLiteralNode.findSymbolInDescendants(
        "*", SymbolNames.Literal.Sound.DURATION, "*");
    SyntaxSymbol octaveNode = scaleSoundLiteralNode.findSymbolInDescendants(
        "*", SymbolNames.Literal.Sound.OCTAVE, "*");
    SyntaxSymbol scaleSoundNode = scaleSoundLiteralNode.findSymbolInDescendants(
        "*", SymbolNames.Literal.Sound.SCALE_SOUND, "*");

    // 音階の音から周波数を計算する
    final String duration = genExpression(code, durationNode, nestLevel, option);
    String octave = genExpression(code, octaveNode, nestLevel, option);
    String scaleSound = genExpression(code, scaleSoundNode, nestLevel, option);
    octave = octave.replaceAll("[^\\d\\-]", "");
    scaleSound = scaleSound.replaceAll("[^\\d\\-]", "");
    double frequency =
        440 * Math.pow(2, (Double.parseDouble(octave) + Double.parseDouble(scaleSound)) / 12);
    // frequency = Math.round(frequency);

    // 音オブジェクト作成
    String soundVar = common.genVarName(scaleSoundLiteralNode);
    String rightExp = common.genFuncCallCode(
        ScriptIdentifiers.Funcs.CREATE_SOUND, "%.3f".formatted(frequency), duration);
    code.append(common.indent(nestLevel))
        .append(Keywords.Js._const_)
        .append(soundVar)
        .append(" = ")
        .append(rightExp)
        .append(";" + Keywords.newLine);
    return soundVar;
  }

  /**
   * 色リテラルのコードを生成する.
   *
   * @param code 生成したコードの格納先
   * @param colorLiteralNode 色リテラルノード
   * @param nestLevel ソースコードのネストレベル
   * @param option コンパイルオプション
   * @return 色リテラルを格納した変数.
   */
  private String genColorLiteralExp(
      StringBuilder code,
      SyntaxSymbol colorLiteralNode,
      int nestLevel,
      CompileOption option) {

    String colorName = "'" + ((TextNode) colorLiteralNode).getText() + "'";
    String colorVar = common.genVarName(colorLiteralNode);
    String rightExp = common.genFuncCallCode(
        ScriptIdentifiers.Funcs.CREATE_COLOR_FROM_NAME, colorName);
    code.append(common.indent(nestLevel))
        .append(Keywords.Js._const_)
        .append(colorVar)
        .append(" = ")
        .append(rightExp)
        .append(";" + Keywords.newLine);
    return colorVar;
  }

  /**
   * 識別子のコードを作成する.
   *
   * @param code 生成したコードの格納先
   * @param identifierNode 識別子のノード
   * @param nestLevel ソースコードのネストレベル
   * @param option コンパイルオプション
   * @return 識別子の文字列
   */
  private String genIdentifierExp(
      StringBuilder code,
      SyntaxSymbol identifierNode,
      int nestLevel,
      CompileOption option) {

    return ((TextNode) identifierNode).getText();
  }

  /** 変数ノードから式を生成する. */
  private String genVarExp(SyntaxSymbol expNode) {
    var varNode = (BhNode) expNode;
    if (common.isOutputParam(varNode.getOriginal())) {
      return "(" + common.genGetOutputParamValCode(varNode.getOriginal()) + ")";
    } else {
      return common.genVarName(varNode.getOriginal());
    }
  }
}
