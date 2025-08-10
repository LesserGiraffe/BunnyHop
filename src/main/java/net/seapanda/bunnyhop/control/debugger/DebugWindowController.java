package net.seapanda.bunnyhop.control.debugger;

import java.util.HashMap;
import java.util.Map;
import javafx.fxml.FXML;
import javafx.scene.input.MouseButton;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.VBox;
import net.seapanda.bunnyhop.bhprogram.ThreadSelection;
import net.seapanda.bunnyhop.bhprogram.common.BhThreadState;
import net.seapanda.bunnyhop.bhprogram.debugger.Debugger;
import net.seapanda.bunnyhop.bhprogram.debugger.Debugger.ThreadSelectionEvent;
import net.seapanda.bunnyhop.bhprogram.debugger.ThreadContext;

/**
 * デバッグのための操作を提供するする UI コンポーネントのコントローラ.
 *
 * @author K.Koike
 */
public class DebugWindowController {
  
  @FXML private VBox debugWindowBase;
  @FXML private ThreadSelectorController threadSelectorController;
  @FXML private ThreadStateViewController threadStateViewController;
  @FXML private StepExecutionViewController stepExecutionViewController;
  private Debugger debugger;

  /** スレッド ID とスレッドコンテキストのマップ. */
  private final Map<Long, ThreadContext> threadIdToContext = new HashMap<>();

  /** 初期化する. */
  public synchronized void initialize(Debugger debugger) {
    this.debugger = debugger;
    threadSelectorController.initialize(debugger);
    threadStateViewController.initialize(debugger);
    stepExecutionViewController.initialize(debugger);
    Debugger.CallbackRegistry registry = debugger.getCallbackRegistry();
    registry.getOnThreadContextReceived().add(event -> addThreadContext(event.context()));
    registry.getOnCleared().add(event -> clear());
    debugger.getCallbackRegistry().getOnThreadSelectionChanged().add(this::showThreadState);
    debugWindowBase.addEventFilter(MouseEvent.ANY, this::consumeIfNotAcceptable);
  }

  /** 受付不能なマウスイベントを consume する. */
  private void consumeIfNotAcceptable(MouseEvent event) {
    MouseButton button = event.getButton();
    if (button != MouseButton.PRIMARY) {
      event.consume();
    }
  }

  /**
   * スレッドの情報を追加する.
   *
   * @param context 追加するスレッドの情報
   */
  private synchronized void addThreadContext(ThreadContext context) {
    long threadId = context.threadId();
    if (threadId < 1) {
      return;
    }
    if (context.state() == BhThreadState.FINISHED
        && !threadIdToContext.containsKey(context.threadId())) {
      return;
    }
    threadIdToContext.put(threadId, context);
    threadSelectorController.addToSelection(threadId);
    boolean isSelectedThread =
        debugger.getThreadSelection().equals(ThreadSelection.of(context.threadId()));
    if (isSelectedThread) {
      threadStateViewController.showThreadState(context);
    }
  }

  /** デバッグ情報をクリアする. */
  private synchronized void clear() {
    threadIdToContext.clear();
  }

  /** スレッドの状態を表示する. */
  private void showThreadState(ThreadSelectionEvent event) {
    if (event.newVal().equals(ThreadSelection.ALL) 
        || event.newVal().equals(ThreadSelection.NONE)) {
      threadStateViewController.hideThreadState();
      return;
    }
    ThreadContext context = threadIdToContext.get(event.newVal().getThreadId());
    threadStateViewController.showThreadState(context);
  }

  /**
   * このコントローラが管理する UI コンポーネントの可視性を切り替える.
   *
   * @param visible このコントローラが管理する UI コンポーネントを表示する場合 true
   */
  public void setVisibility(boolean visible) {
    if (visible) {
      debugWindowBase.getScene().getWindow().setOpacity(1.0);
    } else {
      debugWindowBase.getScene().getWindow().setOpacity(0);
    }
  }
}
