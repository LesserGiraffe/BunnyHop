package pflab.bunnyHop.remote;

import java.io.IOException;
import java.net.MalformedURLException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.rmi.Naming;
import java.rmi.NotBoundException;
import java.rmi.RemoteException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.Consumer;
import javafx.application.Platform;
import javafx.scene.control.Alert;
import pflab.bunnyHop.common.BhParams;
import pflab.bunnyHop.root.MsgPrinter;

/**
 *
 * @author K.Koike
 */
public class RemoteCommunicator {

	RemoteCmdProcessor rcp = null;	//!< RMIオブジェクト
	public static final RemoteCommunicator instance = new RemoteCommunicator();
	private RemoteCommunicator(){};
	ExecutorService asyncExec = Executors.newCachedThreadPool();
	
	/**
	 * 引数で指定したファイルをリモートマシンへ送る
	 * @param filePath 転送するファイルのパス
	 * @param onTransferEnd ファイル転送が終わったときに呼ぶ関数
	 */
	public void sendFile(Path filePath, Consumer<Boolean> onTransferEnd, String ipAddr) {
		
		Runnable sendFunc = () -> {

			MsgPrinter.instance.MsgForUser("ファイル転送中 " + filePath.getFileName() + "\n");
			Exception reason = new Exception();
			boolean success = false;
			for (int i = 0; i < BhParams.numRmiRetry+1; ++i) {
				
				if (i != 0) {
					MsgPrinter.instance.MsgForUser("ファイル転送リトライ (" + i + ")\n");
				}				
				byte[] data = null;
				try {
					findRCP(false, ipAddr);
					data = Files.readAllBytes(filePath);
					success = rcp.sendFile(filePath.getFileName().toString(), data);
				}
				catch (IOException | NotBoundException e) {
					reason = e;
					continue;
				}
				if (success) {
					MsgPrinter.instance.MsgForUser("ファイル転送完了 " + filePath.getFileName() + "\n");
					onTransferEnd.accept(true);
					return;
				}
			}
			String msg = reason.getMessage();
			Platform.runLater(() -> {
				MsgPrinter.instance.MsgForUser("ファイル転送エラー " + filePath.getFileName() + "\n");
				Alert alert = new Alert(Alert.AlertType.ERROR);
				alert.setTitle("ファイル転送エラー");
				alert.setHeaderText(null);
				alert.setContentText(msg);
				alert.showAndWait();
				onTransferEnd.accept(false);
			});
		};
		asyncExec.submit(sendFunc);
	}
	
	/**
	 * RemoteCmdProcessorオブジェクト(RMIオブジェクト)を探す
	 * @param doAgain すでにRMIオブジェクトが見つかっている場合でも再取得する場合true
	 * @return RemoteCmdProcessorオブジェクトが見つかった場合true.
	 */
	synchronized private void findRCP(boolean doAgain, String ipAddr) throws MalformedURLException, NotBoundException, RemoteException {
		
		if ((!doAgain) && (rcp != null)) {	//探す必要なし
			return;
		}
		rcp = (RemoteCmdProcessor)Naming.lookup("rmi://" + ipAddr + ":" + BhParams.rmiTCPPort + "/RemoteCmdProcessor");
	}
}
