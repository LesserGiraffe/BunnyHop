package pflab.bunnyHop.model;

import pflab.bunnyHop.model.templates.BhNodeTemplates;
import java.nio.file.Path;
import java.nio.file.Paths;

import jdk.nashorn.api.scripting.ScriptObjectMirror;

import pflab.bunnyHop.root.MsgPrinter;
import pflab.bunnyHop.common.BhParams;
import pflab.bunnyHop.common.TreeNode;
import pflab.bunnyHop.common.Util;
import pflab.bunnyHop.message.MsgSender;
import pflab.bunnyHop.configFileReader.BhScriptManager;

/**
 * BhNode のカテゴリ一覧を表示している部分のmodel
 * @author K.Koike
 * */
public class BhNodeCategoryList implements MsgSender {

	private TreeNode<String> templateTreeRoot;
	public BhNodeCategoryList(){};

	/**
	 * ノードテンプレートの配置情報が記されたファイルを読み込み
	 * テンプレートリストを作成する
	 * @return テンプレートの作成に成功した場合true
	 * */
	public boolean genNodeCategoryList() {

		Path filePath = Paths.get(Util.execPath, BhParams.Path.bhDefDir, BhParams.Path.TemplateListDir, BhParams.Path.nodeTemplateListJson);
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
}





















