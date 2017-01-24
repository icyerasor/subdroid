package com.feldschmid.subdroid_donate.util;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;

import org.apache.http.HttpResponse;

import android.os.Environment;

import com.feldschmid.svn.base.MyException;
import com.feldschmid.svn.cmd.Get;

public class FileHelper {
	
	public static final String fileSeperator = System.getProperty("file.separator");
	
	public static DownloadFile save(Get get, String path, boolean force) throws FileAlreadyExistsException, IOException, MyException {
		String fileName = getFileName(get.getURI().getPath());
		String sdcard = Environment.getExternalStorageDirectory().getAbsolutePath();
		String destination = sdcard+fileSeperator+"subdroid"+fileSeperator+path+fileSeperator+fileName;
		File file = new File(destination);
		if(!file.exists()) {
			file.getParentFile().mkdirs();
			file.createNewFile();
		}
		else {
			if(!force)  {
				throw new FileAlreadyExistsException(destination, get.getURI().toString());
			}
		}
		
		InputStream in = get.execute();
		FileOutputStream f = new FileOutputStream(file);
		byte[] buffer = new byte[1024];
	    int len = 0;
	    while ( (len = in.read(buffer)) > 0 ) {
	         f.write(buffer, 0, len);
	    }
	    
	    // try to find the correct mimeType
	    HttpResponse response = get.getHttpResponse();
	    String contentType = response.getFirstHeader("Content-Type").getValue().toLowerCase();
	    String filteredType = null;
	    if(contentType == null) {
	    	filteredType = "text/*";
	    }
	    filteredType = normalizeType(destination, contentType);
	    return new DownloadFile(destination, filteredType);
	}
	
	private static String normalizeType(String destination, String contentType) {
		if(destination.endsWith("jpg")  || destination.endsWith("jpeg") || destination.endsWith("png") || destination.endsWith("gif")) {
			contentType = "image/*";
		}
		return contentType;
	}

	public static final String getFileName(String path) {
        int separatorIndex = path.lastIndexOf(fileSeperator);
        return (separatorIndex < 0) ? path : path.substring(separatorIndex + 1,
                path.length());
	}
}
