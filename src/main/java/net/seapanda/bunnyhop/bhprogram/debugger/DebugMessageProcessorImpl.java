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

package net.seapanda.bunnyhop.bhprogram.debugger;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.SequencedCollection;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;
import net.seapanda.bunnyhop.bhprogram.common.BhSymbolId;
import net.seapanda.bunnyhop.bhprogram.common.message.debug.GetGlobalListValsResp;
import net.seapanda.bunnyhop.bhprogram.common.message.debug.GetGlobalVarsResp;
import net.seapanda.bunnyhop.bhprogram.common.message.debug.GetLocalListValsResp;
import net.seapanda.bunnyhop.bhprogram.common.message.debug.GetLocalVarsResp;
import net.seapanda.bunnyhop.bhprogram.common.message.thread.BhThreadContext;
import net.seapanda.bunnyhop.bhprogram.common.message.variable.BhListVariable;
import net.seapanda.bunnyhop.bhprogram.common.message.variable.BhScalarVariable;
import net.seapanda.bunnyhop.bhprogram.common.message.variable.BhVariable;
import net.seapanda.bunnyhop.bhprogram.debugger.variable.ListVariable;
import net.seapanda.bunnyhop.bhprogram.debugger.variable.ScalarVariable;
import net.seapanda.bunnyhop.bhprogram.debugger.variable.StackFrameId;
import net.seapanda.bunnyhop.bhprogram.debugger.variable.Variable;
import net.seapanda.bunnyhop.bhprogram.debugger.variable.VariableInfo;
import net.seapanda.bunnyhop.common.TextDefs;
import net.seapanda.bunnyhop.model.node.BhNode;
import net.seapanda.bunnyhop.model.node.syntaxsymbol.InstanceId;
import net.seapanda.bunnyhop.model.workspace.WorkspaceSet;
import net.seapanda.bunnyhop.service.LogManager;

/**
 * デバッグメッセージを処理するクラス.
 *
 * @author K.Koike
 */
public class DebugMessageProcessorImpl implements DebugMessageProcessor {

  private final Debugger debugger;
  private final Collection<InstanceId> mainRoutineIds;
  /**
   * key: {@link BhNode} のインスタンス ID.
   * value: key に対応する {@link BhNode}.
   */
  private final Map<InstanceId, BhNode> instIdToNode = new ConcurrentHashMap<>();

  /**
   * コンストラクタ.
   *
   * @param mainRoutineIds BhProgram のエントリポイントとなるノードの処理を呼ぶ関数の ID 一覧.
   */
  public DebugMessageProcessorImpl(
      WorkspaceSet wss, Debugger debugger, Collection<InstanceId> mainRoutineIds) {
    this.debugger = debugger;
    this.mainRoutineIds = new HashSet<>(mainRoutineIds);
    wss.getCallbackRegistry().getOnNodeAdded().add(
        event -> instIdToNode.put(event.node().getInstanceId(), event.node()));
    wss.getCallbackRegistry().getOnNodeRemoved().add(
        event -> instIdToNode.remove(event.node().getInstanceId()));
  }

  @Override
  public void process(BhThreadContext context) {
    logErrMsg(context.getException());
    var cache = new HashMap<InstanceId, String>();
    List<CallStackItem> callStack = context.getCallStack().stream()
        .map(item ->
            createCallStackItem(item.symbolId(), item.frameIdx(), context.getThreadId(), cache))
        .collect(Collectors.toCollection(ArrayList::new));

    CallStackItem nextStep = null;
    if (!context.getNextStep().equals(BhSymbolId.NONE)) {
      int frameIdx = callStack.isEmpty() ? 0 : callStack.getLast().getIdx() + 1;
      nextStep = createCallStackItem(context.getNextStep(), frameIdx, context.getThreadId(), cache);
    }
    var threadContext = new ThreadContext(
        context.getThreadId(), context.getState(), callStack, nextStep, context.getException());
    debugger.add(threadContext);
  }

