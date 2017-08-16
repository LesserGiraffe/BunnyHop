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
package pflab.bunnyhop.common;

import java.rmi.registry.Registry;

/**
 * パラメータ一式をまとめたクラス
 * @author K.Koike
 */
public class BhParams {

	public static String applicationName = "BunnyHop";
	public static double defaultAppWidthRate = 0.7; // !< 起動時の画面幅のディスプレイに対する割合
	public static double defaultAppHeightRate = 0.7; // !< 起動時の画面高さのディスプレイに対する割合
	public static double defaultWorkspaceWidth = 200 * Util.rem;
	public static double defaultWorkspaceHeight = 200 * Util.rem;
	public static double defaultVerticalDivPos = 0.85;	//ワークスペースとメッセージエリアを分けるディバイダの初期位置
	public static double nodeScale = 0.5 * Util.rem;
	public static double wsMagnification = 1.2;		//ctrl + マウスホイールや拡大, 縮小ボタンを押したときの拡大縮小倍率
	public static double maxZoomLevel = 20;		//!< 最大拡大レベル
	public static double minZoomLevel = -20;	//!< 最小拡大レベル
	public static double replacedNodePos = 2.0 * BhParams.nodeScale;	//!< 入れ替えられたノードがワークスペースに移ったときの元の位置に対する位置 (単位rem)
	public static double bhNodeSpaceOnSelectionPanel = 2.0 * BhParams.nodeScale;		//!< BhNode選択パネル上でのBhNode間のスペース
	public static int numTimesMaxUndo = 128;	//!< undo 可能な最大回数
	public static int maxBottomTextAreaChars = 4096*4;	//!< 下部テキストエリアの最大表示文字数
	public static int maxTextMsgQueueSize = 2048;	//!< 下部テキストエリアの表示文字列バッファサイズ
	public static int numDivOfQTreeSpace = 4;	//!< 4分木空間の分割数 (2^numDivOfQTreeSpace)^2
	public static double maxWorkspaceSizeLevel = 3;		//!< ワークスペースの最大の大きさレベル
	public static double minWorkspaceSizeLevel = -1;	//!< ワークスペースの最小の大きさレベル
	public static String mainWorkspaceName = "main";	//!< 最初からあるワークスペースの名前
	public static int executorShutdownTimeout = 5;	//!< ExecutorService のシャットダウンを待つ時間 (sec)
	
	/**
	 * ファイルパス関連のパラメータ
	 */
	public static class Path {
		public static String viewDir = "BhView"; //!< fxmlとcssファイルのあるフォルダ名
		public static String foundationFxml = "Foundation.fxml"; //!< アプリの基底部分のビューが定義してあるfxmlファイルの名前
		public static String workspaceFxml = "Workspace.fxml"; // !< ワークスペース部分のビューが定義してあるfxmlファイルの名前
		public static String nodeSelectionPanelFxml = "NodeSelectionPanel.fxml";	//!< BhNodeテンプレートリスト部分のビューが定義してあるfxmlの名前
		public static String imitButtonFXML = "ImitButton.fxml";	//!< イミテーションボタンが定義してあるfxmlの名前
		public static String cssDir = "css";	//!< cssファイルのあるフォルダ名
		public static String fxmlDir = "fxml";	//!< fxmlファイルのあるフォルダ名
		public static String bhDefDir = "BhDefine"; //!< ノードやコネクタの定義フォルダがあるフォルダ名
		public static String nodeDefDir = "Node"; //!< ノード定義ファイルがあるフォルダ名
		public static String connectorDefDir = "Connector"; //!< コネクタ定義ファイルがあるフォルダ名
		public static String nodeStyleDefDir = "NodeStyle"; //!< ノードのスタイル定義ファイルがあるフォルダ名
		public static String javascriptDir = "Functions";	//!< Javascript コードが書かれたファイルのあるトップフォルダ
		public static String TemplateListDir = "TemplateList";	//ノードテンプレートの配置情報が書かれたファイルがあるフォルダ名
		public static String nodeTemplateListJson = "NodeTemplateList.json";	//!< ノードテンプレートの配置情報が書かれたファイルの名前
		public static String commonJS = "common.js";
		public static String compiler = "Compiler";
		public static String compiled = "Compiled";
		public static String commonCode = "CommonCode.js";
		public static String appFileName = "BhAppScript.js";
		public static String remoteDir = "Remote";
	}

