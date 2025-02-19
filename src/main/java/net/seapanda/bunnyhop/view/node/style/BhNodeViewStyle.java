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

package net.seapanda.bunnyhop.view.node.style;


import java.util.ArrayList;
import java.util.List;
import net.seapanda.bunnyhop.common.BhConstants;
import net.seapanda.bunnyhop.model.node.parameter.BhNodeViewStyleId;
import net.seapanda.bunnyhop.utility.Vec2D;
import net.seapanda.bunnyhop.view.bodyshape.BodyShapeBase.BodyShape;
import net.seapanda.bunnyhop.view.connectorshape.ConnectorShape;
import net.seapanda.bunnyhop.view.connectorshape.ConnectorShape.CnctrShape;
import net.seapanda.bunnyhop.view.node.BhNodeView;
import net.seapanda.bunnyhop.view.node.ConnectiveNodeView;
import net.seapanda.bunnyhop.view.node.component.ComponentType;

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
  public String cssClass = "defaultNode";
  /** {@link BhNodeView} の種類. */
  public ComponentType component = ComponentType.NONE;
  
  public Connective connective = new Connective();

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

  public Button privatTemplate = new Button("defaultPrivateTemplateButton");

  /** ボタンのパラメータ. */
  public static class Button {
    public double buttonPosX = 0.5 * BhConstants.LnF.NODE_SCALE;
    public double buttonPosY = 0.5 * BhConstants.LnF.NODE_SCALE;
    public String cssClass = "defaultPrivateTemplateButton";

    public Button(String cssClass) {
      this.cssClass = cssClass;
    }
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

  /** 子ノードの描画方向. */
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
    this.cssClass = org.cssClass;
    this.component = org.component;
    this.textField.minWidth = org.textField.minWidth;
    this.textField.cssClass = org.textField.cssClass;
    this.textField.editable = org.textField.editable;
    this.label.cssClass = org.label.cssClass;
    this.comboBox.cssClass = org.comboBox.cssClass;
    this.textArea.minWidth = org.textArea.minWidth;
    this.textArea.minHeight = org.textArea.minHeight;
    this.textArea.editable = org.textArea.editable;
    this.textArea.cssClass = org.textArea.cssClass;
    this.privatTemplate.cssClass = org.privatTemplate.cssClass;
    this.privatTemplate.buttonPosX = org.privatTemplate.buttonPosX;
    this.privatTemplate.buttonPosY = org.privatTemplate.buttonPosY;
  }

  /**
   * コネクタの大きさを取得する.
   *
   * @param isFixed 描画対象が固定ノードの場合 true を指定すること.
   * @return コネクタの大きさ
   */
  public Vec2D getConnectorSize(boolean isFixed) {
    double cnctrWidth = 0.0;
    CnctrShape shape = isFixed ? connectorShapeFixed : connectorShape;
    if (shape != CnctrShape.NONE) {
      cnctrWidth = connectorWidth;
    }
    double cnctrHeight = 0.0;
    if (shape != CnctrShape.NONE) {
      cnctrHeight = connectorHeight;
    }
    return new Vec2D(cnctrWidth, cnctrHeight);
  }
}
