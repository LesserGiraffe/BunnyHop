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

package net.seapanda.bunnyhop.model.workspace;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.SequencedSet;
import java.util.stream.Collectors;
import net.seapanda.bunnyhop.common.BhConstants;
import net.seapanda.bunnyhop.model.BhNodePlacer;
import net.seapanda.bunnyhop.model.node.BhNode;
import net.seapanda.bunnyhop.model.node.BhNode.Swapped;
import net.seapanda.bunnyhop.undo.UserOperation;
import net.seapanda.bunnyhop.utility.TetraConsumer;
import net.seapanda.bunnyhop.utility.Vec2D;
import org.apache.commons.lang3.function.TriConsumer;
import org.apache.commons.lang3.mutable.MutableInt;

/**
 * カット & ペーストの機能を提供するクラス.
 *
 * @author K.Koike
 */
public class CutAndPaste {
  
  /** カット予定のノード. */
  private final SequencedSet<BhNode> readyToCut = new LinkedHashSet<>();
  private final EventManager eventManager = this.new EventManager();
  /** ノードの貼り付け位置をずらすためのカウンタ. */
  private final MutableInt pastePosOffsetCount;

  /** コンストラクタ. */
  public CutAndPaste(MutableInt pastePosOffsetCount) {
    this.pastePosOffsetCount = pastePosOffsetCount;
  }

  /**
   * カット予定の {@link BhNode} をリストに追加する.
   * 既存のカット予定, コピー予定のノードはそれぞれのリストから取り除かれる.
   *
   * @param toAdd カット予定の {@link BhNode} を格納したコレクション
   * @param userOpe undo 用コマンドオブジェクト
   */
  public void addNodeToList(BhNode toAdd, UserOperation userOpe) {
    if (readyToCut.contains(toAdd) || toAdd.isDeleted()) {
      return;
    }
    readyToCut.add(toAdd);
    eventManager.invokeOnCutNodeAdded(toAdd, userOpe);
    userOpe.pushCmdOfAddNodeToCutList(this, toAdd);
    toAdd.getEventManager().addOnWorkspaceChanged(eventManager.onWorkspaceChanged);
  }

  /**
   * カット予定のBhNodeリストをクリアする.
   *
   * @param userOpe undo 用コマンドオブジェクト
   */
  public void clearList(UserOperation userOpe) {
    while (readyToCut.size() != 0) {
      removeNodeFromList(readyToCut.getFirst(), userOpe);
    }    
  }

  /**
   * カット予定のノードリストからノードを取り除く.
   *
   * @param toRemove 取り除くノード
   * @param userOpe undo 用コマンドオブジェクト
   */
  public void removeNodeFromList(BhNode toRemove, UserOperation userOpe) {
    if (readyToCut.contains(toRemove)) {
      readyToCut.remove(toRemove);
      eventManager.invokeOnCutNodeRemoved(toRemove, userOpe);
      userOpe.pushCmdOfRemoveNodeFromCutList(this, toRemove);
      toRemove.getEventManager().removeOnWorkspaceChanged(eventManager.onWorkspaceChanged);
    }
  }

  /** カット予定のノードのセットを返す. */
  public SequencedSet<BhNode> getList() {
    return new LinkedHashSet<>(readyToCut);
  }
  
  /**
   * カット予定リストのノードを引数で指定したワークスペースに移動する.
   *
   * @param wsToPasteIn 貼り付け先のワークスペース
   * @param pasteBasePos 貼り付け基準位置
   * @param userOpe undo 用コマンドオブジェクト
   */
  public void paste(
      Workspace wsToPasteIn, Vec2D pasteBasePos, UserOperation userOpe) {
    if (readyToCut.isEmpty()) {
      return;
    }
    Collection<BhNode> candidates = readyToCut.stream()
        .filter(this::canCut).collect(Collectors.toCollection(HashSet::new));

    Collection<BhNode> nodesToPaste = candidates.stream()
        .filter(node -> node.getEventInvoker().onCutRequested(candidates, userOpe))
        .collect(Collectors.toCollection(ArrayList::new));

    // 貼り付け処理
    for (var node : nodesToPaste) {
      SequencedSet<Swapped> swappedNodes = BhNodePlacer.moveToWs(
          wsToPasteIn,
          node,
          pasteBasePos.x,
          pasteBasePos.y + pastePosOffsetCount.getValue() * BhConstants.LnF.REPLACED_NODE_SHIFT * 2,
          userOpe);
      Vec2D size = node.getViewProxy().getSizeIncludingOuters(true);
      pasteBasePos.x += size.x + BhConstants.LnF.REPLACED_NODE_SHIFT * 2;
      execHookOnPaste(node, swappedNodes, userOpe);
    }
    if (pastePosOffsetCount.getValue() > 2) {
      pastePosOffsetCount.setValue(-2);
    } else {
      pastePosOffsetCount.increment();
    }
    clearList(userOpe);
  }

