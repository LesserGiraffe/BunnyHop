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
package pflab.bunnyhop.view;

import java.io.IOException;
import java.nio.file.Path;
import javafx.fxml.FXMLLoader;
import javafx.scene.control.Button;
import pflab.bunnyhop.modelprocessor.ImitationBuilder;
import pflab.bunnyhop.root.MsgPrinter;
import pflab.bunnyhop.common.BhParams;
import pflab.bunnyhop.message.BhMsg;
import pflab.bunnyhop.message.MsgData;
import pflab.bunnyhop.message.MsgTransporter;
import pflab.bunnyhop.model.TextNode;
import pflab.bunnyhop.model.connective.ConnectiveNode;
import pflab.bunnyhop.modelhandler.BhNodeHandler;
import pflab.bunnyhop.configfilereader.FXMLCollector;
import pflab.bunnyhop.root.BunnyHop;
import pflab.bunnyhop.undo.UserOperationCommand;

/**
 *イミテーション作成機能を持つノードビューであることを示すインタフェース
 * @author K.Koike
 * */
public interface ImitationCreator {
	
	/**
	 *イミテーションノード作成時にクリックするボタンを返す
	 * @return イミテーションノード作成時にクリックするボタン
	 * */
	public Button imitCreateButton();
	
	/**
	 * このクラス以外からの呼び出し禁止
	 * @param model 作成するイミテーションのオリジナルノード
	 */
	default void imitHandler(pflab.bunnyhop.model.imitation.Imitatable model) {
		
		UserOperationCommand userOpeCmd = new UserOperationCommand();
		MsgData pos = MsgTransporter.instance.sendMessage(BhMsg.GET_POS_ON_WORKSPACE, model);
		double x = pos.doublePair._1 + BhParams.REPLACED_NODE_POS;
		double y = pos.doublePair._2 + BhParams.REPLACED_NODE_POS;
		ImitationBuilder imitBuilder = new ImitationBuilder(userOpeCmd, true);
		model.accept(imitBuilder);
		BhNodeHandler.instance.addRootNode(model.getWorkspace(), imitBuilder.getTopImitation(), x, y, userOpeCmd);
		BunnyHop.instance.pushUserOpeCmd(userOpeCmd);
	}
	
	/**
	 * イミテーションノード作成ボタン操作時のイベントを追加する
	 * @param model 作成するイミテーションのオリジナルノード
	 */
	default void setCreateImitHandler(TextNode model) {
		
		if (imitCreateButton() != null) {
			imitCreateButton().setOnAction(event -> {
				imitHandler(model);
				event.consume();
			});
		}
	}

	/**
	 * イミテーションノード作成ボタン操作時のイベントを追加する
	 * @param model 作成するイミテーションのオリジナルノード
	 */
	default void setCreateImitHandler(ConnectiveNode model) {
		
		if (imitCreateButton() != null) {
			imitCreateButton().setOnAction((event) -> {
				imitHandler(model);
				event.consume();
			});
		}
	}
	
	/**
	 * イミテーションノード作成ボタンのスタイルを指定する
	 * @param style イミテーションノード作成ボタンのスタイル情報が格納されたオブジェクト
	 */
	default void setBtnStyle(BhNodeViewStyle.Imitation style) {
		
		Button imitCreateButton = imitCreateButton();
		if (imitCreateButton != null) {
			imitCreateButton.setTranslateX(style.buttonPosX);
			imitCreateButton.setTranslateY(style.buttonPosY);
			imitCreateButton.getStyleClass().add(style.cssClass);
		}
	}
	
	/**
	 * FXML ファイルからイミテーション作成ボタンをロードする
	 * @param fileName ボタンをロードするFXMLファイル名
	 * @param buttonStyle ボタンに適用するスタイル
	 * @return ファイルからロードしたイミテーション作成ボタン
	 */
	default Button loadButton(String fileName, BhNodeViewStyle.Imitation buttonStyle) {

		Button imitCreateImitBtn = null;
		Path filePath = FXMLCollector.instance.getFilePath(fileName);
		try {
			FXMLLoader loader = new FXMLLoader(filePath.toUri().toURL());
			imitCreateImitBtn = (Button)loader.load();
			setBtnStyle(buttonStyle);
		} catch (IOException | ClassCastException e) {
			MsgPrinter.instance.errMsgForDebug(ImitationCreator.class.getSimpleName() + ".loadButton\n" + e.toString());
		}
		return imitCreateImitBtn;
	}
}
