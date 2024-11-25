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

package net.seapanda.bunnyhop.view.node.part;

import static java.nio.file.FileVisitOption.FOLLOW_LINKS;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;
import net.seapanda.bunnyhop.common.Vec2D;
import net.seapanda.bunnyhop.common.constant.BhConstants;
import net.seapanda.bunnyhop.model.node.attribute.BhNodeId;
import net.seapanda.bunnyhop.service.BhScriptManager;
import net.seapanda.bunnyhop.service.MsgPrinter;
import net.seapanda.bunnyhop.service.Util;
import net.seapanda.bunnyhop.view.bodyshape.BodyShapeBase;
import net.seapanda.bunnyhop.view.bodyshape.BodyShapeBase.BodyShape;
import net.seapanda.bunnyhop.view.connectorshape.ConnectorShape;
import net.seapanda.bunnyhop.view.connectorshape.ConnectorShape.CnctrShape;
import net.seapanda.bunnyhop.view.node.BhNodeView;
import net.seapanda.bunnyhop.view.node.ConnectiveNodeView;
import org.mozilla.javascript.NativeArray;
import org.mozilla.javascript.NativeObject;

/**
 * 描画時の見た目 (大きさ, 色など) の情報を持つクラス.
 *
 * @author K.Koike
 */
public class BhNodeViewStyle {

  /** ノードスタイルに付けられたID. */
  public String nodeStyleId;
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
    public List<Arrangement> subGroup = new ArrayList<>();

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
      org.subGroup.forEach(subGrp -> subGroup.add(new Arrangement(subGrp)));
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

  /** ノードスタイルのテンプレートを格納するハッシュ. xml の nodeStyleID がキー */
  private static final HashMap<String, BhNodeViewStyle> nodeStyleIdToNodeStyleTemplate = 
      new HashMap<>();
  /** ノードIDとノードスタイルのペアを格納するハッシュ. */
  private static final HashMap<BhNodeId, String> nodeIdToNodeStyleID = new HashMap<>();

  /** コネクタの位置. */
  public enum ConnectorPos {
    LEFT, TOP
  }

  /** 切り欠きの位置. */
  public enum NotchPos {
    RIGHT, BOTTOM
  }

  /** 子ノードの描画方向. */
  public enum ChildArrangement {
    ROW, COLUMN
  }

  /** コンストラクタ. */
  public BhNodeViewStyle() {}

