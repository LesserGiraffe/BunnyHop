/**
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

import static java.nio.file.FileVisitOption.*;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;

import org.mozilla.javascript.NativeArray;
import org.mozilla.javascript.NativeObject;

import net.seapanda.bunnyhop.common.BhParams;
import net.seapanda.bunnyhop.common.Vec2D;
import net.seapanda.bunnyhop.common.tools.MsgPrinter;
import net.seapanda.bunnyhop.common.tools.Util;
import net.seapanda.bunnyhop.configfilereader.BhScriptManager;
import net.seapanda.bunnyhop.model.node.BhNodeID;
import net.seapanda.bunnyhop.view.bodyshape.BodyShape;
import net.seapanda.bunnyhop.view.bodyshape.BodyShape.BODY_SHAPE;
import net.seapanda.bunnyhop.view.connectorshape.ConnectorShape;
import net.seapanda.bunnyhop.view.connectorshape.ConnectorShape.CNCTR_SHAPE;

/**
 * 描画時の見た目(大きさ, 色など)の情報を持つクラス
 * @author K.Koike
 * */
public class BhNodeViewStyle {

	public String nodeStyleID; //!< ノードスタイルに付けられたID
	public double paddingTop = 2.5 * BhParams.LnF.NODE_SCALE; //!< ノード上部の余白
	public double paddingBottom = 2.5 * BhParams.LnF.NODE_SCALE; //!< ノード下部の余白
	public double paddingLeft = 2.5 * BhParams.LnF.NODE_SCALE; //!< ノード左部の余白
	public double paddingRight = 2.5 * BhParams.LnF.NODE_SCALE; //!< ノード右部の余白
	public double width = 0.0; //!< ノードの余白とコネクタを除いた部分の幅
	public double height = 0.0; //!< ノードの余白とコネクタを除いた部分の高さ
	public BODY_SHAPE bodyShape = BODY_SHAPE.BODY_SHAPE_ROUND_RECT;
	public CNCTR_POS connectorPos = CNCTR_POS.TOP; //!< コネクタの位置
	public double connectorShift = 0.5 * BhParams.LnF.NODE_SCALE; //!< ノードの左上からのコネクタの位置
	public double connectorWidth = 1.5 * BhParams.LnF.NODE_SCALE; //!< コネクタ部分の幅
	public double connectorHeight = 1.5 * BhParams.LnF.NODE_SCALE; //!< コネクタ部分の高さ
	public ConnectorShape.CNCTR_SHAPE connectorShape = ConnectorShape.CNCTR_SHAPE.ARROW; //!< コネクタの形
	public NOTCH_POS notchPos = NOTCH_POS.RIGHT; //!< 切り欠きの位置
	public double notchWidth = 1.5 * BhParams.LnF.NODE_SCALE; //!< コネクタ部分の幅
	public double notchHeight = 1.5 * BhParams.LnF.NODE_SCALE; //!< コネクタ部分の高さ
	public ConnectorShape.CNCTR_SHAPE notchShape =  ConnectorShape.CNCTR_SHAPE.NONE;	//!< 切り欠きの形
	public double connectorBoundsRate = 2.0; //!< ドラッグ&ドロップ時などに適用されるコネクタの範囲
	public String cssClass = "defaultNode";

	public Connective connective = new Connective();

	public static class Connective {
		public double outerWidth = 0.0; //!< 外部描画される部分の幅
		public double outerHeight = 0.0; //!< 外部描画される部分の高さ
		public Arrangement inner = new Arrangement();
		public Arrangement outer = new Arrangement();
	}

	public static class Arrangement {
		public double space = 2.5 * BhParams.LnF.NODE_SCALE; //!< ノード内部に描画するノード同士の間隔
		public double paddingTop = 0; //!< 内部ノード上部の余白
		public double paddingRight = 0; //!< 内部ノード右部の余白
		public double paddingBottom = 0; //!< 内部ノード下部の余白
		public double paddingLeft = 0; //!< 内部ノード左部の余白
		public CHILD_ARRANGEMENT arrangement = CHILD_ARRANGEMENT.COLUMN; //!< 子要素のノードとサブグループが並ぶ方向
		public List<String> cnctrNameList = new ArrayList<>();
		public List<Arrangement> subGroup = new ArrayList<>();

