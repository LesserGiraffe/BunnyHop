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

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import net.seapanda.bunnyhop.compiler.api.CompilableSyntaxTreeNode;
import net.seapanda.bunnyhop.compiler.api.CompileOption;
import net.seapanda.bunnyhop.utility.Utility;

/**
 * 式のコード生成を行うクラス.
 *
 * @author K.Koike
 */
class ExpCodeGenerator {

  private final SymbolNames symbols;
  private final CommonCodeGenerator common;
  private final VarDeclCodeGenerator varDeclCodeGen;

  ExpCodeGenerator(
      SymbolNames symbols, CommonCodeGenerator common, VarDeclCodeGenerator varDeclCodeGen) {
    this.symbols = symbols;
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
  String generateExpression(
      CompilableSyntaxTreeNode expNode,
      StringBuilder code,
      int nestLevel,
      CompileOption option) {
    String expSymbolName = expNode.getSymbolName();
    if (symbols.exp.binary.list.contains(expSymbolName)) {
      return generateBinaryExp(expNode, code, nestLevel, option);

    } else if (symbols.exp.unary.list.contains(expSymbolName)) {
      return generateUnaryExp(expNode, code, nestLevel, option);

    } else if (symbols.exp.variable.list.contains(expSymbolName)) {
      return generateVarExp(expNode);

    } else if (symbols.exp.globalDataVar.list.contains(expSymbolName)) {
      return common.generateVarName(expNode.getOriginal());

    } else if (symbols.exp.literal.values.contains(expSymbolName)) {
      return generateLiteral(code, expNode, nestLevel, option);

    } else if (symbols.exp.literal.list.contains(expSymbolName)) {
      return generateExpression(
          expNode.findDescendantOf("*", "Literal", "*"), code, nestLevel, option);

    } else if (symbols.exp.predefFunc.list.contains(expSymbolName)) {
      return generatePreDefFuncCallExp(expNode, code, nestLevel, option, true);

    } else if (symbols.exp.constant.list.contains(expSymbolName)) {
      return generateConstValExp(expNode);
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
  String generateBinaryExp(
      CompilableSyntaxTreeNode binaryExpNode,
      StringBuilder code,
      int nestLevel,
      CompileOption option) {
    if (symbols.exp.binary.nonlogicalList.contains(binaryExpNode.getSymbolName())) {
      return generateNonlogicalBinaryExp(binaryExpNode, code, nestLevel, option);
    } else if (symbols.exp.binary.logicalList.contains(binaryExpNode.getSymbolName())) {
      return generateLogicalBinaryExp(binaryExpNode, code, nestLevel, option);
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
  String generateNonlogicalBinaryExp(
      CompilableSyntaxTreeNode binaryExpNode,
      StringBuilder code,
      int nestLevel,
      CompileOption option) {
    CompilableSyntaxTreeNode leftExp =
        binaryExpNode.findDescendantOf("*",  symbols.exp.binary.leftExp, "*");
    String leftExpCode = generateExpression(leftExp, code, nestLevel, option);

    CompilableSyntaxTreeNode rightExp =
        binaryExpNode.findDescendantOf("*", symbols.exp.binary.rightExp, "*");
    String rightExpCode = generateExpression(rightExp, code, nestLevel, option);

    CompilableSyntaxTreeNode operator =
        binaryExpNode.findDescendantOf("*", symbols.exp.binary.operator, "*");
    String operatorCode = symbols.exp.binary.operatorMap.get(operator.getLexeme());

    String tmpVar = common.generateVarName(binaryExpNode);
    generateDebugCode(binaryExpNode, code, nestLevel, option);
    code.append(common.indent(nestLevel))
        .append(Js._const_)
        .append(tmpVar)
        .append(" = ")
        .append(leftExpCode)
        .append(operatorCode)
        .append(rightExpCode)
        .append(";")
        .append(newLine);

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
  String generateLogicalBinaryExp(
      CompilableSyntaxTreeNode binaryExpNode,
      StringBuilder code,
      int nestLevel,
      CompileOption option) {
    CompilableSyntaxTreeNode leftExp =
        binaryExpNode.findDescendantOf("*", symbols.exp.binary.leftExp, "*");
    String leftExpCode = generateExpression(leftExp, code, nestLevel, option);
    String tmpVar = common.generateVarName(binaryExpNode);
    CompilableSyntaxTreeNode operator =
        binaryExpNode.findDescendantOf("*", symbols.exp.binary.operator, "*");
    String cond =
        operator.getLexeme().equals(symbols.exp.binary.opAnd) ? tmpVar : ("!" + tmpVar);

    code.append(common.indent(nestLevel))
        .append(Js._let_) // 再代入するので const 不可
        .append(tmpVar)
        .append(" = ")
        .append(leftExpCode)
        .append(";")
        .append(newLine)
        .append(common.indent(nestLevel))
        .append(Js._if_)
        .append("(")
        .append(cond)
        .append(") {")
        .append(newLine);

    CompilableSyntaxTreeNode rightExp =
        binaryExpNode.findDescendantOf("*", symbols.exp.binary.rightExp, "*");
    String rightExpCode = generateExpression(rightExp, code, nestLevel + 1, option);
    generateDebugCode(binaryExpNode, code, nestLevel + 1, option);
    code.append(common.indent(nestLevel + 1))
        .append(tmpVar)
        .append(" = ")
        .append(tmpVar)
        .append(symbols.exp.binary.operatorMap.get(operator.getLexeme()))
        .append(rightExpCode)
        .append(";")
        .append(newLine)
        .append(common.indent(nestLevel))
        .append("}")
        .append(newLine);

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
  String generateUnaryExp(
      CompilableSyntaxTreeNode unaryExpNode,
      StringBuilder code,
      int nestLevel,
      CompileOption option) {
    CompilableSyntaxTreeNode primaryExp =
        unaryExpNode.findDescendantOf("*", symbols.exp.unary.primaryExp, "*");
    String primaryExpCode = generateExpression(primaryExp, code, nestLevel, option);
    String operatorCode = symbols.exp.unary.operatorMap.get(unaryExpNode.getSymbolName());
    String tmpVar = common.generateVarName(unaryExpNode);
    generateDebugCode(unaryExpNode, code, nestLevel, option);
    code.append(common.indent(nestLevel))
        .append(Js._const_)
        .append(tmpVar)
        .append(" = ")
        .append(operatorCode)
        .append(primaryExpCode)
        .append(";")
        .append(newLine);

    return tmpVar;
  }

  /**
   * リテラルのコードを生成する.
   *
   * @param code 生成したコードの格納先
   * @param literal リテラルのシンボル
   * @param nestLevel ソースコードのネストレベル
   * @param option コンパイルオプション
   * @return リテラル式
   */
  private String generateLiteral(
      StringBuilder code,
      CompilableSyntaxTreeNode literal,
      int nestLevel,
      CompileOption option) {
    String inputText = literal.getLexeme();
    String symbolName = literal.getSymbolName();
    String exp;
    if (symbols.exp.literal.numLiteral.equals(symbolName)) {
      exp = "(" + common.toJsNumber(inputText) + ")";
    } else if (symbols.exp.literal.boolLiteral.equals(symbolName)) {
      exp = "(" + inputText + ")";
    } else if (symbols.exp.literal.strLiteral.equals(symbolName)) {
      exp = "(" + common.toJsString(inputText) + ")";
    } else if (symbols.exp.literal.freqSoundLiteral.equals(symbolName)
        || symbols.exp.literal.scaleSoundLiteral.equals(symbolName)) {
      exp = generateSoundLiteralExp(literal, code, nestLevel, option);
    } else if (symbols.exp.literal.colorLiteral.equals(symbolName)) {
      exp = generateColorLiteralExp(literal, code, nestLevel);
    } else {
      throw new AssertionError("Invalid literal " + literal.getSymbolName());
    }
    return exp;
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
  String generatePreDefFuncCallExp(
      CompilableSyntaxTreeNode funcCallNode,
      StringBuilder code,
      int nestLevel,
      CompileOption option,
      boolean storeRetVal) {
    List<String> argList = generatePreDefFuncArgs(funcCallNode, code, false, nestLevel, option);
    List<String> outArgList = generatePreDefFuncArgs(funcCallNode, code, true, nestLevel, option);
    argList.addAll(outArgList);
    String retValName = null;
    if (storeRetVal) {
      retValName = common.generateVarName(funcCallNode);
      code.append(common.indent(nestLevel))
          .append(Js._const_)
          .append(retValName)
          .append(";")
          .append(newLine);
    }
    generateDebugCode(funcCallNode, code, nestLevel, option);
    String[] argArray = argList.toArray(new String[0]);
    String funcCallCode;
    // 恒等写像は最初の引数を結果の変数に代入するだけ
    String funcName = symbols.exp.predefFunc.getFuncName(createFuncId(funcCallNode));
    if (funcName.equals(ScriptIdentifiers.Funcs.IDENTITY)) {
      funcCallCode = argArray[0];
    } else {
      funcCallCode = common.generateFuncCall(funcName, argArray);
    }
    code.append(common.indent(nestLevel));
    if (storeRetVal) {
      code.append(retValName).append(" = ");
    }
    code.append(funcCallCode).append(";").append(newLine);
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
  List<String> generatePreDefFuncArgs(
      CompilableSyntaxTreeNode funcCallNode,
      StringBuilder code,
      boolean outArg,
      int nestLevel,
      CompileOption option) {
    var argList = new ArrayList<String>();
    int idArg = 0;
    while (true) {
      String argCnctrName = outArg ? symbols.predefFunc.outArg : symbols.predefFunc.arg;
      argCnctrName += idArg;
      CompilableSyntaxTreeNode argExp = funcCallNode.findDescendantOf("*", argCnctrName, "*");
      if (argExp == null) {
        break;
      }
      if (outArg) {
        argList.add(generateOutArg(argExp, code, nestLevel, option));
      } else {
        argList.add(generateExpression(argExp, code, nestLevel, option));
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
  FuncId createFuncId(CompilableSyntaxTreeNode funcCallNode) {
    //呼び出しオプションを探す
    int idOption = 0;
    List<String> funcIdentifier = new ArrayList<>(List.of(funcCallNode.getSymbolName()));
    while (true) {
      String optionCnctrName = symbols.predefFunc.option + idOption;
      CompilableSyntaxTreeNode optionExp = funcCallNode.findDescendantOf("*", optionCnctrName, "*");
      if (optionExp == null) {
        break;
      }
      funcIdentifier.add(optionExp.getLexeme());
      ++idOption;
    }
    return FuncId.of(funcIdentifier.toArray(new String[0]));
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
  String generateUserDefFuncCallExp(
      CompilableSyntaxTreeNode funcCallNode,
      StringBuilder code,
      int nestLevel,
      CompileOption option,
      boolean storeRetVal) {
    CompilableSyntaxTreeNode arg = funcCallNode.findDescendantOf("*", symbols.userDefFunc.arg, "*");
    CompilableSyntaxTreeNode outArg =
        funcCallNode.findDescendantOf("*", symbols.userDefFunc.outArg, "*");
    List<String> argList = generateArgList(arg, code, false, nestLevel, option);
    List<String> outArgList = generateArgList(outArg, code, true, nestLevel, option);
    argList.addAll(outArgList);
    argList.addFirst(ScriptIdentifiers.Vars.THREAD_CONTEXT);
    String funcName = common.generateFuncName(funcCallNode.getOriginal());
    String[] argArray = argList.toArray(new String[0]);
    final String funcCallCode = common.generateFuncCall(funcName, argArray);

    String retValName = null;
    if (storeRetVal) {
      retValName = common.generateVarName(funcCallNode);
      code.append(common.indent(nestLevel))
          .append(Js._const_)
          .append(retValName)
          .append(";")
          .append(newLine);
    }
    generateDebugCode(funcCallNode, code, nestLevel, option);
    code.append(common.indent(nestLevel));
    if (storeRetVal) {
      code.append(retValName).append(" = ");
    }
    code.append(funcCallCode)
        .append(";")
        .append(newLine);
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
  private LinkedList<String> generateArgList(
      CompilableSyntaxTreeNode argNode,
      StringBuilder code,
      boolean assignToOutParams,
      int nestLevel,
      CompileOption option) {
    LinkedList<String> argList;
    CompilableSyntaxTreeNode nextArg =
        argNode.findDescendantOf("*", symbols.userDefFunc.nextArg, "*");
    if (nextArg != null && !nextArg.getSymbolName().equals(symbols.userDefFunc.argVoid)) {
      argList = generateArgList(nextArg, code, assignToOutParams, nestLevel, option);
    } else {
      argList = new LinkedList<>();
    }
    CompilableSyntaxTreeNode argument = argNode.findDescendantOf("*", symbols.userDefFunc.arg, "*");
    if (argument != null) {
      if (assignToOutParams) {
        argList.addFirst(generateOutArg(argument, code, nestLevel, option));
      } else {
        argList.addFirst(generateExpression(argument, code, nestLevel, option));
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
  private String generateOutArg(
      CompilableSyntaxTreeNode varNode,
      StringBuilder code,
      int nestLevel,
      CompileOption option) {
    if (symbols.exp.variable.list.contains(varNode.getSymbolName())) {
      CompilableSyntaxTreeNode varDecl = varNode.getOriginal();
      if (common.isOutputParam(varDecl)) {
        return common.generateVarName(varDecl); // out -> out
      } else {
        return common.generateVarAccessorName(varDecl); // in -> out
      }
    } else if (symbols.exp.variable.voidList.contains(varNode.getSymbolName())) {
      // 出力引数に変数指定がなかった場合
      List<String> vars = varDeclCodeGen.generateVarDeclAndAccessor(
          varNode,
          symbols.exp.variable.initValMap.get(varNode.getSymbolName()),
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
  private String generateSoundLiteralExp(
      CompilableSyntaxTreeNode soundLiteral,
      StringBuilder code,
      int nestLevel,
      CompileOption option) {
    String symbolName = soundLiteral.getSymbolName();
    String exp;
    if (symbols.exp.literal.freqSoundLiteral.equals(symbolName)) {
      exp = generateFreqSoundLiteralExp(soundLiteral, code, nestLevel, option);
    } else if (symbols.exp.literal.scaleSoundLiteral.equals(symbolName)) {
      exp = generateScaleSoundLiteralExp(soundLiteral, code, nestLevel, option);
    } else {
      throw new AssertionError("Invalid sound literal " + soundLiteral.getSymbolName());
    }
    return exp;
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
  private String generateFreqSoundLiteralExp(
      CompilableSyntaxTreeNode freqSoundLiteral,
      StringBuilder code,
      int nestLevel,
      CompileOption option) {
    CompilableSyntaxTreeNode volumeNode =
        freqSoundLiteral.findDescendantOf(symbols.exp.literal.sound.volume, "*");
    CompilableSyntaxTreeNode durationNode =
        freqSoundLiteral.findDescendantOf(symbols.exp.literal.sound.duration, "*");
    CompilableSyntaxTreeNode frequencyNode =
        freqSoundLiteral.findDescendantOf(symbols.exp.literal.sound.frequency, "*");
    String volume = generateExpression(volumeNode, code, nestLevel, option);
    String duration = generateExpression(durationNode, code, nestLevel, option);
    String frequency = generateExpression(frequencyNode, code, nestLevel, option);
    // 音オブジェクト作成
    String soundVar = common.generateVarName(freqSoundLiteral);
    String rightExp = common.generateFuncCall(
        ScriptIdentifiers.Funcs.CREATE_SOUND, volume, frequency, duration);
    generateDebugCode(freqSoundLiteral, code, nestLevel, option);
    code.append(common.indent(nestLevel))
        .append(Js._const_)
        .append(soundVar)
        .append(" = ")
        .append(rightExp)
        .append(";")
        .append(newLine);
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
  private String generateScaleSoundLiteralExp(
      CompilableSyntaxTreeNode scaleSoundLiteral,
      StringBuilder code,
      int nestLevel,
      CompileOption option) {
    CompilableSyntaxTreeNode volumeNode =
        scaleSoundLiteral.findDescendantOf(symbols.exp.literal.sound.volume, "*");
    CompilableSyntaxTreeNode durationNode =
        scaleSoundLiteral.findDescendantOf(symbols.exp.literal.sound.duration, "*");
    CompilableSyntaxTreeNode octaveNode =
        scaleSoundLiteral.findDescendantOf(symbols.exp.literal.sound.octave, "*");
    CompilableSyntaxTreeNode scaleSoundNode =
        scaleSoundLiteral.findDescendantOf(symbols.exp.literal.sound.scaleSound, "*");

    // 音階の音から周波数を計算する
    final String volume = generateExpression(volumeNode, code, nestLevel, option);
    final String duration = generateExpression(durationNode, code, nestLevel, option);
    String octave = generateExpression(octaveNode, code, nestLevel, option);
    String scaleSound = generateExpression(scaleSoundNode, code, nestLevel, option);
    octave = octave.replaceAll("[^\\d\\-]", "");
    scaleSound = scaleSound.replaceAll("[^\\d\\-]", "");
    double frequency =
        440 * Math.pow(2, (Double.parseDouble(octave) + Double.parseDouble(scaleSound)) / 12);
    // 音オブジェクト作成
    String soundVar = common.generateVarName(scaleSoundLiteral);
    String rightExp = common.generateFuncCall(
        ScriptIdentifiers.Funcs.CREATE_SOUND, volume, "(%.3f)".formatted(frequency), duration);
    generateDebugCode(scaleSoundLiteral, code, nestLevel, option);
    code.append(common.indent(nestLevel))
        .append(Js._const_)
        .append(soundVar)
        .append(" = ")
        .append(rightExp)
        .append(";")
        .append(newLine);
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
  private String generateColorLiteralExp(
      CompilableSyntaxTreeNode colorLiteralNode,
      StringBuilder code,
      int nestLevel) {
    String colorName = "'" + colorLiteralNode.getLexeme() + "'";
    String colorVar = common.generateVarName(colorLiteralNode);
    String rightExp = common.generateFuncCall(
        ScriptIdentifiers.Funcs.CREATE_COLOR_FROM_NAME, colorName);
    code.append(common.indent(nestLevel))
        .append(Js._const_)
        .append(colorVar)
        .append(" = ")
        .append(rightExp)
        .append(";")
        .append(newLine);
    return colorVar;
  }

  /**
   * 定数のコードを生成する.
   *
   * @param constValNode 識別子のノード
   * @return 定数の文字列
   */
  private String generateConstValExp(CompilableSyntaxTreeNode constValNode) {
    return "(" + symbols.exp.constant.valueMap.get(constValNode.getSymbolName()) + ")";
  }

  /** 変数ノードから式を生成する. */
  private String generateVarExp(CompilableSyntaxTreeNode expNode) {
    CompilableSyntaxTreeNode varDeclNode = expNode.getOriginal();
    if (common.isOutputParam(varDeclNode)) {
      return "(" + common.generateGetOutputParamVal(varDeclNode) + ")";
    } else {
      return common.generateVarName(varDeclNode);
    }
  }

  /**
   * ノードを処理する前のデバッグ用コードを生成する.
   *
   * @param code 生成したコードの格納先
   * @param nestLevel ソースコードのネストレベル
   * @param option コンパイルオプション
   */
  public void generateDebugCode(
      CompilableSyntaxTreeNode node,
      StringBuilder code,
      int nestLevel,
      CompileOption option) {
    String instVar = common.generateAssignNodeInstId(node, code, nestLevel, option);
    common.generateSetInstIdToThreadContext(instVar, code, nestLevel, option);
    if (node.canSetBreakpoint()) {
      common.generateConditionalWait(instVar, code, nestLevel, option);
    }
  }
}
