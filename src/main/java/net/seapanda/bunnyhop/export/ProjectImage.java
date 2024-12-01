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

package net.seapanda.bunnyhop.export;

import java.util.ArrayList;
import java.util.Collection;

/**
 * プロジェクト全体の保存用イメージ.
 *
 * @author K.Koike
 */
class ProjectImage {

  /** アプリケーションのバージョン. */
  final AppVersion appVersion;
  /** セーブデータのバージョン. */
  final SaveDataVersion saveDataVersion;
  /** プロジェクトが持つワークスペース一式. */
  private final ArrayList<WorkspaceImage> workspaceImages;

  /**
   * コンストラクタ.
   *
   * @param appVersion このデータを作ったアプリケーションのバージョン
   * @param saveDataVersion セーブデータのバージョン
   * @param workspaceImages プロジェクトに含まれるワークスペースの保存用イメージ
   */
  ProjectImage(
      AppVersion appVersion,
      SaveDataVersion saveDataVersion,
      Collection<WorkspaceImage> workspaceImages) {
    this.appVersion = appVersion;
    this.saveDataVersion = saveDataVersion;
    this.workspaceImages = new ArrayList<>(workspaceImages);
  }

  /** デフォルトコンストラクタ. (デシリアライズ用) */
  public ProjectImage() {
    appVersion = AppVersion.NONE;
    saveDataVersion = SaveDataVersion.NONE;
    workspaceImages = new ArrayList<>();
  }

  /** ワークスペースの保存用イメージ一式を返す. */
  Collection<WorkspaceImage> getWorkspaceImages() {
    return new ArrayList<>(workspaceImages);
  }
}
