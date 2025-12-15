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
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import net.seapanda.bunnyhop.common.configuration.BhConstants;
import net.seapanda.bunnyhop.node.model.parameter.BhNodeViewStyleId;
import net.seapanda.bunnyhop.node.view.bodyshape.BodyShapeBase;
import net.seapanda.bunnyhop.node.view.component.ComponentType;
import net.seapanda.bunnyhop.node.view.connectorshape.ConnectorShape;
import net.seapanda.bunnyhop.node.view.style.BhNodeViewStyle.Arrangement;
import net.seapanda.bunnyhop.node.view.style.BhNodeViewStyle.ChildArrangement;
import net.seapanda.bunnyhop.node.view.style.BhNodeViewStyle.ConnectorAlignment;
import net.seapanda.bunnyhop.node.view.style.BhNodeViewStyle.ConnectorPos;
import net.seapanda.bunnyhop.node.view.style.BhNodeViewStyle.NotchPos;
import net.seapanda.bunnyhop.ui.view.ViewConstructionException;

/**
 * {@link BhNodeViewStyle} を作成する機能を提供するクラス.
 *
 * @author K.Koike
 */
public class JsonBhNodeViewStyleFactory implements BhNodeViewStyleFactory {

  /** ノードスタイルのテンプレートを格納するハッシュ. xml の nodeStyleID がキー */
  private final Map<BhNodeViewStyleId, BhNodeViewStyle> styleIdToStyle = new HashMap<>();

  /**
   * 引数で指定したディレクトリ以下の json ファイルからノードスタイルを作成し保持する.
   *
   * @param dirPath このディレクトリ以下からノードスタイルの定義ファイルを探す.
   */
  public JsonBhNodeViewStyleFactory(Path dirPath) throws ViewConstructionException {
    try {
      styleIdToStyle.put(
          BhNodeViewStyleId.of(BhConstants.BhModelDef.ATTR_VAL_DEFAULT_NODE_STYLE_ID),
          new BhNodeViewStyle());
      List<Path> paths = Files
          .walk(dirPath, FOLLOW_LINKS)
          .filter(path -> path.getFileName().toString().endsWith(".json"))
          .toList();
      for (Path filePath : paths) {
        registerNodeStyle(filePath);
      }
    } catch (ViewConstructionException e) {
      throw e;
    } catch (IOException e) {
      throw new ViewConstructionException("Directory not found.  (%s)\n %s".formatted(dirPath, e));
    }
  }

  /**
   * 引数で指定したファイルからノードスタイルを読み取って {@link #styleIdToStyle} に格納する.
   *
   * @param filePath ノードスタイルファイルのパス
   */
  private void registerNodeStyle(Path filePath) throws ViewConstructionException {
    var gson = new Gson();
    try (var jr = gson.newJsonReader(new FileReader(filePath.toString()))) {
      JsonObject jsonObj = gson.fromJson(jr, JsonObject.class);
      BhNodeViewStyleId styleId = BhNodeViewStyleId.of(filePath.getFileName().toString());
      BhNodeViewStyle style =
          genBhNodeViewStyle(jsonObj, filePath.toAbsolutePath().toString(), styleId);
      styleIdToStyle.put(style.id, style);
    } catch (ViewConstructionException e) {
      throw e;
    } catch (Exception e) {
      throw new ViewConstructionException(e.toString());
    }
  }
  
