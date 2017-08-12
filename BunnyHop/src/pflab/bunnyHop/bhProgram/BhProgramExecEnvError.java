package pflab.bunnyHop.bhProgram;

/**
 * BhProgramの実行環境に関するエラーコード
 * @author K.Koike
 */
public enum BhProgramExecEnvError {
	
	SUCCESS,	//!< エラーなし
	SEND_WHEN_DISCONNECTED,	//!< 切断中に送信しようとした
	SEND_QUEUE_FULL,	//!< 送信キューが満杯で送信できない
}
