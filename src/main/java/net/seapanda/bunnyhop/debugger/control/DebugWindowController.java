package net.seapanda.bunnyhop.debugger.control;


import java.util.HashMap;
import java.util.Map;
import javafx.fxml.FXML;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import net.seapanda.bunnyhop.bhprogram.common.BhThreadState;
import net.seapanda.bunnyhop.debugger.model.Debugger;
import net.seapanda.bunnyhop.debugger.model.Debugger.CurrentThreadChangedEvent;
import net.seapanda.bunnyhop.debugger.model.thread.ThreadContext;
import net.seapanda.bunnyhop.debugger.model.thread.ThreadSelection;

/**
 * デバッグのための操作を提供するする UI コンポーネントのコントローラ.
 *
 * @author K.Koike
 */
public class DebugWindowController {

  @FXML private VBox debugWindowBase;
  @FXML private ThreadSelectorController threadSelectorController;
  @FXML private ThreadStateViewController threadStateViewController;
  private final Debugger debugger;

  /** スレッド ID とスレッドコンテキストのマップ. */
  private final Map<Long, ThreadContext> threadIdToContext = new HashMap<>();

  /** コンストラクタ. */
  public DebugWindowController(Debugger debugger) {
    this.debugger = debugger;
  }

  /** このコントローラを初期化する. */
  @FXML
  public void initialize() {
    setEventHandlers();
  }

  /** イベントハンドラを登録する. */
  private void setEventHandlers() {
    Debugger.CallbackRegistry registry = debugger.getCallbackRegistry();
    registry.getOnThreadContextAdded().add(event -> addThreadContext(event.context()));
    registry.getOnCleared().add(event -> clear());
    debugger.getCallbackRegistry().getOnCurrentThreadChanged().add(this::showThreadState);
  }

  /**
   * スレッドの情報を追加する.
   *
   * @param context 追加するスレッドの情報
   */
  private void addThreadContext(ThreadContext context) {
    long threadId = context.threadId;
    if (threadId < 1) {
      return;
    }
    if (context.state == BhThreadState.FINISHED
        && !threadIdToContext.containsKey(threadId)) {
      return;
    }
    threadIdToContext.put(threadId, context);
    threadSelectorController.addToOptions(threadId);
    boolean isSelectedThread = debugger.getCurrentThread().equals(ThreadSelection.of(threadId));
    if (isSelectedThread) {
      threadStateViewController.showThreadState(context);
    }
  }

  /** デバッグ情報をクリアする. */
  private void clear() {
    threadIdToContext.clear();
  }

  /** スレッドの状態を表示する. */
  private void showThreadState(CurrentThreadChangedEvent event) {
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
      ((Stage) debugWindowBase.getScene().getWindow()).show();
    } else {
      debugWindowBase.getScene().getWindow().hide();
    }
  }
}
