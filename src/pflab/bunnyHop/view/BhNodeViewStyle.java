package pflab.bunnyHop.view;


import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;
import jdk.nashorn.api.scripting.ScriptObjectMirror;

import pflab.bunnyHop.root.MsgPrinter;
import pflab.bunnyHop.common.BhParams;
import pflab.bunnyHop.common.Point2D;
import pflab.bunnyHop.common.Util;
import pflab.bunnyHop.configFileReader.BhScriptManager;
import pflab.bunnyHop.view.connectorShape.ConnectorShape;
import pflab.bunnyHop.view.connectorShape.ConnectorShape.CNCTR_SHAPE;

/**
 * 描画時の見た目(大きさ, 色など)の情報を持つクラス
 * @author K.Koike
 * */
public class BhNodeViewStyle {

	public String nodeStyleID;							//!< ノードスタイルに付けられたID
	public double topMargin = 2.5 * BhParams.nodeScale;			//!< ノード上部の余白
	public double bottomMargin = 2.5 * BhParams.nodeScale;		//!< ノード下部の余白
	public double leftMargin = 2.5 * BhParams.nodeScale;			//!< ノード左部の余白
	public double rightMargin = 2.5 * BhParams.nodeScale;			//!< ノード右部の余白
	public double width = 0.0;				//!< ノードの余白とコネクタを除いた部分の幅
	public double height = 0.0;				//!< ノードの余白とコネクタを除いた部分の高さ
	public CNCTR_POS connectorPos = CNCTR_POS.TOP;		//!< コネクタの位置
	public double connectorShift = 0.5 * BhParams.nodeScale;		//!< ノードの左上からのコネクタの位置
	public double connectorWidth = 1.5 * BhParams.nodeScale;		//!< コネクタ部分の幅
	public double connectorHeight = 1.5 * BhParams.nodeScale;		//!< コネクタ部分の高さ
	public ConnectorShape.CNCTR_SHAPE connectorShape = ConnectorShape.CNCTR_SHAPE.CNCTR_SHAPE_ARROW;	//!< コネクタの形
	public double connectorBoundsRate = 2.0;			//!< ドラッグ&ドロップ時などに適用されるコネクタの範囲
	public boolean drawBody = true;
	public String cssClass = "defaultNode";

	Connective connective = new Connective();
	public static class Connective {
		public double outerWidth = 0.0;						//!< 外部描画される部分の幅
		public double outerHeight = 0.0;					//!< 外部描画される部分の高さ
		public Arrangement inner = new Arrangement();
		public Arrangement outer = new Arrangement();
	}
	
	public static class Arrangement {
		public double interval = 2.5 * BhParams.nodeScale;		//!< ノード内部に描画するノード同士の間隔
		public CHILD_ARRANGEMENT arrangement = CHILD_ARRANGEMENT.COLUMN;	//!< 子要素のノードとサブグループが並ぶ方向
		public List<String> cnctrNameList = new ArrayList<>();
		public List<Arrangement> subGroup = new ArrayList<>();
		
		void copy(Arrangement org) {
			interval = org.interval;
			arrangement = org.arrangement;
			cnctrNameList.addAll(org.cnctrNameList);
			org.subGroup.forEach(orgSubGroup -> {
				Arrangement newSubGroup =  new Arrangement();
				newSubGroup.copy(orgSubGroup);
				subGroup.add(newSubGroup);
			});
			
		}
	}
	
	TextField textField = new TextField();
	public static class TextField {
		public double whiteSpaceMargine = 0;	//!< 入力されたテキストの後に付くwhite space の数
		public double minWhiteSpace =  5;	//!< テキストーフィールドの最小幅 (white space 換算)
		public String cssClass = "defaultTextField";
	}
	
	Label label = new Label();
	public static class Label {
		public String cssClass = "defaultLabel";
	}
	
	ComboBox comboBox = new ComboBox();
	public static class ComboBox {
		public String cssClass = "defaultComboBox";
	}
	
