package pflab.bunnyHop.view;

import java.io.IOException;
import java.nio.file.Path;
import javafx.fxml.FXMLLoader;
import javafx.scene.control.Button;
import pflab.bunnyHop.modelProcessor.ImitationBuilder;
import pflab.bunnyHop.root.MsgPrinter;
import pflab.bunnyHop.common.BhParams;
import pflab.bunnyHop.message.BhMsg;
import pflab.bunnyHop.message.MsgData;
import pflab.bunnyHop.message.MsgTransporter;
import pflab.bunnyHop.model.TextNode;
import pflab.bunnyHop.model.connective.ConnectiveNode;
import pflab.bunnyHop.modelHandler.BhNodeHandler;
import pflab.bunnyHop.configFileReader.FXMLCollector;
import pflab.bunnyHop.undo.UserOperationCommand;

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
	default void imitHandler(pflab.bunnyHop.model.Imitatable model) {
		
		UserOperationCommand userOpeCmd = new UserOperationCommand();
		MsgData pos = MsgTransporter.instance().sendMessage(BhMsg.GET_POS_ON_WORKSPACE, model);
		double x = pos.doublePair._1 + BhParams.replacedNodePos;
		double y = pos.doublePair._2 + BhParams.replacedNodePos;
		ImitationBuilder imitBuilder = new ImitationBuilder(userOpeCmd, true);
		model.accept(imitBuilder);
		BhNodeHandler.instance.addRootNode(model.getWorkspace(), imitBuilder.getTopImitation(), x, y, userOpeCmd);
		MsgTransporter.instance().sendMessage(BhMsg.PUSH_USER_OPE_CMD, new MsgData(userOpeCmd), model.getWorkspace());
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
		} catch (IOException | ClassCastException ex) {
			MsgPrinter.instance.ErrMsgForDebug(ex.toString());
		}
		return imitCreateImitBtn;
	}
}
