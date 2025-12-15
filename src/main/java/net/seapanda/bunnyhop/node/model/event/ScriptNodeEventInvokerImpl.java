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


import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.SequencedCollection;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import net.seapanda.bunnyhop.common.configuration.BhConstants;
import net.seapanda.bunnyhop.node.model.BhNode;
import net.seapanda.bunnyhop.node.model.ConnectiveNode;
import net.seapanda.bunnyhop.node.model.Connector;
import net.seapanda.bunnyhop.node.model.TextNode;
import net.seapanda.bunnyhop.node.model.TextNode.FormatResult;
import net.seapanda.bunnyhop.node.model.TextNode.TextOption;
import net.seapanda.bunnyhop.node.model.factory.BhNodeFactory;
import net.seapanda.bunnyhop.node.model.factory.BhNodeFactory.MvcType;
import net.seapanda.bunnyhop.node.model.parameter.BhNodeId;
import net.seapanda.bunnyhop.node.model.service.BhNodePlacer;
import net.seapanda.bunnyhop.service.LogManager;
import net.seapanda.bunnyhop.service.script.BhScriptRepository;
import net.seapanda.bunnyhop.service.undo.UserOperation;
import net.seapanda.bunnyhop.utility.textdb.TextDatabase;
import org.mozilla.javascript.Context;
import org.mozilla.javascript.Function;
import org.mozilla.javascript.NativeObject;
import org.mozilla.javascript.Script;
import org.mozilla.javascript.ScriptableObject;

/**
 * 外部スクリプトに定義されたノードのイベントハンドラを呼び出す機能を提供するクラス.
 *
 * @author K.Koike
 */
public class ScriptNodeEventInvokerImpl implements ScriptNodeEventInvoker {

  private final Map<BhNodeId, Map<EventType, String>> eventHandlerMap = new HashMap<>();
  private final BhScriptRepository repository;
  private final CommonDataSupplier supplier;
  private final BhNodeFactory factory;
  private final TextDatabase textDb;

  /** コンストラクタ. */
  public ScriptNodeEventInvokerImpl(
      BhScriptRepository repository,
      CommonDataSupplier supplier,
      BhNodeFactory factory,
      TextDatabase textDb) {
    this.repository = repository;
    this.supplier = supplier;
    this.factory = factory;
    this.textDb = textDb;
  }

  @Override
  public void register(BhNodeId nodeId, EventType type, String scriptName) {
    Objects.requireNonNull(nodeId);
    Objects.requireNonNull(type);
    Objects.requireNonNull(scriptName);
    if (nodeId.equals(BhNodeId.NONE) || scriptName.isEmpty()) {
      return;
    }
    eventHandlerMap.putIfAbsent(nodeId, new HashMap<>());
    eventHandlerMap.get(nodeId).put(type, scriptName);
  }

  @Override
  public void onMovedFromWsToChild(BhNode target, BhNode oldReplaced, UserOperation userOpe) {
    ScriptNameAndScript defined = getScript(target.getId(), EventType.ON_MOVED_FROM_WS_TO_CHILD);
    if (defined == null) {
      return;
    }
    Map<String, Object> nameToObj = new HashMap<>() {{
        put(BhConstants.JsIdName.BH_REPLACED_OLD_NODE, oldReplaced);
      }};
    Context cx = Context.enter();
    ScriptableObject scope = createScriptScope(cx, target, userOpe, nameToObj);
    try {
      defined.script().exec(cx, scope);
    } catch (Exception e) {
      LogManager.logger().error(defined.name() + "\n" + e);
    } finally {
      Context.exit();
    }
  }

