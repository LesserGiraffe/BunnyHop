package pflab.bunnyHop.model;

import pflab.bunnyHop.modelHandler.BhNodeHandler;
import java.io.Serializable;
import javax.script.Bindings;
import javax.script.CompiledScript;
import javax.script.ScriptException;
import pflab.bunnyHop.ModelProcessor.ImitationReplacer;
import pflab.bunnyHop.ModelProcessor.NodeDeleter;
import pflab.bunnyHop.root.MsgPrinter;
import pflab.bunnyHop.common.BhParams;
import pflab.bunnyHop.common.Util;
import pflab.bunnyHop.message.MsgSender;
import pflab.bunnyHop.message.MsgTransporter;
import pflab.bunnyHop.model.connective.ConnectiveNode;
import pflab.bunnyHop.model.connective.Connector;
import pflab.bunnyHop.configFileReader.BhScriptManager;
import pflab.bunnyHop.undo.UserOperationCommand;
import pflab.bunnyHop.view.BhNodeView;

/**
 * ノードの基底クラス
 * @author K.Koike
 */
public abstract class BhNode extends SyntaxSymbol implements MsgSender, Serializable {

	private final String bhID; //!< ノードID (\<Node\> タグの bhID)
	protected Connector parentConnector;	//!< このノードを繋いでいるコネクタ
	protected Workspace workspace;	//!< このノードがあるWorkSpace.
	private final String scriptNameOnMovedFromChildToWS;	//子ノードからワークスペースに移されたときに実行されるスクリプトの名前
	private final String scriptNameOnMovedToChild;	//!< ワークスペースもしくは, 子ノードから子ノードに移されたときに実行されるスクリプトの名前
	public final String type;	//!< ノードのタイプ (connective, void, textField, ...)
	private BhNode lastReplaced;	//!< 最後にこのノードと入れ替わったノード
	transient protected Bindings scriptScope;	//!< Javascript実行時の変数スコープ
	
	/**
	 * BhNode がとり得る状態
	 */
	public enum State {
		ROOT_DIRECTLY_UNDER_WS,	//!< ワークスペース直下のルートノード
		ROOT_DANGLING,			//!< ワークスペース直下に無いルートノード (宙ぶらりん状態)
		CHILD,					//!< 子ノード (ルートが宙ぶらりんかどうかは問わない)
	}
	
	/**
	 * 手動で子ノードからの取り外しができる場合 trueを返す<br>
	 * ルートノードの場合falseが返る
	 * @return 手動で子ノードからの取り外しができる場合 true
	 */
	public abstract boolean isRemovable();
	
	/**
	 * 引数のノードをこのノードの代わりに置き換えることができる場合 true を返す
	 * @param node 置き換え対象のBhNode
	 * @return このノードの代わりに置き換えられる場合 true
	 * */
	public abstract boolean canBeReplacedWith(BhNode node);
	
	/**
	 * このノードがイミテーションノードだった場合, そのオリジナルノードを返す<br>
	 * イミテーションノードで無かった場合 null を返す
	 * @return このノードのオリジナルノード. このノードがイミテーションノードで無い場合は null を返す
	 */
	public abstract Imitatable getOriginalNode();
	
	/**
	 * 最後尾に繋がる外部ノードを探す
	 * @return 最後尾に繋がる外部ノード
	 */
	public abstract BhNode findOuterEndNode();
	
	/**
	 * このノード以下のノードツリーのコピーを作成し返す
	 * @param userOpeCmd undo用コマンドオブジェクト
	 * @return このノード以下のノードツリーのコピー
	 */
	public abstract BhNode copy(UserOperationCommand userOpeCmd);
	
	/**
	 * コンストラクタ
	 * @param bhID ノードID (\<Node\> タグの bhID)
	 * @param symbolName 終端, 非終端記号名 (\<Node\> タグの name)
	 * @param type xml のtype属性
	 * @param scriptNameOnMovedFromChildToWS 子ノードからワークスペースに移されたときに実行されるスクリプトの名前
	 * @param scriptNameOnMovedToChild ワークスペースもしくは, 子ノードから子ノードに移されたときに実行されるスクリプトの名前
	 * */
	protected BhNode(
		String bhID,
		String symbolName,
		String type,
		String scriptNameOnMovedFromChildToWS,
		String scriptNameOnMovedToChild) {
		super(symbolName);
		this.bhID = bhID;
		this.type = type;
		this.scriptNameOnMovedFromChildToWS = scriptNameOnMovedFromChildToWS;
		this.scriptNameOnMovedToChild = scriptNameOnMovedToChild;
	}
	
	/**
	 * コピーコンストラクタ
	 * @param org コピー元オブジェクト
	 */
	protected BhNode(BhNode org) {
		super(org);
		bhID = org.bhID;
		parentConnector = null;
		workspace = null;
		scriptNameOnMovedFromChildToWS = org.scriptNameOnMovedFromChildToWS;
		scriptNameOnMovedToChild = org.scriptNameOnMovedToChild;
		type = org.type;
		lastReplaced = null;
		scriptScope = null;
	}
	
