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
import com.google.gson.JsonIOException;
import com.google.gson.JsonSyntaxException;
import com.google.gson.reflect.TypeToken;
import java.io.FileReader;
import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collection;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import net.seapanda.bunnyhop.common.configuration.BhConstants;
import net.seapanda.bunnyhop.node.model.BhNode;
import net.seapanda.bunnyhop.node.model.ConnectiveNode;
import net.seapanda.bunnyhop.node.model.Connector;
import net.seapanda.bunnyhop.node.model.TextNode;
import net.seapanda.bunnyhop.node.model.factory.BhNodeFactory;
import net.seapanda.bunnyhop.node.model.factory.BhNodeFactory.MvcType;
import net.seapanda.bunnyhop.node.model.parameter.BhNodeId;
import net.seapanda.bunnyhop.node.model.parameter.BhNodeVersion;
import net.seapanda.bunnyhop.node.model.parameter.ConnectorId;
import net.seapanda.bunnyhop.node.model.syntaxsymbol.InstanceId;
import net.seapanda.bunnyhop.node.model.traverse.BhNodeWalker;
import net.seapanda.bunnyhop.node.service.BhNodePlacer;
import net.seapanda.bunnyhop.service.LogManager;
import net.seapanda.bunnyhop.service.undo.UserOperation;
import net.seapanda.bunnyhop.ui.view.ViewConstructionException;
import net.seapanda.bunnyhop.workspace.model.Workspace;
import net.seapanda.bunnyhop.workspace.model.factory.WorkspaceFactory;

/**
 * JSON 形式で保存されたプロジェクトをロードする機能を提供するクラス.
 *
 * @author K.Koike
 */
public class JsonProjectReader {

  /** ロードしたファイルのパス. */
  private Path filePath;
  /** 復元対象となった {@link BhNode} と元となった {@link BhNodeImage} の対応一覧. */
  private final Map<BhNode, BhNodeImage> nodeToImage = new HashMap<>();
  /**
   * {@link BhNodeImage} が保持していた {@link InstanceId} と, 
   * その {@link BhNodeImage} から復元された {@link BhNode} の対応一覧を取得する.
   */
  private final Map<InstanceId, BhNode> instIdToNode = new HashMap<>();
  /** ロード中に発生した警告一覧. */
  private final EnumSet<ImportWarning> warnings = EnumSet.noneOf(ImportWarning.class);
  /** 復元対象となった {@link BhNode} の接続先のコネクタが見つからなかったときのエラー情報一覧. */
  private final Collection<ConnectorNotFoundInfo> cnctrNotFoundInfoList = new ArrayList<>();
  /**
   * 復元もとの {@link BhNodeImage} が保持していた {@link BhNodeId} の内,
   * 現バージョンのアプリケーションでサポートしていない ID の一覧.
   */
  private final Collection<BhNodeId> unknownNodeIds = new ArrayList<>();
  /** 復元対象の {@link BhNode} の {@link BhNodeVersion} が, 現在の同種のノードのバージョンと互換性が無いときのエラー情報一覧. */
  private final Collection<IncompatibleNodeVersionInfo> incompatibleNodeVersionInfoList =
      new ArrayList<>();
  /** 派生ノードが見つからなかったときのエラー情報一覧. */
  private final Collection<DerivativeNotFoundInfo> dervNotFoundInfoList = new ArrayList<>();
  /** {@link BhNodeImage} の破損フラグが立っていたノード一覧. */
  private final Collection<BhNode> corruptedNodes = new ArrayList<>();
  /** ノードの作成に使用するオブジェクト. */
  private final BhNodeFactory nodeFactory;
  /** ワークスペースの作成に使用するオブジェクト. */
  private final WorkspaceFactory wsFactory;

  /**
   * {@code filePath} のファイルの内容から, 元のワークスペース一式を復元する.
   *
   * @param filePath セーブデータのファイルパス
   * @param nodeFactory ノードの作成に使用するオブジェクト
   * @param wsFactory ワークスペースの作成に使用するオブジェクト
   * @return インポートの結果を格納した {@link JsonProjectReader.Result} オブジェクト
   */
  public static Result imports(
        Path filePath,
        BhNodeFactory nodeFactory,
        WorkspaceFactory wsFactory) throws
      IOException,
      JsonIOException,
      JsonSyntaxException,
      CorruptedSaveDataException,
      IncompatibleSaveFormatException,
      ViewConstructionException {
    var importer = new JsonProjectReader(nodeFactory, wsFactory);
    try {
      List<Workspace> workspaces = importer.load(filePath);
      setCorrupted(importer);
      return new Result(
          workspaces,
          importer.nodeToImage,
          importer.instIdToNode,
          importer.warnings,
          importer.cnctrNotFoundInfoList,
          importer.unknownNodeIds,
          importer.incompatibleNodeVersionInfoList,
          importer.dervNotFoundInfoList,
          importer.getWarningMsg(),
          importer.filePath);
    } catch (Exception e) {
      if (!importer.warnings.isEmpty()) {
        LogManager.logger().error(
            "Import %s\n[Warnings]\n%s".formatted(filePath, importer.getWarningMsg()));
      }
      throw e;
    }
  }

