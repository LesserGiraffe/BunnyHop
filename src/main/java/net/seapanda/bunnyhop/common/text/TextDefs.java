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

package net.seapanda.bunnyhop.common.text;

import net.seapanda.bunnyhop.utility.textdb.JsonTextDatabase;
import net.seapanda.bunnyhop.utility.textdb.TextDatabase;
import net.seapanda.bunnyhop.utility.textdb.TextId;

/**
 * アプリケーションで使用するテキストを集めたクラス.
 *
 * @author K.Koike
 */
public class TextDefs {

  private static volatile TextDatabase db = new JsonTextDatabase("{}");

  /** テキストを取得する際に呼ぶメソッドのインタフェース. */
  @FunctionalInterface
  public interface Getter {
    /**
     * テキストを取得する.
     *
     * @param params 書式付き文字列の中に組み込むデータ
     */
    String get(Object... params);
  }

  /** テキストの取得元となるオブジェクトを設定する. */
  public static void setTextDatabase(TextDatabase db) {
    if (db == null) {
      return;
    }
    TextDefs.db = db;
  }

  /** メニュービューのテキスト. */
  public static class MenuView {
    public static Getter remote = params -> db.get(
        TextId.of("gui", "menu-view", "remote"), params);
    public static Getter local = params -> db.get(
        TextId.of("gui", "menu-view", "local"), params);
  }

  /** デバッガに表示されるテキスト. */
  public static class Debugger {
    public static Getter thread = params -> db.get(TextId.of("gui", "debugger", "thread"));
    public static Getter allThreads = params -> db.get(
        TextId.of("gui", "debugger", "all-threads"));
    public static Getter stackOverflow = params -> db.get(
        TextId.of("gui", "debugger", "stack-overflow"));
    public static Getter outOfMemory = params -> db.get(
        TextId.of("gui", "debugger", "out-of-memory"));
    public static Getter runtimeErrOccurred = params -> db.get(
        TextId.of("gui", "debugger", "runtime-error-occurred"));
    
    /** スレッドの状態を表すテキスト. */
    public static class ThreadStatus {
      public static Getter status = params -> db.get(
          TextId.of("gui", "debugger", "thread-status", "status"));
      public static Getter error = params -> db.get(
          TextId.of("gui", "debugger", "thread-status", "error"));
      public static Getter running = params -> db.get(
          TextId.of("gui", "debugger", "thread-status", "running"));
      public static Getter suspended = params -> db.get(
          TextId.of("gui", "debugger", "thread-status", "suspended"));
      public static Getter finished = params -> db.get(
          TextId.of("gui", "debugger", "thread-status", "finished"));
    }

    /** コールスタックに表示されるテキスト. */
    public static class CallStack {
      public static Getter ellipsis = params -> db.get(
          TextId.of("gui", "debugger", "call-stack", "ellipsis"), params);
      public static Getter unknownNode = params -> db.get(
          TextId.of("gui", "debugger", "call-stack", "unknown-node"), params);
      public static Getter mainRoutine = params -> db.get(
          TextId.of("gui", "debugger", "call-stack", "main-routine"), params);
      public static Getter next = params -> db.get(
          TextId.of("gui", "debugger", "call-stack", "next"), params);
      public static Getter error = params -> db.get(
          TextId.of("gui", "debugger", "call-stack", "error"), params);
    }

    /** ワークスペース選択コンポーネントに表示されるテキスト. */
    public static class WorkspaceSelector {
      public static Getter allWs = params -> db.get(
          TextId.of("gui", "debugger", "workspace-selector", "all-ws"), params);
    }

    /** 変数検査ビューに表示されるテキスト. */
    public static class VarInspection {
      public static Getter localVars = params -> db.get(
          TextId.of("gui", "debugger", "variable-inspection", "local-vars"), params);
      public static Getter globalVars = params -> db.get(
          TextId.of("gui", "debugger", "variable-inspection", "global-vars"), params);
      public static Getter emptyList = params -> db.get(
          TextId.of("gui", "debugger", "variable-inspection", "empty-list"), params);
      public static Getter unknownVar = params -> db.get(
          TextId.of("gui", "debugger", "variable-inspection", "unknown-var"), params);
    }
  }

  /** プロジェクトのセーブに関するメッセージ. */
  public static class Export {

    /** プロジェクトをセーブするか確認するときのメッセージ. */
    public static class AskIfSave {
      public static Getter title = params -> db.get(
          TextId.of("msg", "export", "ask-if-save", "title"), params);
      public static Getter body = params -> db.get(
          TextId.of("msg", "export", "ask-if-save", "body"), params);
    }