	Imitation imitation = new Imitation();
	public static class Imitation {
		public double buttonPosX = 0.5 * BhParams.nodeScale;
		public double buttonPosY = 0.5 * BhParams.nodeScale;
		public String cssClass = "defaultImitButton";
	}

	private static final HashMap<String, BhNodeViewStyle> nodeStyleID_nodeStyleTemplate = new HashMap<>(); //!< ノードスタイルのテンプレートを格納するハッシュ. JSON ファイルの nodeStyleID がキー
	private static final HashMap<String, String> nodeID_nodeStyleID = new HashMap<>(); //!< ノードIDとノードスタイルのペアを格納するハッシュ
	public static final HashMap<String, String> nodeID_inputControlFileName = new HashMap<>();	//!< ノードIDとBhNodeの入力GUI部品のfxmlファイル名のペアを格納するハッシュ

	public enum CNCTR_POS {
		LEFT,
		TOP
	}

	public enum CHILD_ARRANGEMENT {
		ROW,
		COLUMN
	}

	/**
	 * コンストラクタ
	 * */
	public BhNodeViewStyle(){}

	/**
	 * コピーコンストラクタ
	 * @param org コピー元
	 * */
	private BhNodeViewStyle(BhNodeViewStyle org){

		this.nodeStyleID = org.nodeStyleID;
		this.topMargin = org.topMargin;
		this.bottomMargin = org.bottomMargin;
		this.leftMargin = org.leftMargin;
		this.rightMargin = org.rightMargin;
		this.width = org.width;
		this.height = org.height;
		this.connectorPos = org.connectorPos;
		this.connectorShift = org.connectorShift;
		this.connectorWidth = org.connectorWidth;
		this.connectorHeight = org.connectorHeight;
		this.connectorShape = org.connectorShape;
		this.connectorBoundsRate = org.connectorBoundsRate;
		this.drawBody = org.drawBody;
		this.connective.outerWidth = org.connective.outerWidth;
		this.connective.outerHeight = org.connective.outerHeight;
		this.connective.inner.copy(org.connective.inner);
		this.connective.outer.copy(org.connective.outer);
		this.cssClass = org.cssClass;
		this.textField.whiteSpaceMargine = org.textField.whiteSpaceMargine;
		this.textField.minWhiteSpace = org.textField.minWhiteSpace;
		this.textField.cssClass = org.textField.cssClass;
		this.label.cssClass = org.label.cssClass;
		this.comboBox.cssClass = org.comboBox.cssClass;
		this.imitation.cssClass = org.imitation.cssClass;
		this.imitation.buttonPosX = org.imitation.buttonPosX;
		this.imitation.buttonPosY = org.imitation.buttonPosY;
	}

	/**
	 * 外部ノードを含まない本体部分のサイズを取得する
	 * @param includeCnctr コネクタ部分の大きさを含む場合true
	 * @return コネクタ部分や外部ノードを含まない本体部分のサイズ
	 * */
	public Point2D getBodySize(boolean includeCnctr) {
		
		Point2D cnctrSize = getConnectorSize();
		double bodyWidth = leftMargin + width  + rightMargin;
		if (includeCnctr && (connectorPos == CNCTR_POS.LEFT))
			bodyWidth += cnctrSize.x;
		
		double bodyHeight = topMargin  + height + bottomMargin;
		if (includeCnctr && (connectorPos == CNCTR_POS.TOP))
			bodyHeight += cnctrSize.y;
		
		return new Point2D(bodyWidth, bodyHeight);
	}

