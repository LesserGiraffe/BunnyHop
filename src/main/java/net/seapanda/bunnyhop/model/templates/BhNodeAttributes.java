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

package net.seapanda.bunnyhop.model.templates;

import java.util.Optional;
import net.seapanda.bunnyhop.common.constant.BhConstants;
import net.seapanda.bunnyhop.common.tools.MsgPrinter;
import net.seapanda.bunnyhop.model.node.attribute.BhNodeId;
import net.seapanda.bunnyhop.view.node.part.BhNodeViewStyle;
import org.w3c.dom.Element;

/**
 * ノードが定義された xml の Node タグが持つ属性一覧を保持するクラス.
 *
 * @author K.Koike
 */
public class BhNodeAttributes {

  private BhNodeId bhNodeId;
  private String name;
  private String nodeStyleId;
  private String onMovedFromChildToWs;
  private String onMovedToChild;
  private String onTextChecking;
  private String onDeletionRequested;
  private String onCutRequested;
  private String onCopyRequested;
  private String onChildReplaced;
  private String onPrivateTemplateCreating;
  private String onTextFormatting;
  private String onSyntaxChecking;
  private String onViewContentsCreating;
  private String initString;
  private boolean canCreateImitManually;

  private BhNodeAttributes() {}

  /**
   * Node タグが持つ属性一覧を読み取る.
   *
   * @param node Node タグを表すオブジェクト
   */
  static Optional<BhNodeAttributes> readBhNodeAttriButes(Element node) {

    BhNodeAttributes nodeAttrs = new BhNodeAttributes();

    // bhNodeID
    nodeAttrs.bhNodeId = BhNodeId.create(node.getAttribute(BhConstants.BhModelDef.ATTR_BH_NODE_ID));
    if (nodeAttrs.bhNodeId.equals(BhNodeId.NONE)) {
      MsgPrinter.INSTANCE.errMsgForDebug(
          "<" + BhConstants.BhModelDef.ELEM_NODE + ">" + " タグには "
          + BhConstants.BhModelDef.ATTR_BH_NODE_ID + " 属性を記述してください.  " + node.getBaseURI());
      return Optional.empty();
    }

    // name
    nodeAttrs.name = node.getAttribute(BhConstants.BhModelDef.ATTR_NAME);

    // nodeStyleID
    nodeAttrs.nodeStyleId = node.getAttribute(BhConstants.BhModelDef.ATTR_NODE_STYLE_ID);
    if (!nodeAttrs.nodeStyleId.isEmpty()) {
      BhNodeViewStyle.putNodeIdToNodeStyleId(nodeAttrs.bhNodeId, nodeAttrs.nodeStyleId);
    }

    // onMovedFromChildToWS
    nodeAttrs.onMovedFromChildToWs =
        node.getAttribute(BhConstants.BhModelDef.ATTR_ON_MOVED_FROM_CHILD_TO_WS);

    // onMovedToChild
    nodeAttrs.onMovedToChild = node.getAttribute(BhConstants.BhModelDef.ATTR_ON_MOVED_TO_CHILD);

    // onTextChecking
    nodeAttrs.onTextChecking = node.getAttribute(BhConstants.BhModelDef.ATTR_ON_TEXT_CHECKING);

    // onTextFormatting
    nodeAttrs.onTextFormatting = node.getAttribute(BhConstants.BhModelDef.ATTR_ON_TEXT_FORMATTING);

    // onDeletionRequested
    nodeAttrs.onDeletionRequested =
        node.getAttribute(BhConstants.BhModelDef.ATTR_ON_DELETION_REQUESTED);

    // onCutRequested
    nodeAttrs.onCutRequested = node.getAttribute(BhConstants.BhModelDef.ATTR_ON_CUT_REQUESTED);

    // onCopyRequested
    nodeAttrs.onCopyRequested = node.getAttribute(BhConstants.BhModelDef.ATTR_ON_COPY_REQUESTED);

    // onChildReplaced
    nodeAttrs.onChildReplaced = node.getAttribute(BhConstants.BhModelDef.ATTR_ON_CHILD_REPLACED);

    // onCutRequested
    nodeAttrs.onCutRequested = node.getAttribute(BhConstants.BhModelDef.ATTR_ON_CUT_REQUESTED);

    // onPrivateTemplateCreating
    nodeAttrs.onPrivateTemplateCreating =
        node.getAttribute(BhConstants.BhModelDef.ATTR_ON_PRIFVATE_TEMPLATE_CREATING);

    // onSyntaxChecking
    nodeAttrs.onSyntaxChecking = node.getAttribute(BhConstants.BhModelDef.ATTR_ON_SYNTAX_CHECKING);
    
    // onViewContentsCreating
    nodeAttrs.onViewContentsCreating = 
        node.getAttribute(BhConstants.BhModelDef.ATTR_ON_VIEW_CONTENTS_CREATING);

    // initString
    nodeAttrs.initString = node.getAttribute(BhConstants.BhModelDef.ATTR_INIT_STRING);

    // canCreateImitManually
    String strCreateImit = node.getAttribute(BhConstants.BhModelDef.ATTR_CAN_CREATE_IMIT_MANUALLY);
    if (strCreateImit.equals(BhConstants.BhModelDef.ATTR_VAL_TRUE)) {
      nodeAttrs.canCreateImitManually = true;
    } else if (strCreateImit.equals(
        BhConstants.BhModelDef.ATTR_VAL_FALSE) || strCreateImit.isEmpty()) {
      nodeAttrs.canCreateImitManually = false;
    } else {
      MsgPrinter.INSTANCE.errMsgForDebug(
          BhConstants.BhModelDef.ATTR_CAN_CREATE_IMIT_MANUALLY + " 属性には "
          + BhConstants.BhModelDef.ATTR_VAL_TRUE + " か "
          + BhConstants.BhModelDef.ATTR_VAL_FALSE + " を指定してください. " + node.getBaseURI());
      return Optional.empty();
    }
    return Optional.of(nodeAttrs);
  }

  public BhNodeId getBhNodeId() {
    return bhNodeId;
  }

  public String getName() {
    return name;
  }

  public String getNodeStyleId() {
    return nodeStyleId;
  }

  public String getOnMovedFromChildToWs() {
    return onMovedFromChildToWs;
  }

  public String getOnMovedToChild() {
    return onMovedToChild;
  }

  public String getOnTextChecking() {
    return onTextChecking;
  }

  public String getOnDeletionRequested() {
    return onDeletionRequested;
  }

  public String getOnCutRequested() {
    return onCutRequested;
  }

  public String getOnCopyRequested() {
    return onCopyRequested;
  }

  public String getOnChildReplaced() {
    return onChildReplaced;
  }

  public String getOnPrivateTemplateCreating() {
    return onPrivateTemplateCreating;
  }

  public String getOnTextFormatting() {
    return onTextFormatting;
  }

  public String getOnSyntaxChecking() {
    return onSyntaxChecking;
  }
  
  public String getOnViewContentsCreating() {
    return onViewContentsCreating;
  }

  public String getIinitString() {
    return initString;
  }

  public boolean getCanCreateImitManually() {
    return canCreateImitManually;
  }
}
