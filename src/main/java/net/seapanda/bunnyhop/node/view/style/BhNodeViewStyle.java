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

package net.seapanda.bunnyhop.node.view.style;


import java.util.ArrayList;
import java.util.List;
import net.seapanda.bunnyhop.common.configuration.BhConstants;
import net.seapanda.bunnyhop.node.model.parameter.BhNodeViewStyleId;
import net.seapanda.bunnyhop.node.view.BhNodeView;
import net.seapanda.bunnyhop.node.view.ConnectiveNodeView;
import net.seapanda.bunnyhop.node.view.bodyshape.BodyShapeBase.BodyShape;
import net.seapanda.bunnyhop.node.view.component.ComponentType;
import net.seapanda.bunnyhop.node.view.connectorshape.ConnectorShape;
import net.seapanda.bunnyhop.node.view.connectorshape.ConnectorShape.CnctrShape;
import net.seapanda.bunnyhop.utility.math.Vec2D;

/**
 * 描画時の見た目 (大きさ, 色など) の情報を持つクラス.
 *
 * @author K.Koike
 */
public class BhNodeViewStyle {

  /** ノードスタイルに付けられたID. */
  public BhNodeViewStyleId id = BhNodeViewStyleId.NONE;
  /** ノード上部の余白. */
  public double paddingTop = 2.5 * BhConstants.LnF.NODE_SCALE;
  /** ノード下部の余白. */
  public double paddingBottom = 2.5 * BhConstants.LnF.NODE_SCALE;
  /** ノード左部の余白. */
  public double paddingLeft = 2.5 * BhConstants.LnF.NODE_SCALE;
  /** ノード右部の余白. */
  public double paddingRight = 2.5 * BhConstants.LnF.NODE_SCALE;
  public BodyShape bodyShape = BodyShape.BODY_SHAPE_ROUND_RECT;
  /** コネクタの位置. */
  public ConnectorPos connectorPos = ConnectorPos.TOP;
  /** ノードの左上からのコネクタの位置. */
  public double connectorShift = 0.5 * BhConstants.LnF.NODE_SCALE;
  /** コネクタ部分の幅. */
  public double connectorWidth = 1.5 * BhConstants.LnF.NODE_SCALE;
  /** コネクタ部分の高さ. */
  public double connectorHeight = 1.5 * BhConstants.LnF.NODE_SCALE;
  /** コネクタをそろえる位置. */
  public ConnectorAlignment connectorAlignment = ConnectorAlignment.EDGE;
  /** コネクタの形. */
  public ConnectorShape.CnctrShape connectorShape = ConnectorShape.CnctrShape.ARROW;
  /** 固定ノードのコネクタの形. */
  public ConnectorShape.CnctrShape connectorShapeFixed = ConnectorShape.CnctrShape.ARROW;
  /** 切り欠きの位置. */
  public NotchPos notchPos = NotchPos.RIGHT;
  /** コネクタ部分の幅. */
  public double notchWidth = 1.5 * BhConstants.LnF.NODE_SCALE;
  /** コネクタ部分の高さ. */
  public double notchHeight = 1.5 * BhConstants.LnF.NODE_SCALE;
  /** 切り欠きの形. */
  public ConnectorShape.CnctrShape notchShape =  ConnectorShape.CnctrShape.NONE;
  /** 固定ノードの切り欠きの形. */
  public ConnectorShape.CnctrShape notchShapeFixed =  ConnectorShape.CnctrShape.NONE;
  /** ドラッグ&ドロップ時などに適用されるコネクタの範囲. */
  public double connectorBoundsRate = 2.0;
  public String[] cssClasses = { "defaultNode" };
  /** {@link BhNodeView} の種類. */
  public ComponentType component = ComponentType.NONE;
  /** 共通部分と特有部分の並べ方. */
  public ChildArrangement baseArrangement = ChildArrangement.ROW;
  /** {@link ConnectiveNodeView} のパラメータ. */
  public Connective connective = new Connective();
  /** ノードの共通部分のパラメータ.  */
  public CommonPart commonPart = new CommonPart();
  /** ノードの固有部分のパラメータ. */
  public SpecificPart specificPart = new SpecificPart();

  /** {@link ConnectiveNodeView} に特有のパラメータ. */
  public static class Connective {
    public Arrangement inner = new Arrangement();
    public Arrangement outer = new Arrangement();
  }

  /** ノードの内部に描画するノードの並べ方のパラメータ. */
  public static class Arrangement {
    /** ノード内部に描画するノード同士の間隔. */
    public double space = 2.5 * BhConstants.LnF.NODE_SCALE;
    /** 内部ノード上部の余白. */
    public double paddingTop = 0;
    /** 内部ノード右部の余白. */
    public double paddingRight = 0;
    /** 内部ノード下部の余白. */
    public double paddingBottom = 0;
    /** 内部ノード左部の余白. */
    public double paddingLeft = 0;
    /** 子要素のノードとサブグループが並ぶ方向. */
    public ChildArrangement arrangement = ChildArrangement.COLUMN;
    public List<String> cnctrNameList = new ArrayList<>();
    public List<Arrangement> subGroups = new ArrayList<>();

