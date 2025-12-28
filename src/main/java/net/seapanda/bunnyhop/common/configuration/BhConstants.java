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

package net.seapanda.bunnyhop.common.configuration;

import java.io.IOException;
import java.nio.file.Files;
import java.util.stream.Stream;
import net.seapanda.bunnyhop.export.SaveDataVersion;
import net.seapanda.bunnyhop.ui.view.Rem;
import net.seapanda.bunnyhop.utility.Utility;
import net.seapanda.bunnyhop.utility.version.AppVersion;

/**
 * BunnyHop の定数一式をまとめたクラス.
 *
 * @author K.Koike
 */
public class BhConstants {

  static {
    var path = java.nio.file.Path.of(Utility.execPath, "version", "system");
    String version = "";
    try (Stream<String> stream = Files.lines(path)) {
      version = stream.findFirst().orElse("");
    } catch (IOException e) { /* do nothing */ }

    if (version.isEmpty()) {
      System.err.println("Cannot read the System version.");
    }
    SYS_VERSION = SystemVersion.of(version);
  }

  public static final String APP_NAME = "BunnyHop";
  /** システムのバージョン. */
  public static final SystemVersion SYS_VERSION;
  /** アプリケーションのバージョン. */
  public static final AppVersion APP_VERSION = AppVersion.of("bh-0.7.4");
  /** セーブデータのバージョン. */
  public static final SaveDataVersion SAVE_DATA_VERSION = SaveDataVersion.of("bhsave-0.1.0");
  /** undo 可能な最大回数. */
  public static final int NUM_TIMES_MAX_UNDO = 128;

  /** UI に関するパラメータ. */
  public static class Ui {
    /** 起動時の画面幅のディスプレイに対する割合. */
    public static final double DEFAULT_APP_WIDTH_RATE = 0.7;
    /** 起動時の画面高さのディスプレイに対する割合. */
    public static final double DEFAULT_APP_HEIGHT_RATE = 0.7;
    public static final double DEFAULT_WORKSPACE_WIDTH = 200 * Rem.VAL;
    public static final double DEFAULT_WORKSPACE_HEIGHT = 200 * Rem.VAL;
    /** ctrl + マウスホイールや拡大, 縮小ボタンを押したときの拡大縮小倍率. */
    public static final double ZOOM_MAGNIFICATION = 1.065;
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
    public static final double NODE_SCALE = 0.5 * Rem.VAL;
    /** 入れ替えられたノードがワークスペースに移ったときの元の位置に対する位置 (rem). */
    public static final double REPLACED_NODE_SHIFT = 2.5 * Ui.NODE_SCALE;
    /** BhNode 選択パネル上での BhNode 間のスペース. */
    public static final double BHNODE_SPACE_ON_SELECTION_VIEW = 2.0 * Ui.NODE_SCALE;
    /** ノードシフタの大きさ. */
    public static final double NODE_SHIFTER_SIZE = Rem.VAL * 5.6;
  }

  /** ファイルパス関連のパラメータ. */
  public static class Path {
    /** ディレクトリ名のリスト. */
    public static class Dir {

