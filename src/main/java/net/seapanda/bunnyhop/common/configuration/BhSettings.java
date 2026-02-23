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

import net.seapanda.bunnyhop.bhprogram.runtime.BhRuntimeType;
import net.seapanda.bunnyhop.ui.model.NodeManipulationMode;
import net.seapanda.bunnyhop.utility.serialization.PreventExport;

/**
 * BunnyHop の設定一式をまとめたクラス.
 *
 * @author K.Koike
 */
public class BhSettings {

  public static String language = "Japanese";

  /** BhSimulator に関するパラメータ. */
  public static class BhSimulator {
    /** BhSimulator 初期化待ちタイムアウト (sec). */
    public static volatile int initTimeout = 5;
    /** BhProgram の開始時に BhSimulator をフォーカスするかどうか. */
    public static volatile boolean focusOnStartBhProgram = false;
    /** BhSimulator に変化があったとき BhSimulator をフォーカスするかどうか. */
    public static volatile boolean focusOnSimulatorChanged = true;
  }

  /** デバッグに関するパラメータ. */
  public static class Debug {
    /** コールスタックに表示するデフォルトの最大要素数. */
    public static volatile int maxCallStackItems = 32;
    /** ブレークポイントの設定が有効かどうか. */
    public static volatile boolean canSetBreakpoint = false;
    /** デバッグウィンドウが表示されているかどうか. */
    public static volatile boolean isDebugWindowVisible = false;
    /** リスト変数を階層表示する際に, 各階層で表示可能な最大の子要素の数. */
    public static volatile int maxListTreeChildren = 100;
    /** 表示されるエラーメッセージの最大文字数. */
    public static volatile int maxErrMsgChars = 4096;
  }

  /** BhRuntime に関するパラメータ. */
  public static class BhRuntime {
    /** 現在制御対象になっている BhRuntime の種類. */
    @PreventExport
    public static volatile BhRuntimeType currentBhRuntimeType = BhRuntimeType.LOCAL;
  }

  /** UI に関するパラメータ. */
  public static class Ui {
    @PreventExport
    public static volatile NodeManipulationMode nodeManipMode = NodeManipulationMode.MODE_0;
    /** 現在選択されているワークスペースでノードが移動したとき, それに視点を合わせる. */
    public static volatile boolean trackNodeInCurrentWorkspace = false;
    /** 現在選択されていないワークスペースでノードが移動したとき, それに視点を合わせる. */
    public static volatile boolean trackNodeInInactiveWorkspace = true;
    /** 現在のノード選択ビューの拡大・縮小レベル. */
    public static volatile int currentNodeSelectionViewZoomLevel = -1;
    /** 現在のワークスペースの拡大・縮小レベル. */
    public static volatile int currentWorkspaceZoomLevel = -1;
  }
}
