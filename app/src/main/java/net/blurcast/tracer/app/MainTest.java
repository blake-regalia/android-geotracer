package net.blurcast.tracer.app;

import android.app.Activity;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;

import net.blurcast.tracer.encoder.Cell_Encoder;
import net.blurcast.tracer.logger._Logger;

public class MainTest extends Activity {

    _Logger mSdCardLogger;

    private void init() {

//        mSdCardLogger = new SdCard_Logger(this);
//        Wap_Encoder wapEncoder = new Wap_Encoder(this, mSdCardLogger);
//        wapEncoder.start(new Subscriber<List<ScanResult>>() {
//            @Override
//            public void event(List<ScanResult> eventData, EventDetails eventDetails) {
//
//            }
//        });

        Cell_Encoder cellEncoder = new Cell_Encoder(this);

    }


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main_test);
        init();
    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.main_test, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();
        if (id == R.id.action_settings) {
            return true;
        }
        return super.onOptionsItemSelected(item);
    }
}
