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

import static net.seapanda.bunnyhop.compiler.SymbolNames.PreDefFunc.ANY_LIST_TO_STR_EXP;
import static net.seapanda.bunnyhop.compiler.SymbolNames.VarDecl.LIST_NAME;

import java.util.Map;
import java.util.function.Consumer;
import org.apache.commons.lang3.mutable.MutableLong;

/**
 * コード生成前の処理を行うクラス.
 *
 * @author K.Koike
 */
class Preprocessor {

  private static final Map<String, Consumer<CompilableSyntaxTreeNode>> SYMBOL_NAME_TO_PREPROCESSOR =
      Map.of(ANY_LIST_TO_STR_EXP, Preprocessor::preprocessAnyListToStrExp);

  /**
   * コンパイル前の処理を行う.
   *
   * @param instIdToNode 処理するノードとそのノードのインスタンス ID を格納したマップ.
   */
  static void process(Map<String, CompilableSyntaxTreeNode> instIdToNode) {
    Consumer<CompilableSyntaxTreeNode> nop = n -> {};
    instIdToNode.values().forEach(
        node -> SYMBOL_NAME_TO_PREPROCESSOR.getOrDefault(node.getSymbolName(), nop).accept(node));

    MutableLong serialNo = new MutableLong(0);
    instIdToNode.values().forEach(
        node -> node.setTag(Long.toHexString(serialNo.incrementAndGet())));
  }


  /**
   * AnyListToStrExp ノードの前処理を行う.
   *
   * <p>文字列に変換するリストの名前を文字列化関数の引数に相当するノードにセットする.
   *
   * @param node AnyListToStrExp ノード
   */
  private static void preprocessAnyListToStrExp(CompilableSyntaxTreeNode node) {
    CompilableSyntaxTreeNode listNode = node.findDescendantOf("*", "Arg0", "*");
    CompilableSyntaxTreeNode listNameNode = listNode.findDescendantOf("*", LIST_NAME, "*");
    if (listNameNode == null) {
      return;
    }
    String listName = listNameNode.getLexeme();
    CompilableSyntaxTreeNode dest = node.findDescendantOf("*", "Arg1", "*");
    dest.setLexeme(listName);
  }
}