	/**
	 * 外部ノードを含む本体部分のサイズを取得する
	 * @param includedCnctr コネクタ部分の大きさを含む場合true
	 * @return コネクタ部分や外部ノードを含まない本体部分のサイズ
	 * */
	public Point2D getBodyAndOuterSize(boolean includedCnctr) {
	
		Point2D bodySize = getBodySize(includedCnctr);
		double totalWidth  = bodySize.x;
		double totalHeight = bodySize.y;
		if (connectorPos == CNCTR_POS.LEFT) {		//外部ノードが右に接続される
			totalWidth += connective.outerWidth;
			totalHeight = Math.max(totalHeight, connective.outerHeight);
		}
		else {												//外部ノードが下に接続される
			totalWidth = Math.max(totalWidth, connective.outerWidth);
			totalHeight += connective.outerHeight;
		}
		return new Point2D(totalWidth, totalHeight);	
	}		
	
	/**
	 * コネクタの大きさを取得する
	 * @return コネクタの大きさ
	 */
	public Point2D getConnectorSize() {
		
		double cnctrWidth = 0.0;
		if (connectorShape != CNCTR_SHAPE.CNCTR_SHAPE_NONE)
			cnctrWidth = connectorWidth;
		
		double cnctrHeight = 0.0;
		if (connectorShape != CNCTR_SHAPE.CNCTR_SHAPE_NONE)
			cnctrHeight = connectorHeight;
		
		return new Point2D(cnctrWidth, cnctrHeight);
	}
	
	public static boolean genViewStyleTemplate() {

		//コネクタファイルパスリスト取得
		Path dirPath = Paths.get(Util.execPath, BhParams.Path.viewDir, BhParams.Path.nodeStyleDefDir);
		Stream<Path> paths = null;	//読み込むファイルパスリスト
		try {
			paths = Files.walk(dirPath).filter(path -> path.getFileName().toString().endsWith(".json"));
		}
		catch (IOException e) {
			MsgPrinter.instance.ErrMsgForDebug("style directory not found " + dirPath);
			return false;
		}
	
		boolean succes = paths.map(filePath -> {
			ScriptObjectMirror jsonObj = BhScriptManager.instance.parseJsonFile(filePath);
			if (jsonObj == null)
				return false;
			
			String styleID = filePath.getFileName().toString();
			Optional<BhNodeViewStyle> bhNodeViewStyle = genBhNodeViewStyle((ScriptObjectMirror)jsonObj, filePath.toAbsolutePath().toString(), styleID);
			bhNodeViewStyle.ifPresent(viewStyle -> nodeStyleID_nodeStyleTemplate.put(viewStyle.nodeStyleID, viewStyle));
			return bhNodeViewStyle.isPresent();
		}).allMatch(success -> success);

		nodeStyleID_nodeStyleTemplate.put(BhParams.BhModelDef.attrValueDefaultNodeStyleID, new BhNodeViewStyle());
		paths.close();
		return succes;
	}

