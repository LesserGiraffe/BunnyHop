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
import static net.seapanda.bunnyhop.utility.function.ThrowingConsumer.unchecked;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
import java.io.FileReader;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.SequencedCollection;
import java.util.stream.Stream;
import net.seapanda.bunnyhop.common.configuration.BhConstants;
import net.seapanda.bunnyhop.node.model.parameter.BhNodeViewStyleId;
import net.seapanda.bunnyhop.node.view.bodyshape.BodyShape;
import net.seapanda.bunnyhop.node.view.component.ComponentType;
import net.seapanda.bunnyhop.node.view.connectorshape.ConnectorShape;
import net.seapanda.bunnyhop.node.view.style.BhNodeViewStyleSnippet.ArrangementSnippet;
import net.seapanda.bunnyhop.node.view.style.BhNodeViewStyleSnippet.BreakpointSnippet;
import net.seapanda.bunnyhop.node.view.style.BhNodeViewStyleSnippet.ButtonSnippet;
import net.seapanda.bunnyhop.node.view.style.BhNodeViewStyleSnippet.ComboBoxSnippet;
import net.seapanda.bunnyhop.node.view.style.BhNodeViewStyleSnippet.CommonPartSnippet;
import net.seapanda.bunnyhop.node.view.style.BhNodeViewStyleSnippet.ConnectiveSnippet;
import net.seapanda.bunnyhop.node.view.style.BhNodeViewStyleSnippet.CorruptionMarkSnippet;
import net.seapanda.bunnyhop.node.view.style.BhNodeViewStyleSnippet.EntryPointMarkSnippet;
import net.seapanda.bunnyhop.node.view.style.BhNodeViewStyleSnippet.ExecStepMarkSnippet;
import net.seapanda.bunnyhop.node.view.style.BhNodeViewStyleSnippet.LabelSnippet;
import net.seapanda.bunnyhop.node.view.style.BhNodeViewStyleSnippet.SpecificPartSnippet;
import net.seapanda.bunnyhop.node.view.style.BhNodeViewStyleSnippet.TextAreaSnippet;
import net.seapanda.bunnyhop.node.view.style.BhNodeViewStyleSnippet.TextFieldSnippet;
import net.seapanda.bunnyhop.ui.view.ViewConstructionException;
import net.seapanda.bunnyhop.utility.function.ThrowingFunction;

/**
 * {@link BhNodeViewStyle} を作成する機能を提供するクラス.
 *
 * @author K.Koike
 */
public class JsonBhNodeViewStyleFactory implements BhNodeViewStyleFactory {

  /** ノードスタイルのテンプレートを格納するマップ.*/
  private final Map<BhNodeViewStyleId, BhNodeViewStyle> styleIdToStyle = new HashMap<>();

  /** ノードスタイルの断片を格納するマップ.*/
  private final Map<Path, BhNodeViewStyleSnippet> pathToStyleSnippet = new HashMap<>();

