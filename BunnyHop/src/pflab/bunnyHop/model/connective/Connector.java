package pflab.bunnyHop.model.connective;

import java.io.Serializable;
import java.util.List;
import javax.script.Bindings;
import javax.script.CompiledScript;
import javax.script.ScriptException;
import pflab.bunnyHop.modelProcessor.BhModelProcessor;
import pflab.bunnyHop.modelProcessor.NodeMVCBuilder;
import pflab.bunnyHop.root.MsgPrinter;
import pflab.bunnyHop.common.BhParams;
import pflab.bunnyHop.common.Showable;
import pflab.bunnyHop.common.Util;
import pflab.bunnyHop.message.MsgTransporter;
import pflab.bunnyHop.model.BhNode;
import pflab.bunnyHop.model.templates.BhNodeTemplates;
import pflab.bunnyHop.model.SyntaxSymbol;
import pflab.bunnyHop.modelHandler.BhNodeHandler;
import pflab.bunnyHop.configFileReader.BhScriptManager;
import pflab.bunnyHop.undo.UserOperationCommand;

/**
 * ノードとノードをつなぐ部分のクラス
 * @author K.Koike
 * */
public class Connector extends SyntaxSymbol implements Cloneable, Showable, Serializable {

	private final String id; 				//!< コネクタID (\<Connector\> タグの bhID)
	public final String defaultNodeID; 		//!< ノードが取り外されたときに変わりに繋がるノードのID (\<Connector\> タグの bhID)
	public final String initNodeID;			//!< 最初に接続されているノードのID
	private BhNode connectedNode;			//!< 接続中のノード<br> null となるのは、テンプレート構築中とClone メソッドの一瞬のみ
	private ConnectorSection parent;	//!< このオブジェクトを保持する ConnectorSection オブジェクト
	private final boolean fixed;	//!< このコネクタにつながるBhNodeが手動で取り外しや入れ替えができない場合true
	private boolean outer = false;	//!< 外部描画ノードを接続するコネクタの場合true
	private String imitTag;	//!< イミテーション生成時のタグ
	private final String scriptNameOnReplaceabilityChecked;	//!< ノードを入れ替え可能かどうかチェックするスクリプトの名前
	transient protected Bindings scriptScope;

	/**
	 * visitor に次の走査対象に渡す
	 * @param visitor 走査対象を渡すvisitor
	 * */
	@Override
	 public void accept(BhModelProcessor visitor) {
		visitor.visit(this);
	}

	/**
	 * コンストラクタ
	 * @param id コネクタID (\<Connector\> タグの bhID)
	 * @param defaultNodeID ノードが取り外されたときに変わりに繋がるノードのID
	 * @param initialNodeID 最初に接続されているノードのID
	 * @param fixed このコネクタにつながるノードの入れ替えや取り外しができない場合true
	 * @param scriptNameOnReplaceabilityChecked ノードを入れ替え可能かどうかチェックするスクリプトの名前
	 * */
	public Connector(
		String id,
		String defaultNodeID,
		String initialNodeID,
		boolean fixed,
		String scriptNameOnReplaceabilityChecked) {
		super("");
		this.id = id;
		this.scriptNameOnReplaceabilityChecked = scriptNameOnReplaceabilityChecked;
		this.defaultNodeID = defaultNodeID;
		this.fixed = fixed;
		if (initialNodeID.isEmpty())
			this.initNodeID = defaultNodeID;
		else
			this.initNodeID = initialNodeID;
	}
	
	/**
	 * コピーコンストラクタ
	 * @param org コピー元オブジェクト
	 * @param name コネクタ名
	 * @param imitTag イミテーションノードの接続先識別タグ
	 * @param isOuter 外部描画フラグ
	 * @param parent 親コネクタセクション
	 */
	private Connector(
		Connector org,
		String name,
		String imitTag,
		ConnectorSection parent) {
		
		super(name);
		id = org.id;
		defaultNodeID = org.defaultNodeID;
		initNodeID = org.initNodeID;
		scriptNameOnReplaceabilityChecked = org.scriptNameOnReplaceabilityChecked;
		fixed = org.fixed;
		this.imitTag = imitTag;
		this.parent = parent;
	}
	
	/**
	 * このノードのコピーを作成して返す
	 * @param userOpeCmd undo用コマンドオブジェクト
	 * @param name コネクタ名
	 * @param imitTag イミテーションノードの接続先識別タグ
	 * @param parent 親コネクタセクション
	 * @return このノードのコピー
	 */
	public Connector copy(
		UserOperationCommand userOpeCmd,
		String name,
		String imitTag,
		ConnectorSection parent) {
		
		Connector newConnector = 
			new Connector(
				this,
				name,
				imitTag,
				parent);
		BhNode newNode = connectedNode.copy(userOpeCmd);
		newConnector.connectNode(newNode, null);
		return newConnector;
	}

	/**
	 * BhModelProcessor を connectedNode に渡す
	 * @param processor connectedNode に渡す BhModelProcessor
	 * */
	public void introduceConnectedNodeTo(BhModelProcessor processor) {
		connectedNode.accept(processor);
	}

	/**
	 * ノードを接続する
	 * @param node 接続されるノード<br> null はダメ
	 * @param userOpeCmd undo用コマンドオブジェクト
	 * */
	public final void connectNode(BhNode node, UserOperationCommand userOpeCmd) {

		assert node != null;	//null接続はダメ
		
		if (userOpeCmd != null)
			userOpeCmd.pushCmdOfConnectNode(connectedNode, this);

		if(connectedNode != null)
			connectedNode.setParentConnector(null);	//古いノードから親を消す
		connectedNode = node;
		node.setParentConnector(this);	//新しいノードの親をセット
	}
	
