package net.seapanda.bunnyhop.bhprogram.debugger;

import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.SequencedSet;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;
import net.seapanda.bunnyhop.bhprogram.common.message.BhCallStackItem;
import net.seapanda.bunnyhop.bhprogram.common.message.BhProgramException;
import net.seapanda.bunnyhop.common.BhSettings;
import net.seapanda.bunnyhop.common.TextDefs;
import net.seapanda.bunnyhop.model.node.BhNode;
import net.seapanda.bunnyhop.model.node.syntaxsymbol.InstanceId;
import net.seapanda.bunnyhop.model.workspace.WorkspaceSet;
import net.seapanda.bunnyhop.service.MessageService;

/**
 * BhProgram のデバッガクラス.
 *
 * @author K.Koike
 */
public class BhDebugger implements Debugger, DebugInfoReceiver {
  
  private final MessageService msgService;
  private final EventManagerImpl eventManager = new EventManagerImpl();
  /**
   * key: {@link BhNode} のインスタンス ID.
   * value: key に対応する {@link BhBode}.
   */
  private final Map<InstanceId, BhNode> instIdToNode = new ConcurrentHashMap<>();

  /** コンストラクタ. */
  public BhDebugger(WorkspaceSet wss, MessageService msgService) {
    this.msgService = msgService;
    wss.getEventManager().addOnNodeAdded((wsSet, ws, node, userOpe) -> 
        instIdToNode.put(node.getInstanceId(), node));
    wss.getEventManager().addOnNodeRemoved((wsSet, ws, node, userOpe) ->
        instIdToNode.remove(node.getInstanceId()));
  }

  @Override
  public void clear() {
    eventManager.invokeOnCleared();
  }

  @Override
  public EventManager getEventManager() {
    return eventManager;
  }

  @Override
  public void receive(BhProgramException exception) {
    var cache = new HashMap<InstanceId, String>();
    List<CallStackItem> callStack = exception.getCallStack().stream()
        .map(item -> newCallStackItem(item, exception.getThreadId(), cache))
        .toList();
    String errMsg = getErrMsg(exception);
    String runtimeErrOccured = TextDefs.Debugger.runtimErrOccured.get();
    msgService.error("%s\n%s\n".formatted(runtimeErrOccured, errMsg));
    var context = new ThreadContext(
        exception.getThreadId(), callStack, errMsg, true);
    eventManager.invokeOnThreadContextGet(context);
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


  /** {@link BhCallStackItem} から {@link CallStackItem} を作成する. */
  private CallStackItem newCallStackItem(
      BhCallStackItem item, long threadId, Map<InstanceId, String> cache) {
    BhNode node = instIdToNode.get(InstanceId.of(item.nodeId().toString()));
    if (node == null) {
      return new CallStackItem(
          item.frameId(), threadId, TextDefs.Debugger.CallStack.unknown.get());
    }
    String alias = cache.get(node.getInstanceId());
    if (alias == null) {
      alias = node.getAlias();
      cache.put(node.getInstanceId(), alias);
    }
    return new CallStackItem(item.frameId(), threadId, alias, node);  
  }

  /** イベントハンドラの管理を行うクラス. */
  public class EventManagerImpl implements EventManager {

    /** {@link ThreadContext} を取得したときに呼び出すメソッドのリスト. */
    private SequencedSet<Consumer<? super ThreadContext>> onThreadContextGet =
        new LinkedHashSet<>();

    /** デバッグ情報をクリアしたときに呼び出すメソッドのリスト. */
    private SequencedSet<Runnable> onCleared = new LinkedHashSet<>();

    @Override
    public void addOnThreadContextGet(Consumer<? super ThreadContext> handler) {
      onThreadContextGet.addLast(handler);
    }

    @Override
    public void removeOnThreadContextGet(Consumer<? super ThreadContext> handler) {
      onThreadContextGet.remove(handler);
    }

    @Override
    public void addOnCleared(Runnable handler) {
      onCleared.addLast(handler);
    }

    @Override
    public void removeOnCleared(Runnable handler) {
      onCleared.remove(handler);
    }

    /** {@link ThreadContext} を取得したときのイベントハンドラを呼び出す. */
    private void invokeOnThreadContextGet(ThreadContext context) {
      onThreadContextGet.forEach(handler -> handler.accept(context));
    }

    /** デバッグ情報をクリアしたときのイベントハンドラを呼び出す. */
    private void invokeOnCleared() {
      onCleared.forEach(handler -> handler.run());
    }
  }
}
