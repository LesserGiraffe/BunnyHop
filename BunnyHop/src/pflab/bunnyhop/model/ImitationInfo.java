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

import pflab.bunnyhop.modelhandler.BhNodeHandler;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import pflab.bunnyhop.undo.UserOperationCommand;

/**
 * イミテーションノードの情報を管理するクラス
 * @author K.Koike
 */
public class ImitationInfo<T extends Imitatable> implements Serializable {
	
	public final boolean canCreateImitManually;	//!< このオブジェクトを持つノードがイミテーションノードの手動作成機能を持つ場合 true
	private final List<T> imitNodeList;					//!< このオブジェクトを持つノードから作成されたイミテーションノードの集合
	private T orgNode;									//!< このオブジェクトを持つノードがイミテーションノードの場合、そのオリジナルノードを保持する
	private final Map<String, String> imitTag_imitNodeID;	//!< イミテーションタグとそれに対応するイミテーションノードIDのマップ
	public final String scopeName;	//!< オリジナルノードと同じスコープにいるかチェックする際の名前
	
	/**
	 * コンストラクタ
	 * @param imitTag_imitNodeID イミテーションタグとそれに対応するイミテーションノードIDのマップ
	 * @param canCreateImitManually イミテーションノードの手動作成機能の有無
	 * @param scopeName オリジナルノードと同じスコープにいるかチェックする際の名前
	 **/	
	public ImitationInfo(
		Map<String, String> imitTag_imitNodeID, 
		boolean canCreateImitManually,
		String scopeName) {
		this.imitTag_imitNodeID = imitTag_imitNodeID;
		imitNodeList = new ArrayList<>();
		orgNode = null;		
		this.canCreateImitManually = canCreateImitManually;
		this.scopeName = scopeName;
	}
	
	/**
	 * コピーコンストラクタ
	 * @param org コピー元オブジェクト
	 * @param userOpeCmd undo用コマンドオブジェクト
	 * @param imitCreator このオブジェクトを持つノード
	 **/
	public ImitationInfo(ImitationInfo<T> org, UserOperationCommand userOpeCmd, T imitCreator) {
		imitTag_imitNodeID = org.imitTag_imitNodeID;
		canCreateImitManually = org.canCreateImitManually;
		imitNodeList = new ArrayList<>();	//元ノードをコピーしても、イミテーションノードとのつながりは無いようにする
		orgNode = null;
		scopeName = org.scopeName;
		if (org.isImitationNode()) {
			//イミテーションをコピーした場合, コピー元と同じオリジナルノードのイミテーションノードとする			
			T original = org.getOriginal();
			original.getImitationInfo().addImitation(imitCreator, userOpeCmd);
			setOriginal(original, userOpeCmd);
		}
	}
	
	/**
	 * イミテーションノードのオリジナルノードをセットする
	 * @param orgNode イミテーションノードの作成元ノード
	 * @param userOpeCmd undo用コマンドオブジェクト
	 **/
	public final void setOriginal(T orgNode, UserOperationCommand userOpeCmd) {
		userOpeCmd.<T>pushCmdSetOriginal(this, this.orgNode);
		this.orgNode = orgNode;
	}
	
	/**
	 * このオブジェクトを持つノードのオリジナルノードを返す
	 * @return このオブジェクトを持つノードのオリジナルノード
	 */
	public final T getOriginal() {
		return orgNode;
	}
	
	/**
	 * イミテーションノードを追加する
	 * @param imitNode 追加するイミテーションノード
	 * @param userOpeCmd undo用コマンドオブジェクト
	 */
	public void addImitation(T imitNode, UserOperationCommand userOpeCmd) {
		imitNodeList.add(imitNode);
		userOpeCmd.<T>pushCmdOfAddImitation(this, imitNode);
	}
	
	/**
	 * イミテーションノードを削除する
	 * @param imitNode 削除するイミテーションノード
	 * @param userOpeCmd undo用コマンドオブジェクト
	 */	
	public void removeImitation(T imitNode, UserOperationCommand userOpeCmd) {
		imitNodeList.remove(imitNode);
		userOpeCmd.<T>pushCmdOfRemoveImitation(this, imitNode);
	}
	
	/**
	 * イミテーションノードリストを取得する
	 * @return イミテーションノードリスト
	 */
	public List<T> getImitationList() {
		return imitNodeList;
	}
	
	/**
	 * 引数で指定したイミテーションタグに対応するイミテーションノードIDがある場合true を返す
	 * @param imitTag このイミテーションタグに対応するイミテーションノードIDがあるか調べる
	 * @return イミテーションノードIDが指定してある場合true
	 */
	public boolean hasImitationID(String imitTag) {
		return imitTag_imitNodeID.containsKey(imitTag);
	}
	
	/**
	 * 引数で指定したイミテーションタグに対応するイミテーションノードIDを返す
	 * @param imitTag このイミテーションタグに対応するイミテーションノードIDを返す
	 * @return 引数で指定したコネクタ名に対応するイミテーションノードID
	 */
	public String getImitationID(String imitTag) {
		String imitID = imitTag_imitNodeID.get(imitTag);
		assert imitID != null;
		return imitID;
	}
	
	/**
	 * イミテーションノードである場合 trueを返す
	 * @return イミテーションノードである場合 true
	 */
	public boolean isImitationNode() {
		return orgNode != null;
	}
	
	/**
	 * 全てのイミテーションノードを消す
	 * @param userOpeCmd undo用コマンドオブジェクト
	 */
	public void deleteAllImitations(UserOperationCommand userOpeCmd) {
		while (!imitNodeList.isEmpty()) {	//重複削除を避けるため, while で空になるまで消す
			
			Imitatable deleted = imitNodeList.get(0);
			if (!deleted.isInWorkspace())
				deleted.getOriginalNode().disconnectOrgImitRelation(deleted, userOpeCmd);	//WSに居ない場合は, 削除予定のノードなので, オリジナル-イミテーションの関係だけ消しておく.
			else
				BhNodeHandler.instance.deleteNode(deleted, userOpeCmd);
		}
	}
}
