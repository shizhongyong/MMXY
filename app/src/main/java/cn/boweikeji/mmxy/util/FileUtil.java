package cn.boweikeji.mmxy.util;

import android.util.Base64;

import java.io.ByteArrayOutputStream;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;

/**
 * Created by shizy on 2018/6/25.
 */

public class FileUtil {

	public static String base64File(String path) {
		try {
			FileInputStream fis = new FileInputStream(path);
			ByteArrayOutputStream baos = new ByteArrayOutputStream();

			int len = 0;
			byte[] buff = new byte[1024 * 1024];
			while ((len = fis.read(buff)) > 0) {
				baos.write(buff, 0, len);
			}

			String result = Base64.encodeToString(baos.toByteArray(), Base64.DEFAULT);

			fis.close();
			baos.close();

			return result;
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}

		return null;
	}

}
