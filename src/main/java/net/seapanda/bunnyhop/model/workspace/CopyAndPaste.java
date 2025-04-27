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
import java.util.function.Predicate;
import java.util.stream.Collectors;
import net.seapanda.bunnyhop.common.BhConstants;
import net.seapanda.bunnyhop.model.BhNodePlacer;
import net.seapanda.bunnyhop.model.factory.BhNodeFactory;
import net.seapanda.bunnyhop.model.factory.BhNodeFactory.MvcType;
import net.seapanda.bunnyhop.model.node.BhNode;
import net.seapanda.bunnyhop.undo.UserOperation;
import net.seapanda.bunnyhop.utility.TetraConsumer;
import net.seapanda.bunnyhop.utility.Vec2D;
import org.apache.commons.lang3.function.TriConsumer;
import org.apache.commons.lang3.mutable.MutableInt;

/**
 * コピー & ペーストの機能を提供するクラス.
 *
 * @author K.Koike
 */
public class CopyAndPaste {
 
  /** コピー予定のノード. */
  private final SequencedSet<BhNode> readyToCopy = new LinkedHashSet<>();
  private final EventManager eventManager = this.new EventManager();
  private final BhNodeFactory factory;
  /** ノードの貼り付け位置をずらすためのカウンタ. */
  private final MutableInt pastePosOffsetCount;

  /** コンストラクタ. */
  public CopyAndPaste(BhNodeFactory factory, MutableInt pastePosOffsetCount) {
    this.factory = factory;
    this.pastePosOffsetCount = pastePosOffsetCount;
  }

  /**
   * コピー予定の {@link BhNode} をリストに追加する.
   *
   * @param toAdd コピー予定の {@link BhNode} を格納したコレクション
   * @param userOpe undo 用コマンドオブジェクト
   */
  public void addNodeToList(BhNode toAdd, UserOperation userOpe) {
    if (readyToCopy.contains(toAdd) || toAdd.isDeleted()) {
      return;
    }
    readyToCopy.add(toAdd);
    eventManager.invokeOnCopyNodeAdded(toAdd, userOpe);
    userOpe.pushCmdOfAddNodeToCopyList(this, toAdd);
    toAdd.getEventManager().addOnWorkspaceChanged(eventManager.onWorkspaceChanged);
  }

  /**
   * コピー予定の {@link BhNode} のリストをクリアする.
   *
   * @param userOpe undo 用コマンドオブジェクト
   */
  public void clearList(UserOperation userOpe) {
    while (readyToCopy.size() != 0) {
      removeNodeFromList(readyToCopy.getFirst(), userOpe);
    }
  }

  /**
   * コピー予定リストのノードをコピーして引数で指定したワークスペースに貼り付ける.
   *
   * @param wsToPasteIn 貼り付け先のワークスペース
   * @param pasteBasePos 貼り付け基準位置
   * @param userOpe undo 用コマンドオブジェクト
   */
  public void paste(Workspace wsToPasteIn, Vec2D pasteBasePos, UserOperation userOpe) {
    if (readyToCopy.isEmpty()) {
      return;
    }
    Collection<BhNode> candidates = readyToCopy.stream()
        .filter(this::canCopy).collect(Collectors.toCollection(HashSet::new));

    Collection<OriginalAndCopy> listOfOrgAndCopy = candidates.stream()
        .map(node -> new OriginalAndCopy(node, genCopyNode(node, candidates, userOpe)))
        .filter(orgAndCopy -> orgAndCopy.copy() != null)
        .collect(Collectors.toCollection(ArrayList::new));

    // 貼り付け処理
    for (var orgAndCopy : listOfOrgAndCopy) {
      factory.setMvc(orgAndCopy.copy(), MvcType.DEFAULT);
      BhNodePlacer.moveToWs(
          wsToPasteIn,
          orgAndCopy.copy(),
          pasteBasePos.x,
          pasteBasePos.y + pastePosOffsetCount.getValue() * BhConstants.LnF.REPLACED_NODE_SHIFT * 2,
          userOpe);
      //コピー直後のノードは大きさが未確定なので, コピー元ノードの大きさを元に貼り付け位置を算出する.
      Vec2D size = orgAndCopy.org().getViewProxy().getSizeIncludingOuters(true);
      pasteBasePos.x += size.x + BhConstants.LnF.REPLACED_NODE_SHIFT * 2;
    }
    if (pastePosOffsetCount.getValue() > 2) {
      pastePosOffsetCount.setValue(-2);
    } else {
      pastePosOffsetCount.increment();
    }
  }

