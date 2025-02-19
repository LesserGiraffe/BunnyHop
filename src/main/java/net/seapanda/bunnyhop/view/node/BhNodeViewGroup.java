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

package net.seapanda.bunnyhop.view.node;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map.Entry;
import java.util.SequencedMap;
import java.util.regex.Pattern;
import javafx.scene.Group;
import javafx.scene.layout.Pane;
import net.seapanda.bunnyhop.model.node.Connector;
import net.seapanda.bunnyhop.utility.Showable;
import net.seapanda.bunnyhop.utility.SimpleCache;
import net.seapanda.bunnyhop.utility.Vec2D;
import net.seapanda.bunnyhop.view.ViewConstructionException;
import net.seapanda.bunnyhop.view.factory.BhNodeViewFactory;
import net.seapanda.bunnyhop.view.node.style.BhNodeViewStyle;
import net.seapanda.bunnyhop.view.node.style.BhNodeViewStyle.Arrangement;
import net.seapanda.bunnyhop.view.traverse.NodeViewComponent;
import net.seapanda.bunnyhop.view.traverse.NodeViewWalker;

/**
 * {@link BhNodeView} の集合を持つクラス.
 *
 * @author K.Koike
 */
public class BhNodeViewGroup implements NodeViewComponent, Showable {

  private static Pattern escapeLbrace = Pattern.compile(Pattern.quote("\\{"));
  private static Pattern escapeRbrace = Pattern.compile(Pattern.quote("\\}"));
  /** `\\...\$` */
  private static Pattern escapeDollar = Pattern.compile("^(\\\\)+\\$");
  /** 疑似ビュー指定パターン `${a}{b}...{z}` */
  private static Pattern embeded =
      Pattern.compile("^\\$(\\{(((\\\\\\{)|(\\\\\\})|[^\\{\\}])*)\\}){2,}$");
  /** 疑似ビュー指定パターン `${a}{b}...{z}` の (a, b, ..., z) を取り出す用. */
  private static Pattern contents =
      Pattern.compile("\\{((?:(?:\\\\\\{)|(?:\\\\\\})|[^\\{\\}])*)\\}");

  /** このグループが子となる {@link BhNodeViewGroup} のリスト. */
  private final List<BhNodeViewGroup> subGroupList = new ArrayList<>();
  /** このグループを持つ {@link ConnectiveNodeView}. */
  private final ConnectiveNodeView parentView;
  /** このグループを持つ {@link BhNodeViewGroup}. */
  private final BhNodeViewGroup parentGroup;
  /** このグループが内部描画ノードを持つグループの場合 true. */
  public final boolean inner; 
  /** ノードの配置を決めるパラメータ. */
  private BhNodeViewStyle.Arrangement arrangeParams;
  /** コネクタ名とそのコネクタにつながる {@link BhNodeView}. */
  private final SequencedMap<String, BhNodeViewBase> childNameToNodeView = new LinkedHashMap<>();
  private final Vec2D relativePos = new Vec2D(0.0, 0.0);
  /** 疑似ビューの ID. */
  private int pseudoViewId = 0;
  /** このグループのサイズのキャッシュデータ. */
  private SimpleCache<Vec2D> sizeCache = new SimpleCache<Vec2D>(new Vec2D());

  /**
   * コンストラクタ.
   *
   * @param parentView このグループを持つConnectiveNode
   * @param inner このグループが外部描画ノードを持つグループの場合true
   */
  public BhNodeViewGroup(ConnectiveNodeView parentView, boolean inner) {
    this.parentView = parentView;
    this.parentGroup = null;
    this.inner = inner;
  }

  /**
   * コンストラクタ.
   *
   * @param parentGroup このグループを持つ {@link BhNodeViewGroup}
   * @param inner このグループが内部描画ノードを持つグループの場合 true
   */
  public BhNodeViewGroup(BhNodeViewGroup parentGroup, boolean inner) {
    this.parentView = null;
    this.parentGroup = parentGroup;
    this.inner = inner;
  }

