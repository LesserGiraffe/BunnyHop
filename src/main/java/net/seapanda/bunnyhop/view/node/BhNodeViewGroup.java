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
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import net.seapanda.bunnyhop.common.Showable;
import net.seapanda.bunnyhop.common.Vec2D;
import net.seapanda.bunnyhop.common.tools.MsgPrinter;
import net.seapanda.bunnyhop.model.node.Connector;
import net.seapanda.bunnyhop.view.ViewHelper;
import net.seapanda.bunnyhop.view.ViewInitializationException;
import net.seapanda.bunnyhop.view.node.part.BhNodeViewStyle;
import net.seapanda.bunnyhop.view.node.part.BhNodeViewStyle.Arrangement;
import net.seapanda.bunnyhop.viewprocessor.NodeViewComponent;
import net.seapanda.bunnyhop.viewprocessor.NodeViewProcessor;

/**
 * {@link BhNodeView} の集合を持つクラス.
 *
 * @author K.Koike
 */
public class BhNodeViewGroup implements NodeViewComponent, Showable {

  /** このグループが子となる {@link BhNodeViewGroup} のリスト. */
  private final List<BhNodeViewGroup> subGroupList = new ArrayList<>();
  /** このグループを持つ {@link ConnectiveNodeView}. */
  private ConnectiveNodeView parentView;
  /** このグループを持つ {@link BhNodeViewGroup}. */
  private BhNodeViewGroup parentGroup;
  /** このグループが内部描画ノードを持つグループの場合 true. */
  public final boolean inner; 
  /** ノードの配置を決めるパラメータ. */
  private BhNodeViewStyle.Arrangement arrangeParams;
  /** 子要素の名前のリスト. */
  private final List<String> childNames = new ArrayList<>();
  /** コネクタ名とそのコネクタにつながる {@link BhNodeView}. */
  private final Map<String, BhNodeView> childNameToNodeView = new HashMap<>();
  private final Vec2D size = new Vec2D(0.0, 0.0);
  private final Vec2D relativePos = new Vec2D(0.0, 0.0);
  /** 疑似ビューの ID. */
  private int pseudoViewId = 0;

  /**
   * コンストラクタ.
   *
   * @param parentView このグループを持つConnectiveNode
   * @param inner このグループが外部描画ノードを持つグループの場合true
   */
  public BhNodeViewGroup(ConnectiveNodeView parentView, boolean inner) {
    this.parentView = parentView;
    this.inner = inner;
  }

  /**
   * コンストラクタ.
   *
   * @param parentGroup このグループを持つ {@link BhNodeViewGroup}
   * @param inner このグループが内部描画ノードを持つグループの場合 true
   */
  public BhNodeViewGroup(BhNodeViewGroup parentGroup, boolean inner) {
    this.parentGroup = parentGroup;
    this.inner = inner;
  }

  /**
   * このノード以下のサブグループを作成する.
   *
   * @param arrangeParams ノード配置パラメータ
   * @param styleId このグループのスタイルが定義してある {@link BhNodeViewStyle} の ID
   * @throws ViewInitializationException グループの初期化に失敗した
   */
  void buildSubGroup(BhNodeViewStyle.Arrangement arrangeParams, String styleId)
      throws ViewInitializationException {
    this.arrangeParams = arrangeParams;
    for (String cnctrName : arrangeParams.cnctrNameList) {
      // コネクタ名が $ から始まる場合は, 疑似ビューの指定と見なす.
      if (cnctrName.startsWith("$")) {
        BhNodeView view = createPseudoView(cnctrName, styleId);
        view.getTreeManager().setParentGroup(this);
        String pseudoViewName = "$" + pseudoViewId++;
        childNameToNodeView.put(pseudoViewName, view);
        childNames.add(pseudoViewName);
        continue;
      }
      childNameToNodeView.put(cnctrName, null);
      childNames.add(cnctrName);
      // 外部ノードをつなぐコネクタは1つだけとする
      if (!inner) {
        return;
      }
    }
    for (Arrangement subGroupParams : arrangeParams.subGroup) {
      var subGroup = new BhNodeViewGroup(this, inner);
      subGroup.buildSubGroup(subGroupParams, styleId);
      subGroupList.add(subGroup);
    }
  }