	/**
	 * jsonオブジェクト から BhNodeViewStyle オブジェクトを作成する
	 * @param jsonObj .JSON ファイルを読み込んで作ったトップレベルオブジェクト
	 * @param fileName jsonObj が記述してある .JSON ファイルのファイル名
	 * @return BhNodeViewStyle (オプション)
	 * */
	private static Optional<BhNodeViewStyle> genBhNodeViewStyle(ScriptObjectMirror jsonObj, String fileName, String styleID) {

		BhNodeViewStyle bhNodeViewStyle = new BhNodeViewStyle();

		//styleID
		bhNodeViewStyle.nodeStyleID = styleID;

		//topMargin
		Optional<Object> val = readValue(BhParams.NodeStyleDef.keyNameTopMargine, Number.class, jsonObj, fileName);
		val.ifPresent(topMargin -> bhNodeViewStyle.topMargin = ((Number)topMargin).doubleValue() * BhParams.nodeScale);	

		//bottomMargin
		val = readValue(BhParams.NodeStyleDef.keyNameBottomMargin, Number.class, jsonObj, fileName);
		val.ifPresent(bottomMargin -> bhNodeViewStyle.bottomMargin = ((Number)bottomMargin).doubleValue() * BhParams.nodeScale);

		//leftMargin
		val = readValue(BhParams.NodeStyleDef.keyNameLeftMargin, Number.class, jsonObj, fileName);
		val.ifPresent(leftMargin -> bhNodeViewStyle.leftMargin = ((Number)leftMargin).doubleValue() * BhParams.nodeScale);

		//rightMargin
		val = readValue(BhParams.NodeStyleDef.keyNameRightMargin, Number.class, jsonObj, fileName);
		val.ifPresent(rightMargin -> bhNodeViewStyle.rightMargin = ((Number)rightMargin).doubleValue() * BhParams.nodeScale);

		//width
		val = readValue(BhParams.NodeStyleDef.keyNameWidth, Number.class, jsonObj, fileName);
		val.ifPresent(width -> bhNodeViewStyle.width = ((Number)width).doubleValue() * BhParams.nodeScale);

		//height
		val = readValue(BhParams.NodeStyleDef.keyNameHeight, Number.class, jsonObj, fileName);
		val.ifPresent(height -> bhNodeViewStyle.height = ((Number)height).doubleValue() * BhParams.nodeScale);

		//connectorPos
		val = readValue(BhParams.NodeStyleDef.keyNameConnectorPos, String.class, jsonObj, fileName);
		val.ifPresent(connectorPos -> {
				String posStr = (String)connectorPos;
				if (posStr.equals(BhParams.NodeStyleDef.valNameTop)) {
					bhNodeViewStyle.connectorPos = CNCTR_POS.TOP;
				}
				else if (posStr.equals(BhParams.NodeStyleDef.valNameLeft)) {
					bhNodeViewStyle.connectorPos = CNCTR_POS.LEFT;
				}
				else {
					MsgPrinter.instance.ErrMsgForDebug("\"" + BhParams.NodeStyleDef.keyNameConnectorPos + "\"" + " (" + posStr + ") " + "format is invalid.  " + "(" + fileName + ")");
				}
			});

		//connectorShift
		val = readValue(BhParams.NodeStyleDef.keyNameConnectorShift , Number.class, jsonObj, fileName);
		val.ifPresent(connectorShift -> bhNodeViewStyle.connectorShift = ((Number)connectorShift).doubleValue() * BhParams.nodeScale);

		//connectorWidth
		val = readValue(BhParams.NodeStyleDef.keyNameConnectorWidth, Number.class, jsonObj, fileName);
		val.ifPresent(connectorWidth -> bhNodeViewStyle.connectorWidth = ((Number)connectorWidth).doubleValue() * BhParams.nodeScale);

		//connectorHeight
		val = readValue(BhParams.NodeStyleDef.keyNameConnectorHeight, Number.class, jsonObj, fileName);
		val.ifPresent(connectorHeight -> bhNodeViewStyle.connectorHeight = ((Number)connectorHeight).doubleValue() * BhParams.nodeScale);

		//connectorShape
		val = readValue(BhParams.NodeStyleDef.keyNameConnectorShape, String.class, jsonObj, fileName);
		val.ifPresent(
			connectorShape -> {
				String shapeStr = (String)connectorShape;
				CNCTR_SHAPE shapeType = ConnectorShape.stringToCNCTR_SHAPE(shapeStr , fileName);
				bhNodeViewStyle.connectorShape = shapeType;
			});

		//connectorBoundsRate
		val = readValue(BhParams.NodeStyleDef.keyNameConnectorBoundsRate, Number.class, jsonObj, fileName);
		val.ifPresent(connectorBoundsRate -> bhNodeViewStyle.connectorBoundsRate = ((Number)connectorBoundsRate).doubleValue());

		//DrawBody
		val = readValue(BhParams.NodeStyleDef.keyNameDrawBody, Boolean.class, jsonObj, fileName);
		val.ifPresent(drawBody -> bhNodeViewStyle.drawBody = ((Boolean)drawBody));
		
		//bodyCssClass
		val = readValue(BhParams.NodeStyleDef.keyNameCssClass, String.class, jsonObj, fileName);
		val.ifPresent(bodyCssClass -> bhNodeViewStyle.cssClass = (String)bodyCssClass);
		
		//connective
		Optional<Object> connectiveOpt = readValue(BhParams.NodeStyleDef.keyNameConnective, ScriptObjectMirror.class, jsonObj, fileName);
		connectiveOpt.ifPresent(connective -> fillConnectiveParams(bhNodeViewStyle.connective, (ScriptObjectMirror)connective, fileName));

		//textField
		Optional<Object> textFieldOpt = readValue(BhParams.NodeStyleDef.keyNameTextField, ScriptObjectMirror.class, jsonObj, fileName);
		textFieldOpt.ifPresent(textField -> fillTextFieldParams(bhNodeViewStyle.textField, (ScriptObjectMirror)textField, fileName));
		
		//label
		Optional<Object> labelOpt = readValue(BhParams.NodeStyleDef.keyNameLabel, ScriptObjectMirror.class, jsonObj, fileName);
		labelOpt.ifPresent(label -> fillLabelParams(bhNodeViewStyle.label, (ScriptObjectMirror)label, fileName));
		
		//comboBox
		Optional<Object> comboBoxOpt = readValue(BhParams.NodeStyleDef.keyNameComboBox, ScriptObjectMirror.class, jsonObj, fileName);
		comboBoxOpt.ifPresent(comboBox -> fillComboBoxParams(bhNodeViewStyle.comboBox, (ScriptObjectMirror)comboBox, fileName));
		
		//imitation
		Optional<Object> imitationOpt = readValue(BhParams.NodeStyleDef.keyNameImitation, ScriptObjectMirror.class, jsonObj, fileName);
		imitationOpt.ifPresent(imitation -> fillImitationParams(bhNodeViewStyle.imitation, (ScriptObjectMirror)imitation, fileName));
		
		return Optional.of(bhNodeViewStyle);
	}