    /** コンストラクタ. */
    public Arrangement() {}

    /** コピーコンストラクタ. */
    public Arrangement(Arrangement org) {
      space = org.space;
      paddingTop = org.paddingTop;
      paddingRight = org.paddingRight;
      paddingBottom = org.paddingBottom;
      paddingLeft = org.paddingLeft;
      arrangement = org.arrangement;
      cnctrNameList.addAll(org.cnctrNameList);
      org.subGroups.forEach(subGrp -> subGroups.add(new Arrangement(subGrp)));
    }
  }

  public TextField textField = new TextField();

  /** テキストフィールドのパラメータ. */
  public static class TextField {
    public double minWidth = 0 * BhConstants.LnF.NODE_SCALE;
    public boolean editable = true;
    public String cssClass = "defaultTextField";
  }

  public Label label = new Label();

  /** ラベルのパラメータ. */
  public static class Label {
    public String cssClass = "defaultLabel";
  }

  public ComboBox comboBox = new ComboBox();

  /** コンボボックスのパラメータ. */
  public static class ComboBox {
    public String cssClass = "defaultComboBox";
  }

  public TextArea textArea = new TextArea();

  /** テキストエリアのパラメータ. */
  public static class TextArea {
    public double minWidth = 4 * BhConstants.LnF.NODE_SCALE;
    public double minHeight = 3 * BhConstants.LnF.NODE_SCALE;
    public boolean editable = true;
    public String cssClass = "defaultTextArea";
  }

  /** ボタンのパラメータ. */
  public static class Button {
    public String cssClass;
    
    public Button(String cssClass) {
      this.cssClass = cssClass;
    }
  }

  /** ブレークポイントのパラメータ. */
  public static class Breakpoint {
    public double radius = 1.8 * BhConstants.LnF.NODE_SCALE;
    public String cssClass;

    public Breakpoint(String cssClass) {
      this.cssClass = cssClass;
    }
  }

  /** 次に実行するノードであることを表す印のパラメータ. */
  public static class ExecStepMark {
    public double size = 5.5 * BhConstants.LnF.NODE_SCALE;
    public String cssClass;

    public ExecStepMark(String cssClass) {
      this.cssClass = cssClass;
    }
  }

  /** ノードが破損していることを表す印のパラメータ. */
  public static class CorruptionMark {
    public double size = 5.5 * BhConstants.LnF.NODE_SCALE;
    public String cssClass;

    public CorruptionMark(String cssClass) {
      this.cssClass = cssClass;
    }
  }

  /** ノードビューの共通部分のパラメータ. */
  public static class CommonPart {
    /** 共通部分のペインに適用される css クラス. */
    public String cssClass = "defaultCommonPart";
    /** 共通部分の子要素を並べる方向. */
    public ChildArrangement arrangement = ChildArrangement.ROW;
    /** プライベートテンプレートボタンのパラメータ. */
    public Button privateTemplate = new Button("defaultPrivateTemplateButton");
    /** ブレークポイントのパラメータ. */
    public Breakpoint breakpoint = new Breakpoint("defaultBreakpoint");
    /** 次に実行するノードであることを表す印のパラメータ. */
    public ExecStepMark execStepMark = new ExecStepMark("defaultExecStepMark");
    /** ノードが破損していることを表す印のパラメータ. */
    public CorruptionMark corruptionMark = new CorruptionMark("defaultCorruptionMark");


    /** コンストラクタ. */
    public CommonPart() {}

    /** コピーコンストラクタ. */
    public CommonPart(CommonPart org) {
      this.arrangement = org.arrangement;
      this.cssClass = org.cssClass;
      this.privateTemplate.cssClass = org.privateTemplate.cssClass;
      this.breakpoint.radius = org.breakpoint.radius;
      this.breakpoint.cssClass = org.breakpoint.cssClass;
      this.execStepMark.size = org.execStepMark.size;
      this.execStepMark.cssClass = org.execStepMark.cssClass;
      this.corruptionMark.size = org.corruptionMark.size;
      this.corruptionMark.cssClass = org.corruptionMark.cssClass;
    }
  }

  /** ノードビューの種類によって固有のコンポーネントが乗る部分のパラメータ. */
  public static class SpecificPart {
    public String cssClass = "defaultSpecificPart";
  }

  /** コネクタの位置. */
  public enum ConnectorPos {

    LEFT(BhConstants.NodeStyleDef.VAL_LEFT),
    TOP(BhConstants.NodeStyleDef.VAL_TOP);

    private final String name;
  
    private ConnectorPos(String name) {
      this.name = name;
    }
  
    /** タイプ名から列挙子を得る. */
    public static ConnectorPos of(String name) {
      for (var val : ConnectorPos.values()) {
        if (val.getName().equals(name)) {
          return val;
        }
      }
      throw new IllegalArgumentException(
          "Unknown %s  (%s)".formatted(ConnectorPos.class.getSimpleName(), name));
    }
  
    public String getName() {
      return name;
    }
  
    @Override
    public String toString() {
      return name;
    }
  }