  /**
   * BhNodeView を追加する. view の適切な追加先が見つからなかった場合, 失敗する.
   *
   * @param view 追加するノードビュー
   * @return 追加に成功した場合 true. 失敗した場合 false.
   */
  boolean addNodeView(BhNodeView view) {
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
   * このグループが持つoldNodeView をnewNodeViewと入れ替える.
   * ただし, 古いノードのGUIツリーからの削除は行わない.
   *
   * @param oldNodeView 入れ替えられる古いノード
   * @param newNodeView 入れ替える新しいノード
   */
  void replace(BhNodeView oldNodeView, BhNodeView newNodeView) {
    for (Entry<String, BhNodeView> entrySet : childNameToNodeView.entrySet()) {
      if (entrySet.getValue().equals(oldNodeView)) {
        entrySet.setValue(newNodeView);
        newNodeView.getTreeManager().setParentGroup(this);    //親をセット
        oldNodeView.getTreeManager().setParentGroup(null);    //親を削除
        newNodeView.getTreeManager().addToGuiTree(oldNodeView.getParent());
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

  Vec2D getSize() {
    return new Vec2D(size.x, size.y);
  }

  /**
   * このグループの絶対位置を更新する関数.
   *
   * @param posX グループ左上のX絶対位置
   * @param posY グループ左上のY絶対位置
   */
  void updateAbsPos(double posX, double posY) {
    for (String childName : childNames) {
      BhNodeView childNodeView = childNameToNodeView.get(childName);
      if (childNodeView == null) {
        continue;
      }
      Vec2D relativePos = childNodeView.getPositionManager().getRelativePosFromParent();
      childNodeView.getPositionManager().setPosOnWorkspace(
          posX + relativePos.x, posY + relativePos.y);
    }
    subGroupList.forEach(subGroup -> {
      Vec2D subGroupRelPos = subGroup.getRelativePosFromParent();
      subGroup.updateAbsPos(posX + subGroupRelPos.x, posY + subGroupRelPos.y);
    });
  }

  /** 子要素を並べ直し, このグループのサイズを変更する. */
  void arrangeAndResize() {
    Vec2D offset = calcOffsetOfCnctr();
    Vec2D childRelPos =
        new Vec2D(offset.x + arrangeParams.paddingLeft, offset.y + arrangeParams.paddingTop);
    Vec2D childMaxLen = new Vec2D(0.0, 0.0);
    Vec2D childSumLen = new Vec2D(0.0, 0.0);

    updateChildNodeRelPos(childRelPos, childMaxLen, childSumLen);
    updateChildGroupRelPos(childRelPos, childMaxLen, childSumLen);
    updateGroupSize(childMaxLen, childSumLen, offset);
  }

  /**
   * 子ノードの相対位置を更新する.
   *
   * @param childRelPos 最初の子ノードの親からの相対位置. 次の子要素を置くべき相対位置で上書きされる.
   * @param childMaxLen 子ノードの最大幅と高さが格納される.
   * @param childSumLen 子ノードと幅の合計と高さの合計が格納される.
   */
  private void updateChildNodeRelPos(Vec2D childRelPos, Vec2D childMaxLen, Vec2D childSumLen) {
    for (String childName : childNames) {
      BhNodeView childNodeView = childNameToNodeView.get(childName);
      if (childNodeView == null) {
        continue;
      }
      //outer はコネクタの大きさを考慮しない
      Vec2D cnctrSize = inner ? childNodeView.viewStyle.getConnectorSize(childNodeView.isFixed()) 
          : new Vec2D(0.0,  0.0);
      Vec2D childNodeSize = childNodeView.getRegionManager().getNodeSizeIncludingOuter(false);
      //コネクタが上に付く
      if (childNodeView.viewStyle.connectorPos == BhNodeViewStyle.ConnectorPos.TOP) {
        childSumLen.add(childNodeSize.x, childNodeSize.y + cnctrSize.y);
        childMaxLen.updateIfGreater(childNodeSize.x, childNodeSize.y);
        //グループの中が縦並び
        if (arrangeParams.arrangement == BhNodeViewStyle.ChildArrangement.COLUMN) {
          childRelPos.add(0, cnctrSize.y);
        }
      //コネクタが左に付く
      } else if (childNodeView.viewStyle.connectorPos == BhNodeViewStyle.ConnectorPos.LEFT) {
        childSumLen.add(childNodeSize.x + cnctrSize.x, childNodeSize.y);
        childMaxLen.updateIfGreater(childNodeSize.x, childNodeSize.y);
        //グループの中が横並び
        if (arrangeParams.arrangement == BhNodeViewStyle.ChildArrangement.ROW) {
          childRelPos.add(cnctrSize.x, 0);
        }
      }
      childNodeView.getPositionManager().setRelativePosFromParent(childRelPos.x, childRelPos.y);
      updateChildRelativePos(childRelPos, childNodeSize);
    }
  }

  /**
   * 子グループの相対位置を更新する.
   *
   * @param childRelPos 最初の子グループの親からの相対位置. 次の子要素を置くべき相対位置で上書きされる.
   * @param childMaxLen 子グループの最大幅と高さが格納される.
   * @param childSumLen 子グループと幅の合計と高さの合計が格納される.
   */
  private void updateChildGroupRelPos(Vec2D childRelPos, Vec2D childMaxLen, Vec2D childSumLen) {
    for (BhNodeViewGroup subGroup : subGroupList) {
      subGroup.setRelativePosFromParent(childRelPos.x, childRelPos.y);
      Vec2D subGroupSize = subGroup.getSize();
      updateChildRelativePos(childRelPos, subGroupSize);
      childMaxLen.updateIfGreter(subGroupSize);
      childSumLen.add(subGroupSize);
    }
  }

  /**
   * このグループのサイズを更新する.
   *
   * @param childMaxLen 子ノードの中の最大の長さ (コネクタ部分を含まない)
   * @param childSumSize 全子ノードの合計の長さ (コネクタ部分を含む)
   * @param offsetOfCnctr コネクタサイズによって決まるオフセット
   * */
  private void updateGroupSize(Vec2D childMaxLen, Vec2D childSumLen, Vec2D offsetOfCnctr) {
    size.x = arrangeParams.paddingLeft;
    size.y = arrangeParams.paddingTop;
    int numSpace = Math.max(0, childNames.size() + subGroupList.size() - 1);
    //グループの中が縦並び
    if (arrangeParams.arrangement == BhNodeViewStyle.ChildArrangement.COLUMN) {
      size.x += offsetOfCnctr.x + childMaxLen.x + arrangeParams.paddingRight;
      size.y += childSumLen.y + numSpace * arrangeParams.space + arrangeParams.paddingBottom;
    // グループの中が横並び
    } else {    
      size.x += childSumLen.x + numSpace * arrangeParams.space + arrangeParams.paddingRight;
      size.y += offsetOfCnctr.y + childMaxLen.y + arrangeParams.paddingBottom;
    }
  }

  /**
   * 子ノードとサブグループのこのノードからの相対位置を更新する.
   *
   * @param relPos 更新する相対位置
   * @param childSize 子ノードまたはサブグループの大きさ
   */
  private void updateChildRelativePos(Vec2D relPos, Vec2D childSize) {
    //グループの中が縦並び
    if (arrangeParams.arrangement == BhNodeViewStyle.ChildArrangement.COLUMN) {
      relPos.y += childSize.y + arrangeParams.space;
    //グループの中が横並び
    } else {
      relPos.x += childSize.x + arrangeParams.space;
    }
  }

  /** コネクタサイズによって決まる子ノードとサブグループを並べる際のオフセットを計算する. */
  private Vec2D calcOffsetOfCnctr() {
    double offsetX = 0.0;
    double offsetY = 0.0;
    // 外部ノードはコネクタの大きさを考慮しない
    if (!inner) {
      return new Vec2D(0.0, 0.0);
    }
    for (String childName : childNames) {
      BhNodeView childNodeView = childNameToNodeView.get(childName);
      if (childNodeView == null) {
        continue;
      }
      Vec2D cnctrSize = childNodeView.viewStyle.getConnectorSize(childNodeView.isFixed());
      // グループの中が縦並びでかつコネクタが左に付く
      if (arrangeParams.arrangement == BhNodeViewStyle.ChildArrangement.COLUMN
          && childNodeView.viewStyle.connectorPos == BhNodeViewStyle.ConnectorPos.LEFT) {
        offsetX = Math.max(offsetX, cnctrSize.x);
      // グループの中が横並びでかつコネクタが上に付く
      } else if (arrangeParams.arrangement == BhNodeViewStyle.ChildArrangement.ROW
          && childNodeView.viewStyle.connectorPos == BhNodeViewStyle.ConnectorPos.TOP) {
        offsetY = Math.max(offsetY, cnctrSize.y);
      }
    }
    return new Vec2D(offsetX, offsetY);
  }

  /**
   * visitor をサブグループに渡す.
   *
   * @param visitor サブグループに渡す visitor
   */
  public void sendToSubGroupList(NodeViewProcessor visitor) {
    subGroupList.forEach(group -> group.accept(visitor));
  }

  /**
   * visitor を子ノードビューに渡す.
   *
   * @param visitor 子ノードビューに渡す visitor
   */
  public void sendToChildNode(NodeViewProcessor visitor) {
    for (BhNodeView child : childNameToNodeView.values()) {
      if (child != null) {
        child.accept(visitor);
      }
    }
  }

  /** このグループの中で定義された MVC 構造を持たない {@link BhNodeView} を作成する. */
  public BhNodeView createPseudoView(String specification, String styleId)
      throws ViewInitializationException {
    String pattern = "^\\$(?:\\{((?:(?:\\\\\\{)|(?:\\\\\\})|[^\\{\\}])+)\\}){2,}$";
    if (!specification.matches(pattern)) {
      throw new ViewInitializationException(
          "Invalid pseudo view format (" + specification + ") in " + styleId + ".");
    }
    pattern = "\\{((?:(?:\\\\\\{)|(?:\\\\\\})|[^\\{\\}])+)\\}";
    Matcher matcher = Pattern.compile(pattern).matcher(specification);
    List<String> specifiers = matcher.results().map(
        result -> result.group(1).replaceAll("\\\\\\{", "{").replaceAll("\\\\\\}", "}")).toList();
    String styleIdOfPseudoView =
        specifiers.get(0).endsWith(".json") ? specifiers.get(0) : specifiers.get(0) + ".json";
    return ViewHelper.INSTANCE.createModellessNodeView(styleIdOfPseudoView, specifiers.get(1));
  }

  @Override
  public void accept(NodeViewProcessor visitor) {
    visitor.visit(this);
  }

  @Override
  public void show(int depth) {
    try {
      MsgPrinter.INSTANCE.msgForDebug(indent(depth) + "<BhNodeViewGroup>  "  + this.hashCode());
      MsgPrinter.INSTANCE.msgForDebug(indent(depth + 1) + (inner ? "<inner>" : "<outer>"));
      childNames.stream()
          .map(childName -> childNameToNodeView.get(childName))
          .filter(childNodeView -> childNodeView != null)
          .forEachOrdered(childNodeView -> childNodeView.show(depth + 1));
      subGroupList.forEach(subGroup -> subGroup.show(depth + 1));
    } catch (Exception e) {
      MsgPrinter.INSTANCE.msgForDebug("connectiveNodeView show exception " + e);
    }
  }
}
