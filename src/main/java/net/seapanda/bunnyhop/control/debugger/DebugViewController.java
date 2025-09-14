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

package net.seapanda.bunnyhop.control.debugger;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import javafx.fxml.FXML;
import javafx.scene.Node;
import javafx.scene.control.ScrollPane;
import net.seapanda.bunnyhop.bhprogram.common.BhThreadState;
import net.seapanda.bunnyhop.bhprogram.debugger.Debugger;
import net.seapanda.bunnyhop.bhprogram.debugger.Debugger.CurrentStackFrameChangedEvent;
import net.seapanda.bunnyhop.bhprogram.debugger.Debugger.CurrentThreadChangedEvent;
import net.seapanda.bunnyhop.bhprogram.debugger.StackFrameSelection;
import net.seapanda.bunnyhop.bhprogram.debugger.ThreadContext;
import net.seapanda.bunnyhop.bhprogram.debugger.ThreadSelection;
import net.seapanda.bunnyhop.bhprogram.debugger.variable.StackFrameId;
import net.seapanda.bunnyhop.bhprogram.debugger.variable.VariableInfo;
import net.seapanda.bunnyhop.common.TextDefs;
import net.seapanda.bunnyhop.service.LogManager;
import net.seapanda.bunnyhop.undo.UserOperation;
import net.seapanda.bunnyhop.view.ViewConstructionException;
import net.seapanda.bunnyhop.view.ViewUtil;
import net.seapanda.bunnyhop.view.factory.DebugViewFactory;

/**
 * デバッグ情報を表示する UI コンポーネントのコントローラ.
 *
 * @author K.Koike
 */
public class DebugViewController {

  @FXML private ScrollPane callStackScrollPane;
  @FXML private ScrollPane localVarScrollPane;
  @FXML private ScrollPane globalVarScrollPane;

  /** スレッド ID とコールスタックビューのマップ. */
  private final Map<Long, CallStackController> threadIdToCallStackCtrl = new HashMap<>();
  /** スタックフレームごとに {@link VariableInspectionController} を保持するオブジェクト. */
  private final VarInspCtrlRegistry localVarInspCtrlRegistry = new VarInspCtrlRegistry();
  /** グローバル変数情報を表示するための {@link VariableInspectionController} オブジェクト. */
  private VariableInspectionController globalVarInspCtrl;
  /** スレッド ID とスレッドコンテキストのマップ. */
  private final Map<Long, ThreadContext> threadIdToContext = new HashMap<>();
  /** 空の情報を表示するコールスタックビューのコントローラ. */
  private CallStackController emptyCallStackCtrl;
  /** 空の変数情報を表示する変数検査ビューのコントローラ. */
  private VariableInspectionController emptyVarInspCtrl;
  private final DebugViewFactory factory;
  private final Debugger debugger;

  /** コンストラクタ. */
  public DebugViewController(Debugger debugger, DebugViewFactory factory) {
    this.factory = factory;
    this.debugger = debugger;
  }

  /** イベントハンドラを設定する. */
  private void setEventHandlers() {
    Debugger.CallbackRegistry registry = debugger.getCallbackRegistry();
    registry.getOnThreadContextAdded().add(event -> addThreadContext(event.context()));
    registry.getOnCurrentThreadChanged().add(this::showCallStackView);
    registry.getOnVariableInfoAdded().add(event -> addVariableInfo(event.info()));
    registry.getOnCurrentStackFrameChanged().add(this::showVariableInspectionView);
    registry.getOnCleared().add(event -> resetContents());
  }

  /**
   * このコントローラの UI 要素を初期化する.
   *
   * @throws ViewConstructionException このコントローラが管理するビューの初期化に失敗したとき.
   */
  @FXML
  public void initialize() throws ViewConstructionException {
    setEventHandlers();
    boolean success = createEmptyCtrl();
    if (!success) {
      throw new ViewConstructionException("Could not create empty view.");
    }
    success = resetContents();
    if (!success) {
      throw new ViewConstructionException("Could not reset view.");
    }
  }

  /** 空の情報を表示するビューとコントローラを作成する. */
  private boolean createEmptyCtrl() {
    long threadId = ThreadSelection.NONE.getThreadId();
    emptyCallStackCtrl = createCallStackCtrl(new ThreadContext(threadId)).orElse(null);

    var stackFrameId =
        new StackFrameId(ThreadSelection.NONE.getThreadId(), StackFrameSelection.NONE.getIndex());
    var varInfo = new VariableInfo(stackFrameId);
    emptyVarInspCtrl = createVarInspectionCtrl(varInfo, true).orElse(null);

    return emptyCallStackCtrl != null && emptyVarInspCtrl != null;
  }

