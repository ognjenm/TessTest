package com.datumsolutions.util;

import java.io.File;
import java.io.IOException;

/**
 * Created by ognjenm on 23/10/16.
 */
public class FileUtils {

    private static final int TEMP_DIR_ATTEMPTS = 10000;
    /**
     * Create temp directory
     * @return
     * @throws IOException
     * This code has a race condition between delete() and mkdir():
     * A malicious process could create the target directory in the meantime
     * (taking the name of the recently-created file).
     */
    @Deprecated
    public static File createTempDirectory() throws IOException {
        final File temp;
        temp = File.createTempFile("temp", Long.toString(System.nanoTime()));
        if(!(temp.delete()))
        {
            throw new IOException("Could not delete temp file: " + temp.getAbsolutePath());
        }
        if(!(temp.mkdir()))
        {
            throw new IOException("Could not create temp directory: " + temp.getAbsolutePath());
        }

        return (temp);
    }

    public static File createTempDir() {
        File baseDir = new File(System.getProperty("java.io.tmpdir"));
        String baseName = System.currentTimeMillis() + "-";

        for (int counter = 0; counter < TEMP_DIR_ATTEMPTS; counter++) {
            File tempDir = new File(baseDir, baseName + counter);
            if (tempDir.mkdir()) {
                System.out.println("created temp dir: " + tempDir.getAbsolutePath().toString());
                return tempDir;
            }
        }
        throw new IllegalStateException("Failed to create directory within "
                + TEMP_DIR_ATTEMPTS + " attempts (tried "
                + baseName + "0 to " + baseName + (TEMP_DIR_ATTEMPTS - 1) + ')');
    }

    static public void deleteDirectory(File path)
    {
        if (path == null)
            return;
        if (path.exists())
        {
            for(File f : path.listFiles())
            {
                if(f.isDirectory())
                {
                    deleteDirectory(f);
                    f.delete();
                }
                else
                {
                    f.delete();
                }
            }
            path.delete();
        }
    }

}