    /** 保存先のファイルを選択するときのメッセージ. */
    public static class FileChooser {
      public static Getter title = params -> db.get(
          TextId.of("msg", "export", "file-chooser", "title"), params);
    }
  
    /** 保存するワークスペースがないときのメッセージ. */
    public static class InformNoWsToSave {
      public static Getter title = params -> db.get(
          TextId.of("msg", "export", "inform-no-ws-to-save", "title"), params);
      public static Getter body = params -> db.get(
          TextId.of("msg", "export", "inform-no-ws-to-save", "body"), params);
    }

    /** プロジェクトのセーブに失敗したときのメッセージ. */
    public static class InformFailedToSave {
      public static Getter title = params -> db.get(
          TextId.of("msg", "export", "inform-failed-to-save", "title"), params);
    }

    public static Getter hasSaved = params -> db.get(
        TextId.of("msg", "export", "has-saved"), params);
  }

  /** プロジェクトのロードに関するメッセージ. */
  public static class Import {
    
    /** セーブデータの一部が正常に読めなかった時のメッセージ. */
    public static class AskIfContinue {
      public static Getter title = params -> db.get(
          TextId.of("msg", "import", "ask-if-continue", "title"), params);
      public static Getter body = params -> db.get(
          TextId.of("msg", "import", "ask-if-continue", "body"), params);
      public static Getter someNodesAreMissing = params -> db.get(
          TextId.of("msg", "import", "ask-if-continue", "some-nodes-are-missing"), params);
      public static Getter loadedIncompatibleNodes = params -> db.get(
          TextId.of("msg", "import", "ask-if-continue", "loaded-incompatible-nodes"), params);
      public static Getter loadedCorruptedNodes = params -> db.get(
          TextId.of("msg", "import", "ask-if-continue", "loaded-corrupted-nodes"), params);
      public static Getter overwroteDuplicateInstIds = params -> db.get(
          TextId.of("msg", "import", "ask-if-continue", "changed-duplicate-instance-ids"), params);
    }

    /** 既存のワークスペースを削除するか確認するときのメッセージ. */
    public static class AskIfClearOldWs {
      public static Getter title = params -> db.get(
          TextId.of("msg", "import", "ask-if-keep-existing-ws", "title"), params);
      public static Getter body = params -> db.get(
          TextId.of("msg", "import", "ask-if-keep-existing-ws", "body"), params);
    }

    /** セーブデータの読み込みに失敗したときのメッセージ. */
    public static class Error {      
      public static Getter title = params -> db.get(
          TextId.of("msg", "import", "error", "title"), params);
      public static Getter unsupportedSaveDataVersion = params -> db.get(
          TextId.of("msg", "import", "error", "unsupported-save-data-version"), params);
      public static Getter corruptedSaveData = params -> db.get(
          TextId.of("msg", "import", "error", "corrupted-save-data"), params);
      public static Getter failedToReadSaveFile = params -> db.get(
          TextId.of("msg", "import", "error", "failed-to-read-save-file"), params);
    }

    /** ロードするファイルを選択するときのメッセージ. */
    public static class FileChooser {
      public static Getter title = params -> db.get(
          TextId.of("msg", "import", "file-chooser", "title"), params);
    }
  }

  /** BhProgram の実行環境を操作したときのメッセージ. */
  public static class BhRuntime {
    
    /** ローカル環境で BhProgram を実行する際のメッセージ. */
    public static class Local {
      public static Getter preparingToRun = params -> db.get(
          TextId.of("msg", "bh-runtime", "local", "preparing-to-start"), params);
      public static Getter failedToRun = params -> db.get(
          TextId.of("msg", "bh-runtime", "local", "failed-to-start"), params);
      public static Getter hasStarted = params -> db.get(
          TextId.of("msg", "bh-runtime", "local", "has-started"), params);
      public static Getter hasAlreadyEnded = params -> db.get(
          TextId.of("msg", "bh-runtime", "local", "has-already-ended"), params);
      public static Getter preparingToEnd = params -> db.get(
          TextId.of("msg", "bh-runtime", "local", "preparing-to-end"), params);
      public static Getter failedToEnd = params -> db.get(
          TextId.of("msg", "bh-runtime", "local", "failed-to-end"), params);
      public static Getter hasEnded = params -> db.get(
          TextId.of("msg", "bh-runtime", "local", "has-ended"), params);
      public static Getter hasConnected = params -> db.get(
          TextId.of("msg", "bh-runtime", "local", "has-connected"), params);
      public static Getter failedToConnect = params -> db.get(
          TextId.of("msg", "bh-runtime", "local", "failed-to-connect"), params);
      public static Getter noRuntimeToConnectTo = params -> db.get(
          TextId.of("msg", "bh-runtime", "local", "no-runtime-to-connect-to"), params);
      public static Getter hasDisconnected = params -> db.get(
          TextId.of("msg", "bh-runtime", "local", "has-disconnected"), params);
      public static Getter failedToDisconnect = params -> db.get(
          TextId.of("msg", "bh-runtime", "local", "failed-to-disconnect"), params);
      public static Getter noRuntimeToDisconnectFrom = params -> db.get(
          TextId.of("msg", "bh-runtime", "local", "no-runtime-to-disconnect-from"), params);

    }

