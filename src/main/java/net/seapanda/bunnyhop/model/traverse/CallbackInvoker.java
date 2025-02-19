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

package net.seapanda.bunnyhop.model.traverse;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.function.Consumer;
import net.seapanda.bunnyhop.model.node.BhNode;
import net.seapanda.bunnyhop.model.node.ConnectiveNode;
import net.seapanda.bunnyhop.model.node.Connector;
import net.seapanda.bunnyhop.model.node.TextNode;
import net.seapanda.bunnyhop.model.node.parameter.BhNodeId;
import net.seapanda.bunnyhop.model.node.section.ConnectorSection;
import net.seapanda.bunnyhop.model.node.section.Subsection;
import net.seapanda.bunnyhop.model.node.syntaxsymbol.SyntaxSymbol;

/**
 * シンボル名 or ノードIDと一致する識別子を持つコールバック関数を呼び出す.
 *
 * @author K.Koike
 */
public class CallbackInvoker implements BhNodeWalker {

  private final CallbackRegistry callbackRegistry;

  private CallbackInvoker(CallbackRegistry callbackRegistry) {
    this.callbackRegistry = callbackRegistry;
  }

  /**
   * コールバック関数の登録先オブジェクトを作成する.
   *
   * @return コールバック関数の登録先オブジェクト
   */
  public static CallbackRegistry newCallbackRegistry() {
    return new CallbackRegistry();
  }

  /**
   * コールバック関数を呼び出す.
   *
   * @param registry 登録されたコールバック関数を持つオブジェクト
   * @param node これ以下のノードのシンボル名とノード ID を調べ, 登録されたコードバック関数を呼び出す.
   */
  public static void invoke(CallbackRegistry registry, BhNode node) {
    node.accept(new CallbackInvoker(registry));
  }

  @Override
  public void visit(ConnectiveNode node) {
    callbackRegistry.call(node);
    node.sendToSections(this);
  }

  @Override
  public void visit(TextNode node) {
    callbackRegistry.call(node);
  }

  @Override
  public void visit(Subsection section) {
    callbackRegistry.call(section);
    section.sendToSubsections(this);
  }

  @Override
  public void visit(ConnectorSection connectorGroup) {
    callbackRegistry.call(connectorGroup);
    connectorGroup.sendToConnectors(this);
  }

  @Override
  public void visit(Connector connector) {
    callbackRegistry.call(connector);
    connector.sendToConnectedNode(this);
  }

  /** {@link BhNode} に適用する処理を格納するクラス. */
  public static class CallbackRegistry {

    private final Map<BhNodeId, Consumer<? super BhNode>> nodeIdToCallback = new HashMap<>();
    private final Map<String, Consumer<? super SyntaxSymbol>> symbolNameToCallback =
        new HashMap<>();
    private Consumer<? super BhNode> callBackForAllNodes;
    private Consumer<? super SyntaxSymbol> callBackForAllSymbols;
    private boolean allNodes = false;
    private boolean allSymbols = false;

    private CallbackRegistry() {}

    /** シンボル名と一致したときに呼ばれるコールバック関数を登録する. */
    public CallbackRegistry set(String symbolName, Consumer<? super SyntaxSymbol> callback) {
      symbolNameToCallback.put(symbolName, callback);
      return this;
    }

    /** ノードIDと一致したときに呼ばれるコールバック関数を登録する. */
    public CallbackRegistry set(BhNodeId nodeId, Consumer<? super BhNode> callback) {
      nodeIdToCallback.put(nodeId, callback);
      return this;
    }

    /** 全ノードに対して呼ぶコールバック関数を登録する. */
    public CallbackRegistry setForAllNodes(Consumer<? super BhNode> callback) {
      allNodes = true;
      callBackForAllNodes = callback;
      return this;
    }

    /** 全 SyntaxSymbol に対して呼ぶコールバック関数を登録する. */
    public CallbackRegistry setForAllSyntaxSymbols(Consumer<? super SyntaxSymbol> callback) {
      allSymbols = true;
      callBackForAllSymbols = callback;
      return this;
    }

    void call(BhNode node) {
      if (allNodes) {
        callBackForAllNodes.accept(node);
      }
      if (allSymbols) {
        callBackForAllSymbols.accept(node);
      }
      Optional.ofNullable(nodeIdToCallback.get(node.getId()))
          .ifPresent(callback -> callback.accept(node));

      Optional.ofNullable(symbolNameToCallback.get(node.getSymbolName()))
          .ifPresent(callback -> callback.accept(node));
    }

    void call(SyntaxSymbol symbol) {
      if (allSymbols) {
        callBackForAllSymbols.accept(symbol);
      }
      Optional.ofNullable(symbolNameToCallback.get(symbol.getSymbolName()))
          .ifPresent(callback -> callback.accept(symbol));
    }
  }
}
