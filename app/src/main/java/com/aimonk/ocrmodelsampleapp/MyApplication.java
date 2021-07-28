package com.aimonk.ocrmodelsampleapp;

import android.app.Application;
import android.os.Environment;

import java.io.File;
import java.io.IOException;

public class MyApplication extends Application {
    private static final String TAG = "MyApplication";

    @Override
    public void onCreate() {
        super.onCreate();
        writeLogs();
    }

    private void writeLogs(){

        if ( isExternalStorageWritable() ) {

            File appDirectory = new File( getExternalFilesDir(null)  + "/" + "TezzScanner" );
            File logDirectory = new File( appDirectory + "/logs" );
            File logFile = new File( logDirectory, "logcat_file" + ".txt" );

            // create app folder
            if ( !appDirectory.exists() ) {
                appDirectory.mkdir();
            }

            // create log folder
            if ( !logDirectory.exists() ) {
                logDirectory.mkdir();
            }
//
//            String path = getExternalFilesDir(null)  + "/" + "TezzScanner" + "/logs";
//            File f = new File(path);
//            File fileTo = new File(f.toString() + "/logcat_file.png");

            // clear the previous logcat and then write the new one to the file
            try {
//                Process process = Runtime.getRuntime().exec("logcat -c");
//                process = Runtime.getRuntime().exec("logcat -f " + logFile);
                Runtime.getRuntime().exec("logcat -c");
                Runtime.getRuntime().exec("logcat -v time -f " + logFile);
            } catch ( IOException e ) {
                e.printStackTrace();
            }

        }
    }

    public boolean isExternalStorageWritable() {
        String state = Environment.getExternalStorageState();
        if ( Environment.MEDIA_MOUNTED.equals( state ) ) {
            return true;
        }
        return false;
    }

    /* Checks if external storage is available to at least read */
    public boolean isExternalStorageReadable() {
        String state = Environment.getExternalStorageState();
        if ( Environment.MEDIA_MOUNTED.equals( state ) ||
                Environment.MEDIA_MOUNTED_READ_ONLY.equals( state ) ) {
            return true;
        }
        return false;
    }
}
