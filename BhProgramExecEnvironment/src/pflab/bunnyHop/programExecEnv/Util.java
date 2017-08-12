package pflab.bunnyHop.programExecEnv;

import java.io.File;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 *
 * @author K.Koike
 */
public class Util {
	
	public static final String execPath;
	public static String scriptDir = "compiled";
	
	static {
		String path = System.getProperty("java.class.path");
		File jarFile = new File(path);
		Path jarPath = Paths.get(jarFile.getAbsolutePath());
		String root = (jarPath.getRoot() == null) ? "" : jarPath.getRoot().toString();
		execPath = root + jarPath.subpath(0, jarPath.getNameCount()-1).toString();
	}
}
