package com.feldschmid.subdroid.util;

import java.io.File;

import android.net.Uri;

public class DownloadFile {
	private String location;
	private String mimeType;
	
	DownloadFile(String location, String mimeType) {
		this.location = location;
		this.mimeType = mimeType;
	}
	
	public String getLocation() {
		return location;
	}
	public void setLocation(String location) {
		this.location = location;
	}
	public String getMimeType() {
		return mimeType;
	}
	public void setMimeType(String mimeType) {
		this.mimeType = mimeType;
	}

	public Uri getData() {
		return Uri.fromFile(new File(location));
	}
}
