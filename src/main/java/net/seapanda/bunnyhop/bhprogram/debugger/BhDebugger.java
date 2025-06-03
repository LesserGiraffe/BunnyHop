package net.seapanda.bunnyhop.bhprogram.debugger;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import net.seapanda.bunnyhop.bhprogram.common.message.BhCallStackItem;
import net.seapanda.bunnyhop.bhprogram.common.message.BhProgramException;
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
  public void receive(BhProgramException exception) {
    var cache = new HashMap<InstanceId, String>();
    List<CallStackItem> callStack = exception.getCallStack().stream()
        .map(item -> newCallStackItem(item, exception.getThreadId(), cache))
        .toList();
    String errMsg = getErrMsg(exception);
    String runtimeErrOccured = TextDefs.Debugger.runtimErrOccured.get();
    msgService.error("%s\n%s\n".formatted(runtimeErrOccured, errMsg));
    var context = new ThreadContext(exception.getThreadId(), callStack, errMsg, true);
    cbRegistry.onThreadContextGotInvoker.invoke(new ThreadContextGotEvent(this, context));
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
