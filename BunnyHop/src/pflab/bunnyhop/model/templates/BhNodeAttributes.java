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
package pflab.bunnyhop.model.templates;

import java.util.Optional;
import org.w3c.dom.Element;
import pflab.bunnyhop.root.MsgPrinter;
import pflab.bunnyhop.common.BhParams;
import pflab.bunnyhop.view.BhNodeViewStyle;

/**
 * \<Node\> タグが持つ属性一覧
 * @author K.Koike
 */
public class BhNodeAttributes {
	
	public String bhNodeID;
	public String name;
	public String nodeStyleID;
	public String onMovedFromChildToWS;
	public String onMovedToChild;
	public String onTextInput;
	public String imitScopeName;
	public String initString;
	public String nodeInputControlFileName;
	public boolean canCreateImitManually;
	
	private BhNodeAttributes(){}
	
	/**
	 * \<Node\> タグが持つ属性一覧を読み取る
	 * @param node \<Node\>タグを表すオブジェクト
	 */
	static Optional<BhNodeAttributes> readBhNodeAttriButes(Element node) {

		BhNodeAttributes nodeAttrs = new BhNodeAttributes();
		
		//bhNodeID
		nodeAttrs.bhNodeID = node.getAttribute(BhParams.BhModelDef.attrNameBhNodeID);
		if (nodeAttrs.bhNodeID.isEmpty()) {
			MsgPrinter.instance.ErrMsgForDebug(
				"<" + BhParams.BhModelDef.elemNameNode + ">" + " タグには " 
				+ BhParams.BhModelDef.attrNameBhNodeID + " 属性を記述してください.  " + node.getBaseURI());
			return Optional.empty();
		}
		
		//name
		nodeAttrs.name = node.getAttribute(BhParams.BhModelDef.attrNameName);
		
		// nodeStyleID
		String nodeStyleID = node.getAttribute(BhParams.NodeStyleDef.keyNameNodeStyleID);
		nodeAttrs.nodeStyleID = nodeStyleID.isEmpty() ? BhParams.BhModelDef.attrValueDefaultNodeStyleID : nodeStyleID;
		BhNodeViewStyle.putNodeID_NodeStyleID(nodeAttrs.bhNodeID, nodeStyleID);
				
		//onMovedFromChildToWS
		nodeAttrs.onMovedFromChildToWS = node.getAttribute(BhParams.BhModelDef.attrNameOnMovedFromChildToWS);
		
		//onMovedToChild
		nodeAttrs.onMovedToChild = node.getAttribute(BhParams.BhModelDef.attrNameOnMovedToChild);
		
		//onTextInput
		nodeAttrs.onTextInput = node.getAttribute(BhParams.BhModelDef.attrNameOnTextInput);

		//imitScopeName
		nodeAttrs.imitScopeName = node.getAttribute(BhParams.BhModelDef.attrNameImitScopeName);
		
		//initString
		nodeAttrs.initString = node.getAttribute(BhParams.BhModelDef.attrNameInitString);
		
		//nodeInputControl
		nodeAttrs.nodeInputControlFileName = node.getAttribute(BhParams.BhModelDef.attrNameNodeInputControl);
		
		//canCreateImitManually
		String strCreateImit = node.getAttribute(BhParams.BhModelDef.attrNameCanCreateImitManually);
		if (strCreateImit.equals(BhParams.BhModelDef.attrValueTrue)) {
			nodeAttrs.canCreateImitManually = true;
		}
		else if (strCreateImit.equals(BhParams.BhModelDef.attrValueFalse) || strCreateImit.isEmpty()) {
			nodeAttrs.canCreateImitManually = false;
		}
		else {
			MsgPrinter.instance.ErrMsgForDebug(BhParams.BhModelDef.attrNameCanCreateImitManually + " 属性には "
				+ BhParams.BhModelDef.attrValueTrue + " か "
				+ BhParams.BhModelDef.attrValueFalse + " を指定してください. " + node.getBaseURI());
			return Optional.empty();
		}
		return Optional.of(nodeAttrs);
	}
	
}