		void copy(Arrangement org) {
			space = org.space;
			paddingTop = org.paddingTop;
			paddingRight = org.paddingRight;
			paddingBottom = org.paddingBottom;
			paddingLeft = org.paddingLeft;
			arrangement = org.arrangement;
			cnctrNameList.addAll(org.cnctrNameList);
			org.subGroup.forEach(orgSubGroup -> {
				Arrangement newSubGroup = new Arrangement();
				newSubGroup.copy(orgSubGroup);
				subGroup.add(newSubGroup);
			});

		}
	}

	public TextField textField = new TextField();

	public static class TextField {
		public double minWidth = 0 * BhParams.LnF.NODE_SCALE;
		public String cssClass = "defaultTextField";
	}

	public Label label = new Label();

	public static class Label {
		public String cssClass = "defaultLabel";
	}

	public ComboBox comboBox = new ComboBox();

	public static class ComboBox {
		public String cssClass = "defaultComboBox";
	}

	public TextArea textArea = new TextArea();

	public static class TextArea {
		public double minWidth = 4 * BhParams.LnF.NODE_SCALE;
		public double minHeight = 3 * BhParams.LnF.NODE_SCALE;
		public String cssClass = "defaultTextArea";
	}


	public Imitation imitation = new Imitation();

	public static class Imitation {
		public double buttonPosX = 0.5 * BhParams.LnF.NODE_SCALE;
		public double buttonPosY = 0.5 * BhParams.LnF.NODE_SCALE;
		public String cssClass = "defaultImitButton";
	}

	private static final HashMap<String, BhNodeViewStyle> nodeStyleID_nodeStyleTemplate = new HashMap<>(); //!< ノードスタイルのテンプレートを格納するハッシュ. JSON ファイルの nodeStyleID がキー
	private static final HashMap<BhNodeID, String> nodeID_nodeStyleID = new HashMap<>(); //!< ノードIDとノードスタイルのペアを格納するハッシュ
	public static final HashMap<BhNodeID, String> nodeID_inputControlFileName = new HashMap<>(); //!< ノードIDとBhNodeの入力GUI部品のfxmlファイル名のペアを格納するハッシュ

	public enum CNCTR_POS {
		LEFT, TOP
	}

	public enum NOTCH_POS {
		RIGHT, BOTTOM
	}

	public enum CHILD_ARRANGEMENT {
		ROW, COLUMN
	}

	/**
	 * コンストラクタ
	 * */
	public BhNodeViewStyle() {}

	/**
	 * コピーコンストラクタ
	 * @param org コピー元
	 * */
	private BhNodeViewStyle(BhNodeViewStyle org) {

		this.nodeStyleID = org.nodeStyleID;
		this.paddingTop = org.paddingTop;
		this.paddingBottom = org.paddingBottom;
		this.paddingLeft = org.paddingLeft;
		this.paddingRight = org.paddingRight;
		this.width = org.width;
		this.height = org.height;
		this.bodyShape = org.bodyShape;
		this.connectorPos = org.connectorPos;
		this.connectorShift = org.connectorShift;
		this.connectorWidth = org.connectorWidth;
		this.connectorHeight = org.connectorHeight;
		this.connectorShape = org.connectorShape;
		this.connectorBoundsRate = org.connectorBoundsRate;
		this.notchPos = org.notchPos;
		this.notchWidth = org.notchWidth;
		this.notchHeight = org.notchHeight;
		this.notchShape = org.notchShape;
		this.connective.outerWidth = org.connective.outerWidth;
		this.connective.outerHeight = org.connective.outerHeight;
		this.connective.inner.copy(org.connective.inner);
		this.connective.outer.copy(org.connective.outer);
		this.cssClass = org.cssClass;
		this.textField.minWidth = org.textField.minWidth;
		this.textField.cssClass = org.textField.cssClass;
		this.label.cssClass = org.label.cssClass;
		this.comboBox.cssClass = org.comboBox.cssClass;
		this.textArea.minWidth = org.textArea.minWidth;
		this.textArea.minHeight = org.textArea.minHeight;
		this.textArea.cssClass = org.textArea.cssClass;
		this.imitation.cssClass = org.imitation.cssClass;
		this.imitation.buttonPosX = org.imitation.buttonPosX;
		this.imitation.buttonPosY = org.imitation.buttonPosY;
	}

