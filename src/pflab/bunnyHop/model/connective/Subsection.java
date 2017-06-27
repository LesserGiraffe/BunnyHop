package pflab.bunnyHop.model.connective;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import pflab.bunnyHop.ModelProcessor.BhModelProcessor;
import pflab.bunnyHop.root.MsgPrinter;
import pflab.bunnyHop.common.Util;
import pflab.bunnyHop.model.BhNode;
import pflab.bunnyHop.model.SyntaxSymbol;
import pflab.bunnyHop.undo.UserOperationCommand;

/**
 * サブグループとして Section の集合を持つクラス
 * @author K.Koike
 * */
public class Subsection extends Section implements Serializable {

	List<Section> subsectionList = new ArrayList<>();	//!< サブグループリスト

	/**
	 * コンストラクタ
	 * @param symbolName     終端, 非終端記号名
	 * @param subsectionList サブセクションリスト
	 * */
	public Subsection(String symbolName, Collection<Section> subsectionList) {
		super(symbolName);
		this.subsectionList.addAll(subsectionList);
	}
	
	/**
	 * コピーコンストラクタ
	 * @param org コピー元オブジェクト
	 */
	private Subsection(Subsection org) {
		super(org);
	}

	@Override
	public Subsection copy(	UserOperationCommand userOpeCmd) {
		
		Subsection newSubsection = new Subsection(this);
		subsectionList.forEach(section -> {
			Section newSection = section.copy(userOpeCmd);
			newSection.setParent(newSubsection);
			newSubsection.subsectionList.add(newSection);
		});
		return newSubsection;
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
	 * visitor をサブグループに渡す
	 * @param visitor サブグループに渡す visitor
	 * */
	public void introduceSubGroupTo(BhModelProcessor visitor) {
		subsectionList.forEach(connector -> connector.accept(visitor));
	}
	
	@Override
	public void findSymbolInDescendants(String symbolName, int hierarchyLevel, boolean toBottom, List<SyntaxSymbol> foundSymbolList) {
		
		if (hierarchyLevel == 0) {
			if (Util.equals(getSymbolName(), symbolName)) {
				foundSymbolList.add(this);
			}
			if (!toBottom) {
				return;
			}
		}
		
		int childLevel = hierarchyLevel - 1;
		for (Section subsection : subsectionList) {
			subsection.findSymbolInDescendants(symbolName, Math.max(0, childLevel), toBottom, foundSymbolList);
		}
	}

	@Override
	public BhNode findOuterEndNode() {
		
		for (int i = subsectionList.size() - 1; i >= 0; --i) {
			BhNode outerEnd = subsectionList.get(i).findOuterEndNode();
			if (outerEnd != null)
				return outerEnd;
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

		MsgPrinter.instance.MsgForDebug(indent(depth) + "<ConnectorGroup" + " name=" + getSymbolName() + "  parent=" + parentHash + "  > " + this.hashCode());
		subsectionList.forEach((connector -> connector.show(depth + 1)));
	}
}






















