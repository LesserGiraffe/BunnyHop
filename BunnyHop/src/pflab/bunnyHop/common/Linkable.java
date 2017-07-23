package pflab.bunnyHop.common;

/**
 * 双方向リストのノードクラス
 * @author K.Koike
 * */
public class Linkable<T> {

	protected Linkable<T> next;	//!< 次のノード
	protected Linkable<T> prev;	//!< 前のノード
	protected T container;		//!< ノードの保持するもの

	/**
	 * コンストラクタ
	 * */
	public Linkable(T container) {
		this.container = container;
	}

	/**
	 * コンストラクタ
	 * */
	public Linkable() {}

	/**
	 * 次のノードを返す
	 * */
	public T getNext() {

		if (next != null)
			return next.container;
		else
			return null;
	}

	/**
	 * 前のノードを返す
	 * */
	public T getPrev() {
		if (prev != null)
			return prev.container;
		else
			return null;
	}

	/**
	 * newNode を自分の次のノードとして繋ぐ
	 * @param newNode 呼び出し元の次のノードとして繋ぐノード
	 * */
	public void connectToNext(Linkable<T> newNode) {

		if (next != null)
			next.prev = newNode;

		newNode.next = next;
		next = newNode;
		newNode.prev = this;
	}

	/**
	 * newNode を自分の前のノードとして繋ぐ
	 * @param newNode 呼び出し元の前のノードとして繋ぐノード
	 * */
	public void connectToPrev(Linkable<T> newNode) {

		if (prev != null)
			prev.next = newNode;

		newNode.prev = prev;
		prev = newNode;
		newNode.next = this;
	}

	/**
	 * 自ノードを双方向リストから取り除く
	 * */
	public void remove() {

		if (next != null)
			next.prev = prev;

		if (prev != null)
			prev.next = next;
		
		next = null;
		prev = null;
	}
}












