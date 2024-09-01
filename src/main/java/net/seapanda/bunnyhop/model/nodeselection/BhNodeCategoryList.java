/*
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

package net.seapanda.bunnyhop.model.nodeselection;

import java.nio.file.Path;
import java.util.Optional;
import net.seapanda.bunnyhop.common.TreeNode;
import net.seapanda.bunnyhop.common.constant.BhParams;
import net.seapanda.bunnyhop.common.tools.MsgPrinter;
import net.seapanda.bunnyhop.configfilereader.BhScriptManager;
import net.seapanda.bunnyhop.message.BhMsg;
import net.seapanda.bunnyhop.message.MsgData;
import net.seapanda.bunnyhop.message.MsgProcessor;
import net.seapanda.bunnyhop.message.MsgReceptionWindow;
import net.seapanda.bunnyhop.model.node.attribute.BhNodeId;
import net.seapanda.bunnyhop.model.templates.BhNodeTemplates;
import org.mozilla.javascript.NativeArray;
import org.mozilla.javascript.NativeObject;

/**
 * ノードのカテゴリ一覧とテンプレートノードを管理するクラス.
 * <p>
 * テンプレートツリー : ノード選択部分のカテゴリ名や各カテゴリのテンプレートノードの ID を文字列として保持する木.
 * </p>
 *
 * @author K.Koike
 */
public class BhNodeCategoryList implements MsgReceptionWindow {

  private TreeNode<String> templateTreeRoot;
  /** このオブジェクト宛てに送られたメッセージを処理するオブジェクト. */
  private MsgProcessor msgProcessor;

  private BhNodeCategoryList() {}

  /**
   * ノードカテゴリとテンプレートノードの配置情報が記されたファイルを読み込みテンプレートツリー作成する.
   *
   * @param filePath ノードカテゴリとテンプレートノードの配置情報が記されたファイルのパス
   */
  public static Optional<BhNodeCategoryList> create(Path filePath) {
    Optional<NativeObject> jsonObj = BhScriptManager.INSTANCE.parseJsonFile(filePath);
    if (jsonObj.isEmpty()) {
      return Optional.empty();
    }
    var newObj = new BhNodeCategoryList();
    newObj.templateTreeRoot = new TreeNode<>("root");
    boolean success =
        newObj.addChildren(jsonObj.get(), newObj.templateTreeRoot, filePath.toString());
    if (success) {
      return Optional.of(newObj);
    }
    return Optional.empty();
  }

  /**
   * テンプレートツリーに子ノードを追加する.
   *
   * @param jsonObj 追加する子ノードをメンバとして持つオブジェクト
   * @param parent 子ノードを追加したいノード
   * @param fileName ノードカテゴリとテンプレートノードの定義が書かれたファイル
   * @return テンプレートツリーを正常に構築できた場合 true
   */
  private boolean addChildren(NativeObject jsonObj, TreeNode<String> parent, String fileName) {
    boolean bhNodeForLeafExists = true;
    for (Object key : jsonObj.keySet()) {
      if (!(key instanceof String)) {
        continue;
      }
      Object val = jsonObj.get(key);
      switch (key.toString()) {
        case BhParams.NodeTemplate.KEY_CSS_CLASS:  //cssクラスのキー
          if (val instanceof String) {
            TreeNode<String> cssClass = new TreeNode<>(BhParams.NodeTemplate.KEY_CSS_CLASS);
            cssClass.children.add(new TreeNode<>(val.toString()));
            parent.children.add(cssClass);
          }
          break;

        case BhParams.NodeTemplate.KEY_CONTENTS:  //ノード ID の配列のキー
          if (val instanceof NativeArray) {
            TreeNode<String> contents = new TreeNode<>(BhParams.NodeTemplate.KEY_CONTENTS);
            bhNodeForLeafExists &= addBhNodeId((NativeArray) val, contents, fileName);
            parent.children.add(contents);
          }
          break;

        default: // カテゴリ名 ("文字列", "制御", "動作", ...)
          if (val instanceof NativeObject) {
            TreeNode<String> child = new TreeNode<>(key.toString());
            parent.children.add(child);
            bhNodeForLeafExists &= addChildren((NativeObject) val, child, fileName);
          }
          break;
      }
    }
    return bhNodeForLeafExists;
  }

  /**
   * JsonArray からBhNode の ID を読み取って, 対応するノードがある ID のみ子ノードとして追加する.
   *
   * @param bhNodeIdList BhNode の ID のリスト
   * @param parent ID を子ノードとして追加する親ノード
   * @return 各 ID に対応する BhNode がすべて見つかった場合true
   */
  private boolean addBhNodeId(NativeArray bhNodeIdList, TreeNode<String> parent, String fileName) {
    boolean allBhNodesExist = true;
    for (Object bhNodeId : bhNodeIdList) {
      if (bhNodeId instanceof String) {  // 配列内の文字列だけをBhNode の IDとみなす
        String bhNodeIdStr = (String) bhNodeId;
        // IDに対応する BhNode がある
        if (BhNodeTemplates.INSTANCE.bhNodeExists(BhNodeId.create(bhNodeIdStr))) {
          parent.children.add(new TreeNode<>(bhNodeIdStr));
        } else {
          allBhNodesExist &= false;
          MsgPrinter.INSTANCE.errMsgForDebug(
              bhNodeIdStr + " に対応する " + BhParams.BhModelDef.ELEM_NODE + " が存在しません.\n"
              + "(" + fileName + ")");
        }
      }
    }
    return allBhNodesExist;
  }

  /**
   * テンプレートツリーのルートノードを返す.
   *
   * @return テンプレートツリーのルートノード
   */
  public TreeNode<String> getRootNode() {
    return templateTreeRoot;
  }

  @Override
  public void setMsgProcessor(MsgProcessor processor) {
    msgProcessor = processor;
  }

  @Override
  public MsgData passMsg(BhMsg msg, MsgData data) {
    return msgProcessor.processMsg(msg, data);
  }
}





