	public String getID() {
		return bhID;
	}
		
	/**
	 * ノードをワークスペース上から削除する
	 * @param userOpeCmd undo用コマンドオブジェクト
	 */
	public void delete(UserOperationCommand userOpeCmd) {
		accept(new NodeDeleter(userOpeCmd));
	}
	
	/**
	 * newBhNodeとこのノードを入れ替える<br>
	 * @param newNode このノードと入れ替えるノード
	 * @param userOpeCmd undo用コマンドオブジェクト
	 * */
	public void replacedWith(BhNode newNode, UserOperationCommand userOpeCmd) {
		
		setLastReplaced(newNode, userOpeCmd);
		parentConnector.connectNode(newNode, userOpeCmd);	//model 更新
		newNode.accept(new ImitationReplacer(userOpeCmd, this));		
	}

	/**
	 * このノードをモデルツリーから取り除く. ノードの削除は行わない.
	 * @param userOpeCmd undo用コマンドオブジェクト
	 * @return このノードを取り除いた結果, 新しくできたノード
	 * */
	public BhNode remove(UserOperationCommand userOpeCmd) {
		return parentConnector.remove(userOpeCmd);
	}
	
	/**
	 * ノードの状態を取得する
	 * @retval State.ROOT_DANGLING ルートノードであるがワークスペースには属していない
	 * @retval State.ROOT_DIRECTLY_UNDER_WS ワークスペースに属しているルートノード
	 * @retval State.CHILD 子ノード. ワークスペースに属していてもいなくても子ノードならばこの値が返る
	 */
	public State getState() {
		
		if(parentConnector == null) {
			//if (workspace == null)
			//	return State.ROOT_DANGLING;
			
			if(workspace.containsAsRoot(this))
				return State.ROOT_DIRECTLY_UNDER_WS;
			else
				return State.ROOT_DANGLING;
		}
		else {
			assert workspace.containsAsRoot(this) == false;
			return State.CHILD;
		}
	}
	
	/**
	 * 接続されるコネクタを登録する
	 * @param parentConnector このノードと繋がるコネクタ
	 * */
	public void setParentConnector(Connector parentConnector) {
		this.parentConnector = parentConnector;
	}

	/**
	 * このノードの親コネクタを返す
	 * @return 親コネクタ. 親コネクタが無い場合は, null を返す.
	 */
	public Connector getParentConnector() {
		return parentConnector;
	}
	
	/**
	 * このノードがあるワークスペースを登録する
	 * @param workspace このノードを直接保持するワークスペース
	 * @param userOpeCmd undo用コマンドオブジェクト
	 * */
	public void setWorkspace(Workspace workspace, UserOperationCommand userOpeCmd) {
		userOpeCmd.pushCmdOfSetWorkspace(this.workspace, this);
		this.workspace = workspace;
	}

	/**
	 * このノードがあるワークスペースを登録する
	 * @return このノードがあるワークスペース
	 * */
	public Workspace getWorkspace() {
		return workspace;
	}
	
	/**
	 * このノードがワークスペースに存在するノードである場合 true を返す
	 * @return このノードがワークスペースに存在するノードである場合 true
	 */
	public boolean isInWorkspace() {	
		return workspace != null;
	}
	
	/**
	 * このノードと入れ替わったノードをセットする
	 * @param lastReplaced このノードと入れ替わったノード
	 * @param userOpeCmd undo用コマンドオブジェクト
	 */
	public void setLastReplaced(BhNode lastReplaced, UserOperationCommand userOpeCmd) {
		userOpeCmd.pushCmdOfSetLastReplaced(this.lastReplaced, this);
		this.lastReplaced = lastReplaced;
	}
	
	/**
	 * このノードと入れ替わったノードを取得する
	 * @return 最後にこのオブジェクトと入れ替わったノード
	 */
	public BhNode getLastReplaced() {
		return lastReplaced;
	}
	
	/**
	 * このノードが選択されているかどうかを調べる
	 * @return このノードが選択されている場合 trueを返す
	 */
	public boolean isSelected() {
		if (workspace == null)
			return false;
		return workspace.getSelectedNodeList().contains(this);
	}
	
	/**
	 * このBhNodeがデフォルトノードの場合true を返す
	 * @return このBhNodeがデフォルトノードの場合true
	 */
	public boolean isDefaultNode() {
		return parentConnector.defaultNodeID.equals(bhID);
	}

	/**
	 * このノードの親となるノードを返す<br> ルートノードの場合は null を返す
	 * @return このノードの親となるノード
	 * */
	public ConnectiveNode findParentNode() {

		if (parentConnector == null)
			return null;

		return parentConnector.getParentNode();
	}

