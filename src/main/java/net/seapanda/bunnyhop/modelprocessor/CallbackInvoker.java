/**
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
package net.seapanda.bunnyhop.modelprocessor;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.function.Consumer;

import net.seapanda.bunnyhop.model.node.BhNode;
import net.seapanda.bunnyhop.model.node.BhNodeID;
import net.seapanda.bunnyhop.model.node.SyntaxSymbol;
import net.seapanda.bunnyhop.model.node.TextNode;
import net.seapanda.bunnyhop.model.node.VoidNode;
import net.seapanda.bunnyhop.model.node.connective.ConnectiveNode;
import net.seapanda.bunnyhop.model.node.connective.Connector;
import net.seapanda.bunnyhop.model.node.connective.ConnectorSection;
import net.seapanda.bunnyhop.model.node.connective.Subsection;

/**
 * シンボル名 or ノードIDと一致する識別子を持つコールバック関数を呼び出す.
 * @author K.Koike
 * */
public class CallbackInvoker implements BhModelProcessor {

	private final Callbacks callbacks;


	private CallbackInvoker(Callbacks callbacks) {
		this.callbacks = callbacks;
	}

	/**
	 * コールバック関数を呼び出す.
	 * @param callbacks 登録されたコールバック関数を持つオブジェクト
	 * @param node これ以下のノードのシンボル名とノードIDを調べ, 登録されたコードバック関数を呼び出す.
	 * */
	public static void invoke(Callbacks callbacks, BhNode node) {
		node.accept(new CallbackInvoker(callbacks));
	}

	@Override
	public void visit(ConnectiveNode node) {
		callbacks.call(node);
		node.sendToSections(this);
	}

	@Override
	public void visit(VoidNode node) {
		callbacks.call(node);
	}

	@Override
	public void visit(TextNode node) {
		callbacks.call(node);
	}

	@Override
	public void visit(Subsection section) {
		callbacks.call(section);
		section.sendToSubsections(this);
	}

	@Override
	public void visit(ConnectorSection connectorGroup) {
		callbacks.call(connectorGroup);
		connectorGroup.sendToConnectors(this);
	}

	@Override
	public void visit(Connector connector) {
		callbacks.call(connector);
		connector.sendToConnectedNode(this);
	}

	public static class Callbacks {

		private final Map<BhNodeID, Consumer<BhNode>> nodeIdToCallback = new HashMap<>();
		private final Map<String, Consumer<SyntaxSymbol>> symbolNameToCallback = new HashMap<>();
		private boolean allNodes = false;
		private Consumer<BhNode> callBackForAllNodes;

		private Callbacks() {}

		public static Callbacks create() {
			return new Callbacks();
		}

		/**
		 * シンボル名と一致したときに呼ばれるコールバック関数を登録する
		 * */
		public Callbacks set(String symbolName, Consumer<SyntaxSymbol> callback) {
			symbolNameToCallback.put(symbolName, callback);
			return this;
		}

		/**
		 * ノードIDと一致したときに呼ばれるコールバック関数を登録する
		 * */
		public Callbacks set(BhNodeID nodeID, Consumer<BhNode> callback) {
			nodeIdToCallback.put(nodeID, callback);
			return this;
		}

		/**
		 * 全ノードに対して呼ぶコールバック関数を登録する
		 * */
		public Callbacks setForAllNodes(Consumer<BhNode> callback) {
			allNodes = true;
			callBackForAllNodes = callback;
			return this;
		}

		void call(BhNode node) {

			if (allNodes)
				callBackForAllNodes.accept(node);

			Optional.ofNullable(nodeIdToCallback.get(node.getID()))
			.ifPresent(callback -> callback.accept(node));

			Optional.ofNullable(symbolNameToCallback.get(node.getSymbolName()))
			.ifPresent(callback -> callback.accept(node));
		}

		void call(SyntaxSymbol symbol) {
			Optional.ofNullable(symbolNameToCallback.get(symbol.getSymbolName()))
			.ifPresent(callback -> callback.accept(symbol));
		}
	}
}


