	/**
	 * 外部ノードを含まない本体部分のサイズを取得する
	 * @param includeCnctr コネクタ部分の大きさを含む場合true
	 * @return コネクタ部分や外部ノードを含まない本体部分のサイズ
	 * */
	public Vec2D getBodySize(boolean includeCnctr) {

		Vec2D cnctrSize = getConnectorSize();
		double bodyWidth = paddingLeft + width + paddingRight;
		if (includeCnctr && (connectorPos == CNCTR_POS.LEFT))
			bodyWidth += cnctrSize.x;

		double bodyHeight = paddingTop + height + paddingBottom;
		if (includeCnctr && (connectorPos == CNCTR_POS.TOP))
			bodyHeight += cnctrSize.y;

		return new Vec2D(bodyWidth, bodyHeight);
	}

	/**
	 * 外部ノードを含む本体部分のサイズを取得する
	 * @param includCnctr コネクタ部分の大きさを含む場合true
	 * @return コネクタ部分や外部ノードを含まない本体部分のサイズ
	 * */
	public Vec2D getBodyAndOuterSize(boolean includCnctr) {

		Vec2D bodySize = getBodySize(includCnctr);
		double totalWidth = bodySize.x;
		double totalHeight = bodySize.y;
		if (connectorPos == CNCTR_POS.LEFT) { //外部ノードが右に接続される
			totalWidth += connective.outerWidth;
			totalHeight = Math.max(totalHeight, connective.outerHeight);
		} else { //外部ノードが下に接続される
			totalWidth = Math.max(totalWidth, connective.outerWidth);
			totalHeight += connective.outerHeight;
		}
		return new Vec2D(totalWidth, totalHeight);
	}

	/**
	 * コネクタの大きさを取得する
	 * @return コネクタの大きさ
	 */
	public Vec2D getConnectorSize() {

		double cnctrWidth = 0.0;
		if (connectorShape != CNCTR_SHAPE.NONE)
			cnctrWidth = connectorWidth;

		double cnctrHeight = 0.0;
		if (connectorShape != CNCTR_SHAPE.NONE)
			cnctrHeight = connectorHeight;

		return new Vec2D(cnctrWidth, cnctrHeight);
	}

	public static boolean genViewStyleTemplate() {

		//コネクタファイルパスリスト取得
		Path dirPath = Paths.get(Util.INSTANCE.EXEC_PATH, BhParams.Path.VIEW_DIR, BhParams.Path.NODE_STYLE_DEF_DIR);
		Stream<Path> paths = null; //読み込むファイルパスリスト
		try {
			paths = Files.walk(dirPath, FOLLOW_LINKS).filter(path -> path.getFileName().toString().endsWith(".json"));
		} catch (IOException e) {
			MsgPrinter.INSTANCE.errMsgForDebug("style directory not found " + dirPath);
			return false;
		}

		boolean succes = paths.map(filePath -> {
			NativeObject jsonObj = BhScriptManager.INSTANCE.parseJsonFile(filePath);
			if (jsonObj == null)
				return false;

			String styleID = filePath.getFileName().toString();
			Optional<BhNodeViewStyle> bhNodeViewStyle =
				genBhNodeViewStyle(jsonObj, filePath.toAbsolutePath().toString(), styleID);
			bhNodeViewStyle.ifPresent(viewStyle -> nodeStyleID_nodeStyleTemplate.put(viewStyle.nodeStyleID, viewStyle));
			return bhNodeViewStyle.isPresent();
		}).allMatch(success -> success);

		nodeStyleID_nodeStyleTemplate.put(BhParams.BhModelDef.ATTR_VALUE_DEFAULT_NODE_STYLE_ID, new BhNodeViewStyle());
		paths.close();
		return succes;
	}