      /** fxml と css ファイルがあるディレクトリ. */
      public static final String VIEW = "BhView";
      /** css ファイルがあるディレクトリ. */
      public static final String CSS = "css";
      /** fxmlファイルのあるディレクトリ. */
      public static final String FXML = "fxml";
      /** ノードやコネクタの定義ディレクトリがあるディレクトリ. */
      public static final String BH_DEF = "BhDefine";
      /** ノード定義ファイルがあるディレクトリ. */
      public static final String NODE_DEF = "Node";
      /** コネクタ定義ファイルがあるディレクトリ. */
      public static final String CONNECTOR_DEF = "Connector";
      /** ノードのスタイル定義ファイルがあるディレクトリ. */
      public static final String NODE_STYLE_DEF = "NodeStyle";
      /** ノードやコネクタのイベントハンドラが書かれたファイルがあるディレクトリ. */
      public static final String EVENT_HANDLERS = "EventHandler";
      /** テンプレートノードの配置情報が書かれたファイルがあるディレクトリ. */
      public static final String TEMPLATE_NODE_LIST = "TemplateNodeList";
      /** アイコンなどの画像があるディレクトリ. */
      public static final String IMAGES = "IMAGES";
      /** ログファイルを置くディレクトリ. */
      public static final String LOG = "Log";
      /** BhProgram で使用するライブラリがあるディレクトリ. */
      public static final String LIBS = "Libs";
      /** コンパイルに必要なファイルがあるディレクトリ. */
      public static final String COMPILE = "Compile";
      /** コンパイルしたファイルを置くディレクトリ. */
      public static final String COMPILED = "Compiled";
      public static final String REMOTE = "Remote";
      /** 言語ファイルが格納されたディレクトリ. */
      public static final String LANGUAGE = "Language";
    }

    /** ファイル名のリスト. */
    public static class File {
      /** ログファイルの接頭語. */
      public static final String LOG_MSG = "msg";
      /** アプリの基底部分のビューが定義してあるfxmlファイルの名前. */
      public static final String FOUNDATION_FXML = "Foundation.fxml";
      /** ワークスペース部分のビューが定義してあるfxmlファイルの名前. */
      public static final String WORKSPACE_FXML = "Workspace.fxml";
      /** ノードシフタのビューが定義してあるfxmlファイルの名前. */
      public static final String NODE_SHIFTER_FXML = "NodeShifter.fxml";
      /** BhNodeテンプレートリスト部分のビューが定義してあるfxmlの名前. */
      public static final String NODE_SELECTION_VIEW_FXML = "NodeSelectionView.fxml";
      /** プライベートテンプレートボタンが定義してあるfxmlの名前. */
      public static final String PRIVATE_TEMPLATE_BUTTON_FXML = "PrivateTemplateButton.fxml";
      /** コールスタックビューが定義してあるfxmlの名前. */
      public static final String CALL_STACK_VIEW_FXML = "CallStackView.fxml";
      /** 変数検査ビューが定義してあるfxmlの名前. */
      public static final String VARIABLE_INSPECTION_VIEW_FXML = "VariableInspectionView.fxml";
      /** デバッグウィンドウの基底部分のビューが定義してあるfxmlファイルの名前. */
      public static final String DEBUG_WINDOW_FXML = "DebugWindow.fxml";
      /** テンプレートノードの配置情報が書かれたファイルの名前. */
      public static final String TEMPLATE_NODE_LIST_JSON = "TemplateNodeList.json";
      public static final String COMMON_FUNCS_JS = "CommonFuncs.js";
      /** BunnyHop が作成した BhProgram のスクリプト名. */
      public static final String APP_FILE_NAME_JS = "BhAppScript.js";
      /** リモートの BhProgram 実行環境をスタートさせるコマンドを生成するスクリプト名. */
      public static final String GEN_REMOTE_EXEC_CMD_JS = "GenerateRemoteExecCmd.js";
      /** リモートの BhProgram 実行環境を終わらせるコマンドを生成するスクリプト名. */
      public static final String GEN_REMOTE_KILL_CMD_JS = "GenerateRemoteKillCmd.js";
      /** リモートの BhProgram 実行環境の TCP ポートを取得するコマンドを生成するスクリプト名. */
      public static final String GEN_GET_PORT_CMD_JS = "GenerateGetPortCmd.js";
      /** BhProgram のコピー先パスを生成するスクリプト名. */
      public static final String GEN_REMOTE_DEST_PATH_JS = "GenerateRemoteDestPath.js";
      /** BunnyHopのアイコン画像名. */
      public static final String BUNNY_HOP_ICON = "BunnyHop16.png";
      /** 言語ファイルの名前. */
      public static final String LANGUAGE_FILE = "BunnyHop.json";

