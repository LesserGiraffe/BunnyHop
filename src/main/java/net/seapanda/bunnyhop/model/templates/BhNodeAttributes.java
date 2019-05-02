/**
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

import org.w3c.dom.Element;

import net.seapanda.bunnyhop.common.BhParams;
import net.seapanda.bunnyhop.common.tools.MsgPrinter;
import net.seapanda.bunnyhop.model.node.BhNodeID;
import net.seapanda.bunnyhop.view.node.BhNodeViewStyle;

/**
 * \<Node\> タグが持つ属性一覧
 * @author K.Koike
 */
public class BhNodeAttributes {

	private BhNodeID bhNodeID;
	private String name;
	private String nodeStyleID;
	private String onMovedFromChildToWS;
	private String onMovedToChild;
	private String onTextAcceptabilityChecked;
	private String onDeletionCmdReceived;
	private String onCutAndPasteCmdReceived;
	private String onChildReplaced;
	private String textFormatter;
	private String imitScopeName;
	private String initString;
	private String nodeInputControlFileName;
	private boolean canCreateImitManually;

	private BhNodeAttributes(){}

	/**
	 * \<Node\> タグが持つ属性一覧を読み取る
	 * @param node \<Node\>タグを表すオブジェクト
	 */
	static Optional<BhNodeAttributes> readBhNodeAttriButes(Element node) {

		BhNodeAttributes nodeAttrs = new BhNodeAttributes();

		//bhNodeID
		nodeAttrs.bhNodeID = BhNodeID.create(node.getAttribute(BhParams.BhModelDef.ATTR_NAME_BHNODE_ID));
		if (nodeAttrs.bhNodeID.equals(BhNodeID.NONE)) {
			MsgPrinter.INSTANCE.errMsgForDebug(
				"<" + BhParams.BhModelDef.ELEM_NAME_NODE + ">" + " タグには "
				+ BhParams.BhModelDef.ATTR_NAME_BHNODE_ID + " 属性を記述してください.  " + node.getBaseURI());
			return Optional.empty();
		}

		//name
		nodeAttrs.name = node.getAttribute(BhParams.BhModelDef.ATTR_NAME_NAME);

		// nodeStyleID
		String nodeStyleID = node.getAttribute(BhParams.NodeStyleDef.KEY_NODE_STYLE_ID);
		nodeAttrs.nodeStyleID = nodeStyleID.isEmpty() ? BhParams.BhModelDef.ATTR_VALUE_DEFAULT_NODE_STYLE_ID : nodeStyleID;
		BhNodeViewStyle.putNodeID_NodeStyleID(nodeAttrs.bhNodeID, nodeAttrs.nodeStyleID);

		//onMovedFromChildToWS
		nodeAttrs.onMovedFromChildToWS = node.getAttribute(BhParams.BhModelDef.ATTR_NAME_ON_MOVED_FROM_CHILD_TO_WS);

		//onMovedToChild
		nodeAttrs.onMovedToChild = node.getAttribute(BhParams.BhModelDef.ATTR_NAME_ON_MOVED_TO_CHILD);

		//onTextAcceptabilityChecked
		nodeAttrs.onTextAcceptabilityChecked = node.getAttribute(BhParams.BhModelDef.ATTR_NAME_ON_TEXT_ACCEPTABILITY_CHECKED);

		//textFormatter
		nodeAttrs.textFormatter = node.getAttribute(BhParams.BhModelDef.ATTR_NAME_TEXT_FORMATTER);

		//onDeletionCmdReceived
		nodeAttrs.onDeletionCmdReceived = node.getAttribute(BhParams.BhModelDef.ATTR_NAME_ON_DELETION_CMD_RECEIVED);

		//onCutAndPasteCmdReceived
		nodeAttrs.onCutAndPasteCmdReceived = node.getAttribute(BhParams.BhModelDef.ATTR_NAME_ON_CUT_AND_PASTE_CMD_RECEIVED);

		//onChildReplaced
		nodeAttrs.onChildReplaced = node.getAttribute(BhParams.BhModelDef.ATTR_NAME_ON_CHILD_REPLACED);

		//imitScopeName
		nodeAttrs.imitScopeName = node.getAttribute(BhParams.BhModelDef.ATTR_NAME_IMIT_SCOPE_NAME);

		//initString
		nodeAttrs.initString = node.getAttribute(BhParams.BhModelDef.ATTR_NAME_INIT_STRING);

		//nodeInputControl
		nodeAttrs.nodeInputControlFileName = node.getAttribute(BhParams.BhModelDef.ATTR_NAME_NODE_INPUT_CONTROL);

		//canCreateImitManually
		String strCreateImit = node.getAttribute(BhParams.BhModelDef.ATTR_NAME_CAN_CREATE_IMIT_MANUALLY);
		if (strCreateImit.equals(BhParams.BhModelDef.ATTR_VALUE_TRUE)) {
			nodeAttrs.canCreateImitManually = true;
		}
		else if (strCreateImit.equals(BhParams.BhModelDef.ATTR_VALUE_FALSE) || strCreateImit.isEmpty()) {
			nodeAttrs.canCreateImitManually = false;
		}
		else {
			MsgPrinter.INSTANCE.errMsgForDebug(BhParams.BhModelDef.ATTR_NAME_CAN_CREATE_IMIT_MANUALLY + " 属性には "
				+ BhParams.BhModelDef.ATTR_VALUE_TRUE + " か "
				+ BhParams.BhModelDef.ATTR_VALUE_FALSE + " を指定してください. " + node.getBaseURI());
			return Optional.empty();
		}
		return Optional.of(nodeAttrs);
	}

	public BhNodeID getBhNodeID() {
		return bhNodeID;
	}

	public String getName() {
		return name;
	}

	public String getNodeStyleID() {
		return nodeStyleID;
	}

	public String getOnMovedFromChildToWS() {
		return onMovedFromChildToWS;
	}

	public String getOnMovedToChild() {
		return onMovedToChild;
	}

	public String getOnTextAcceptabilityChecked() {
		return onTextAcceptabilityChecked;
	}

	public String getOnDeletionCmdReceived() {
		return onDeletionCmdReceived;
	}

	public String getOnCutAndPasteCmdReceived() {
		return onCutAndPasteCmdReceived;
	}

	public String getOnChildReplaced() {
		return onChildReplaced;
	}

	public String getTextFormatter() {
		return textFormatter;
	}

	public String getImitScopeName() {
		return imitScopeName;
	}

	public String getIinitString() {
		return initString;
	}

	public String getNodeInputControlFileName() {
		return nodeInputControlFileName;
	}

	public boolean getCanCreateImitManually() {
		return canCreateImitManually;
	}

}



