	/**
	 * jsonオブジェクト から BhNodeViewStyle オブジェクトを作成する
	 * @param jsonObj .JSON ファイルを読み込んで作ったトップレベルオブジェクト
	 * @param fileName jsonObj が記述してある .JSON ファイルのファイル名
	 * @return BhNodeViewStyle (オプション)
	 * */
	private static Optional<BhNodeViewStyle> genBhNodeViewStyle(NativeObject jsonObj, String fileName, String styleID) {

		BhNodeViewStyle bhNodeViewStyle = new BhNodeViewStyle();

		//styleID
		bhNodeViewStyle.nodeStyleID = styleID;

		//paddingTop
		Optional<Object> val = readValue(BhParams.NodeStyleDef.KEY_PADDING_TOP, Number.class, jsonObj, fileName);
		val.ifPresent(paddingTop ->
			bhNodeViewStyle.paddingTop = ((Number) paddingTop).doubleValue() * BhParams.LnF.NODE_SCALE);

		//paddingBottom
		val = readValue(BhParams.NodeStyleDef.KEY_PADDING_BOTTOM, Number.class, jsonObj, fileName);
		val.ifPresent(paddingBottom ->
			bhNodeViewStyle.paddingBottom = ((Number) paddingBottom).doubleValue() * BhParams.LnF.NODE_SCALE);

		//paddingLeft
		val = readValue(BhParams.NodeStyleDef.KEY_PADDING_LEFT, Number.class, jsonObj, fileName);
		val.ifPresent(paddingLeft ->
			bhNodeViewStyle.paddingLeft = ((Number) paddingLeft).doubleValue() * BhParams.LnF.NODE_SCALE);

		//paddingRight
		val = readValue(BhParams.NodeStyleDef.KEY_PADDING_RIGHT, Number.class, jsonObj, fileName);
		val.ifPresent(paddingRight ->
			bhNodeViewStyle.paddingRight = ((Number) paddingRight).doubleValue() * BhParams.LnF.NODE_SCALE);

		//width
		val = readValue(BhParams.NodeStyleDef.KEY_WIDTH, Number.class, jsonObj, fileName);
		val.ifPresent(width -> bhNodeViewStyle.width = ((Number) width).doubleValue() * BhParams.LnF.NODE_SCALE);

		//height
		val = readValue(BhParams.NodeStyleDef.KEY_HEIGHT, Number.class, jsonObj, fileName);
		val.ifPresent(height -> bhNodeViewStyle.height = ((Number) height).doubleValue() * BhParams.LnF.NODE_SCALE);

		//bodyShape
		val = readValue(BhParams.NodeStyleDef.KEY_BODY_SHAPE, String.class, jsonObj, fileName);
		val.ifPresent(
			bodyShape -> {
				String shapeStr = (String)bodyShape;
				BODY_SHAPE shapeType = BodyShape.getBodyTypeFromName(shapeStr, fileName);
				bhNodeViewStyle.bodyShape = shapeType;
			});

		//connectorPos
		val = readValue(BhParams.NodeStyleDef.KEY_CONNECTOR_POS, String.class, jsonObj, fileName);
		val.ifPresent(connectorPos -> {
			String posStr = (String) connectorPos;
			if (posStr.equals(BhParams.NodeStyleDef.VAL_TOP)) {
				bhNodeViewStyle.connectorPos = CNCTR_POS.TOP;
			} else if (posStr.equals(BhParams.NodeStyleDef.VAL_LEFT)) {
				bhNodeViewStyle.connectorPos = CNCTR_POS.LEFT;
			} else {
				MsgPrinter.INSTANCE.errMsgForDebug("\"" + BhParams.NodeStyleDef.KEY_CONNECTOR_POS + "\"" + " (" + posStr
						+ ") " + "format is invalid.  " + "(" + fileName + ")");
			}
		});

		//connectorShift
		val = readValue(BhParams.NodeStyleDef.KEY_CONNECTOR_SHIFT, Number.class, jsonObj, fileName);
		val.ifPresent(connectorShift ->
			bhNodeViewStyle.connectorShift = ((Number) connectorShift).doubleValue() * BhParams.LnF.NODE_SCALE);

		//connectorWidth
		val = readValue(BhParams.NodeStyleDef.KEY_CONNECTOR_WIDTH, Number.class, jsonObj, fileName);
		val.ifPresent(connectorWidth ->
			bhNodeViewStyle.connectorWidth = ((Number) connectorWidth).doubleValue() * BhParams.LnF.NODE_SCALE);

		//connectorHeight
		val = readValue(BhParams.NodeStyleDef.KEY_CONNECTOR_HEIGHT, Number.class, jsonObj, fileName);
		val.ifPresent(connectorHeight ->
			bhNodeViewStyle.connectorHeight = ((Number) connectorHeight).doubleValue() * BhParams.LnF.NODE_SCALE);

		//connectorShape
		val = readValue(BhParams.NodeStyleDef.KEY_CONNECTOR_SHAPE, String.class, jsonObj, fileName);
		val.ifPresent(
			connectorShape -> {
				String shapeStr = (String) connectorShape;
				CNCTR_SHAPE shapeType = ConnectorShape.getConnectorTypeFromName(shapeStr, fileName);
				bhNodeViewStyle.connectorShape = shapeType;
			});

		//notchPos
		val = readValue(BhParams.NodeStyleDef.KEY_NOTCH_POS, String.class, jsonObj, fileName);
		val.ifPresent(notchPos -> {
			String posStr = (String) notchPos;
			if (posStr.equals(BhParams.NodeStyleDef.VAL_RIGHT)) {
				bhNodeViewStyle.notchPos = NOTCH_POS.RIGHT;
			} else if (posStr.equals(BhParams.NodeStyleDef.VAL_BOTTOM)) {
				bhNodeViewStyle.notchPos = NOTCH_POS.BOTTOM;
			} else {
				MsgPrinter.INSTANCE.errMsgForDebug("\"" + BhParams.NodeStyleDef.KEY_NOTCH_POS + "\"" + " (" + posStr
						+ ") " + "format is invalid.  " + "(" + fileName + ")");
			}
		});

		//notchWidth
		val = readValue(BhParams.NodeStyleDef.KEY_NOTCH_WIDTH, Number.class, jsonObj, fileName);
		val.ifPresent(notchWidth ->
			bhNodeViewStyle.notchWidth = ((Number)notchWidth).doubleValue() * BhParams.LnF.NODE_SCALE);

		//notchHeight
		val = readValue(BhParams.NodeStyleDef.KEY_NOTCH_HEIGHT, Number.class, jsonObj, fileName);
		val.ifPresent(notchHeight ->
			bhNodeViewStyle.notchHeight = ((Number)notchHeight).doubleValue() * BhParams.LnF.NODE_SCALE);

		//notchShape
		val = readValue(BhParams.NodeStyleDef.KEY_NOTCH_SHAPE, String.class, jsonObj, fileName);
		val.ifPresent(
			notchShape -> {
				String shapeStr = (String)notchShape;
				CNCTR_SHAPE shapeType = ConnectorShape.getConnectorTypeFromName(shapeStr, fileName);
				bhNodeViewStyle.notchShape = shapeType;
			});

		//connectorBoundsRate
		val = readValue(BhParams.NodeStyleDef.KEY_CONNECTOR_BOUNDS_RATE, Number.class, jsonObj, fileName);
		val.ifPresent(connectorBoundsRate ->
			bhNodeViewStyle.connectorBoundsRate = ((Number) connectorBoundsRate).doubleValue());

		//bodyCssClass
		val = readValue(BhParams.NodeStyleDef.KEY_CSS_CLASS, String.class, jsonObj, fileName);
		val.ifPresent(bodyCssClass -> bhNodeViewStyle.cssClass = (String) bodyCssClass);

		//connective
		Optional<Object> connectiveOpt =
			readValue(BhParams.NodeStyleDef.KEY_CONNECTIVE, NativeObject.class, jsonObj, fileName);
		connectiveOpt.ifPresent(
			connective -> fillConnectiveParams(bhNodeViewStyle.connective, (NativeObject)connective, fileName));

		//textField
		Optional<Object> textFieldOpt =
			readValue(BhParams.NodeStyleDef.KEY_TEXT_FIELD, NativeObject.class, jsonObj, fileName);
		textFieldOpt.ifPresent(
			textField -> fillTextFieldParams(bhNodeViewStyle.textField, (NativeObject) textField, fileName));

		//label
		Optional<Object> labelOpt =
			readValue(BhParams.NodeStyleDef.KEY_LABEL, NativeObject.class, jsonObj, fileName);
		labelOpt.ifPresent(label -> fillLabelParams(bhNodeViewStyle.label, (NativeObject) label, fileName));

		//comboBox
		Optional<Object> comboBoxOpt =
			readValue(BhParams.NodeStyleDef.KEY_COMBO_BOX, NativeObject.class, jsonObj, fileName);
		comboBoxOpt.ifPresent(
			comboBox -> fillComboBoxParams(bhNodeViewStyle.comboBox, (NativeObject) comboBox, fileName));

		//textArea
		Optional<Object> textAreaOpt =
			readValue(BhParams.NodeStyleDef.KEY_TEXT_AREA, NativeObject.class, jsonObj, fileName);
		textAreaOpt.ifPresent(
			textArea -> fillTextAreaParams(bhNodeViewStyle.textArea, (NativeObject)textArea, fileName));

		//imitation
		Optional<Object> imitationOpt =
			readValue(BhParams.NodeStyleDef.KEY_IMITATION, NativeObject.class, jsonObj, fileName);
		imitationOpt.ifPresent(
			imitation -> fillImitationParams(bhNodeViewStyle.imitation, (NativeObject) imitation, fileName));

		return Optional.of(bhNodeViewStyle);
	}