  @Override
  public void onMovedFromChildToWs(
      BhNode target,
      ConnectiveNode oldParent,
      BhNode oldRoot,
      BhNode newReplaced,
      UserOperation userOpe) {
    ScriptNameAndScript defined = getScript(target.getId(), EventType.ON_MOVED_FROM_CHILD_TO_WS);
    if (defined == null) {
      return;
    }
    Map<String, Object> nameToObj = new HashMap<>() {{
        put(BhConstants.JsIdName.BH_OLD_PARENT, oldParent);
        put(BhConstants.JsIdName.BH_OLD_ROOT, oldRoot);
        put(BhConstants.JsIdName.BH_REPLACED_NEW_NODE, newReplaced);
      }};
    Context cx = Context.enter();
    ScriptableObject scope = createScriptScope(cx, target, userOpe, nameToObj);
    try {
      defined.script().exec(cx, scope);
    } catch (Exception e) {
      LogManager.logger().error(defined.name() + "\n" + e);
    } finally {
      Context.exit();
    }
  }

  @Override
  public void onChildReplaced(
      BhNode target,
      BhNode oldChild,
      BhNode newChild,
      Connector parentCnctr,
      UserOperation userOpe) {
    ScriptNameAndScript defined = getScript(target.getId(), EventType.ON_CHILD_REPLACED);
    if (defined == null) {
      return;
    }
    Map<String, Object> nameToObj = new HashMap<>() {{
        put(BhConstants.JsIdName.BH_REPLACED_NEW_NODE, newChild);
        put(BhConstants.JsIdName.BH_REPLACED_OLD_NODE, oldChild);
        put(BhConstants.JsIdName.BH_PARENT_CONNECTOR, parentCnctr);
      }};
    Context cx = Context.enter();
    ScriptableObject scope = createScriptScope(cx, target, userOpe, nameToObj);
    try {
      defined.script().exec(cx, scope);
    } catch (Exception e) {
      LogManager.logger().error(defined.name() + "\n" + e);
    } finally {
      Context.exit();
    }
  }

  @Override
  public boolean onDeletionRequested(
      BhNode target,
      Collection<? extends BhNode> nodesToDelete,
      CauseOfDeletion causeOfDeletion,
      UserOperation userOpe) {
    ScriptNameAndScript defined = getScript(target.getId(), EventType.ON_DELETION_REQUESTED);
    if (defined == null) {
      return true;
    }
    Map<String, Object> nameToObj = new HashMap<>() {{
        put(BhConstants.JsIdName.BH_CANDIDATE_NODE_LIST, new ArrayList<>(nodesToDelete));
        put(BhConstants.JsIdName.BH_CAUSE_OF_DELETION, causeOfDeletion);
      }};
    Context cx = Context.enter();
    ScriptableObject scope = createScriptScope(cx, target, userOpe, nameToObj);
    try {
      return (Boolean) defined.script().exec(cx, scope);
    } catch (Exception e) {
      LogManager.logger().error(defined.name() + "\n" + e);
    } finally {
      Context.exit();
    }
    return true;
  }

  @Override
  public boolean onCutRequested(
      BhNode target, Collection<? extends BhNode> nodesToCut, UserOperation userOpe) {
    ScriptNameAndScript defined = getScript(target.getId(), EventType.ON_CUT_REQUESTED);
    if (defined == null) {
      return true;
    }
    Map<String, Object> nameToObj = new HashMap<>() {{
        put(BhConstants.JsIdName.BH_CANDIDATE_NODE_LIST, new ArrayList<>(nodesToCut));
      }};
    Context cx = Context.enter();
    ScriptableObject scope = createScriptScope(cx, target, userOpe, nameToObj);
    try {
      return (Boolean) defined.script().exec(cx, scope);
    } catch (Exception e) {
      LogManager.logger().error(
          "'%s' must return a boolean value.\n%s".formatted(defined.name(), e));
    } finally {
      Context.exit();
    }
    return true;
  }
  
