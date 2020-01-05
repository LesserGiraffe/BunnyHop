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
package net.seapanda.bunnyhop.model.nodeselection;

import java.nio.file.Path;
import java.util.Optional;

import org.mozilla.javascript.NativeArray;
import org.mozilla.javascript.NativeObject;

import net.seapanda.bunnyhop.common.TreeNode;
import net.seapanda.bunnyhop.common.constant.BhParams;
import net.seapanda.bunnyhop.common.tools.MsgPrinter;
import net.seapanda.bunnyhop.configfilereader.BhScriptManager;
import net.seapanda.bunnyhop.message.BhMsg;
import net.seapanda.bunnyhop.message.MsgData;
import net.seapanda.bunnyhop.message.MsgProcessor;
import net.seapanda.bunnyhop.message.MsgReceptionWindow;
import net.seapanda.bunnyhop.model.node.attribute.BhNodeID;
import net.seapanda.bunnyhop.model.templates.BhNodeTemplates;

/**
 * ノードのカテゴリ一覧を表示している部分のモデル
 * @author K.Koike
 */
public class BhNodeCategoryList implements MsgReceptionWindow {

	private TreeNode<String> templateTreeRoot;
	private MsgProcessor msgProcessor;	//!< このオブジェクト宛てに送られたメッセージを処理するオブジェクト

	private BhNodeCategoryList() {};

	/**
	 * ノードカテゴリとテンプレートノードの配置情報が記されたファイルを読み込みテンプレートリストを作成する
	 * @param ノードカテゴリとテンプレートノードの配置情報が記されたファイルのパス
	 * @return
	 */
	public static Optional<BhNodeCategoryList> create(Path filePath) {

		Optional<NativeObject> jsonObj = BhScriptManager.INSTANCE.parseJsonFile(filePath);
		if (jsonObj.isEmpty())
			return Optional.empty();

		var newObj = new BhNodeCategoryList();

		newObj.templateTreeRoot = new TreeNode<>("root");
		boolean success = newObj.addChildren(jsonObj.get(), newObj.templateTreeRoot, filePath.toString());
		if (success)
			return Optional.of(newObj);

		return Optional.empty();
	}

	/**
	 * テンプレートツリーに子ノードを追加する.<br>
	 * 子ノードが葉だった場合、対応するBhNode があるかどうかを調べ, ない場合はfalse を返す
	 * @param jsonObj このオブジェクトのメンバーが追加すべき子ノードとなる
	 * @param parent 子ノードを追加したいノード
	 * @return 下位の葉ノードに対応するBhNode があった場合true
	 * */
	private boolean addChildren(NativeObject jsonObj, TreeNode<String> parent, String fileName) {

		boolean bhNodeForLeafExists = true;
		for(Object key : jsonObj.keySet()) {

			if (!(key instanceof String))
				continue;

			Object val = jsonObj.get(key);
			switch (key.toString()) {
				case BhParams.NodeTemplate.KEY_CSS_CLASS:	//cssクラスのキー
					if (val instanceof String) {
						TreeNode<String> cssClass = new TreeNode<>(BhParams.NodeTemplate.KEY_CSS_CLASS);
						cssClass.children.add(new TreeNode<>(val.toString()));
						parent.children.add(cssClass);
					}
					break;

				case BhParams.NodeTemplate.KEY_CONTENTS:	//ノードIDの配列のキー
					if (val instanceof NativeArray) {
						TreeNode<String> contents = new TreeNode<>(BhParams.NodeTemplate.KEY_CONTENTS);
						bhNodeForLeafExists &= addBhNodeID((NativeArray)val, contents, fileName);
						parent.children.add(contents);
					}
					break;

				default:					//カテゴリ名
					if (val instanceof NativeObject) {
						TreeNode<String> child = new TreeNode<>(key.toString());
						parent.children.add(child);
						bhNodeForLeafExists &= addChildren((NativeObject)val, child, fileName);
					}
					break;
			}
		}
		return bhNodeForLeafExists;
	}

	/**
	 * JsonArray からBhNode の ID を読み取って、対応するノードがあるIDのみ子ノードとして追加する
	 * @param bhNodeIDList BhNode の ID のリスト
	 * @param parent IDを子ノードとして追加する親ノード
	 * @return 各IDに対応するBhNodeがすべて見つかった場合true
	 * */
	private boolean addBhNodeID(NativeArray bhNodeIDList, TreeNode<String> parent, String fileName) {

		boolean allBhNodesExist = true;
		for (Object bhNodeID : bhNodeIDList) {
			if (bhNodeID instanceof String) {	// 配列内の文字列だけをBhNode の IDとみなす

				String bhNodeIDStr = (String)bhNodeID;
				if (BhNodeTemplates.INSTANCE.bhNodeExists(BhNodeID.create(bhNodeIDStr))) {	//IDに対応する BhNode がある
					parent.children.add(new TreeNode<>(bhNodeIDStr));
				}
				else {
					allBhNodesExist &= false;
					MsgPrinter.INSTANCE.errMsgForDebug(
						bhNodeIDStr + " に対応する " + BhParams.BhModelDef.ELEM_NAME_NODE + " が存在しません.\n" +
						"(" + fileName + ")");
				}
			}
		}
		return allBhNodesExist;
	}

	/**
	 * BhNode選択リストのルートノードを返す
	 * @return BhNode選択リストのルートノード
	 */
	public TreeNode<String> getRootNode() {
		return templateTreeRoot;
	}

	@Override
	public void setMsgProcessor(MsgProcessor processor) {
		msgProcessor = processor;
	}

	@Override
	public MsgData passMsg(BhMsg msg, MsgData data) {
		return msgProcessor.processMsg(msg, data);
	}
}





















