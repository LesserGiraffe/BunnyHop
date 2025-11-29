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

package net.seapanda.bunnyhop.workspace.model;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.SequencedSet;
import java.util.function.BiFunction;
import java.util.function.Consumer;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import net.seapanda.bunnyhop.common.configuration.BhConstants;
import net.seapanda.bunnyhop.common.configuration.BhSettings;
import net.seapanda.bunnyhop.node.model.BhNode;
import net.seapanda.bunnyhop.node.model.factory.BhNodeFactory;
import net.seapanda.bunnyhop.node.model.factory.BhNodeFactory.MvcType;
import net.seapanda.bunnyhop.node.service.BhNodePlacer;
import net.seapanda.bunnyhop.node.view.common.BhNodeLocation;
import net.seapanda.bunnyhop.service.undo.UserOperation;
import net.seapanda.bunnyhop.ui.view.ViewUtil;
import net.seapanda.bunnyhop.utility.event.ConsumerInvoker;
import net.seapanda.bunnyhop.utility.event.SimpleConsumerInvoker;
import net.seapanda.bunnyhop.utility.math.Vec2D;
import net.seapanda.bunnyhop.workspace.view.WorkspaceView;
import org.apache.commons.lang3.mutable.MutableInt;

/**
 * コピー & ペーストの機能を提供するクラス.
 *
 * @author K.Koike
 */
public class CopyAndPaste {
 