      /** BhProgram のライブラリのパス. */
      public static class BhLibs {
        public static final String COMMON_JS = "Common.js";
        public static final String LOCAL_COMMON_JS = "LocalCommon.js";
        public static final String REMOTE_COMMON_JS = "RemoteCommon.js";
        public static final String TEXT_DB_JS = "TextDb.js";
      }
    }
  }

  /** ノードやコネクタ定義のパラメータ. */
  public static class BhModelDef {
    public static final String ELEM_NODE = "Node";
    public static final String ELEM_SECTION = "Section";
    public static final String ELEM_CONNECTOR = "Connector";
    public static final String ELEM_CONNECTOR_PARAM_SET = "ConnectorParamSet";
    public static final String ELEM_CONNECTOR_SECTION = "ConnectorSection";
    public static final String ELEM_DERIVATION = "Derivation";
    public static final String ATTR_NODE_STYLE_ID = "nodeStyleID";
    public static final String ATTR_BH_NODE_ID = "bhNodeID";
    public static final String ATTR_VERSION = "version";
    public static final String ATTR_BH_CONNECTOR_ID = "bhConnectorID";
    public static final String ATTR_DEFAULT_BHNODE_ID = "defaultBhNodeID";
    public static final String ATTR_PARAM_SET_ID = "paramSetID";
    public static final String ATTR_TYPE = "type";
    public static final String ATTR_NAME = "name";
    public static final String ATTR_FIXED = "fixed";
    public static final String ATTR_ON_TEXT_FORMATTING = "onTextFormatting";
    public static final String ATTR_ON_TEXT_CHECKING = "onTextChecking";
    public static final String ATTR_ON_CONNECTABILITY_CHECKING = "onConnectabilityChecking";
    public static final String ATTR_ON_COMPILE_ERROR_CHECKING = "onCompileErrorChecking";
    public static final String ATTR_ON_MOVED_FROM_CHILD_TO_WS = "onMovedFromChildToWS";
    public static final String ATTR_ON_MOVED_FROM_WS_TO_CHILD = "onMovedFromWsToChild";
    public static final String ATTR_ON_CHILD_REPLACED = "onChildReplaced";
    public static final String ATTR_ON_DELETION_REQUESTED = "onDeletionRequested";
    public static final String ATTR_ON_CUT_REQUESTED = "onCutRequested";
    public static final String ATTR_ON_COPY_REQUESTED = "onCopyRequested";
    public static final String ATTR_ON_COMPANION_NODES_CREATING = "onCompanionNodesCreating";
    public static final String ATTR_ON_TEST_OPTIONS_CREATING = "onTextOptionsCreating";
    public static final String ATTR_ON_CREATED_AS_TEMPLATE = "onCreatedAsTemplate";
    public static final String ATTR_ON_UI_EVENT_RECEIVED = "onUiEventReceived";
    public static final String ATTR_ON_ALIAS_ASKED = "onAliasAsked";
    public static final String ATTR_ON_USER_DEFINED_NAME_ASKED = "onUserDefinedNameAsked";
    public static final String ATTR_ON_RELATED_NODES_REQUIRED = "onRelatedNodesRequired";
    public static final String ATTR_DERIVATIVE_ID = "derivativeID";
    public static final String ATTR_TEXT = "text";
    public static final String ATTR_DERIVATION_ID = "derivationID";
    public static final String ATTR_DERIVATIVE_JOINT = "derivativeJoint";
    public static final String ATTR_IMPORT = "import";
    public static final String ATTR_BREAKPOINT = "breakpoint";
    public static final String ATTR_VAL_TRUE = "true";
    public static final String ATTR_VAL_FALSE = "false";
    public static final String ATTR_VAL_DEFAULT_NODE_STYLE_ID = "";
    public static final String ATTR_VAL_CONNECTIVE = "connective";
    public static final String ATTR_VAL_TEXT = "text";
    public static final String ATTR_VAL_SET = "set";
    public static final String ATTR_VAL_IGNORE = "ignore";
    public static final String ATTR_VAL_SPECIFY_PARENT = "specifyParent";
  }