  /**
   * jsonオブジェクト から {@link BhNodeViewStyle} オブジェクトを作成する.
   *
   * @param jsonObj この JSON オブジェクトからスタイル情報を取得する
   * @param fileName {@code jsonObj} が記述してある .JSON ファイルのファイル名
   * @return 作成した {@link BhNodeViewStyle} オブジェクト
   */
  private BhNodeViewStyle genBhNodeViewStyle(
      JsonObject jsonObj, String fileName, BhNodeViewStyleId styleId)
      throws ViewConstructionException {
    BhNodeViewStyle style = new BhNodeViewStyle();

    // styleID
    style.id = styleId;

    // paddingTop
    style.paddingTop = readNumber(BhConstants.NodeStyleDef.KEY_PADDING_TOP, jsonObj, fileName)
        .map(val -> val.doubleValue() * BhConstants.Ui.NODE_SCALE).orElse(style.paddingTop);

    // paddingBottom
    style.paddingBottom = readNumber(BhConstants.NodeStyleDef.KEY_PADDING_BOTTOM, jsonObj, fileName)
        .map(val -> val.doubleValue() * BhConstants.Ui.NODE_SCALE).orElse(style.paddingBottom);

    // paddingLeft
    style.paddingLeft = readNumber(BhConstants.NodeStyleDef.KEY_PADDING_LEFT, jsonObj, fileName)
        .map(val -> val.doubleValue() * BhConstants.Ui.NODE_SCALE).orElse(style.paddingLeft);

    // paddingRight
    style.paddingRight = readNumber(BhConstants.NodeStyleDef.KEY_PADDING_RIGHT, jsonObj, fileName)
        .map(val -> val.doubleValue() * BhConstants.Ui.NODE_SCALE).orElse(style.paddingRight);

    // bodyShape
    style.bodyShape = readString(BhConstants.NodeStyleDef.KEY_BODY_SHAPE, jsonObj, fileName)
        .map(val -> BodyShapeBase.getBodyTypeFromName(val, fileName)).orElse(style.bodyShape);

    // connectorPos
    style.connectorPos = readString(BhConstants.NodeStyleDef.KEY_CONNECTOR_POS, jsonObj, fileName)
        .map(ConnectorPos::of).orElse(style.connectorPos);

    // connectorShift
    style.connectorShift =
        readNumber(BhConstants.NodeStyleDef.KEY_CONNECTOR_SHIFT, jsonObj, fileName)
        .map(val -> val.doubleValue() * BhConstants.Ui.NODE_SCALE).orElse(style.connectorShift);

    // connectorShift
    style.connectorAlignment =
        readString(BhConstants.NodeStyleDef.KEY_CONNECTOR_ALIGNMENT, jsonObj, fileName)
        .map(ConnectorAlignment::of).orElse(style.connectorAlignment);

    // connectorWidth
    style.connectorWidth =
        readNumber(BhConstants.NodeStyleDef.KEY_CONNECTOR_WIDTH, jsonObj, fileName)
        .map(val -> val.doubleValue() * BhConstants.Ui.NODE_SCALE).orElse(style.connectorWidth);

    // connectorHeight
    style.connectorHeight =
        readNumber(BhConstants.NodeStyleDef.KEY_CONNECTOR_HEIGHT, jsonObj, fileName)
        .map(val -> val.doubleValue() * BhConstants.Ui.NODE_SCALE).orElse(style.connectorHeight);

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
        .map(NotchPos::of).orElse(style.notchPos);

    // notchWidth
    style.notchWidth = readNumber(BhConstants.NodeStyleDef.KEY_NOTCH_WIDTH, jsonObj, fileName)
        .map(val -> val.doubleValue() * BhConstants.Ui.NODE_SCALE).orElse(style.notchWidth);

    // notchHeight
    style.notchHeight = readNumber(BhConstants.NodeStyleDef.KEY_NOTCH_HEIGHT, jsonObj, fileName)
        .map(val -> val.doubleValue() * BhConstants.Ui.NODE_SCALE).orElse(style.notchHeight);

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
        .map(Number::doubleValue).orElse(style.connectorBoundsRate);

    // cssClass
    style.cssClasses = readString(BhConstants.NodeStyleDef.KEY_CSS_CLASS, jsonObj, fileName)
        .map(clz -> clz.split(","))
        .orElse(style.cssClasses);
    
    // component
    style.component = readString(BhConstants.NodeStyleDef.KEY_COMPONENT, jsonObj, fileName)
        .map(ComponentType::of).orElse(style.component);

    // baseArrangement
    style.baseArrangement = 
        readString(BhConstants.NodeStyleDef.KEY_BASE_ARRANGEMENT, jsonObj, fileName)
        .map(ChildArrangement::of).orElse(style.baseArrangement);

    // connective
    JsonObject obj =
        readObject(BhConstants.NodeStyleDef.KEY_CONNECTIVE, jsonObj, fileName).orElse(null);
    if (obj != null) {
      fillConnectiveParams(style.connective, obj, fileName);
    }

    // commonPart
    obj = readObject(BhConstants.NodeStyleDef.KEY_COMMON_PART, jsonObj, fileName).orElse(null);
    if (obj != null) {
      fillCommonPartParams(style.commonPart, obj, fileName);
    }

    // specificPart
    obj = readObject(BhConstants.NodeStyleDef.KEY_SPECIFIC_PART, jsonObj, fileName).orElse(null);
    if (obj != null) {
      fillSpecificPartParams(style.specificPart, obj, fileName);
    }

    // textField
    obj = readObject(BhConstants.NodeStyleDef.KEY_TEXT_FIELD, jsonObj, fileName).orElse(null);
    if (obj != null) {
      fillTextFieldParams(style.textField, obj, fileName);
    }

    // label
    obj = readObject(BhConstants.NodeStyleDef.KEY_LABEL, jsonObj, fileName).orElse(null);
    if (obj != null) {
      fillLabelParams(style.label, obj, fileName);
    }

    // comboBox
    obj = readObject(BhConstants.NodeStyleDef.KEY_COMBO_BOX, jsonObj, fileName).orElse(null);
    if (obj != null) {
      fillComboBoxParams(style.comboBox, obj, fileName);
    }

    // textArea
    obj = readObject(BhConstants.NodeStyleDef.KEY_TEXT_AREA, jsonObj, fileName).orElse(null);
    if (obj != null) {
      fillTextAreaParams(style.textArea, obj, fileName);
    }

    // component
    style.component = readString(BhConstants.NodeStyleDef.KEY_COMPONENT, jsonObj, fileName)
        .map(ComponentType::of).orElse(style.component);

    // baseArrangement
    style.baseArrangement = 
        readString(BhConstants.NodeStyleDef.KEY_BASE_ARRANGEMENT, jsonObj, fileName)
        .map(ChildArrangement::of).orElse(style.baseArrangement);

    return style;
  }