  /** ペースト時のイベントハンドラを実行する. */
  private void execHookOnPaste(
      BhNode node, SequencedSet<Swapped> swappedNodes, UserOperation userOpe) {
    if (!swappedNodes.isEmpty()) {
      node.getEventInvoker().onMovedFromChildToWs(
          swappedNodes.getFirst().newNode().findParentNode(),
          swappedNodes.getFirst().newNode().findRootNode(),
          swappedNodes.getFirst().newNode(),
          true,
          userOpe);
    }
    for (var swapped : swappedNodes) {
      swapped.newNode().findParentNode().getEventInvoker().onChildReplaced(
          node,
          swapped.newNode(),
          swapped.newNode().getParentConnector(),
          userOpe);
    }
  }

  /**
   * カットの対象になるかどうか判定する.
   *
   * @param node 判定対象のノード
   * @return カットの対象になる場合 true
   */
  private boolean canCut(BhNode node) {
    return (node.isChild() && node.findRootNode().isRoot())
        || node.isRoot();
  }

  /**
   * このオブジェクトに対するイベントハンドラの追加と削除を行うオブジェクトを返す.
   *
   * @return このオブジェクトに対するイベントハンドラの追加と削除を行うオブジェクト
   */
  public EventManager getEventManager() {
    return eventManager;
  }

  /** イベントハンドラの管理を行うクラス. */
  public class EventManager {

    /** カット予定ノードが追加されたときのイベントハンドラのセット. */
    private final
        SequencedSet<TriConsumer<? super CutAndPaste, ? super BhNode, ? super UserOperation>>
        onCutNodeAddedList = new LinkedHashSet<>();
    /** カット予定ノードが削除されたときのイベントハンドラのセット. */
    private final
        SequencedSet<TriConsumer<? super CutAndPaste, ? super BhNode, ? super UserOperation>>
        onCutNodeRemovedList = new LinkedHashSet<>();
    /** 管理している {@link BhNode} のワークスペースが変わった時のイベントハンドラ. */
    private final
        TetraConsumer<? super BhNode, ? super Workspace, ? super Workspace, ? super UserOperation>
        onWorkspaceChanged = (node, oldWs, newWs, userOpe) -> {
          if (oldWs != newWs) {
            CutAndPaste.this.removeNodeFromList(node, userOpe);
          }
        };

    /**
     * ノードがカット候補になったときのイベントハンドラを追加する.
     *
     * @param handler 追加するイベントハンドラ
     */
    public void addOnCutNodeAdded(
        TriConsumer<? super CutAndPaste, ? super BhNode, ? super UserOperation> handler) {
      onCutNodeAddedList.addLast(handler);
    }

    /**
     * ノードがカット候補になったときのイベントハンドラを削除する.
     *
     * @param handler 削除するイベントハンドラ
     */
    public void removeOnCutNodeAdded(
        TriConsumer<? super CutAndPaste, ? super BhNode, ? super UserOperation> handler) {
      onCutNodeAddedList.remove(handler);
    }

    /** ノードがカット候補になったときときのイベントハンドラを呼び出す. */
    private void invokeOnCutNodeAdded(BhNode node, UserOperation userOpe) {
      onCutNodeAddedList.forEach(handler -> handler.accept(CutAndPaste.this, node, userOpe));
    }

    /**
     * ノードがカット候補でなくなったときのイベントハンドラを追加する.
     *
     * @param handler 追加するイベントハンドラ
     */
    public void addOnCutNodeRemoved(
        TriConsumer<? super CutAndPaste, ? super BhNode, ? super UserOperation> handler) {
      onCutNodeRemovedList.addLast(handler);
    }

    /**
     * ノードがカット候補でなくなったときのイベントハンドラを削除する.
     *
     * @param handler 削除するイベントハンドラ
     */
    public void removeOnCutNodeRemoved(
        TriConsumer<? super CutAndPaste, ? super BhNode, ? super UserOperation> handler) {
      onCutNodeRemovedList.remove(handler);
    }

    /** ノードがカット候補でなくなったときのイベントハンドラを呼び出す. */
    private void invokeOnCutNodeRemoved(BhNode node, UserOperation userOpe) {
      onCutNodeRemovedList.forEach(handler -> handler.accept(CutAndPaste.this, node, userOpe));
    }
  }
}
