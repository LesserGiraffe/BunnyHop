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

package net.seapanda.bunnyhop.node.model.event;

import java.util.HashMap;
import net.seapanda.bunnyhop.common.configuration.BhConstants;
import net.seapanda.bunnyhop.node.model.factory.BhNodeFactory;
import net.seapanda.bunnyhop.node.model.service.BhNodePlacer;
import net.seapanda.bunnyhop.service.LogManager;
import net.seapanda.bunnyhop.service.script.BhScriptRepository;
import net.seapanda.bunnyhop.utility.textdb.TextDatabase;
import org.mozilla.javascript.Context;
import org.mozilla.javascript.NativeObject;
import org.mozilla.javascript.ScriptableObject;

/**
 * 外部スクリプトが共通で使うデータを提供するクラス.
 *
 * @author K.Koike
 */
public class CommonDataSupplier {

  private final BhScriptRepository repository;
  private final BhNodeFactory factory;
  private final TextDatabase textDb;
  /** 外部スクリプトが共通で使う Javascript オブジェクト. */
  private volatile Object commonObj = null;

  /** コンストラクタ. */
  public CommonDataSupplier(
      BhScriptRepository repository,
      BhNodeFactory factory,
      TextDatabase textDb) {
    this.repository = repository;
    this.factory = factory;
    this.textDb = textDb;
  }

  /** 外部スクリプトが共通で使うオブジェクトを返す. */
  public Object getCommonObj() {
    // BhNodeFactory がノードを作れるようになってから JavaScript オブジェクトを作りたいので遅延初期化する.
    if (commonObj != null) {
      return commonObj;
    }

    synchronized (this) {
      if (commonObj == null) {
        commonObj = genCommonJsObj();
      }
      return commonObj;
    }
  }

  /** 各スクリプトが共通で使う JavaScript オブジェクトを生成する. */
  private Object genCommonJsObj() {
    if (!repository.allExist(BhConstants.Path.File.BH_UTILITY_JS)) {
      return new NativeObject();
    }
    try {
      Context cx = Context.enter();
      Object jsObj = repository.getScript(BhConstants.Path.File.BH_UTILITY_JS)
            .exec(cx, createScriptScope(cx));
      return (jsObj instanceof NativeObject) ? jsObj : new NativeObject();
    } catch (Exception e) {
      LogManager.logger().error(
          "Failed to execute %s\n%s".formatted(BhConstants.Path.File.BH_UTILITY_JS, e));
    } finally {
      Context.exit();
    }
    return new NativeObject();
  }

  private ScriptableObject createScriptScope(Context cx) {
    ScriptableObject scope = cx.initStandardObjects();
    new HashMap<String, Object>() {{
        put(BhConstants.JsIdName.BH_NODE_FACTORY, factory);
        put(BhConstants.JsIdName.BH_NODE_PLACER, new BhNodePlacer());
        put(BhConstants.JsIdName.BH_TEXT_DB, textDb);
      }}
      .forEach((key, val) -> scope.put(key, scope, Context.javaToJS(val, scope)));
    return scope;
  }
}