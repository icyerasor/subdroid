package com.feldschmid.subdroid.util;


/**
 * This exception contains all information to retry a file-download for a file
 * that already exists locally
 * 
 */
public class FileAlreadyExistsException extends Exception {
	private static final long serialVersionUID = 1L;
	
	private String url;
	
	public String getUrl() {
		return url;
	}

	public FileAlreadyExistsException(Exception e) {
		super(e);
	}
	
	public FileAlreadyExistsException(String s, String url) {
		super(s);
		this.url = url;
	}
	
	public FileAlreadyExistsException(String s, Exception e) {
		super(s, e);
	}
}
