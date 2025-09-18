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
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import net.seapanda.bunnyhop.common.BhConstants;
import net.seapanda.bunnyhop.model.ModelConstructionException;
import net.seapanda.bunnyhop.model.factory.BhNodeFactory;
import net.seapanda.bunnyhop.model.node.parameter.BhNodeId;
import net.seapanda.bunnyhop.utility.TreeNode;
import net.seapanda.bunnyhop.utility.textdb.TextDatabase;

/**
 * ノードのカテゴリ一覧を保持するクラス.
 *
 * @author K.Koike
 */
public class JsonBhNodeCategoryTree implements BhNodeCategoryTree {

  private static final Pattern escapeLbrace = Pattern.compile(Pattern.quote("\\{"));
  private static final Pattern escapeRbrace = Pattern.compile(Pattern.quote("\\}"));
  /** `\\...\$`/ */
  private static final Pattern escapeDollar = Pattern.compile("^(\\\\)+\\$");
  /** テキスト DB 参照パターン `${a}{b}...{z}`. */
  private static final Pattern embedded =
      Pattern.compile("^\\$(\\{(((\\\\\\{)|(\\\\})|[^{}])*)})+$");
  /** テキスト DB 参照パターン `${a}{b}...{z}` の (a, b, ..., z) を取り出す用. */
  private static final Pattern contents =
      Pattern.compile("\\{((?:\\\\\\{|\\\\}|[^{}])*)}");

  private final TreeNode<String> root;
  private final BhNodeFactory factory;
  private final TextDatabase textDb;

  /** コンストラクタ. */
  public JsonBhNodeCategoryTree(Path filePath, BhNodeFactory factory, TextDatabase textDb)
      throws ModelConstructionException {
    this.factory = factory;
    this.textDb = textDb;
    var gson = new Gson();
    try (var jr = gson.newJsonReader(new FileReader(filePath.toString()))) {
      JsonObject jsonObj = gson.fromJson(jr, JsonObject.class);      
      root = new TreeNode<>("root");
      addChildren(jsonObj, root, filePath.toString());
    } catch (Exception e) {
      throw new ModelConstructionException(e.toString());
    }
  }

  /**
   * テンプレートツリーに子ノードを追加する.
   *
   * @param jsonObj 追加する子ノードをメンバとして持つオブジェクト
   * @param parent 子ノードを追加したいノード
   * @param fileName ノードカテゴリとテンプレートノードの定義が書かれたファイル
   */
  private void addChildren(JsonObject jsonObj, TreeNode<String> parent, String fileName)
      throws ModelConstructionException {
    for (String key : jsonObj.keySet()) {
      if (key == null) {
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
            addBhNodeId(val.getAsJsonArray(), contents, fileName);
            parent.addChild(contents);
          }
          break;

        default: // カテゴリ名 ("文字列", "制御", "動作", ...)
          if (val.isJsonObject()) {
            TreeNode<String> child = new TreeNode<>(toCategoryName(key));
            parent.addChild(child);
            addChildren(val.getAsJsonObject(), child, fileName);
          }
          break;
      }
    }
  }

  /**
   * JsonArray からBhNode の ID を読み取って, 対応するノードがある ID のみ子ノードとして追加する.
   *
   * @param bhNodeIdList BhNode の ID のリスト
   * @param parent ID を子ノードとして追加する親ノード
   */
  private void addBhNodeId(JsonArray bhNodeIdList, TreeNode<String> parent, String fileName)
      throws ModelConstructionException {
    for (JsonElement bhNodeId : bhNodeIdList) {
      // 配列内の文字列だけをBhNode の IDとみなす
      if (bhNodeId.isJsonPrimitive() && bhNodeId.getAsJsonPrimitive().isString()) {
        String bhNodeIdStr = bhNodeId.getAsString();        
        // ID に対応する BhNode がある
        if (factory.canCreate(BhNodeId.of(bhNodeIdStr))) {
          parent.addChild(new TreeNode<>(bhNodeIdStr));
        } else {
          throw new ModelConstructionException("Cannot find the %s whose ID is '%s'\n(%s)"
            .formatted(BhConstants.BhModelDef.ELEM_NODE, bhNodeIdStr, fileName));
        }
      }
    }
  }

  /** {@code str} をノードカテゴリ名に変換する. */
  private String toCategoryName(String str) {
    if (embedded.matcher(str).find()) {
      Matcher matcher = contents.matcher(str);
      List<String> textId = matcher.results().map(
          result -> {  
            String tmp = escapeLbrace.matcher(result.group(1)).replaceAll("{");
            return escapeRbrace.matcher(tmp).replaceAll("}");
          }).toList();
      return textDb.get(textId);
    }
    if (escapeDollar.matcher(str).find()) {
      return str.substring(1);
    }
    return str;
  }

  @Override
  public TreeNode<String> getRoot() {
    return root;
  }
}
 