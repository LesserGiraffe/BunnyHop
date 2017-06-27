package pflab.bunnyHop.model.connective;

import java.io.Serializable;
import java.util.List;
import java.util.Map;
import pflab.bunnyHop.ModelProcessor.BhModelProcessor;
import pflab.bunnyHop.root.MsgPrinter;
import pflab.bunnyHop.common.BhParams;
import pflab.bunnyHop.common.Util;
import pflab.bunnyHop.model.BhNode;
import pflab.bunnyHop.model.Imitatable;
import pflab.bunnyHop.model.ImitationInfo;
import pflab.bunnyHop.model.templates.BhNodeTemplates;
import pflab.bunnyHop.model.SyntaxSymbol;
import pflab.bunnyHop.undo.UserOperationCommand;

/**
 * 子ノードと接続されるノード
 * @author K.Koike
 * */
public class ConnectiveNode extends Imitatable implements Serializable{

	private Section childSection;							//!< セクションの集合 (ノード内部に描画されるもの)
	public ImitationInfo<ConnectiveNode> imitInfo;	//!< イミテーションノードに関連する情報がまとめられたオブジェクト

	/**
	 * コンストラクタ<br>
	 * ノード内部・外部とは描画位置のこと
	 * @param id ノードID (\<Node\> タグの bhID)
	 * @param name ノード名 (\<Node\> タグの name)
	 * @param childSection 子セクション
	 * @param scopeName イミテーションノードがオリジナルノードと同じスコープにいるかチェックする際の名前
	 * @param scriptNameOnMovedFromChildToWS ワークスペース移動時に実行されるスクリプトの名前
	 * @param scriptNameOnMovedToChild 子ノードとして接続されたときに実行されるスクリプトの名前
	 * @param imitTag_imitNodeID イミテーションタグとそれに対応するイミテーションノードIDのマップ
	 * @param canCreateImitManually このノードがイミテーション作成機能を持つ場合true
	 * */
	public ConnectiveNode(
			String id,
			String name,
			Section childSection,
			String scopeName,
			String scriptNameOnMovedFromChildToWS,
			String scriptNameOnMovedToChild,
			Map<String, String> imitTag_imitNodeID,
			boolean canCreateImitManually) {
		super(
			id,
			name,
			BhParams.BhModelDef.attrValueConnective,
			scriptNameOnMovedFromChildToWS,
			scriptNameOnMovedToChild);
		this.childSection = childSection;
		imitInfo = new ImitationInfo<>(imitTag_imitNodeID, canCreateImitManually, scopeName);
	}
	
	/**
	 * コピーコンストラクタ
	 * @param org コピー元オブジェクト
	 */
	private ConnectiveNode(ConnectiveNode org) {
		super(org);
	}

	@Override
	public ConnectiveNode copy(	UserOperationCommand userOpeCmd) {
		
		ConnectiveNode newNode = new ConnectiveNode(this);
		newNode.childSection = childSection.copy(userOpeCmd);
		newNode.childSection.setParent(newNode);
		newNode.imitInfo = new ImitationInfo<>(imitInfo, userOpeCmd, newNode);
		return newNode;
	}

	/**
	 * BhModelProcessor に自オブジェクトを渡す
	 * @param processor 自オブジェクトを渡す BhModelProcessorオブジェクト
	 * */
	@Override
	public void accept(BhModelProcessor processor) {
		processor.visit(this);
	}

	/**
	 * BhModelProcessor を子Section に渡す
	 * @param processor 子Section に渡す BhModelProcessor
	 * */
	public void introduceSectionsTo(BhModelProcessor processor) {
		childSection.accept(processor);
	}

	
	@Override 
	public ConnectiveNode createImitNode(UserOperationCommand userOpeCmd, String imitTag) {
			
		//イミテーションノード作成
		BhNode imitationNode = BhNodeTemplates.instance().genBhNode(imitInfo.getImitationID(imitTag), userOpeCmd);
		
		//オリジナルとイミテーションの関連付け
		ConnectiveNode connectiveImit = (ConnectiveNode)imitationNode; //ノードテンプレート作成時に整合性チェックしているのでキャストに問題はない
		imitInfo.addImitation(connectiveImit, userOpeCmd);
		connectiveImit.imitInfo.setOriginal(this, userOpeCmd);
		return connectiveImit;
	}

	@Override
	public ImitationInfo<ConnectiveNode> getImitationInfo() {
		return imitInfo;
	}
	
	@Override
	public ConnectiveNode getOriginalNode() {
		return imitInfo.getOriginal();
	}
	
	@Override
	public BhNode findOuterEndNode() {
		return childSection.findOuterEndNode();
	}
	
	/**
	 * モデルの構造を表示する
	 * @param depth 表示インデント数
	 * */
	@Override
	public void show(int depth) {

		String parentHashCode = "null";
		if (parentConnector != null)
			parentHashCode = parentConnector.hashCode() + "";

		String lastReplacedHash = "";
		if (getLastReplaced() != null)
			lastReplacedHash =  getLastReplaced().hashCode() + "";
		
		MsgPrinter.instance.MsgForDebug(indent(depth) + "<ConnectiveNode" + "  bhID=" + getID()  + "  parent=" + parentHashCode + "  > " + this.hashCode());
		MsgPrinter.instance.MsgForDebug(indent(depth+1) + "<" + "last replaced " + lastReplacedHash + "> ");
		MsgPrinter.instance.MsgForDebug(indent(depth+1) + "<" + "scopeName " + imitInfo.scopeName + "> ");
		MsgPrinter.instance.MsgForDebug(indent(depth+1) + "<" + "imitation" + "> ");
		imitInfo.getImitationList().forEach(imit -> {
			MsgPrinter.instance.MsgForDebug(indent(depth+2) + "imit " + imit.hashCode());
		});
		childSection.show(depth + 1);
	}
	
	@Override
	public void findSymbolInDescendants(String symbolName, int hierarchyLevel, boolean toBottom, List<SyntaxSymbol> foundSymbolList) {
		
		if (hierarchyLevel == 0) {
			if (Util.equals(getSymbolName(), symbolName)) {
				foundSymbolList.add(this);
			}
			if (!toBottom) {
				return;
			}
		}
		
		childSection.findSymbolInDescendants(symbolName, Math.max(0, hierarchyLevel-1), toBottom, foundSymbolList);
	}
}






















