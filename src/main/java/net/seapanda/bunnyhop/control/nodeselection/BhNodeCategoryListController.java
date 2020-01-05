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
package net.seapanda.bunnyhop.control.nodeselection;

import javafx.fxml.FXML;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TreeView;
import javafx.scene.layout.Region;
import net.seapanda.bunnyhop.common.constant.Rem;
import net.seapanda.bunnyhop.common.tools.MsgPrinter;
import net.seapanda.bunnyhop.message.BhMsg;
import net.seapanda.bunnyhop.message.MsgData;
import net.seapanda.bunnyhop.message.MsgProcessor;
import net.seapanda.bunnyhop.model.nodeselection.BhNodeCategoryList;
import net.seapanda.bunnyhop.view.ViewInitializationException;
import net.seapanda.bunnyhop.view.nodeselection.BhNodeCategoryListView;

/**
 * BhNode のカテゴリ選択画面のコントローラ
 * @author K.Koike
 */
public class BhNodeCategoryListController implements MsgProcessor {

	@FXML private ScrollPane nodeCategoryListViewBase;
	@FXML private TreeView<BhNodeCategoryListView.BhNodeCategory> categoryTree;
	private BhNodeCategoryList model;
	private BhNodeCategoryListView view;

	/**
	 * コントローラとビューの初期化を行う.
	 * @param categoryList ノードカテゴリリストのモデル
	 */
	public boolean init(BhNodeCategoryList categoryList) {

		model = categoryList;
		try {
			view = new BhNodeCategoryListView(categoryTree, model);
		}
		catch(ViewInitializationException e) {
			MsgPrinter.INSTANCE.errMsgForDebug(getClass().getSimpleName() + ".init\n" + e);
			return false;
		}
		nodeCategoryListViewBase.setMinWidth(Region.USE_PREF_SIZE);
		nodeCategoryListViewBase.widthProperty().addListener(
			(obs, oldVal, newVal) -> nodeCategoryListViewBase.setMinWidth(Rem.VAL * 3));

		return true;
	}

	/**
	 * カテゴリリストのビューを返す
	 * @return カテゴリリストのビュー
	 */
	public BhNodeCategoryListView getView() {
		return view;
	}

	@Override
	public MsgData processMsg(BhMsg msg, MsgData data) {
		throw new AssertionError("receive an unknown msg " + msg);
	}
}