	/**
	 * BhNodeViewStyle.Connective にスタイル情報を格納する
	 * @param jsonObj key = "connective" の value であるオブジェクト
	 * @param fileName jsonObj が記述してある .JSON ファイルの名前
	 * */
	private static void fillConnectiveParams(
		BhNodeViewStyle.Connective connectiveStyle,
		NativeObject jsonObj,
		String fileName) {
		// inner
		Optional<Object> val = readValue(BhParams.NodeStyleDef.KEY_INNER, NativeObject.class, jsonObj, fileName);
		val.ifPresent(
			innerArrange -> fillArrangementParams(connectiveStyle.inner, (NativeObject) innerArrange, fileName));

		// outer
		val = readValue(BhParams.NodeStyleDef.KEY_OUTER, NativeObject.class, jsonObj, fileName);
		val.ifPresent(
			outerArrange -> fillArrangementParams(connectiveStyle.outer, (NativeObject) outerArrange, fileName));

	}

	/**
	 * BhNodeViewStyle.Arrangement にスタイル情報を格納する
	 * @param arrangement 並べ方に関するパラメータの格納先
	 * @param jsonObj key = "inner" または "outer" の value であるオブジェクト
	 * @param fileName jsonObj が記述してある .JSON ファイルの名前
	 * */
	private static void fillArrangementParams(
		BhNodeViewStyle.Arrangement arrangement,
		NativeObject jsonObj,
		String fileName) {

		//space
		Optional<Object> val = readValue(BhParams.NodeStyleDef.KEY_SPACE, Number.class, jsonObj, fileName);
		val.ifPresent(space -> {
			arrangement.space = ((Number) space).doubleValue() * BhParams.LnF.NODE_SCALE;
		});

		//paddingTop
		val = readValue(BhParams.NodeStyleDef.KEY_PADDING_TOP, Number.class, jsonObj, fileName);
		val.ifPresent(paddingTop -> {
			arrangement.paddingTop = ((Number) paddingTop).doubleValue() * BhParams.LnF.NODE_SCALE;
		});

		//paddingRight
		val = readValue(BhParams.NodeStyleDef.KEY_PADDING_RIGHT, Number.class, jsonObj, fileName);
		val.ifPresent(paddingRight -> {
			arrangement.paddingRight = ((Number) paddingRight).doubleValue() * BhParams.LnF.NODE_SCALE;
		});

		//paddingBottom
		val = readValue(BhParams.NodeStyleDef.KEY_PADDING_BOTTOM, Number.class, jsonObj, fileName);
		val.ifPresent(paddingBottom -> {
			arrangement.paddingBottom = ((Number) paddingBottom).doubleValue() * BhParams.LnF.NODE_SCALE;
		});

		//paddingLeft
		val = readValue(BhParams.NodeStyleDef.KEY_PADDING_LEFT, Number.class, jsonObj, fileName);
		val.ifPresent(paddingLeft -> {
			arrangement.paddingLeft = ((Number) paddingLeft).doubleValue() * BhParams.LnF.NODE_SCALE;
		});

		//arrangement
		val = readValue(BhParams.NodeStyleDef.KEY_ARRANGEMENR, String.class, jsonObj, fileName);
		val.ifPresent(childArrange -> {
			String arrangeStr = (String) childArrange;
			if (arrangeStr.equals(BhParams.NodeStyleDef.VAL_ROW)) {
				arrangement.arrangement = CHILD_ARRANGEMENT.ROW;
			} else if (arrangeStr.equals(BhParams.NodeStyleDef.VAL_COLUMN)) {
				arrangement.arrangement = CHILD_ARRANGEMENT.COLUMN;
			} else {
				MsgPrinter.INSTANCE.errMsgForDebug("\"" + BhParams.NodeStyleDef.KEY_ARRANGEMENR + "\"" + " ("
						+ arrangeStr + ") " + "format is invalid.  " + "(" + fileName + ")");
			}
		});

		//cnctrNameList
		val = readValue(BhParams.NodeStyleDef.KEY_CONNECTOR_LIST, NativeArray.class, jsonObj, fileName);
		val.ifPresent(cnctrs -> {
			NativeArray cnctrList = (NativeArray)cnctrs;
			for (Object cnctrName : cnctrList)
				arrangement.cnctrNameList.add(cnctrName.toString());
		});

		//subGroup
		int groupID = 0;
		while (true) {
			String subGroupKeyName = BhParams.NodeStyleDef.KEY_SUB_GROUP + groupID;
			val = readValue(subGroupKeyName, NativeObject.class, jsonObj, fileName);
			if (val.isPresent()) {
				Arrangement subGroup = new Arrangement();
				fillArrangementParams(subGroup, (NativeObject) val.get(), fileName);
				arrangement.subGroup.add(subGroup);
			} else {
				break;
			}
			++groupID;
		}
	}

