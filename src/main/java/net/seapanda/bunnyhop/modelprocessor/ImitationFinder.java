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
import java.util.Collection;

import net.seapanda.bunnyhop.model.node.BhNode;
import net.seapanda.bunnyhop.model.node.TextNode;
import net.seapanda.bunnyhop.model.node.connective.ConnectiveNode;
import net.seapanda.bunnyhop.model.node.imitation.Imitatable;

/**
 * 走査したノードが保持するイミテーションノードを集めるクラス.
 * @author K.Koike
 */
public class ImitationFinder implements BhModelProcessor {

	private Collection<Imitatable> imitations = new ArrayList<>();

	/**
	 * 引数で指定したノード以下のオリジナルノードが持つイミテーションノードを全て返す.
	 * @param node このノード以下のオリジナルノードが持つイミテーションノードを探す
	 * @param userOpeCmd undo用コマンドオブジェクト
	 * @return 発見したイミテーションノードのリスト
	 */
	public static Collection<Imitatable> find(BhNode node) {

		var finder = new ImitationFinder();
		node.accept(finder);
		return finder.imitations;
	}

	/**
	 * コンストラクタ
	 * @param userOpeCmd undo用コマンドオブジェクト
	 */
	private ImitationFinder() {}

	@Override
	public void visit(ConnectiveNode node) {

		imitations.addAll(node.getImitationList());
		node.sendToSections(this);
	}

	@Override
	public void visit(TextNode node) {
		imitations.addAll(node.getImitationList());
	}
}
