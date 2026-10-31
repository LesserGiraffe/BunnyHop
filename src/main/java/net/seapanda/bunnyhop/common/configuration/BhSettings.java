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

import java.util.concurrent.atomic.AtomicReference;
import net.seapanda.bunnyhop.bhprogram.runtime.BhRuntimeType;
import net.seapanda.bunnyhop.ui.model.NodeManipulationMode;
import net.seapanda.bunnyhop.utility.concurrent.reference.ConcurrentBoolean;
import net.seapanda.bunnyhop.utility.concurrent.reference.ConcurrentDouble;
import net.seapanda.bunnyhop.utility.concurrent.reference.ConcurrentInteger;
import net.seapanda.bunnyhop.utility.concurrent.reference.ConcurrentString;
import net.seapanda.bunnyhop.utility.concurrent.reference.ConcurrentValue;

/**
 * BunnyHop の設定一式をまとめたクラス.
 *
 * @author K.Koike
 */
public class BhSettings {

  public final ConcurrentString language = new ConcurrentString("Japanese");
  public final Simulator simulator = new Simulator();
  public final Debug debug = new Debug();
  public final transient Runtime runtime = new Runtime();
  public final Ui ui = new Ui();
  public final Search search = new Search();
  public final Undo undo = new Undo();
  /** メインウィンドウ関連のパラメータ. */
  public final Window mainWindow = new Window();
  /** デバッグウィンドウ関連のパラメータ. */
  public final Window debugWindow = new Window();

  /** BhSimulator に関するパラメータ. */
  public static class Simulator {
    /** BhSimulator 初期化待ちタイムアウト (sec). */
    public final ConcurrentInteger initTimeout = new ConcurrentInteger(15);
    /** BhProgram の開始時に BhSimulator にフォーカスするかどうか. */
    public final ConcurrentBoolean focusOnStartBhProgram = new ConcurrentBoolean(false);
  }

  /** デバッグに関するパラメータ. */
  public static class Debug {
    /** コールスタックに表示するデフォルトの最大要素数. */
    public final ConcurrentInteger maxCallStackItems = new ConcurrentInteger(32);
    /** ブレークポイントの設定が有効かどうか. */
    public final ConcurrentBoolean canSetBreakpoint = new ConcurrentBoolean(false);
    /** デバッグウィンドウが表示されているかどうか. */
    public final ConcurrentBoolean debugWindowVisible = new ConcurrentBoolean(false);
    /** リスト変数を階層表示する際に, 各階層で表示可能な最大の子要素の数. */
    public final ConcurrentInteger maxListVarTreeChildren = new ConcurrentInteger(100);
    /** 表示されるエラーメッセージの最大文字数. */
    public final ConcurrentInteger maxErrMsgChars = new ConcurrentInteger(4096);
  }

  /** BhRuntime に関するパラメータ. */
  public static class Runtime {
    /** 現在制御対象になっている BhRuntime の種類. */
    public final transient ConcurrentValue<BhRuntimeType> currentBhRuntimeType =
        new ConcurrentValue<>(BhRuntimeType.LOCAL);
  }

  /** UI に関するパラメータ. */
  public static class Ui {
    public final transient AtomicReference<NodeManipulationMode> nodeManipMode =
        new AtomicReference<>(NodeManipulationMode.MODE_0);
    /** 現在選択されているワークスペースでノードが移動したとき, それに視点を合わせる. */
    public final ConcurrentBoolean trackNodeInCurrentWorkspace = new ConcurrentBoolean(false);
    /** 現在選択されていないワークスペースでノードが移動したとき, それに視点を合わせる. */
    public final ConcurrentBoolean trackNodeInInactiveWorkspace = new ConcurrentBoolean(true);
    /** 現在のノード選択ビューの拡大・縮小レベル. */
    public final ConcurrentInteger currentNodeSelectionViewZoomLevel = new ConcurrentInteger(-1);
    /** 現在のワークスペースの拡大・縮小レベル. */
    public final ConcurrentInteger currentWorkspaceZoomLevel = new ConcurrentInteger(-1);
    /** ノード選択ビューとそれ以外の部分を分ける位置. */
    public final ConcurrentDouble nodeSelectionSplitPos = new ConcurrentDouble(-1);
    /** 通知ビューとそれ以外の部分を分ける位置. */
    public final ConcurrentDouble notificationSplitPos = new ConcurrentDouble(-1);
  }

  /** ウィンドウの状態を保持するクラス. */
  public static class Window {

    /** ウィンドウの幅. */
    public final ConcurrentInteger width = new ConcurrentInteger(Integer.MIN_VALUE);
    /** ウィンドウの高さ. */
    public final ConcurrentInteger height = new ConcurrentInteger(Integer.MIN_VALUE);
    /** ウィンドウの X 座標. */
    public final ConcurrentInteger posX = new ConcurrentInteger(Integer.MIN_VALUE);
    /** ウィンドウの Y 座標. */
    public final ConcurrentInteger posY = new ConcurrentInteger(Integer.MIN_VALUE);
    /** ウィンドウが最大化されていたかどうか. */
    public final ConcurrentBoolean maximized = new ConcurrentBoolean(false);
  }

  /** 検索に関するパラメータ. */
  public static class Search {
    /** メインメッセージエリアにおける文字列検索で表示される結果の最大数. */
    public final ConcurrentInteger maxResultsInMainMessage = new ConcurrentInteger(500);
    /** 変数一覧における文字列検索で表示される結果の最大数. */
    public final ConcurrentInteger maxResultsInVariableInspection = new ConcurrentInteger(500);
    /** エラーノード一覧における文字列検索で表示される結果の最大数. */
    public final ConcurrentInteger maxResultsInErrorNodeList = new ConcurrentInteger(500);
    /** コーススタックビューにおける文字列検索で表示される結果の最大数. */
    public final ConcurrentInteger maxResultsInCallStack = new ConcurrentInteger(500);
    /** ブレークポイント一覧における文字列検索で表示される結果の最大数. */
    public final ConcurrentInteger maxResultsInBreakpointList = new ConcurrentInteger(500);
    /** ノード検索で表示される結果の最大数. */
    public final ConcurrentInteger maxItemsInNodeSearchResult = new ConcurrentInteger(500);
    /** ノード検索結果の 1 項目に表示する文字列の最大文字数. */
    public final ConcurrentInteger maxCharsInNodeSearchResultItem = new ConcurrentInteger(64);
  }

  /** Undo / Redo の設定に関するパラメータ. */
  public static class Undo {
    /** テキストの変更を Undo / Redo の対象にするかどうか. */
    public final ConcurrentBoolean textChangeUndoEnabled = new ConcurrentBoolean(true);
  }
}
