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

package net.seapanda.bunnyhop.view.node.part;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import com.google.gson.reflect.TypeToken;
import net.seapanda.bunnyhop.view.node.BhNodeView;

/**
 * {@link BhNodeView} が持つコンテンツの基底クラス.
 *
 * @author K.Koike 
 */
public abstract class NodeViewContent {
  
  /** このオブジェクトの型を識別するための文字列. */
  public final String type;

  public abstract String toJson();

  NodeViewContent(String type) {
    this.type = type;
  }
  
  /**
   * Json 文字列から {@link NodeViewContent} オブジェクトを作成する.
   *
   * @param json この文字列から {@link NodeViewContent} オブジェクトを作成する
   * @return {@link NodeViewContent} オブジェクト
   * @throws IllegalArgumentException {@code json} から {@link NodeViewContent} オブジェクトを作れなかった場合
   */
  public static NodeViewContent fromJson(String json) throws IllegalArgumentException {
    try {
      JsonElement root = JsonParser.parseString(json);
      String type = root.getAsJsonObject().get("type").getAsString();
      return switch (type) {
        case Text.type -> new Gson().fromJson(json, new TypeToken<Text>(){}.getType());
        default -> throw new Exception();
      };
    } catch (Exception e) {
      throw new IllegalArgumentException("Cannot create a %s object from json.\n%s".formatted(
          NodeViewContent.class.getSimpleName(), json));
    }
  }

  /** 文字列を持つコンテンツ. */
  public static class Text extends NodeViewContent {

    public static final String type = "text";
    public final String text;

    public Text(String text) {
      super(Text.type);
      this.text = text;
    }

    @Override
    public String toJson() {
      return new Gson().toJson(this);
    }

    @Override
    public String toString() {
      return text;
    }
  }
}