	/**
	 * ノードやコネクタ定義のパラメータ
	 */
	public static class BhModelDef {
		public static String elemNameNode = "Node";
		public static String elemNameSection = "Section";
		public static String elemNameConnector = "Connector";
		public static String elemNamePrivateConnector = "PrivateConnector";
		public static String elemNameConnectorSection = "ConnectorSection";
		public static String elemNameImitation = "Imitation";
		public static String elemNameItems = "Items";
		public static String elemNameString = "String";
		public static String attrNameBhNodeID = "bhNodeID";
		public static String attrNameBhConnectorID = "bhConnectorID";
		public static String attrNameDefaultBhNodeID = "defaultBhNodeID";
		public static String attrNameInitialBhNodeID = "initialBhNodeID";
		public static String attrNameType = "type";
		public static String attrNameName = "name";
		public static String attrNameUpperLimit = "upperLimit";
		public static String attrNameLowerLimit = "lowerLimit";
		public static String attrNameFixed = "fixed";
		public static String attrNameDefaultStr = "defaultStr";
		public static String attrNameOnTextInput = "onTextInput";
		public static String attrNameOnCompiled = "onCompiled";
		public static String attrNameOnMovedFromChildToWS = "onMovedFromChildToWS";
		public static String attrNameOnMovedToChild = "onMovedToChild";
		public static String attrNameOnReplaceabilityChecked = "onReplaceabilityChecked";
		public static String attrNameImitationNodeID = "imitationNodeID";
		public static String attrNameCanCreateImitManually = "canCreateImitManually";
		public static String attrNameNodeInputControl = "nodeInputControl";
		public static String attrNameInitString = "initString";
		public static String attrNameImitationTag = "imitationTag";
		public static String attrNameImitScopeName = "imitScopeName";
		public static String attrNameValue = "value";
		public static String attrValueConnective = "connective";
		public static String attrValueTextField = "textField";
		public static String attrValueComboBox = "comboBox";
		public static String attrValueLabel = "label";
		public static String attrValueVoid = "void";
		public static String attrValueTrue = "true";
		public static String attrValueFalse = "false";
		public static String attrValueDefaultNodeStyleID = "";
		public static String attrValueTagManual = "tagManual";
		public static String arrtValueInitialBhNodeID = "initialBhNodeID";
	}

	/**
	 * javascript コードに内部でも使うキーワード
	 * */
	public static class JsKeyword {
		public static String keyBhThis = "bhThis";	//!< スクリプトの呼び出し元オブジェクト
		public static String keyBhNodeHandler = "bhNodeHandler";
		public static String keyBhNodeView = "bhNodeView";
		public static String keyBhMsgTransporter = "bhMsgTransporter";
		public static String keyBhText = "bhText";	//!< TextNode のString型フィールドアクセス用キーワード
		public static String keyBhOldParent = "bhOldParent";
		public static String keyBhOldRoot = "bhOldRoot";
		public static String keyBhOldNodeID = "bhOldNodeID";
		public static String keyBhNewNodeID = "bhNewNodeID";
		public static String keyBhReplacedNewNode = "bhReplacedNewNode";
		public static String keyBhReplacedOldNode = "bhReplacedOldNode";
		public static String keyBhManuallyReplaced = "bhManuallyReplaced";	//!< 手動で子ノードからワークスペースに移動したかどうかのフラグ名
		public static String keyBhLocalContext = "bhLocalContext";
		public static String keyBhNextSymbolName = "bhNextSymbolName";
		public static String keyBhUserOpeCmd = "bhUserOpeCmd";
		public static String keyBhCommon = "bhCommon";
		public static String keyIpAddr = "ipAddr";
		public static String keyUname = "uname";
		public static String keyPassword = "password";
		public static String keyExecExnvironment = "execEnvironment";
		public static String keyBhProgramFilePath = "bhProgramFilePath";
	}

