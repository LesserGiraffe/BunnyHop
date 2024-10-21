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

package net.seapanda.bunnyhop.common.constant;

/**
 * BunnyHop の定数一式をまとめたクラス.
 *
 * @author K.Koike
 */
public class BhConstants {

  public static final String APPLICATION_NAME = "BunnyHop";
  /** undo 可能な最大回数. */
  public static final int NUM_TIMES_MAX_UNDO = 128;
  /** ExecutorService のシャットダウンを待つ時間 (sec). */
  public static final int EXECUTOR_SHUTDOWN_TIMEOUT = 5;

  /** Look & Feel. */
  public static class LnF {
    /** 起動時の画面幅のディスプレイに対する割合. */
    public static final double DEFAULT_APP_WIDTH_RATE = 0.6;
    /** 起動時の画面高さのディスプレイに対する割合. */
    public static final double DEFAULT_APP_HEIGHT_RATE = 0.6;
    public static final double DEFAULT_WORKSPACE_WIDTH = 200 * Rem.VAL;
    public static final double DEFAULT_WORKSPACE_HEIGHT = 200 * Rem.VAL;
    /** ワークスペースとメッセージエリアを分けるディバイダの初期位置. */
    public static final double DEFAULT_VERTICAL_DIV_POS = 0.85;
    /** ctrl + マウスホイールや拡大, 縮小ボタンを押したときの拡大縮小倍率. */
    public static final double ZOOM_MAGNIFICATION = 1.1;
    /** 拡大レベルの最大値. */
    public static final double MAX_ZOOM_LEVEL = 30;
    /** 拡大レベルの最小値. */
    public static final double MIN_ZOOM_LEVEL = -40;
    public static final int INITIAL_ZOOM_LEVEL = -1;
    /** 4 分木空間の分割数 (2 ^ NUM_DIV_OF_QTREE_SPACE) ^ 2. */
    public static final int NUM_DIV_OF_QTREE_SPACE = 4;
    /** ワークスペースの大きさレベルの最大値. */
    public static final double MAX_WORKSPACE_SIZE_LEVEL = 3;
    /** ワークスペースの大きさレベルの最小値. */
    public static final double MIN_WORKSPACE_SIZE_LEVEL = -1;
    /** 最初からあるワークスペースの名前. */
    public static final String INITIAL_WORKSPACE_NAME = "メイン";
    public static final double NODE_SCALE = 0.5 * Rem.VAL;
    /** 入れ替えられたノードがワークスペースに移ったときの元の位置に対する位置 (rem). */
    public static final double REPLACED_NODE_SHIFT = 2.5 * BhConstants.LnF.NODE_SCALE;
    /** BhNode 選択パネル上での BhNode 間のスペース. */
    public static final double BHNODE_SPACE_ON_SELECTION_PANEL = 2.0 * BhConstants.LnF.NODE_SCALE;
    /** マルチノードシフタの大きさ. */
    public static final double NODE_SHIFTER_SIZE = Rem.VAL * 7.0;
  }

