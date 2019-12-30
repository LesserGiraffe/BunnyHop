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
package net.seapanda.bunnyhop.model.imitation;

import java.util.Collection;

import net.seapanda.bunnyhop.model.node.BhNode;
import net.seapanda.bunnyhop.model.node.BhNodeID;
import net.seapanda.bunnyhop.model.node.BhNodeViewType;
import net.seapanda.bunnyhop.model.templates.BhNodeAttributes;
import net.seapanda.bunnyhop.undo.UserOperationCommand;

/**
 * イミテーションノードおよびオリジナルノードのインタフェース
 * @author K.Koike
 */
public abstract class Imitatable extends BhNode {

	protected Imitatable(BhNodeViewType type, BhNodeAttributes attributes) {
		super(type, attributes);
	}

	protected Imitatable(Imitatable org) {
		super(org);
	}

	/**
	 * イミテーションノードであった場合true を返す
	 * @return イミテーションノードであった場合true を返す
	 */
	public abstract boolean isImitationNode();

	/**
	 * 引数で指定したイミテーションIDに対応するイミテーションノードIDがある場合 true を返す
	 * @param imitID このイミテーションIDに対応するイミテーションノードIDがあるか調べる
	 * @return イミテーションノードIDが指定してある場合 true
	 */
	public abstract boolean imitationNodeExists(ImitationID imitID);

	/**
	 * 入れ替え用の既存のイミテーションノードを探す. <br>
	 * 見つからない場合は, 新規作成する.
	 * @param oldNode この関数が返すノードと入れ替えられる古いノード
	 * @param userOpeCmd undo用コマンドオブジェクト
	 * @return oldNodeと入れ替えるためのイミテーションノード
	 */
	public abstract Imitatable findExistingOrCreateNewImit(BhNode oldNode, UserOperationCommand userOpeCmd);

	/**
	 * 引数で指定したイミテーションタグに対応するイミテーションノードIDを返す
	 * @param imitID このイミテーションIDに対応するイミテーションノードIDを返す
	 * @return 引数で指定したコネクタ名に対応するイミテーションノードID
	 */
	public abstract BhNodeID getImitationNodeID(ImitationID imitID);

	/**
	 * イミテーションノードリストを取得する
	 * @return イミテーションノードリスト
	 */
	public abstract Collection<? extends Imitatable> getImitationList();
}



























