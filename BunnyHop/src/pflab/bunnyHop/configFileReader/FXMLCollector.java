package pflab.bunnyHop.configFileReader;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Stream;
import pflab.bunnyHop.common.BhParams;
import pflab.bunnyHop.common.Util;
import pflab.bunnyHop.root.MsgPrinter;

/**
 * FXMLファイルとそのパスを保存するクラス
 * @author Koike
 */
public class FXMLCollector {
	
	public static final FXMLCollector instance = new FXMLCollector();
	private static final Map<String, Path> fileName_filePath = new HashMap<>();
	
	private FXMLCollector(){}
	
	/**
	 * FXMLファイルのファイル名とそのパスを集める
	 * @return FXMLファイルのフォルダが見つからなかった場合 falseを返す
	 */
	public boolean collectFXMLFiles() {
		
		Path dirPath = Paths.get(Util.execPath, BhParams.Path.viewDir, BhParams.Path.fxmlDir);
		Stream<Path> paths;	//読み込むファイルパスリスト
		try {
			paths = Files.walk(dirPath).filter(path -> path.getFileName().toString().endsWith(".fxml")); //.fxmlファイルだけ収集
		}
		catch (IOException e) {
			MsgPrinter.instance.ErrMsgForDebug("fxml directory not found " + dirPath);
			return false;
		}
		paths.forEach(filePath -> fileName_filePath.put(filePath.getFileName().toString(), filePath));
		return true;
	}
	
	/**
	 * FXMLファイル名からそのファイルのフルパスを取得する
	 * @param fileName フルパスを知りたいFXMLファイル名
	 * @return fileName で指定したファイルのパスオブジェクト. パスが見つからない場合はnullを返す
	 */
	public Path getFilePath(String fileName) {
		return fileName_filePath.getOrDefault(fileName, null);
	}
}