  /** 読み込みに失敗したノードに破損フラグを設定する. */
  private static void setCorrupted(JsonProjectReader importer) {
    importer.cnctrNotFoundInfoList.forEach(info -> info.node.setCorrupted(true));
    importer.incompatibleNodeVersionInfoList.forEach(info -> info.node.setCorrupted(true));
    importer.dervNotFoundInfoList.forEach(info -> info.orgNode.setCorrupted(true));
  }

  private JsonProjectReader(BhNodeFactory nodeFactory, WorkspaceFactory wsFactory) {
    this.nodeFactory = nodeFactory;
    this.wsFactory = wsFactory;
  }

  /**
   * {@code filePath} のファイルの内容から, 元となったワークスペース一式を作成する.
   *
   * @param filePath セーブデータのファイルパス.
   * @return {@code filePath} から復元したワークスペース一式
   */
  private List<Workspace> load(Path filePath) throws
      IOException,
      JsonIOException,
      JsonSyntaxException,
      CorruptedSaveDataException,
      IncompatibleSaveFormatException,
      ViewConstructionException {
    this.filePath = filePath;
    var gson = new Gson();
    try (var jr = gson.newJsonReader(new FileReader(filePath.toString()))) {
      var projImage = gson.<ProjectImage>fromJson(jr, new TypeToken<ProjectImage>(){}.getType());
      return genProject(projImage);
    }
  }

  /** {@code image} から, 元となったワークスペース一式を作成する. */
  private List<Workspace> genProject(ProjectImage projImage) throws
      CorruptedSaveDataException,  
      IncompatibleSaveFormatException,
      ViewConstructionException {
    checkSaveDataVersion(projImage.saveDataVersion);
    var wsList = new ArrayList<Workspace>();
    var rootNodes = new ArrayList<BhNode>();
    for (WorkspaceImage wsImage : projImage.getWorkspaceImages()) {
      Workspace ws = genWorkspace(wsImage);
      wsList.add(ws);
      rootNodes.addAll(ws.getRootNodes());
    }
    rootNodes.forEach(root -> new DerivativeAssigner().assign(root));
    return wsList;
  }

  /** セーブデータのバージョンをチェックする. */
  void checkSaveDataVersion(SaveDataVersion version) throws IncompatibleSaveFormatException {
    if (version == null
        || version.comparePrefix(BhConstants.SAVE_DATA_VERSION) != 0
        || version.compareMajor(BhConstants.SAVE_DATA_VERSION) != 0
        || version.compareMinor(BhConstants.SAVE_DATA_VERSION) > 0) {
      String msg = "Incompatible save data version : %s.\nSupported save data version: %s."
          .formatted(version, BhConstants.SAVE_DATA_VERSION);
      throw new IncompatibleSaveFormatException(msg, version);
    }
  }

  /** {@code wsImage} から元となった {@link Workspace} を作成する. */
  private Workspace genWorkspace(WorkspaceImage wsImage)
      throws CorruptedSaveDataException, ViewConstructionException {
    Workspace ws = wsFactory.create(wsImage.name);
    wsFactory.setMvc(ws, wsImage.size);
    // ルートノードの追加
    var userOpe = new UserOperation();
    for (BhNodeImage nodeImage : wsImage.getRootNodes()) {
      BhNode root = createBhNode(nodeImage);
      if (root != null) {
        nodeFactory.setMvc(root, MvcType.DEFAULT);
        BhNodePlacer.moveToWs(ws, root, nodeImage.pos.x, nodeImage.pos.y, userOpe);
      }
    }
    return ws;
  }