	/**
	 * BhNodeViewStyle.Imitaion を埋める
	 * @param imitation jsonObj の情報を格納するオブジェクト
	 * @param jsonObj key = "imitation" の value であるオブジェクト
	 * @param fileName jsonObj が記述してある .json ファイルの名前
	 * */
	private static void fillImitationParams(
		BhNodeViewStyle.Imitation imitation,
		NativeObject jsonObj,
		String fileName) {

		//buttonPosX
		Optional<Object> val = readValue(BhParams.NodeStyleDef.KEY_BUTTON_POS_X, Number.class, jsonObj, fileName);
		val.ifPresent(btnPosX -> {
			imitation.buttonPosX = ((Number) btnPosX).doubleValue() * BhParams.LnF.NODE_SCALE;
		});

		//buttonPosY
		val = readValue(BhParams.NodeStyleDef.KEY_BUTTON_POS_Y, Number.class, jsonObj, fileName);
		val.ifPresent(btnPosY -> {
			imitation.buttonPosY = ((Number) btnPosY).doubleValue() * BhParams.LnF.NODE_SCALE;
		});

		//buttonCssClass
		val = readValue(BhParams.NodeStyleDef.KEY_CSS_CLASS, String.class, jsonObj, fileName);
		val.ifPresent(buttonCssClass -> imitation.cssClass = (String) buttonCssClass);
	}

