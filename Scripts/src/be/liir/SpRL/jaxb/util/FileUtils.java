package be.liir.SpRL.jaxb.util;

import java.io.File;

public class FileUtils {

	public static boolean checkOutputPath(String outputPath) {
		boolean result = false;
		File path = new File(outputPath);
		if(!path.exists()){
			path.mkdirs();
			result = true;
		}else if(path.isDirectory())
			result = true;
		return result;
	}

}