  /**
   * {@code nodeImage} から元となった {@link BhNode} を復元する.
   * {@code nodeImage} の子孫要素も復元される.
   *
   * @param nodeImage このオブジェクトから {@link BhNode} を復元する.
   * @return {@code Image} から復元したノード.
   */
  private BhNode createBhNode(BhNodeImage nodeImage) throws CorruptedSaveDataException {
    if (!canCreateNodeOf(nodeImage)) {
      return null;
    }
    var userOpe = new UserOperation();
    BhNode node = nodeFactory.create(nodeImage.nodeId, userOpe);
    node.setInstanceId(nodeImage.instanceId);
    node.setDefault(nodeImage.isDefault);
    if (node.isBreakpointGroupLeader()) {
      node.setBreakpoint(nodeImage.isBreakpointSet, userOpe);
    }
    // ノードの互換性のチェックは行うが, 互換性がない場合でもノード復元は止めない.
    checkNodeVersionCompatibility(nodeImage, node);
    checkIfCorrupted(nodeImage, node);
    buildDescendant(nodeImage, node);

    if (node instanceof TextNode textNode) {
      textNode.setText(nodeImage.text);
    }
    // InstanceId の重複チェックは genBhNode を呼び出した後で行う必要がある.
    checkNodeImageInstanceId(nodeImage);

    instIdToNode.put(nodeImage.instanceId, node);
    nodeToImage.put(node, nodeImage);
    return node;
  }

  /** {@code node} 以下の子孫要素を {@code nodeImage} をもとに復元する. */
  private void buildDescendant(BhNodeImage nodeImage, BhNode node)
      throws CorruptedSaveDataException {
    for (ConnectorImage cnctrImage : nodeImage.getChildren()) {
      BhNode child = createBhNode(cnctrImage.connectedNode);
      if (child == null || !(node instanceof ConnectiveNode parent)) {
        continue;
      }
      connectChild(parent, child, cnctrImage.connectorId)
          .ifPresent(cnctr -> cnctr.setDefaultNodeId(cnctrImage.defaultNodeId));
    }
  }

  /** {@code nodeImage} で指定した {@link BhNode} が作成可能か調べる. */
  private boolean canCreateNodeOf(BhNodeImage nodeImage) {
    boolean canCreate = nodeFactory.canCreate(nodeImage.nodeId);
    if (!canCreate) {
      warnings.add(ImportWarning.UNKNOWN_BH_NODE_ID);
      unknownNodeIds.add(nodeImage.nodeId);
    }
    return canCreate;
  }

  /** {@link BhNodeImage} のバージョンと {@link BhNode} のバージョンに互換性があるか調べる. */
  private boolean checkNodeVersionCompatibility(BhNodeImage nodeImage, BhNode node) {
    BhNodeVersion imageVersion = nodeImage.version;
    boolean isCompatible =
        imageVersion.comparePrefix(node.getVersion()) == 0
        && imageVersion.compareMajor(node.getVersion()) == 0
        && imageVersion.compareMinor(node.getVersion()) <= 0;

    if (!isCompatible) {
      warnings.add(ImportWarning.INCOMPATIBLE_BH_NODE_VERSION);
      incompatibleNodeVersionInfoList.add(
          new IncompatibleNodeVersionInfo(node, nodeImage.version, node.getVersion()));
    }
    return isCompatible;
  }

  /** 破損フラグをチェックする. */
  private void checkIfCorrupted(BhNodeImage nodeImage, BhNode node) {
    if (nodeImage.isCorrupted) {
      warnings.add(ImportWarning.CORRUPTED_NODE);
      node.setCorrupted(true);
      corruptedNodes.add(node);
    }
  }

  /**
   * {@link BhNodeImage} の {@link InstanceId} が適切かチェックする.
   *
   * <p>本メソッドでは, セーブデータ内部の {@link InstanceId} 同士が重複していないかチェックするが,
   * 既存の {@link InstanceId} との重複はチェックしない.
   */
  private void checkNodeImageInstanceId(BhNodeImage image) throws CorruptedSaveDataException {
    if (instIdToNode.containsKey(image.instanceId)) {
      throw new CorruptedSaveDataException(
          "Duplicated instance ID (%s).".formatted(image.instanceId));
    } else if (image.instanceId.equals(InstanceId.NONE)) {
      throw new CorruptedSaveDataException("Invalid instance ID (NONE).");
    }
  }

  /**
   * 2 つのノードを親子関係にする.
   *
   * @param parent 親ノード
   * @param child 子ノード
   * @param id {@code parent} の下にあって, この ID を持つコネクタの下に {@code child} を接続する
   * @return {@code parent} と {@code child} を接続するコネクタ.  {@code id} に対応するコネクタが見つからなかった場合 empty.
   */
  private Optional<Connector> connectChild(ConnectiveNode parent, BhNode child, ConnectorId id) {
    Connector cnctr = parent.findConnector(id);
    if (cnctr != null) {
      cnctr.connectNode(child);
      return Optional.of(cnctr);
    }
    warnings.add(ImportWarning.CONNECTOR_NOT_FOUND);
    cnctrNotFoundInfoList.add(new ConnectorNotFoundInfo(parent, id));
    return Optional.empty();
  }

