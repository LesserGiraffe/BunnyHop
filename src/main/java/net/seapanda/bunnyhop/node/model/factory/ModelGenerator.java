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

package net.seapanda.bunnyhop.node.model.factory;

import java.util.Map;
import net.seapanda.bunnyhop.node.model.BhNode;
import net.seapanda.bunnyhop.node.model.ConnectiveNode;
import net.seapanda.bunnyhop.node.model.Connector;
import net.seapanda.bunnyhop.node.model.TextNode;
import net.seapanda.bunnyhop.node.model.derivative.DerivativeReplacer;
import net.seapanda.bunnyhop.node.model.event.EventType;
import net.seapanda.bunnyhop.node.model.event.ScriptConnectorEventInvoker;
import net.seapanda.bunnyhop.node.model.event.ScriptNodeEventInvoker;
import net.seapanda.bunnyhop.node.model.parameter.BhNodeId;
import net.seapanda.bunnyhop.node.model.parameter.BhNodeParameters;
import net.seapanda.bunnyhop.node.model.parameter.ConnectorParameters;
import net.seapanda.bunnyhop.node.model.parameter.DerivationId;
import net.seapanda.bunnyhop.node.model.section.Section;

/**
 * {@link BhNode} と {@link Connector} を生成するクラス.
 *
 * @author K.Koike
 */
public class ModelGenerator {
  
  private final BhNodeFactory factory;
  private final DerivativeReplacer replacer;
  private final ScriptNodeEventInvoker nodeEventInvoker;
  private final ScriptConnectorEventInvoker cnctrEventInvoker;

  /** コンストラクタ. */
  public ModelGenerator(
      BhNodeFactory factory,
      DerivativeReplacer replacer,
      ScriptNodeEventInvoker nodeEventInvoker,
      ScriptConnectorEventInvoker cnctrEventInvoker) {
    this.factory = factory;
    this.replacer = replacer;
    this.nodeEventInvoker = nodeEventInvoker;
    this.cnctrEventInvoker = cnctrEventInvoker;
  }

  /** {@link ConnectiveNode} を作成する. */
  ConnectiveNode newConnectiveNode(
      Section childSection,
      Map<DerivationId, BhNodeId> derivationToDerivative,
      BhNodeAttributes attributes) {
    var params = new BhNodeParameters(
        attributes.bhNodeId(),
        attributes.name(),
        attributes.nodeStyleId(),
        attributes.version(),
        !attributes.onCompanionNodesCreating().isEmpty(),
        attributes.breakpointSetting());
    registerEventHandlers(attributes);
    return new ConnectiveNode(
        params,
        childSection,
        derivationToDerivative,
        factory,
        replacer,
        nodeEventInvoker);
  }

  /** {@link TextNode} を作成する. */
  TextNode newTextNode(
      Map<DerivationId, BhNodeId> derivationToDerivative,
      BhNodeAttributes attributes) {
    var params = new BhNodeParameters(
        attributes.bhNodeId(),
        attributes.name(),
        attributes.nodeStyleId(),
        attributes.version(),
        !attributes.onCompanionNodesCreating().isEmpty(),
        attributes.breakpointSetting());
    registerEventHandlers(attributes);
    return new TextNode(
        params,
        attributes.text(),
        derivationToDerivative,
        factory,
        replacer,
        nodeEventInvoker);
  }

  /** {@link Connector} を作成する. */
  Connector newConnector(ConnectorAttribute attributes) {
    registerEventHandlers(attributes);
    var params = new ConnectorParameters(
        attributes.connectorId(),
        attributes.name(),
        attributes.defaultNodeId(),
        attributes.derivationId(),
        attributes.derivativeJointId(),
        attributes.fixed());
    return new Connector(params, factory, cnctrEventInvoker);
  }

  /** ノードのイベントハンドラを {@link #nodeEventInvoker} に登録する. */
  private void registerEventHandlers(BhNodeAttributes attr) {
    nodeEventInvoker.register(
        attr.bhNodeId(), EventType.ON_MOVED_FROM_WS_TO_CHILD, attr.onMovedFromWsToChild());
    nodeEventInvoker.register(
        attr.bhNodeId(), EventType.ON_MOVED_FROM_CHILD_TO_WS, attr.onMovedFromChildToWs());
    nodeEventInvoker.register(
        attr.bhNodeId(), EventType.ON_CHILD_REPLACED, attr.onChildReplaced());
    nodeEventInvoker.register(
        attr.bhNodeId(), EventType.ON_DELETION_REQUESTED, attr.onDeletionRequested());
    nodeEventInvoker.register(
        attr.bhNodeId(), EventType.ON_CUT_REQUESTED, attr.onCutRequested());
    nodeEventInvoker.register(
        attr.bhNodeId(), EventType.ON_COPY_REQUESTED, attr.onCopyRequested());
    nodeEventInvoker.register(
        attr.bhNodeId(), EventType.ON_CREATED_AS_TEMPLATE, attr.onCreatedAsTemplate());
    nodeEventInvoker.register(
        attr.bhNodeId(), EventType.ON_UI_EVENT_RECEIVED, attr.onUiEventReceived());
    nodeEventInvoker.register(
        attr.bhNodeId(), EventType.ON_TEXT_CHECKING, attr.onTextChecking());
    nodeEventInvoker.register(
        attr.bhNodeId(), EventType.ON_TEXT_FORMATTING, attr.onTextFormatting());
    nodeEventInvoker.register(
        attr.bhNodeId(), EventType.ON_TEXT_OPTIONS_CREATING, attr.onTextOptionsCreating());
    nodeEventInvoker.register(
        attr.bhNodeId(), EventType.ON_COMPANION_NODES_CREATING, attr.onCompanionNodesCreating());
    nodeEventInvoker.register(
        attr.bhNodeId(), EventType.ON_COMPILE_ERR_CHECKING, attr.onCompileErrorChecking());
    nodeEventInvoker.register(
        attr.bhNodeId(), EventType.ON_ALIAS_ASKED, attr.onAliasAsked());
    nodeEventInvoker.register(
        attr.bhNodeId(), EventType.ON_USER_DEFINED_NAME_ASKED, attr.onUserDefinedNameAsked());
  }

  private void registerEventHandlers(ConnectorAttribute attr) {
    cnctrEventInvoker.register(
        attr.connectorId(), EventType.ON_CONNECTABILITY_CHECKING, attr.onConnectabilityChecking());
  }
}
