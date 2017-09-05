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
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import pflab.bunnyhop.root.MsgPrinter;
import pflab.bunnyhop.common.BhParams;
import pflab.bunnyhop.model.connective.Connector;

/**
 * \<Conncetor\> タグからコネクタを作成するクラス
 * @author K.Koike
 */
public class ConnectorConstructor {

	public ConnectorConstructor(){}
	
	/**
	 * コネクタテンプレートを作成する
	 * @param doc テンプレートを作成するxml の Document オブジェクト
	 * @return 作成したコネクタオブジェクト
	 */
	public Optional<Connector> genTemplate(Document doc) {

		//ルートタグチェック
		Element root = doc.getDocumentElement();
		if (!root.getNodeName().equals(BhParams.BhModelDef.ELEM_NAME_CONNECTOR)) {
			MsgPrinter.instance.ErrMsgForDebug("コネクタ定義のルート要素は " + BhParams.BhModelDef.ELEM_NAME_CONNECTOR + " で始めてください.  " + doc.getBaseURI());
			return Optional.empty();
		}
		return genTemplate(root);
	}
	
	/**
	 * コネクタテンプレートを作成する
	 * @param cnctrRoot \<Connector\> タグの要素
	 * @return 作成したコネクタオブジェクト
	 */
	public Optional<Connector> genTemplate(Element cnctrRoot) {
		
		//コネクタID
		String bhID = cnctrRoot.getAttribute(BhParams.BhModelDef.ATTR_NAME_BHCONNECTOR_ID);
		if (bhID.isEmpty()) {
			MsgPrinter.instance.ErrMsgForDebug("<" + BhParams.BhModelDef.ELEM_NAME_CONNECTOR + ">" + " タグには "
				+ BhParams.BhModelDef.ATTR_NAME_BHCONNECTOR_ID + " 属性を付加してください.  " + cnctrRoot.getBaseURI());
			return Optional.empty();
		}

		//Fixed
		String fixedStr = cnctrRoot.getAttribute(BhParams.BhModelDef.ATTR_NAME_FIXED);
		if (!fixedStr.isEmpty() && !fixedStr.equals(BhParams.BhModelDef.ATTR_VALUE_TRUE) && !fixedStr.equals(BhParams.BhModelDef.ATTR_VALUE_FALSE)) {
			MsgPrinter.instance.ErrMsgForDebug("<" + BhParams.BhModelDef.ELEM_NAME_CONNECTOR + ">" + " タグの "
				+ BhParams.BhModelDef.ATTR_NAME_FIXED + " 属性は, " + cnctrRoot.getBaseURI()
				+ BhParams.BhModelDef.ATTR_VALUE_TRUE + "か" + BhParams.BhModelDef.ATTR_VALUE_FALSE + "で無ければなりません.  " + cnctrRoot.getBaseURI());
			return Optional.empty();
		}
		boolean fixed = fixedStr.equals(BhParams.BhModelDef.ATTR_VALUE_TRUE);

		//初期接続ノードID
		String initNodeID = cnctrRoot.getAttribute(BhParams.BhModelDef.ATTR_NAME_INITIAL_BHNODE_ID);
		
		//デフォルトノードID
		String defNodeID;
		boolean hasFixedInitNode = fixed && !initNodeID.isEmpty();
		if (hasFixedInitNode) {	//初期ノードが固定ノードである => 初期ノードがデフォルトノードとなる
			defNodeID = initNodeID;
		}
		else{	
			defNodeID = cnctrRoot.getAttribute(BhParams.BhModelDef.ATTR_NAME_DEFAULT_BHNODE_ID);
			if (defNodeID.equals(BhParams.BhModelDef.ATTR_VALUE_INITIAL_BHNODE_ID))	{	//デフォルトノードに初期ノードを指定してある
				if (initNodeID.isEmpty()) {	//初期ノードが未指定
					MsgPrinter.instance.ErrMsgForDebug(
						BhParams.BhModelDef.ATTR_NAME_DEFAULT_BHNODE_ID + "が未指定の場合, "
						+ BhParams.BhModelDef.ATTR_VALUE_INITIAL_BHNODE_ID + " 属性を指定してください.");
					return Optional.empty();
				}
				else {	//デフォルトノード = 初期ノード
					defNodeID = initNodeID;
				}
			}
			
			if (defNodeID.isEmpty()) {	//初期ノードが固定ノードではないのに, デフォルトノード
				MsgPrinter.instance.ErrMsgForDebug(
					"固定初期ノードを持たない "
					+ "<" + BhParams.BhModelDef.ELEM_NAME_CONNECTOR + "> および "
					+ "<" + BhParams.BhModelDef.ELEM_NAME_PRIVATE_CONNECTOR + "> タグは"
					+ BhParams.BhModelDef.ATTR_NAME_DEFAULT_BHNODE_ID + " 属性を持たなければなりません.  " + cnctrRoot.getBaseURI());
				return Optional.empty();
			}	
		}		
		
		//ノード入れ替え時の実行スクリプト
		String scriptNameOnReplaceabilityChecked = cnctrRoot.getAttribute(BhParams.BhModelDef.ATTR_NAME_ON_REPLACEABILITY_CHECKED);
		if (!BhNodeTemplates.checkIfAllScriptsExist(cnctrRoot.getBaseURI(), scriptNameOnReplaceabilityChecked)) {
			return Optional.empty();
		}
		
		return Optional.of(new Connector(bhID, defNodeID, initNodeID, fixed, scriptNameOnReplaceabilityChecked));
	}
}