  @Override
  public Predicate<? super BhNode> onCopyRequested(
      BhNode target,
      Collection<? extends BhNode> nodesToCopy,
      UserOperation userOpe) {
    ScriptNameAndScript defined = getScript(target.getId(), EventType.ON_COPY_REQUESTED);
    if (defined == null) {
      return node -> true;
    }
    Map<String, Object> nameToObj = new HashMap<>() {{
        put(BhConstants.JsIdName.BH_CANDIDATE_NODE_LIST, new ArrayList<>(nodesToCopy));
      }};
    Context cx = Context.enter();
    ScriptableObject scope = createScriptScope(cx, target, userOpe, nameToObj);
    try {
      Function copyCheckFunc = (Function) defined.script().exec(cx, scope);
      if (copyCheckFunc == null) {
        throw new Exception();
      }
      return node -> isNodeToCopy(node, copyCheckFunc, defined.name());
    } catch (Exception e) {
      LogManager.logger().error(String.format(
          "'%s' must return a function that returns a boolean value.\n%s", defined.name(), e));
    } finally {
      Context.exit();
    }
    return node -> true;
  }

  /**
   * コピー判定関数.
   *
   * @param node このノードをコピーするか判定する.
   * @param copyCheckFunc コピー判定関数
   * @param scriptName {@code copyCheckFunc} を返したスクリプトの名前
   * @return {@code node} をコピーする場合 true
   */
  private boolean isNodeToCopy(BhNode node, Function copyCheckFunc, String scriptName) {
    Context cx = Context.enter();
    ScriptableObject scope = cx.initStandardObjects();
    try {
      return (Boolean) copyCheckFunc.call(cx, scope, scope, new Object[] {node});
    } catch (Exception e) {
      LogManager.logger().error(String.format(
          "'%s' must return null or a function that returns a boolean value.\n%s", scriptName, e));
      throw e;
    } finally {
      Context.exit();
    }
  }

  @Override
  public void onCreatedAsTemplate(BhNode target, UserOperation userOpe) {
    ScriptNameAndScript defined = getScript(target.getId(), EventType.ON_CREATED_AS_TEMPLATE);
    if (defined == null) {
      return;
    }
    Map<String, Object> nameToObj = new HashMap<>();
    Context cx = Context.enter();
    ScriptableObject scope = createScriptScope(cx, target, userOpe, nameToObj);
    try {
      defined.script().exec(cx, scope);
    } catch (Exception e) {
      LogManager.logger().error(defined.name() + "\n" + e);
    } finally {
      Context.exit();
    }
  }

  @Override
  public void onUiEventReceived(BhNode target, UiEvent event, UserOperation userOpe) {
    ScriptNameAndScript defined = getScript(target.getId(), EventType.ON_UI_EVENT_RECEIVED);
    if (defined == null) {
      return;
    }
    Map<String, Object> nameToObj = new HashMap<>() {{
        put(BhConstants.JsIdName.BH_UI_EVENT, event);
      }};
    Context cx = Context.enter();
    ScriptableObject scope = createScriptScope(cx, target, userOpe, nameToObj);
    try {
      defined.script().exec(cx, scope);
    } catch (Exception e) {
      LogManager.logger().error(defined.name() + "\n" + e);
    } finally {
      Context.exit();
    }
  }

  /**
   * 引数の文字列が {@code target} にセット可能かどうか判断するときの処理を実行する.
   *
   * @param target イベントハンドラが定義されたノード
   * @param text セット可能かどうか判断する文字列
   * @return 引数の文字列がセット可能だった場合 true
   */
  @Override
  public boolean onTextChecking(TextNode target, String text) {
    ScriptNameAndScript defined = getScript(target.getId(), EventType.ON_TEXT_CHECKING);
    if (defined == null) {
      return true;
    }
    Map<String, Object> nameToObj = new HashMap<>() {{
        put(BhConstants.JsIdName.BH_TEXT, text);
      }};
    Context cx = Context.enter();
    ScriptableObject scope = createScriptScope(cx, target, nameToObj);
    try {
      return (Boolean) defined.script().exec(cx, scope);
    } catch (Exception e) {
      LogManager.logger().error(
          "'%s' must return a boolean value.\n%s".formatted(defined.name(), e));
    } finally {
      Context.exit();
    }
    return true;
  }

