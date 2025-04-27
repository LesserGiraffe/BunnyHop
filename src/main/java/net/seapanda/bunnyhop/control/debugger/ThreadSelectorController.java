package net.seapanda.bunnyhop.control.debugger;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.function.Consumer;
import javafx.css.PseudoClass;
import javafx.fxml.FXML;
import javafx.geometry.Point2D;
import javafx.scene.control.ComboBox;
import javafx.scene.control.ListCell;
import javafx.scene.control.Tooltip;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.VBox;
import javafx.scene.text.Text;
import net.seapanda.bunnyhop.bhprogram.debugger.ThreadContext;
import net.seapanda.bunnyhop.common.BhConstants;
import net.seapanda.bunnyhop.common.BhSettings;
import net.seapanda.bunnyhop.common.Rem;
import net.seapanda.bunnyhop.common.TextDefs;
import net.seapanda.bunnyhop.view.ViewUtil;
import net.seapanda.bunnyhop.view.node.component.SelectableItem;

/**
 * デバッガのスレッド選択コンポーネントのコントローラ.
 *
 * @author K.Koike
 */
public class ThreadSelectorController {
  
  @FXML private VBox threadSelVbox;
  @FXML private ComboBox<SelectableItem<Long, String>> threadComboBox;
  @FXML private Text threadStatusText;

  /** スレッド ID とスレッドコンテキストのマップ. */
  private final Map<Long, ThreadContext> threadIdToThreadContext = new HashMap<>();
  /** スレッドが選択されたときのイベントハンドラ. */
  private Consumer<? super Long> onThreadSelected = threadId -> {};
  /** エラーメッセージを表示するツールチップ. */
  private final Tooltip errMsgTooltip = new Tooltip();

  /** 初期化する. */
  public void initialize() {
    errMsgTooltip.setId(BhConstants.UiId.BH_RUNTIME_ERR_MSG);
    errMsgTooltip.setAutoHide(false);
    errMsgTooltip.addEventFilter(MouseEvent.MOUSE_PRESSED, event -> errMsgTooltip.hide());
    threadComboBox.setButtonCell(new ThreadSelectorListCell());
    threadComboBox.setCellFactory(items -> new ThreadSelectorListCell());
    threadComboBox.valueProperty().addListener(
        (observable, oldVal, newVal) -> invokeOnThreadSelected(newVal));
    reset();
  }

  /**
   * スレッドが選択されたときのイベントハンドラを登録する.
   *
   * @param handler 登録するイベントハンドラ.
   */
  void setOnThreadSelected(Consumer<? super Long> handler) {
    if (handler != null) {
      onThreadSelected = handler;
    }
  }

  /** {@code threadId} で指定したスレッドを選択する. */
  private void selectThread(long threadId) {
    Optional<SelectableItem<Long, String>> toBeSelected = threadComboBox.getItems().stream()
        .filter(item -> item.getModel() == threadId)
        .findFirst();
    if (toBeSelected.isEmpty()) {
      return;
    }
    ViewUtil.runSafe(() -> threadComboBox.getSelectionModel().select(toBeSelected.get()));
  }

  /** このコントローラの管理するコンポーネントを初期状態に戻す. */
  void reset() {
    String threadName = "%s %s".formatted(
        TextDefs.Debugger.thread.get(), TextDefs.Debugger.notSelected.get());
    threadComboBox.getItems().clear();
    threadComboBox.getItems().add(new SelectableItem<>(-1L, threadName));
    threadComboBox.getSelectionModel().selectFirst();
    threadIdToThreadContext.clear();
  }

  /**
   * スレッドの選択肢に {@code context} で指定したスレッドを追加する.
   *
   * @param context 選択肢に追加するスレッドの情報を格納したオブジェクト
   */
  void addToThreadSelection(ThreadContext context) {
    long threadId = context.threadId();
    if (threadId < 1) {
      return;
    }
    boolean isNewThreadId = !threadIdToThreadContext.containsKey(threadId);
    if (isNewThreadId) {
      threadComboBox.getItems().addLast(new SelectableItem<>(
          threadId, "%s %s".formatted(TextDefs.Debugger.thread.get(), threadId)));
    }
    threadIdToThreadContext.put(context.threadId(), context);
    if (threadComboBox.getValue().getModel() < 0L) {
      selectThread(threadId);
    }
  }

  /**
   * {@code context} に対応するスレッドの状態を表示する.
   *
   * @param context 表示するスレッドの状態を格納したオブジェクト
   */
  private void showThreadStatus(ThreadContext context) {
    PseudoClass pseudo = PseudoClass.getPseudoClass(BhConstants.Css.PSEUDO_ERROR);
    if (context == null || !context.errorOccured()) {
      threadStatusText.setText("");
      threadStatusText.pseudoClassStateChanged(pseudo, false);
      threadStatusText.setOnMousePressed(null);

    } else if (context.errorOccured()) {
      errMsgTooltip.setText(truncateErrMsg(context.msg()));
      
      threadStatusText.setText("%s: %s".formatted(
          TextDefs.Debugger.ThreadStatus.status.get(), TextDefs.Debugger.ThreadStatus.error.get()));
      threadStatusText.pseudoClassStateChanged(pseudo, true);
      threadStatusText.setOnMousePressed(event -> toggleErrToolTipVisibility());
    }
  }

  private static String truncateErrMsg(String errMsg) {
    return (errMsg.length() > BhSettings.Message.maxErrMsgChars)
        ? errMsg.substring(0, BhSettings.Message.maxErrMsgChars) + "..."
        : errMsg;
  }

  private void invokeOnThreadSelected(SelectableItem<Long, String> item) {
    ViewUtil.runSafe(() -> {
      long threadId = (item == null) ? -1 : item.getModel();
      onThreadSelected.accept(threadId);
      ThreadContext context = (item == null) ? null : threadIdToThreadContext.get(threadId);
      showThreadStatus(context);
      errMsgTooltip.hide();
    });
  }

  /** エラーメッセージの可視性を切り替える. */
  private void toggleErrToolTipVisibility() {
    if (errMsgTooltip.isShowing()) {
      errMsgTooltip.hide(); 
    } else {
      Point2D p = threadStatusText.localToScreen(0.5 * Rem.VAL, 1.5 * Rem.VAL);
      errMsgTooltip.show(threadStatusText, p.getX(), p.getY());
    }
  }

  /** スレッド選択コンボボックスのアイテムの View. */
  private class ThreadSelectorListCell extends ListCell<SelectableItem<Long, String>> {
    @Override
    protected void updateItem(SelectableItem<Long, String> item, boolean empty) {
      super.updateItem(item, empty);
      if (!empty && item != null) {
        setText(item.getView());
      } else {
        setText(null);
      }
    }
  }  
}