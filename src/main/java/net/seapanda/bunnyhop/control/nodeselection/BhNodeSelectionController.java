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

import net.seapanda.bunnyhop.message.BhMsg;
import net.seapanda.bunnyhop.message.MsgData;
import net.seapanda.bunnyhop.message.MsgProcessor;
import net.seapanda.bunnyhop.model.workspace.Workspace;
import net.seapanda.bunnyhop.view.nodeselection.BhNodeSelectionView;

/**
 * ノード選択ビューのコントローラ
 * @author K.Koike
 */
public class BhNodeSelectionController implements MsgProcessor {

  private Workspace model;
  private BhNodeSelectionView view;

  /**
   * コンストラクタ
   * @param model 操作対象のモデル
   * @param view 操作対象のビュー
   */
  public BhNodeSelectionController(Workspace model, BhNodeSelectionView view) {

    this.model = model;
    this.view = view;
  }

  /**
   * メッセージ受信
   * @param msg メッセージの種類
   * @param data メッセージの種類に応じて処理するもの
   * */
  @Override
  public MsgData processMsg(BhMsg msg, MsgData data) {

    switch (msg) {

      case ADD_ROOT_NODE:
        model.addRootNode(data.node);
        view.addNodeView(data.nodeView);
        break;

      case REMOVE_ROOT_NODE:
        model.removeRootNode(data.node);
        view.removeNodeView(data.nodeView);
        break;

      default:
        // do nothing.
    }

    return null;
  };
}
