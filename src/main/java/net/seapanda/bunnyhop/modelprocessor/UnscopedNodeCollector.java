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

import java.util.ArrayList;
import java.util.List;

import net.seapanda.bunnyhop.model.imitation.Imitatable;
import net.seapanda.bunnyhop.model.node.BhNode;
import net.seapanda.bunnyhop.model.node.TextNode;
import net.seapanda.bunnyhop.model.node.connective.ConnectiveNode;

/**
 * スコープ外のノードを探して集めるクラス
 * @author K.Koike
 * */
public class UnscopedNodeCollector implements BhModelProcessor {

	private final List<Imitatable> unscopedNodeList = new ArrayList<>();	//!< オリジナルノードと同じスコープに居ないイミテーションノードのリスト

	/**
	 * 以下の2種類のスコープ外ノードを集める
	 *   ・引数のノード以下にあるスコープ外イミテーションノード
	 *   ・引数のノード以下にあるオリジナルノードのスコープ外イミテーションノード
	 * */
	public static List<Imitatable> collect(BhNode node) {

		var collector = new UnscopedNodeCollector();
		node.accept(collector);
		return collector.unscopedNodeList;
	}

	private UnscopedNodeCollector() {}

	@Override
	public void visit(ConnectiveNode node) {

		node.introduceSectionsTo(this);
		node.getImitationInfo().getImitationList().forEach(
			imitNode -> {
				if (imitNode.isUnscoped())
					unscopedNodeList.add(imitNode);
		});

		if (node.isUnscoped())
			unscopedNodeList.add(node);
	}

	@Override
	public void visit(TextNode node) {

		node.getImitationInfo().getImitationList().forEach(
			imitNode -> {
				if (imitNode.isUnscoped())
					unscopedNodeList.add(imitNode);
		});

		if (node.isUnscoped())
			unscopedNodeList.add(node);
	}
}
