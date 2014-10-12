package net.blurcast.tracer.encoder;

import android.content.Context;

import net.blurcast.tracer.driver.Cell_Driver;

/**
 * Created by blake on 10/12/14.
 */
public class Cell_Encoder {

    private Cell_Driver mCell;

    public Cell_Encoder(Context context) {
        mCell = Cell_Driver.getInstance(context);
    }
}
