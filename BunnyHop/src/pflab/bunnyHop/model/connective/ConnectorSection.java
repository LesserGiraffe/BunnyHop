package pflab.bunnyHop.model.connective;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

import pflab.bunnyHop.modelProcessor.BhModelProcessor;
import pflab.bunnyHop.root.MsgPrinter;
import pflab.bunnyHop.common.Util;
import pflab.bunnyHop.model.BhNode;
import pflab.bunnyHop.model.SyntaxSymbol;
import pflab.bunnyHop.undo.UserOperationCommand;

/**
 * コネクタ集合を持つグループ
 * @author K.Koike
 * */
public class ConnectorSection extends Section implements Serializable {

	private final List<Connector> cnctrList; //!< コネクタリスト
	private final List<ConnectorSection.CnctrInstantiationParams> cnctrInstantiationParamsList;	//!< コネクタ生成時のパラメータ

	/**
	 * コンストラクタ
	 * @param symbolName  終端, 非終端記号名
	 * @param cnctrList 保持するコネクタのリスト
	 * @param cnctrInstantiationParamsList コネクタ生成時のパラメータ群のリスト
	 * */
	public ConnectorSection (
		String symbolName,
		List<Connector> cnctrList,
		List<ConnectorSection.CnctrInstantiationParams> cnctrInstantiationParamsList) {
		super(symbolName);
		this.cnctrList = cnctrList;
		this.cnctrInstantiationParamsList = cnctrInstantiationParamsList;
	}

	/**
	 * コピーコンストラクタ
	 * @param org コピー元オブジェクト
	 * @param parentNode このセクションを保持する ConnectiveNode オブジェクト
	 * @param parentSection このセクションを保持している Subsection オブジェクト

	 */
	private ConnectorSection(ConnectorSection org) {
		super(org);
		cnctrList = new ArrayList<>();
		cnctrInstantiationParamsList = org.cnctrInstantiationParamsList;
	}
	
	@Override
	public ConnectorSection copy(UserOperationCommand userOpeCmd) {
		
		ConnectorSection newSection = new ConnectorSection(this);
		for (int i = 0; i < cnctrList.size(); ++i) {
			CnctrInstantiationParams cnctrInstParams = cnctrInstantiationParamsList.get(i);
			Connector newConnector = 
				cnctrList.get(i).copy(
					userOpeCmd,
					cnctrInstParams.cnctrName,
					cnctrInstParams.imitationTag,
					newSection);
			newSection.cnctrList.add(newConnector);
		}
		return newSection;
	}

	/**
	 * visitor に自オブジェクトを渡す
	 * @param visitor 自オブジェクトを渡す visitorオブジェクト
	 * */
	@Override
	public void accept(BhModelProcessor visitor) {
		visitor.visit(this);
	}

	/**
	 * visitor をコネクタに渡す
	 * @param visitor コネクタに渡す visitor
	 * */
	public void introduceConnectorsTo(BhModelProcessor visitor) {
		cnctrList.forEach(connector -> connector.accept(visitor));
	}
		
	/**
	 * コネクタのリストを返す
	 * @return コネクタのリスト
	 */
	public List<Connector> getConnectorList() {
		return cnctrList;
	}
	
	@Override
	public void findSymbolInDescendants(int hierarchyLevel, boolean toBottom, List<SyntaxSymbol> foundSymbolList, String... symbolNames) {
		
		if (hierarchyLevel == 0) {
			for (String symbolName : symbolNames) {
				if (Util.equals(getSymbolName(), symbolName)) {
					foundSymbolList.add(this);
				}
			}
			if (!toBottom) {
				return;
			}
		}
		
		int childLevel = hierarchyLevel-1;
		for (Connector cnctr : cnctrList) {
			cnctr.findSymbolInDescendants(Math.max(0, childLevel), toBottom, foundSymbolList, symbolNames);
		}
	}
	
	@Override
	public BhNode findOuterEndNode() {
		
		for (int i = cnctrList.size() - 1; i >= 0; --i) {
			if (cnctrList.get(i).isOuter()) {
				return cnctrList.get(i).getConnectedNode().findOuterEndNode();
			}
		}
		return null;
	}
	
	/**
	 * モデルの構造を表示する
	 * @param depth 表示インデント数
	 * */
	@Override
	public void show(int depth) {

		int parentHash;
		if (parentNode != null)
			parentHash = parentNode.hashCode();
		else
			parentHash = parentSection.hashCode();

		MsgPrinter.instance.MsgForDebug(indent(depth) + "<ConnectorGroup  " + "name=" + getSymbolName() + "  parenNode=" + parentHash  + "  > " + this.hashCode());
		cnctrList.forEach((connector -> connector.show(depth + 1)));
	}
	
	/**
	 * コネクタ生成時のパラメータ
	 */
	public static class CnctrInstantiationParams implements Serializable{

		public final String cnctrName;	//!< コネクタ名
		public final String imitationTag;	//!< イミテーションタグ

		/**
		 * @param cnctrName コネクタ名
		 * @param imitationTag イミテーションタグ
		 */
		public CnctrInstantiationParams(
			String cnctrName, 
			String imitationTag) {
			this.cnctrName = cnctrName;
			this.imitationTag = imitationTag;
		}
	}
}




