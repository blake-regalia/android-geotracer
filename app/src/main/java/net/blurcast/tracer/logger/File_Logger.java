package net.blurcast.tracer.logger;

import android.content.Context;
import android.os.Environment;
import android.util.Log;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Calendar;
import java.util.Random;

/**
 * Created by blake on 10/11/14.
 */
public class File_Logger extends _Logger {

    private static final String TAG = File_Logger.class.getSimpleName();

    private static final char CHAR_JOIN  = '-';
    private static final char CHAR_SPLIT = '_';
    private static final char CHAR_END   = '.';

    protected File mFilesDir;
    protected File mTraceFile;
    protected FileOutputStream mTraceFileData;


    public File_Logger(Context context) {
        super(context);

        // set file directory
        mFilesDir = mContext.getFilesDir();

        // generate unique filename
        Calendar start = Calendar.getInstance();
        start.setTimeInMillis(System.currentTimeMillis());
        String fileName = ""+(start.get(Calendar.YEAR)+"")+CHAR_JOIN
                +zeroPadTwoDigits(start.get(Calendar.MONTH) + 1)+CHAR_JOIN
                +zeroPadTwoDigits(start.get(Calendar.DAY_OF_MONTH))+CHAR_SPLIT
                +zeroPadTwoDigits(start.get(Calendar.HOUR_OF_DAY))+CHAR_JOIN
                +zeroPadTwoDigits(start.get(Calendar.MINUTE))+CHAR_JOIN
                +zeroPadTwoDigits(start.get(Calendar.SECOND))+CHAR_SPLIT
                +new Random().nextInt();

        // create new file
        this.createAndOpenFile(fileName);

        // write file header
        this.writeHeader();
    }

    private String zeroPadTwoDigits(int d) {
        if(d < 10) {
            return "0"+d;
        }
        return ""+d;
    }

    /**
     * Creates a new trace file in the current application's file directory
     * @param fileName
     */
    protected void createAndOpenFile(String fileName) {
        try {
            mTraceFile = new File(mFilesDir, fileName);
            mTraceFileData = mContext.openFileOutput(fileName, Context.MODE_PRIVATE);
        } catch (FileNotFoundException e) {

        }
    }


    /**
     * Writes data to the open trace file
     * @param data      byte[] of data to submit to the log file
     */
    @Override
    public void submit(byte[] data) {
        try {
            if(mTraceFileData == null) throw new NullPointerException("Trace file was never opened");
            mTraceFileData.write(data);
        } catch (IOException e) {
            Log.e(TAG, e.getMessage());
        }
    }


    /**
     * Closes the trace file and moves it to the SD card
     */
    @Override
    public void close() {
        if(mTraceFileData != null) {
            try {
                mTraceFileData.close();
                mTraceFileData = null;
                this.finish();
            } catch (IOException e) {
                Log.e(TAG, e.getMessage());
            }
        }
        else {
            Log.w(TAG, "No trace file to close");
        }
    }

    protected void finish() {}
}