	/**
	 * ノードのスタイル定義のパラメータ
	 */
	public static class NodeStyleDef {
		public static String keyNameNodeStyleID = "nodeStyleID";
		public static String keyNameTopMargine = "topMargin";
		public static String keyNameBottomMargin = "bottomMargin";
		public static String keyNameLeftMargin = "leftMargin";
		public static String keyNameRightMargin = "rightMargin";
		public static String keyNameWidth = "width";
		public static String keyNameHeight = "height";
		public static String keyNameConnectorWidth = "connectorWidth";
		public static String keyNameConnectorHeight = "connectorHeight";
		public static String keyNameConnectorShape = "connectorShape";
		public static String keyNameConnectorBoundsRate = "connectorBoundsRate";
		public static String keyNameDrawBody = "drawBody";
		public static String keyNameCssClass = "cssClass";
		public static String keyNameConnective = "connective";
		public static String keyNameImitation = "imitation";
		public static String keyNameButtonPosX = "buttonPosX";
		public static String keyNameButtonPosY ="buttonPosY";
		public static String keyNameTextField = "textField";
		public static String keyNameLabel = "label";
		public static String keyNameComboBox = "comboBox";
		public static String keyNameWhiteSpaceMargine = "whiteSpaceMargine";
		public static String keyNameMinWhiteSpace = "minWhiteSpace";
		public static String keyNameBackGroundColor = "backGroundColor";
		public static String keyNameFormatErrColor = "formatErrColor";
		public static String keyNameFontSize = "fontSize";
		public static String keyNameFontFamily = "fontFamily";
		public static String keyNameConnectorPos = "connectorPos";
		public static String keyNameConnectorShift = "connectorShift";
		public static String keyNameInner = "inner"; 
		public static String keyNameOuter = "outer";
		public static String keyNameArrangement = "arrangement";
		public static String keyNameInterval = "interval";
		public static String keyNameConnectorList = "connectorList";
		public static String keyNameSubGroup = "subGroup";
		public static String valNameArrow = "ARROW";
		public static String valNameCharT = "CHAR_T";
		public static String valNameCharU = "CHAR_U";
		public static String valNameCharV = "CHAR_V";
		public static String valNameCross = "CROSS";
		public static String valNameDiamond = "DIAMOND";
		public static String valNameHexagon = "HEXAGON";
		public static String valNameInvTrapezoid = "INV_TRAPEZOID";
		public static String valNameInvTriangle = "INV_TRIANGLE";
		public static String valNameNone = "NONE";
		public static String valNameOctagon = "OCTAGON";
		public static String valNamePentagon = "PENTAGON";
		public static String valNameSuare = "SQUARE";
		public static String valNameTrapezoid = "TRAPEZOID";
		public static String valNameTriangle = "TRIANGLE";
		public static String valNameRow = "Row";
		public static String valNameColumn = "Column";
		public static String valNameLeft = "Left";
		public static String valNameTop = "Top";
	}
	
	public static class NodeTemplateList {
		public static String keyNameCssClass = "cssClass";
		public static String keyNameContents = "contents";
	}

	/**
	 * FXMLファイル内のキーワード
	 * */
	public static class Fxml {
		public static String idWsPane = "wsPane";
		public static String idNodeListPanel = "nodeListPanel";
		public static String idNodeCategoryList = "nodeCategoryList";
		public static String idWorkspaceSet = "workspaceSet";
		public static String idLeftSeparator = "leftSeparator";
		public static String idBottomMsgArea = "bottomMsgArea";
	}
	
	public static class CSS {
		public static String pseudoSelected = "selected";
		public static String pseudoHighlightImit = "highlightImit";
		public static String pseudoOverlapped = "overlapped";
		public static String pseudoEmpty = "empty";
		public static String pseudoError = "error";
		public static String classBhNode = "BhNode";
		public static String classVoidNode = "voidNode";
		public static String classComboBoxNode = "comboBoxNode";
		public static String classTextFieldNode = "textFieldNode";
		public static String classLabelNode = "labelNode";
		public static String classConnectiveNode = "ConnectiveNode";
		public static String classSuffixPane = "-Pane";
	}
	
	/**
	 * BunnyHopと連携する外部プログラム関連のパラメータ
	 */
	public static class ExternalProgram {
		
		public static String bhProgramExecEnvironment = "BhProgramExecEnvironment.jar";
		public static int programExecEnvTerminationTimeout = 15;	//!< BhProgramExecEnvironment終了待ちのタイムアウト時間 (sec)
		public static int programExecEnvStartTimeout = 15;	//!< BhProgramExecEnvironment開始待ちのタイムアウト時間 (sec)
		public static int fileCopyTerminationTimeout = 15;	//!< ファイルコピープロセス終了待ちののタイムアウト (sec)
		public static int popRecvDataTimeout = 3;	//!< BhProgram実行環境からの受信データ待ちタイムアウト (sec)
		public static int popSendDataTimeout = 3;	//!< BhProgram実行環境への送信データ待ちタイムアウト (sec)
		public static int tcpPortReadTimeout = 15;	//!< TCPポート読み取りのタイムアウト (sec)
		public static int maxRemoteCmdQueueSize = 2048;
		public static String rmiTcpPortSuffix = "@RmiTcpPort";	//BhProgram実行環境との通信に使うRMIオブジェクトを探す際のTCPポート
		public static String remoteExecCmdGenerator = "remoteExecCmdGenerator.js";	//!< リモートのBhProgram実行環境をスタートさせるコマンドを生成するスクリプト名
		public static String remoteKillCmdGenerator = "remoteKillCmdGenerator.js";	//!< リモートのBhProgram実行環境を終わらせるコマンドを生成するスクリプト名
		public static String copyCmdGenerator = "copyCmdGenerator.js";	//!< リモートのBhProgram実行環境にBhProgramファイルをコピーするコマンドを生成するスクリプト名
	}
}