  /** コピー予定のノード. */
  private final SequencedSet<BhNode> readyToCopy = new LinkedHashSet<>();
  private final CallbackRegistry cbRegistry = this.new CallbackRegistry();
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
    userOpe.pushCmd(ope -> removeNodeFromList(toAdd, ope));
    cbRegistry.onNodeAddedInvoker.invoke(new NodeAddedEvent(this, toAdd));
    toAdd.getCallbackRegistry().getOnWorkspaceChanged().add(cbRegistry.onWsChanged);
  }

  /**
   * コピー予定の {@link BhNode} のリストをクリアする.
   *
   * @param userOpe undo 用コマンドオブジェクト
   */
  public void clearList(UserOperation userOpe) {
    while (!readyToCopy.isEmpty()) {
      removeNodeFromList(readyToCopy.getFirst(), userOpe);
    }
  }

  /**
   * コピー予定リストのノードをコピーして引数で指定したワークスペースに貼り付ける.
   *
   * @param destination 貼り付け先のワークスペース
   * @param basePos 貼り付け基準位置
   * @param userOpe undo 用コマンドオブジェクト
   */
  public void paste(Workspace destination, Vec2D basePos, UserOperation userOpe) {
    if (readyToCopy.isEmpty()) {
      return;
    }
    Collection<BhNode> candidates = readyToCopy.stream()
        .filter(this::canCopy).collect(Collectors.toCollection(HashSet::new));

    List<OriginalAndCopy> listOfOrgAndCopy = candidates.stream()
        .map(node -> new OriginalAndCopy(node, genCopyNode(node, candidates, userOpe)))
        .filter(orgAndCopy -> orgAndCopy.copy() != null)
        .collect(Collectors.toCollection(ArrayList::new));

    // 貼り付け処理
    for (var orgAndCopy : listOfOrgAndCopy) {
      factory.setMvc(orgAndCopy.copy(), MvcType.DEFAULT);
      BhNodePlacer.moveToWs(
          destination,
          orgAndCopy.copy(),
          basePos.x,
          basePos.y + pastePosOffsetCount.getValue() * BhConstants.Ui.REPLACED_NODE_SHIFT * 2,
          userOpe);
      //コピー直後のノードは大きさが未確定なので, コピー元ノードの大きさを元に貼り付け位置を算出する.
      Vec2D size = orgAndCopy.org().getView()
          .map(view -> view.getRegionManager().getNodeTreeSize(true))
          .orElse(new Vec2D());
      basePos.x += size.x + BhConstants.Ui.REPLACED_NODE_SHIFT * 2;
    }
    if (pastePosOffsetCount.getValue() > 2) {
      pastePosOffsetCount.setValue(-2);
    } else {
      pastePosOffsetCount.increment();
    }
    if (!listOfOrgAndCopy.isEmpty()) {
      pushViewpointChangeCmd(userOpe, new BhNodeLocation(listOfOrgAndCopy.getLast().copy()));
    }
  }

  /**
   * {@code target} をコピーする.
   *
   * <p>返されるノードの MVC は構築されない.
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
      userOpe.pushCmd(ope -> addNodeToList(toRemove, ope));
      cbRegistry.onNodeRemovedInvoker.invoke(new NodeRemovedEvent(this, toRemove));
      toRemove.getCallbackRegistry().getOnWorkspaceChanged().remove(cbRegistry.onWsChanged);
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

  /** ワークスペース上の注視点を変更するコマンドを追加する. */
  private static void pushViewpointChangeCmd(UserOperation userOpe, BhNodeLocation location) {
    BiFunction<WorkspaceView, Vec2D, Boolean> fnShouldChange
        = (wsView, pos) -> wsView.getWorkspace().isCurrentWorkspace()
        ? BhSettings.Ui.trackNodeInCurrentWorkspace : BhSettings.Ui.trackNodeInInactiveWorkspace;
    ViewUtil.pushViewpointChangeCmd(
        null,
        null,
        location.wsView,
        location.center,
        fnShouldChange,
        userOpe);
  }

  /**
   * このオブジェクトに対するイベントハンドラの追加と削除を行うオブジェクトを返す.
   *
   * @return このオブジェクトに対するイベントハンドラの追加と削除を行うオブジェクト
   */
  public CallbackRegistry getCallbackRegistry() {
    return cbRegistry;
  }

  /** コピー元とコピーされた {@link BhNode} のペア. */
  private record OriginalAndCopy(BhNode org, BhNode copy) {}

  /** {@link CopyAndPaste} に対してイベントハンドラを追加または削除する機能を提供するクラス. */
  public class CallbackRegistry {

    /** コピー予定のノードが追加されたときのイベントハンドラを管理するオブジェクト. */
    private final ConsumerInvoker<NodeAddedEvent> onNodeAddedInvoker = 
        new SimpleConsumerInvoker<>();

    /** コピー予定のノードが削除されたときのイベントハンドラを管理するオブジェクト. */
    private final ConsumerInvoker<NodeRemovedEvent> onNodeRemovedInvoker = 
        new SimpleConsumerInvoker<>();

    /** 管理している {@link BhNode} のワークスペースが変わったときのイベントハンドラ. */
    private final Consumer<? super BhNode.WorkspaceChangeEvent> onWsChanged = event -> {
      if (event.oldWs() != event.newWs()) {
        CopyAndPaste.this.removeNodeFromList(event.node(), event.userOpe());
      }
    };

    /** コピー予定のノードが追加されたときのイベントハンドラを管理するオブジェクト. */
    public ConsumerInvoker<NodeAddedEvent>.Registry getOnNodeAdded() {
      return onNodeAddedInvoker.getRegistry();
    }

    /** コピー予定のノードが削除されたときのイベントハンドラを管理するオブジェクト. */
    public ConsumerInvoker<NodeRemovedEvent>.Registry getOnNodeRemoved() {
      return onNodeRemovedInvoker.getRegistry();
    }
  }

  /**
   * {@link CopyAndPaste} にノードが追加されたときの情報を格納したレコード.
   *
   * @param copyAndPaste ノードが追加された {@link CopyAndPaste} オブジェクト
   * @param added 追加されたノード
   */
  public record NodeAddedEvent(CopyAndPaste copyAndPaste, BhNode added) {}

  /**
   * {@link CopyAndPaste} からノードが削除されたときの情報を格納したレコード.
   *
   * @param copyAndPaste ノードが削除された {@link CopyAndPaste} オブジェクト
   * @param removed 削除されたノード
   */
  public record NodeRemovedEvent(CopyAndPaste copyAndPaste, BhNode removed) {}  
}