	/**
	 * BhNodeViewStyle.TextField を埋める
	 * @param textField jsonオブジェクトから読み取った内容を格納するオブジェクト
	 * @param jsonObj key = "textField" の value であるオブジェクト
	 * @param fileName jsonObj が記述してある .json ファイルの名前
	 * */
	private static void fillTextFieldParams(
		BhNodeViewStyle.TextField textField,
		NativeObject jsonObj,
		String fileName) {

		//cssClass
		Optional<Object> val = readValue(BhParams.NodeStyleDef.KEY_CSS_CLASS, String.class, jsonObj, fileName);
		val.ifPresent(textCssClass -> textField.cssClass = (String) textCssClass);

		//minWidth
		val = readValue(BhParams.NodeStyleDef.KEY_MIN_WIDTH, Number.class, jsonObj, fileName);
		val.ifPresent(minWidth -> textField.minWidth = ((Number) minWidth).doubleValue() * BhParams.LnF.NODE_SCALE);
	}

	/**
	 * BhNodeViewStyle.Label を埋める
	 * @param label jsonオブジェクトから読み取った内容を格納するオブジェクト
	 * @param jsonObj key = "label" の value であるオブジェクト
	 * @param fileName jsonObj が記述してある .json ファイルの名前
	 * */
	private static void fillLabelParams(
		BhNodeViewStyle.Label textField,
		NativeObject jsonObj,
		String fileName) {
		//cssClass
		Optional<Object> val = readValue(BhParams.NodeStyleDef.KEY_CSS_CLASS, String.class, jsonObj, fileName);
		val.ifPresent(textCssClass -> textField.cssClass = (String) textCssClass);
	}

	/**
	 * BhNodeViewStyle.ComboBox を埋める
	 * @param comboBox jsonオブジェクトから読み取った内容を格納するオブジェクト
	 * @param jsonObj key = "comboBox" の value であるオブジェクト
	 * @param fileName jsonObj が記述してある .json ファイルの名前
	 * */
	private static void fillComboBoxParams(
		BhNodeViewStyle.ComboBox comboBox,
		NativeObject jsonObj,
		String fileName) {
		//cssClass
		Optional<Object> val = readValue(BhParams.NodeStyleDef.KEY_CSS_CLASS, String.class, jsonObj, fileName);
		val.ifPresent(comboBoxCssClass -> comboBox.cssClass = (String) comboBoxCssClass);
	}