  /**
   * 引数で指定したディレクトリ以下の json ファイルからノードスタイルを作成し保持する.
   *
   * @param dirPath このディレクトリ以下からノードスタイルの定義ファイルを探す.
   */
  public JsonBhNodeViewStyleFactory(Path dirPath) throws ViewConstructionException {
    try (Stream<Path> paths = Files.walk(dirPath, FOLLOW_LINKS)) {
      styleIdToStyle.put(
          BhNodeViewStyleId.of(BhConstants.BhModelDef.ATTR_VAL_DEFAULT_NODE_STYLE_ID),
          new BhNodeViewStyle());
      List<Path> jsonPaths =
          paths.filter(path -> path.getFileName().toString().endsWith(".json")).toList();
      for (Path path : jsonPaths) {
        registerNodeStyle(path);
      }
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
    BhNodeViewStyleId styleId = BhNodeViewStyleId.of(filePath.getFileName().toString());
    BhNodeViewStyle style = createBhNodeViewStyleSnippet(filePath).build(styleId);
    styleIdToStyle.put(styleId, style);
  }

  /**
   * {@code filePath} で指定したファイルから {@link BhNodeViewStyleSnippet} オブジェクトを作成する.
   *
   * @param filePath ノードのスタイルが記述してある JSON ファイルのパス
   * @return 作成した {@link BhNodeViewStyle} オブジェクト
   */
  private BhNodeViewStyleSnippet createBhNodeViewStyleSnippet(Path filePath) throws
      ViewConstructionException {
    var gson = new Gson();
    try (var jr = gson.newJsonReader(new FileReader(filePath.toFile()))) {
      JsonObject jsonObj = gson.fromJson(jr, JsonObject.class);
      var snippet = new BhNodeViewStyleSnippet();
      String pathStr = filePath.toFile().getCanonicalPath();
      populateBhNodeViewStyle(snippet, jsonObj, pathStr);
      pathToStyleSnippet.put(filePath, snippet);
      snippet.addSubSnippets(importSubStyleSnippets(jsonObj, filePath));
      return snippet;
    } catch (Exception e) {
      throw new ViewConstructionException(e.toString());
    }
  }

  /**
   * JSON の "import" キーで指定されたスタイルファイルを読み込み,
   * {@link BhNodeViewStyleSnippet} オブジェクトのコレクションとして返す.
   *
   * @param jsonObj この JSON オブジェクトからインポートするスタイルファイルのリストを取得する
   * @param filePath {@code jsonObj} が記述してある JSON ファイルのパス
   * @return インポートされたスタイルのコレクション
   * @throws ViewConstructionException スタイルの読み込みに失敗した場合
   */
  private SequencedCollection<BhNodeViewStyleSnippet> importSubStyleSnippets(
      JsonObject jsonObj, Path filePath)  throws ViewConstructionException {
    try {
      String pathStr = filePath.toFile().getCanonicalPath();
      var styles = new ArrayList<BhNodeViewStyleSnippet>();
      readArray(BhConstants.NodeStyleDef.KEY_IMPORT, jsonObj, pathStr)
          .orElse(new JsonArray())
          .asList().stream()
          .filter(val -> val.isJsonPrimitive() && val.getAsJsonPrimitive().isString())
          .map(val -> val.getAsJsonPrimitive().getAsString())
          .map(filePath.getParent()::resolve)
          .forEach(unchecked(path -> {
            BhNodeViewStyleSnippet style = pathToStyleSnippet.containsKey(path)
                ? pathToStyleSnippet.get(path) : createBhNodeViewStyleSnippet(path);
            styles.add(style);
          }));
      return styles;
    } catch (Exception e) {
      throw new ViewConstructionException(e.toString());
    }
  }

  /**
   * {@link BhNodeViewStyleSnippet} にスタイル情報を格納する.
   *
   * @param snippet このオブジェクトにスタイル情報を格納する
   * @param jsonObj この JSON オブジェクトからスタイル情報を取得する
   * @param fileName {@code jsonObj} が記述してある JSON ファイルの名前
   */
  private void populateBhNodeViewStyle(
      BhNodeViewStyleSnippet snippet, JsonObject jsonObj, String fileName)
      throws ViewConstructionException {

    // paddingTop
    snippet.paddingTop = readNumber(BhConstants.NodeStyleDef.KEY_PADDING_TOP, jsonObj, fileName)
        .map(val -> val.doubleValue() * BhConstants.Ui.NODE_SCALE)
        .orElse(snippet.paddingTop);

    // paddingBottom
    snippet.paddingBottom =
        readNumber(BhConstants.NodeStyleDef.KEY_PADDING_BOTTOM, jsonObj, fileName)
        .map(val -> val.doubleValue() * BhConstants.Ui.NODE_SCALE)
        .orElse(snippet.paddingBottom);

    // paddingLeft
    snippet.paddingLeft = readNumber(BhConstants.NodeStyleDef.KEY_PADDING_LEFT, jsonObj, fileName)
        .map(val -> val.doubleValue() * BhConstants.Ui.NODE_SCALE)
        .orElse(snippet.paddingLeft);

    // paddingRight
    snippet.paddingRight = readNumber(BhConstants.NodeStyleDef.KEY_PADDING_RIGHT, jsonObj, fileName)
        .map(val -> val.doubleValue() * BhConstants.Ui.NODE_SCALE)
        .orElse(snippet.paddingRight);

    // bodyShape
    snippet.bodyShape = readString(BhConstants.NodeStyleDef.KEY_BODY_SHAPE, jsonObj, fileName)
        .map(val -> BodyShape.getBodyTypeFromName(val, fileName))
        .orElse(snippet.bodyShape);

    // connectorPos
    snippet.connectorPos = readString(BhConstants.NodeStyleDef.KEY_CONNECTOR_POS, jsonObj, fileName)
        .map(ConnectorPos::of)
        .orElse(snippet.connectorPos);

    // connectorShift
    snippet.connectorShift =
        readNumber(BhConstants.NodeStyleDef.KEY_CONNECTOR_SHIFT, jsonObj, fileName)
        .map(val -> val.doubleValue() * BhConstants.Ui.NODE_SCALE)
        .orElse(snippet.connectorShift);

    // connectorWidth
    snippet.connectorWidth =
        readNumber(BhConstants.NodeStyleDef.KEY_CONNECTOR_WIDTH, jsonObj, fileName)
            .map(val -> val.doubleValue() * BhConstants.Ui.NODE_SCALE)
            .orElse(snippet.connectorWidth);

    // connectorHeight
    snippet.connectorHeight =
        readNumber(BhConstants.NodeStyleDef.KEY_CONNECTOR_HEIGHT, jsonObj, fileName)
            .map(val -> val.doubleValue() * BhConstants.Ui.NODE_SCALE)
            .orElse(snippet.connectorHeight);

    // connectorAlignment
    snippet.connectorAlignment =
        readString(BhConstants.NodeStyleDef.KEY_CONNECTOR_ALIGNMENT, jsonObj, fileName)
        .map(ConnectorAlignment::of)
        .orElse(snippet.connectorAlignment);

    // connectorShape
    snippet.connectorShape =
        readString(BhConstants.NodeStyleDef.KEY_CONNECTOR_SHAPE, jsonObj, fileName)
        .map(val -> ConnectorShape.getConnectorTypeFromName(val, fileName))
        .orElse(snippet.connectorShape);

    // connectorShapeFixed
    snippet.connectorShapeFixed =
        readString(BhConstants.NodeStyleDef.KEY_CONNECTOR_SHAPE_FIXED, jsonObj, fileName)
        .map(val -> ConnectorShape.getConnectorTypeFromName(val, fileName))
        .orElse(snippet.connectorShape);  // fixed の場合の設定が存在しない場合は, 非 fixed の設定と同じにする.

    // notchPos
    snippet.notchPos = readString(BhConstants.NodeStyleDef.KEY_NOTCH_POS, jsonObj, fileName)
        .map(NotchPos::of)
        .orElse(snippet.notchPos);

    // notchWidth
    snippet.notchWidth = readNumber(BhConstants.NodeStyleDef.KEY_NOTCH_WIDTH, jsonObj, fileName)
        .map(val -> val.doubleValue() * BhConstants.Ui.NODE_SCALE)
        .orElse(snippet.notchWidth);

    // notchHeight
    snippet.notchHeight = readNumber(BhConstants.NodeStyleDef.KEY_NOTCH_HEIGHT, jsonObj, fileName)
        .map(val -> val.doubleValue() * BhConstants.Ui.NODE_SCALE)
        .orElse(snippet.notchHeight);

    // notchShape
    snippet.notchShape =
        readString(BhConstants.NodeStyleDef.KEY_NOTCH_SHAPE, jsonObj, fileName)
        .map(val -> ConnectorShape.getConnectorTypeFromName(val, fileName))
        .orElse(snippet.notchShape);

    // notchShapeFixed
    snippet.notchShapeFixed =
        readString(BhConstants.NodeStyleDef.KEY_NOTCH_SHAPE_FIXED, jsonObj, fileName)
        .map(val -> ConnectorShape.getConnectorTypeFromName(val, fileName))
        .orElse(snippet.notchShape); // fixed の場合の設定が存在しない場合は, 非 fixed の設定と同じにする.

    // connectorBoundsRate
    snippet.connectorBoundsRate =
        readNumber(BhConstants.NodeStyleDef.KEY_CONNECTOR_BOUNDS_RATE, jsonObj, fileName)
        .map(Number::doubleValue)
        .orElse(snippet.connectorBoundsRate);

    // cssClass
    snippet.cssClasses = readString(BhConstants.NodeStyleDef.KEY_CSS_CLASS, jsonObj, fileName)
        .map(clz -> clz.split(","))
        .orElse(snippet.cssClasses);

    // component
    snippet.component = readString(BhConstants.NodeStyleDef.KEY_COMPONENT, jsonObj, fileName)
        .map(ComponentType::of)
        .orElse(snippet.component);

    // baseArrangement
    snippet.baseArrangement =
        readString(BhConstants.NodeStyleDef.KEY_BASE_ARRANGEMENT, jsonObj, fileName)
        .map(ChildArrangement::of)
        .orElse(snippet.baseArrangement);

    // connective
    readObject(BhConstants.NodeStyleDef.KEY_CONNECTIVE, jsonObj, fileName)
        .ifPresent(
            unchecked(obj -> populateConnectiveParams(snippet.connective, obj, fileName)));

    // commonPart
    readObject(BhConstants.NodeStyleDef.KEY_COMMON_PART, jsonObj, fileName)
        .ifPresent(unchecked(obj -> populateCommonPartStyle(snippet.commonPart, obj, fileName)));

    // specificPart
    readObject(BhConstants.NodeStyleDef.KEY_SPECIFIC_PART, jsonObj, fileName).ifPresent(
        unchecked(obj -> populateSpecificPartStyle(snippet.specificPart, obj, fileName)));

    // textField
    readObject(BhConstants.NodeStyleDef.KEY_TEXT_FIELD, jsonObj, fileName)
        .ifPresent(unchecked(obj -> populateTextFieldStyle(snippet.textField, obj, fileName)));

    // label
    readObject(BhConstants.NodeStyleDef.KEY_LABEL, jsonObj, fileName)
        .ifPresent(unchecked(obj -> populateLabelStyle(snippet.label, obj, fileName)));

    // comboBox
    readObject(BhConstants.NodeStyleDef.KEY_COMBO_BOX, jsonObj, fileName)
        .ifPresent(
            unchecked(obj -> populateComboBoxStyle(snippet.comboBox, obj, fileName)));

    // textArea
    readObject(BhConstants.NodeStyleDef.KEY_TEXT_AREA, jsonObj, fileName)
        .ifPresent(
            unchecked(obj -> populateTextAreaStyle(snippet.textArea, obj, fileName)));
  }

  /**
   * {@link ConnectiveSnippet} にスタイル情報を格納する.
   *
   * @param snippet このオブジェクトにスタイル情報を格納する
   * @param jsonObj この JSON オブジェクトからスタイル情報を取得する
   * @param fileName {@code jsonObj} が記述してある JSON ファイルの名前
   */
  private void populateConnectiveParams(
      ConnectiveSnippet snippet, JsonObject jsonObj, String fileName)
      throws ViewConstructionException {
    // inner
    snippet.inner = readObject(BhConstants.NodeStyleDef.KEY_INNER, jsonObj, fileName)
        .map(ThrowingFunction.unchecked(
            obj -> populateArrangementStyle(new ArrangementSnippet(), obj, fileName)))
        .orElse(snippet.inner);

    // outer
    snippet.outer = readObject(BhConstants.NodeStyleDef.KEY_OUTER, jsonObj, fileName)
        .map(ThrowingFunction.unchecked(
            obj -> populateArrangementStyle(new ArrangementSnippet(), obj, fileName)))
        .orElse(snippet.outer);
  }

  /**
   * {@link BhNodeViewStyle.Arrangement} にスタイル情報を格納する.
   *
   * @param snippet このオブジェクトにスタイル情報を格納する
   * @param jsonObj この JSON オブジェクトからスタイル情報を取得する
   * @param fileName {@code jsonObj} が記述してある JSON ファイルの名前
   */
  private ArrangementSnippet populateArrangementStyle(
      ArrangementSnippet snippet, JsonObject jsonObj, String fileName)
      throws ViewConstructionException {
    // space
    snippet.space = readNumber(BhConstants.NodeStyleDef.KEY_SPACE, jsonObj, fileName)
        .map(val -> val.doubleValue() * BhConstants.Ui.NODE_SCALE)
        .orElse(snippet.space);

    // paddingTop
    snippet.paddingTop = readNumber(BhConstants.NodeStyleDef.KEY_PADDING_TOP, jsonObj, fileName)
        .map(val -> val.doubleValue() * BhConstants.Ui.NODE_SCALE)
        .orElse(snippet.paddingTop);

    // paddingRight
    snippet.paddingRight = readNumber(BhConstants.NodeStyleDef.KEY_PADDING_RIGHT, jsonObj, fileName)
        .map(val -> val.doubleValue() * BhConstants.Ui.NODE_SCALE)
        .orElse(snippet.paddingRight);

    // paddingBottom
    snippet.paddingBottom =
        readNumber(BhConstants.NodeStyleDef.KEY_PADDING_BOTTOM, jsonObj, fileName)
        .map(val -> val.doubleValue() * BhConstants.Ui.NODE_SCALE)
        .orElse(snippet.paddingBottom);

    // paddingLeft
    snippet.paddingLeft = readNumber(BhConstants.NodeStyleDef.KEY_PADDING_LEFT, jsonObj, fileName)
        .map(val -> val.doubleValue() * BhConstants.Ui.NODE_SCALE)
        .orElse(snippet.paddingLeft);

    // arrangement
    snippet.arrangement = readString(BhConstants.NodeStyleDef.KEY_ARRANGEMENT, jsonObj, fileName)
        .map(ChildArrangement::of)
        .orElse(snippet.arrangement);

    // cnctrNameList
    readArray(BhConstants.NodeStyleDef.KEY_CONNECTOR_LIST, jsonObj, fileName)
        .orElse(new JsonArray())
        .asList().stream()
        .filter(val -> val.isJsonPrimitive() && val.getAsJsonPrimitive().isString())
        .map(val -> val.getAsJsonPrimitive().getAsString())
        .forEach(snippet.cnctrNames::add);

    // subGroup
    int groupId = 0;
    while (true) {
      String subGroupKeyName = BhConstants.NodeStyleDef.KEY_SUB_GROUP + groupId++;
      JsonObject obj = readObject(subGroupKeyName, jsonObj, fileName).orElse(null);
      if (obj == null) {
        break;
      }
      ArrangementSnippet subGroup = new ArrangementSnippet();
      populateArrangementStyle(subGroup, obj, fileName);
      snippet.subGroups.add(subGroup);
    }
    return snippet;
  }

  /**
   * {@link CommonPartSnippet} にスタイル情報を格納する.
   *
   * @param snippet このオブジェクトにスタイル情報を格納する
   * @param jsonObj この JSON オブジェクトからスタイル情報を取得する
   * @param fileName {@code jsonObj} が記述してある JSON ファイルの名前
   */
  private void populateCommonPartStyle(
      CommonPartSnippet snippet, JsonObject jsonObj, String fileName)
      throws ViewConstructionException {
    // cssClass
    snippet.cssClass = readString(BhConstants.NodeStyleDef.KEY_CSS_CLASS, jsonObj, fileName)
        .orElse(snippet.cssClass);

    // arrangement
    snippet.arrangement = readString(BhConstants.NodeStyleDef.KEY_ARRANGEMENT, jsonObj, fileName)
        .map(ChildArrangement::of)
        .orElse(snippet.arrangement);

    // privateTemplate
    readObject(BhConstants.NodeStyleDef.KEY_PRIVATE_TEMPLATE, jsonObj, fileName)
        .ifPresent(
            unchecked(obj -> populateButtonStyle(snippet.privateTemplate, obj, fileName)));

    // breakpoint
    readObject(BhConstants.NodeStyleDef.KEY_BREAK_POINT, jsonObj, fileName)
        .ifPresent(unchecked(obj -> populateBreakpointStyle(snippet.breakpoint, obj, fileName)));

    // execStep
    readObject(BhConstants.NodeStyleDef.KEY_EXEC_STEP, jsonObj, fileName).ifPresent(
        unchecked(obj -> populateExecStepMarkStyle(snippet.execStepMark, obj, fileName)));

    // corruption
    readObject(BhConstants.NodeStyleDef.KEY_CORRUPTION, jsonObj, fileName).ifPresent(
        unchecked(obj -> populateCorruptionMarkStyle(snippet.corruptionMark, obj, fileName)));

    // entryPoint
    readObject(BhConstants.NodeStyleDef.KEY_ENTRY_POINT, jsonObj, fileName).ifPresent(
        unchecked(obj -> populateEntryPointMarkStyle(snippet.entryPointMark, obj, fileName)));
  }

  /**
   * {@link SpecificPartSnippet} にスタイル情報を格納する.
   *
   * @param snippet このオブジェクトにスタイル情報を格納する
   * @param jsonObj この JSON オブジェクトからスタイル情報を取得する
   * @param fileName {@code jsonObj} が記述してある JSON ファイルの名前
   */
  private void populateSpecificPartStyle(
      SpecificPartSnippet snippet, JsonObject jsonObj, String fileName)
      throws ViewConstructionException {
    // cssClass
    snippet.cssClass = readString(BhConstants.NodeStyleDef.KEY_CSS_CLASS, jsonObj, fileName)
        .orElse(snippet.cssClass);
  }

  /**
   * {@link ButtonSnippet} にスタイル情報を格納する.
   *
   * @param snippet このオブジェクトにスタイル情報を格納する
   * @param jsonObj この JSON オブジェクトからスタイル情報を取得する
   * @param fileName {@code jsonObj} が記述してある JSON ファイルの名前
   */
  private void populateButtonStyle(ButtonSnippet snippet, JsonObject jsonObj, String fileName)
      throws ViewConstructionException {
    // cssClass
    snippet.cssClass = readString(BhConstants.NodeStyleDef.KEY_CSS_CLASS, jsonObj, fileName)
        .orElse(snippet.cssClass);
  }

  /**
   * {@link BhNodeViewStyle.Breakpoint} にスタイル情報を格納する.
   *
   * @param snippet このオブジェクトにスタイル情報を格納する
   * @param jsonObj この JSON オブジェクトからスタイル情報を取得する
   * @param fileName {@code jsonObj} が記述してある JSON ファイルの名前
   */
  private void populateBreakpointStyle(
      BreakpointSnippet snippet, JsonObject jsonObj, String fileName)
      throws ViewConstructionException {
    // cssClass
    snippet.cssClass = readString(BhConstants.NodeStyleDef.KEY_CSS_CLASS, jsonObj, fileName)
        .orElse(snippet.cssClass);

    // radius
    snippet.radius = readNumber(BhConstants.NodeStyleDef.KEY_RADIUS, jsonObj, fileName)
        .map(val -> val.doubleValue() * BhConstants.Ui.NODE_SCALE)
        .orElse(snippet.radius);
  }

  /**
   * {@link ExecStepMarkSnippet} にスタイル情報を格納する.
   *
   * @param snippet このオブジェクトにスタイル情報を格納する
   * @param jsonObj この JSON オブジェクトからスタイル情報を取得する
   * @param fileName {@code jsonObj} が記述してある JSON ファイルの名前
   */
  private void populateExecStepMarkStyle(
      ExecStepMarkSnippet snippet, JsonObject jsonObj, String fileName)
      throws ViewConstructionException {
    // cssClass
    snippet.cssClass = readString(BhConstants.NodeStyleDef.KEY_CSS_CLASS, jsonObj, fileName)
        .orElse(snippet.cssClass);

    // size
    snippet.size = readNumber(BhConstants.NodeStyleDef.KEY_SIZE, jsonObj, fileName)
        .map(val -> val.doubleValue() * BhConstants.Ui.NODE_SCALE)
        .orElse(snippet.size);
  }

  /**
   * {@link CorruptionMarkSnippet} にスタイル情報を格納する.
   *
   * @param snippet このオブジェクトにスタイル情報を格納する
   * @param jsonObj この JSON オブジェクトからスタイル情報を取得する
   * @param fileName {@code jsonObj} が記述してある JSON ファイルの名前
   */
  private void populateCorruptionMarkStyle(
      CorruptionMarkSnippet snippet, JsonObject jsonObj, String fileName)
      throws ViewConstructionException {
    // cssClass
    snippet.cssClass = readString(BhConstants.NodeStyleDef.KEY_CSS_CLASS, jsonObj, fileName)
        .orElse(snippet.cssClass);

    // size
    snippet.size = readNumber(BhConstants.NodeStyleDef.KEY_SIZE, jsonObj, fileName)
        .map(val -> val.doubleValue() * BhConstants.Ui.NODE_SCALE)
        .orElse(snippet.size);
  }

  /**
   * {@link EntryPointMarkSnippet} にスタイル情報を格納する.
   *
   * @param snippet このオブジェクトにスタイル情報を格納する
   * @param jsonObj この JSON オブジェクトからスタイル情報を取得する
   * @param fileName {@code jsonObj} が記述してある JSON ファイルの名前
   */
  private void populateEntryPointMarkStyle(
      EntryPointMarkSnippet snippet, JsonObject jsonObj, String fileName)
      throws ViewConstructionException {
    // cssClass
    snippet.cssClass = readString(BhConstants.NodeStyleDef.KEY_CSS_CLASS, jsonObj, fileName)
        .orElse(snippet.cssClass);

    // radius
    snippet.radius = readNumber(BhConstants.NodeStyleDef.KEY_RADIUS, jsonObj, fileName)
        .map(val -> val.doubleValue() * BhConstants.Ui.NODE_SCALE)
        .orElse(snippet.radius);
  }

  /**
   * {@link TextFieldSnippet} にスタイル情報を格納する.
   *
   * @param snippet このオブジェクトにスタイル情報を格納する
   * @param jsonObj この JSON オブジェクトからスタイル情報を取得する
   * @param fileName {@code jsonObj} が記述してある JSON ファイルの名前
   */
  private static void populateTextFieldStyle(
      TextFieldSnippet snippet, JsonObject jsonObj, String fileName)
      throws ViewConstructionException {
    // cssClass
    snippet.cssClass = readString(BhConstants.NodeStyleDef.KEY_CSS_CLASS, jsonObj, fileName)
          .orElse(snippet.cssClass);

    // minWidth
    snippet.minWidth = readNumber(BhConstants.NodeStyleDef.KEY_MIN_WIDTH, jsonObj, fileName)
        .map(val -> val.doubleValue() * BhConstants.Ui.NODE_SCALE)
        .orElse(snippet.minWidth);
        
    // editable
    snippet.editable = readBool(BhConstants.NodeStyleDef.KEY_EDITABLE, jsonObj, fileName)
        .orElse(snippet.editable);
  }

  /**
   * {@link LabelSnippet} にスタイル情報を格納する.
   *
   * @param snippet このオブジェクトにスタイル情報を格納する
   * @param jsonObj この JSON オブジェクトからスタイル情報を取得する
   * @param fileName {@code jsonObj} が記述してある JSON ファイルの名前
   */
  private void populateLabelStyle(LabelSnippet snippet, JsonObject jsonObj, String fileName)
      throws ViewConstructionException {
    // cssClass
    snippet.cssClass = readString(BhConstants.NodeStyleDef.KEY_CSS_CLASS, jsonObj, fileName)
        .orElse(snippet.cssClass);
  }

  /**
   * {@link ComboBoxSnippet} にスタイル情報を格納する.
   *
   * @param snippet このオブジェクトにスタイル情報を格納する
   * @param jsonObj この JSON オブジェクトからスタイル情報を取得する
   * @param fileName {@code jsonObj} が記述してある JSON ファイルの名前
   */
  private void populateComboBoxStyle(ComboBoxSnippet snippet, JsonObject jsonObj, String fileName)
      throws ViewConstructionException {
    // cssClass
    snippet.cssClass = readString(BhConstants.NodeStyleDef.KEY_CSS_CLASS, jsonObj, fileName)
        .orElse(snippet.cssClass);
  }

  /**
   * {@link TextAreaSnippet} にスタイル情報を格納する.
   *
   * @param snippet このオブジェクトにスタイル情報を格納する
   * @param jsonObj この JSON オブジェクトからスタイル情報を取得する
   * @param fileName {@code jsonObj} が記述してある JSON ファイルの名前
   */
  private void populateTextAreaStyle(TextAreaSnippet snippet, JsonObject jsonObj, String fileName)
      throws ViewConstructionException {
    // cssClass
    snippet.cssClass = readString(BhConstants.NodeStyleDef.KEY_CSS_CLASS, jsonObj, fileName)
        .orElse(snippet.cssClass);

    // minWidth
    snippet.minWidth = readNumber(BhConstants.NodeStyleDef.KEY_MIN_WIDTH, jsonObj, fileName)
        .map(val -> val.doubleValue() * BhConstants.Ui.NODE_SCALE)
        .orElse(snippet.minWidth);

    // minHeight
    snippet.minHeight = readNumber(BhConstants.NodeStyleDef.KEY_MIN_HEIGHT, jsonObj, fileName)
        .map(val -> val.doubleValue() * BhConstants.Ui.NODE_SCALE)
        .orElse(snippet.minHeight);

    // editable
    snippet.editable = readBool(BhConstants.NodeStyleDef.KEY_EDITABLE, jsonObj, fileName)
        .orElse(snippet.editable);
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
          "The type of '%s' must be Number. (%s)\n  %s", key, fileName, elem));
    }
    JsonPrimitive primitive = elem.getAsJsonPrimitive();
    if (!primitive.isNumber()) {
      throw new ViewConstructionException(String.format(
          "The type of '%s' must be Number. (%s)\n  %s", key, fileName, primitive));
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
          "The type of '%s' must be String. (%s)\n  %s", key, fileName, elem));
    }
    JsonPrimitive primitive = elem.getAsJsonPrimitive();
    if (!primitive.isString()) {
      throw new ViewConstructionException(String.format(
          "The type of '%s' must be String. (%s)\n  %s", key, fileName, primitive));
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
          "The type of '%s' must be Boolean. (%s)\n  %s", key, fileName, elem));
    }
    JsonPrimitive primitive = elem.getAsJsonPrimitive();
    if (!primitive.isBoolean()) {
      throw new ViewConstructionException(String.format(
          "The type of '%s' must be Boolean. (%s)\n  %s", key, fileName, primitive));
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
          "The type of '%s' must be Array. (%s)\n  %s", key, fileName, elem));
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
          "The type of '%s' must be Object. (%s)\n  %s", key, fileName, elem));
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