  /**
   * スレッドの情報を追加する.
   *
   * @param context 追加するスレッドの情報
   */
  private synchronized void addThreadContext(ThreadContext context) {
    long threadId = context.threadId;
    if (threadId < 1) {
      return;
    }
    if (context.state == BhThreadState.FINISHED && !threadIdToContext.containsKey(threadId)) {
      return;
    }

    localVarInspCtrlRegistry.remove(threadId);
    if (threadIdToContext.isEmpty()) {
      debugger.selectCurrentThread(ThreadSelection.of(threadId));
    }
    boolean isContextThreadSameAsDebugThread =
        debugger.getCurrentThread().equals(ThreadSelection.of(threadId));

    if (isContextThreadSameAsDebugThread) {
      debugger.selectCurrentStackFrame(StackFrameSelection.NONE);
    }
    threadIdToContext.put(threadId, context);
    CallStackController callStackCtrl = createCallStackCtrl(context).orElse(null);
    if (callStackCtrl == null) {
      return;
    }
    CallStackController oldCallStackCtrl = threadIdToCallStackCtrl.put(threadId, callStackCtrl);
    if (oldCallStackCtrl != null) {
      oldCallStackCtrl.discard();
    }
    // 最初にコールスタックのトップを選択しておく.
    if (!callStackCtrl.getThreadContext().callStack.isEmpty()) {
      callStackCtrl.getThreadContext().callStack.getLast().select(new UserOperation());
    }
    if (isContextThreadSameAsDebugThread) {
      ViewUtil.runSafe(() -> callStackScrollPane.setContent(callStackCtrl.getView()));
    }
  }

  /** {@code context} からコールスタックを表示するビューを作成する. */
  private Optional<CallStackController> createCallStackCtrl(ThreadContext context) {
    try {
      return Optional.ofNullable(factory.createCallStackView(context));
    } catch (ViewConstructionException e) {
      LogManager.logger().error(e.toString());
    }
    return Optional.empty();
  }

  /** コールスタックビューを表示する. */
  private synchronized void showCallStackView(CurrentThreadChangedEvent event) {
    long currentThreadId = event.newVal().getThreadId();
    Node callStackView = threadIdToCallStackCtrl.containsKey(currentThreadId)
        ? threadIdToCallStackCtrl.get(currentThreadId).getView()
        : emptyCallStackCtrl.getView();
    ViewUtil.runSafe(() -> callStackScrollPane.setContent(callStackView));
  }

  /**
   * 変数情報を追加する.
   *
   * @param varInfo 追加する変数情報
   */
  private synchronized void addVariableInfo(VariableInfo varInfo) {
    StackFrameId stackFrameId = varInfo.getStackFrameId().orElse(null);
    if (stackFrameId == null) {
      globalVarInspCtrl.getModel().addVariables(varInfo.getVariables());
      return;
    }

    boolean isStackFrameNew = !localVarInspCtrlRegistry.contains(stackFrameId);
    if  (isStackFrameNew) {
      localVarInspCtrlRegistry.register(new VariableInfo(stackFrameId));
    }
    localVarInspCtrlRegistry.get(stackFrameId)
        .ifPresent(varInspCtrl -> varInspCtrl.getModel().addVariables(varInfo.getVariables()));
  }

  /** {@code varInfo} から変数検査ビューを作成する. */
  private Optional<VariableInspectionController> createVarInspectionCtrl(
      VariableInfo varInfo, boolean isLocal) {
    try {
      String viewName = isLocal
          ? TextDefs.Debugger.VarInspection.localVars.get()
          : TextDefs.Debugger.VarInspection.globalVars.get();
      return Optional.ofNullable(factory.createVariableInspectionView(varInfo, viewName));
    } catch (ViewConstructionException e) {
      LogManager.logger().error(e.toString());
    }
    return Optional.empty();
  }

  /** 変数検査ビューを表示する. */
  private synchronized void showVariableInspectionView(CurrentStackFrameChangedEvent event) {
    if (event.newVal().equals(StackFrameSelection.NONE)) {
      ViewUtil.runSafe(() -> localVarScrollPane.setContent(emptyVarInspCtrl.getView()));
    }
    var stackFrameId =
        new StackFrameId(event.currentThread().getThreadId(), event.newVal().getIndex());
    boolean isNewStackFrame = !localVarInspCtrlRegistry.contains(stackFrameId);
    if (isNewStackFrame) {
      localVarInspCtrlRegistry.register(new VariableInfo(stackFrameId));
    }
    localVarInspCtrlRegistry.get(stackFrameId).ifPresent(varInspCtrl ->
        ViewUtil.runSafe(() -> localVarScrollPane.setContent(varInspCtrl.getView())));
    if  (isNewStackFrame) {
      debugger.requestLocalVars();
    }
  }

