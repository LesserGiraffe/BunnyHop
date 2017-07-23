package pflab.bunnyHop.model.templates;

import java.util.Optional;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import pflab.bunnyHop.root.MsgPrinter;
import pflab.bunnyHop.common.BhParams;
import pflab.bunnyHop.model.connective.Connector;

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
		if (!root.getNodeName().equals(BhParams.BhModelDef.elemNameConnector)) {
			MsgPrinter.instance.ErrMsgForDebug("コネクタ定義のルート要素は " + BhParams.BhModelDef.elemNameConnector + " で始めてください.  " + doc.getBaseURI());
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
		String bhID = cnctrRoot.getAttribute(BhParams.BhModelDef.attrNameBhConnectorID);
		if (bhID.isEmpty()) {
			MsgPrinter.instance.ErrMsgForDebug("<" + BhParams.BhModelDef.elemNameConnector + ">" + " タグには "
				+ BhParams.BhModelDef.attrNameBhConnectorID + " 属性を付加してください.  " + cnctrRoot.getBaseURI());
			return Optional.empty();
		}

		//Fixed
		String fixedStr = cnctrRoot.getAttribute(BhParams.BhModelDef.attrNameFixed);
		if (!fixedStr.isEmpty() && !fixedStr.equals(BhParams.BhModelDef.attrValueTrue) && !fixedStr.equals(BhParams.BhModelDef.attrValueFalse)) {
			MsgPrinter.instance.ErrMsgForDebug("<" + BhParams.BhModelDef.elemNameConnector + ">" + " タグの "
				+ BhParams.BhModelDef.attrNameFixed + " 属性は, " + cnctrRoot.getBaseURI()
				+ BhParams.BhModelDef.attrValueTrue + "か" + BhParams.BhModelDef.attrValueFalse + "で無ければなりません.  " + cnctrRoot.getBaseURI());
			return Optional.empty();
		}
		boolean fixed = fixedStr.equals(BhParams.BhModelDef.attrValueTrue);

		//初期接続ノードID
		String initNodeID = cnctrRoot.getAttribute(BhParams.BhModelDef.attrNameInitialBhNodeID);
		
		//デフォルトノードID
		String defNodeID;
		boolean hasFixedInitNode = fixed && !initNodeID.isEmpty();
		if (hasFixedInitNode) {	//初期ノードが固定ノードである => 初期ノードがデフォルトノードとなる
			defNodeID = initNodeID;
		}
		else{	
			defNodeID = cnctrRoot.getAttribute(BhParams.BhModelDef.attrNameDefaultBhNodeID);
			if (defNodeID.equals(BhParams.BhModelDef.arrtValueInitialBhNodeID))	{	//デフォルトノードに初期ノードを指定してある
				if (initNodeID.isEmpty()) {	//初期ノードが未指定
					MsgPrinter.instance.ErrMsgForDebug(
						BhParams.BhModelDef.attrNameDefaultBhNodeID + "=\"" + BhParams.BhModelDef.attrValueDefaultNodeStyleID + "\" の場合, "
						+ BhParams.BhModelDef.arrtValueInitialBhNodeID + " 属性を指定してください.");
					return Optional.empty();
				}
				else {	//デフォルトノード = 初期ノード
					defNodeID = initNodeID;
				}
			}
			
			if (defNodeID.isEmpty()) {	//初期ノードが固定ノードではないのに, デフォルトノード
				MsgPrinter.instance.ErrMsgForDebug(
					"固定初期ノードを持たない "
					+ "<" + BhParams.BhModelDef.elemNameConnector + "> および "
					+ "<" + BhParams.BhModelDef.elemNamePrivateConnector + "> タグは"
					+ BhParams.BhModelDef.attrNameDefaultBhNodeID + " 属性を持たなければなりません.  " + cnctrRoot.getBaseURI());
				return Optional.empty();
			}	
		}		
		
		//ノード入れ替え時の実行スクリプト
		String scriptNameOnReplaceabilityChecked = cnctrRoot.getAttribute(BhParams.BhModelDef.attrNameOnReplaceabilityChecked);
		if (!BhNodeTemplates.checkIfAllScriptsExist(cnctrRoot.getBaseURI(), scriptNameOnReplaceabilityChecked)) {
			return Optional.empty();
		}
		
		return Optional.of(new Connector(bhID, defNodeID, initNodeID, fixed, scriptNameOnReplaceabilityChecked));
	}
}
