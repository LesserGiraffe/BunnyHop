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
import net.seapanda.bunnyhop.command.BhCmd;
import net.seapanda.bunnyhop.command.CmdData;
import net.seapanda.bunnyhop.command.CmdDispatcher;
import net.seapanda.bunnyhop.command.CmdProcessor;
import net.seapanda.bunnyhop.common.BhConstants;
import net.seapanda.bunnyhop.model.node.attribute.BhNodeId;
import net.seapanda.bunnyhop.service.BhScriptManager;
import net.seapanda.bunnyhop.service.BhService;
import net.seapanda.bunnyhop.utility.TreeNode;
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
public class BhNodeCategoryList implements CmdDispatcher {

  private TreeNode<String> templateTreeRoot;
  /** このオブジェクト宛てに送られたメッセージを処理するオブジェクト. */
  private CmdProcessor msgProcessor = (msg, data) -> null;

  private BhNodeCategoryList() {}

  /**
   * ノードカテゴリとテンプレートノードの配置情報が記されたファイルを読み込みテンプレートツリー作成する.
   *
   * @param filePath ノードカテゴリとテンプレートノードの配置情報が記されたファイルのパス
   */
  public static Optional<BhNodeCategoryList> create(Path filePath) {
    Optional<NativeObject> jsonObj = BhScriptManager.parseJsonFile(filePath);
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
        case BhConstants.NodeTemplate.KEY_CSS_CLASS:  //cssクラスのキー
          if (val instanceof String) {
            TreeNode<String> cssClass = new TreeNode<>(BhConstants.NodeTemplate.KEY_CSS_CLASS);
            cssClass.addChild(new TreeNode<>(val.toString()));
            parent.addChild(cssClass);
          }
          break;

        case BhConstants.NodeTemplate.KEY_CONTENTS:  //ノード ID の配列のキー
          if (val instanceof NativeArray) {
            TreeNode<String> contents = new TreeNode<>(BhConstants.NodeTemplate.KEY_CONTENTS);
            bhNodeForLeafExists &= addBhNodeId((NativeArray) val, contents, fileName);
            parent.addChild(contents);
          }
          break;

        default: // カテゴリ名 ("文字列", "制御", "動作", ...)
          if (val instanceof NativeObject) {
            TreeNode<String> child = new TreeNode<>(key.toString());
            parent.addChild(child);
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
        if (BhService.bhNodeFactory().bhNodeExists(BhNodeId.of(bhNodeIdStr))) {
          parent.addChild(new TreeNode<>(bhNodeIdStr));
        } else {
          allBhNodesExist &= false;
          BhService.msgPrinter().errForDebug("Cannot find %s for %s\n(%s)"
              .formatted(BhConstants.BhModelDef.ELEM_NODE, bhNodeIdStr, fileName));
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
  public void setMsgProcessor(CmdProcessor processor) {
    msgProcessor = processor;
  }

  @Override
  public CmdData dispatch(BhCmd msg, CmdData data) {
    return msgProcessor.process(msg, data);
  }
}
