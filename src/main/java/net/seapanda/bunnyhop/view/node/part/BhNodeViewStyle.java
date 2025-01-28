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

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
import java.io.FileReader;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;
import net.seapanda.bunnyhop.common.BhConstants;
import net.seapanda.bunnyhop.common.BhSettings;
import net.seapanda.bunnyhop.model.node.attribute.BhNodeId;
import net.seapanda.bunnyhop.service.BhService;
import net.seapanda.bunnyhop.utility.Utility;
import net.seapanda.bunnyhop.utility.Vec2D;
import net.seapanda.bunnyhop.view.bodyshape.BodyShapeBase;
import net.seapanda.bunnyhop.view.bodyshape.BodyShapeBase.BodyShape;
import net.seapanda.bunnyhop.view.connectorshape.ConnectorShape;
import net.seapanda.bunnyhop.view.connectorshape.ConnectorShape.CnctrShape;
import net.seapanda.bunnyhop.view.node.BhNodeView;
import net.seapanda.bunnyhop.view.node.ConnectiveNodeView;

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

  /** ノードスタイルのテンプレートを格納するハッシュ. xml の nodeStyleID がキー */
  private static final HashMap<String, BhNodeViewStyle> nodeStyleIdToNodeStyleTemplate = 
      new HashMap<>();
  /** ノードIDとノードスタイルのペアを格納するハッシュ. */
  private static final HashMap<BhNodeId, String> nodeIdToNodeStyleID = new HashMap<>();

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
        Utility.execPath,
        BhConstants.Path.VIEW_DIR,
        BhConstants.Path.NODE_STYLE_DEF_DIR,
        BhSettings.language);
    Stream<Path> paths = null; //読み込むファイルパスリスト
    try {
      paths = Files
          .walk(dirPath, FOLLOW_LINKS)
          .filter(path -> path.getFileName().toString().endsWith(".json"));
    } catch (IOException e) {
      BhService.msgPrinter().errForDebug("Directory not found.  (%s)".formatted(dirPath));
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
    var gson = new Gson();
    try (var jr = gson.newJsonReader(new FileReader(filePath.toString()))) {
      JsonObject jsonObj = gson.fromJson(jr, JsonObject.class);
      String styleId = filePath.getFileName().toString();
      Optional<BhNodeViewStyle> bhNodeViewStyle =
          genBhNodeViewStyle(jsonObj, filePath.toAbsolutePath().toString(), styleId);
      bhNodeViewStyle.ifPresent(
          viewStyle -> nodeStyleIdToNodeStyleTemplate.put(viewStyle.nodeStyleId, viewStyle));
      return bhNodeViewStyle.isPresent();
    } catch (Exception e) {
      BhService.msgPrinter().errForDebug(e.toString());
      return false;
    }
  }

  /**
   * jsonオブジェクト から BhNodeViewStyle オブジェクトを作成する.
   *
   * @param jsonObj .JSON ファイルを読み込んで作ったトップレベルオブジェクト
   * @param fileName jsonObj が記述してある .JSON ファイルのファイル名
   * @return BhNodeViewStyle (オプション)
   */
  private static Optional<BhNodeViewStyle> genBhNodeViewStyle(
      JsonObject jsonObj, String fileName, String styleId) {
    BhNodeViewStyle style = new BhNodeViewStyle();

    // styleID
    style.nodeStyleId = styleId;

    // paddingTop
    style.paddingTop = readNumber(BhConstants.NodeStyleDef.KEY_PADDING_TOP, jsonObj, fileName)
        .map(val -> val.doubleValue() * BhConstants.LnF.NODE_SCALE).orElse(style.paddingTop);

    // paddingBottom
    style.paddingBottom = readNumber(BhConstants.NodeStyleDef.KEY_PADDING_BOTTOM, jsonObj, fileName)
        .map(val -> val.doubleValue() * BhConstants.LnF.NODE_SCALE).orElse(style.paddingBottom);

    // paddingLeft
    style.paddingLeft = readNumber(BhConstants.NodeStyleDef.KEY_PADDING_LEFT, jsonObj, fileName)
        .map(val -> val.doubleValue() * BhConstants.LnF.NODE_SCALE).orElse(style.paddingLeft);

    // paddingRight
    style.paddingRight = readNumber(BhConstants.NodeStyleDef.KEY_PADDING_RIGHT, jsonObj, fileName)
        .map(val -> val.doubleValue() * BhConstants.LnF.NODE_SCALE).orElse(style.paddingRight);

    // bodyShape
    style.bodyShape = readString(BhConstants.NodeStyleDef.KEY_BODY_SHAPE, jsonObj, fileName)
        .map(val -> BodyShapeBase.getBodyTypeFromName(val, fileName)).orElse(style.bodyShape);

    // connectorPos
    style.connectorPos = readString(BhConstants.NodeStyleDef.KEY_CONNECTOR_POS, jsonObj, fileName)
        .map(val -> ConnectorPos.of(val)).orElse(style.connectorPos);

    // connectorShift
    style.connectorShift =
        readNumber(BhConstants.NodeStyleDef.KEY_CONNECTOR_SHIFT, jsonObj, fileName)
        .map(val -> val.doubleValue() * BhConstants.LnF.NODE_SCALE).orElse(style.connectorShift);

    // connectorWidth
    style.connectorWidth =
        readNumber(BhConstants.NodeStyleDef.KEY_CONNECTOR_WIDTH, jsonObj, fileName)
        .map(val -> val.doubleValue() * BhConstants.LnF.NODE_SCALE).orElse(style.connectorWidth);

    // connectorHeight
    style.connectorHeight =
        readNumber(BhConstants.NodeStyleDef.KEY_CONNECTOR_HEIGHT, jsonObj, fileName)
        .map(val -> val.doubleValue() * BhConstants.LnF.NODE_SCALE).orElse(style.connectorHeight);

    // connectorShape
    style.connectorShape =
        readString(BhConstants.NodeStyleDef.KEY_CONNECTOR_SHAPE, jsonObj, fileName)
        .map(val -> ConnectorShape.getConnectorTypeFromName(val, fileName))
        .orElse(style.connectorShape);

    // connectorShapeFixed
    style.connectorShapeFixed =
        readString(BhConstants.NodeStyleDef.KEY_CONNECTOR_SHAPE_FIXED, jsonObj, fileName)
        .map(val -> ConnectorShape.getConnectorTypeFromName(val, fileName))
        .orElse(style.connectorShape);  // fixed の場合の設定が存在しない場合は, 非 fixed の設定と同じにする.

    // notchPos
    style.notchPos = readString(BhConstants.NodeStyleDef.KEY_NOTCH_POS, jsonObj, fileName)
        .map(val -> NotchPos.of(val)).orElse(style.notchPos);

    // notchWidth
    style.notchWidth = readNumber(BhConstants.NodeStyleDef.KEY_NOTCH_WIDTH, jsonObj, fileName)
        .map(val -> val.doubleValue() * BhConstants.LnF.NODE_SCALE).orElse(style.notchWidth);

    // notchHeight
    style.notchHeight = readNumber(BhConstants.NodeStyleDef.KEY_NOTCH_HEIGHT, jsonObj, fileName)
        .map(val -> val.doubleValue() * BhConstants.LnF.NODE_SCALE).orElse(style.notchHeight);

    // notchShape
    style.notchShape =
        readString(BhConstants.NodeStyleDef.KEY_NOTCH_SHAPE, jsonObj, fileName)
        .map(val -> ConnectorShape.getConnectorTypeFromName(val, fileName))
        .orElse(style.notchShape);

    // notchShapeFixed
    style.notchShapeFixed =
        readString(BhConstants.NodeStyleDef.KEY_NOTCH_SHAPE_FIXED, jsonObj, fileName)
        .map(val -> ConnectorShape.getConnectorTypeFromName(val, fileName))
        .orElse(style.notchShape); // fixed の場合の設定が存在しない場合は, 非 fixed の設定と同じにする.

    // connectorBoundsRate
    style.connectorBoundsRate =
        readNumber(BhConstants.NodeStyleDef.KEY_CONNECTOR_BOUNDS_RATE, jsonObj, fileName)
        .map(val -> val.doubleValue()).orElse(style.connectorBoundsRate);

    // bodyCssClass
    style.cssClass = readString(BhConstants.NodeStyleDef.KEY_CSS_CLASS, jsonObj, fileName)
        .orElse(style.cssClass);
    
    // connective
    readObject(BhConstants.NodeStyleDef.KEY_CONNECTIVE, jsonObj, fileName)
        .ifPresent(obj -> fillConnectiveParams(style.connective, obj, fileName));

    // textField
    readObject(BhConstants.NodeStyleDef.KEY_TEXT_FIELD, jsonObj, fileName)
        .ifPresent(obj -> fillTextFieldParams(style.textField, obj, fileName));

    // label
    readObject(BhConstants.NodeStyleDef.KEY_LABEL, jsonObj, fileName)
        .ifPresent(obj -> fillLabelParams(style.label, obj, fileName));

    // comboBox
    readObject(BhConstants.NodeStyleDef.KEY_COMBO_BOX, jsonObj, fileName)
        .ifPresent(obj -> fillComboBoxParams(style.comboBox, obj, fileName));

    // textArea
    readObject(BhConstants.NodeStyleDef.KEY_TEXT_AREA, jsonObj, fileName)
        .ifPresent(obj -> fillTextAreaParams(style.textArea, obj, fileName));

    // privateTemplate
    readObject(BhConstants.NodeStyleDef.KEY_PRIVATE_TEMPLATE, jsonObj, fileName)
        .ifPresent(obj -> fillButtonParams(style.privatTemplate, obj, fileName));

    // component
    style.component = readString(BhConstants.NodeStyleDef.KEY_COMPONENT, jsonObj, fileName)
        .map(val -> ComponentType.of(val)).orElse(style.component);
    
    return Optional.of(style);
  }

  /**
   * {@link BhNodeViewStyle.Connective} にスタイル情報を格納する.
   *
   * @param jsonObj key = "connective" の value であるオブジェクト
   * @param fileName jsonObj が記述してある .JSON ファイルの名前
   */
  private static void fillConnectiveParams(
      BhNodeViewStyle.Connective connectiveStyle, JsonObject jsonObj, String fileName) {
    // inner
    readObject(BhConstants.NodeStyleDef.KEY_INNER, jsonObj, fileName)
        .ifPresent(obj -> fillArrangementParams(connectiveStyle.inner, obj, fileName));

    // outer
    readObject(BhConstants.NodeStyleDef.KEY_OUTER, jsonObj, fileName)
        .ifPresent(obj -> fillArrangementParams(connectiveStyle.outer, obj, fileName));
  }

  /**
   * {@link BhNodeViewStyle.Arrangement} にスタイル情報を格納する.
   *
   * @param arrangement 並べ方に関するパラメータの格納先
   * @param jsonObj key = "inner" または "outer" の value であるオブジェクト
   * @param fileName jsonObj が記述してある .JSON ファイルの名前
   */
  private static void fillArrangementParams(
      BhNodeViewStyle.Arrangement arrangement, JsonObject jsonObj, String fileName) {
    // space
    arrangement.space = readNumber(BhConstants.NodeStyleDef.KEY_SPACE, jsonObj, fileName)
        .map(val -> val.doubleValue() * BhConstants.LnF.NODE_SCALE).orElse(arrangement.space);

    // paddingTop
    arrangement.paddingTop = readNumber(BhConstants.NodeStyleDef.KEY_PADDING_TOP, jsonObj, fileName)
        .map(val -> val.doubleValue() * BhConstants.LnF.NODE_SCALE).orElse(arrangement.paddingTop);

    // paddingRight
    arrangement.paddingRight =
        readNumber(BhConstants.NodeStyleDef.KEY_PADDING_RIGHT, jsonObj, fileName)
        .map(val -> val.doubleValue() * BhConstants.LnF.NODE_SCALE)
        .orElse(arrangement.paddingRight);

    // paddingBottom
    arrangement.paddingBottom =
        readNumber(BhConstants.NodeStyleDef.KEY_PADDING_BOTTOM, jsonObj, fileName)
        .map(val -> val.doubleValue() * BhConstants.LnF.NODE_SCALE)
        .orElse(arrangement.paddingBottom);

    // paddingLeft
    arrangement.paddingLeft =
        readNumber(BhConstants.NodeStyleDef.KEY_PADDING_LEFT, jsonObj, fileName)
        .map(val -> val.doubleValue() * BhConstants.LnF.NODE_SCALE)
        .orElse(arrangement.paddingLeft);

    // arrangement
    arrangement.arrangement =
        readString(BhConstants.NodeStyleDef.KEY_ARRANGEMENT, jsonObj, fileName)
        .map(val -> ChildArrangement.of(val)).orElse(arrangement.arrangement);

    // cnctrNameList
    readArray(BhConstants.NodeStyleDef.KEY_CONNECTOR_LIST, jsonObj, fileName)
        .ifPresent(vals ->
            vals.asList().stream()
                .filter(val -> val.isJsonPrimitive() && val.getAsJsonPrimitive().isString())
                .map(val -> val.getAsJsonPrimitive().getAsString())
                .forEach(val -> arrangement.cnctrNameList.add(val)));

    // subGroup
    int groupId = 0;
    while (true) {
      String subGroupKeyName = BhConstants.NodeStyleDef.KEY_SUB_GROUP + groupId;
      Optional<JsonObject> obj = readObject(subGroupKeyName, jsonObj, fileName);
      if (obj.isEmpty()) {
        break;
      }
      Arrangement subGroup = new Arrangement();
      fillArrangementParams(subGroup, obj.get(), fileName);
      arrangement.subGroups.add(subGroup);      
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
      BhNodeViewStyle.Button button, JsonObject jsonObj, String fileName) {
    // buttonPosX
    button.buttonPosX = readNumber(BhConstants.NodeStyleDef.KEY_BUTTON_POS_X, jsonObj, fileName)
        .map(val -> val.doubleValue() * BhConstants.LnF.NODE_SCALE).orElse(button.buttonPosX);

    // buttonPosY
    button.buttonPosY = readNumber(BhConstants.NodeStyleDef.KEY_BUTTON_POS_Y, jsonObj, fileName)
        .map(val -> val.doubleValue() * BhConstants.LnF.NODE_SCALE).orElse(button.buttonPosY);

    // buttonCssClass
    button.cssClass = readString(BhConstants.NodeStyleDef.KEY_CSS_CLASS, jsonObj, fileName)
        .orElse(button.cssClass);
  }

  /**
   * {@link BhNodeViewStyle.TextField} にスタイル情報を格納する.
   *
   * @param textField jsonオブジェクトから読み取った内容を格納するオブジェクト
   * @param jsonObj key = "textField" の value であるオブジェクト
   * @param fileName jsonObj が記述してある .json ファイルの名前
   */
  private static void fillTextFieldParams(
      BhNodeViewStyle.TextField textField, JsonObject jsonObj, String fileName) {
    // cssClass
    textField.cssClass = readString(BhConstants.NodeStyleDef.KEY_CSS_CLASS, jsonObj, fileName)
          .orElse(textField.cssClass);

    // minWidth
    textField.minWidth = readNumber(BhConstants.NodeStyleDef.KEY_MIN_WIDTH, jsonObj, fileName)
        .map(val -> val.doubleValue() * BhConstants.LnF.NODE_SCALE).orElse(textField.minWidth);
        
    // editable
    textField.editable = readBool(BhConstants.NodeStyleDef.KEY_EDITABLE, jsonObj, fileName)
        .orElse(textField.editable);
  }

  /**
   * {@link BhNodeViewStyle.Label} にスタイル情報を格納する.
   *
   * @param label jsonオブジェクトから読み取った内容を格納するオブジェクト
   * @param jsonObj key = "label" の value であるオブジェクト
   * @param fileName jsonObj が記述してある .json ファイルの名前
   */
  private static void fillLabelParams(
      BhNodeViewStyle.Label label, JsonObject jsonObj, String fileName) {
    // cssClass
    label.cssClass = readString(BhConstants.NodeStyleDef.KEY_CSS_CLASS, jsonObj, fileName)
        .orElse(label.cssClass);
  }

  /**
   * {@link BhNodeViewStyle.ComboBox} にスタイル情報を格納する.
   *
   * @param comboBox jsonオブジェクトから読み取った内容を格納するオブジェクト
   * @param jsonObj key = "comboBox" の value であるオブジェクト
   * @param fileName jsonObj が記述してある .json ファイルの名前
   */
  private static void fillComboBoxParams(
      BhNodeViewStyle.ComboBox comboBox, JsonObject jsonObj, String fileName) {
    // cssClass
    comboBox.cssClass = readString(BhConstants.NodeStyleDef.KEY_CSS_CLASS, jsonObj, fileName)
        .orElse(comboBox.cssClass);
  }

  /**
   * {@link BhNodeViewStyle.TextArea} にスタイル情報を格納する.
   *
   * @param textArea jsonオブジェクトから読み取った内容を格納するオブジェクト
   * @param jsonObj key = "textArea" の value であるオブジェクト
   * @param fileName jsonObj が記述してある .json ファイルの名前
   */
  private static void fillTextAreaParams(
      BhNodeViewStyle.TextArea textArea, JsonObject jsonObj, String fileName) {
    // cssClass
    textArea.cssClass = readString(BhConstants.NodeStyleDef.KEY_CSS_CLASS, jsonObj, fileName)
        .orElse(textArea.cssClass);

    // minWidth
    textArea.minWidth = readNumber(BhConstants.NodeStyleDef.KEY_MIN_WIDTH, jsonObj, fileName)
        .map(val -> val.doubleValue() * BhConstants.LnF.NODE_SCALE).orElse(textArea.minWidth);

    // minHeight
    textArea.minHeight = readNumber(BhConstants.NodeStyleDef.KEY_MIN_HEIGHT, jsonObj, fileName)
        .map(val -> val.doubleValue() * BhConstants.LnF.NODE_SCALE).orElse(textArea.minHeight);

    // editable
    textArea.editable = readBool(BhConstants.NodeStyleDef.KEY_EDITABLE, jsonObj, fileName)
        .orElse(textArea.editable);
  }

  /**
   * {@code jsonObj} の {@code keyName} の値を {@link Number} 型として読む.
   *
   * @param key このキーの値を {@code jsonObj} から読む
   * @param jsonObj この JSON オブジェクトから {@code key} の値を読む
   * @param fileName jsonObj を読み取ったファイルの名前
   * @return Number オブジェクト. 読めなかった場合は empty を返す.
   */
  private static Optional<Number> readNumber(String key, JsonObject jsonObj, String fileName) {
    JsonElement elem = jsonObj.get(key);
    if (elem == null) {
      return Optional.empty();
    }
    if (!elem.isJsonPrimitive()) {
      BhService.msgPrinter().errForDebug(String.format(
          "The type of '%s' must be Number. (%s)\n  %s", key, fileName, elem.toString()));
      return Optional.empty();
    }
    JsonPrimitive primitive = elem.getAsJsonPrimitive();
    if (!primitive.isNumber()) {
      BhService.msgPrinter().errForDebug(String.format(
          "The type of '%s' must be Number. (%s)\n  %s", key, fileName, primitive.toString()));
      return Optional.empty();
    }
    return Optional.of(primitive.getAsNumber());
  }

  /**
   * {@code jsonObj} の {@code keyName} の値を {@link String} 型として読む.
   *
   * @param key このキーの値を {@code jsonObj} から読む
   * @param jsonObj この JSON オブジェクトから {@code key} の値を読む
   * @param fileName jsonObj を読み取ったファイルの名前
   * @return String オブジェクト. 読めなかった場合は empty を返す.
   */
  private static Optional<String> readString(String key, JsonObject jsonObj, String fileName) {
    JsonElement elem = jsonObj.get(key);
    if (elem == null) {
      return Optional.empty();
    }
    if (!elem.isJsonPrimitive()) {
      BhService.msgPrinter().errForDebug(String.format(
          "The type of '%s' must be String. (%s)\n  %s", key, fileName, elem.toString()));
      return Optional.empty();
    }
    JsonPrimitive primitive = elem.getAsJsonPrimitive();
    if (!primitive.isString()) {
      BhService.msgPrinter().errForDebug(String.format(
          "The type of '%s' must be String. (%s)\n  %s", key, fileName, primitive.toString()));
      return Optional.empty();
    }
    return Optional.of(primitive.getAsString());
  }

  /**
   * {@code jsonObj} の {@code keyName} の値を {@link Boolean} 型として読む.
   *
   * @param key このキーの値を {@code jsonObj} から読む
   * @param jsonObj この JSON オブジェクトから {@code key} の値を読む
   * @param fileName jsonObj を読み取ったファイルの名前
   * @return Boolean オブジェクト. 読めなかった場合は empty を返す.
   */
  private static Optional<Boolean> readBool(String key, JsonObject jsonObj, String fileName) {
    JsonElement elem = jsonObj.get(key);
    if (elem == null) {
      return Optional.empty();
    }
    if (!elem.isJsonPrimitive()) {
      BhService.msgPrinter().errForDebug(String.format(
          "The type of '%s' must be Boolean. (%s)\n  %s", key, fileName, elem.toString()));
      return Optional.empty();
    }
    JsonPrimitive primitive = elem.getAsJsonPrimitive();
    if (!primitive.isBoolean()) {
      BhService.msgPrinter().errForDebug(String.format(
          "The type of '%s' must be Boolean. (%s)\n  %s", key, fileName, primitive.toString()));
      return Optional.empty();
    }
    return Optional.of(primitive.getAsBoolean());
  }

  /**
   * {@code jsonObj} の {@code keyName} の値を {@link JsonArray} 型として読む.
   *
   * @param key このキーの値を {@code jsonObj} から読む
   * @param jsonObj この JSON オブジェクトから {@code key} の値を読む
   * @param fileName jsonObj を読み取ったファイルの名前
   * @return JsonArray オブジェクト. 読めなかった場合は empty を返す.
   */
  private static Optional<JsonArray> readArray(String key, JsonObject jsonObj, String fileName) {
    JsonElement elem = jsonObj.get(key);
    if (elem == null) {
      return Optional.empty();
    }
    if (!elem.isJsonArray()) {
      BhService.msgPrinter().errForDebug(String.format(
          "The type of '%s' must be Array. (%s)\n  %s", key, fileName, elem.toString()));
      return Optional.empty();
    }
    return Optional.of(elem.getAsJsonArray());
  }

  /**
   * {@code jsonObj} の {@code keyName} の値を {@link JsonArray} 型として読む.
   *
   * @param key このキーの値を {@code jsonObj} から読む
   * @param jsonObj この JSON オブジェクトから {@code key} の値を読む
   * @param fileName jsonObj を読み取ったファイルの名前
   * @return JsonArray オブジェクト. 読めなかった場合は empty を返す.
   */
  private static Optional<JsonObject> readObject(String key, JsonObject jsonObj, String fileName) {
    JsonElement elem = jsonObj.get(key);
    if (elem == null) {
      return Optional.empty();
    }
    if (!elem.isJsonObject()) {
      BhService.msgPrinter().errForDebug(String.format(
          "The type of '%s' must be Object. (%s)\n  %s", key, fileName, elem.toString()));
      return Optional.empty();
    }
    return Optional.of(elem.getAsJsonObject());
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
          BhService.msgPrinter().errForDebug(
              "A node style file (%s) is not found among *.json files.".formatted(nodeStyleId));
        }
      }
    }
    return success;
  }
}
