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

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonIOException;
import com.google.gson.reflect.TypeToken;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Path;
import java.util.Collection;
import java.util.List;
import java.util.SequencedSet;
import net.seapanda.bunnyhop.common.configuration.BhConstants;
import net.seapanda.bunnyhop.node.model.BhNode;
import net.seapanda.bunnyhop.utility.math.Vec2D;
import net.seapanda.bunnyhop.workspace.model.Workspace;
import net.seapanda.bunnyhop.workspace.view.WorkspaceView;

/**
 * プロジェクト JSON 形式で保存する機能を提供するクラス.
 *
 * @author K.Koike
 */
public class JsonProjectWriter {

  /**
   * {@code workspaces} で指定した {@link Workspace} 一式とそれらに含まれる全 {@link BhNode} の情報を
   * JSON 形式で {@code filePath} に保存する.
   *
   * @param workspaces これらの {@link Workspace} とその下にある {@link BhNode} を保存する.
   * @param filePath データを保存するファイルのパス.
   */
  public static void export(SequencedSet<Workspace> workspaces, Path filePath)
      throws JsonIOException, IOException {
    List<WorkspaceImage> wsi = workspaces.stream().map(JsonProjectWriter::convertToImage).toList();
    var image = new ProjectImage(
        BhConstants.SYS_VERSION, BhConstants.APP_VERSION, BhConstants.SAVE_DATA_VERSION, wsi);
    Gson gson = new GsonBuilder().create();
    try (var jw = gson.newJsonWriter(new FileWriter(filePath.toString()))) {
      gson.toJson(image, new TypeToken<ProjectImage>(){}.getType(), jw);
    }
  }

  /** {@code workspace} に対応する {@link WorkspaceImage} を作成する. */
  private static WorkspaceImage convertToImage(Workspace workspace) {
    Collection<BhNodeImage> nodeImages = workspace.getRootNodes().stream()
        .map(root -> NodeImageBuilder.build(root))
        .toList();
    Vec2D size = workspace.getView().map(WorkspaceView::getSize).orElse(new Vec2D());
    return new WorkspaceImage(workspace.getName(), size, nodeImages);
  }
}