  /** 外部スクリプトで使う識別子名. */
  public static class JsIdName {
    /** スクリプトの呼び出し元オブジェクト. */
    public static final String BH_THIS = "bhThis";
    public static final String BH_NODE_PLACER = "bhNodePlacer";
    /** TextNode のString型フィールドアクセス用キーワード. */
    public static final String BH_TEXT = "bhText";
    public static final String BH_ADDED_TEXT = "bhAddedText";
    public static final String BH_OLD_PARENT = "bhOldParent";
    public static final String BH_OLD_ROOT = "bhOldRoot";
    public static final String BH_REPLACED_NEW_NODE = "bhReplacedNewNode";
    public static final String BH_REPLACED_OLD_NODE = "bhReplacedOldNode";
    public static final String BH_CURRENT_NODE = "bhCurrentNode";
    public static final String BH_NODE_TO_CONNECT = "bhNodeToConnect";
    public static final String BH_PARENT_CONNECTOR = "bhParentConnector";
    public static final String BH_USER_OPE = "bhUserOpe";
    public static final String BH_COMMON = "bhCommon";
    public static final String BH_NODE_FACTORY = "bhNodeFactory";
    public static final String BH_TARGET_NODES = "bhTargetNodes";
    public static final String BH_CAUSE_OF_DELETION = "bhCauseOfDeletion";
    public static final String BH_IS_WHOLE_TEXT_FORMATTED = "bhIsWholeTextFormatted";
    public static final String BH_FORMATTED_TEXT = "bhFormattedText";
    public static final String BH_TEXT_DB = "bhTextDb";
    public static final String BH_UI_EVENT = "bhUiEvent";
    public static final String BH_VISUAL_EFFECT_MANAGER = "bhVisualEffectManager";
    public static final String BH_VISUAL_EFFECT_TYPE = "bhVisualEffectType";
    public static final String BH_VISUAL_EFFECT_TARGET = "bhVisualEffectTarget";
    public static final String IP_ADDR = "ipAddr";
    public static final String UNAME = "uname";
  }