  /**
   * このノード以下のサブグループを作成する.
   *
   * @param arrangeParams ノード配置パラメータ
   * @param factory サブグループ内の疑似ビューを作成するのに使用するオブジェクト
   * @throws ViewConstructionException グループの初期化に失敗した
   */
  void buildSubGroup(BhNodeViewStyle.Arrangement arrangeParams, BhNodeViewFactory factory)
      throws ViewConstructionException {
    this.arrangeParams = arrangeParams;
    for (String cnctrName : arrangeParams.cnctrNameList) {
      String childName = cnctrName;
      BhNodeViewBase childView = null;
      // 疑似ビュー指定パターン
      if (embeded.matcher(cnctrName).find()) {
        childName = "$" + pseudoViewId++;
        childView = createPseudoView(cnctrName, factory);
      } else if (escapeDollar.matcher(cnctrName).find()) {
        // 先頭の `\` を取り除く.
        childName = cnctrName.substring(1);
      }
      childNameToNodeView.put(childName, childView);
      // 外部ノードをつなぐコネクタは1つだけとする
      if (!inner) {
        return;
      }
    }
    for (Arrangement subGroupParams : arrangeParams.subGroups) {
      var subGroup = new BhNodeViewGroup(this, inner);
      subGroup.buildSubGroup(subGroupParams, factory);
      subGroupList.add(subGroup);
    }
  }

  /**
   * BhNodeView を追加する. view の適切な追加先が見つからなかった場合, 失敗する.
   *
   * @param view 追加するノードビュー
   * @return 追加に成功した場合 true. 失敗した場合 false.
   */
  boolean addNodeView(BhNodeViewBase view) {
    Connector cnctr = view.getModel().map(model -> model.getParentConnector()).orElse(null);
    if (cnctr != null) {
      String cnctrName = cnctr.getSymbolName();
      // このグループ内に追加すべき場所が見つかった
      if (childNameToNodeView.containsKey(cnctrName)) {
        childNameToNodeView.put(cnctrName, view);
        view.getTreeManager().setParentGroup(this);
        cnctr.setOuterFlag(!inner);
        return true;
      } else {
        //サブグループに追加する
        for (BhNodeViewGroup subGroup : subGroupList) {
          if (subGroup.addNodeView(view)) {
            return true;
          }
        }
      }
    }
    return false;
  }

  /**
   * このグループが持つ {@code oldNodeView} を {@code newNodeView} と入れ替える.
   *
   * @param oldNodeView 入れ替えられる古いノード
   * @param newNodeView 入れ替える新しいノード
   */
  void replace(BhNodeViewBase oldNodeView, BhNodeViewBase newNodeView) {
    for (Entry<String, BhNodeViewBase> entrySet : childNameToNodeView.entrySet()) {
      if (entrySet.getValue().equals(oldNodeView)) {
        entrySet.setValue(newNodeView);
        newNodeView.getTreeManager().setParentGroup(this);    //親をセット
        oldNodeView.getTreeManager().setParentGroup(null);    //親を削除
        return;
      }
    }
  }

  /**
   * このグループの親ノードを返す.
   *
   * <p> このグループが別のグループのサブグループでも, その上にある親ノードビューを返す. </p>
   *
   * @return このグループの親ノードビュー
   */
  public ConnectiveNodeView getParentView() {
    if (parentView != null) {
      return parentView;
    }
    return parentGroup.getParentView();
  }

  /**
   * このグループの親グループを返す.
   *
   * @return このグループの親グループ. 存在しない場合は null.
   */
  public BhNodeViewGroup getParentGroup() {
    return parentGroup;
  }

  /**
   * 親ノードまたはグループからの相対位置を取得する.
   *
   * @return 親ノードまたは親グループからの相対位置
   */
  Vec2D getRelativePosFromParent() {
    return new Vec2D(relativePos.x, relativePos.y);
  }