	/**
	 * このノードのルートであるノードを返す <br> ルートノードの場合は自身を帰す  
	 * @return このノードのルートノード
	 * */
	public BhNode findRootNode() {

		if (parentConnector == null)
			return this;

		return parentConnector.getParentNode().findRootNode();
	}
	
	@Override
	public boolean isDescendantOf(SyntaxSymbol ancestor) {

		if (this == ancestor)
			return true;

		if (parentConnector == null)
			return false;

		return parentConnector.isDescendantOf(ancestor);
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
			
		if (parentConnector == null)
			return null;

		return parentConnector.findSymbolInAncestors(symbolName, Math.max(0, hierarchyLevel-1), toTop);
	}
	
	/**
	 * スクリプト実行時のスコープ変数を登録する
	 * @param view スクリプト実行時のスコープとして登録するBhNodeView
	 */
	public void setScriptScope(BhNodeView view) {
		scriptScope = BhScriptManager.instance.createScriptScope();
		scriptScope.put(BhParams.JsKeyword.keyBhThis, this);
		scriptScope.put(BhParams.JsKeyword.keyBhNodeView, view);
		scriptScope.put(BhParams.JsKeyword.keyBhNodeHandler, BhNodeHandler.instance);
		scriptScope.put(BhParams.JsKeyword.keyBhMsgTransporter, MsgTransporter.instance());		
		scriptScope.put(BhParams.JsKeyword.keyBhCommon, BhScriptManager.instance.getCommonJsObj());
	}
	
	/**
	 * 子ノードに移ったときのスクリプトを実行する
	 * @param oldParent 移る前に接続されていた親. ワークスペースから子ノードに移動したときはnull.
	 * @param oldRoot 移る前に所属していたノードツリーのルートノード. ワークスペースから子ノードに移動したときは, このオブジェクト.
	 * @param oldReplaced 元々子ノードとしてつながっていたノード
	 * @param userOpeCmd undo用コマンドオブジェクト
	 * */
	public void execScriptOnMovedToChild(
		ConnectiveNode oldParent,
		BhNode oldRoot, 
		BhNode oldReplaced,
		UserOperationCommand userOpeCmd) {

		CompiledScript onMovedToChild = BhScriptManager.instance.getCompiledScript(scriptNameOnMovedToChild);
		if (onMovedToChild == null)
			return;
		
		scriptScope.put(BhParams.JsKeyword.keyBhOldParent, oldParent);
		scriptScope.put(BhParams.JsKeyword.keyBhOldRoot, oldRoot);
		scriptScope.put(BhParams.JsKeyword.keyBhReplacedOldNode, oldReplaced);
		scriptScope.put(BhParams.JsKeyword.keyBhUserOpeCmd, userOpeCmd);
		try {
			onMovedToChild.eval(scriptScope);
		} 
		catch (ScriptException e) {
			MsgPrinter.instance.ErrMsgForDebug("BhNode.execOnMovedToChildScript   " + scriptNameOnMovedToChild + "\n" + e.getMessage() + "\n");
		}
	}
	
	/**
	 * 子ノードからワークスペースに移ったときのスクリプトを実行する
	 * @param oldParent 移る前に接続されていた親
	 * @param oldRoot 移る前に所属していたルートノード
	 * @param newReplaced WSに移る際, このノードの替わりにつながったノード
	 * @param manuallyRemoved D&Dで子ノードからワークスペースに移された場合true
	 * @param userOpeCmd undo用コマンドオブジェクト
	 */
	public void execScriptOnMovedFromChildToWS(
		ConnectiveNode oldParent, 
		BhNode oldRoot, 
		BhNode newReplaced,
		Boolean manuallyRemoved,
		UserOperationCommand userOpeCmd) {
		
		CompiledScript onMovedFromChildToWS = BhScriptManager.instance.getCompiledScript(scriptNameOnMovedFromChildToWS);
		if (onMovedFromChildToWS == null)
			return;
		
		scriptScope.put(BhParams.JsKeyword.keyBhOldParent, oldParent);
		scriptScope.put(BhParams.JsKeyword.keyBhOldRoot, oldRoot);
		scriptScope.put(BhParams.JsKeyword.keyBhReplacedNewNode, newReplaced);
		scriptScope.put(BhParams.JsKeyword.keyBhManuallyReplaced, manuallyRemoved);
		scriptScope.put(BhParams.JsKeyword.keyBhUserOpeCmd, userOpeCmd);
		try {
			onMovedFromChildToWS.eval(scriptScope);
		} catch (ScriptException e) {
			MsgPrinter.instance.ErrMsgForDebug("BhNode.execOnMovedFromChildToWSScript   " + scriptNameOnMovedFromChildToWS + "\n" + e.getMessage() + "\n");
		}		
	}
}










