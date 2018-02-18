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
package net.seapanda.bunnyhop.view;

import net.seapanda.bunnyhop.message.BhMsg;
import net.seapanda.bunnyhop.message.MsgData;
import net.seapanda.bunnyhop.message.MsgTransporter;
import net.seapanda.bunnyhop.model.WorkspaceSet;

/**
 * ゴミ箱操作インタフェースクラス
 * @author Koike
 * */
public class TrashboxFacade {

	public static TrashboxFacade INSTANCE = new TrashboxFacade();

	private WorkspaceSet wss;	//ゴミ箱のビューにアクセスするためのワークスペースビュー.

	public void init(WorkspaceSet wss) {
		this.wss = wss;
	}

	/**
	 * 引数で指定した位置がゴミ箱領域内ならゴミ箱を開く.
	 * それ以外の場合, ゴミ箱を閉じる.
	 * @param sceneX Scene上でのX位置
	 * @param sceneY Scene上でのY位置
	 */
	public void openCloseTrashbox(double sceneX, double sceneY) {

		boolean inTrashboxArea = MsgTransporter.INSTANCE.sendMessage(
			BhMsg.IS_IN_TRASHBOX_AREA,
			new MsgData(sceneX, sceneY),
			wss).bool;

		openCloseTrashbox(inTrashboxArea);
	}

	/**
	 * ゴミ箱の開閉を行う
	 * @param open ゴミ箱を開く場合 true
	 * */
	public void openCloseTrashbox(boolean open) {

		MsgTransporter.INSTANCE.sendMessage(
			BhMsg.OPEN_TRASHBOX,
			new MsgData(open),
			wss);
	}

	/**
	 * 引数で指定した位置がゴミ箱領域内かどうか調べる
	 * @param sceneX Scene上でのX位置
	 * @param sceneY Scene上でのY位置
	 * @retval true ゴミ箱領域内
	 * @retval false ゴミ箱領域外
	 */
	public boolean isInTrashboxArea(double sceneX, double sceneY) {

		return MsgTransporter.INSTANCE.sendMessage(
					BhMsg.IS_IN_TRASHBOX_AREA,
					new MsgData(sceneX, sceneY),
					wss).bool;
	}

}








