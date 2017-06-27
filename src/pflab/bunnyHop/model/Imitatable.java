package pflab.bunnyHop.model;

import java.io.Serializable;
import pflab.bunnyHop.ModelProcessor.ImitationBuilder;
import pflab.bunnyHop.undo.UserOperationCommand;
/**
 * イミテーションノード操作のインタフェース<br>
 * @author K.Koike
 */
public abstract class Imitatable extends BhNode implements Serializable {

	public Imitatable(
		String bhID,
		String symbolName,
		String type,
		String scriptNameOnMovedFromChildToWS,
		String scriptNameOnMovedToChild) {
		super(
			bhID,
			symbolName,
			type,
			scriptNameOnMovedFromChildToWS,
			scriptNameOnMovedToChild);
	}
	
	/**
	 * コピーコンストラクタ
	 * @param org コピー元オブジェクト
	 */
	public Imitatable(Imitatable org) {
		super(org);
	}
	
	/**
	 * 引数で指定したイミテーションタグに対応したイミテーションノードを作成する
	 * @param userOpeCmd undo用コマンドオブジェクト
	 * @param imitTag このイミテーションタグに対応したイミテーションノードを作成する
	 * @return 作成されたイミテーションノード. イミテーションを持たないノードの場合nullを返す
	 */
	public abstract BhNode createImitNode(UserOperationCommand userOpeCmd, String imitTag);
	
	/**
	 * イミテーションノードであった場合true を返す
	 * @return イミテーションノードであった場合true を返す
	 */
	public boolean isImitationNode() {
		return getImitationInfo().isImitationNode();
	}
	
	/**
	 * イミテーションノード情報を格納するオブジェクトを返す
	 * @return イミテーション情報
	 */
	public abstract <T extends Imitatable> ImitationInfo<T> getImitationInfo();
	
	/**
	 * 入れ替え用の既存のイミテーションノードを探す. <br>
	 * 見つからない場合は, 新規作成する.
	 * @param oldNode この関数が返すノードと入れ替えられる古いノード
	 * @param userOpeCmd undo用コマンドオブジェクト
	 * @return oldNodeと入れ替えるためのイミテーションノード
	 */
	public Imitatable findExistingOrCreateNewImit(BhNode oldNode, UserOperationCommand userOpeCmd) {
		
		BhNode outerTailOfOldNode = oldNode.findOuterEndNode();
		for(Imitatable imit : getImitationInfo().getImitationList()) {
			//入れ替わるノードの外部末尾ノードが最後に入れ替わったノードの外部末尾ノードと一致するイミテーションノードを入れ替えイミテーションノードとする
			if  (imit.getLastReplaced() != null) {
				if(!imit.isInWorkspace() && imit.getLastReplaced().findOuterEndNode() == outerTailOfOldNode) {
					return imit;
				}
			}
		}
		
		ImitationBuilder imitBuilder = new ImitationBuilder(userOpeCmd);
		accept(imitBuilder);
		return imitBuilder.getTopImitation();
	}
	
	/**
	 * オリジナルノードと同じスコープにいるかチェックする
	 * @return スコープが定義してあるノードでかつ, オリジナルノードと同じスコープに居ないイミテーションノードである場合trueを返す
	 */
	public boolean isUnscoped() {
		
		if (!isImitationNode() || 
			getImitationInfo().scopeName.isEmpty() ||
			getState() != BhNode.State.CHILD)	//トップノードはスコープ判定対象外
			return false;
		
		SyntaxSymbol scope = getOriginalNode().findSymbolInAncestors(getImitationInfo().scopeName, 1, true);
		if (scope == null)	// スコープが見つからない場合, 可視範囲制限は無いものとする. => global scope
			return false;
		
		return !this.isDescendantOf(scope);
	}
	
	/**
	 * オリジナル - イミテーションの関係を削除する
	 * @param deleted 削除されるイミテーションノード
	 * @param userOpeCmd undo用コマンドオブジェクト
	 */
	public void disconnectOrgImitRelation(Imitatable deleted, UserOperationCommand userOpeCmd) {
		getImitationInfo().removeImitation(deleted, userOpeCmd);
		deleted.getImitationInfo().setOriginal(null, userOpeCmd);
	}
	
	@Override
	public boolean isRemovable() {				
		if (parentConnector == null)
			return false;
		
		if (isDefaultNode())	//デフォルトノードは移動不可
			return false;
		
		return !parentConnector.isFixed();
	}
	
	@Override
	public boolean canBeReplacedWith(BhNode node) {
	
		if (getState() != BhNode.State.CHILD)
			return false;
		
		if (findRootNode().getState() == BhNode.State.ROOT_DANGLING)
			return false;
		
		if (node.isDescendantOf(this) || this.isDescendantOf(node))	//同じtree に含まれている場合置き換え不可
			return false;		
		
		do {
			if (!(node instanceof Imitatable))
				break;
			
			Imitatable imitNode = (Imitatable)node;
			if (!imitNode.isImitationNode())
				break;
			
			if (imitNode.getImitationInfo().scopeName.isEmpty())
				break;
			
			SyntaxSymbol scope = imitNode.getOriginalNode().findSymbolInAncestors(imitNode.getImitationInfo().scopeName, 1, true);
			if (scope == null)	//可視範囲制限なし
				break;
			
			if (!this.isDescendantOf(scope))
				return false;
		}
		while(false);
		
		return parentConnector.canConnectedNodeBeReplacedWith(node);
	}
}