  @Override
  public FormatResult onTextFormatting(TextNode target, String text, String addedText) {
    ScriptNameAndScript defined = getScript(target.getId(), EventType.ON_TEXT_FORMATTING);
    if (defined == null) {
      return new FormatResult(false, addedText);
    }
    Map<String, Object> nameToObj = new HashMap<>() {{
        put(BhConstants.JsIdName.BH_TEXT, text);
        put(BhConstants.JsIdName.BH_ADDED_TEXT, addedText);
      }};
    Context cx = Context.enter();
    ScriptableObject scope = createScriptScope(cx, target, nameToObj);
    try {
      NativeObject jsObj = (NativeObject) defined.script().exec(cx, scope);
      Boolean isWholeFormatted =
          (Boolean) jsObj.get(BhConstants.JsIdName.BH_IS_WHOLE_TEXT_FORMATTED);
      String formattedText = (String) jsObj.get(BhConstants.JsIdName.BH_FORMATTED_TEXT);
      return new FormatResult(isWholeFormatted, formattedText);
    } catch (Exception e) {
      LogManager.logger().error(
          "Invalid text formatter  (%s).\n%s".formatted(defined.name(), e));
    } finally {
      Context.exit();
    }
    return new FormatResult(false, addedText);
  }

  @Override
  public List<TextOption> onTextOptionCreating(TextNode target) {
    ScriptNameAndScript defined = getScript(target.getId(), EventType.ON_TEXT_OPTIONS_CREATING);
    if (defined == null) {
      return new ArrayList<>();
    }
    Context cx = Context.enter();
    ScriptableObject scope = createScriptScope(cx, target, new HashMap<>());
    try {
      List<?> contents = (List<?>) defined.script().exec(cx, scope);
      var options = new ArrayList<TextOption>();
      for (Object content : contents) {
        List<?> modelAndView = (List<?>) content;
        options.add(new TextOption(modelAndView.get(0).toString(), modelAndView.get(1)));
      }
      return options;
    } catch (Exception e) {
      LogManager.logger().error(defined.name() + "\n" + e);
    } finally {
      Context.exit();
    }
    return new ArrayList<>();
  }

  @Override
  public List<BhNode> onCompanionNodesCreating(BhNode target, MvcType type, UserOperation userOpe) {
    ScriptNameAndScript defined = getScript(target.getId(), EventType.ON_COMPANION_NODES_CREATING);
    if (defined == null) {
      return new ArrayList<>();
    }
    Map<String, Object> nameToObj = new HashMap<>() {{
        put(BhConstants.JsIdName.BH_USER_OPE, userOpe);
      }};
    Context cx = Context.enter();
    ScriptableObject scope = createScriptScope(cx, target, userOpe, nameToObj);
    try {
      List<BhNode> companionNodes = ((Collection<?>) defined.script().exec(cx, scope)).stream()
          .filter(elem -> elem instanceof BhNode)
          .map(elem -> (BhNode) elem)
          .collect(Collectors.toCollection(ArrayList::new));
      companionNodes.forEach(node -> factory.setMvc(node, type));
      return companionNodes;
    } catch (Exception e) {
      LogManager.logger().error(
          "'%s' must return a collection of BhNode(s).\n%s".formatted(defined.name(), e));
    } finally {
      Context.exit();
    }
    return new ArrayList<>();    
  }

  @Override
  public SequencedCollection<String> onCompileErrChecking(BhNode target) {
    ScriptNameAndScript defined = getScript(target.getId(), EventType.ON_COMPILE_ERR_CHECKING);
    if (defined == null) {
      return new ArrayList<>();
    }
    Context cx = Context.enter();
    ScriptableObject scope = createScriptScope(cx, target, new HashMap<>());
    try {
      return ((Collection<?>) defined.script().exec(cx, scope)).stream()
          .map(Object::toString)
          .collect(Collectors.toCollection(ArrayList::new));
    } catch (Exception e) {
      LogManager.logger().error("'%s' must return a collection.\n%s".formatted(defined.name(), e));
    } finally {
      Context.exit();
    }
    return new ArrayList<>();
  }