	/**
	 * BhNodeViewStyle.Connective にスタイル情報を格納する
	 * @param jsonObj key = "connective" の value であるオブジェクト
	 * @param fileName jsonObj が記述してある .JSON ファイルの名前
	 * */
	private static void fillConnectiveParams(BhNodeViewStyle.Connective connectiveStyle, ScriptObjectMirror jsonObj, String fileName) {
		// inner		
		Optional<Object> val = readValue(BhParams.NodeStyleDef.keyNameInner, ScriptObjectMirror.class, jsonObj, fileName);
		val.ifPresent(innerArrange -> fillArrangementParams(connectiveStyle.inner, (ScriptObjectMirror)innerArrange, fileName));
		
		// outer
		val = readValue(BhParams.NodeStyleDef.keyNameOuter, ScriptObjectMirror.class, jsonObj, fileName);
		val.ifPresent(outerArrange -> fillArrangementParams(connectiveStyle.outer, (ScriptObjectMirror)outerArrange, fileName));

	}
	
	/**
	 * BhNodeViewStyle.Arrangement にスタイル情報を格納する
	 * @param arrangement 並べ方に関するパラメータの格納先
	 * @param jsonObj key = "inner" または "outer" の value であるオブジェクト
	 * @param fileName jsonObj が記述してある .JSON ファイルの名前
	 * */
	private static void fillArrangementParams(BhNodeViewStyle.Arrangement arrangement, ScriptObjectMirror jsonObj, String fileName) {
		
		//interval
		Optional<Object> val = readValue(BhParams.NodeStyleDef.keyNameInterval, Number.class, jsonObj, fileName);
		val.ifPresent(interval -> {arrangement.interval = ((Number)interval).doubleValue() * BhParams.nodeScale;});

		//arrangement
		val = readValue(BhParams.NodeStyleDef.keyNameArrangement, String.class, jsonObj, fileName);
		val.ifPresent(childArrange -> {
				String arrangeStr = (String)childArrange;
				if (arrangeStr.equals(BhParams.NodeStyleDef.valNameRow)) {
					arrangement.arrangement = CHILD_ARRANGEMENT.ROW;
				}
				else if (arrangeStr.equals(BhParams.NodeStyleDef.valNameColumn)) {
					arrangement.arrangement = CHILD_ARRANGEMENT.COLUMN;
				}
				else {
					MsgPrinter.instance.ErrMsgForDebug("\"" + BhParams.NodeStyleDef.keyNameArrangement + "\"" + " (" + arrangeStr + ") " + "format is invalid.  " + "(" + fileName + ")");
				}
			});
		
		//cnctrNameList
		val = readValue(BhParams.NodeStyleDef.keyNameConnectorList, ScriptObjectMirror.class, jsonObj, fileName);
		val.ifPresent(cnctrs -> {
			ScriptObjectMirror cnctrList = (ScriptObjectMirror)cnctrs;
			if (cnctrList.isArray()) {
				cnctrList.values().forEach(cnctrName -> arrangement.cnctrNameList.add(cnctrName.toString()));
			}
		});
		
		//subGroup
		int groupID = 0;
		while (true) {
			String subGroupKeyName = BhParams.NodeStyleDef.keyNameSubGroup + groupID;
			val = readValue(subGroupKeyName, ScriptObjectMirror.class, jsonObj, fileName);
			if (val.isPresent()) {
				Arrangement subGroup = new Arrangement();
				fillArrangementParams(subGroup, (ScriptObjectMirror)val.get(), fileName);
				arrangement.subGroup.add(subGroup);			
			}
			else {
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
	private static void fillImitationParams(BhNodeViewStyle.Imitation imitation, ScriptObjectMirror jsonObj, String fileName) {

		//buttonPosX
		Optional<Object> val = readValue(BhParams.NodeStyleDef.keyNameButtonPosX, Number.class, jsonObj, fileName);
		val.ifPresent(btnPosX -> {imitation.buttonPosX = ((Number)btnPosX).doubleValue() * BhParams.nodeScale;});
		
		//buttonPosY
		val = readValue(BhParams.NodeStyleDef.keyNameButtonPosY, Number.class, jsonObj, fileName);
		val.ifPresent(btnPosY -> {imitation.buttonPosY = ((Number)btnPosY).doubleValue() * BhParams.nodeScale;});
		
		//buttonCssClass
		val = readValue(BhParams.NodeStyleDef.keyNameCssClass, String.class, jsonObj, fileName);
		val.ifPresent(buttonCssClass -> imitation.cssClass = (String)buttonCssClass);
	}
	
	/**
	 * BhNodeViewStyle.TextField を埋める
	 * @param textField jsonオブジェクトから読み取った内容を格納するオブジェクト
	 * @param jsonObj key = "textField" の value であるオブジェクト
	 * @param fileName jsonObj が記述してある .json ファイルの名前
	 * */
	private static void fillTextFieldParams(BhNodeViewStyle.TextField textField, ScriptObjectMirror jsonObj, String fileName) {

		//whiteSpaceMargine
		Optional<Object> val = readValue(BhParams.NodeStyleDef.keyNameWhiteSpaceMargine, Number.class, jsonObj, fileName);
		val.ifPresent(wsMargine -> textField.whiteSpaceMargine = ((Number)wsMargine).doubleValue());
		
		//minWhiteSpace
		val = readValue(BhParams.NodeStyleDef.keyNameMinWhiteSpace, Number.class, jsonObj, fileName);
		val.ifPresent(minWS -> textField.minWhiteSpace = ((Number)minWS).doubleValue());
		
		//cssClass		
		val = readValue(BhParams.NodeStyleDef.keyNameCssClass, String.class, jsonObj, fileName);
		val.ifPresent(textCssClass -> textField.cssClass = (String)textCssClass);
	}
	
	/**
	 * BhNodeViewStyle.Label を埋める
	 * @param label jsonオブジェクトから読み取った内容を格納するオブジェクト
	 * @param jsonObj key = "label" の value であるオブジェクト
	 * @param fileName jsonObj が記述してある .json ファイルの名前
	 * */
	private static void fillLabelParams(BhNodeViewStyle.Label textField, ScriptObjectMirror jsonObj, String fileName) {
		//cssClass		
		Optional<Object> val = readValue(BhParams.NodeStyleDef.keyNameCssClass, String.class, jsonObj, fileName);
		val.ifPresent(textCssClass -> textField.cssClass = (String)textCssClass);
	}
	
	/**
	 * BhNodeViewStyle.ComboBox を埋める
	 * @param comboBox jsonオブジェクトから読み取った内容を格納するオブジェクト
	 * @param jsonObj key = "comboBox" の value であるオブジェクト
	 * @param fileName jsonObj が記述してある .json ファイルの名前
	 * */
	private static void fillComboBoxParams(BhNodeViewStyle.ComboBox comboBox, ScriptObjectMirror jsonObj, String fileName) {
		//cssClass
		Optional<Object> val = readValue(BhParams.NodeStyleDef.keyNameCssClass, String.class, jsonObj, fileName);
		val.ifPresent(comboBoxCssClass -> comboBox.cssClass = (String)comboBoxCssClass);
	}
	
	/**
	 * Json オブジェクトからエラーチェック付きで Value を読む
	 * @param keyName keyの名前
	 * @param valueType 想定される value の型 (String, Number, Boolean, ScriptObjectMirror, ...)
	 * @param jsonObj key と value が格納されているJson オブジェクト
	 * @param fileName jsonObj を読み取ったファイルの名前
	 * @return JsonValue オブジェクト (オプション)
	 * */
	private static Optional<Object> readValue(String keyName, Class valueType, ScriptObjectMirror jsonObj, String fileName) {
		
		Object val = jsonObj.get(keyName);
		if (val == null)
			return Optional.empty();
		
		if (!valueType.isAssignableFrom(val.getClass())) {
			MsgPrinter.instance.ErrMsgForDebug("The type of " + keyName + " must be " + valueType.getSimpleName() + ".  \n"
										+"The actual type is " + val.getClass().getSimpleName() + ". " + "(" + fileName + ")");
			return Optional.empty();
		}
		return Optional.of(val);
	}

	/**
	 * ノードIDとノードスタイルID のペアを登録する
	 * @param nodeID ノードID (bhNodeID属性)
	 * @param nodeStyleID ノードスタイルID (nodeStyleID属性)
	 * */
	public static void putNodeID_NodeStyleID(String nodeID, String nodeStyleID) {
		nodeID_nodeStyleID.put(nodeID, nodeStyleID);
	}

	/**
	 * ノードID から ノードスタイルオブジェクトを取得する
	 * @param nodeID ノードID (bhNodeID属性)
	 * @return ノードスタイルオブジェクト
	 * */
	public static BhNodeViewStyle getNodeViewStyleFromNodeID(String nodeID) {

		String nodeStyleID = nodeID_nodeStyleID.get(nodeID);
		BhNodeViewStyle nodeStyle = nodeStyleID_nodeStyleTemplate.get(nodeStyleID);
		return new BhNodeViewStyle(nodeStyle);
	}

	/**
	 * 登録された全てのBhNodeIDに対してノードスタイルのテンプレートが存在するかどうかチェック
	 * @return 登録された全てのBhNodeIDに対してノードスタイルのテンプレートが存在する場合 true
	 * */
	public static boolean checkNodeIdAndNodeTemplate() {

		return nodeID_nodeStyleID.values().stream().map(nodeStyleID -> {
				if (!nodeStyleID_nodeStyleTemplate.containsKey(nodeStyleID)) {
					MsgPrinter.instance.ErrMsgForDebug("The node style file " + "(" + nodeStyleID +")" + " is not found among *.json files");
					return false;
				}
				else {
					return true;
				}
			}).allMatch(bool -> bool);
	}
}
