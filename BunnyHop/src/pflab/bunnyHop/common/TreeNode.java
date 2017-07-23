package pflab.bunnyHop.common;

import java.util.ArrayList;

/**
 * @author K.Koike
 */
public class TreeNode<T> {

	public T content;
	public  ArrayList<TreeNode<T>> children = new ArrayList<>();

	public boolean isLeaf() {
		return children.isEmpty();
	}

	public TreeNode (T content) {
		this.content = content;
	}
}