  @Override
  public String onAliasAsked(BhNode target) {
    ScriptNameAndScript defined = getScript(target.getId(), EventType.ON_ALIAS_ASKED);
    if (defined == null) {
      return "";
    }
    Context cx = Context.enter();
    ScriptableObject scope = createScriptScope(cx, target, new HashMap<>());
    try {
      return (String) defined.script().exec(cx, scope);
    } catch (Exception e) {
      LogManager.logger().error(
          "'%s' must return a string value.\n%s".formatted(defined.name(), e));
    } finally {
      Context.exit();
    }
    return "";
  }

  @Override
  public Optional<String> onUserDefinedNameAsked(BhNode target) {
    ScriptNameAndScript defined = getScript(target.getId(), EventType.ON_USER_DEFINED_NAME_ASKED);
    if (defined == null) {
      return Optional.empty();
    }
    Context cx = Context.enter();
    ScriptableObject scope = createScriptScope(cx, target, new HashMap<>());
    try {
      return Optional.of((String) defined.script().exec(cx, scope));
    } catch (Exception e) {
      LogManager.logger().error(
          "'%s' must return a string value.\n%s".formatted(defined.name(), e));
    } finally {
      Context.exit();
    }
    return Optional.empty();
  }

  /**
   * スクリプト実行時のスコープ (スクリプトのトップレベルの変数から参照できるオブジェクト群) を作成する.
   *
   * @param cx スコープを作成するのに使用する {@link Context} オブジェクト
   * @param target イベントハンドラに, そのハンドラが定義された {@link BhNode} として渡すオブジェクト
   * @param nameToObj 変数名とその変数に格納されるオブジェクトのマップ
   */
  private ScriptableObject createScriptScope(
      Context cx, BhNode target, Map<String, Object> nameToObj) {
    return createScriptScope(cx, target, null, nameToObj);
  }

  /**
   * スクリプト実行時のスコープ (スクリプトのトップレベルの変数から参照できるオブジェクト群) を作成する.
   *
   * @param cx スコープを作成するのに使用する {@link Context} オブジェクト
   * @param target イベントハンドラに, そのハンドラが定義された {@link BhNode} として渡すオブジェクト
   * @param userOpe イベントハンドラに渡す {@link UserOperation} オブジェクト
   * @param nameToObj 変数名とその変数に格納されるオブジェクトのマップ
   */
  private ScriptableObject createScriptScope(
      Context cx, BhNode target, UserOperation userOpe, Map<String, Object> nameToObj) {
    nameToObj = new HashMap<>(nameToObj);
    nameToObj.put(BhConstants.JsIdName.BH_THIS, target);
    nameToObj.put(BhConstants.JsIdName.BH_NODE_PLACER, new BhNodePlacer());
    nameToObj.put(BhConstants.JsIdName.BH_COMMON, supplier.getCommonObj());
    nameToObj.put(BhConstants.JsIdName.BH_NODE_FACTORY, factory);
    nameToObj.put(BhConstants.JsIdName.BH_TEXT_DB, textDb);
    if (userOpe != null) {
      nameToObj.put(BhConstants.JsIdName.BH_USER_OPE, userOpe);
    }
    ScriptableObject scope = cx.initStandardObjects();
    for (String name : nameToObj.keySet()) {
      Object val = Context.javaToJS(nameToObj.get(name), scope);
      scope.put(name, scope, val);
    }
    return scope;
  }

  /**
   * {@code nodeId} と {@code type} から, 対応するスクリプト名と {@link Script} オブジェクトを取得する.
   * 見つからない場合は null を返す.
   */
  private ScriptNameAndScript getScript(BhNodeId nodeId, EventType type) {
    Map<EventType, String> typeToScriptName = eventHandlerMap.get(nodeId);
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