  /** ノードのスタイル定義のパラメータ. */
  public static class NodeStyleDef {
    public static final String KEY_PADDING_TOP = "paddingTop";
    public static final String KEY_PADDING_BOTTOM = "paddingBottom";
    public static final String KEY_PADDING_LEFT = "paddingLeft";
    public static final String KEY_PADDING_RIGHT = "paddingRight";
    public static final String KEY_CONNECTOR_WIDTH = "connectorWidth";
    public static final String KEY_CONNECTOR_HEIGHT = "connectorHeight";
    public static final String KEY_CONNECTOR_SHAPE = "connectorShape";
    public static final String KEY_CONNECTOR_SHAPE_FIXED = "connectorShapeFixed";
    public static final String KEY_NOTCH_SHAPE = "notchShape";
    public static final String KEY_NOTCH_SHAPE_FIXED = "notchShapeFixed";
    public static final String KEY_NOTCH_HEIGHT = "notchHeight";
    public static final String KEY_NOTCH_WIDTH = "notchWidth";
    public static final String KEY_CONNECTOR_BOUNDS_RATE = "connectorBoundsRate";
    public static final String KEY_CSS_CLASS = "cssClass";
    public static final String KEY_CONNECTIVE = "connective";
    public static final String KEY_PRIVATE_TEMPLATE = "privateTemplate";
    public static final String KEY_BREAK_POINT = "breakpoint";
    public static final String KEY_EXEC_STEP = "execStep";
    public static final String KEY_CORRUPTION = "corruption";
    public static final String KEY_ENTRY_POINT = "entryPoint";
    public static final String KEY_TEXT_FIELD = "textField";
    public static final String KEY_LABEL = "label";
    public static final String KEY_COMBO_BOX = "comboBox";
    public static final String KEY_TEXT_AREA = "textArea";
    public static final String KEY_MIN_WIDTH = "minWidth";
    public static final String KEY_MIN_HEIGHT = "minHeight";
    public static final String KEY_BODY_SHAPE = "bodyShape";
    public static final String KEY_CONNECTOR_POS = "connectorPos";
    public static final String KEY_NOTCH_POS = "notchPos";
    public static final String KEY_CONNECTOR_SHIFT = "connectorShift";
    public static final String KEY_CONNECTOR_ALIGNMENT = "connectorAlignment";
    public static final String KEY_INNER = "inner";
    public static final String KEY_OUTER = "outer";
    public static final String KEY_ARRANGEMENT = "arrangement";
    public static final String KEY_BASE_ARRANGEMENT = "baseArrangement";
    public static final String KEY_SPACE = "space";
    public static final String KEY_CONNECTOR_LIST = "connectorList";
    public static final String KEY_SUB_GROUP = "subGroup";
    public static final String KEY_COMPONENT = "component";
    public static final String KEY_EDITABLE = "editable";
    public static final String KEY_COMMON_PART = "commonPart";
    public static final String KEY_SPECIFIC_PART = "specificPart";
    public static final String KEY_RADIUS = "radius";
    public static final String KEY_SIZE = "size";
    public static final String VAL_ROW = "Row";
    public static final String VAL_COLUMN = "Column";
    public static final String VAL_LEFT = "Left";
    public static final String VAL_RIGHT = "Right";
    public static final String VAL_TOP = "Top";
    public static final String VAL_BOTTOM = "Bottom";
    public static final String VAL_TEXT_FIELD = "TextField";
    public static final String VAL_COMBO_BOX = "ComboBox";
    public static final String VAL_LABEL = "Label";
    public static final String VAL_TEXT_AREA = "TextArea";
    public static final String VAL_NONE = "None";
    public static final String VAL_CENTER = "Center";
    public static final String VAL_EDGE = "Edge";
  }

  /** ノード選択ビューに関するキーワード. */
  public static class NodeSelection {
    public static final String KEY_CSS_CLASS = "cssClass";
    public static final String KEY_CONTENTS = "contents";
    /** ノード固有のノード選択ビューのカテゴリ名. */
    public static final String PRIVATE_TEMPLATE_NODE = "privateTemplateNode";
    /** ノード選択ビューのスペースを表すノードの名前. */
    public static final String SELECTION_VIEW_SPACE = "SelectionViewSpace";
  }

  /** UI コンポーネントの ID. */
  public static class UiId {
    public static final String WS_PANE = "wsPane";
    public static final String WS_SCROLL_PANE = "wsScrollPane";
    public static final String WORKSPACE_SET_TAB = "workspaceSetTab";
    public static final String BH_RUNTIME_ERR_MSG = "bhRuntimeErrMsg";
  }

  /** CSS ファイルで使用されるキーワード. */
  public static class Css {
    public static final String PSEUDO_SELECTED = "selected";
    public static final String PSEUDO_OVERLAPPED = "overlapped";
    public static final String PSEUDO_JUMP_TARGET = "jumpTarget";
    public static final String PSEUDO_RELATED_NODE_GROUP = "relatedNodeGroup";
    public static final String PSEUDO_EXEC_STEP = "execStep";
    public static final String PSEUDO_MOVE_GROUP = "moveGroup";
    public static final String PSEUDO_EMPTY = "empty";
    public static final String PSEUDO_ERROR = "error";
    public static final String PSEUDO_IS_EVEN = "isEven";
    public static final String PSEUDO_TEXT_DECORATE = "textDecorate";
    public static final String PSEUDO_UNFIXED_DEFAULT = "unfixedDefault";
    public static final String PSEUDO_COLUMN = "column";
    public static final String PSEUDO_ROW = "row";
    public static final String CLASS_BH_NODE = "BhNode";
    public static final String CLASS_COMBO_BOX_NODE = "comboBoxNode";
    public static final String CLASS_TEXT_FIELD_NODE = "textFieldNode";
    public static final String CLASS_TEXT_AREA_NODE = "textAreaNode";
    public static final String CLASS_LABEL_NODE = "labelNode";
    public static final String CLASS_NO_CONTENT_NODE = "noContentNode";
    public static final String CLASS_CONNECTIVE_NODE = "ConnectiveNode";
    public static final String CLASS_COMPILE_ERROR_MARK = "compileErrorMark";
    public static final String CLASS_TRIANGLE = "triangle";
    public static final String CLASS_EXCLAMATION_BAR = "exclamationBar";
    public static final String CLASS_EXCLAMATION_DOT = "exclamationDot";
    public static final String CLASS_CIRCLE = "circle";

