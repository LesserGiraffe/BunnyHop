package pflab.bunnyHop.undo;

import java.util.Deque;
import java.util.LinkedList;
import pflab.bunnyHop.common.BhParams;

/**
 * undo/redo時に UserOperationCommand クラスを操作するクラス
 * @author Koike
 */
public class UserOpeCmdManager {

	Deque<UserOperationCommand> undoStack = new LinkedList<>();	//Undo できるコマンドのスタック
	Deque<UserOperationCommand> redoStack = new LinkedList<>();	//Redo できるコマンドのスタック
	
	/**
	 * Undo の対象になるコマンドを追加する
	 * @param cmd Undo の対象になるコマンド
	 */
	public void pushUndoCommand(UserOperationCommand cmd) {
		
		undoStack.addLast(cmd);
		if(undoStack.size() > BhParams.numMaxUndoTimes)
			undoStack.removeFirst();
		
		redoStack.clear();
	}
	
	public void undo() {
		
		if (undoStack.isEmpty())
			return;
		
		UserOperationCommand invCmd = undoStack.removeLast().doInverseOperation();
		redoStack.addLast(invCmd);
	}

	public void redo() {
		
		if (redoStack.isEmpty())
			return;
		
		UserOperationCommand invCmd = redoStack.removeLast().doInverseOperation();
		undoStack.addLast(invCmd);
	}
}


