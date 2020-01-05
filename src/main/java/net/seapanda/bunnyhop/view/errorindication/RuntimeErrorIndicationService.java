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
package net.seapanda.bunnyhop.view.errorindication;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import net.seapanda.bunnyhop.bhprogram.common.BhProgramException;
import net.seapanda.bunnyhop.model.syntaxsynbol.SyntaxSymbolID;
import net.seapanda.bunnyhop.view.node.BhNodeView;

/**
 * 実行時エラー情報の表示を担当するクラス
 * @author K.Koike
 */
public class RuntimeErrorIndicationService {

	public static final RuntimeErrorIndicationService INSTANCE = new RuntimeErrorIndicationService();
	Map<SyntaxSymbolID, BhNodeView> nodeIdToNodeView;	//!< シンボルIDとエラーの表示対象となるノードビューのマップ
	private List<RuntimeErrorIndicator> indicatorList = new ArrayList<>();


	private RuntimeErrorIndicationService() {}

	/**
	 * 引数で指定した例外に対するエラー情報を登録する
	 * @param exception この例外のエラー情報を登録する
	 */
	public synchronized void register(BhProgramException exception) {

	}

	/**
	 * 登録済みのエラー情報を削除する
	 */
	public synchronized void deleteAll() {

	}

	/**
	 * 登録済みのエラー情報を全て表示する
	 */
	public synchronized void displayAll() {

	}

	/**
	 * 登録済みのエラー情報を全て非表示にする
	 */
	public synchronized void hideAll() {

	}

	/**
	 * エラーの表示対象となる全ノードビューを登録する
	 * @param nodeViewList ノードIDとエラーの表示対象となるノードビューのマップ
	 */
	public synchronized void setNodeViewsForErrorIndication(Collection<BhNodeView> nodeViewList) {

		var nodeIdToNodeView = new HashMap<SyntaxSymbolID, BhNodeView>();
		nodeViewList.forEach(view -> nodeIdToNodeView.put(view.getModel().getSymbolID(), view));
		this.nodeIdToNodeView = nodeIdToNodeView;
	}
}