	/**
	 * BhNodeViewStyle.TextArea を埋める
	 * @param textArea jsonオブジェクトから読み取った内容を格納するオブジェクト
	 * @param jsonObj key = "textArea" の value であるオブジェクト
	 * @param fileName jsonObj が記述してある .json ファイルの名前
	 * */
	private static void fillTextAreaParams(
		BhNodeViewStyle.TextArea textArea,
		NativeObject jsonObj,
		String fileName) {

		//cssClass
		Optional<Object> val = readValue(BhParams.NodeStyleDef.KEY_CSS_CLASS, String.class, jsonObj, fileName);
		val.ifPresent(textAreaCssClass -> textArea.cssClass = (String)textAreaCssClass);

		//minWidth
		val = readValue(BhParams.NodeStyleDef.KEY_MIN_WIDTH, Number.class, jsonObj, fileName);
		val.ifPresent(minWidth -> textArea.minWidth = ((Number) minWidth).doubleValue() * BhParams.LnF.NODE_SCALE);

		//minHeight
		val = readValue(BhParams.NodeStyleDef.KEY_MIN_HEIGHT, Number.class, jsonObj, fileName);
		val.ifPresent(minHeight -> textArea.minHeight = ((Number) minHeight).doubleValue() * BhParams.LnF.NODE_SCALE);
	}



	/**
	 * Json オブジェクトからエラーチェック付きで Value を読む
	 * @param keyName keyの名前
	 * @param valueType 想定される value の型 (String, Number, Boolean, NativeObject, ...)
	 * @param jsonObj key と value が格納されているJson オブジェクト
	 * @param fileName jsonObj を読み取ったファイルの名前
	 * @return JsonValue オブジェクト (オプション)
	 * */
	private static Optional<Object> readValue(
		String keyName,
		Class<?> valueType,
		NativeObject jsonObj,
		String fileName) {

		Object val = jsonObj.get(keyName);
		if (val == null)
			return Optional.empty();

		if (!valueType.isAssignableFrom(val.getClass())) {
			MsgPrinter.INSTANCE.errMsgForDebug(
				"The type of " + keyName + " must be " + valueType.getSimpleName() + ".  \n"
				+ "The actual type is " + val.getClass().getSimpleName() + ". " + "(" + fileName + ")");
			return Optional.empty();
		}
		return Optional.of(val);
	}

	/**
	 * ノードIDとノードスタイルID のペアを登録する
	 * @param nodeID ノードID (bhNodeID属性)
	 * @param nodeStyleID ノードスタイルID (nodeStyleID属性)
	 * */
	public static void putNodeID_NodeStyleID(BhNodeID nodeID, String nodeStyleID) {
		nodeID_nodeStyleID.put(nodeID, nodeStyleID);
	}

	/**
	 * ノードID から ノードスタイルオブジェクトを取得する
	 * @param nodeID ノードID (bhNodeID属性)
	 * @return ノードスタイルオブジェクト
	 * */
	public static BhNodeViewStyle getNodeViewStyleFromNodeID(BhNodeID nodeID) {

		String nodeStyleID = nodeID_nodeStyleID.get(nodeID);
		BhNodeViewStyle nodeStyle = nodeStyleID_nodeStyleTemplate.get(nodeStyleID);
		return new BhNodeViewStyle(nodeStyle);
	}

	/**
	 * 登録された全てのBhNodeIDに対してノードスタイルのテンプレートが存在するかどうかチェック
	 * @return 登録された全てのBhNodeIDに対してノードスタイルのテンプレートが存在する場合 true
	 * */
	public static boolean checkNodeIdAndNodeTemplate() {

		return nodeID_nodeStyleID.values().stream().allMatch(nodeStyleID -> {
			if (!nodeStyleID_nodeStyleTemplate.containsKey(nodeStyleID)) {
				MsgPrinter.INSTANCE.errMsgForDebug(
						"A node style file " + "(" + nodeStyleID + ")" + " is not found among *.json files");
				return false;
			} else {
				return true;
			}
		});
	}
}
