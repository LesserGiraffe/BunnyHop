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
package pflab.bunnyhop.model;

import pflab.bunnyhop.model.templates.BhNodeTemplates;
import java.nio.file.Path;
import java.nio.file.Paths;

import jdk.nashorn.api.scripting.ScriptObjectMirror;

import pflab.bunnyhop.root.MsgPrinter;
import pflab.bunnyhop.common.BhParams;
import pflab.bunnyhop.common.TreeNode;
import pflab.bunnyhop.common.Util;
import pflab.bunnyhop.configfilereader.BhScriptManager;
import pflab.bunnyhop.message.BhMsg;
import pflab.bunnyhop.message.MsgData;
import pflab.bunnyhop.message.MsgProcessor;
import pflab.bunnyhop.message.MsgReceptionWindow;

/**
 * BhNode のカテゴリ一覧を表示している部分のmodel
 * @author K.Koike
 * */
public class BhNodeCategoryList implements MsgReceptionWindow {

	private TreeNode<String> templateTreeRoot;
	private MsgProcessor msgProcessor;	//!< このオブジェクト宛てに送られたメッセージを処理するオブジェクト
	
	public BhNodeCategoryList(){};

	/**
	 * ノードテンプレートの配置情報が記されたファイルを読み込み
	 * テンプレートリストを作成する
	 * @return テンプレートの作成に成功した場合true
	 * */
	public boolean genNodeCategoryList() {

		Path filePath = Paths.get(Util.execPath, BhParams.Path.bhDefDir, BhParams.Path.TemplateListDir, BhParams.Path.nodeTemplateList);
		ScriptObjectMirror jsonObj = BhScriptManager.instance.parseJsonFile(filePath);
		templateTreeRoot = new TreeNode<>("root");
		return addChildren(jsonObj, templateTreeRoot, filePath.toString());
	}

	/**
	 * テンプレートツリーに子ノードを追加する.<br>
	 * 子ノードが葉だった場合、対応するBhNode があるかどうかを調べ, ない場合はfalse を返す
	 * @param jsonObj このオブジェクトのメンバーが追加すべき子ノードとなる
	 * @param parent 子ノードを追加したいノード
	 * @return 下位の葉ノードに対応するBhNode があった場合true
	 * */
	private boolean addChildren(ScriptObjectMirror jsonObj, TreeNode<String> parent, String fileName) {
		
		boolean bhNodeForLeafExists = true;
		for(String key : jsonObj.keySet()) {
		
			Object val = jsonObj.get(key);
			if (key.equals(BhParams.NodeTemplateList.keyNameCssClass)) {	//cssクラスのキー
				if (!(val instanceof String))
					continue;
				
				TreeNode<String> cssClass = new TreeNode<>(BhParams.NodeTemplateList.keyNameCssClass);
				cssClass.children.add(new TreeNode<>(val.toString()));
				parent.children.add(cssClass);
			}
			else if (key.equals(BhParams.NodeTemplateList.keyNameContents)) {	//ノードIDの配列のキー
				if (!(val instanceof ScriptObjectMirror))
					continue;
				
				if (((ScriptObjectMirror)val).isArray()) {
					TreeNode<String> contents = new TreeNode<>(BhParams.NodeTemplateList.keyNameContents);
					bhNodeForLeafExists &= addBhNodeID((ScriptObjectMirror)val, contents, fileName);
					parent.children.add(contents);
				}
			}
			else {	//カテゴリ名
				if (!(val instanceof ScriptObjectMirror))
					continue;
				
				if (!((ScriptObjectMirror)val).isArray()) {				
					TreeNode<String> child = new TreeNode<>(key);
					parent.children.add(child);
					bhNodeForLeafExists &= addChildren((ScriptObjectMirror)val, child, fileName);
				}
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
	private boolean addBhNodeID(ScriptObjectMirror bhNodeIDList, TreeNode<String> parent, String fileName) {

		boolean allBhNodeExist = true;
		for (Object bhNodeID : bhNodeIDList.values()) {
			if (String.class.isAssignableFrom(bhNodeID.getClass())) {	// 配列内の文字列だけをBhNode の IDとみなす

				String bhNodeIDStr = (String)bhNodeID;
				if (BhNodeTemplates.instance().bhNodeExists(bhNodeIDStr)) {	//IDに対応する BhNode がある
					parent.children.add(new TreeNode<>(bhNodeIDStr));
				}
				else {
					allBhNodeExist &= false;
					MsgPrinter.instance.ErrMsgForDebug(bhNodeIDStr + " に対応する " + BhParams.BhModelDef.elemNameNode + " が存在しません.\n" + "(" + fileName + ")");
				}
			}
		}
		return allBhNodeExist;
	}
	/**
	 * BhNode選択リストのルートノードを返す
	 * @return BhNode選択リストのルートノード
	 **/
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





















