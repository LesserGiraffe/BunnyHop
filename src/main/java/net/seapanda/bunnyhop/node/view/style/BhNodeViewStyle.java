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
import net.seapanda.bunnyhop.node.view.bodyshape.BodyShapeType;
import net.seapanda.bunnyhop.node.view.component.ComponentType;
import net.seapanda.bunnyhop.node.view.connectorshape.ConnectorShapeType;
import net.seapanda.bunnyhop.utility.math.Vec2D;

/**
 * ノードの見た目 (大きさ, 色など) の情報を持つクラス.
 *
 * @author K.Koike
 */
public class BhNodeViewStyle {

  /** ノードスタイルに付けられたID. */
  public BhNodeViewStyleId id = BhNodeViewStyleId.NONE;
  /** ノード上部の余白. */
  public double paddingTop = 2.5 * BhConstants.Ui.NODE_SCALE;
  /** ノード下部の余白. */
  public double paddingBottom = 2.5 * BhConstants.Ui.NODE_SCALE;
  /** ノード左部の余白. */
  public double paddingLeft = 2.5 * BhConstants.Ui.NODE_SCALE;
  /** ノード右部の余白. */
  public double paddingRight = 2.5 * BhConstants.Ui.NODE_SCALE;
  public BodyShapeType bodyShape = BodyShapeType.BODY_SHAPE_ROUND_RECT;
  /** コネクタの位置. */
  public ConnectorPos connectorPos = ConnectorPos.TOP;
  /** ノードの左上からのコネクタの位置. */
  public double connectorShift = 0.5 * BhConstants.Ui.NODE_SCALE;
  /** コネクタ部分の幅. */
  public double connectorWidth = 1.5 * BhConstants.Ui.NODE_SCALE;
  /** コネクタ部分の高さ. */
  public double connectorHeight = 1.5 * BhConstants.Ui.NODE_SCALE;
  /** コネクタをそろえる位置. */
  public ConnectorAlignment connectorAlignment = ConnectorAlignment.EDGE;
  /** コネクタの形. */
  public ConnectorShapeType connectorShape = ConnectorShapeType.ARROW;
  /** 固定ノードのコネクタの形. */
  public ConnectorShapeType connectorShapeFixed = ConnectorShapeType.ARROW;
  /** 切り欠きの位置. */
  public NotchPos notchPos = NotchPos.RIGHT;
  /** コネクタ部分の幅. */
  public double notchWidth = 1.5 * BhConstants.Ui.NODE_SCALE;
  /** コネクタ部分の高さ. */
  public double notchHeight = 1.5 * BhConstants.Ui.NODE_SCALE;
  /** 切り欠きの形. */
  public ConnectorShapeType notchShape =  ConnectorShapeType.NONE;
  /** 固定ノードの切り欠きの形. */
  public ConnectorShapeType notchShapeFixed =  ConnectorShapeType.NONE;
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
  public TextField textField = new TextField();
  public Label label = new Label();
  public ComboBox comboBox = new ComboBox();
  public TextArea textArea = new TextArea();


  /** コンストラクタ. */
  public BhNodeViewStyle() {}

  /**
   * コピーコンストラクタ.
   *
   * @param org コピー元
   */
  BhNodeViewStyle(BhNodeViewStyle org) {
    id = org.id;
    paddingTop = org.paddingTop;
    paddingBottom = org.paddingBottom;
    paddingLeft = org.paddingLeft;
    paddingRight = org.paddingRight;
    bodyShape = org.bodyShape;
    connectorPos = org.connectorPos;
    connectorShift = org.connectorShift;
    connectorAlignment = org.connectorAlignment;
    connectorWidth = org.connectorWidth;
    connectorHeight = org.connectorHeight;
    connectorShape = org.connectorShape;
    connectorShapeFixed = org.connectorShapeFixed;
    connectorBoundsRate = org.connectorBoundsRate;
    notchPos = org.notchPos;
    notchWidth = org.notchWidth;
    notchHeight = org.notchHeight;
    notchShape = org.notchShape;
    notchShapeFixed = org.notchShapeFixed;
    connective = new Connective(org.connective);
    cssClasses = org.cssClasses.clone();
    component = org.component;
    baseArrangement = org.baseArrangement;
    textField = new TextField(org.textField);
    label = new Label(org.label);
    comboBox = new ComboBox(org.comboBox);
    textArea = new TextArea(org.textArea);
    commonPart = new CommonPart(org.commonPart);
    specificPart = new SpecificPart(org.specificPart);
  }

  /**
   * コネクタの大きさを取得する.
   *
   * @param isFixed 描画対象が固定ノードの場合 true を指定すること.
   * @return コネクタの大きさ
   */
  public Vec2D getConnectorSize(boolean isFixed) {
    ConnectorShapeType
        shape = isFixed ? connectorShapeFixed : connectorShape;
    double cnctrWidth = (shape == ConnectorShapeType.NONE) ? 0 : connectorWidth;
    double cnctrHeight = (shape == ConnectorShapeType.NONE) ? 0 : connectorHeight;
    return new Vec2D(cnctrWidth, cnctrHeight);
  }

  /** {@link ConnectiveNodeView} に特有のパラメータ. */
  public static class Connective {
    public Arrangement inner = new Arrangement();
    public Arrangement outer = new Arrangement();

    private Connective() {}

    /** コピーコンストラクタ. */
    private Connective(Connective org) {
      inner = new Arrangement(org.inner);
      outer = new Arrangement(org.outer);
    }
  }