  /** ファイルパス関連のパラメータ. */
  public static class Path {
    /** fxmlとcssファイルのあるフォルダ名. */
    public static final String VIEW_DIR = "BhView";
    /** cssファイルのあるフォルダ名. */
    public static final String CSS_DIR = "css";
    /** fxmlファイルのあるフォルダ名. */
    public static final String FXML_DIR = "fxml";
    /** ノードやコネクタの定義フォルダがあるフォルダ名. */
    public static final String BH_DEF_DIR = "BhDefine";
    /** ノード定義ファイルがあるフォルダ名. */
    public static final String NODE_DEF_DIR = "Node";
    /** コネクタ定義ファイルがあるフォルダ名. */
    public static final String CONNECTOR_DEF_DIR = "Connector";
    /** ノードのスタイル定義ファイルがあるフォルダ名. */
    public static final String NODE_STYLE_DEF_DIR = "NodeStyle";
    /** Javascript コードが書かれたファイルのあるトップフォルダ. */
    public static final String FUNCTIONS_DIR = "Functions";
    /** ノードテンプレートの配置情報が書かれたファイルがあるフォルダ名. */
    public static final String TEMPLATE_LIST_DIR = "TemplateList";
    /** アイコンなどの画像があるフォルダ名. */
    public static final String IMAGES_DIR = "IMAGES";
    public static final String LOG_DIR = "Log";
    /** メッセージ定義ファイルがあるフォルダ名. */
    public static final String MESSAGE_DIR = "Message";
    /** デフォルトメッセージプロパティファイル名. */
    public static final String DEFAULT_MESSAGE_FILE_NAME = "message";
    public static final String LOG_FILE_NAME = "msg";
    public static final String lib = "lib";
    public static final String COMPILED_DIR = "Compiled";
    public static final String REMOTE_DIR = "Remote";
    /** アプリの基底部分のビューが定義してあるfxmlファイルの名前. */
    public static final String FOUNDATION_FXML = "Foundation.fxml";
    /** ワークスペース部分のビューが定義してあるfxmlファイルの名前. */
    public static final String WORKSPACE_FXML = "Workspace.fxml";
    /** マルチノードシフタのビューが定義してあるfxmlファイルの名前. */
    public static final String MULTI_NODE_SHIFTER_FXML = "MultiNodeShifter.fxml";
    /** BhNodeテンプレートリスト部分のビューが定義してあるfxmlの名前. */
    public static final String NODE_SELECTION_PANEL_FXML = "NodeSelectionPanel.fxml";
    /** イミテーションボタンが定義してあるfxmlの名前. */
    public static final String IMIT_BUTTON_FXML = "ImitButton.fxml";
    /** プライベートテンプレートボタンが定義してあるfxmlの名前. */
    public static final String PRIVATE_TEMPLATE_BUTTON_FXML = "PrivateTemplateButton.fxml";
    /** ノードテンプレートの配置情報が書かれたファイルの名前. */
    public static final String NODE_TEMPLATE_LIST_JSON = "NodeTemplateList.json";
    public static final String COMMON_EVENT_JS = "CommonEvent.js";
    public static final String COMMON_CODE_JS = "CommonCode.js";
    public static final String LOCAL_COMMON_CODE_JS = "LocalCommonCode.js";
    public static final String REMOTE_COMMON_CODE_JS = "RemoteCommonCode.js";
    /** BunnyHop が作成した BhProgram のスクリプト名. */
    public static final String APP_FILE_NAME_JS = "BhAppScript.js";
    public static final String GEN_COMPOUND_NODES_JS = "GenCompoundNodes.js";
    /** リモートのBhProgram実行環境をスタートさせるコマンドを生成するスクリプト名. */
    public static final String REMOTE_EXEC_CMD_GENERATOR_JS = "RemoteExecCmdGenerator.js";
    /** リモートのBhProgram実行環境を終わらせるコマンドを生成するスクリプト名. */
    public static final String REMOTE_KILL_CMD_GENERATOR_JS = "RemoteKillCmdGenerator.js";
    /** BunnyHopのアイコン画像名. */
    public static final String BUNNY_HOP_ICON = "BunnyHop16.png";
    /** リモートのBhProgram実行環境が入ったフォルダ名. */
    public static final String REMOTE_BUNNYHOP_DIR = "BunnyHop";
    /** BhProgramのファイルを格納するリモート実行環境の下のフォルダ名. */
    public static final String REMOTE_COMPILED_DIR = "Compiled";
  }

