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

package net.seapanda.bunnyhop.configuration;

import static net.seapanda.bunnyhop.common.configuration.BhConstants.Path.Dir.APP_GUI;
import static net.seapanda.bunnyhop.common.configuration.BhConstants.Path.Dir.CSS;
import static net.seapanda.bunnyhop.common.configuration.BhConstants.Path.Dir.FXML;
import static net.seapanda.bunnyhop.common.configuration.BhConstants.Path.Dir.SETTINGS;
import static net.seapanda.bunnyhop.common.configuration.BhConstants.Path.Dir.VIEW;
import static net.seapanda.bunnyhop.common.configuration.BhConstants.Path.File.BH_CONFIGURATOR_CSS;
import static net.seapanda.bunnyhop.common.configuration.BhConstants.Path.File.BH_CONFIGURATOR_FXML;
import static net.seapanda.bunnyhop.common.configuration.BhConstants.Path.File.BH_SETTINGS_JSON;
import static net.seapanda.bunnyhop.simulator.common.BhSimConstants.Path.File.BH_SIM_SETTINGS_JSON;
import static net.seapanda.bunnyhop.utility.Utility.execPath;
import static net.seapanda.bunnyhop.utility.function.ThrowingConsumer.unchecked;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;
import javafx.application.Application;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonType;
import javafx.scene.control.ComboBox;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import net.seapanda.bunnyhop.common.configuration.BhSettings;
import net.seapanda.bunnyhop.service.FileCollector;
import net.seapanda.bunnyhop.service.message.BhMessageService;
import net.seapanda.bunnyhop.simulator.common.BhSimSettings;
import net.seapanda.bunnyhop.utility.serialization.JsonExporter;
import net.seapanda.bunnyhop.utility.serialization.JsonImporter;

/**
 * BunnyHop の設定を行うための GUI アプリケーションのエントリポイントとなるクラス.
 *
 * @author K.Koike
 */
public class BhConfigurator extends Application {

  @FXML
  private ComboBox<Language> languageComboBox;

  /** 設定ファイルとその設定を反映するクラスのクラスオブジェクト. */
  private final Map<Path, Class<?>> pathToClass = new HashMap<>() {{
      put(Paths.get(execPath, SETTINGS, BH_SETTINGS_JSON), BhSettings.class);
      put(Paths.get(execPath, SETTINGS, BH_SIM_SETTINGS_JSON), BhSimSettings.class);
    }};

  public static void main(String[] args) {
    launch(args);
  }

  @Override
  public void start(Stage stage) throws Exception {
    var messageService = new BhMessageService();
    try {
      importSettings();
      loadView(stage, getViewFile());
      stage.setOnCloseRequest(event -> exportSettings());
      stage.show();
    } catch (Exception e) {
      messageService.alert(
          Alert.AlertType.ERROR,
          "Failed to start BhConfigurator",
          null,
          e.toString(),
          ButtonType.OK);
      System.exit(-1);
    } finally {
      messageService.close();
    }
  }

  private void loadView(Stage stage, Path viewFile) throws IOException {
    FXMLLoader loader = new FXMLLoader(viewFile.toUri().toURL());
    loader.setController(this);
    VBox root = loader.load();
    var scene = new Scene(root);
    stage.setScene(scene);
    stage.setTitle("BhConfigurator");
    applyStyle(scene);
  }

  private Path getViewFile() throws Exception {
    Path fxmlDir =
        Paths.get(execPath, VIEW, FXML);
    var fxmlCollector = new FileCollector(fxmlDir, "fxml");
    return fxmlCollector.getFilePath(BH_CONFIGURATOR_FXML);
  }

  /** アプリケーションの設定をファイルに保存する. */
  private void exportSettings() {
    try {
      pathToClass.entrySet().stream()
          .filter(entry -> entry.getKey().toFile().exists())
          .forEach(unchecked(entry -> JsonExporter.export(entry.getValue(), entry.getKey())));
    } catch (Exception e) { /* Do nothing.*/ }
  }

  /** アプリケーションの設定をファイルから読み込む. */
  private void importSettings() {
    try {
      pathToClass.entrySet().stream()
          .filter(entry -> entry.getKey().toFile().exists())
          .forEach(unchecked(entry -> JsonImporter.imports(entry.getValue(), entry.getKey())));
    } catch (Exception e) { /* Do nothing.*/ }
  }

  /** {@code scene} に css を適用する. */
  private void applyStyle(Scene scene) {
    Path cssFile = Paths.get(execPath, VIEW, CSS, APP_GUI, BH_CONFIGURATOR_CSS);
    scene.getStylesheets().addAll(cssFile.toUri().toString());
  }

  @FXML
  private void initialize() {
    Language language = Language.fromValue(BhSettings.language);
    if (language != null) {
      languageComboBox.setValue(language);
    }
    languageComboBox.valueProperty().addListener((obs, oldVal, newVal) -> {
      BhSettings.language = newVal.getValue();
      BhSimSettings.language = newVal.getValue();
    });
  }
}
