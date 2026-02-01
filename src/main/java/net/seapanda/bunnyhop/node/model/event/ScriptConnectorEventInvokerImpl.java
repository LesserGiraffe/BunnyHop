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
import java.util.Map;
import java.util.Objects;
import net.seapanda.bunnyhop.common.configuration.BhConstants;
import net.seapanda.bunnyhop.node.model.BhNode;
import net.seapanda.bunnyhop.node.model.Connector;
import net.seapanda.bunnyhop.node.model.parameter.ConnectorId;
import net.seapanda.bunnyhop.service.LogManager;
import net.seapanda.bunnyhop.service.script.BhScriptRepository;
import org.mozilla.javascript.Context;
import org.mozilla.javascript.Script;
import org.mozilla.javascript.ScriptableObject;

/**
 * 外部スクリプトに定義されたコネクタのイベントハンドラを呼び出す機能を提供するクラス.
 *
 * @author K.Koike
 */
public class ScriptConnectorEventInvokerImpl implements ScriptConnectorEventInvoker {
  
  private final Map<ConnectorId, Map<EventType, String>> eventHandlerMap = new HashMap<>();
  private final BhScriptRepository repository;
  private final CommonDataSupplier supplier;

  /** コンストラクタ. */
  public ScriptConnectorEventInvokerImpl(
      BhScriptRepository repository, CommonDataSupplier supplier) {
    this.repository = repository;
    this.supplier = supplier;
  }

  @Override
  public void register(ConnectorId cnctrId, EventType type, String scriptName) {
    Objects.requireNonNull(cnctrId);
    Objects.requireNonNull(type);
    Objects.requireNonNull(scriptName);
    if (cnctrId.equals(ConnectorId.NONE) || scriptName.isEmpty()) {
      return;
    }
    eventHandlerMap.putIfAbsent(cnctrId, new HashMap<>());
    eventHandlerMap.get(cnctrId).put(type, scriptName);
  }  

  @Override
  public boolean onConnectabilityChecking(Connector target, BhNode node) {
    ScriptNameAndScript defined = getScript(target.getId(), EventType.ON_CONNECTABILITY_CHECKING);
    if (defined == null) {
      return false;
    }
    Map<String, Object> nameToObj = new HashMap<>() {{
        put(BhConstants.JsIdName.BH_CURRENT_NODE, target.getConnectedNode());
        put(BhConstants.JsIdName.BH_NODE_TO_CONNECT, node);
      }};
    Context cx = Context.enter();
    ScriptableObject scriptScope = createScriptScope(cx, target, nameToObj);
    try {
      return (Boolean) defined.script().exec(cx, scriptScope);
    } catch (Exception e) {
      LogManager.logger().error(
          "'%s' must return a boolean value.\n%s".formatted(defined.name(), e));
    } finally {
      Context.exit();
    }
    return false;
  }

  /**
   * スクリプト実行時のスコープ (スクリプトのトップレベルの変数から参照できるオブジェクト群) を作成する.
   *
   * @param cx スコープを作成するのに使用する {@link Context} オブジェクト
   * @param target イベントハンドラに, そのハンドラが定義された {@link Connector} として渡すオブジェクト
   * @param nameToObj 変数名とその変数に格納されるオブジェクトのマップ
   */
  private ScriptableObject createScriptScope(
      Context cx, Connector target, Map<String, Object> nameToObj) {
    ScriptableObject scope = cx.initStandardObjects();
    new HashMap<>(nameToObj) {{
        put(BhConstants.JsIdName.BH_THIS, target);
        put(BhConstants.JsIdName.BH_UTIL, supplier.getCommonObj());
      }}
      .forEach((key, val) -> scope.put(key, scope, Context.javaToJS(val, scope)));
    return scope;
  }

  /**
   * {@code cnctrId} と {@code type} から, 対応するスクリプト名と {@link Script} オブジェクトを取得する.
   * 見つからない場合は null を返す.
   */
  private ScriptNameAndScript getScript(ConnectorId cnctrId, EventType type) {
    Map<EventType, String> typeToScriptName = eventHandlerMap.get(cnctrId);
    if (typeToScriptName == null) {
      return null;
    }
    String scriptName = typeToScriptName.get(type);
    if (scriptName == null || scriptName.isEmpty()) {
      return null;
    }
    Script script = repository.getScript(scriptName);
    if (script == null) {
      return null;
    }
    return new ScriptNameAndScript(scriptName, script);
  }

  private record ScriptNameAndScript(String name, Script script) {}  
}