  /** ノードやコネクタ定義のパラメータ. */
  public static class BhModelDef {
    public static final String ELEM_NODE = "Node";
    public static final String ELEM_SECTION = "Section";
    public static final String ELEM_CONNECTOR = "Connector";
    public static final String ELEM_PRIVATE_CONNECTOR = "PrivateConnector";
    public static final String ELEM_CONNECTOR_SECTION = "ConnectorSection";
    public static final String ELEM_IMITATION = "Imitation";
    public static final String ATTR_NODE_STYLE_ID = "nodeStyleID";
    public static final String ATTR_BH_NODE_ID = "bhNodeID";
    public static final String ATTR_BH_CONNECTOR_ID = "bhConnectorID";
    public static final String ATTR_DEFAULT_BHNODE_ID = "defaultBhNodeID";
    public static final String ATTR_NAME_INITIAL_BHNODE_ID = "initialBhNodeID";
    public static final String ATTR_CLASS = "class";
    public static final String ATTR_TYPE = "type";
    public static final String ATTR_NAME = "name";
    public static final String ATTR_FIXED = "fixed";
    public static final String ATTR_ON_TEXT_FORMATTING = "onTextFormatting";
    public static final String ATTR_ON_TEXT_CHECKING = "onTextChecking";
    public static final String ATTR_ON_CONNECTABILITY_CHECKING = "onConnectabilityChecking";
    public static final String ATTR_ON_DEFAULT_BH_NODE_SELECTING = "onDefaultBhNodeSelecting";
    public static final String ATTR_ON_SYNTAX_CHECKING = "onSyntaxChecking";
    public static final String ATTR_ON_MOVED_FROM_CHILD_TO_WS = "onMovedFromChildToWS";
    public static final String ATTR_ON_MOVED_TO_CHILD = "onMovedToChild";
    public static final String ATTR_ON_CHILD_REPLACED = "onChildReplaced";
    public static final String ATTR_ON_DELETION_REQUESTED = "onDeletionRequested";
    public static final String ATTR_ON_CUT_REQUESTED = "onCutRequested";
    public static final String ATTR_ON_COPY_REQUESTED = "onCopyRequested";
    public static final String ATTR_ON_PRIFVATE_TEMPLATE_CREATING = "onPrivateTemplateCreating";
    public static final String ATTR_ON_TEST_OPTIONS_CREATING = "onTextOptionsCreating";
    public static final String ATTR_IMITATION_NODE_ID = "imitationNodeID";
    public static final String ATTR_CAN_CREATE_IMIT_MANUALLY = "canCreateImitManually";
    public static final String ATTR_INIT_STRING = "initString";
    public static final String ATTR_IMITATION_ID = "imitationID";
    public static final String ATTR_IMIT_CNCT_POS = "imitCnctPos";
    public static final String ATTR_IMIT_SCOPE_NAME = "imitScopeName";
    public static final String ATTR_VALUE = "value";
    public static final String ATTR_VAL_TRUE = "true";
    public static final String ATTR_VAL_FALSE = "false";
    public static final String ATTR_VAL_DEFAULT_NODE_STYLE_ID = "";
    public static final String ATTR_VAL_IMIT_ID_MANUAL = "imitIdManual";
    public static final String ATTR_VAL_TAG_REFER_TO_PARENT = "tagReferToParent";
    public static final String ATTR_VAL_CONNECTIVE = "connective";
    public static final String ATTR_VAL_TEXT = "text";
  }

  /** JavaScript コードに内部でも使うキーワード. */
  public static class JsKeyword {
    /** スクリプトの呼び出し元オブジェクト. */
    public static final String KEY_BH_THIS = "bhThis";
    public static final String KEY_BH_NODE_HANDLER = "bhNodeHandler";
    public static final String KEY_BH_MSG_SERVICE = "bhMsgService";
    /** TextNode のString型フィールドアクセス用キーワード. */
    public static final String KEY_BH_TEXT = "bhText";
    public static final String KEY_BH_ADDED_TEXT = "bhAddedText";
    public static final String KEY_BH_OLD_PARENT = "bhOldParent";
    public static final String KEY_BH_OLD_ROOT = "bhOldRoot";
    public static final String KEY_BH_REPLACED_NEW_NODE = "bhReplacedNewNode";
    public static final String KEY_BH_REPLACED_OLD_NODE = "bhReplacedOldNode";
    public static final String KEY_BH_CURRENT_NODE = "bhCurrentNode";
    public static final String KEY_BH_NODE_TO_CONNECT = "bhNodeToConnect";
    public static final String KEY_BH_PARENT_CONNECTOR = "bhParentConnector";
    public static final String KEY_BH_NODE_TO_DELETE = "bhNodeToDelete";
    /** 手動で子ノードからワークスペースに移動したかどうかのフラグ名. */
    public static final String KEY_BH_MANUALLY_REMOVED = "bhManuallyRemoved";
    public static final String KEY_BH_NEXT_SYMBOL_NAME = "bhNextSymbolName";
    public static final String KEY_BH_USER_OPE_CMD = "bhUserOpeCmd";
    public static final String KEY_BH_COMMON = "bhCommon";
    public static final String KEY_BH_NODE_TEMPLATES = "bhNodeTemplates";
    public static final String KEY_BH_CANDIDATE_NODE_LIST = "bhCandidateNodeList";
    public static final String KEY_BH_NODES_TO_DELETE = "bhNodesToDelete";
    public static final String KEY_BH_CAUSE_OF_DELETION = "bhCauseOfDeletion";
    public static final String KEY_BH_LIST_OF_NODES_TO_COMPILE = "bhListOfNodesToCompile";
    public static final String KEY_IP_ADDR = "ipAddr";
    public static final String KEY_UNAME = "uname";
    public static final String KEY_PASSWORD = "password";
    public static final String KEY_BH_PROGRAM_FILE_PATH = "bhProgramFilePath";
    public static final String KEY_BH_IS_ENTIRE_TEXT_FORMATTED = "bhIsEntireTextFormatted";
    public static final String KEY_BH_FORMATTED_TEXT = "bhFormattedText";
  }

