package com.michogarcia.mjpegview;

import android.graphics.Bitmap;

/**
 * @author Micho Garcia
 */
public interface ImjpegViewListener {

    void success();

    void error();

    void hasBitmap(Bitmap bm);
}
