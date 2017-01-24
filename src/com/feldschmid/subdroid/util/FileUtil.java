package com.feldschmid.subdroid.util;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.channels.FileChannel;


public class FileUtil {

  public static void copyFile(File source, File dest) throws IOException {
    FileChannel inC = new FileInputStream(source).getChannel();
    FileChannel outC = new FileOutputStream(dest).getChannel();

    try {
      inC.transferTo(0, inC.size(), outC);
    } finally {
      if (inC != null) {
        inC.close();
      }
      if (outC != null) {
        outC.close();
      }
    }
  }
}