    public static final String CLASS_NODE_SHIFTER_LINK = "nodeShifterLink";
    public static final String CLASS_PRIVATE_NODE_TEMPLATE = "privateNodeTemplate";
    public static final String CALL_STACK_ITEM = "callStackItem";
    public static final String VARIABLE_LIST_ITEM = "variableListItem";
    public static final String BREAKPOINT_LIST_ITEM = "breakpointListItem";
    public static final String ERROR_NODE_LIST_ITEM = "errorNodeListItem";
  }

  /** BhProgram の実行環境に関するパラメータ. */
  public static class BhRuntime {
    public static final String BH_PROGRAM_EXEC_MAIN_CLASS =
        "net.seapanda.bunnyhop.runtime.AppMain";
    
    /** タイムアウト. */
    public static class Timeout {
      /** SSH 接続のタイムアウト時間. (ms) */
      public static final int SSH_CONNECTION = 7000;
      /**  プロセスの終了完了待ちタイムアウト時間 (ms). */
      public static final int PROC_END = 3000;
      /** BhRuntime への送信データ待ちタイムアウト (ms). */
      public static final int SEND_DATA = 1000;
      /** リモートの BhRuntime の起動待ちのタイムアウト時間 (ms). */
      public static final int REMOTE_START = 6000;
      /** リモートの BhRuntime との接続待ちのタイムアウト時間 (ms). */
      public static final int REMOTE_CONNECT = 6000;
      /** リモートの BhRuntime との接続待ちのタイムアウト時間 (ms). */
      public static final int HALT_TRANSCEIVER = 5000;
      /** リモートの BhRuntime 終了待ちのタイムアウト時間 (ms). */
      public static final int REMOTE_TERMINATE = 6000;
      /** リモートの BhRuntime の起動待ちのタイムアウト時間 (ms). */
      public static final int LOCAL_START = 5000;
      /** アプリケーション終了時にリモートの BhRuntime の終了処理を開始するまでの待ち時間 (ms). */
      public static final int REMOTE_END_ON_EXIT = 3000;
    }

    public static final int MAX_REMOTE_CMD_QUEUE_SIZE = 2048;
    /** BhProgram 実行環境との通信に使う RMI オブジェクトを探す際の TCP ポートに付けられる接尾辞. */
    public static final String RMI_TCP_PORT_SUFFIX = "@RmiTcpPort";
    public static final String LOCAL_HOST = "localhost";
    public static final int SSH_PORT = 22;
  }

  /** BunnyHop が出力するテキストメッセージに関するパラメータ. */
  public static class Message {
    /** メインメッセージエリアの最大表示文字数. */
    public static final int MAX_MAIN_MSG_AREA_CHARS = 131072;
    /** ログファイルの最大個数. */
    public static final int MAX_LOG_FILE_NUM = 4;
    /** ログファイル1つあたりの最大バイト数. */
    public static final int LOG_FILE_SIZE_LIMIT = 1024 * 1024;
  }

  /** 言語一覧. */
  public static class Language {
    public static final String JAPANESE = "Japanese";
  }
}