  /** コネクタをそろえる部分. */
  public enum ConnectorAlignment {

    /** コネクタの端をノードボディの端に合わせる. */
    CENTER(BhConstants.NodeStyleDef.VAL_CENTER),
    /** コネクタの中央をノードボディの中央に合わせる. */
    EDGE(BhConstants.NodeStyleDef.VAL_EDGE);

    private final String name;
  
    private ConnectorAlignment(String name) {
      this.name = name;
    }
  
    /** タイプ名から列挙子を得る. */
    public static ConnectorAlignment of(String name) {
      for (var val : ConnectorAlignment.values()) {
        if (val.getName().equals(name)) {
          return val;
        }
      }
      throw new IllegalArgumentException(
          "Unknown %s  (%s)".formatted(ConnectorAlignment.class.getSimpleName(), name));
    }
  
    public String getName() {
      return name;
    }
  
    @Override
    public String toString() {
      return name;
    }
  }

  /** 切り欠きの位置. */
  public enum NotchPos {

    RIGHT(BhConstants.NodeStyleDef.VAL_RIGHT),
    BOTTOM(BhConstants.NodeStyleDef.VAL_BOTTOM);

    private final String name;
  
    private NotchPos(String name) {
      this.name = name;
    }
  
    /** タイプ名から列挙子を得る. */
    public static NotchPos of(String name) {
      for (var val : NotchPos.values()) {
        if (val.getName().equals(name)) {
          return val;
        }
      }
      throw new IllegalArgumentException(
          "Unknown %s  (%s)".formatted(NotchPos.class.getSimpleName(), name));
    }
  
    public String getName() {
      return name;
    }
  
    @Override
    public String toString() {
      return name;
    }
  }

  /** 子要素の描画方向. */
  public enum ChildArrangement {

    ROW(BhConstants.NodeStyleDef.VAL_ROW),
    COLUMN(BhConstants.NodeStyleDef.VAL_COLUMN);
  
    private final String name;
  
    private ChildArrangement(String name) {
      this.name = name;
    }
  
    /** タイプ名から列挙子を得る. */
    public static ChildArrangement of(String name) {
      for (var val : ChildArrangement.values()) {
        if (val.getName().equals(name)) {
          return val;
        }
      }
      throw new IllegalArgumentException(
          "Unknown %s  (%s)".formatted(ChildArrangement.class.getSimpleName(), name));
    }
  
    public String getName() {
      return name;
    }
  
    @Override
    public String toString() {
      return name;
    }
  }

  /** コンストラクタ. */
  public BhNodeViewStyle() {}

  /**
   * コピーコンストラクタ.
   *
   * @param org コピー元
   */
  BhNodeViewStyle(BhNodeViewStyle org) {
    this.id = org.id;
    this.paddingTop = org.paddingTop;
    this.paddingBottom = org.paddingBottom;
    this.paddingLeft = org.paddingLeft;
    this.paddingRight = org.paddingRight;
    this.bodyShape = org.bodyShape;
    this.connectorPos = org.connectorPos;
    this.connectorShift = org.connectorShift;
    this.connectorAlignment = org.connectorAlignment;
    this.connectorWidth = org.connectorWidth;
    this.connectorHeight = org.connectorHeight;
    this.connectorShape = org.connectorShape;
    this.connectorShapeFixed = org.connectorShapeFixed;
    this.connectorBoundsRate = org.connectorBoundsRate;
    this.notchPos = org.notchPos;
    this.notchWidth = org.notchWidth;
    this.notchHeight = org.notchHeight;
    this.notchShape = org.notchShape;
    this.notchShapeFixed = org.notchShapeFixed;
    this.connective.inner = new Arrangement(org.connective.inner);
    this.connective.outer = new Arrangement(org.connective.outer);
    this.cssClasses = org.cssClasses;
    this.component = org.component;
    this.baseArrangement = org.baseArrangement;
    this.textField.minWidth = org.textField.minWidth;
    this.textField.cssClass = org.textField.cssClass;
    this.textField.editable = org.textField.editable;
    this.label.cssClass = org.label.cssClass;
    this.comboBox.cssClass = org.comboBox.cssClass;
    this.textArea.minWidth = org.textArea.minWidth;
    this.textArea.minHeight = org.textArea.minHeight;
    this.textArea.editable = org.textArea.editable;
    this.textArea.cssClass = org.textArea.cssClass;
    this.commonPart = new CommonPart(org.commonPart);
    this.specificPart.cssClass = org.specificPart.cssClass;
  }

  /**
   * コネクタの大きさを取得する.
   *
   * @param isFixed 描画対象が固定ノードの場合 true を指定すること.
   * @return コネクタの大きさ
   */
  public Vec2D getConnectorSize(boolean isFixed) {
    CnctrShape shape = isFixed ? connectorShapeFixed : connectorShape;
    double cnctrWidth = (shape == CnctrShape.NONE) ? 0 : connectorWidth;
    double cnctrHeight = (shape == CnctrShape.NONE) ? 0 : connectorHeight;
    return new Vec2D(cnctrWidth, cnctrHeight);
  }
}