  /**
   * 親ノードまたはグループからの相対位置をセットする.
   *
   * @param posX 親ノードまたは親グループからのX相対位置
   * @param posY 親ノードまたは親グループからのY相対位置
   */
  void setRelativePosFromParent(double posX, double posY) {
    relativePos.x = posX;
    relativePos.y = posY;
  }

  /**
   * このグループとこのグループ以下のノードのワークスペース上の位置を更新する関数.
   *
   * @param posX グループ左上の X 位置
   * @param posY グループ左上の Y 位置
   */
  void updateTreePosOnWorkspace(double posX, double posY) {
    for (BhNodeViewBase child : childNameToNodeView.values()) {
      if (child == null) {
        continue;
      }
      Vec2D relativePos = child.getPositionManager().getRelativePosFromParent();
      child.updatePosOnWorkspace(posX + relativePos.x, posY + relativePos.y);
    }
    subGroupList.forEach(subGroup -> {
      Vec2D subGroupRelPos = subGroup.getRelativePosFromParent();
      subGroup.updateTreePosOnWorkspace(posX + subGroupRelPos.x, posY + subGroupRelPos.y);
    });
  }

  /** 子要素のこのグループからの相対位置を更新する. */
  void updateChildRelativePos() {
    Vec2D offset = calcOffsetOfCnctr();
    Vec2D childRelPos =
        new Vec2D(offset.x + arrangeParams.paddingLeft, offset.y + arrangeParams.paddingTop);
    childRelPos = updateChildNodeRelPos(childRelPos);
    updateChildGroupRelPos(childRelPos);
  }

  /**
   * 子ノードの相対位置を更新する.
   *
   * @param relPos 最初の子ノードの親からの相対位置.
   * @return 次のグループの相対位置.
   */
  private Vec2D updateChildNodeRelPos(Vec2D relPos) {
    for (BhNodeViewBase child : childNameToNodeView.values()) {
      if (child == null) {
        continue;
      }
      //outer はコネクタの大きさを考慮しない
      Vec2D cnctrSize = inner ? child.viewStyle.getConnectorSize(child.isFixed()) 
          : new Vec2D(0.0,  0.0);
      Vec2D childNodeSize = child.getRegionManager().getNodeTreeSize(false);
      // コネクタが上に付く && グループの中が縦並び
      if (child.viewStyle.connectorPos == BhNodeViewStyle.ConnectorPos.TOP
          && arrangeParams.arrangement == BhNodeViewStyle.ChildArrangement.COLUMN) {
        relPos.add(0, cnctrSize.y);

      // コネクタが左に付く && グループの中が横並び
      } else if (child.viewStyle.connectorPos == BhNodeViewStyle.ConnectorPos.LEFT
          && arrangeParams.arrangement == BhNodeViewStyle.ChildArrangement.ROW) {
        relPos.add(cnctrSize.x, 0);
      }
      child.getPositionManager().setRelativePosFromParent(relPos.x, relPos.y);
      calcNextRelativePosOffset(relPos, childNodeSize);
      child.updateChildRelativePos();
    }
    return relPos;
  }

  /**
   * 子グループの相対位置を更新する.
   *
   * @param relPos 最初の子グループの親からの相対位置.
   */
  private void updateChildGroupRelPos(Vec2D relPos) {
    for (BhNodeViewGroup subGroup : subGroupList) {
      subGroup.setRelativePosFromParent(relPos.x, relPos.y);
      Vec2D subGroupSize = subGroup.getSize();
      calcNextRelativePosOffset(relPos, subGroupSize);
      subGroup.updateChildRelativePos();
    }
  }

  /**
   * 子要素の相対位置とその大きさから次の子要素の相対位置を計算する.
   *
   * @param relPos 子要素の相対位置.  次の子要素の相対位置で更新される.
   * @param childSize 子要素の大きさ
   */
  private void calcNextRelativePosOffset(Vec2D relPos, Vec2D childSize) {
    //グループの中が縦並び
    if (arrangeParams.arrangement == BhNodeViewStyle.ChildArrangement.COLUMN) {
      relPos.y += childSize.y + arrangeParams.space;
    //グループの中が横並び
    } else {
      relPos.x += childSize.x + arrangeParams.space;
    }
  }

