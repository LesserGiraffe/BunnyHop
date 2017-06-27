package pflab.bunnyHop.compiler;

/**
 * コンパイルオプションを格納するクラス
 * @author K.Koike
 */
public class CompileOption {

	public final boolean local;	//!< ルーカルで実行するコードを生成する場合true
	public final boolean isDebug;	//!< デバッグ用コードを追加する場合true
	public final boolean handleException; //!< 例外処理を行う場合true
	public final boolean withComments;	//!< ソースコードにコメントを追加する場合true

	
	public CompileOption(
		boolean local,
		boolean isDebug,
		boolean handleException,
		boolean withComments) {
		this.local = local;
		this.isDebug = isDebug;
		this.handleException = handleException;
		this.withComments = withComments;
	}
}