  /**
   * {@code target} をコピーする.
   *
   * <p> 返されるノードの MVC は構築されない. </p>
   *
   * @param target コピー対象のノード
   * @param nodesToCopy {@code target} ノードとともにコピーされるノード
   * @param userOpe undo 用コマンドオブジェクト
   * @return 作成したコピーノード.  コピーノードを作らなかった場合 null.
   */
  private BhNode genCopyNode(
      BhNode target, Collection<? extends BhNode> nodesToCopy, UserOperation userOpe) {
    Predicate<? super BhNode> fnIsNodeToBeCopied =
        target.getEventInvoker().onCopyRequested(nodesToCopy, userOpe);
    return target.copy(fnIsNodeToBeCopied, userOpe);
  }

  /**
   * コピー予定のノードリストからノードを取り除く.
   *
   * @param toRemove 取り除くノード
   * @param userOpe undo 用コマンドオブジェクト
   * */
  public void removeNodeFromList(BhNode toRemove, UserOperation userOpe) {
    if (readyToCopy.contains(toRemove)) {
      readyToCopy.remove(toRemove);
      eventManager.invokeOnCopyNodeRemoved(toRemove, userOpe);
      userOpe.pushCmdOfRemoveNodeFromCopyList(this, toRemove);
      toRemove.getEventManager().removeOnWorkspaceChanged(eventManager.onWorkspaceChanged);
    }
  }

  /** コピー予定のノードのセットを返す. */
  public SequencedSet<BhNode> getList() {
    return new LinkedHashSet<>(readyToCopy);
  }

  /**
   * コピーの対象になるかどうか判定する.
   *
   * @param node 判定対象のノード
   * @return コピーの対象になる場合 true
   */
  private boolean canCopy(BhNode node) {
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

  /** コピー元とコピーされた {@link BhNode} のペア. */
  private record OriginalAndCopy(BhNode org, BhNode copy) {}

  /** イベントハンドラの管理を行うクラス. */
  public class EventManager {

    /** コピー予定ノードが追加されたときのイベントハンドラのセット. */
    private final
        SequencedSet<TriConsumer<? super CopyAndPaste, ? super BhNode, ? super UserOperation>>
        onCopyNodeAddedList = new LinkedHashSet<>();
    /** コピー予定ノードが削除されたときのイベントハンドラのセット. */
    private final
        SequencedSet<TriConsumer<? super CopyAndPaste, ? super BhNode, ? super UserOperation>>
        onCopyNodeRemovedList = new LinkedHashSet<>();
    /** 管理している {@link BhNode} のワークスペースが変わった時のイベントハンドラ. */
    private final
        TetraConsumer<? super BhNode, ? super Workspace, ? super Workspace, ? super UserOperation>
        onWorkspaceChanged = (node, oldWs, newWs, userOpe) -> {
          if (oldWs != newWs) {
            CopyAndPaste.this.removeNodeFromList(node, userOpe);
          }
        };

    /**
     * ノードがコピー候補になったときのイベントハンドラを追加する.
     *
     * @param handler 追加するイベントハンドラ
     */
    public void addOnCopyNodeAdded(
        TriConsumer<? super CopyAndPaste, ? super BhNode, ? super UserOperation> handler) {
      onCopyNodeAddedList.addLast(handler);
    }

    /**
     * ノードがコピー候補になったときのイベントハンドラを削除する.
     *
     * @param handler 削除するイベントハンドラ
     */
    public void removeOnCopyNodeAdded(
        TriConsumer<? super CopyAndPaste, ? super BhNode, ? super UserOperation> handler) {
      onCopyNodeAddedList.remove(handler);
    }

    /** ノードがコピー候補になったときのイベントハンドラを呼び出す. */
    private void invokeOnCopyNodeAdded(BhNode node, UserOperation userOpe) {
      onCopyNodeAddedList.forEach(handler -> handler.accept(CopyAndPaste.this, node, userOpe));
    }

    /**
     * ノードがコピー候補でなくなったときのイベントハンドラを追加する.
     *
     * @param handler 追加するイベントハンドラ
     */
    public void addOnCopyNodeRemoved(
        TriConsumer<? super CopyAndPaste, ? super BhNode, ? super UserOperation> handler) {
      onCopyNodeRemovedList.addLast(handler);
    }

    /**
     * ノードがコピー候補でなくなったときのイベントハンドラを削除する.
     *
     * @param handler 削除するイベントハンドラ
     */
    public void removeOnCopyNodeRemoved(
        TriConsumer<? super CopyAndPaste, ? super BhNode, ? super UserOperation> handler) {
      onCopyNodeRemovedList.remove(handler);
    }

    /** ノードがコピー候補でなくなったときのイベントハンドラを呼び出す. */
    private void invokeOnCopyNodeRemoved(BhNode node, UserOperation userOpe) {
      onCopyNodeRemovedList.forEach(handler -> handler.accept(CopyAndPaste.this, node, userOpe));
    }
  }  
}