  /**
   * コピーコンストラクタ.
   *
   * @param org コピー元
   */
  private BhNodeViewStyle(BhNodeViewStyle org) {
    this.nodeStyleId = org.nodeStyleId;
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

  /** {@link BhNodeViewStyle} のテンプレートを作成する. */
  public static boolean genViewStyleTemplate() {
    Path dirPath = Paths.get(
        Util.INSTANCE.execPath, BhConstants.Path.VIEW_DIR, BhConstants.Path.NODE_STYLE_DEF_DIR);
    Stream<Path> paths = null; //読み込むファイルパスリスト
    try {
      paths = Files
          .walk(dirPath, FOLLOW_LINKS)
          .filter(path -> path.getFileName().toString().endsWith(".json"));
    } catch (IOException e) {
      MsgPrinter.INSTANCE.errMsgForDebug("Directory not found.  (%s)".formatted(dirPath));
      return false;
    }

    boolean succes = paths.map(filePath -> registerNodeStyle(filePath)).allMatch(Boolean::valueOf);
    nodeStyleIdToNodeStyleTemplate.put(
        BhConstants.BhModelDef.ATTR_VAL_DEFAULT_NODE_STYLE_ID, new BhNodeViewStyle());
    paths.close();
    return succes;
  }

  /**
   * 引数で指定したファイルからノードスタイルを読み取ってレジストリに格納する.
   *
   * @param filePath ノードスタイルファイルのパス
   * @return 成功した場合 true.
   */
  private static Boolean registerNodeStyle(Path filePath) {
    Optional<NativeObject> jsonObj = BhScriptManager.INSTANCE.parseJsonFile(filePath);
    if (jsonObj.isEmpty()) {
      return false;
    }
    String styleId = filePath.getFileName().toString();
    Optional<BhNodeViewStyle> bhNodeViewStyle =
        genBhNodeViewStyle(jsonObj.get(), filePath.toAbsolutePath().toString(), styleId);
    bhNodeViewStyle.ifPresent(
        viewStyle -> nodeStyleIdToNodeStyleTemplate.put(viewStyle.nodeStyleId, viewStyle));
    return bhNodeViewStyle.isPresent();
  }

  /**
   * jsonオブジェクト から BhNodeViewStyle オブジェクトを作成する.
   *
   * @param jsonObj .JSON ファイルを読み込んで作ったトップレベルオブジェクト
   * @param fileName jsonObj が記述してある .JSON ファイルのファイル名
   * @return BhNodeViewStyle (オプション)
   */
  private static Optional<BhNodeViewStyle> genBhNodeViewStyle(
      NativeObject jsonObj, String fileName, String styleId) {
    BhNodeViewStyle style = new BhNodeViewStyle();
    //styleID
    style.nodeStyleId = styleId;

    //paddingTop
    Optional<Object> val =
        readValue(BhConstants.NodeStyleDef.KEY_PADDING_TOP, Number.class, jsonObj, fileName);
    val.ifPresent(paddingTop ->
        style.paddingTop = ((Number) paddingTop).doubleValue() * BhConstants.LnF.NODE_SCALE);

    //paddingBottom
    val = readValue(BhConstants.NodeStyleDef.KEY_PADDING_BOTTOM, Number.class, jsonObj, fileName);
    val.ifPresent(paddingBottom ->
        style.paddingBottom = ((Number) paddingBottom).doubleValue() * BhConstants.LnF.NODE_SCALE);

    //paddingLeft
    val = readValue(BhConstants.NodeStyleDef.KEY_PADDING_LEFT, Number.class, jsonObj, fileName);
    val.ifPresent(paddingLeft ->
        style.paddingLeft = ((Number) paddingLeft).doubleValue() * BhConstants.LnF.NODE_SCALE);

    //paddingRight
    val = readValue(BhConstants.NodeStyleDef.KEY_PADDING_RIGHT, Number.class, jsonObj, fileName);
    val.ifPresent(paddingRight ->
        style.paddingRight = ((Number) paddingRight).doubleValue() * BhConstants.LnF.NODE_SCALE);

    //bodyShape
    val = readValue(BhConstants.NodeStyleDef.KEY_BODY_SHAPE, String.class, jsonObj, fileName);
    val.ifPresent(bodyShape -> {
      String shapeStr = (String) bodyShape;
      BodyShape shapeType = BodyShapeBase.getBodyTypeFromName(shapeStr, fileName);
      style.bodyShape = shapeType;
    });

    //connectorPos
    val = readValue(BhConstants.NodeStyleDef.KEY_CONNECTOR_POS, String.class, jsonObj, fileName);
    val.ifPresent(connectorPos -> {
      String posStr = (String) connectorPos;
      if (posStr.equals(BhConstants.NodeStyleDef.VAL_TOP)) {
        style.connectorPos = ConnectorPos.TOP;
      } else if (posStr.equals(BhConstants.NodeStyleDef.VAL_LEFT)) {
        style.connectorPos = ConnectorPos.LEFT;
      } else {
        MsgPrinter.INSTANCE.errMsgForDebug("'%s' (%s) format is invalid.  (%s)".formatted(
            BhConstants.NodeStyleDef.KEY_CONNECTOR_POS, posStr, fileName));
      }
    });

    //connectorShift
    val = readValue(BhConstants.NodeStyleDef.KEY_CONNECTOR_SHIFT, Number.class, jsonObj, fileName);
    val.ifPresent(connectorShift -> style.connectorShift = 
        ((Number) connectorShift).doubleValue() * BhConstants.LnF.NODE_SCALE);

    //connectorWidth
    val = readValue(BhConstants.NodeStyleDef.KEY_CONNECTOR_WIDTH, Number.class, jsonObj, fileName);
    val.ifPresent(connectorWidth -> style.connectorWidth =
        ((Number) connectorWidth).doubleValue() * BhConstants.LnF.NODE_SCALE);

    //connectorHeight
    val = readValue(BhConstants.NodeStyleDef.KEY_CONNECTOR_HEIGHT, Number.class, jsonObj, fileName);
    val.ifPresent(connectorHeight -> style.connectorHeight =
        ((Number) connectorHeight).doubleValue() * BhConstants.LnF.NODE_SCALE);

    //connectorShape
    val = readValue(BhConstants.NodeStyleDef.KEY_CONNECTOR_SHAPE, String.class, jsonObj, fileName);
    val.ifPresent(shape -> {
      String shapeStr = (String) shape;
      CnctrShape shapeType = ConnectorShape.getConnectorTypeFromName(shapeStr, fileName);
      style.connectorShape = shapeType;
    });

    //connectorShapeFixed
    style.connectorShapeFixed = style.connectorShape;
    val = readValue(
        BhConstants.NodeStyleDef.KEY_CONNECTOR_SHAPE_FIXED, String.class, jsonObj, fileName);
    val.ifPresent(shape -> {
      String shapeStr = (String) shape;
      CnctrShape shapeType = ConnectorShape.getConnectorTypeFromName(shapeStr, fileName);
      style.connectorShapeFixed = shapeType;
    });

    //notchPos
    val = readValue(BhConstants.NodeStyleDef.KEY_NOTCH_POS, String.class, jsonObj, fileName);
    val.ifPresent(notchPos -> {
      String posStr = (String) notchPos;
      if (posStr.equals(BhConstants.NodeStyleDef.VAL_RIGHT)) {
        style.notchPos = NotchPos.RIGHT;
      } else if (posStr.equals(BhConstants.NodeStyleDef.VAL_BOTTOM)) {
        style.notchPos = NotchPos.BOTTOM;
      } else {
        MsgPrinter.INSTANCE.errMsgForDebug("'%s' (%s) format is invalid.  (%s)"
            .formatted(BhConstants.NodeStyleDef.KEY_NOTCH_POS, posStr, fileName));
      }
    });

    //notchWidth
    val = readValue(BhConstants.NodeStyleDef.KEY_NOTCH_WIDTH, Number.class, jsonObj, fileName);
    val.ifPresent(notchWidth ->
        style.notchWidth = ((Number) notchWidth).doubleValue() * BhConstants.LnF.NODE_SCALE);

    //notchHeight
    val = readValue(BhConstants.NodeStyleDef.KEY_NOTCH_HEIGHT, Number.class, jsonObj, fileName);
    val.ifPresent(notchHeight ->
        style.notchHeight = ((Number) notchHeight).doubleValue() * BhConstants.LnF.NODE_SCALE);

    //notchShape
    val = readValue(BhConstants.NodeStyleDef.KEY_NOTCH_SHAPE, String.class, jsonObj, fileName);
    val.ifPresent(notchShape -> {
      String shapeStr = (String) notchShape;
      CnctrShape shapeType = ConnectorShape.getConnectorTypeFromName(shapeStr, fileName);
      style.notchShape = shapeType;
    });

    //notchShapeFixed
    style.notchShapeFixed = style.notchShape;
    val = readValue(
        BhConstants.NodeStyleDef.KEY_NOTCH_SHAPE_FIXED, String.class, jsonObj, fileName);
    val.ifPresent(notchShape -> {
      String shapeStr = (String) notchShape;
      CnctrShape shapeType = ConnectorShape.getConnectorTypeFromName(shapeStr, fileName);
      style.notchShapeFixed = shapeType;
    });

    //connectorBoundsRate
    val = readValue(
        BhConstants.NodeStyleDef.KEY_CONNECTOR_BOUNDS_RATE, Number.class, jsonObj, fileName);
    val.ifPresent(connectorBoundsRate ->
        style.connectorBoundsRate = ((Number) connectorBoundsRate).doubleValue());

    //bodyCssClass
    val = readValue(BhConstants.NodeStyleDef.KEY_CSS_CLASS, String.class, jsonObj, fileName);
    val.ifPresent(bodyCssClass -> style.cssClass = (String) bodyCssClass);

    //connective
    val = readValue(BhConstants.NodeStyleDef.KEY_CONNECTIVE, NativeObject.class, jsonObj, fileName);
    val.ifPresent(
        connective -> fillConnectiveParams(style.connective, (NativeObject) connective, fileName));

    //textField
    val = readValue(BhConstants.NodeStyleDef.KEY_TEXT_FIELD, NativeObject.class, jsonObj, fileName);
    val.ifPresent(
        textField -> fillTextFieldParams(style.textField, (NativeObject) textField, fileName));

    //label
    val = readValue(BhConstants.NodeStyleDef.KEY_LABEL, NativeObject.class, jsonObj, fileName);
    val.ifPresent(label -> fillLabelParams(style.label, (NativeObject) label, fileName));

    //comboBox
    val = readValue(BhConstants.NodeStyleDef.KEY_COMBO_BOX, NativeObject.class, jsonObj, fileName);
    val.ifPresent(
        comboBox -> fillComboBoxParams(style.comboBox, (NativeObject) comboBox, fileName));

    //textArea
    val = readValue(BhConstants.NodeStyleDef.KEY_TEXT_AREA, NativeObject.class, jsonObj, fileName);
    val.ifPresent(
        textArea -> fillTextAreaParams(style.textArea, (NativeObject) textArea, fileName));

    //privateTemplate
    val = readValue(
        BhConstants.NodeStyleDef.KEY_PRIVATE_TEMPLATE, NativeObject.class, jsonObj, fileName);
    val.ifPresent(privateTemplate ->
        fillButtonParams(style.privatTemplate, (NativeObject) privateTemplate, fileName));

    //component
    val = readValue(BhConstants.NodeStyleDef.KEY_COMPONENT, String.class, jsonObj, fileName);
    val.ifPresent(type -> style.component = ComponentType.toType((String) type));
    
    return Optional.of(style);
  }

  /**
   * {@link BhNodeViewStyle.Connective} にスタイル情報を格納する.
   *
   * @param jsonObj key = "connective" の value であるオブジェクト
   * @param fileName jsonObj が記述してある .JSON ファイルの名前
   */
  private static void fillConnectiveParams(
      BhNodeViewStyle.Connective connectiveStyle,
      NativeObject jsonObj,
      String fileName) {
        
    // inner
    Optional<Object> val =
        readValue(BhConstants.NodeStyleDef.KEY_INNER, NativeObject.class, jsonObj, fileName);
    val.ifPresent(innerArrange -> 
        fillArrangementParams(connectiveStyle.inner, (NativeObject) innerArrange, fileName));

    // outer
    val = readValue(BhConstants.NodeStyleDef.KEY_OUTER, NativeObject.class, jsonObj, fileName);
    val.ifPresent(outerArrange -> 
        fillArrangementParams(connectiveStyle.outer, (NativeObject) outerArrange, fileName));
  }

  /**
   * {@link BhNodeViewStyle.Arrangement} にスタイル情報を格納する.
   *
   * @param arrangement 並べ方に関するパラメータの格納先
   * @param jsonObj key = "inner" または "outer" の value であるオブジェクト
   * @param fileName jsonObj が記述してある .JSON ファイルの名前
   */
  private static void fillArrangementParams(
      BhNodeViewStyle.Arrangement arrangement,
      NativeObject jsonObj,
      String fileName) {

    //space
    Optional<Object> val =
        readValue(BhConstants.NodeStyleDef.KEY_SPACE, Number.class, jsonObj, fileName);
    val.ifPresent(space -> {
      arrangement.space = ((Number) space).doubleValue() * BhConstants.LnF.NODE_SCALE;
    });

    //paddingTop
    val = readValue(BhConstants.NodeStyleDef.KEY_PADDING_TOP, Number.class, jsonObj, fileName);
    val.ifPresent(paddingTop -> {
      arrangement.paddingTop = ((Number) paddingTop).doubleValue() * BhConstants.LnF.NODE_SCALE;
    });

    //paddingRight
    val = readValue(BhConstants.NodeStyleDef.KEY_PADDING_RIGHT, Number.class, jsonObj, fileName);
    val.ifPresent(paddingRight -> {
      arrangement.paddingRight = ((Number) paddingRight).doubleValue() * BhConstants.LnF.NODE_SCALE;
    });

    //paddingBottom
    val = readValue(BhConstants.NodeStyleDef.KEY_PADDING_BOTTOM, Number.class, jsonObj, fileName);
    val.ifPresent(paddingBottom -> arrangement.paddingBottom = 
        ((Number) paddingBottom).doubleValue() * BhConstants.LnF.NODE_SCALE);

    //paddingLeft
    val = readValue(BhConstants.NodeStyleDef.KEY_PADDING_LEFT, Number.class, jsonObj, fileName);
    val.ifPresent(paddingLeft -> {
      arrangement.paddingLeft = ((Number) paddingLeft).doubleValue() * BhConstants.LnF.NODE_SCALE;
    });

    //arrangement
    val = readValue(BhConstants.NodeStyleDef.KEY_ARRANGEMENR, String.class, jsonObj, fileName);
    val.ifPresent(childArrange -> {
      String arrangeStr = (String) childArrange;
      if (arrangeStr.equals(BhConstants.NodeStyleDef.VAL_ROW)) {
        arrangement.arrangement = ChildArrangement.ROW;
      } else if (arrangeStr.equals(BhConstants.NodeStyleDef.VAL_COLUMN)) {
        arrangement.arrangement = ChildArrangement.COLUMN;
      } else {
        MsgPrinter.INSTANCE.errMsgForDebug("'%s' (%s) format is invalid.  (%s)"
            .formatted(BhConstants.NodeStyleDef.KEY_ARRANGEMENR, arrangeStr, fileName));
      }
    });

    //cnctrNameList
    val = readValue(
        BhConstants.NodeStyleDef.KEY_CONNECTOR_LIST, NativeArray.class, jsonObj, fileName);
    val.ifPresent(cnctrs -> {
      for (Object cnctrName : (NativeArray) cnctrs) {
        arrangement.cnctrNameList.add(cnctrName.toString());
      }
    });

    //subGroup
    int groupId = 0;
    while (true) {
      String subGroupKeyName = BhConstants.NodeStyleDef.KEY_SUB_GROUP + groupId;
      val = readValue(subGroupKeyName, NativeObject.class, jsonObj, fileName);
      if (val.isPresent()) {
        Arrangement subGroup = new Arrangement();
        fillArrangementParams(subGroup, (NativeObject) val.get(), fileName);
        arrangement.subGroup.add(subGroup);
      } else {
        break;
      }
      ++groupId;
    }
  }

  /**
   * {@link BhNodeViewStyle.Button} にスタイル情報を格納する.
   *
   * @param button jsonObj の情報を格納するオブジェクト
   * @param jsonObj ボタンのパラメータが格納されたオブジェクト
   * @param fileName jsonObj が記述してある .json ファイルの名前
   */
  private static void fillButtonParams(
      BhNodeViewStyle.Button button,
      NativeObject jsonObj,
      String fileName) {

    //buttonPosX
    Optional<Object> val =
        readValue(BhConstants.NodeStyleDef.KEY_BUTTON_POS_X, Number.class, jsonObj, fileName);
    val.ifPresent(btnPosX -> {
      button.buttonPosX = ((Number) btnPosX).doubleValue() * BhConstants.LnF.NODE_SCALE;
    });

    //buttonPosY
    val = readValue(BhConstants.NodeStyleDef.KEY_BUTTON_POS_Y, Number.class, jsonObj, fileName);
    val.ifPresent(btnPosY -> {
      button.buttonPosY = ((Number) btnPosY).doubleValue() * BhConstants.LnF.NODE_SCALE;
    });

    //buttonCssClass
    val = readValue(BhConstants.NodeStyleDef.KEY_CSS_CLASS, String.class, jsonObj, fileName);
    val.ifPresent(buttonCssClass -> button.cssClass = (String) buttonCssClass);
  }

  /**
   * {@link BhNodeViewStyle.TextField} にスタイル情報を格納する.
   *
   * @param textField jsonオブジェクトから読み取った内容を格納するオブジェクト
   * @param jsonObj key = "textField" の value であるオブジェクト
   * @param fileName jsonObj が記述してある .json ファイルの名前
   */
  private static void fillTextFieldParams(
      BhNodeViewStyle.TextField textField,
      NativeObject jsonObj,
      String fileName) {

    //cssClass
    Optional<Object> val =
        readValue(BhConstants.NodeStyleDef.KEY_CSS_CLASS, String.class, jsonObj, fileName);
    val.ifPresent(textCssClass -> textField.cssClass = (String) textCssClass);

    //minWidth
    val = readValue(BhConstants.NodeStyleDef.KEY_MIN_WIDTH, Number.class, jsonObj, fileName);
    val.ifPresent(minWidth ->
        textField.minWidth = ((Number) minWidth).doubleValue() * BhConstants.LnF.NODE_SCALE);
    
    // editable
    val = readValue(BhConstants.NodeStyleDef.KEY_EDITABLE, Boolean.class, jsonObj, fileName);
    val.ifPresent(editable -> textField.editable = ((Boolean) editable).booleanValue());
  }

  /**
   * {@link BhNodeViewStyle.Label} にスタイル情報を格納する.
   *
   * @param label jsonオブジェクトから読み取った内容を格納するオブジェクト
   * @param jsonObj key = "label" の value であるオブジェクト
   * @param fileName jsonObj が記述してある .json ファイルの名前
   */
  private static void fillLabelParams(
      BhNodeViewStyle.Label textField,
      NativeObject jsonObj,
      String fileName) {

    //cssClass
    Optional<Object> val =
        readValue(BhConstants.NodeStyleDef.KEY_CSS_CLASS, String.class, jsonObj, fileName);
    val.ifPresent(textCssClass -> textField.cssClass = (String) textCssClass);
  }

  /**
   * {@link BhNodeViewStyle.ComboBox} にスタイル情報を格納する.
   *
   * @param comboBox jsonオブジェクトから読み取った内容を格納するオブジェクト
   * @param jsonObj key = "comboBox" の value であるオブジェクト
   * @param fileName jsonObj が記述してある .json ファイルの名前
   */
  private static void fillComboBoxParams(
      BhNodeViewStyle.ComboBox comboBox,
      NativeObject jsonObj,
      String fileName) {

    //cssClass
    Optional<Object> val =
        readValue(BhConstants.NodeStyleDef.KEY_CSS_CLASS, String.class, jsonObj, fileName);
    val.ifPresent(comboBoxCssClass -> comboBox.cssClass = (String) comboBoxCssClass);
  }

  /**
   * {@link BhNodeViewStyle.TextArea} にスタイル情報を格納する.
   *
   * @param textArea jsonオブジェクトから読み取った内容を格納するオブジェクト
   * @param jsonObj key = "textArea" の value であるオブジェクト
   * @param fileName jsonObj が記述してある .json ファイルの名前
   */
  private static void fillTextAreaParams(
      BhNodeViewStyle.TextArea textArea,
      NativeObject jsonObj,
      String fileName) {

    //cssClass
    Optional<Object> val =
        readValue(BhConstants.NodeStyleDef.KEY_CSS_CLASS, String.class, jsonObj, fileName);
    val.ifPresent(textAreaCssClass -> textArea.cssClass = (String) textAreaCssClass);

    //minWidth
    val = readValue(BhConstants.NodeStyleDef.KEY_MIN_WIDTH, Number.class, jsonObj, fileName);
    val.ifPresent(minWidth ->
        textArea.minWidth = ((Number) minWidth).doubleValue() * BhConstants.LnF.NODE_SCALE);

    //minHeight
    val = readValue(BhConstants.NodeStyleDef.KEY_MIN_HEIGHT, Number.class, jsonObj, fileName);
    val.ifPresent(minHeight ->
        textArea.minHeight = ((Number) minHeight).doubleValue() * BhConstants.LnF.NODE_SCALE);

    // editable
    val = readValue(BhConstants.NodeStyleDef.KEY_EDITABLE, Boolean.class, jsonObj, fileName);
    val.ifPresent(editable -> textArea.editable = ((Boolean) editable).booleanValue());
  }

  /**
   * {@code jsonObj} からエラーチェック付きで Value を読む.
   *
   * @param keyName keyの名前
   * @param valueType 想定される value の型 (String, Number, Boolean, NativeObject, ...)
   * @param jsonObj key と value が格納されているJson オブジェクト
   * @param fileName jsonObj を読み取ったファイルの名前
   * @return JsonValue オブジェクト (オプション)
   */
  private static Optional<Object> readValue(
      String keyName,
      Class<?> valueType,
      NativeObject jsonObj,
      String fileName) {

    Object val = jsonObj.get(keyName);
    if (val == null) {
      return Optional.empty();
    }

    if (!valueType.isAssignableFrom(val.getClass())) {
      MsgPrinter.INSTANCE.errMsgForDebug(
          "The type of '%s' must be %s.\nThe actual type is %s.  (%s)".formatted(
              keyName, valueType.getSimpleName(), val.getClass().getSimpleName(), fileName));
      return Optional.empty();
    }
    return Optional.of(val);
  }

  /**
   * ノード ID とノードスタイル ID のペアを登録する.
   *
   * @param nodeId ノードID (bhNodeID属性)
   * @param styleId ノードスタイルID (nodeStyleID属性)
   */
  public static void registerNodeIdAndStyleId(BhNodeId nodeId, String styleId) {
    nodeIdToNodeStyleID.put(nodeId, styleId);
  }

  /**
   * ノード ID から ノードスタイルオブジェクトを取得する.
   *
   * @param nodeId ノード ID
   * @return ノードスタイルオブジェクト
   */
  public static Optional<BhNodeViewStyle> getStyleFromNodeId(BhNodeId nodeId) {
    return Optional.ofNullable(nodeIdToNodeStyleID.getOrDefault(nodeId, null))
        .map(styleId -> nodeStyleIdToNodeStyleTemplate.getOrDefault(styleId, null))
        .map(nodeStyle -> new BhNodeViewStyle(nodeStyle));
  }

  /**
   * スタイル ID から ノードスタイルオブジェクトを取得する.
   *
   * @param styleId スタイル ID
   * @return ノードスタイルオブジェクト
   */
  public static Optional<BhNodeViewStyle> getStyleFromStyleId(String styleId) {
    return Optional.ofNullable(nodeStyleIdToNodeStyleTemplate.getOrDefault(styleId, null))
        .map(nodeStyle -> new BhNodeViewStyle(nodeStyle));
  }

  /**
   * 登録された全てのBhNodeIDに対してノードスタイルのテンプレートが存在するかどうかチェック.
   *
   * @return 登録された全てのBhNodeIDに対してノードスタイルのテンプレートが存在する場合 true
   */
  public static boolean checkNodeIdAndNodeTemplate() {
    boolean success = nodeIdToNodeStyleID.values().stream()
        .map(nodeStyleID -> nodeStyleIdToNodeStyleTemplate.containsKey(nodeStyleID))
        .allMatch(Boolean::valueOf);
    
    if (!success) {
      for (String nodeStyleId : nodeIdToNodeStyleID.values()) {
        if (!nodeStyleIdToNodeStyleTemplate.containsKey(nodeStyleId)) {
          MsgPrinter.INSTANCE.errMsgForDebug(
              "A node style file (%s) is not found among *.json files.".formatted(nodeStyleId));
        }
      }
    }
    return success;
  }
}
