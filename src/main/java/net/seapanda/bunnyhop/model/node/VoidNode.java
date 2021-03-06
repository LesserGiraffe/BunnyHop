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
package net.seapanda.bunnyhop.model.node;

import java.io.Serializable;
import java.util.List;
import java.util.function.Predicate;

import net.seapanda.bunnyhop.common.constant.VersionInfo;
import net.seapanda.bunnyhop.common.tools.MsgPrinter;
import net.seapanda.bunnyhop.common.tools.Util;
import net.seapanda.bunnyhop.model.node.attribute.BhNodeViewType;
import net.seapanda.bunnyhop.model.syntaxsynbol.SyntaxSymbol;
import net.seapanda.bunnyhop.model.templates.BhNodeAttributes;
import net.seapanda.bunnyhop.modelprocessor.BhModelProcessor;
import net.seapanda.bunnyhop.undo.UserOperationCommand;

/**
 * コネクタに何も繋がっていないことを表す終端BhNode
 * @author K.Koike
 * */
public class VoidNode extends BhNode implements Serializable {

	private static final long serialVersionUID = VersionInfo.SERIAL_VERSION_UID;

	/**
	 * コンストラクタ
	 * @param attributes ノードの設定情報
	 * */
	public VoidNode(BhNodeAttributes attributes) {
		super(BhNodeViewType.VOID, attributes);
	}

	/**
	 * コピーコンストラクタ
	 * @param org コピー元オブジェクト
	 */
	private VoidNode(VoidNode org) {
		super(org);
	}

	@Override
	public VoidNode copy(UserOperationCommand userOpeCmd, Predicate<BhNode> isNodeToBeCopied) {
		return new VoidNode(this);
	}

	@Override
	public BhNode getOriginal() {
		return null;
	}

	@Override
	public void accept(BhModelProcessor processor) {
		processor.visit(this);
	}

	/**
	 * モデルの構造を表示する
	 * @param depth 表示インデント数
	 * */
	@Override
	public void show(int depth) {
		String lastReplacedHash = "";
		if (getLastReplaced() != null)
			lastReplacedHash =  getLastReplaced().hashCode() + "";

		String parentHashCode = null;
		if (parentConnector != null)
			parentHashCode = parentConnector.hashCode() + "";

		MsgPrinter.INSTANCE.msgForDebug(indent(depth) + "<voidNode" + "  bhID=" + getID() + "  parent="+ parentHashCode + "> " + this.hashCode());
		MsgPrinter.INSTANCE.msgForDebug(indent(depth+1) + "<" + "last replaced " + lastReplacedHash + "> ");
	}

	@Override
	public boolean isRemovable() {
		return false;
	}

	@Override
	public boolean canBeReplacedWith(BhNode node) {

		if (getState() != BhNode.State.CHILD)
			return false;

		if (findRootNode().getState() != BhNode.State.ROOT_DIRECTLY_UNDER_WS)
			return false;

		if (node.isDescendantOf(this) || this.isDescendantOf(node))	//同じtree に含まれている場合置き換え不可
			return false;

		return parentConnector.isConnectedNodeReplaceableWith(node);
	}

	@Override
	public BhNode findOuterNode(int gneration) {

		if (gneration <= 0)
			return this;

		return null;
	}

	@Override
	public void findSymbolInDescendants(int generation, boolean toBottom, List<SyntaxSymbol> foundSymbolList, String... symbolNames) {

		if (generation == 0)
			for (String symbolName : symbolNames)
				if (Util.INSTANCE.equals(getSymbolName(), symbolName))
					foundSymbolList.add(this);
	}
}