    /** リモート環境で BhProgram を実行する際のメッセージ. */
    public static class Remote {
      public static Getter preparingToRun = params -> db.get(
          TextId.of("msg", "bh-runtime", "remote", "preparing-to-start"), params);
      public static Getter failedToRun = params -> db.get(
          TextId.of("msg", "bh-runtime", "remote", "failed-to-start"), params);
      public static Getter hasStarted = params -> db.get(
          TextId.of("msg", "bh-runtime", "remote", "has-started"), params);
      public static Getter preparingToEnd = params -> db.get(
          TextId.of("msg", "bh-runtime", "remote", "preparing-to-end"), params);
      public static Getter failedToEnd = params -> db.get(
          TextId.of("msg", "bh-runtime", "remote", "failed-to-end"), params);
      public static Getter hasEnded = params -> db.get(
          TextId.of("msg", "bh-runtime", "remote", "has-ended"), params);
      public static Getter transferring = params -> db.get(
          TextId.of("msg", "bh-runtime", "remote", "transferring"), params);
      public static Getter failedToTransfer = params -> db.get(
          TextId.of("msg", "bh-runtime", "remote", "failed-to-transfer"), params);      
      public static Getter preparingToConnect = params -> db.get(
          TextId.of("msg", "bh-runtime", "remote", "preparing-to-connect"), params);
      public static Getter hasConnected = params -> db.get(
          TextId.of("msg", "bh-runtime", "remote", "has-connected"), params);    
      public static Getter failedToConnect = params -> db.get(
          TextId.of("msg", "bh-runtime", "remote", "failed-to-connect"), params);
      public static Getter preparingToDisconnect = params -> db.get(
          TextId.of("msg", "bh-runtime", "remote", "preparing-to-disconnect"), params);
      public static Getter hasDisconnected = params -> db.get(
          TextId.of("msg", "bh-runtime", "remote", "has-disconnected"), params);    
      public static Getter failedToDisconnect = params -> db.get(
          TextId.of("msg", "bh-runtime", "remote", "failed-to-disconnect"), params);

      /** リモート環境の BhProgram を停止するか確認する際のメッセージ. */
      public static class AskIfStop {
        public static Getter title = params -> db.get(
            TextId.of("msg", "bh-runtime", "remote", "ask-if-stop", "title"), params);
        public static Getter body = params -> db.get(
            TextId.of("msg", "bh-runtime", "remote", "ask-if-stop", "body"), params);
      }
    }

    /** すでに実行環境に対する特定の操作を行っていることを伝えるメッセージ. */
    public static class AlreadyDoing {
      public static Getter execution = params -> db.get(
          TextId.of("msg", "bh-runtime", "already-doing", "preparation"), params);
      public static Getter termination = params -> db.get(
          TextId.of("msg", "bh-runtime", "already-doing", "termination"), params);
      public static Getter connection = params -> db.get(
          TextId.of("msg", "bh-runtime", "already-doing", "connection"), params);
      public static Getter disconnection = params -> db.get(
          TextId.of("msg", "bh-runtime", "already-doing", "disconnection"), params);
    }

    /** ファイル転送時に出力されるメッセージ. */
    public static class FileTransfer {
      public static Getter stopped = params -> db.get(
          TextId.of("msg", "bh-runtime", "file-transfer", "stopped"), params);
      public static Getter transferring = params -> db.get(
          TextId.of("msg", "bh-runtime", "file-transfer", "transferring"), params);
      public static Getter complete = params -> db.get(
          TextId.of("msg", "bh-runtime", "file-transfer", "complete"), params);
      public static Getter start = params -> db.get(
          TextId.of("msg", "bh-runtime", "file-transfer", "start"), params);
    }