	/**
	 * このコネクタの親となるノードを返す
	 * @return このコネクタの親となるノード
	 * */
	public ConnectiveNode getParentNode() {
		return parent.findParentNode();
	}

	/**
	 * このコネクタにつながるノードの入れ替えと取り外しができない場合trueを返す
	 * @return このコネクタにつながるノードの入れ替えと取り外しができない場合true
	 */
	public boolean isFixed() {
		return fixed;
	}
	
	/**
	 * 引数で指定したノードが現在つながっているノードと入れ替え可能かどうか調べる
	 * @param newNode 新しく入れ替わるノード
	 * @return 引数で指定したノードが現在つながっているノードと入れ替え可能である場合, true を返す
	 */
	public boolean canConnectedNodeBeReplacedWith(BhNode newNode) {

		if (fixed)
			return false;
		
		CompiledScript onReplaceabilityChecked = BhScriptManager.instance.getCompiledScript(scriptNameOnReplaceabilityChecked);
		if (onReplaceabilityChecked == null)
			return false;
		
		scriptScope.put(BhParams.JsKeyword.keyBhNewNodeID, newNode.getID());
		scriptScope.put(BhParams.JsKeyword.keyBhOldNodeID, connectedNode.getID());
		scriptScope.put(BhParams.JsKeyword.keyBhReplacedNewNode, newNode);
		scriptScope.put(BhParams.JsKeyword.keyBhReplacedOldNode, connectedNode);
		Object canBeReplaced;
		try {
			canBeReplaced = onReplaceabilityChecked.eval(scriptScope);
		} catch (ScriptException e) {
			MsgPrinter.instance.ErrMsgForDebug("Connector.isReplacable   " + scriptNameOnReplaceabilityChecked + "\n" + e.getMessage() + "\n");
			return false;
		}
		if (canBeReplaced instanceof Boolean)
			return (Boolean)canBeReplaced;

		return false;
	}
	
	/**
	 * 現在繋がっているノードを取り除く
	 * @param userOpeCmd undo用コマンドオブジェクト
	 * @return 現在繋がっているノードを取り除いた結果, 新しくできたノード
	 * */
	public BhNode remove(UserOperationCommand userOpeCmd) {

		assert connectedNode != null;
		
		BhNode newNode = BhNodeTemplates.instance().genBhNode(defaultNodeID, userOpeCmd);	//デフォルトノードを作成		
		NodeMVCBuilder mvcBuilder = new NodeMVCBuilder(NodeMVCBuilder.ControllerType.Default, userOpeCmd);
		newNode.accept(mvcBuilder);	//MVC構築
		connectedNode.replacedWith(newNode, userOpeCmd);
		return newNode;
	}

	public String getID() {
		return id;
	}
	
	/**
	 * このコネクタに接続されているBhNode を返す
	 * @return このコネクタに接続されているBhNode
	 */
	public BhNode getConnectedNode() {
		return connectedNode;
	}
	
	/**
	 * スクリプト実行時のスコープ変数を登録する
	 */
	public final void setScriptScope() {
		scriptScope = BhScriptManager.instance.createScriptScope();
		scriptScope.put(BhParams.JsKeyword.keyBhThis, this);
		scriptScope.put(BhParams.JsKeyword.keyBhNodeHandler, BhNodeHandler.instance);
		scriptScope.put(BhParams.JsKeyword.keyBhMsgTransporter, MsgTransporter.instance());
		scriptScope.put(BhParams.JsKeyword.keyBhCommon, BhScriptManager.instance.getCommonJsObj());
	}

	/**
	 * イミテーション作成時のタグを取得する
	 * @return イミテーション作成時のタグ
	 */
	public String getImitationTag() {
		return imitTag;
	}
	
	/**
	 * 外部描画ノードかどうかを示すフラグをセットする
	 * @param outer このコネクタが外部描画ノードを接続する場合true
	 */
	public void setOuterFlag(boolean outer) {
		this.outer = outer;
	}
	
	/**
	 * 外部描画ノードをつなぐコネクタかどうかを調べる
	 * @return 外部描画ノードをコネクタの場合true
	 * */
	public boolean isOuter() {
		return outer;
	}
	
	@Override
	public void findSymbolInDescendants(int hierarchyLevel, boolean toBottom, List<SyntaxSymbol> foundSymbolList, String... symbolNames) {
		
		if (hierarchyLevel == 0) {
			for (String symbolName : symbolNames) {
				if (Util.equals(getSymbolName(), symbolName)) {
					foundSymbolList.add(this);
				}
			}
			if (!toBottom) {
				return;
			}
		}
		
		connectedNode.findSymbolInDescendants(Math.max(0, hierarchyLevel-1), toBottom, foundSymbolList, symbolNames);
	}
	
	@Override
	public SyntaxSymbol findSymbolInAncestors(String symbolName, int hierarchyLevel, boolean toTop) {

		if (hierarchyLevel == 0) {
			if (Util.equals(getSymbolName(), symbolName)) {
				return this;
			}
			if (!toTop) {
				return null;
			}
		}
			
		return parent.findSymbolInAncestors(symbolName, Math.max(0, hierarchyLevel-1), toTop);
	}
	
	@Override
	public boolean isDescendantOf(SyntaxSymbol ancestor) {

		if (this == ancestor)
			return true;

		return parent.isDescendantOf(ancestor);
	}
	
	/**
	 * モデルの構造を表示する
	 * @param depth 表示インデント数
	 * */
	@Override
	public void show(int depth) {
		MsgPrinter.instance.MsgForDebug(indent(depth) + "<Connector" + " bhID=" + id + " nodeID=" + connectedNode.getID() + "  parent=" + parent.hashCode() + "> " + this.hashCode());
		connectedNode.show(depth + 1);
	}
}























