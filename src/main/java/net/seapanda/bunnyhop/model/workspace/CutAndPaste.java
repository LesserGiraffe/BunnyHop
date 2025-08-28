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
import java.util.function.Consumer;
import java.util.stream.Collectors;
import net.seapanda.bunnyhop.common.BhConstants;
import net.seapanda.bunnyhop.model.BhNodePlacer;
import net.seapanda.bunnyhop.model.node.BhNode;
import net.seapanda.bunnyhop.model.node.BhNode.Swapped;
import net.seapanda.bunnyhop.undo.UserOperation;
import net.seapanda.bunnyhop.utility.function.ConsumerInvoker;
import net.seapanda.bunnyhop.utility.math.Vec2D;
import org.apache.commons.lang3.mutable.MutableInt;

/**
 * カット & ペーストの機能を提供するクラス.
 *
 * @author K.Koike
 */
public class CutAndPaste {
  
  /** カット予定のノード. */
  private final SequencedSet<BhNode> readyToCut = new LinkedHashSet<>();
  private final CallbackRegistry cbRegistry = this.new CallbackRegistry();
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
    cbRegistry.onNodeAddedInvoker.invoke(new NodeAddedEvent(this, toAdd));
    userOpe.pushCmdOfAddNodeToCutList(this, toAdd);
    toAdd.getCallbackRegistry().getOnWorkspaceChanged().add(cbRegistry.onWsChanged);
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
      cbRegistry.onNodeRemovedInvoker.invoke(new NodeRemovedEvent(this, toRemove));
      userOpe.pushCmdOfRemoveNodeFromCutList(this, toRemove);
      toRemove.getCallbackRegistry().getOnWorkspaceChanged().remove(cbRegistry.onWsChanged);
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
      Vec2D size = node.getView()
          .map(view -> view.getRegionManager().getNodeTreeSize(true))
          .orElse(new Vec2D());
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
   * {@link CutAndPaste} オブジェクトに対するイベントハンドラの追加と削除を行うオブジェクトを返す.
   *
   * @return このオブジェクトに対するイベントハンドラの追加と削除を行うオブジェクト
   */
  public CallbackRegistry getCallbackRegistry() {
    return cbRegistry;
  }

  /** {@link CutAndPaste} に対してイベントハンドラを追加または削除する機能を提供するクラス. */
  public class CallbackRegistry {
    
    /** カット予定のノードが追加されたときのイベントハンドラを管理するオブジェクト. */
    private final ConsumerInvoker<NodeAddedEvent> onNodeAddedInvoker = 
        new ConsumerInvoker<>();

    /** カット予定のノードが削除されたときのイベントハンドラを管理するオブジェクト. */
    private final ConsumerInvoker<NodeRemovedEvent> onNodeRemovedInvoker = 
        new ConsumerInvoker<>();

    /** 管理している {@link BhNode} のワークスペースが変わったときのイベントハンドラ. */
    private final Consumer<? super BhNode.WorkspaceChangeEvent> onWsChanged = event -> {
      if (event.oldWs() != event.newWs()) {
        CutAndPaste.this.removeNodeFromList(event.node(), event.userOpe());
      }
    };

    /** カット予定のノードが追加されたときのイベントハンドラを管理するオブジェクト. */
    public ConsumerInvoker<NodeAddedEvent>.Registry getOnNodeAdded() {
      return onNodeAddedInvoker.getRegistry();
    }

    /** カット予定のノードが削除されたときのイベントハンドラを管理するオブジェクト. */
    public ConsumerInvoker<NodeRemovedEvent>.Registry getOnNodeRemoved() {
      return onNodeRemovedInvoker.getRegistry();
    }
  }

  /**
   * {@link CutAndPaste} にノードが追加されたときの情報を格納したレコード.
   *
   * @param cutAndPaste ノードが追加された {@link CutAndPaste} オブジェクト
   * @param added 追加されたノード
   */
  public record NodeAddedEvent(CutAndPaste cutAndPaste, BhNode added) {}

  /**
   * {@link CutAndPaste} からノードが削除されたときの情報を格納したレコード.
   *
   * @param cutAndPaste ノードが削除された {@link CutAndPaste} オブジェクト
   * @param removed 削除されたノード
   */
  public record NodeRemovedEvent(CutAndPaste cutAndPaste, BhNode removed) {}
}