  /**
   * {@link BhNodeViewStyle.Connective} にスタイル情報を格納する.
   *
   * @param jsonObj この JSON オブジェクトからスタイル情報を取得する
   * @param fileName {@code jsonObj} が記述してある .JSON ファイルの名前
   */
  private void fillConnectiveParams(
      BhNodeViewStyle.Connective connectiveStyle, JsonObject jsonObj, String fileName)
      throws ViewConstructionException {
    // inner
    JsonObject obj = readObject(BhConstants.NodeStyleDef.KEY_INNER, jsonObj, fileName).orElse(null);
    if (obj != null) {
      fillArrangementParams(connectiveStyle.inner, obj, fileName);
    }

    // outer
    obj = readObject(BhConstants.NodeStyleDef.KEY_OUTER, jsonObj, fileName).orElse(null);
    if (obj != null) {
      fillArrangementParams(connectiveStyle.outer, obj, fileName);
    }
  }


  /**
   * {@link BhNodeViewStyle.Arrangement} にスタイル情報を格納する.
   *
   * @param arrangement 並べ方に関するパラメータの格納先
   * @param jsonObj この JSON オブジェクトからスタイル情報を取得する
   * @param fileName {@code jsonObj} が記述してある .JSON ファイルの名前
   */
  private void fillArrangementParams(
      BhNodeViewStyle.Arrangement arrangement, JsonObject jsonObj, String fileName)
      throws ViewConstructionException {
    // space
    arrangement.space = readNumber(BhConstants.NodeStyleDef.KEY_SPACE, jsonObj, fileName)
        .map(val -> val.doubleValue() * BhConstants.Ui.NODE_SCALE).orElse(arrangement.space);

    // paddingTop
    arrangement.paddingTop = readNumber(BhConstants.NodeStyleDef.KEY_PADDING_TOP, jsonObj, fileName)
        .map(val -> val.doubleValue() * BhConstants.Ui.NODE_SCALE).orElse(arrangement.paddingTop);

    // paddingRight
    arrangement.paddingRight =
        readNumber(BhConstants.NodeStyleDef.KEY_PADDING_RIGHT, jsonObj, fileName)
        .map(val -> val.doubleValue() * BhConstants.Ui.NODE_SCALE)
        .orElse(arrangement.paddingRight);

    // paddingBottom
    arrangement.paddingBottom =
        readNumber(BhConstants.NodeStyleDef.KEY_PADDING_BOTTOM, jsonObj, fileName)
        .map(val -> val.doubleValue() * BhConstants.Ui.NODE_SCALE)
        .orElse(arrangement.paddingBottom);

    // paddingLeft
    arrangement.paddingLeft =
        readNumber(BhConstants.NodeStyleDef.KEY_PADDING_LEFT, jsonObj, fileName)
        .map(val -> val.doubleValue() * BhConstants.Ui.NODE_SCALE)
        .orElse(arrangement.paddingLeft);

    // arrangement
    arrangement.arrangement =
        readString(BhConstants.NodeStyleDef.KEY_ARRANGEMENT, jsonObj, fileName)
        .map(ChildArrangement::of).orElse(arrangement.arrangement);

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
   * {@link BhNodeViewStyle.CommonPart} にスタイル情報を格納する.
   *
   * @param commonPart パラメータの格納先
   * @param jsonObj この JSON オブジェクトからスタイル情報を取得する
   * @param fileName {@code jsonObj} が記述してある .JSON ファイルの名前
   */
  private void fillCommonPartParams(
      BhNodeViewStyle.CommonPart commonPart, JsonObject jsonObj, String fileName)
      throws ViewConstructionException {
    // cssClass
    commonPart.cssClass = readString(BhConstants.NodeStyleDef.KEY_CSS_CLASS, jsonObj, fileName)
        .orElse(commonPart.cssClass);
    
    // arrangement
    commonPart.arrangement = 
        readString(BhConstants.NodeStyleDef.KEY_ARRANGEMENT, jsonObj, fileName)
        .map(ChildArrangement::of).orElse(commonPart.arrangement);
    
    // privateTemplate
    JsonObject obj =
        readObject(BhConstants.NodeStyleDef.KEY_PRIVATE_TEMPLATE, jsonObj, fileName).orElse(null);
    if (obj != null) {
      fillButtonParams(commonPart.privateTemplate, obj, fileName);
    }

    // breakpoint
    obj = readObject(BhConstants.NodeStyleDef.KEY_BREAK_POINT, jsonObj, fileName).orElse(null);
    if (obj != null) {
      fillBreakpointParams(commonPart.breakpoint, obj, fileName);
    }

    // execStepMark
    obj = readObject(BhConstants.NodeStyleDef.KEY_EXEC_STEP, jsonObj, fileName).orElse(null);
    if (obj != null) {
      fillExecStepMarkParams(commonPart.execStepMark, obj, fileName);
    }

    // corruptionMark
    obj = readObject(BhConstants.NodeStyleDef.KEY_CORRUPTION, jsonObj, fileName).orElse(null);
    if (obj != null) {
      fillCorruptionMarkParams(commonPart.corruptionMark, obj, fileName);
    }

    // entryPointMark
    obj = readObject(BhConstants.NodeStyleDef.KEY_ENTRY_POINT, jsonObj, fileName).orElse(null);
    if (obj != null) {
      fillEntryPointMarkParams(commonPart.entryPointMark, obj, fileName);
    }
  }

  /**
   * {@link BhNodeViewStyle.SpecificPart} にスタイル情報を格納する.
   *
   * @param specificPart パラメータの格納先
   * @param jsonObj この JSON オブジェクトからスタイル情報を取得する
   * @param fileName {@code jsonObj} が記述してある .JSON ファイルの名前
   */
  private void fillSpecificPartParams(
      BhNodeViewStyle.SpecificPart specificPart, JsonObject jsonObj, String fileName)
      throws ViewConstructionException {
    // cssClass
    specificPart.cssClass = readString(BhConstants.NodeStyleDef.KEY_CSS_CLASS, jsonObj, fileName)
        .orElse(specificPart.cssClass);
  }

  /**
   * {@link BhNodeViewStyle.Button} にスタイル情報を格納する.
   *
   * @param button jsonObj の情報を格納するオブジェクト
   * @param jsonObj ボタンのパラメータが格納されたオブジェクト
   * @param fileName jsonObj が記述してある .json ファイルの名前
   */
  private void fillButtonParams(
      BhNodeViewStyle.Button button, JsonObject jsonObj, String fileName)
      throws ViewConstructionException {
    // cssClass
    button.cssClass = readString(BhConstants.NodeStyleDef.KEY_CSS_CLASS, jsonObj, fileName)
        .orElse(button.cssClass);
  }

  /**
   * {@link BhNodeViewStyle.Breakpoint} にスタイル情報を格納する.
   *
   * @param breakpoint jsonObj の情報を格納するオブジェクト
   * @param jsonObj ブレークポイントのパラメータが格納されたオブジェクト
   * @param fileName jsonObj が記述してある .json ファイルの名前
   */
  private void fillBreakpointParams(
      BhNodeViewStyle.Breakpoint breakpoint, JsonObject jsonObj, String fileName)
      throws ViewConstructionException {
    // cssClass
    breakpoint.cssClass = readString(BhConstants.NodeStyleDef.KEY_CSS_CLASS, jsonObj, fileName)
        .orElse(breakpoint.cssClass);

    // radius
    breakpoint.radius = readNumber(BhConstants.NodeStyleDef.KEY_RADIUS, jsonObj, fileName)
        .map(val -> val.doubleValue() * BhConstants.Ui.NODE_SCALE).orElse(breakpoint.radius);
  }

  /**
   * {@link BhNodeViewStyle.ExecStepMark} にスタイル情報を格納する.
   *
   * @param execStepMark jsonObj の情報を格納するオブジェクト
   * @param jsonObj 次に実行するノードであることを表す印のパラメータが格納されたオブジェクト
   * @param fileName jsonObj が記述してある .json ファイルの名前
   */
  private void fillExecStepMarkParams(
      BhNodeViewStyle.ExecStepMark execStepMark, JsonObject jsonObj, String fileName)
      throws ViewConstructionException {
    // cssClass
    execStepMark.cssClass = readString(BhConstants.NodeStyleDef.KEY_CSS_CLASS, jsonObj, fileName)
        .orElse(execStepMark.cssClass);

    // size
    execStepMark.size = readNumber(BhConstants.NodeStyleDef.KEY_SIZE, jsonObj, fileName)
        .map(val -> val.doubleValue() * BhConstants.Ui.NODE_SCALE).orElse(execStepMark.size);
  }

  /**
   * {@link BhNodeViewStyle.CorruptionMark} にスタイル情報を格納する.
   *
   * @param corruptionMark jsonObj の情報を格納するオブジェクト
   * @param jsonObj 破損マークのパラメータが格納されたオブジェクト
   * @param fileName jsonObj が記述してある .json ファイルの名前
   */
  private void fillCorruptionMarkParams(
      BhNodeViewStyle.CorruptionMark corruptionMark, JsonObject jsonObj, String fileName)
      throws ViewConstructionException {
    // cssClass
    corruptionMark.cssClass = readString(BhConstants.NodeStyleDef.KEY_CSS_CLASS, jsonObj, fileName)
        .orElse(corruptionMark.cssClass);

    // size
    corruptionMark.size = readNumber(BhConstants.NodeStyleDef.KEY_SIZE, jsonObj, fileName)
        .map(val -> val.doubleValue() * BhConstants.Ui.NODE_SCALE).orElse(corruptionMark.size);
  }

  /**
   * {@link BhNodeViewStyle.EntryPointMark} にスタイル情報を格納する.
   *
   * @param entryPoint jsonObj の情報を格納するオブジェクト
   * @param jsonObj ブレークポイントのパラメータが格納されたオブジェクト
   * @param fileName jsonObj が記述してある .json ファイルの名前
   */
  private void fillEntryPointMarkParams(
      BhNodeViewStyle.EntryPointMark entryPoint, JsonObject jsonObj, String fileName)
      throws ViewConstructionException {
    // cssClass
    entryPoint.cssClass = readString(BhConstants.NodeStyleDef.KEY_CSS_CLASS, jsonObj, fileName)
        .orElse(entryPoint.cssClass);

    // radius
    entryPoint.radius = readNumber(BhConstants.NodeStyleDef.KEY_RADIUS, jsonObj, fileName)
        .map(val -> val.doubleValue() * BhConstants.Ui.NODE_SCALE).orElse(entryPoint.radius);
  }

  /**
   * {@link BhNodeViewStyle.TextField} にスタイル情報を格納する.
   *
   * @param textField jsonオブジェクトから読み取った内容を格納するオブジェクト
   * @param jsonObj key = "textField" の value であるオブジェクト
   * @param fileName jsonObj が記述してある .json ファイルの名前
   */
  private static void fillTextFieldParams(
      BhNodeViewStyle.TextField textField, JsonObject jsonObj, String fileName)
      throws ViewConstructionException {
    // cssClass
    textField.cssClass = readString(BhConstants.NodeStyleDef.KEY_CSS_CLASS, jsonObj, fileName)
          .orElse(textField.cssClass);

    // minWidth
    textField.minWidth = readNumber(BhConstants.NodeStyleDef.KEY_MIN_WIDTH, jsonObj, fileName)
        .map(val -> val.doubleValue() * BhConstants.Ui.NODE_SCALE).orElse(textField.minWidth);
        
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
  private void fillLabelParams(
      BhNodeViewStyle.Label label, JsonObject jsonObj, String fileName)
      throws ViewConstructionException {
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
  private void fillComboBoxParams(
      BhNodeViewStyle.ComboBox comboBox, JsonObject jsonObj, String fileName)
      throws ViewConstructionException {
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
  private void fillTextAreaParams(
      BhNodeViewStyle.TextArea textArea, JsonObject jsonObj, String fileName)
      throws ViewConstructionException {
    // cssClass
    textArea.cssClass = readString(BhConstants.NodeStyleDef.KEY_CSS_CLASS, jsonObj, fileName)
        .orElse(textArea.cssClass);

    // minWidth
    textArea.minWidth = readNumber(BhConstants.NodeStyleDef.KEY_MIN_WIDTH, jsonObj, fileName)
        .map(val -> val.doubleValue() * BhConstants.Ui.NODE_SCALE).orElse(textArea.minWidth);

    // minHeight
    textArea.minHeight = readNumber(BhConstants.NodeStyleDef.KEY_MIN_HEIGHT, jsonObj, fileName)
        .map(val -> val.doubleValue() * BhConstants.Ui.NODE_SCALE).orElse(textArea.minHeight);

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
  private static Optional<Number> readNumber(String key, JsonObject jsonObj, String fileName)
      throws ViewConstructionException {
    JsonElement elem = jsonObj.get(key);
    if (elem == null) {
      return Optional.empty();
    }
    if (!elem.isJsonPrimitive()) {
      throw new ViewConstructionException(String.format(
          "The type of '%s' must be Number. (%s)\n  %s", key, fileName, elem.toString()));
    }
    JsonPrimitive primitive = elem.getAsJsonPrimitive();
    if (!primitive.isNumber()) {
      throw new ViewConstructionException(String.format(
          "The type of '%s' must be Number. (%s)\n  %s", key, fileName, primitive.toString()));
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
  private static Optional<String> readString(String key, JsonObject jsonObj, String fileName)
      throws ViewConstructionException {
    JsonElement elem = jsonObj.get(key);
    if (elem == null) {
      return Optional.empty();
    }
    if (!elem.isJsonPrimitive()) {
      throw new ViewConstructionException(String.format(
          "The type of '%s' must be String. (%s)\n  %s", key, fileName, elem.toString()));
    }
    JsonPrimitive primitive = elem.getAsJsonPrimitive();
    if (!primitive.isString()) {
      throw new ViewConstructionException(String.format(
          "The type of '%s' must be String. (%s)\n  %s", key, fileName, primitive.toString()));
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
  private static Optional<Boolean> readBool(String key, JsonObject jsonObj, String fileName)
      throws ViewConstructionException {
    JsonElement elem = jsonObj.get(key);
    if (elem == null) {
      return Optional.empty();
    }
    if (!elem.isJsonPrimitive()) {
      throw new ViewConstructionException(String.format(
          "The type of '%s' must be Boolean. (%s)\n  %s", key, fileName, elem.toString()));
    }
    JsonPrimitive primitive = elem.getAsJsonPrimitive();
    if (!primitive.isBoolean()) {
      throw new ViewConstructionException(String.format(
          "The type of '%s' must be Boolean. (%s)\n  %s", key, fileName, primitive.toString()));
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
  private static Optional<JsonArray> readArray(String key, JsonObject jsonObj, String fileName)
      throws ViewConstructionException {
    JsonElement elem = jsonObj.get(key);
    if (elem == null) {
      return Optional.empty();
    }
    if (!elem.isJsonArray()) {
      throw new ViewConstructionException(String.format(
          "The type of '%s' must be Array. (%s)\n  %s", key, fileName, elem.toString()));
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
  private static Optional<JsonObject> readObject(String key, JsonObject jsonObj, String fileName)
      throws ViewConstructionException {
    JsonElement elem = jsonObj.get(key);
    if (elem == null) {
      return Optional.empty();
    }
    if (!elem.isJsonObject()) {
      throw new ViewConstructionException(String.format(
          "The type of '%s' must be Object. (%s)\n  %s", key, fileName, elem.toString()));
    }
    return Optional.of(elem.getAsJsonObject());
  }

  @Override
  public BhNodeViewStyle createStyleOf(BhNodeViewStyleId styleId) {
    if (!canCreateStyleOf(styleId)) {
      return null;
    }
    return new BhNodeViewStyle(styleIdToStyle.get(styleId));
  }

  @Override
  public boolean canCreateStyleOf(BhNodeViewStyleId styleId) {
    return styleIdToStyle.containsKey(styleId);
  }
}