  /**
   * 既存の {@link CallStackController} と {@link VariableInspectionController} を全て破棄して
   * このオブジェクトが管理するデバッグ情報を初期状態に戻す.
   */
  private boolean resetContents() {
    threadIdToContext.clear();
    threadIdToCallStackCtrl.values().forEach(CallStackController::discard);
    threadIdToCallStackCtrl.clear();
    localVarInspCtrlRegistry.clearAll();
    if (globalVarInspCtrl != null) {
      globalVarInspCtrl.discard();
    }
    globalVarInspCtrl = createVarInspectionCtrl(new VariableInfo(), false).orElse(null);
    if (globalVarInspCtrl == null) {
      return false;
    }
    ViewUtil.runSafe(() -> {
      callStackScrollPane.setContent(emptyCallStackCtrl.getView());
      localVarScrollPane.setContent(emptyVarInspCtrl.getView());
      globalVarScrollPane.setContent(globalVarInspCtrl.getView());
    });
    return true;
  }

  /** スレッド ID とスタックフレームインデックスのセットと {@link VariableInspectionController} の対応を保持するクラス. */
  private class VarInspCtrlRegistry {
    /** スレッド ID -> スタックフレームインデックス -> {@link VariableInspectionController}. */
    private final Map<Long, Map<Long, VariableInspectionController>> frameIdToVarInspCtrl =
        new HashMap<>();

    /** 指定されたスタックフレームに対応する変数情報を取得する. */
    Optional<VariableInspectionController> get(StackFrameId stackFrameId) {
      return Optional.ofNullable(frameIdToVarInspCtrl.get(stackFrameId.threadId()))
          .map(frameIdxToVarInfo -> frameIdxToVarInfo.get(stackFrameId.frameIdx()));
    }

    /**
     * {@code varInfo} から {@link VariableInspectionController} を作成して, このオブジェクトに登録する.
     * すでに {@code varInfo} に対応する {@link VariableInspectionController} オブジェクトが登録済であった場合,
     * そのオブジェクトの {@link VariableInspectionController#discard} を呼んだ後, 新しいもので置き換える.
     *
     * @return 登録された VariableInspectionController オブジェクト.  登録に失敗した場合 empty.
     */
    public Optional<VariableInspectionController> register(VariableInfo varInfo) {
      StackFrameId stackFrameId = varInfo.getStackFrameId().orElse(null);
      if (stackFrameId == null) {
        return Optional.empty();
      }
      var varInspCtrl = createVarInspectionCtrl(varInfo, true).orElse(null);
      if (varInspCtrl == null) {
        return Optional.empty();
      }
      frameIdToVarInspCtrl.computeIfAbsent(stackFrameId.threadId(), key -> new HashMap<>());
      VariableInspectionController oldCtrl = frameIdToVarInspCtrl
          .get(stackFrameId.threadId())
          .put(stackFrameId.frameIdx(), varInspCtrl);
      if (oldCtrl != null) {
        oldCtrl.discard();
      }
      return Optional.of(varInspCtrl);
    }

    /**
     * {@code stackFrameId} に対応する {@link VariableInspectionController} が登録されているか調べる.
     *
     * @param stackFrameId この ID に対応する {@link VariableInspectionController} が登録されているか調べる
     * @return {@code stackFrameId} に対応する {@link VariableInspectionController} が登録されている場合 true
     */
    public boolean contains(StackFrameId stackFrameId) {
      return Optional.ofNullable(frameIdToVarInspCtrl.get(stackFrameId.threadId()))
          .map(frameIdxToVarInfo -> frameIdxToVarInfo.containsKey(stackFrameId.frameIdx()))
          .orElse(false);
    }

    /** このオブジェクトに設定した変数情報をすべて消す. */
    void clearAll() {
      for (long key : new ArrayList<>(frameIdToVarInspCtrl.keySet())) {
        remove(key);
      }
    }

    /** {@code threadId} で指定したスレッド ID に関連するスタックフレームの変数情報をすべて消す. */
    void remove(long threadId) {
      Map<Long, VariableInspectionController> frameIdxToVarInspCtrl =
          frameIdToVarInspCtrl.remove(threadId);
      if (frameIdxToVarInspCtrl == null) {
        return;
      }
      frameIdxToVarInspCtrl.values().forEach(VariableInspectionController::discard);
    }
  }
}
