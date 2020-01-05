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
package net.seapanda.bunnyhop.view.node.part;

import java.io.IOException;
import java.util.Optional;

import javafx.event.ActionEvent;
import javafx.scene.control.Button;
import net.seapanda.bunnyhop.common.Vec2D;
import net.seapanda.bunnyhop.common.constant.BhParams;
import net.seapanda.bunnyhop.common.tools.MsgPrinter;
import net.seapanda.bunnyhop.message.MsgService;
import net.seapanda.bunnyhop.model.node.imitation.Imitatable;
import net.seapanda.bunnyhop.model.node.imitation.ImitationID;
import net.seapanda.bunnyhop.modelprocessor.ImitationBuilder;
import net.seapanda.bunnyhop.modelservice.BhNodeHandler;
import net.seapanda.bunnyhop.modelservice.ModelExclusiveControl;
import net.seapanda.bunnyhop.root.BunnyHop;
import net.seapanda.bunnyhop.undo.UserOperationCommand;

/**
 * イミテーション作成ボタン
 * @author K.Koike
 */
public final class ImitationCreationButton extends Button {

	/**
	 * イミテーション作成ボタンを作成する
	 * @param model ボタンを持つビューに対応するノード
	 * @param buttonStyle ボタンに適用するスタイル
	 */
	public static Optional<ImitationCreationButton> create(
		Imitatable node, BhNodeViewStyle.Button buttonStyle) {

		try {
			return Optional.of(new ImitationCreationButton(node, buttonStyle));
		}
		catch (IOException | ClassCastException e) {
			MsgPrinter.INSTANCE.errMsgForDebug(ImitationCreationButton.class.getSimpleName()
				+ "  failed to create a ImitationCreationButton\n" + e);
			return Optional.empty();
		}
	}

	private ImitationCreationButton(Imitatable node, BhNodeViewStyle.Button buttonStyle)
		throws IOException, ClassCastException {

		ComponentLoader.loadButton(BhParams.Path.IMIT_BUTTON_FXML, this, buttonStyle);
		setOnAction(event -> ImitationCreationButton.onImitCreating(event, node));
	}

	/**
	 * イミテーション作成時のイベントハンドラ
	 */
	private static void onImitCreating(ActionEvent event, Imitatable node) {

		if (MsgService.INSTANCE.isTemplateNode(node))
			return;

		ModelExclusiveControl.INSTANCE.lockForModification();
		try {
			createImitationNode(node);
			event.consume();
		}
		finally {
			ModelExclusiveControl.INSTANCE.unlockForModification();
		}
	}

	/**
	 * @param model 作成するイミテーションのオリジナルノード
	 */
	private static void createImitationNode(Imitatable node) {

		UserOperationCommand userOpeCmd = new UserOperationCommand();
		Vec2D pos = MsgService.INSTANCE.getPosOnWS(node);
		double x = pos.x + BhParams.LnF.REPLACED_NODE_SHIFT;
		double y = pos.y + BhParams.LnF.REPLACED_NODE_SHIFT;
		Imitatable imitNode = ImitationBuilder.build(node, ImitationID.MANUAL, true, userOpeCmd);
		BhNodeHandler.INSTANCE.addRootNode(node.getWorkspace(), imitNode, x, y, userOpeCmd);
		BunnyHop.INSTANCE.pushUserOpeCmd(userOpeCmd);
	}
}