  /** ノードの内部に描画するノードの並べ方のパラメータ. */
  public static class Arrangement {
    /** ノード内部に描画するノード同士の間隔. */
    public double space = 2.5 * BhConstants.Ui.NODE_SCALE;
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
    public List<String> cnctrNames = new ArrayList<>();
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
      cnctrNames.addAll(org.cnctrNames);
      org.subGroups.forEach(subGrp -> subGroups.add(new Arrangement(subGrp)));
    }
  }

  /** テキストフィールドのパラメータ. */
  public static class TextField {
    public double minWidth = 0 * BhConstants.Ui.NODE_SCALE;
    public boolean editable = true;
    public String cssClass = "defaultTextField";

    private TextField() {}

    private TextField(TextField org) {
      minWidth = org.minWidth;
      editable = org.editable;
      cssClass = org.cssClass;
    }
  }

  /** ラベルのパラメータ. */
  public static class Label {
    public String cssClass = "defaultLabel";

    private Label() {}

    /** コピーコンストラクタ. */
    private Label(Label org) {
      cssClass = org.cssClass;
    }
  }

  /** コンボボックスのパラメータ. */
  public static class ComboBox {
    public String cssClass = "defaultComboBox";

    private ComboBox() {}

    private ComboBox(ComboBox org) {
      cssClass = org.cssClass;
    }
  }

  /** テキストエリアのパラメータ. */
  public static class TextArea {
    public double minWidth = 4 * BhConstants.Ui.NODE_SCALE;
    public double minHeight = 3 * BhConstants.Ui.NODE_SCALE;
    public boolean editable = true;
    public String cssClass = "defaultTextArea";

    private TextArea() {}

    private TextArea(TextArea org) {
      minWidth = org.minWidth;
      minHeight = org.minHeight;
      editable = org.editable;
      cssClass = org.cssClass;
    }
  }

  /** ボタンのパラメータ. */
  public static class Button {
    public String cssClass = "defaultPrivateTemplateButton";

    private Button() {}

    private Button(Button org) {
      cssClass = org.cssClass;
    }
  }

  /** ブレークポイントのパラメータ. */
  public static class Breakpoint {
    public double radius = 1.8 * BhConstants.Ui.NODE_SCALE;
    public String cssClass = "defaultBreakpoint";

    private Breakpoint() {}

    private Breakpoint(Breakpoint org) {
      radius = org.radius;
      cssClass = org.cssClass;
    }
  }

  /** 次に実行するノードであることを表す印のパラメータ. */
  public static class ExecStepMark {
    public double size = 5.5 * BhConstants.Ui.NODE_SCALE;
    public String cssClass = "defaultExecStepMark";

    private ExecStepMark() {}

    private ExecStepMark(ExecStepMark org) {
      size = org.size;
      cssClass = org.cssClass;
    }
  }

  /** ノードが破損していることを表す印のパラメータ. */
  public static class CorruptionMark {
    public double size = 5.5 * BhConstants.Ui.NODE_SCALE;
    public String cssClass = "defaultCorruptionMark";

    private CorruptionMark() {}

    private CorruptionMark(CorruptionMark org) {
      size = org.size;
      cssClass = org.cssClass;
    }
  }

  /** ノードがエントリポイントであることを表す印のパラメータ. */
  public static class EntryPointMark {
    public double radius = 1.8 * BhConstants.Ui.NODE_SCALE;
    public String cssClass = "defaultEntryPointMark";

    private EntryPointMark() {}

    private EntryPointMark(EntryPointMark org) {
      radius = org.radius;
      cssClass = org.cssClass;
    }
  }

  /** ノードビューの共通部分のパラメータ. */
  public static class CommonPart {
    /** 共通部分のペインに適用される css クラス. */
    public String cssClass = "defaultCommonPart";
    /** 共通部分の子要素を並べる方向. */
    public ChildArrangement arrangement = ChildArrangement.ROW;
    /** プライベートテンプレートボタンのパラメータ. */
    public Button privateTemplate = new Button();
    /** ブレークポイントのパラメータ. */
    public Breakpoint breakpoint = new Breakpoint();
    /** 次に実行するノードであることを表す印のパラメータ. */
    public ExecStepMark execStepMark = new ExecStepMark();
    /** ノードが破損していることを表す印のパラメータ. */
    public CorruptionMark corruptionMark = new CorruptionMark();
    /** ノードがエントリポイントであることを表す印のパラメータ. */
    public EntryPointMark entryPointMark = new EntryPointMark();

    /** コンストラクタ. */
    private CommonPart() {}

    /** コピーコンストラクタ. */
    private CommonPart(CommonPart org) {
      cssClass = org.cssClass;
      arrangement = org.arrangement;
      privateTemplate = new Button(org.privateTemplate);
      breakpoint = new Breakpoint(org.breakpoint);
      execStepMark = new ExecStepMark(org.execStepMark);
      corruptionMark = new CorruptionMark(org.corruptionMark);
      entryPointMark = new EntryPointMark(org.entryPointMark);
    }
  }

  /** ノードビューの種類によって固有のコンポーネントが乗る部分のパラメータ. */
  public static class SpecificPart {
    public String cssClass = "defaultSpecificPart";

    private SpecificPart() {}

    private SpecificPart(SpecificPart org) {
      cssClass = org.cssClass;
    }
  }
}