  /** ロード中に発生した全ての警告のメッセージを取得する. */
  public String getWarningMsg() {
    StringBuilder msg = new StringBuilder();
    if (warnings.contains(ImportWarning.UNKNOWN_BH_NODE_ID)) {
      msg.append(ImportWarning.UNKNOWN_BH_NODE_ID + "\n");
      for (BhNodeId unknownNodeId : unknownNodeIds) {
        msg.append("  %s: %s\n".formatted(
            unknownNodeId.getClass().getSimpleName(),
            unknownNodeId.toString()));
      }
      msg.append("\n");
    }
    if (warnings.contains(ImportWarning.INCOMPATIBLE_BH_NODE_VERSION)) {
      msg.append(ImportWarning.INCOMPATIBLE_BH_NODE_VERSION + "\n");
      for (IncompatibleNodeVersionInfo info : incompatibleNodeVersionInfoList) {
        msg.append("  %s: %s,  image version: %s,  node version: %s\n".formatted(
            info.node.getId().getClass().getSimpleName(),
            info.node.getId(),
            info.imageVer,
            info.nodeVer));
      }
      msg.append("\n");
    }
    if (warnings.contains(ImportWarning.CONNECTOR_NOT_FOUND)) {
      msg.append(ImportWarning.CONNECTOR_NOT_FOUND + "\n");
      for (ConnectorNotFoundInfo info : cnctrNotFoundInfoList) {
        msg.append("  connective node id: %s,  %s: %s\n".formatted(
            info.node.getId(),
            info.connectorId.getClass().getSimpleName(),
            info.connectorId));
      }
      msg.append("\n");
    }
    if (warnings.contains(ImportWarning.DERIVATIVE_NOT_FOUND)) {
      msg.append(ImportWarning.DERIVATIVE_NOT_FOUND + "\n");
      for (DerivativeNotFoundInfo info : dervNotFoundInfoList) {
        msg.append("  %s: %s,  original instance id: %s,  derivative instance id: %s\n".formatted(
            info.orgNode.getId().getClass().getSimpleName(),
            info.orgNode.getId(),
            info.orgNode.getId(),
            info.dervInstId));
      }
      msg.append("\n");
    }
    if (warnings.contains(ImportWarning.CORRUPTED_NODE)) {
      msg.append(ImportWarning.CORRUPTED_NODE + "\n");
      for (BhNode node : corruptedNodes) {
        msg.append("  %s: %s\n".formatted(
            node.getId().getClass().getSimpleName(),
            node.getId().toString()));
      }
      msg.append("\n");
    }
    return msg.toString();
  }

  /**
   * インポートの結果.
   *
   * @param workspaces 復元された全てのワークスペースのリスト
   * @param nodeToImage 復元対象となった {@link BhNode} と元となった {@link BhNodeImage} の対応一覧
   *                    key には復元に失敗した {@link BhNode} も含まれる.
   * @param instanceIdToNode {@link BhNodeImage} が保持していた {@link InstanceId} と, 
   *                         その {@link BhNodeImage} から復元された {@link BhNode} の対応一覧を取得する.
   *                         value には復元に失敗した {@link BhNode} も含まれる.
   * @param warnings ロード中に発生した警告一覧
   * @param cnctrNotFoundInfoList 復元対象となった {@link BhNode} の接続先のコネクタが見つからなかったときのエラー情報一覧.
   * @param unknownNodeIds 復元もとの {@link BhNodeImage} が保持していた {@link BhNodeId} の内,
   *                       現バージョンのアプリケーションでサポートしていない ID の一覧.
   * @param incompatibleNodeVersionInfoList 復元対象の {@link BhNode} の {@link BhNodeVersion} が
   *                                        現在の同種のノードのバージョンと互換性が無いときのエラー情報一覧
   * @param derivativeNotFoundInfoList 復元もとの {@link BhNodeImage} に格納されていた派生ノードが見つからなかったときのエラー情報一覧
   * @param warningMsg ロード中に発生した全ての警告のメッセージ
   * @param filePath ロードしたファイルのパス.
   */
  public record Result(
      List<Workspace> workspaces,
      Map<BhNode, BhNodeImage> nodeToImage,
      Map<InstanceId, BhNode> instanceIdToNode,
      Set<ImportWarning> warnings,
      Collection<ConnectorNotFoundInfo> cnctrNotFoundInfoList,
      Collection<BhNodeId> unknownNodeIds,
      Collection<IncompatibleNodeVersionInfo> incompatibleNodeVersionInfoList,
      Collection<DerivativeNotFoundInfo> derivativeNotFoundInfoList,
      String warningMsg,
      Path filePath) {

    /** 既存の {@link Result} に警告を追加した新しい {@link Result} を作るためのコンストラクタ. */
    public Result(
        Result result, String additionalWarningMsg, ImportWarning... additionalWarnings) {
      this(
          result.workspaces,
          result.nodeToImage,
          result.instanceIdToNode,
          createWarningsSet(result.warnings, List.of(additionalWarnings)),
          result.cnctrNotFoundInfoList,
          result.unknownNodeIds,
          result.incompatibleNodeVersionInfoList,
          result.derivativeNotFoundInfoList,
          result.warningMsg + additionalWarningMsg,
          result.filePath);
    }

    private static Set<ImportWarning> createWarningsSet(
        Set<ImportWarning> original, Collection<ImportWarning> additional) {
      var warnings = EnumSet.noneOf(ImportWarning.class);
      warnings.addAll(original);
      warnings.addAll(additional);
      return warnings;
    }
  }

