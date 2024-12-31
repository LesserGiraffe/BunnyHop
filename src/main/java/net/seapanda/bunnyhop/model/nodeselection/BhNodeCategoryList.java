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

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import java.io.FileReader;
import java.nio.file.Path;
import java.util.List;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import net.seapanda.bunnyhop.common.BhConstants;
import net.seapanda.bunnyhop.model.AppRoot;
import net.seapanda.bunnyhop.model.node.attribute.BhNodeId;
import net.seapanda.bunnyhop.service.BhService;
import net.seapanda.bunnyhop.utility.TreeNode;

/**
 * ノードのカテゴリ一覧とテンプレートノードを管理するクラス.
 * <p>
 * テンプレートツリー : ノード選択部分のカテゴリ名や各カテゴリのテンプレートノードの ID を文字列として保持する木.
 * </p>
 *
 * @author K.Koike
 */
public class BhNodeCategoryList {

  private static Pattern escapeLbrace = Pattern.compile(Pattern.quote("\\{"));
  private static Pattern escapeRbrace = Pattern.compile(Pattern.quote("\\}"));
  /** `\\...\$` */
  private static Pattern escapeDollar = Pattern.compile("^(\\\\)+\\$");
  /** テキスト DB 参照パターン `${a}{b}...{z}` */
  private static Pattern embeded =
      Pattern.compile("^\\$(\\{(((\\\\\\{)|(\\\\\\})|[^\\{\\}])*)\\})+$");
  /** テキスト DB 参照パターン `${a}{b}...{z}` の (a, b, ..., z) を取り出す用. */
  private static Pattern contents =
      Pattern.compile("\\{((?:(?:\\\\\\{)|(?:\\\\\\})|[^\\{\\}])*)\\}");

  private TreeNode<String> templateTreeRoot;
  /** このオブジェクトを保持する {@link AppRoot} オブジェクト. */
  private AppRoot appRoot;

  private BhNodeCategoryList() {}

  /**
   * ノードカテゴリとテンプレートノードの配置情報が記されたファイルを読み込みテンプレートツリー作成する.
   *
   * @param filePath ノードカテゴリとテンプレートノードの配置情報が記されたファイルのパス
   */
  public static Optional<BhNodeCategoryList> create(Path filePath) {

    var gson = new Gson();
    try (var jr = gson.newJsonReader(new FileReader(filePath.toString()))) {
      JsonObject jsonObj = gson.fromJson(jr, JsonObject.class);      
      var newObj = new BhNodeCategoryList();
      newObj.templateTreeRoot = new TreeNode<>("root");
      boolean success =
          newObj.addChildren(jsonObj, newObj.templateTreeRoot, filePath.toString());
      if (success) {
        return Optional.of(newObj);
      }
    } catch (Exception e) {
      BhService.msgPrinter().errForDebug(e.toString());
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
  private boolean addChildren(JsonObject jsonObj, TreeNode<String> parent, String fileName) {
    boolean allBhNodesExist = true;
    for (String key : jsonObj.keySet()) {
      if (!(key instanceof String)) {
        continue;
      }
      JsonElement val = jsonObj.get(key);
      switch (key) {
        case BhConstants.NodeTemplate.KEY_CSS_CLASS:  //cssクラスのキー
          if (val.isJsonPrimitive() && val.getAsJsonPrimitive().isString()) {
            TreeNode<String> cssClass = new TreeNode<>(BhConstants.NodeTemplate.KEY_CSS_CLASS);
            cssClass.addChild(new TreeNode<>(val.getAsString()));
            parent.addChild(cssClass);
          }
          break;

        case BhConstants.NodeTemplate.KEY_CONTENTS:  //ノード ID の配列のキー
          if (val.isJsonArray()) {
            TreeNode<String> contents = new TreeNode<>(BhConstants.NodeTemplate.KEY_CONTENTS);
            allBhNodesExist &= addBhNodeId(val.getAsJsonArray(), contents, fileName);
            parent.addChild(contents);
          }
          break;

        default: // カテゴリ名 ("文字列", "制御", "動作", ...)
          if (val.isJsonObject()) {
            TreeNode<String> child = new TreeNode<>(toCategoryName(key));
            parent.addChild(child);
            allBhNodesExist &= addChildren(val.getAsJsonObject(), child, fileName);
          }
          break;
      }
    }
    return allBhNodesExist;
  }

  /**
   * JsonArray からBhNode の ID を読み取って, 対応するノードがある ID のみ子ノードとして追加する.
   *
   * @param bhNodeIdList BhNode の ID のリスト
   * @param parent ID を子ノードとして追加する親ノード
   * @return 各 ID に対応する BhNode がすべて見つかった場合true
   */
  private boolean addBhNodeId(JsonArray bhNodeIdList, TreeNode<String> parent, String fileName) {
    boolean allBhNodesExist = true;
    for (JsonElement bhNodeId : bhNodeIdList) {
      // 配列内の文字列だけをBhNode の IDとみなす
      if (bhNodeId.isJsonPrimitive() && bhNodeId.getAsJsonPrimitive().isString()) {
        String bhNodeIdStr = bhNodeId.getAsString();        
        // ID に対応する BhNode がある
        if (BhService.bhNodeFactory().bhNodeExists(BhNodeId.of(bhNodeIdStr))) {
          parent.addChild(new TreeNode<>(bhNodeIdStr));
        } else {
          allBhNodesExist &= false;
          BhService.msgPrinter().errForDebug("Cannot find the %s whose ID is '%s'\n(%s)"
              .formatted(BhConstants.BhModelDef.ELEM_NODE, bhNodeIdStr, fileName));
        }
      }
    }
    return allBhNodesExist;
  }

  /** {@code str} をノードカテゴリ名に変換する. */
  private static String toCategoryName(String str) {
    if (embeded.matcher(str).find()) {
      Matcher matcher = contents.matcher(str);
      List<String> textId = matcher.results().map(
          result -> {  
            String tmp = escapeLbrace.matcher(result.group(1)).replaceAll("{");
            return escapeRbrace.matcher(tmp).replaceAll("}");
          }).toList();
      return BhService.textDb().get(textId);
    }
    if (escapeDollar.matcher(str).find()) {
      return str.substring(1);
    }
    return str;
  }

  /**
   * テンプレートツリーのルートノードを返す.
   *
   * @return テンプレートツリーのルートノード
   */
  public TreeNode<String> getRootNode() {
    return templateTreeRoot;
  }

  /** このオブジェクトを保持する {@link AppRoot} オブジェクトを取得する.*/
  public AppRoot getAppRoot() {
    return appRoot;
  }

  /** このオブジェクトを保持する {@link AppRoot} オブジェクトを設定する.*/
  public void setAppRoot(AppRoot appRoot) {
    this.appRoot = appRoot;
  }
}
