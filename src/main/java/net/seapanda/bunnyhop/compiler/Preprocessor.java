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

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Consumer;
import net.seapanda.bunnyhop.model.node.BhNode;
import net.seapanda.bunnyhop.model.node.TextNode;
import net.seapanda.bunnyhop.model.node.syntaxsymbol.SyntaxSymbol;
import net.seapanda.bunnyhop.model.traverse.CallbackInvoker;

/**
 * コード生成前の処理を行うクラス.
 *
 * @author K.Koike
 */
public class Preprocessor {

  private static final Map<String, Consumer<SyntaxSymbol>> NODE_NAME_TO_PREPROCESSOR_FUNC =
      new HashMap<>() {{
          put(SymbolNames.PreDefFunc.ANY_ARRAY_TO_STR_EXP, Preprocessor::procAnyArrayToStrExp);
        }
      };

  /**
   * コンパイル前の処理を行う.
   *
   * @param nodesToPreprocess 処理するノードのリスト
   */
  public static void process(Collection<BhNode> nodesToPreprocess) {

    // コールバック登録
    CallbackInvoker.CallbackRegistry callbacks = CallbackInvoker.newCallbackRegistry();
    NODE_NAME_TO_PREPROCESSOR_FUNC.entrySet().forEach(
        nodeIdAndFunc -> callbacks.set(nodeIdAndFunc.getKey(), nodeIdAndFunc.getValue()));

    // コールバック呼び出し
    nodesToPreprocess.forEach(node -> CallbackInvoker.invoke(callbacks, node));
  }


  /**
   * AnyArrayToStrExp ノードの前処理を行う.
   *
   * @param node AnyArrayToStrExp ノード
   */
  private static void procAnyArrayToStrExp(SyntaxSymbol node) {
    SyntaxSymbol listNode = node.findSymbolInDescendants("*", "Arg0", "*");
    SyntaxSymbol listNameNode =
        listNode.findSymbolInDescendants("*", SymbolNames.VarDecl.LIST_NAME, "*");
    if (listNameNode instanceof TextNode textNode) {
      SyntaxSymbol dest = node.findSymbolInDescendants("*", "Arg1", "*");
      ((TextNode) dest).setText(textNode.getText());
    }
  }
}
