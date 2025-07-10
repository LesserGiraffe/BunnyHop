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
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;
import net.seapanda.bunnyhop.bhprogram.common.BhSymbolId;
import net.seapanda.bunnyhop.bhprogram.common.message.exception.BhProgramException;
import net.seapanda.bunnyhop.bhprogram.common.message.thread.BhThreadContext;
import net.seapanda.bunnyhop.common.BhSettings;
import net.seapanda.bunnyhop.common.TextDefs;
import net.seapanda.bunnyhop.model.node.BhNode;
import net.seapanda.bunnyhop.model.node.syntaxsymbol.InstanceId;
import net.seapanda.bunnyhop.model.workspace.WorkspaceSet;
import net.seapanda.bunnyhop.service.MessageService;
import net.seapanda.bunnyhop.utility.function.ConsumerInvoker;

/**
 * BhProgram のデバッガクラス.
 *
 * @author K.Koike
 */
public class BhDebugger implements Debugger, DebugInfoReceiver {
  
  private final MessageService msgService;
  private final CallbackRegistryImpl cbRegistry = new CallbackRegistryImpl();
  /**
   * key: {@link BhNode} のインスタンス ID.
   * value: key に対応する {@link BhBode}.
   */
  private final Map<InstanceId, BhNode> instIdToNode = new ConcurrentHashMap<>();

  /** コンストラクタ. */
  public BhDebugger(WorkspaceSet wss, MessageService msgService) {
    this.msgService = msgService;
    wss.getCallbackRegistry().getOnNodeAdded().add(
        event -> instIdToNode.put(event.node().getInstanceId(), event.node()));
    wss.getCallbackRegistry().getOnNodeRemoved().add(
        event -> instIdToNode.remove(event.node().getInstanceId()));
  }

  @Override
  public void clear() {
    cbRegistry.onClearedInvoker.invoke(new ClearEvent(this));
  }

  @Override
  public CallbackRegistry getCallbackRegistry() {
    return cbRegistry;
  }

  @Override
  public void receive(BhThreadContext context) {
    if (context.getException() != null) {
      String errMsg = getErrMsg(context.getException());
      String runtimeErrOccured = TextDefs.Debugger.runtimErrOccured.get();
      msgService.error("%s\n%s\n".formatted(runtimeErrOccured, errMsg));
    }
    var cache = new HashMap<InstanceId, String>();
    List<CallStackItem> callStack = context.getCallStack().stream()
        .map(item -> createCallStackItem(
            item.symbolId(), item.frameIdx(), context.getThreadId(), false, cache))
        .collect(Collectors.toCollection(ArrayList::new));    

    if (!context.getNextStep().equals(BhSymbolId.NONE)) {
      int frameIdx = (callStack.size() > 0) ? callStack.getLast().getIdx() + 1 : 0;
      CallStackItem item =
          createCallStackItem(context.getNextStep(), frameIdx, context.getThreadId(), true, cache);
      callStack.addLast(item);
    }
    var threadContext = new ThreadContext(
        context.getThreadId(), context.getState(), callStack, context.getException());
    cbRegistry.onThreadContextGotInvoker.invoke(new ThreadContextGotEvent(this, threadContext));
  }

  /** {@code exception} からエラーメッセージを取得する. */
  private String getErrMsg(BhProgramException exception) {
    String msg = exception.getMessage();
    if (exception.getCause() == null) {
      return truncateErrMsg(msg);
    }
    String errMsg = switch (exception.getCause()) {
      case StackOverflowError e -> TextDefs.Debugger.stackOverflow.get();
      case OutOfMemoryError e -> TextDefs.Debugger.outOfMemory.get();
      default -> {
        msg += msg.isEmpty() ? "" : "\n";
        yield msg + exception.getCause().getMessage();
      }
    };
    return truncateErrMsg(errMsg);
  }

  private static String truncateErrMsg(String errMsg) {
    return (errMsg.length() > BhSettings.Message.maxErrMsgChars)
        ? errMsg.substring(0, BhSettings.Message.maxErrMsgChars) + "..."
        : errMsg;
  }


  /**
   * {@link CallStackItem} を作成する.
   *
   * @param symbolId 関数呼び出しノードの ID
   * @param frameIdx コールスタックのスタックフレームのインデックス
   * @param threadId コールスタックを持つスレッドの ID
   * @param isNotCalled 関数呼び出しを未実行の場合 true
   * @param aliasCache 関数呼び出しノードのエイリアスのキャッシュ
   */
  private CallStackItem createCallStackItem(
      BhSymbolId symbolId,
      int frameIdx,
      long threadId,
      boolean isNotCalled,
      Map<InstanceId, String> aliasCache) {
    BhNode node = instIdToNode.get(InstanceId.of(symbolId.toString()));
    if (node == null) {
      return new CallStackItem(
          frameIdx, threadId, TextDefs.Debugger.CallStack.unknown.get(), isNotCalled);
    }
    String alias = aliasCache.get(node.getInstanceId());
    if (alias == null) {
      alias = node.getAlias();
      aliasCache.put(node.getInstanceId(), alias);
    }
    return new CallStackItem(frameIdx, threadId, alias, node, isNotCalled);
  }

  /** イベントハンドラの管理を行うクラス. */
  public class CallbackRegistryImpl implements CallbackRegistry {

    /** {@link ThreadContext} を取得したときのイベントハンドラを管理するオブジェクト. */
    private ConsumerInvoker<ThreadContextGotEvent> onThreadContextGotInvoker =
        new ConsumerInvoker<>();

    /** デバッグ情報をクリアしたときのイベントハンドラを管理するオブジェクト. */
    private ConsumerInvoker<ClearEvent> onClearedInvoker = new ConsumerInvoker<>();

    @Override
    public ConsumerInvoker<ThreadContextGotEvent>.Registry getOnThreadContextGot() {
      return onThreadContextGotInvoker.getRegistry();
    }

    @Override
    public ConsumerInvoker<ClearEvent>.Registry getOnCleared() {
      return onClearedInvoker.getRegistry();
    }
  }
}
