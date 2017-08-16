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
package pflab.bunnyhop.control;

import javafx.fxml.FXML;
import javafx.scene.control.SplitPane;
import pflab.bunnyhop.message.MsgTransporter;
import pflab.bunnyhop.model.BhNodeCategoryList;
import pflab.bunnyhop.model.WorkspaceSet;
import pflab.bunnyhop.undo.UserOperationCommand;

/**
 * GUIの基底部分のコントローラ
 * @author K.Koike
 */
public class FoundationController {
	
	//View
	@FXML SplitPane horizontalSplitter;
	
	//Controller
	@FXML private BhBasicOperationController bhBasicOperationController;
	@FXML private WorkspaceSetController workspaceSetController;
	@FXML private BhNodeCategoryListController nodeCategoryListController;
		
	/**
	 * モデルとイベントハンドラをセットする
	 * @param wss ワークスペースセットのモデル
	 * @param nodeCategoryList ノードカテゴリリストのモデル
	 */
	public void init(WorkspaceSet wss, BhNodeCategoryList nodeCategoryList) {
		
		workspaceSetController.init(wss, bhBasicOperationController);
		nodeCategoryListController.init(nodeCategoryList);
		bhBasicOperationController.init(
			wss,
			workspaceSetController.getTabPane(), 
			nodeCategoryListController.getView());
		
		MsgTransporter.instance().setSenderAndReceiver(wss, workspaceSetController, new UserOperationCommand());
		MsgTransporter.instance().setSenderAndReceiver(nodeCategoryList, nodeCategoryListController, new UserOperationCommand());
	}
}
