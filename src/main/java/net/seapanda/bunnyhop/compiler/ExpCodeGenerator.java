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
   * @param expNode 式のノード
   * @param code 途中式の格納先
   * @param nestLevel ソースコードのネストレベル
   * @param option コンパイルオプション
   * @return 式もしくは式の評価結果を格納した変数. {@code expNode} に該当する式ノードが見つからなかった場合 null.
   */
  String genExpression(
      SyntaxSymbol expNode,
      StringBuilder code,
      int nestLevel,
      CompileOption option) {
    String expSymbolName = expNode.getSymbolName();
    if (SymbolNames.BinaryExp.LIST.contains(expSymbolName)) {
      return genBinaryExp(expNode, code, nestLevel, option);

    } else if (SymbolNames.UnaryExp.LIST.contains(expSymbolName)) {
      return genUnaryExp(expNode, code, nestLevel, option);

    } else if (SymbolNames.VarDecl.VAR_LIST.contains(expSymbolName)) {
      return genVarExp(expNode);

    } else if (SymbolNames.GlobalData.VAR_LIST.contains(expSymbolName)) {
      var varNode = (BhNode) expNode;
      return common.genVarName(varNode.getOriginal());

    } else if (SymbolNames.Literal.LIST.contains(expSymbolName)) {
      return genLiteral(code, expNode, nestLevel, option);

    } else if (SymbolNames.Literal.EXP_LIST.contains(expSymbolName)) {
      return genExpression(expNode.findDescendantOf("*", "Literal", "*"), code, nestLevel, option);

    } else if (SymbolNames.PreDefFunc.EXP_LIST.contains(expSymbolName)) {
      return genPreDefFuncCallExp(expNode, code, nestLevel, option, true);

    } else if (SymbolNames.ConstantValue.LIST.contains(expSymbolName)) {
      return genConstValExp(expNode);
    }
    return null;
  }

  /**
   * 二項演算式を作成する.
   *
   * @param binaryExpNode 二項式のノード
   * @param code 途中式の格納先
   * @param nestLevel ソースコードのネストレベル
   * @param option コンパイルオプション
   * @return 式もしくは式の評価結果を格納した変数
   */
  String genBinaryExp(
      SyntaxSymbol binaryExpNode,
      StringBuilder code,
      int nestLevel,
      CompileOption option) {
    if (SymbolNames.BinaryExp.NONLOGICAL_LIST.contains(binaryExpNode.getSymbolName())) {
      return genNonlogicalBinaryExp(binaryExpNode, code, nestLevel, option);
    } else if (SymbolNames.BinaryExp.LOGICAL_LIST.contains(binaryExpNode.getSymbolName())) {
      return genLogicalBinaryExp(binaryExpNode, code, nestLevel, option);
    }
    throw new AssertionError(
        "Unknown binary expression.  (%s)".formatted(binaryExpNode.getSymbolName()));
  }

  /**
   * 非論理二項演算式を作成する.
   *
   * @param binaryExpNode 二項式のノード
   * @param code 途中式の格納先
   * @param nestLevel ソースコードのネストレベル
   * @param option コンパイルオプション
   * @return 式もしくは式の評価結果を格納した変数
   */
  String genNonlogicalBinaryExp(
      SyntaxSymbol binaryExpNode,
      StringBuilder code,
      int nestLevel,
      CompileOption option) {
    SyntaxSymbol leftExp =
        binaryExpNode.findDescendantOf("*",  SymbolNames.BinaryExp.LEFT_EXP, "*");
    String leftExpCode = genExpression(leftExp, code, nestLevel, option);

    SyntaxSymbol rightExp =
        binaryExpNode.findDescendantOf("*", SymbolNames.BinaryExp.RIGHT_EXP, "*");
    String rightExpCode = genExpression(rightExp, code, nestLevel, option);

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
   * @param binaryExpNode 二項式のノード
   * @param code 途中式の格納先
   * @param nestLevel ソースコードのネストレベル
   * @param option コンパイルオプション
   * @return 式もしくは式の評価結果を格納した変数
   */
  String genLogicalBinaryExp(
      SyntaxSymbol binaryExpNode,
      StringBuilder code,
      int nestLevel,
      CompileOption option) {
    SyntaxSymbol leftExp =
        binaryExpNode.findDescendantOf("*",  SymbolNames.BinaryExp.LEFT_EXP, "*");
    String leftExpCode = genExpression(leftExp, code, nestLevel, option);
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
    String rightExpCode = genExpression(rightExp, code, nestLevel + 1, option);
    common.genSetNextNodeInstId(code, binaryExpNode.getInstanceId(), nestLevel + 1, option);
    genConditionalWait(code, binaryExpNode, nestLevel + 1, option);
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
   * @param unaryExpNode 単項式のノード
   * @param code 途中式の格納先
   * @param nestLevel ソースコードのネストレベル
   * @param option コンパイルオプション
   * @return 式もしくは式の評価結果を格納した変数
   */
  String genUnaryExp(
      SyntaxSymbol unaryExpNode,
      StringBuilder code,
      int nestLevel,
      CompileOption option) {
    SyntaxSymbol primaryExp =
        unaryExpNode.findDescendantOf("*",  SymbolNames.UnaryExp.PRIMARY_EXP, "*");
    String primaryExpCode = genExpression(primaryExp, code, nestLevel, option);
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
   * @param literal リテラルのシンボル
   * @param nestLevel ソースコードのネストレベル
   * @param option コンパイルオプション
   * @return リテラル式
   */
  private String genLiteral(
      StringBuilder code,
      SyntaxSymbol literal,
      int nestLevel,
      CompileOption option) {
    if (SymbolNames.Literal.ARRAY_TYPES.contains(literal.getSymbolName())) {
      return "([])";  //空リスト
    }
    String inputText = "";
    if (literal instanceof TextNode textNode) {
      inputText = textNode.getText();
    }
    return switch (literal.getSymbolName()) {
      case SymbolNames.Literal.NUM_LITERAL -> "(" + common.toJsNumber(inputText) + ")";
      case SymbolNames.Literal.BOOL_LITERAL -> "(" + inputText + ")";
      case SymbolNames.Literal.STR_LITERAL -> "(" + common.toJsString(inputText) + ")";

      case SymbolNames.Literal.FREQ_SOUND_LITERAL,
           SymbolNames.Literal.SCALE_SOUND_LITERAL ->
          genSoundLiteralExp(literal, code, nestLevel, option);

      case SymbolNames.Literal.COLOR_LITERAL ->
          genColorLiteralExp(literal, code, nestLevel);

      default -> throw new AssertionError("Invalid literal " + literal.getSymbolName());
    };
  }

  /**
   * 定義済み関数の呼び出し式を作成する.
   *
   * @param funcCallNode 関数呼び出し式のノード
   * @param code 関数呼び出し式の格納先
   * @param nestLevel ソースコードのネストレベル
   * @param option コンパイルオプション
   * @param storeRetVal 戻り値を変数に格納するコードを出力する場合true.
   * @return 式もしくは式の評価結果を格納した変数. storeRetVal が false の場合は null.
   */
  String genPreDefFuncCallExp(
      SyntaxSymbol funcCallNode,
      StringBuilder code,
      int nestLevel,
      CompileOption option,
      boolean storeRetVal) {
    List<String> argList = genPreDefFuncArgs(funcCallNode, code, false, nestLevel, option);
    List<String> outArgList = genPreDefFuncArgs(funcCallNode, code, true, nestLevel, option);
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
    String[] argArray = argList.toArray(new String[0]);
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
   * @param funcCallNode 関数呼び出し式のノード
   * @param code 関数呼び出し式の格納先
   * @param outArg 出力引数を作成する場合 true. 入力引数を作成する場合 false.
   * @param nestLevel ソースコードのネストレベル
   * @param option コンパイルオプション
   * @return 実引数を格納したリスト
   */
  List<String> genPreDefFuncArgs(
      SyntaxSymbol funcCallNode,
      StringBuilder code,
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
        argList.add(genOutArg(argExp, code, nestLevel, option));
      } else {
        argList.add(genExpression(argExp, code, nestLevel, option));
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
    List<String> funcIdentifier = new ArrayList<>(List.of(funcCallNode.getSymbolName()));
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
    return FuncId.create(funcIdentifier.toArray(new String[0]));
  }

  /**
   * ユーザー定義関数の呼び出し式を生成する.
   *
   * @param funcCallNode 関数呼び出しのノード
   * @param code 生成したコードの格納先
   * @param nestLevel ソースコードのネストレベル
   * @param option コンパイルオプション
   * @param storeRetVal 戻り値を変数に格納するコードを出力する場合true.
   * @return 式もしくは式の評価結果を格納した変数. storeRetVal が false の場合は null.
   */
  String genUserDefFuncCallExp(
      SyntaxSymbol funcCallNode,
      StringBuilder code,
      int nestLevel,
      CompileOption option,
      boolean storeRetVal) {
    SyntaxSymbol arg = funcCallNode.findDescendantOf("*", SymbolNames.UserDefFunc.ARG, "*");
    SyntaxSymbol outArg = funcCallNode.findDescendantOf("*", SymbolNames.UserDefFunc.OUT_ARG, "*");
    List<String> argList = genArgList(arg, code, false, nestLevel, option);
    List<String> outArgList = genArgList(outArg, code, true, nestLevel, option);
    argList.addAll(outArgList);
    argList.addFirst(ScriptIdentifiers.Vars.THREAD_CONTEXT);
    String funcName = common.genFuncName(((BhNode) funcCallNode).getOriginal());
    String[] argArray = argList.toArray(new String[0]);
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
      code.append(retValName).append(" = ");
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
      SyntaxSymbol argNode,
      StringBuilder code,
      boolean assignToOutParams,
      int nestLevel,
      CompileOption option) {
    LinkedList<String> argList;
    SyntaxSymbol nextArg =
        argNode.findDescendantOf("*", SymbolNames.UserDefFunc.NEXT_ARG, "*");
    if (nextArg != null && !nextArg.getSymbolName().equals(SymbolNames.UserDefFunc.ARG_VOID)) {
      argList = genArgList(nextArg, code, assignToOutParams, nestLevel, option);
    } else {
      argList = new LinkedList<>();
    }
    SyntaxSymbol argument = argNode.findDescendantOf("*", SymbolNames.UserDefFunc.ARG, "*");
    if (argument != null) {
      if (assignToOutParams) {
        argList.addFirst(genOutArg(argument, code, nestLevel, option));
      } else {
        argList.addFirst(genExpression(argument, code, nestLevel, option));
      }
    }
    return argList;
  }

  /**
   * 出力実引数を作成する.
   *
   * @param varNode 変数ノード
   * @param code 生成したコードの格納先
   * @param nestLevel ソースコードのネストレベル
   * @param option コンパイルオプション
   * @return 出力変数
   */
  private String genOutArg(
      SyntaxSymbol varNode,
      StringBuilder code,
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
   * @param soundLiteral 音リテラルシンボル
   * @param code 生成したコードの格納先
   * @param nestLevel ソースコードのネストレベル
   * @param option コンパイルオプション
   * @return 音リテラルを格納した変数.
   */
  private String genSoundLiteralExp(
      SyntaxSymbol soundLiteral,
      StringBuilder code,
      int nestLevel,
      CompileOption option) {
    return switch (soundLiteral.getSymbolName()) {
      case SymbolNames.Literal.FREQ_SOUND_LITERAL ->
          genFreqSoundLiteralExp(soundLiteral, code, nestLevel, option);

      case SymbolNames.Literal.SCALE_SOUND_LITERAL ->
          genScaleSoundLiteralExp(soundLiteral, code, nestLevel, option);

      default ->
          throw new AssertionError("Invalid sound literal " + soundLiteral.getSymbolName());
    };
  }

  /**
   * 周波数指定の音リテラルのコードを生成する.
   *
   * @param freqSoundLiteral 周波数指定の音リテラルシンボル
   * @param code 生成したコードの格納先
   * @param nestLevel ソースコードのネストレベル
   * @param option コンパイルオプション
   * @return 音リテラルを格納した変数.
   */
  private String genFreqSoundLiteralExp(
      SyntaxSymbol freqSoundLiteral,
      StringBuilder code,
      int nestLevel,
      CompileOption option) {
    SyntaxSymbol volumeNode = 
        freqSoundLiteral.findDescendantOf(SymbolNames.Literal.Sound.VOLUME, "*");
    SyntaxSymbol durationNode =
        freqSoundLiteral.findDescendantOf(SymbolNames.Literal.Sound.DURATION, "*");
    SyntaxSymbol frequencyNode =
        freqSoundLiteral.findDescendantOf(SymbolNames.Literal.Sound.FREQUENCY, "*");
    String volume = genExpression(volumeNode, code, nestLevel, option);
    String duration = genExpression(durationNode, code, nestLevel, option);
    String frequency = genExpression(frequencyNode, code, nestLevel, option);
    // 音オブジェクト作成
    String soundVar = common.genVarName(freqSoundLiteral);
    String rightExp = common.genFuncCall(
        ScriptIdentifiers.Funcs.CREATE_SOUND, volume, frequency, duration);
    common.genSetNextNodeInstId(code, freqSoundLiteral.getInstanceId(), nestLevel, option);
    genConditionalWait(code, freqSoundLiteral, nestLevel, option);
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
   * @param scaleSoundLiteral 音階の音を指定する音リテラルシンボル
   * @param code 生成したコードの格納先
   * @param nestLevel ソースコードのネストレベル
   * @param option コンパイルオプション
   * @return 音リテラルを格納した変数.
   */
  private String genScaleSoundLiteralExp(
      SyntaxSymbol scaleSoundLiteral,
      StringBuilder code,
      int nestLevel,
      CompileOption option) {
    SyntaxSymbol volumeNode =
        scaleSoundLiteral.findDescendantOf(SymbolNames.Literal.Sound.VOLUME, "*");
    SyntaxSymbol durationNode =
        scaleSoundLiteral.findDescendantOf(SymbolNames.Literal.Sound.DURATION, "*");
    SyntaxSymbol octaveNode =
        scaleSoundLiteral.findDescendantOf(SymbolNames.Literal.Sound.OCTAVE, "*");
    SyntaxSymbol scaleSoundNode =
        scaleSoundLiteral.findDescendantOf(SymbolNames.Literal.Sound.SCALE_SOUND, "*");

    // 音階の音から周波数を計算する
    final String volume = genExpression(volumeNode, code, nestLevel, option);
    final String duration = genExpression(durationNode, code, nestLevel, option);
    String octave = genExpression(octaveNode, code, nestLevel, option);
    String scaleSound = genExpression(scaleSoundNode, code, nestLevel, option);
    octave = octave.replaceAll("[^\\d\\-]", "");
    scaleSound = scaleSound.replaceAll("[^\\d\\-]", "");
    double frequency =
        440 * Math.pow(2, (Double.parseDouble(octave) + Double.parseDouble(scaleSound)) / 12);
    // 音オブジェクト作成
    String soundVar = common.genVarName(scaleSoundLiteral);
    String rightExp = common.genFuncCall(
        ScriptIdentifiers.Funcs.CREATE_SOUND, volume, "(%.3f)".formatted(frequency), duration);
    common.genSetNextNodeInstId(code, scaleSoundLiteral.getInstanceId(), nestLevel, option);
    genConditionalWait(code, scaleSoundLiteral, nestLevel, option);
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
   * @param colorLiteralNode 色リテラルノード
   * @param code 生成したコードの格納先
   * @param nestLevel ソースコードのネストレベル
   * @return 色リテラルを格納した変数.
   */
  private String genColorLiteralExp(
      SyntaxSymbol colorLiteralNode,
      StringBuilder code,
      int nestLevel) {
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
   * 定数のコードを作成する.
   *
   * @param constValNode 識別子のノード
   * @return 定数の文字列
   */
  private String genConstValExp(SyntaxSymbol constValNode) {
    return "(" + SymbolNames.ConstantValue.VALUE_MAP.get(constValNode.getSymbolName()) + ")";
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