  /**
   * このグループのサイズを取得する.
   */
  public Vec2D getSize() {
    if (!sizeCache.isDirty()) {
      return new Vec2D(sizeCache.getVal());
    }
    Vec2D cnctrOffset = calcOffsetOfCnctr();
    Vec2D childSumLen = calcChildSumLen();
    Vec2D childMaxLen = calcChildMaxLen();
    var size = new Vec2D(arrangeParams.paddingLeft, arrangeParams.paddingTop);
    int numSpace = Math.max(0, childNameToNodeView.size() + subGroupList.size() - 1);
    
    //グループの中が縦並び
    if (arrangeParams.arrangement == BhNodeViewStyle.ChildArrangement.COLUMN) {
      size.x += cnctrOffset.x + childMaxLen.x + arrangeParams.paddingRight;
      size.y += childSumLen.y + numSpace * arrangeParams.space + arrangeParams.paddingBottom;
    // グループの中が横並び
    } else {    
      size.x += childSumLen.x + numSpace * arrangeParams.space + arrangeParams.paddingRight;
      size.y += cnctrOffset.y + childMaxLen.y + arrangeParams.paddingBottom;
    }
    sizeCache.update(new Vec2D(size));
    return size;
  }

  /** 子ノードとサブグループを並べる際のコネクタサイズによって決まるオフセットを計算する. */
  private Vec2D calcOffsetOfCnctr() {
    double offsetX = 0.0;
    double offsetY = 0.0;
    // 外部ノードはコネクタの大きさを考慮しない
    if (!inner) {
      return new Vec2D(0.0, 0.0);
    }
    for (BhNodeViewBase child : childNameToNodeView.values()) {
      if (child == null) {
        continue;
      }
      Vec2D cnctrSize = child.viewStyle.getConnectorSize(child.isFixed());
      // グループの中が縦並びでかつコネクタが左に付く
      if (arrangeParams.arrangement == BhNodeViewStyle.ChildArrangement.COLUMN
          && child.viewStyle.connectorPos == BhNodeViewStyle.ConnectorPos.LEFT) {
        offsetX = Math.max(offsetX, cnctrSize.x);
      // グループの中が横並びでかつコネクタが上に付く
      } else if (arrangeParams.arrangement == BhNodeViewStyle.ChildArrangement.ROW
          && child.viewStyle.connectorPos == BhNodeViewStyle.ConnectorPos.TOP) {
        offsetY = Math.max(offsetY, cnctrSize.y);
      }
    }
    return new Vec2D(offsetX, offsetY);
  }

  /** 子要素の縦横の長さの総計を求める. */
  private Vec2D calcChildSumLen() {
    var childSumLen = new Vec2D(0, 0);
    for (BhNodeViewGroup subGroup : subGroupList) {
      childSumLen.add(subGroup.getSize());
    }

    for (BhNodeViewBase child : childNameToNodeView.values()) {
      if (child == null) {
        continue;
      }
      // outer はコネクタの大きさを考慮しない
      Vec2D cnctrSize = inner ? child.viewStyle.getConnectorSize(child.isFixed()) :
          new Vec2D(0.0,  0.0);
      Vec2D childNodeSize = child.getRegionManager().getNodeTreeSize(false);
      //コネクタが上に付く
      if (child.viewStyle.connectorPos == BhNodeViewStyle.ConnectorPos.TOP) {
        childSumLen.add(childNodeSize.x, childNodeSize.y + cnctrSize.y);
      //コネクタが左に付く
      } else if (child.viewStyle.connectorPos == BhNodeViewStyle.ConnectorPos.LEFT) {
        childSumLen.add(childNodeSize.x + cnctrSize.x, childNodeSize.y);
      }
    }
    return childSumLen;
  }