    /** BhProgram の実行環境と通信する際のメッセージ. */
    public static class Communication {
      public static Getter preparingToCommunicate = params -> db.get(
          TextId.of("msg", "bh-runtime", "communication", "preparing-to-communicate"), params);
      public static Getter failedToEstablishConnection = params -> db.get(
          TextId.of("msg", "bh-runtime", "communication", "failed-to-establish-connection"),
          params);
      public static Getter hasSentText = params -> db.get(
          TextId.of("msg", "bh-runtime", "communication", "has-sent-text"), params);
      public static Getter failedToProcessText = params -> db.get(
          TextId.of("msg", "bh-runtime", "communication", "failed-to-process-text"), params);
      public static Getter failedToPushText = params -> db.get(
          TextId.of("msg", "bh-runtime", "communication", "failed-to-push-text"), params);
      public static Getter failedToSendTextForNoConnection = params -> db.get(
          TextId.of("msg", "bh-runtime", "communication", "failed-to-send-text-for-no-connection"),
          params);
      public static Getter otherOpsAreInProgress = params -> db.get(
          TextId.of("msg", "bh-runtime", "communication", "other-ops-are-in-progress"),
          params);
    }
  }

  /** コンパイル時に出力されるメッセージ. */
  public static class Compile {

    /** エラーノードを削除して良いか確認する際のメッセージ. */
    public static class InformCompileError {
      public static Getter title = params -> db.get(
          TextId.of("msg", "compile", "inform-compile-error", "title"), params);
      public static Getter body = params -> db.get(
          TextId.of("msg", "compile", "inform-compile-error", "body"), params);
    }

    /** ファイル書き込みエラーが発生したときのメッセージ. */
    public static class InformFailedToWrite {
      public static Getter title = params -> db.get(
          TextId.of("msg", "compile", "inform-failed-to-write", "title"), params);
    }

    /** 実行可能なノードが存在しないときのメッセージ. */
    public static class InformSelectNodeToExecute {
      public static Getter title = params -> db.get(
          TextId.of("msg", "compile", "inform-no-entry-points", "title"), params);
      public static Getter body = params -> db.get(
          TextId.of("msg", "compile", "inform-no-entry-points", "body"), params);
    }

    public static Getter succeeded = params -> db.get(
        TextId.of("msg", "compile", "succeeded"), params);
  }

  /** ワークスペースの操作に関するメッセージ. */
  public static class Workspace {
    public static Getter initialWsName = params -> db.get(
        TextId.of("msg", "workspace", "initial-ws-name"), params);
    public static Getter defaultWsName = params -> db.get(
        TextId.of("msg", "workspace", "default-ws-name"), params);
    
    /** ワークスペースの名前入力ダイアログで出力されるメッセージ. */
    public static class PromptToNameWs {
      public static Getter title = params -> db.get(
          TextId.of("msg", "workspace", "prompt-to-name-ws", "title"), params);
      public static Getter body = params -> db.get(
          TextId.of("msg", "workspace", "prompt-to-name-ws", "body"), params);
    }

    /** ワークスペースを削除するかどうか確認するときに出力されるメッセージ. */
    public static class AskIfDeleteWs {
      public static Getter title = params -> db.get(
          TextId.of("msg", "workspace", "ask-if-delete-ws", "title"), params);
      public static Getter body = params -> db.get(
          TextId.of("msg", "workspace", "ask-if-delete-ws", "body"), params);
    }
  }

  /** メニューバーから選択できる操作で表示されるメッセージ. */
  public static class MenubarOps {
    public static Getter freeMemory = params -> db.get(
        TextId.of("msg", "menubar-opts", "free-memory"), params);
    
    /** バージョン画面のメッセージ. */
    public static class Version {
      public static Getter title = params -> db.get(
          TextId.of("msg", "menubar-opts", "version", "title"), params);
      public static Getter system = params -> db.get(
          TextId.of("msg", "menubar-opts", "version", "system"), params);
      public static Getter runtime = params -> db.get(
          TextId.of("msg", "menubar-opts", "version", "runtime"), params);
      public static Getter simulator = params -> db.get(
          TextId.of("msg", "menubar-opts", "version", "simulator"), params);
    }
  }

  /** 検索ボックスに表示されるテキスト. */
  public static class SearchBox {
    public static Getter result = params -> db.get(
        TextId.of("gui", "search-box", "result"), params);
    public static Getter resultCount = params -> db.get(
        TextId.of("gui", "search-box", "result-count"), params);
  }
}