  /**
   * コネクタが見つからなかった場合ののエラー情報を格納するレコード.
   *
   * @param node このノード以下からコネクタを探した
   * @param connectorId 探したコネクタの ID
   */
  private record ConnectorNotFoundInfo(BhNode node, ConnectorId connectorId) {}

  /**
   * {@link BhNode} のバージョンに互換性が無かった場合のエラー情報を格納するレコード.
   *
   * @param node バージョンに互換性がなかった {@link BhNode}
   * @param imageVer {@link BhNode} の復元に使った {@link BhNodeImage} に格納されていた {@link BhNodeVersion}
   * @param nodeVer {@code imageVer} を元に作成した {@link BhNode} のバージョン
   */
  private record IncompatibleNodeVersionInfo(
      BhNode node, BhNodeVersion imageVer, BhNodeVersion nodeVer) {}

  /**
   * 派生ノードが見つからなかった場合のエラー情報を格納するレコード.
   *
   * @param orgNode 保持すべき派生ノードが見つからなかったオリジナルノード
   * @param dervInstId 見つからなかった派生ノードの {@link InstanceId}
   */
  private record DerivativeNotFoundInfo(BhNode orgNode, InstanceId dervInstId) {}

  /**
   * オリジナルノードに派生ノードを割り当てるクラス.
   */
  private class DerivativeAssigner implements BhNodeWalker {
    
    /** {@code root} 以下のオリジナルノードに対して，元々持っていた派生ノードを割り当てる.*/
    public void assign(BhNode root) {
      root.accept(this);
    }

    public DerivativeAssigner() { }

    @Override
    public void visit(ConnectiveNode original) {
      original.sendToSections(this);

      final var userOpe = new UserOperation();
      var importer = JsonProjectReader.this;
      // 親子の接続に失敗している場合, importer.nodeToImage.get(original) は null を返す可能性がある.
      if (importer.nodeToImage.get(original) == null) {
        return;
      }
      for (InstanceId dervInstId : importer.nodeToImage.get(original).derivativeIds) {
        BhNode derivative = importer.instIdToNode.get(dervInstId);
        if (derivative instanceof ConnectiveNode connective) {
          original.addDerivative(connective, userOpe);
        } else {
          importer.warnings.add(ImportWarning.DERIVATIVE_NOT_FOUND);
          importer.dervNotFoundInfoList.add(new DerivativeNotFoundInfo(original, dervInstId));
        }
      }
    }

    @Override
    public void visit(TextNode original) {
      final var userOpe = new UserOperation();
      var importer = JsonProjectReader.this;
      // 親子の接続に失敗している場合, importer.nodeToImage.get(original) は null を返す可能性がある.
      if (importer.nodeToImage.get(original) == null) {
        return;
      }
      for (InstanceId dervInstId : importer.nodeToImage.get(original).derivativeIds) {
        BhNode derivative = importer.instIdToNode.get(dervInstId);
        if (derivative instanceof TextNode textNode) {
          original.addDerivative(textNode, userOpe);
        } else {
          importer.warnings.add(ImportWarning.DERIVATIVE_NOT_FOUND);
          importer.dervNotFoundInfoList.add(new DerivativeNotFoundInfo(original, dervInstId));
        }
      }
    }
  }
}
