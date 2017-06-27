package pflab.bunnyHop.message;

import java.util.Collection;
import pflab.bunnyHop.common.Pair;
import pflab.bunnyHop.model.BhNode;
import pflab.bunnyHop.model.Workspace;
import pflab.bunnyHop.view.BhNodeSelectionView;
import pflab.bunnyHop.view.BhNodeView;
import pflab.bunnyHop.view.WorkspaceView;
import pflab.bunnyHop.undo.UserOperationCommand;

/**
 * MsgTransporterが送信するデータ
 * @author K.Koike 
 * */
public class MsgData {

	public final BhNode node;
	public final BhNodeView nodeView;
	public final Pair<Double, Double> doublePair;
	public final Workspace workspace;
	public final WorkspaceView workspaceView;
	public final boolean bool;
	public final UserOperationCommand userOpeCmd;
	public final String text;
	public final Collection<BhNodeSelectionView> nodeSelectionViewList;

	public MsgData(BhNodeView view) {
		this.node = null;
		this.nodeView = view;
		this.doublePair = null;
		this.workspace = null;
		this.workspaceView = null;
		this.bool = false;
		this.userOpeCmd = null;
		this.text = null;
		this.nodeSelectionViewList = null;
	}

	public MsgData(double val1, double val2) {
		this.node = null;
		this.nodeView = null;
		this.doublePair = new Pair<>(val1, val2);
		this.workspace = null;
		this.workspaceView = null;
		this.bool = false;
		this.userOpeCmd = null;
		this.text = null;
		this.nodeSelectionViewList = null;
	}

	public MsgData(Workspace workspace, WorkspaceView workspaceView) {
		this.node = null;
		this.nodeView = null;
		this.doublePair = null;
		this.workspace = workspace;
		this.workspaceView = workspaceView;
		this.bool = false;
		this.userOpeCmd = null;
		this.text = null;
		this.nodeSelectionViewList = null;
	}

	public MsgData(BhNode node, BhNodeView view) {
		this.node = node;
		this.nodeView = view;
		this.doublePair = null;
		this.workspace = null;
		this.workspaceView = null;
		this.bool = false;
		this.userOpeCmd = null;
		this.text = null;
		this.nodeSelectionViewList = null;
	}

	public MsgData(BhNode node) {
		this.node = node;
		this.nodeView = null;
		this.doublePair = null;
		this.workspace = null;
		this.workspaceView = null;
		this.bool = false;
		this.userOpeCmd = null;
		this.text = null;
		this.nodeSelectionViewList = null;
	}

	public MsgData(BhNodeSelectionView templatePanel) {
		this.node = null;
		this.nodeView = null;
		this.doublePair = null;
		this.workspace = null;
		this.workspaceView = null;
		this.bool = false;
		this.userOpeCmd = null;
		this.text = null;
		this.nodeSelectionViewList = null;
	}

	public MsgData(boolean bool) {
		this.node = null;
		this.nodeView = null;
		this.doublePair = null;
		this.workspace = null;
		this.workspaceView = null;
		this.bool = bool;
		this.userOpeCmd = null;
		this.text = null;
		this.nodeSelectionViewList = null;
	}
	
	public MsgData(UserOperationCommand userOpeCmd) {
		this.node = null;
		this.nodeView = null;
		this.doublePair = null;
		this.workspace = null;
		this.workspaceView = null;
		this.bool = false;
		this.userOpeCmd = userOpeCmd;
		this.text = null;
		this.nodeSelectionViewList = null;
	}
	
	public MsgData(String text) {
		this.node = null;
		this.nodeView = null;
		this.doublePair = null;
		this.workspace = null;
		this.workspaceView = null;
		this.bool = false;
		this.userOpeCmd = null;
		this.text = text;
		this.nodeSelectionViewList = null;
	}
	
	public MsgData(Workspace workspace) {
		this.node = null;
		this.nodeView = null;
		this.doublePair = null;
		this.workspace = workspace;
		this.workspaceView = null;
		this.bool = false;
		this.userOpeCmd = null;
		this.text = null;
		this.nodeSelectionViewList = null;
	}
	
	public MsgData(boolean bool, String text) {
		this.node = null;
		this.nodeView = null;
		this.doublePair = null;
		this.workspace = null;
		this.workspaceView = null;
		this.bool = bool;
		this.userOpeCmd = null;
		this.text = text;
		this.nodeSelectionViewList = null;
	}
	
	public MsgData(Collection<BhNodeSelectionView> nodeSelectionViewList) {
		this.node = null;
		this.nodeView = null;
		this.doublePair = null;
		this.workspace = null;
		this.workspaceView = null;
		this.bool = false;
		this.userOpeCmd = null;
		this.text = null;
		this.nodeSelectionViewList = nodeSelectionViewList;
	}
}
















