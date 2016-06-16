package com.michogarcia.mjpegview;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.util.Log;

import java.io.BufferedInputStream;
import java.io.ByteArrayInputStream;
import java.io.DataInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

public class MjpegInputStream extends DataInputStream {
    private final byte[] SOI_MARKER = {(byte) 0xFF, (byte) 0xD8};
    private final byte[] EOF_MARKER = {(byte) 0xFF, (byte) 0xD9};
    private final static int HEADER_MAX_LENGTH = 100;
    private final static int FRAME_MAX_LENGTH = 40000 + HEADER_MAX_LENGTH;

    public static MjpegInputStream read(String url) {
        Response response;
        OkHttpClient client = new OkHttpClient();
        try {
            response = client.newCall(new Request.Builder().url(url).build()).execute();
            return new MjpegInputStream(response.body().byteStream());
        } catch (IOException ignored) {
            Log.w(Params.TAG, "IO Exception: " + ignored.getMessage());
        }
        return null;
    }

    public MjpegInputStream(InputStream in) {
        super(new BufferedInputStream(in, FRAME_MAX_LENGTH));
    }

    private int getEndOfSequence(DataInputStream in, byte[] sequence)
            throws IOException {
        int seqIndex = 0;
        byte c;
        for (int i = 0; i < FRAME_MAX_LENGTH; i++) {
            c = (byte) in.readUnsignedByte();
            if (c == sequence[seqIndex]) {
                seqIndex++;
                if (seqIndex == sequence.length)
                    return i + 1;
            } else
                seqIndex = 0;
        }
        return -1;
    }

    private int getStartOfSequence(DataInputStream in, byte[] sequence)
            throws IOException {
        int end = getEndOfSequence(in, sequence);
        return (end < 0) ? (-1) : (end - sequence.length);
    }

    private int parseContentLength(byte[] headerBytes) throws IOException,
            NumberFormatException {
        ByteArrayInputStream headerIn = new ByteArrayInputStream(headerBytes);
        Properties props = new Properties();
        props.load(headerIn);
        String CONTENT_LENGTH = "Content-Length";
        return Integer.parseInt(props.getProperty(CONTENT_LENGTH));
    }

    public Bitmap readMjpegFrame() throws IOException {
        mark(FRAME_MAX_LENGTH);
        int headerLen = getStartOfSequence(this, SOI_MARKER);
        reset();
        byte[] header = new byte[headerLen];
        readFully(header);
        int contentLength;
        try {
            contentLength = parseContentLength(header);
        } catch (NumberFormatException nfe) {
            contentLength = getEndOfSequence(this, EOF_MARKER);
        }
        reset();
        byte[] frameData = new byte[contentLength];
        skipBytes(headerLen);
        readFully(frameData);
        return BitmapFactory.decodeStream(new ByteArrayInputStream(frameData));
    }
}
