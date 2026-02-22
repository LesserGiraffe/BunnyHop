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

import static net.seapanda.bunnyhop.utility.function.ThrowingConsumer.unchecked;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import java.io.FileWriter;
import java.io.IOException;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import net.seapanda.bunnyhop.bhprogram.runtime.BhRuntimeType;
import net.seapanda.bunnyhop.ui.model.NodeManipulationMode;

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

  /**
   * BhSettings の全 static 変数を JSON ファイルに書き出す.
   * 内部クラスの深さに関係なく再帰的に処理する.
   *
   * @param filePath 出力先の JSON ファイルパス
   * @throws IOException ファイル書き込みに失敗した場合
   */
  public static void exportToJson(Path filePath) throws IOException {
    Map<String, Object> settingsMap = collectClassFields(BhSettings.class);
    Gson gson = new GsonBuilder().setPrettyPrinting().create();
    try (FileWriter writer = new FileWriter(filePath.toFile())) {
      gson.toJson(settingsMap, writer);
    }
  }

  /** 引数で指定したクラスの static フィールドと内部クラスを再帰的に Map に収集する. */
  private static Map<String, Object> collectClassFields(Class<?> clazz) {
    Map<String, Object> map = new LinkedHashMap<>();

    // static フィールドを収集
    for (Field field : clazz.getDeclaredFields()) {
      int modifiers = field.getModifiers();
      if (!Modifier.isStatic(modifiers)
          || field.isSynthetic()
          || field.isAnnotationPresent(PreventExport.class)) {
        continue;
      }

      try {
        field.setAccessible(true);
        map.put(field.getName(), field.get(null));
      } catch (Exception e) { /* Do nothing. */ }
    }

    // 内部クラスを再帰的に処理
    for (Class<?> innerClass : clazz.getDeclaredClasses()) {
      Map<String, Object> innerMap = collectClassFields(innerClass);
      if (!innerMap.isEmpty()) {
        map.put(innerClass.getSimpleName(), innerMap);
      }
    }
    return map;
  }

  /**
   * JSON ファイルから設定を読み込み、BhSettings の static 変数に格納する.
   *
   * @param filePath 読み込む JSON ファイルパス
   * @throws IOException ファイル読み込みに失敗した場合
   */
  public static void importFromJson(Path filePath) throws IOException {
    Gson gson = new Gson();
    try (var reader = Files.newBufferedReader(filePath)) {
      @SuppressWarnings("unchecked")
      Map<String, Object> settingsMap = gson.fromJson(reader, Map.class);
      applyFieldsToClass(BhSettings.class, settingsMap);
    }
  }

  /** Map の内容を指定クラスの static フィールドに再帰的に適用する. */
  private static void applyFieldsToClass(Class<?> clazz, Map<String, Object> map) {
    if (map == null) {
      return;
    }

    for (Field field : clazz.getDeclaredFields()) {
      if (!Modifier.isStatic(field.getModifiers()) || field.isSynthetic()) {
        continue;
      }
      try {
        field.setAccessible(true);
        readValue(field.getName(), field.getType(), map)
            .ifPresent(unchecked(val -> field.set(null, val)));
      } catch (Exception e) { /* Do Nothing */ }
    }

    // 内部クラスを再帰的に処理
    for (Class<?> innerClass : clazz.getDeclaredClasses()) {
      String className = innerClass.getSimpleName();
      if (map.containsKey(className) && map.get(className) instanceof Map) {
        @SuppressWarnings("unchecked")
        Map<String, Object> innerMap = (Map<String, Object>) map.get(className);
        applyFieldsToClass(innerClass, innerMap);
      }
    }
  }

  /** {@code name} と {@code type} で指定したフィールドの値を {@code map} から読みだして格納する. */
  private static Optional<Object> readValue(String name, Class<?> type, Map<String, Object> map)
      throws ReflectiveOperationException {
    if (!map.containsKey(name)) {
      return Optional.empty();
    }
    return Optional.ofNullable(convertValue(map.get(name), type));
  }

  /** JSON から読み込んだ値を適切な型に変換する. */
  private static Object convertValue(Object value, Class<?> targetType)
      throws ReflectiveOperationException {
    if (value == null) {
      return null;
    }

    // enum 型の場合は of メソッドを使用
    if (targetType.isEnum()) {
      Method ofMethod = targetType.getMethod("valueOf", String.class);
      return ofMethod.invoke(null, value.toString());
    }

    // プリミティブ型およびラッパー型の変換
    if (targetType == int.class || targetType == Integer.class) {
      return ((Number) value).intValue();
    }
    if (targetType == long.class || targetType == Long.class) {
      return ((Number) value).longValue();
    }
    if (targetType == double.class || targetType == Double.class) {
      return ((Number) value).doubleValue();
    }
    if (targetType == float.class || targetType == Float.class) {
      return ((Number) value).floatValue();
    }
    if (targetType == boolean.class || targetType == Boolean.class) {
      return value;
    }
    if (targetType == String.class) {
      return value.toString();
    }

    return value;
  }
}