  @Override
  public void process(GetLocalVarsResp resp) {
    resp.getException().ifPresent(this::logErrMsg);
    if (resp.result == null) {
      return;
    }
    var stackFrameId = new StackFrameId(resp.result.threadId(), resp.result.frame().idx());
    SequencedCollection<Variable> vars = resp.result.frame().variables().stream()
        .map(this::createVariable)
        .collect(Collectors.toCollection(ArrayList::new));
    var varInfo = new VariableInfo(stackFrameId, vars);
    debugger.add(varInfo);
  }

  @Override
  public void process(GetLocalListValsResp resp) {
    resp.getException().ifPresent(this::logErrMsg);
    if (resp.result == null) {
      return;
    }
    var stackFrameId = new StackFrameId(resp.result.threadId(), resp.result.frameIdx());
    Variable variable = createVariable(resp.result.variable());
    debugger.add(new VariableInfo(stackFrameId, variable));
  }

  @Override
  public void process(GetGlobalVarsResp resp) {
    resp.getException().ifPresent(this::logErrMsg);
    if (resp.variables.isEmpty()) {
      return;
    }
    SequencedCollection<Variable> vars = resp.variables.stream()
        .map(this::createVariable)
        .collect(Collectors.toCollection(ArrayList::new));
    debugger.add(new VariableInfo(vars));
  }

  @Override
  public void process(GetGlobalListValsResp resp)  {
    resp.getException().ifPresent(this::logErrMsg);
    if (resp.variable == null) {
      return;
    }
    Variable variable = createVariable(resp.variable);
    debugger.add(new VariableInfo(variable));
  }

  /**
   * {@link CallStackItem} を作成する.
   *
   * @param symbolId 関数呼び出しノードの ID
   * @param frameIdx コールスタックのスタックフレームのインデックス
   * @param threadId コールスタックを持つスレッドの ID
   * @param funcNameCache 関数名を格納するキャッシュ
   */
  private CallStackItem createCallStackItem(
      BhSymbolId symbolId,
      int frameIdx,
      long threadId,
      Map<InstanceId, String> funcNameCache) {
    var instId = InstanceId.of(symbolId.toString());
    BhNode node = instIdToNode.get(instId);
    String name = getFuncName(node, instId, funcNameCache);
    return new CallStackItem(frameIdx, threadId, name, node);
  }

  /**
   * 関数呼び出しノード ({@code node}) から関数名を取得する.
   *
   * <p>{@code node} が null の場合, {@code instId} が {@link #mainRoutineIds} に含まれていれば,
   * メインルーチンのエイリアスを返す.
   * 一致しない場合, 不明なノードの共通の関数名を返す.
   */
  private String getFuncName(BhNode node, InstanceId instId, Map<InstanceId, String> cache) {
    if (node == null) {
      return mainRoutineIds.contains(instId)
          ? TextDefs.Debugger.CallStack.mainRoutine.get()
          : TextDefs.Debugger.CallStack.unknown.get();
    }
    String name = cache.get(node.getInstanceId());
    if (name == null) {
      name = node.getAlias();
      cache.put(node.getInstanceId(), name);
    }
    return name;
  }

  private void logErrMsg(Exception exception) {
    String errMsg = "";
    if (exception != null) {
      errMsg = exception.getMessage();
      if (exception.getCause() != null) {
        errMsg += "\n" + exception.getCause().getMessage();
      }
    }
    if (!errMsg.isEmpty()) {
      LogManager.logger().error(errMsg);
    }
  }

  /** {@link BhVariable} オブジェクトから {@link Variable} オブジェクトを作成する. */
  private Variable createVariable(BhVariable bhVar) {
    BhNode node = instIdToNode.get(InstanceId.of(bhVar.id.toString()));
    String name = node.getUserDefinedName().orElse("");
    if (bhVar instanceof BhListVariable bhListVar) {
      return new ListVariable(bhVar.id, name, node, bhListVar.length, bhListVar.slices);
    } else if (bhVar instanceof BhScalarVariable bhScalarVar) {
      return new ScalarVariable(bhVar.id, name, node, bhScalarVar.val);
    }
    throw new AssertionError("Unknown variable type: " + bhVar);
  }
}
