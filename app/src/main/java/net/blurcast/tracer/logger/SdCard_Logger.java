package net.blurcast.tracer.logger;

import android.content.Context;
import android.os.Environment;
import android.util.Log;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

/**
 * Created by blake on 10/11/14.
 */
public class SdCard_Logger extends File_Logger {

    private static final String TAG = SdCard_Logger.class.getSimpleName();

    private static final String PATH_DEFAULT_SD_DIRECTORY = "./mobile-tracer-files/";

    protected File mTraceDir;

    public SdCard_Logger(Context context) {
        super(context);

        // prepare sd card directory
        File sdCardDir = Environment.getExternalStorageDirectory();
        mTraceDir = new File(sdCardDir, PATH_DEFAULT_SD_DIRECTORY);
        mTraceDir.mkdir();
    }

    @Override
    protected void finish() {

        File sdCardPath = new File(mTraceDir, mTraceFile.getName());
        Log.d(TAG, "copying trace file to: " + sdCardPath.getAbsolutePath());

        try {
            this.copy(mTraceFile, sdCardPath);
            mTraceFile.delete();
        } catch (IOException e) {
            Log.e(TAG, e.getMessage());
        }
    }

    public void copy(File src, File dst) throws IOException {
        InputStream in = new FileInputStream(src);
        OutputStream out = new FileOutputStream(dst);

        byte[] buf = new byte[2048];
        int len;
        while((len = in.read(buf)) > 0) {
            out.write(buf, 0, len);
        }
        in.close();
        out.close();
    }
}
