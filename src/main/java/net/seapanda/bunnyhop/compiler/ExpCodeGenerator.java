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
import net.seapanda.bunnyhop.node.model.BhNode;
import net.seapanda.bunnyhop.node.model.TextNode;
import net.seapanda.bunnyhop.node.model.syntaxsymbol.SyntaxSymbol;
import net.seapanda.bunnyhop.utility.Utility;

/**
 * 式のコード生成を行うクラス.
 *
 * @author K.Koike
 */
class ExpCodeGenerator {

  private final CommonCodeGenerator common;
  private final VarDeclCodeGenerator varDeclCodeGen;

  ExpCodeGenerator(CommonCodeGenerator common, VarDeclCodeGenerator varDeclCodeGen) {
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
   * @return 式もしくは式の評価結果を格納した変数. {@code expNode} に該当する式ノードが見つからなかった場合 null.
   */
  String genExpression(
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

    } else if (SymbolNames.Literal.EXP_LIST.contains(expSymbolName)) {
      return genExpression(
          code, expNode.findDescendantOf("*", "Literal", "*"), nestLevel, option);

    } else if (SymbolNames.PreDefFunc.EXP_LIST.contains(expSymbolName)) {
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
  String genBinaryExp(
      StringBuilder code,
      SyntaxSymbol binaryExpNode,
      int nestLevel,
      CompileOption option) {
    if (SymbolNames.BinaryExp.NONLOGICAL_LIST.contains(binaryExpNode.getSymbolName())) {
      return genNonlogicalBinaryExp(code, binaryExpNode, nestLevel, option);
    } else if (SymbolNames.BinaryExp.LOGICAL_LIST.contains(binaryExpNode.getSymbolName())) {
      return genLogicalBinaryExp(code, binaryExpNode, nestLevel, option);
    }
    throw new AssertionError(
        "Unknown binary expression.  (%s)".formatted(binaryExpNode.getSymbolName()));
  }

  /**
   * 非論理二項演算式を作成する.
   *
   * @param code 途中式の格納先
   * @param binaryExpNode 二項式のノード
   * @param nestLevel ソースコードのネストレベル
   * @param option コンパイルオプション
   * @return 式もしくは式の評価結果を格納した変数
   */
  String genNonlogicalBinaryExp(
      StringBuilder code,
      SyntaxSymbol binaryExpNode,
      int nestLevel,
      CompileOption option) {
    SyntaxSymbol leftExp =
        binaryExpNode.findDescendantOf("*",  SymbolNames.BinaryExp.LEFT_EXP, "*");
    String leftExpCode = genExpression(code, leftExp, nestLevel, option);

    SyntaxSymbol rightExp =
        binaryExpNode.findDescendantOf("*", SymbolNames.BinaryExp.RIGHT_EXP, "*");
    String rightExpCode = genExpression(code, rightExp, nestLevel, option);

    TextNode operator =
        (TextNode) binaryExpNode.findDescendantOf("*", SymbolNames.BinaryExp.OPERATOR, "*");
    String operatorCode = SymbolNames.BinaryExp.OPERATOR_MAP.get(operator.getText());    

    String tmpVar = common.genVarName(binaryExpNode);
    common.genSetNextNodeInstId(code, binaryExpNode.getInstanceId(), nestLevel, option);
    genConditionalWait(code, binaryExpNode, nestLevel, option);
    code.append(common.indent(nestLevel))
        .append(Keywords.Js._const_)
        .append(tmpVar)
        .append(" = ")
        .append(leftExpCode)
        .append(operatorCode)
        .append(rightExpCode)
        .append(";" + Keywords.newLine);

    return tmpVar;
  }

  /**
   * 論理二項演算式を作成する.
   * 短絡評価をするため, 非論理二項演算式と分ける.
   *
   * @param code 途中式の格納先
   * @param binaryExpNode 二項式のノード
   * @param nestLevel ソースコードのネストレベル
   * @param option コンパイルオプション
   * @return 式もしくは式の評価結果を格納した変数
   */
  String genLogicalBinaryExp(
      StringBuilder code,
      SyntaxSymbol binaryExpNode,
      int nestLevel,
      CompileOption option) {
    SyntaxSymbol leftExp =
        binaryExpNode.findDescendantOf("*",  SymbolNames.BinaryExp.LEFT_EXP, "*");
    String leftExpCode = genExpression(code, leftExp, nestLevel, option);
    String tmpVar = common.genVarName(binaryExpNode);
    TextNode operator =
        (TextNode) binaryExpNode.findDescendantOf("*", SymbolNames.BinaryExp.OPERATOR, "*");
    String cond =
        operator.getText().equals(SymbolNames.BinaryExp.OP_AND) ? tmpVar : ("!" + tmpVar);

    code.append(common.indent(nestLevel))
        .append(Keywords.Js._let_) // 再代入するので const 不可
        .append(tmpVar)
        .append(" = ")
        .append(leftExpCode)
        .append(";" + Keywords.newLine)
        .append(common.indent(nestLevel))
        .append(Keywords.Js._if_)
        .append("(")
        .append(cond)
        .append(") {" + Keywords.newLine);

    SyntaxSymbol rightExp =
        binaryExpNode.findDescendantOf("*", SymbolNames.BinaryExp.RIGHT_EXP, "*");
    String rightExpCode = genExpression(code, rightExp, nestLevel + 1, option);
    common.genSetNextNodeInstId(code, binaryExpNode.getInstanceId(), nestLevel, option);
    genConditionalWait(code, binaryExpNode, nestLevel, option);
    code.append(common.indent(nestLevel + 1))
        .append(tmpVar)
        .append(" = ")
        .append(tmpVar)
        .append(SymbolNames.BinaryExp.OPERATOR_MAP.get(operator.getText()))
        .append(rightExpCode)
        .append(";" + Keywords.newLine)
        .append(common.indent(nestLevel))
        .append("}" + Keywords.newLine);

    return tmpVar;
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
  String genUnaryExp(
      StringBuilder code,
      SyntaxSymbol unaryExpNode,
      int nestLevel,
      CompileOption option) {
    SyntaxSymbol primaryExp =
        unaryExpNode.findDescendantOf("*",  SymbolNames.UnaryExp.PRIMARY_EXP, "*");
    String primaryExpCode = genExpression(code, primaryExp, nestLevel, option);
    String operatorCode = SymbolNames.UnaryExp.OPERATOR_MAP.get(unaryExpNode.getSymbolName());
    String tmpVar = common.genVarName(unaryExpNode);
    common.genSetNextNodeInstId(code, unaryExpNode.getInstanceId(), nestLevel, option);
    genConditionalWait(code, unaryExpNode, nestLevel, option);
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
        return "(" + common.toJsNumber(inputText) + ")";

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
        throw new AssertionError("Invalid literal " + literalNode.getSymbolName());
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
  String genPreDefFuncCallExp(
      StringBuilder code,
      SyntaxSymbol funcCallNode,
      int nestLevel,
      CompileOption option,
      boolean storeRetVal) {
    List<String> argList = genPreDefFuncArgs(code, funcCallNode, false, nestLevel, option);
    List<String> outArgList = genPreDefFuncArgs(code, funcCallNode, true, nestLevel, option);
    argList.addAll(outArgList);
    String retValName = null;
    if (storeRetVal) {
      retValName = common.genVarName(funcCallNode);
      code.append(common.indent(nestLevel))
          .append(Keywords.Js._const_)
          .append(retValName)
          .append(";" + Keywords.newLine);
    }
    common.genSetNextNodeInstId(code, funcCallNode.getInstanceId(), nestLevel, option);
    genConditionalWait(code, funcCallNode, nestLevel, option);
    String[] argArray = argList.toArray(new String[argList.size()]);
    String funcCallCode;
    // 恒等写像は最初の引数を結果の変数に代入するだけ
    String funcName = SymbolNames.PreDefFunc.NAME_MAP.get(createFuncId(funcCallNode));
    if (funcName.equals(ScriptIdentifiers.Funcs.IDENTITY)) {
      funcCallCode = argArray[0];
    } else {
      funcCallCode = common.genFuncCall(funcName, argArray);
    }
    code.append(common.indent(nestLevel));
    if (storeRetVal) {
      code.append(retValName).append(" = ");
    }
    code.append(funcCallCode).append(";").append(Keywords.newLine);
    return retValName;
  }

  /**
   * 定義済み関数を呼ぶコードの実引数部分を作成する.
   *
   * @param code 関数呼び出し式の格納先
   * @param funcCallNode 関数呼び出し式のノード
   * @param outArg 出力引数を作成する場合 true. 入力引数を作成する場合 false.
   * @param nestLevel ソースコードのネストレベル
   * @param option コンパイルオプション
   * @return 実引数を格納したリスト
   */
  List<String> genPreDefFuncArgs(
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
      SyntaxSymbol argExp = funcCallNode.findDescendantOf("*", argCnctrName, "*");
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
  FuncId createFuncId(SyntaxSymbol funcCallNode) {
    //呼び出しオプションを探す
    int idOption = 0;
    List<String> funcIdentifier = new ArrayList<>(Arrays.asList(funcCallNode.getSymbolName()));
    while (true) {
      String optionCnctrName = SymbolNames.PreDefFunc.OPTION + idOption;
      SyntaxSymbol optionExp = funcCallNode.findDescendantOf("*", optionCnctrName, "*");
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
  String genUserDefFuncCallExp(
      StringBuilder code,
      SyntaxSymbol funcCallNode,
      int nestLevel,
      CompileOption option,
      boolean storeRetVal) {
    SyntaxSymbol arg = funcCallNode.findDescendantOf("*", SymbolNames.UserDefFunc.ARG, "*");
    SyntaxSymbol outArg = funcCallNode.findDescendantOf("*", SymbolNames.UserDefFunc.OUT_ARG, "*");
    List<String> argList = genArgList(code, arg, false, nestLevel, option);
    List<String> outArgList = genArgList(code, outArg, true, nestLevel, option);
    argList.addAll(outArgList);
    argList.addFirst(ScriptIdentifiers.Vars.THREAD_CONTEXT);
    String funcName = common.genFuncName(((BhNode) funcCallNode).getOriginal());
    String[] argArray = argList.toArray(new String[argList.size()]);
    final String funcCallCode = common.genFuncCall(funcName, argArray);

    String retValName = null;
    if (storeRetVal) {
      retValName = common.genVarName(funcCallNode);
      code.append(common.indent(nestLevel))
          .append(Keywords.Js._const_)
          .append(retValName)
          .append(";" + Keywords.newLine);
    }
    common.genSetNextNodeInstId(code, funcCallNode.getInstanceId(), nestLevel, option);
    genConditionalWait(code, funcCallNode, nestLevel, option);

    code.append(common.indent(nestLevel));
    if (storeRetVal) {
      code.append(retValName + " = ");
    }
    code.append(funcCallCode)
        .append(";" + Keywords.newLine);
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
        argNode.findDescendantOf("*", SymbolNames.UserDefFunc.NEXT_ARG, "*");
    if (nextArg != null && !nextArg.getSymbolName().equals(SymbolNames.UserDefFunc.ARG_VOID)) {
      argList = genArgList(code, nextArg, assignToOutParams, nestLevel, option);
    } else {
      argList = new LinkedList<>();
    }
    SyntaxSymbol argument = argNode.findDescendantOf("*", SymbolNames.UserDefFunc.ARG, "*");
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
   * 出力実引数を作成する.
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
        return common.genVarAccessorName(varDecl); // in -> out
      }
    } else if (SymbolNames.VarDecl.VAR_VOID_LIST.contains(varNode.getSymbolName())) {
      // 出力引数に変数指定がなかった場合
      List<String> vars = varDeclCodeGen.genVarDeclAndAccessor(
          varNode,
          SymbolNames.VarDecl.INIT_VAL_MAP.get(varNode.getSymbolName()),
          code,
          nestLevel,
          option);
      return vars.get(1);
    }
    throw new IllegalStateException(Utility.getCurrentMethodName());
  }

  /**
   * 音リテラルのコードを生成する.
   *
   * @param code 生成したコードの格納先
   * @param soundLiteralNode 音リテラルノード
   * @param nestLevel ソースコードのネストレベル
   * @param option コンパイルオプション
   * @return 音リテラルを格納した変数.
   */
  private String genSoundLiteralExp(
      StringBuilder code,
      SyntaxSymbol soundLiteralNode,
      int nestLevel,
      CompileOption option) {
    if (soundLiteralNode.getSymbolName().equals(SymbolNames.Literal.SOUND_LITERAL_VOID)) {
      String soundVar = common.genVarName(soundLiteralNode);
      String rightExp = ScriptIdentifiers.Vars.NIL_SOUND;
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
    throw new AssertionError("Invalid sound literal " + soundLiteralNode.getSymbolName());
  }

  /**
   * 周波数指定の音リテラルのコードを生成する.
   *
   * @param code 生成したコードの格納先
   * @param freqSoundLiteralNode 周波数指定の音リテラルノード
   * @param nestLevel ソースコードのネストレベル
   * @param option コンパイルオプション
   * @return 音リテラルを格納した変数.
   */
  private String genFreqSoundLiteralExp(
      StringBuilder code,
      SyntaxSymbol freqSoundLiteralNode,
      int nestLevel,
      CompileOption option) {
    SyntaxSymbol volumeNode = 
        freqSoundLiteralNode.findDescendantOf("*", SymbolNames.Literal.Sound.VOLUME, "*");
    SyntaxSymbol durationNode =
        freqSoundLiteralNode.findDescendantOf("*", SymbolNames.Literal.Sound.DURATION, "*");
    SyntaxSymbol frequencyNode =
        freqSoundLiteralNode.findDescendantOf("*", SymbolNames.Literal.Sound.FREQUENCY, "*");
    String volume = genExpression(code, volumeNode, nestLevel, option);
    String duration = genExpression(code, durationNode, nestLevel, option);
    String frequency = genExpression(code, frequencyNode, nestLevel, option);
    // 音オブジェクト作成
    String soundVar = common.genVarName(freqSoundLiteralNode);
    String rightExp = common.genFuncCall(
        ScriptIdentifiers.Funcs.CREATE_SOUND, volume, frequency, duration);
    common.genSetNextNodeInstId(code, freqSoundLiteralNode.getInstanceId(), nestLevel, option);
    genConditionalWait(code, freqSoundLiteralNode, nestLevel, option);
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
   */
  private String genScaleSoundLiteralExp(
      StringBuilder code,
      SyntaxSymbol scaleSoundLiteralNode,
      int nestLevel,
      CompileOption option) {
    SyntaxSymbol volumeNode = scaleSoundLiteralNode.findDescendantOf(
        "*", SymbolNames.Literal.Sound.VOLUME, "*");
    SyntaxSymbol durationNode = scaleSoundLiteralNode.findDescendantOf(
        "*", SymbolNames.Literal.Sound.DURATION, "*");
    SyntaxSymbol octaveNode = scaleSoundLiteralNode.findDescendantOf(
        "*", SymbolNames.Literal.Sound.OCTAVE, "*");
    SyntaxSymbol scaleSoundNode = scaleSoundLiteralNode.findDescendantOf(
        "*", SymbolNames.Literal.Sound.SCALE_SOUND, "*");

    // 音階の音から周波数を計算する
    final String volume = genExpression(code, volumeNode, nestLevel, option);
    final String duration = genExpression(code, durationNode, nestLevel, option);
    String octave = genExpression(code, octaveNode, nestLevel, option);
    String scaleSound = genExpression(code, scaleSoundNode, nestLevel, option);
    octave = octave.replaceAll("[^\\d\\-]", "");
    scaleSound = scaleSound.replaceAll("[^\\d\\-]", "");
    double frequency =
        440 * Math.pow(2, (Double.parseDouble(octave) + Double.parseDouble(scaleSound)) / 12);
    // 音オブジェクト作成
    String soundVar = common.genVarName(scaleSoundLiteralNode);
    String rightExp = common.genFuncCall(
        ScriptIdentifiers.Funcs.CREATE_SOUND, volume, "(%.3f)".formatted(frequency), duration);
    common.genSetNextNodeInstId(code, scaleSoundLiteralNode.getInstanceId(), nestLevel, option);
    genConditionalWait(code, scaleSoundLiteralNode, nestLevel, option);
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
    String rightExp = common.genFuncCall(
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
      return "(" + common.genGetOutputParamVal(varNode.getOriginal()) + ")";
    } else {
      return common.genVarName(varNode.getOriginal());
    }
  }

  /**
   * {@code symbol} が一時停止可能なノードである場合, 一時停止するコードを生成する.
   *
   * @param code 生成したコードの格納先
   * @param symbol このシンボルが一時停止可能なノードである場合, 一時停止するコードを生成する.
   * @param nestLevel ソースコードのネストレベル
   * @param option コンパイルオプション
   */
  private void genConditionalWait(
      StringBuilder code,
      SyntaxSymbol symbol,
      int nestLevel,
      CompileOption option) {
    if (symbol instanceof BhNode node) {
      if (node.isBreakpointGroupLeader()) {
        common.genConditionalWait(node.getInstanceId(), code, nestLevel, option);
      }
    }
  }
}
