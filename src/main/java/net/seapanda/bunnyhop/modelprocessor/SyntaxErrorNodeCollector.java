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

import net.seapanda.bunnyhop.model.node.BhNode;
import net.seapanda.bunnyhop.model.node.TextNode;
import net.seapanda.bunnyhop.model.node.VoidNode;
import net.seapanda.bunnyhop.model.node.connective.ConnectiveNode;
import net.seapanda.bunnyhop.model.node.imitation.Imitatable;

/**
 * 構文エラーノードを探して集めるクラス
 * @author K.Koike
 * */
public class SyntaxErrorNodeCollector implements BhModelProcessor {

	private final List<BhNode> errorNodeList = new ArrayList<>();	//!< オリジナルノードと同じスコープに居ないイミテーションノードのリスト

	/**
	 * 以下の2種類の構文エラーノードを管理対象に入れる
	 *   ・引数のノード以下にある構文エラーノード
	 *   ・引数のノード以下にあるオリジナルノードが持つイミテーションで構文エラーを起こしているノード
	 * */
	public static List<BhNode> collect(BhNode node) {

		var collector = new SyntaxErrorNodeCollector();
		node.accept(collector);
		return collector.errorNodeList;
	}

	private SyntaxErrorNodeCollector() {}

	@Override
	public void visit(ConnectiveNode node) {

		node.sendToSections(this);
		for (Imitatable imitNode : node.getImitationList()) {
			if (imitNode.hasSyntaxError())
				errorNodeList.add(imitNode);
		}

		if (node.hasSyntaxError())
			errorNodeList.add(node);
	}

	@Override
	public void visit(TextNode node) {

		for (Imitatable imitNode : node.getImitationList()) {
			if (imitNode.hasSyntaxError())
				errorNodeList.add(imitNode);
		}

		if (node.hasSyntaxError())
			errorNodeList.add(node);
	}

	@Override
	public void visit(VoidNode node) {

		if (node.hasSyntaxError())
			errorNodeList.add(node);
	}
}
