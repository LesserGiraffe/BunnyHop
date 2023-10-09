package net.seapanda.bunnyhop.control.node;

import net.seapanda.bunnyhop.message.BhMsg;
import net.seapanda.bunnyhop.message.MsgData;
import net.seapanda.bunnyhop.model.node.TextNode;
import net.seapanda.bunnyhop.view.node.NoContentNodeView;

public class NoContentNodeController  extends BhNodeController {


  private final TextNode model;  //!< 管理するモデル
  private final NoContentNodeView view;  //!< 管理するビュー

  public NoContentNodeController(TextNode model, NoContentNodeView view) {
    super(model, view);
    this.model = model;
    this.view = view;
  }

  /**
   * 受信したメッセージを処理する
   * @param msg メッセージの種類
   * @param data メッセージの種類に応じて処理するデータ
   * @return メッセージを処理した結果返すデータ
   * */
  @Override
  public MsgData processMsg(BhMsg msg, MsgData data) {

    switch (msg) {
      case IMITATE_TEXT:
        model.setText(data.strPair._1);
        break;

      case GET_VIEW_TEXT:
        return new MsgData();

      default:
        return super.processMsg(msg, data);
    }
    return null;
  }
}