  /** ノードのスタイル定義のパラメータ. */
  public static class NodeStyleDef {
    public static final String KEY_PADDING_TOP = "paddingTop";
    public static final String KEY_PADDING_BOTTOM = "paddingBottom";
    public static final String KEY_PADDING_LEFT = "paddingLeft";
    public static final String KEY_PADDING_RIGHT = "paddingRight";
    public static final String KEY_WIDTH = "width";
    public static final String KEY_HEIGHT = "height";
    public static final String KEY_CONNECTOR_WIDTH = "connectorWidth";
    public static final String KEY_CONNECTOR_HEIGHT = "connectorHeight";
    public static final String KEY_CONNECTOR_SHAPE = "connectorShape";
    public static final String KEY_CONNECTOR_SHAPE_FIXED = "connectorShapeFixed";
    public static final String KEY_NOTCH_SHAPE = "notchShape";
    public static final String KEY_NOTCH_SHAPE_FIXED = "notchShapeFixed";
    public static final String KEY_NOTCH_HEIGHT = "notchHeight";
    public static final String KEY_NOTCH_WIDTH = "notchWidth";
    public static final String KEY_GRABBER_SIZE = "grabberSize";
    public static final String KEY_CONNECTOR_BOUNDS_RATE = "connectorBoundsRate";
    public static final String KEY_CSS_CLASS = "cssClass";
    public static final String KEY_CONNECTIVE = "connective";
    public static final String KEY_IMITATION = "imitation";
    public static final String KEY_PRIVATE_TEMPLATE = "privateTemplate";
    public static final String KEY_BUTTON_POS_X = "buttonPosX";
    public static final String KEY_BUTTON_POS_Y = "buttonPosY";
    public static final String KEY_TEXT_FIELD = "textField";
    public static final String KEY_LABEL = "label";
    public static final String KEY_COMBO_BOX = "comboBox";
    public static final String KEY_TEXT_AREA = "textArea";
    public static final String KEY_MIN_WIDTH = "minWidth";
    public static final String KEY_MIN_HEIGHT = "minHeight";
    public static final String KEY_BACK_GROUND_COLOR = "backGroundColor";
    public static final String KEY_FONT_SIZE = "fontSize";
    public static final String KEY_FONT_FAMILY = "fontFamily";
    public static final String KEY_BODY_SHAPE = "bodyShape";
    public static final String KEY_CONNECTOR_POS = "connectorPos";
    public static final String KEY_NOTCH_POS = "notchPos";
    public static final String KEY_CONNECTOR_SHIFT = "connectorShift";
    public static final String KEY_INNER = "inner";
    public static final String KEY_OUTER = "outer";
    public static final String KEY_ARRANGEMENR = "arrangement";
    public static final String KEY_SPACE = "space";
    public static final String KEY_CONNECTOR_LIST = "connectorList";
    public static final String KEY_SUB_GROUP = "subGroup";
    public static final String KEY_COMPONENT = "component";
    public static final String VAL_ROW = "Row";
    public static final String VAL_COLUMN = "Column";
    public static final String VAL_LEFT = "Left";
    public static final String VAL_RIGHT = "Right";
    public static final String VAL_TOP = "Top";
    public static final String VAL_BOTTOM = "Bottom";
    public static final String VAL_RIGHT_TRIANGULAR_NOTCH = "RIGHT_TRIANGULAR_NOTCH";
    public static final String VAL_CONNECTIVE = "Connective";
    public static final String VAL_TEXT_FIELD = "TextField";
    public static final String VAL_COMBO_BOX = "ComboBox";
    public static final String VAL_LABEL = "Label";
    public static final String VAL_TEXT_AREA = "TextArea";
    public static final String VAL_NO_VIEW = "NoView";
    public static final String VAL_NONE = "None";
  }

  /** ノードテンプレートに関するキーワード. */
  public static class NodeTemplate {
    public static final String KEY_CSS_CLASS = "cssClass";
    public static final String KEY_CONTENTS = "contents";
    /** ノード固有のノード選択ビューのカテゴリ名. */
    public static final String PRIVATE_NODE_TEMPLATE = "privateNodeTemplate";
    /** ノード選択ビューのスペースを表すノードの名前. */
    public static final String SELECTION_VIEW_SPACE = "SelectionViewSpace";
  }