  /** 子要素の縦横の長さの最大値を求める. */
  private Vec2D calcChildMaxLen() {
    var childMaxLen = new Vec2D(0, 0);
    for (BhNodeViewGroup subGroup : subGroupList) {
      childMaxLen.updateIfGreter(subGroup.getSize());
    }

    for (BhNodeView child : childNameToNodeView.values()) {
      if (child == null) {
        continue;
      }
      // outer はコネクタの大きさを考慮しない
      Vec2D childNodeSize = child.getRegionManager().getNodeTreeSize(false);
      childMaxLen.updateIfGreater(childNodeSize.x, childNodeSize.y);
    }
    return childMaxLen;
  }

  /**
   * visitor をサブグループに渡す.
   *
   * @param visitor サブグループに渡す visitor
   */
  public void sendToSubGroupList(NodeViewWalker visitor) {
    subGroupList.forEach(group -> group.accept(visitor));
  }

  /**
   * visitor を子ノードビューに渡す.
   *
   * @param visitor 子ノードビューに渡す visitor
   */
  public void sendToChildNode(NodeViewWalker visitor) {
    for (BhNodeView child : childNameToNodeView.values()) {
      if (child != null) {
        child.accept(visitor);
      }
    }
  }

  /** このグループの中で定義された MVC 構造を持たない {@link BhNodeView} を作成する. */
  private BhNodeViewBase createPseudoView(String specification, BhNodeViewFactory factory)
      throws ViewConstructionException {
    BhNodeView nodeView = factory.createViewOf(specification);
    if (nodeView instanceof BhNodeViewBase view) {
      view.getTreeManager().setParentGroup(this);
      return view;
    } 
    throw new ViewConstructionException("Invalid pseudo view type.  (%s)".formatted(nodeView));
  }

  /** このグループが保持する疑似ビューを {@code parent} に子要素として追加する. */
  void addPseudoViewToGuiTree(Pane parent) {
    for (BhNodeView child : childNameToNodeView.values()) {
      // 疑似ビューはモデルを持たない
      if (child != null && child.getModel().isEmpty()) {
        child.getTreeManager().addToGuiTree(parent);
      }
    }
  }

  /** このグループが保持する疑似ビューを {@code parent} に子要素として追加する. */
  void addPseudoViewToGuiTree(Group parent) {
    for (BhNodeView child : childNameToNodeView.values()) {
      // 疑似ビューはモデルを持たない
      if (child != null && child.getModel().isEmpty()) {
        child.getTreeManager().addToGuiTree(parent);
      }
    }
  }

  /** このグループが保持する疑似ビューを親 GUI コンポーネントから取り除く. */
  void removePseudoViewFromGuiTree() {
    for (BhNodeView child : childNameToNodeView.values()) {
      // 疑似ビューはモデルを持たない
      if (child != null && child.getModel().isEmpty()) {
        child.getTreeManager().removeFromGuiTree();
      }
    }
  }

  /** このグループに子要素のサイズが変わったことを伝える. */
  void notifyChildSizeChanged() {
    sizeCache.setDirty(true);
    if (parentGroup != null) {
      parentGroup.notifyChildSizeChanged();
    }
    if (parentView != null) {
      parentView.notifyChildSizeChanged();
    }
  }

  @Override
  public void accept(NodeViewWalker visitor) {
    visitor.visit(this);
  }

  @Override
  public void show(int depth) {
    try {
      System.out.println(
          "%s<BhNodeViewGroup>  %s".formatted(indent(depth), hashCode()));
      System.out.println(indent(depth + 1) + (inner ? "<inner>" : "<outer>"));
      childNameToNodeView.values().stream()
          .filter(childNodeView -> childNodeView != null)
          .forEachOrdered(childNodeView -> childNodeView.show(depth + 1));
      subGroupList.forEach(subGroup -> subGroup.show(depth + 1));
    } catch (Exception e) {
      System.out.println("connectiveNodeView show exception " + e);
    }
  }
}