  /** FXML ファイル使用されるキーワード. */
  public static class Fxml {
    public static final String ID_WS_PANE = "wsPane";
    public static final String ID_WS_SCROLL_PANE = "wsScrollPane";
    public static final String ID_WORKSPACE_SET_TAB = "workspaceSetTab";
    public static final String ID_NODE_VIEW_SHADOW_PANE = "nodeViewShadowPane";
  }

  /** CSS ファイルで使用されるのキーワード. */
  public static class Css {
    public static final String PSEUDO_SELECTED = "selected";
    public static final String PSEUDO_HIGHLIGHT_IMIT = "highlightImit";
    public static final String PSEUDO_OVERLAPPED = "overlapped";
    public static final String PSEUDO_EMPTY = "empty";
    public static final String PSEUDO_ERROR = "error";
    public static final String PSEUDO_IS_EVEN = "isEven";
    public static final String CLASS_BHNODE = "BhNode";
    public static final String CLASS_VOID_NODE = "voidNode";
    public static final String CLASS_COMBO_BOX_NODE = "comboBoxNode";
    public static final String CLASS_TEXT_FIELD_NODE = "textFieldNode";
    public static final String CLASS_TEXT_AREA_NODE = "textAreaNode";
    public static final String CLASS_LABEL_NODE = "labelNode";
    public static final String CLASS_NO_CONTENT_NODE = "labelNode";
    public static final String CLASS_CONNECTIVE_NODE = "ConnectiveNode";
    public static final String CLASS_SUFFIX_PANE = "-Pane";
    public static final String CLASS_SUFFIX_SYNTAX_ERROR = "-SyntaxError";
    public static final String CLASS_NODE_SHIFTER_LINK = "nodeShifterLink";
    public static final String CLASS_PRIVATE_NODE_TEMPLATE = "privateNodeTemplate";
  }

  /** BhProgram の実行環境に関するパラメータ. */
  public static class BhRuntime {
    public static final String BH_PROGRAM_RUNTIME_JAR = "bhruntimelib.jar";
    public static final String BH_PROGRAM_EXEC_MAIN_CLASS = 
        "net.seapanda.bunnyhop.runtime.AppMain";
    /** リモートの BhRuntime 終了待ちのタイムアウト時間 (sec). */
    public static final int REMOTE_RUNTIME_TERMINATION_TIMEOUT = 10;
    /** リモートの BhRuntime 終了待ちのタイムアウト時間 (sec). */
    public static final int REMOTE_RUNTIME_TERMINATION_TIMEOUT_SHORT = 4;
    /**  終了したプロセスの終了完了待ちタイムアウト時間 (sec). */
    public static final int DEAD_PROC_END_TIMEOUT = 3;
    /** BhProgram 実行環境からの受信データ待ちタイムアウト (sec). */
    public static final int POP_RECV_DATA_TIMEOUT = 3;
    /** BhProgram 実行環境への送信データ待ちタイムアウト (sec). */
    public static final int POP_SEND_DATA_TIMEOUT = 3;
    /** TCPポート読み取りのタイムアウト (sec). */
    public static final int TCP_PORT_READ_TIMEOUT = 15;
    public static final int MAX_REMOTE_CMD_QUEUE_SIZE = 2048;
    /** BhProgram 実行環境との通信に使う RMI オブジェクトを探す際の TCP ポートに付けられる接尾辞. */
    public static final String RMI_TCP_PORT_SUFFIX = "@RmiTcpPort";
    public static final String LOLCAL_HOST = "localhost";
    public static final int SSH_PORT = 22;
  }

  /** BunnyHop が出力するテキストメッセージに関するパラメータ. */
  public static class Message {
    /** メインメッセージエリアの最大表示文字数. */
    public static final int MAX_MAIN_MSG_AREA_CHARS = 131072;
    /** メインメッセージエリアの表示文字列バッファサイズ. */
    public static final int MAX_MAIN_MSG_QUEUE_SIZE = 2048;
    /** ログファイルの最大個数. */
    public static final int MAX_LOG_FILE_NUM = 4;
    /** ログファイル1つあたりの最大バイト数. */
    public static final int LOG_FILE_SIZE_LIMIT = 1024 * 1024;
  }
}
